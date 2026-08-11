package com.k1ngtle.vsia.signality.core.math;

import net.minecraft.world.phys.Vec3;

public final class BeamGeometry {
   private static final double SIDELOBE_LEVEL_LINEAR = 0.00316;

   private BeamGeometry() {
   }

   public static double offAxisGain(double onAxisGain, double offAxisRad, double halfBeamWidthRad) {
      if (offAxisRad < 0.0) {
         offAxisRad = -offAxisRad;
      }

      double t = offAxisRad / halfBeamWidthRad;
      if (t <= 1.0) {
         double cs = Math.cos((Math.PI / 2) * t);
         return onAxisGain * cs * cs;
      } else {
         double dt = t - 1.0;
         return onAxisGain * 0.00316 * Math.exp(-dt * dt);
      }
   }

   public static double closureRateMps(Vec3 emitterPos, Vec3 emitterVel, Vec3 targetPos, Vec3 targetVel) {
      Vec3 toTarget = targetPos.subtract(emitterPos);
      double range = toTarget.length();
      if (range < 1.0E-6) {
         return 0.0;
      } else {
         Vec3 los = toTarget.scale(1.0 / range);
         Vec3 rel = targetVel.subtract(emitterVel);
         return -(rel.x * los.x + rel.y * los.y + rel.z * los.z);
      }
   }

   public static double grazingAngle(Vec3 axisUnit) {
      return -Math.asin(Math.max(-1.0, Math.min(1.0, axisUnit.y)));
   }

   public static double angleBetween(Vec3 a, Vec3 b) {
      double dot = a.x * b.x + a.y * b.y + a.z * b.z;
      return Math.acos(Math.max(-1.0, Math.min(1.0, dot)));
   }
}
