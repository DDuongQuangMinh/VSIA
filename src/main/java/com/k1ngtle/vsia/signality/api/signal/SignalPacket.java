package com.k1ngtle.vsia.signality.api.signal;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record SignalPacket(
   UUID transmitterId,
   Vec3 originWorld,
   double frequencyHz,
   double transmitPowerWatts,
   double antennaGain,
   byte[] payload,
   long timestampNanos,
   int ttlHops,
   @Nullable String polarization
) {
   public SignalPacket(
      UUID transmitterId,
      Vec3 originWorld,
      double frequencyHz,
      double transmitPowerWatts,
      double antennaGain,
      byte[] payload,
      long timestampNanos,
      int ttlHops,
      @Nullable String polarization
   ) {
      if (transmitterId == null) {
         throw new IllegalArgumentException("transmitterId");
      } else if (originWorld == null) {
         throw new IllegalArgumentException("originWorld");
      } else if (frequencyHz <= 0.0) {
         throw new IllegalArgumentException("frequencyHz");
      } else if (transmitPowerWatts <= 0.0) {
         throw new IllegalArgumentException("transmitPower");
      } else if (antennaGain <= 0.0) {
         throw new IllegalArgumentException("antennaGain");
      } else {
         if (payload == null) {
            payload = new byte[0];
         }

         this.transmitterId = transmitterId;
         this.originWorld = originWorld;
         this.frequencyHz = frequencyHz;
         this.transmitPowerWatts = transmitPowerWatts;
         this.antennaGain = antennaGain;
         this.payload = payload;
         this.timestampNanos = timestampNanos;
         this.ttlHops = ttlHops;
         this.polarization = polarization;
      }
   }
}
