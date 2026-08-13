package com.k1ngtle.vsia.signality.api.signal;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public interface ISignalReceiver {
   UUID id();

   ServerLevel level();

   Vec3 positionWorld();

   SignalBand band();

   default double antennaGain() {
      return 1.0;
   }

   double sensitivityWatts();

   double[] tunedFrequenciesHz();

   double tuningBandwidthHz();

   default String requiredPolarization() {
      return null;
   }

   /** Maximum physical delivery distance in blocks. */
   default double maximumReceptionRangeBlocks() {
      return Double.POSITIVE_INFINITY;
   }

   void onReceive(SignalPacket var1, double var2);
}
