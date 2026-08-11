package com.k1ngtle.vsia.signality.core.signal;

import com.k1ngtle.vsia.signality.Signality;
import com.k1ngtle.vsia.signality.api.occlusion.CompositeOcclusionProvider;
import com.k1ngtle.vsia.signality.api.radar.RadarRegistry;
import com.k1ngtle.vsia.signality.api.signal.ISignalReceiver;
import com.k1ngtle.vsia.signality.api.signal.ISignalTransmitter;
import com.k1ngtle.vsia.signality.api.signal.PathLossModel;
import com.k1ngtle.vsia.signality.api.signal.SignalPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class SignalBus {
   public static final int OCCLUSION_SAMPLES = 8;
   private static final ConcurrentHashMap<UUID, ISignalTransmitter> TX = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<UUID, ISignalReceiver> RX = new ConcurrentHashMap<>();

   private SignalBus() {
   }

   public static void registerTransmitter(ISignalTransmitter tx) {
      TX.put(tx.id(), tx);
   }

   public static void unregisterTransmitter(UUID id) {
      TX.remove(id);
   }

   public static void registerReceiver(ISignalReceiver rx) {
      RX.put(rx.id(), rx);
   }

   public static void unregisterReceiver(UUID id) {
      RX.remove(id);
   }

   public static void broadcast(SignalPacket packet, ServerLevel level) {
      Objects.requireNonNull(packet, "packet");
      Objects.requireNonNull(level, "level");
      List<ISignalReceiver> recipients = receiversIn(level);
      if (!recipients.isEmpty()) {
         CompositeOcclusionProvider occlusion = RadarRegistry.occlusionProviders();

         for (ISignalReceiver rx : recipients) {
            if (!rx.id().equals(packet.transmitterId()) && rx.band().contains(packet.frequencyHz()) && polarizationOk(packet, rx) && tunedTo(packet, rx)) {
               Vec3 from = packet.originWorld();
               Vec3 to = rx.positionWorld();
               double range = from.distanceTo(to);
               if (range < 0.001) {
                  range = 0.001;
               }

               double freeSpace = PathLossModel.freeSpacePower(packet.transmitPowerWatts(), packet.antennaGain(), rx.antennaGain(), packet.frequencyHz(), range);
               if (!(freeSpace < rx.sensitivityWatts())) {
                  double blockedFraction = sampleBlockedFraction(level, occlusion, from, to);
                  double terrainAtt = PathLossModel.terrainAttenuationFromBlockedFraction(blockedFraction);
                  double received = freeSpace * terrainAtt;
                  if (!(received < rx.sensitivityWatts())) {
                     try {
                        rx.onReceive(packet, received);
                     } catch (Throwable var19) {
                        Signality.LOGGER.warn("Receiver {} threw in onReceive", rx.id(), var19);
                     }
                  }
               }
            }
         }
      }
   }

   private static List<ISignalReceiver> receiversIn(ServerLevel level) {
      List<ISignalReceiver> out = new ArrayList<>();

      for (ISignalReceiver rx : RX.values()) {
         if (rx.level() == level) {
            out.add(rx);
         }
      }

      return out;
   }

   private static boolean polarizationOk(SignalPacket packet, ISignalReceiver rx) {
      String required = rx.requiredPolarization();
      return required == null ? true : required.equalsIgnoreCase(packet.polarization());
   }

   private static boolean tunedTo(SignalPacket packet, ISignalReceiver rx) {
      double[] freqs = rx.tunedFrequenciesHz();
      if (freqs != null && freqs.length != 0) {
         double bw = rx.tuningBandwidthHz();
         double half = bw / 2.0;

         for (double f : freqs) {
            if (Math.abs(packet.frequencyHz() - f) <= half) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static double sampleBlockedFraction(ServerLevel level, CompositeOcclusionProvider occlusion, Vec3 from, Vec3 to) {
      int hits = 0;

      for (int i = 1; i <= 8; i++) {
         double t = (double)i / 9.0;
         Vec3 a = from.lerp(to, Math.max(0.0, t - 0.125));
         Vec3 b = from.lerp(to, t);
         if (occlusion.isOccluded(level, a, b)) {
            hits++;
         }
      }

      return (double)hits / 8.0;
   }
}
