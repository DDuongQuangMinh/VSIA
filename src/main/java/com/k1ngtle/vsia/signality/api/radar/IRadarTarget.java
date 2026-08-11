package com.k1ngtle.vsia.signality.api.radar;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface IRadarTarget {
   UUID id();

   ServerLevel level();

   Vec3 positionWorld();

   Vec3 velocityWorld();

   double boundingRadius();

   double radarCrossSection(double var1, double var3);

   @Nullable
   default Object vsShip() {
      return null;
   }

   default boolean detectable() {
      return true;
   }
}
