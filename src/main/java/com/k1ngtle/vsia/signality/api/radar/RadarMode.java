package com.k1ngtle.vsia.signality.api.radar;

public enum RadarMode {
   SEARCH(true, false, 1.0),
   TRACK_WHILE_SCAN(true, true, 1.0),
   SINGLE_TARGET_TRACK(false, true, 0.25),
   MAPPING(true, false, 1.0),
   PASSIVE(true, true, 1.0);

   private final boolean wideArc;
   private final boolean persistsTracks;
   private final double beamWidthMultiplier;

   private RadarMode(boolean wideArc, boolean persistsTracks, double beamWidthMultiplier) {
      this.wideArc = wideArc;
      this.persistsTracks = persistsTracks;
      this.beamWidthMultiplier = beamWidthMultiplier;
   }

   public boolean wideArc() {
      return this.wideArc;
   }

   public boolean persistsTracks() {
      return this.persistsTracks;
   }

   public double beamWidthMultiplier() {
      return this.beamWidthMultiplier;
   }
}
