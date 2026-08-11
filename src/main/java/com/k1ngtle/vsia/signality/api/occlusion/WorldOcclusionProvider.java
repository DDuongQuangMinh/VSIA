package com.k1ngtle.vsia.signality.api.occlusion;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class WorldOcclusionProvider implements IOcclusionProvider {
   public static final WorldOcclusionProvider INSTANCE = new WorldOcclusionProvider();

   private WorldOcclusionProvider() {
   }

   @Override
   public boolean isOccluded(ServerLevel level, Vec3 from, Vec3 to) {
      ClipContext ctx = new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, (Entity)null);
      HitResult hit = level.clip(ctx);
      return hit.getType() == Type.BLOCK;
   }

   @Override
   public boolean threadSafe() {
      return false;
   }
}
