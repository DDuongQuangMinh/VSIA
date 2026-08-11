package com.k1ngtle.vsia.signality.api.rcs;

import com.k1ngtle.vsia.signality.api.radar.IRadarTarget;
import net.minecraft.world.phys.Vec3;

public final class DefaultRcsProviders {
   public static final IRcsProvider HEURISTIC = (target, emitter, aspect) -> {
      double r = Math.max(0.25, target.boundingRadius());
      double baseRcs = Math.PI * r * r;
      double aspectMod = DefaultRcsProviders.AspectAngle.broadsideWeight(aspect);
      double waveMod = wavelengthRegimeFactor(r, emitter.profile().wavelengthMeters());
      return baseRcs * aspectMod * waveMod;
   };
   public static final IRcsProvider TARGET_DELEGATE = (target, emitter, aspect) -> {
      double v = target.radarCrossSection(aspect, emitter.profile().wavelengthMeters());
      return !(v <= 0.0) && !Double.isNaN(v) && Double.isFinite(v) ? v : Double.NaN;
   };

   private DefaultRcsProviders() {
   }

   public static double wavelengthRegimeFactor(double radiusM, double wavelengthM) {
      double ratio = radiusM / wavelengthM;
      if (ratio > 10.0) {
         return 1.0;
      } else if (ratio < 0.1) {
         return Math.pow(ratio * 10.0, 4.0) * 1.0E-4;
      } else {
         double t = (ratio - 0.1) / 9.9;
         return 1.0E-4 + 0.9999 * smoothStep(t);
      }
   }

   private static double smoothStep(double t) {
      t = Math.max(0.0, Math.min(1.0, t));
      return t * t * (3.0 - 2.0 * t);
   }

   public static final class AspectAngle {
      private AspectAngle() {
      }

      public static double of(Vec3 emitterPos, IRadarTarget target) {
         Vec3 vel = target.velocityWorld();
         double speedSq = vel.lengthSqr();
         if (speedSq < 1.0E-6) {
            return Math.PI / 2;
         } else {
            Vec3 toTarget = target.positionWorld().subtract(emitterPos);
            double tLen = toTarget.length();
            if (tLen < 1.0E-6) {
               return 0.0;
            } else {
               double dot = (toTarget.x * vel.x + toTarget.y * vel.y + toTarget.z * vel.z)
                  / (tLen * Math.sqrt(speedSq));
               return Math.acos(Math.max(-1.0, Math.min(1.0, -dot)));
            }
         }
      }

      public static double broadsideWeight(double aspectRad) {
         return 0.3 + 0.7 * Math.abs(Math.sin(aspectRad));
      }
   }
}
