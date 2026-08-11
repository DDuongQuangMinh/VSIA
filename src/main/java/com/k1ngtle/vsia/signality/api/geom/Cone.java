package com.k1ngtle.vsia.signality.api.geom;

import net.minecraft.world.phys.Vec3;

public record Cone(Vec3 apex, Vec3 axisUnit, double halfAngleRad, double rangeMeters, double cosHalfAngle, double tanHalfAngle) {
   public Cone(Vec3 apex, Vec3 axisUnit, double halfAngleRad, double rangeMeters, double cosHalfAngle, double tanHalfAngle) {
      if (halfAngleRad <= 0.0 || halfAngleRad > Math.PI / 2) {
         throw new IllegalArgumentException("halfAngle must be in (0, Ï€/2]");
      } else if (rangeMeters <= 0.0) {
         throw new IllegalArgumentException("range must be > 0");
      } else {
         this.apex = apex;
         this.axisUnit = axisUnit;
         this.halfAngleRad = halfAngleRad;
         this.rangeMeters = rangeMeters;
         this.cosHalfAngle = cosHalfAngle;
         this.tanHalfAngle = tanHalfAngle;
      }
   }

   public static Cone of(Vec3 apex, Vec3 axis, double halfAngleRad, double rangeMeters) {
      double len = axis.length();
      if (len < 1.0E-9) {
         throw new IllegalArgumentException("axis is zero");
      } else {
         Vec3 unit = axis.scale(1.0 / len);
         return new Cone(apex, unit, halfAngleRad, rangeMeters, Math.cos(halfAngleRad), Math.tan(halfAngleRad));
      }
   }
}
