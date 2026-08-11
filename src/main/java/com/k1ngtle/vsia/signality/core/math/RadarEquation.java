package com.k1ngtle.vsia.signality.core.math;

import com.k1ngtle.vsia.signality.api.radar.RadarProfile;

public final class RadarEquation {
   private static final double FOUR_PI_CUBED = Math.pow(Math.PI * 4, 3.0);

   private RadarEquation() {
   }

   public static double receivedPowerWatts(RadarProfile profile, double rcsSquareMeters, double rangeMeters) {
      return receivedPowerWatts(
         profile.peakPowerWatts(), profile.antennaGain(), profile.wavelengthMeters(), profile.systemLossLinear(), rcsSquareMeters, rangeMeters
      );
   }

   public static double receivedPowerWatts(double peakPowerW, double antennaGainLinear, double wavelengthM, double systemLoss, double rcs, double rangeM) {
      if (rangeM < 0.001) {
         rangeM = 0.001;
      }

      double num = peakPowerW * antennaGainLinear * antennaGainLinear * wavelengthM * wavelengthM * rcs;
      double R4 = rangeM * rangeM * rangeM * rangeM;
      double denom = FOUR_PI_CUBED * R4 * systemLoss;
      return num / denom;
   }

   public static double snr(RadarProfile profile, double rcsSquareMeters, double rangeM) {
      double pr = receivedPowerWatts(profile, rcsSquareMeters, rangeM);
      return pr / profile.noisePowerWatts();
   }

   public static double maxDetectionRange(RadarProfile profile, double rcsSquareMeters) {
      double num = profile.peakPowerWatts()
         * profile.antennaGain()
         * profile.antennaGain()
         * profile.wavelengthMeters()
         * profile.wavelengthMeters()
         * rcsSquareMeters;
      double denom = FOUR_PI_CUBED * profile.systemLossLinear() * profile.noisePowerWatts() * profile.minDetectableSnr();
      return Math.pow(num / denom, 0.25);
   }
}
