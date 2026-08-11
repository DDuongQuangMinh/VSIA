package com.k1ngtle.vsia.signality.api.occlusion;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public interface IOcclusionProvider {
   boolean isOccluded(ServerLevel var1, Vec3 var2, Vec3 var3);

   default boolean threadSafe() {
      return false;
   }
}
