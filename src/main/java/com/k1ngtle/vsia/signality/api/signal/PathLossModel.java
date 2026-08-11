package com.k1ngtle.vsia.signality.api.signal;

public final class PathLossModel {
   public static final double C = 2.99792458E8;

   private PathLossModel() {
   }

   public static double wavelength(double frequencyHz) {
      return 2.99792458E8 / frequencyHz;
   }

   public static double freeSpacePower(double pt, double gt, double gr, double frequencyHz, double rangeM) {
      if (rangeM < 0.001) {
         rangeM = 0.001;
      }

      double lambda = wavelength(frequencyHz);
      double denom = (Math.PI * 4) * rangeM;
      double factor = lambda / denom;
      return pt * gt * gr * factor * factor;
   }

   public static double knifeEdgeAttenuation(double v) {
      if (v < -0.78) {
         return 1.0;
      } else {
         double db = 6.9 + 20.0 * Math.log10(Math.sqrt((v - 0.1) * (v - 0.1) + 1.0) + v - 0.1);
         return Math.pow(10.0, -db / 10.0);
      }
   }

   public static double terrainAttenuationFromBlockedFraction(double blockedFraction) {
      double f = Math.max(0.0, Math.min(1.0, blockedFraction));
      if (f <= 1.0E-6) {
         return 1.0;
      } else {
         double v = -0.78 + 5.78 * f;
         return knifeEdgeAttenuation(v);
      }
   }

   public static double dbm(double watts) {
      return 10.0 * Math.log10(watts) + 30.0;
   }

   public static double wattsFromDbm(double dbm) {
      return Math.pow(10.0, (dbm - 30.0) / 10.0);
   }
}
