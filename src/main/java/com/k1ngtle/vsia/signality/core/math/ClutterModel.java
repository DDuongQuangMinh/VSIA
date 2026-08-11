package com.k1ngtle.vsia.signality.core.math;

import com.k1ngtle.vsia.signality.api.radar.RadarProfile;

public final class ClutterModel {
   private static final double GRAZING_FLOOR_RAD = 0.1;

   private ClutterModel() {
   }

   public static double beamFootprintArea(double rangeMeters, double halfBeamWidthRad, double grazingAngleRad) {
      double linear = 2.0 * rangeMeters * halfBeamWidthRad;
      double area = linear * linear;
      double sg = Math.sin(Math.max(0.1, Math.abs(grazingAngleRad)));
      return area / sg;
   }

   public static double clutterPowerWatts(RadarProfile profile, double rangeMeters, double grazingAngleRad, double sigmaZeroLinear) {
      double area = beamFootprintArea(rangeMeters, profile.halfBeamWidthRad(), grazingAngleRad);
      double sigmaClutter = sigmaZeroLinear * area;
      return RadarEquation.receivedPowerWatts(profile, sigmaClutter, rangeMeters);
   }

   public static double scr(double targetEchoWatts, double clutterWatts) {
      return clutterWatts <= 1.0E-30 ? Double.POSITIVE_INFINITY : targetEchoWatts / clutterWatts;
   }

   public static double defaultSigmaZero(double grazingAngleRad) {
      return 0.04 * Math.sin(Math.max(0.1, Math.abs(grazingAngleRad)));
   }

   public static boolean isLookDown(double grazingAngleRad) {
      return grazingAngleRad > 0.01;
   }
}
