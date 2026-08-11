package com.k1ngtle.vsia.signality.api.radar;

public record RadarProfile(
   double peakPowerWatts,
   double antennaGain,
   double wavelengthMeters,
   double systemLossLinear,
   double noisePowerWatts,
   double minDetectableSnr,
   double halfBeamWidthRad,
   double maxRangeMeters,
   double pulseRepetitionHz,
   double dopplerGateMps,
   RadarBand band
) {
   public RadarProfile(
      double peakPowerWatts,
      double antennaGain,
      double wavelengthMeters,
      double systemLossLinear,
      double noisePowerWatts,
      double minDetectableSnr,
      double halfBeamWidthRad,
      double maxRangeMeters,
      double pulseRepetitionHz,
      double dopplerGateMps,
      RadarBand band
   ) {
      if (peakPowerWatts <= 0.0) {
         throw new IllegalArgumentException("peakPower must be > 0");
      } else if (antennaGain <= 0.0) {
         throw new IllegalArgumentException("antennaGain must be > 0");
      } else if (wavelengthMeters <= 0.0) {
         throw new IllegalArgumentException("wavelength must be > 0");
      } else if (systemLossLinear < 1.0) {
         throw new IllegalArgumentException("systemLoss must be >= 1.0");
      } else if (noisePowerWatts <= 0.0) {
         throw new IllegalArgumentException("noisePower must be > 0");
      } else if (minDetectableSnr <= 0.0) {
         throw new IllegalArgumentException("minDetectableSnr must be > 0");
      } else if (halfBeamWidthRad <= 0.0 || halfBeamWidthRad > Math.PI / 2) {
         throw new IllegalArgumentException("halfBeamWidth must be in (0, Ï€/2]");
      } else if (maxRangeMeters <= 0.0) {
         throw new IllegalArgumentException("maxRange must be > 0");
      } else if (pulseRepetitionHz <= 0.0) {
         throw new IllegalArgumentException("PRF must be > 0");
      } else if (dopplerGateMps < 0.0) {
         throw new IllegalArgumentException("dopplerGate must be >= 0");
      } else if (band == null) {
         throw new IllegalArgumentException("band must not be null");
      } else {
         this.peakPowerWatts = peakPowerWatts;
         this.antennaGain = antennaGain;
         this.wavelengthMeters = wavelengthMeters;
         this.systemLossLinear = systemLossLinear;
         this.noisePowerWatts = noisePowerWatts;
         this.minDetectableSnr = minDetectableSnr;
         this.halfBeamWidthRad = halfBeamWidthRad;
         this.maxRangeMeters = maxRangeMeters;
         this.pulseRepetitionHz = pulseRepetitionHz;
         this.dopplerGateMps = dopplerGateMps;
         this.band = band;
      }
   }

   public int ticksPerScan() {
      return Math.max(1, (int)Math.round(20.0 / this.pulseRepetitionHz));
   }

   public static RadarProfile.Builder builder() {
      return new RadarProfile.Builder();
   }

   public static final class Builder {
      private double peakPower = 50000.0;
      private double antennaGain = 3162.0;
      private double wavelength = RadarBand.X.centreWavelengthMeters();
      private double systemLoss = 2.0;
      private double noisePower = 1.0E-13;
      private double minSnr = 10.0;
      private double halfBeamWidth = Math.toRadians(2.5);
      private double maxRange = 80000.0;
      private double prf = 5.0;
      private double dopplerGate = 30.0;
      private RadarBand band = RadarBand.X;

      public RadarProfile.Builder peakPowerWatts(double v) {
         this.peakPower = v;
         return this;
      }

      public RadarProfile.Builder antennaGain(double v) {
         this.antennaGain = v;
         return this;
      }

      public RadarProfile.Builder antennaGainDb(double db) {
         this.antennaGain = Math.pow(10.0, db / 10.0);
         return this;
      }

      public RadarProfile.Builder wavelengthMeters(double v) {
         this.wavelength = v;
         return this;
      }

      public RadarProfile.Builder systemLossLinear(double v) {
         this.systemLoss = v;
         return this;
      }

      public RadarProfile.Builder noisePowerWatts(double v) {
         this.noisePower = v;
         return this;
      }

      public RadarProfile.Builder minDetectableSnr(double v) {
         this.minSnr = v;
         return this;
      }

      public RadarProfile.Builder minDetectableSnrDb(double db) {
         this.minSnr = Math.pow(10.0, db / 10.0);
         return this;
      }

      public RadarProfile.Builder halfBeamWidthRad(double v) {
         this.halfBeamWidth = v;
         return this;
      }

      public RadarProfile.Builder halfBeamWidthDeg(double d) {
         this.halfBeamWidth = Math.toRadians(d);
         return this;
      }

      public RadarProfile.Builder maxRangeMeters(double v) {
         this.maxRange = v;
         return this;
      }

      public RadarProfile.Builder pulseRepetitionHz(double v) {
         this.prf = v;
         return this;
      }

      public RadarProfile.Builder dopplerGateMps(double v) {
         this.dopplerGate = v;
         return this;
      }

      public RadarProfile.Builder band(RadarBand b) {
         this.band = b;
         this.wavelength = b.centreWavelengthMeters();
         return this;
      }

      public RadarProfile build() {
         return new RadarProfile(
            this.peakPower,
            this.antennaGain,
            this.wavelength,
            this.systemLoss,
            this.noisePower,
            this.minSnr,
            this.halfBeamWidth,
            this.maxRange,
            this.prf,
            this.dopplerGate,
            this.band
         );
      }
   }
}
