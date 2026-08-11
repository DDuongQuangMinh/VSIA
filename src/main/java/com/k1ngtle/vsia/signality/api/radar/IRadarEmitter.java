package com.k1ngtle.vsia.signality.api.radar;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface IRadarEmitter {
   UUID id();

   ServerLevel level();

   Vec3 originWorld();

   Vec3 axisWorld();

   default Vec3 axisWorldAt(double subTickFraction) {
      return this.axisWorld();
   }

   default Vec3 velocityWorld() {
      return Vec3.ZERO;
   }

   RadarProfile profile();

   default RadarMode mode() {
      return RadarMode.SEARCH;
   }

   void onContacts(List<RadarContact> var1);

   @Nullable
   default Object vsShip() {
      return null;
   }

   default boolean shouldScan() {
      return true;
   }
}
