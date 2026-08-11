package com.k1ngtle.vsia.signality.api.radar;

public enum RadarBand {
   HF(15.0),
   VHF(2.0),
   UHF(0.7),
   L(0.23),
   S(0.1),
   C(0.05),
   X(0.03),
   KU(0.018),
   K(0.012),
   KA(0.008),
   MMW(0.003);

   private final double centreWavelengthMeters;

   private RadarBand(double centreWavelengthMeters) {
      this.centreWavelengthMeters = centreWavelengthMeters;
   }

   public double centreWavelengthMeters() {
      return this.centreWavelengthMeters;
   }

   public boolean isClutterProne() {
      return this.ordinal() >= L.ordinal();
   }
}
