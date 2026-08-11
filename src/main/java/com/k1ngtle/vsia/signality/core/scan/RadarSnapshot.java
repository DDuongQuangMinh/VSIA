package com.k1ngtle.vsia.signality.core.scan;

import com.k1ngtle.vsia.signality.api.geom.Obb;
import com.k1ngtle.vsia.signality.api.radar.IRadarEmitter;
import com.k1ngtle.vsia.signality.api.radar.IRadarTarget;
import com.k1ngtle.vsia.signality.api.radar.RadarMode;
import com.k1ngtle.vsia.signality.api.radar.RadarProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class RadarSnapshot {
   private final ServerLevel level;
   private final long gameTime;
   private final List<RadarSnapshot.EmitterSnap> emitters;
   private final List<RadarSnapshot.TargetSnap> targets;

   public RadarSnapshot(ServerLevel level, long gameTime, List<RadarSnapshot.EmitterSnap> emitters, List<RadarSnapshot.TargetSnap> targets) {
      this.level = level;
      this.gameTime = gameTime;
      this.emitters = List.copyOf(emitters);
      this.targets = List.copyOf(targets);
   }

   public ServerLevel level() {
      return this.level;
   }

   public long gameTime() {
      return this.gameTime;
   }

   public List<RadarSnapshot.EmitterSnap> emitters() {
      return this.emitters;
   }

   public List<RadarSnapshot.TargetSnap> targets() {
      return this.targets;
   }

   public static record EmitterSnap(
      UUID id,
      ServerLevel level,
      Vec3 originWorld,
      Vec3 axisWorld,
      Vec3 velocityWorld,
      RadarProfile profile,
      RadarMode mode,
      @Nullable Object vsShipHandle,
      IRadarEmitter liveRef
   ) {
   }

   public static record TargetSnap(
      UUID id, Vec3 positionWorld, Vec3 velocityWorld, double boundingRadius, @Nullable Obb obb, @Nullable Object vsShipHandle, IRadarTarget liveRef
   ) {
   }
}
