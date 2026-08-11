package com.k1ngtle.vsia.signality.core.math;

public final class DopplerFilter {
   public static final double DEFAULT_IMPROVEMENT_LINEAR = 3162.0;

   private DopplerFilter() {
   }

   public static double dopplerShiftHz(double closureRateMps, double carrierFrequencyHz) {
      return 2.0 * closureRateMps * carrierFrequencyHz / 2.99792458E8;
   }

   public static boolean passesGate(double closureRateMps, double gateMps) {
      return Math.abs(closureRateMps) >= gateMps;
   }

   public static double clutterSuppressionFactor(double closureRateMps, double gateMps) {
      return clutterSuppressionFactor(closureRateMps, gateMps, 3162.0);
   }

   public static double clutterSuppressionFactor(double closureRateMps, double gateMps, double improvementLinear) {
      if (!passesGate(closureRateMps, gateMps)) {
         return 1.0;
      } else {
         double margin = (Math.abs(closureRateMps) - gateMps) / Math.max(gateMps, 1.0);
         double smooth = margin / (margin + 1.0);
         return 1.0 + (improvementLinear - 1.0) * smooth;
      }
   }

   public static boolean isDetectable(double targetEchoWatts, double clutterWatts, double closureRateMps, double gateMps, double minScrLinear) {
      if (clutterWatts <= 0.0) {
         return true;
      } else {
         double rawScr = targetEchoWatts / clutterWatts;
         double effective = rawScr * clutterSuppressionFactor(closureRateMps, gateMps);
         return effective >= minScrLinear;
      }
   }
}
