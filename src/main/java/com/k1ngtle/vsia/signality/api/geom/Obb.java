package com.k1ngtle.vsia.signality.api.geom;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

public record Obb(Vec3 centerWorld, Vec3 halfExtents, Quaterniondc rotation) {
   public double enclosingRadius() {
      double hx = this.halfExtents.x;
      double hy = this.halfExtents.y;
      double hz = this.halfExtents.z;
      return Math.sqrt(hx * hx + hy * hy + hz * hz);
   }

   public Vec3[] corners() {
      double hx = this.halfExtents.x;
      double hy = this.halfExtents.y;
      double hz = this.halfExtents.z;
      Vec3[] out = new Vec3[8];
      Vector3d tmp = new Vector3d();

      for (int i = 0; i < 8; i++) {
         double sx = (i & 1) == 0 ? -hx : hx;
         double sy = (i & 2) == 0 ? -hy : hy;
         double sz = (i & 4) == 0 ? -hz : hz;
         tmp.set(sx, sy, sz);
         this.rotation.transform(tmp);
         out[i] = new Vec3(this.centerWorld.x + tmp.x, this.centerWorld.y + tmp.y, this.centerWorld.z + tmp.z);
      }

      return out;
   }
}
