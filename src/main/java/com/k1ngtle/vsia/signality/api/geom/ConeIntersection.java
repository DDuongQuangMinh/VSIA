package com.k1ngtle.vsia.signality.api.geom;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ConeIntersection {
   private ConeIntersection() {
   }

   public static boolean containsSphere(Cone cone, Vec3 sphereCenter, double radius) {
      double dx = sphereCenter.x - cone.apex().x;
      double dy = sphereCenter.y - cone.apex().y;
      double dz = sphereCenter.z - cone.apex().z;
      Vec3 n = cone.axisUnit();
      double along = dx * n.x + dy * n.y + dz * n.z;
      if (along < -radius) {
         return false;
      } else if (along > cone.rangeMeters() + radius) {
         return false;
      } else {
         double px = dx - along * n.x;
         double py = dy - along * n.y;
         double pz = dz - along * n.z;
         double perp = Math.sqrt(px * px + py * py + pz * pz);
         double clampedAlong = Math.max(0.0, along);
         double allowable = clampedAlong * cone.tanHalfAngle() + radius / cone.cosHalfAngle();
         return perp <= allowable;
      }
   }

   public static boolean intersectsAabb(Cone cone, AABB aabb) {
      Vec3 center = aabb.getCenter();
      double radius = 0.5 * Math.sqrt(square(aabb.getXsize()) + square(aabb.getYsize()) + square(aabb.getZsize()));
      return containsSphere(cone, center, radius);
   }

   public static boolean intersectsObb(Cone cone, Obb obb) {
      if (!containsSphere(cone, obb.centerWorld(), obb.enclosingRadius())) {
         return false;
      } else {
         for (Vec3 corner : obb.corners()) {
            if (containsSphere(cone, corner, 0.0)) {
               return true;
            }
         }

         return containsSphere(cone, obb.centerWorld(), 0.0);
      }
   }

   private static double square(double v) {
      return v * v;
   }
}
