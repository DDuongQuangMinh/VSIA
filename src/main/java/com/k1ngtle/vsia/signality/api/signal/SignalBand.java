package com.k1ngtle.vsia.signality.api.signal;

public enum SignalBand {
   VLF(3000.0, 30000.0),
   LF(30000.0, 300000.0),
   MF(300000.0, 3000000.0),
   HF(3000000.0, 3.0E7),
   VHF(3.0E7, 3.0E8),
   UHF(3.0E8, 3.0E9),
   SHF(3.0E9, 3.0E10),
   EHF(3.0E10, 3.0E11);

   private final double minHz;
   private final double maxHz;

   private SignalBand(double minHz, double maxHz) {
      this.minHz = minHz;
      this.maxHz = maxHz;
   }

   public double minHz() {
      return this.minHz;
   }

   public double maxHz() {
      return this.maxHz;
   }

   public boolean contains(double frequencyHz) {
      return frequencyHz >= this.minHz && frequencyHz < this.maxHz;
   }

   public static SignalBand forFrequency(double frequencyHz) {
      for (SignalBand b : values()) {
         if (b.contains(frequencyHz)) {
            return b;
         }
      }

      return frequencyHz >= EHF.maxHz ? EHF : VLF;
   }
}
