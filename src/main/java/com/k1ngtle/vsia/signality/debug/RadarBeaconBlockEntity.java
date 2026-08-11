package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.api.radar.IRadarTarget;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class RadarBeaconBlockEntity extends BlockEntity implements IRadarTarget {
   public static final double[] RCS_PRESETS = new double[]{0.1, 1.0, 10.0, 100.0, 1000.0};
   private static final Set<RadarBeaconBlockEntity> ACTIVE = ConcurrentHashMap.newKeySet();
   private UUID id = UUID.randomUUID();
   private int rcsIndex = 1;

   public RadarBeaconBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)SignalityBlocks.RADAR_BEACON_BE.get(), pos, state);
   }

   public static Stream<IRadarTarget> beaconsIn(ServerLevel level) {
      return ACTIVE.stream().filter(b -> b.level == level && !b.isRemoved()).map(b -> (IRadarTarget)b);
   }

   public void onLoad() {
      super.onLoad();
      if (this.level != null && !this.level.isClientSide) {
         ACTIVE.add(this);
      }
   }

   public void setRemoved() {
      ACTIVE.remove(this);
      super.setRemoved();
   }

   public double rcs() {
      return RCS_PRESETS[this.rcsIndex];
   }

   public double cycleRcs() {
      this.rcsIndex = (this.rcsIndex + 1) % RCS_PRESETS.length;
      this.setChanged();
      return this.rcs();
   }

   @Override
   public UUID id() {
      return this.id;
   }

   @Override
   public ServerLevel level() {
      return (ServerLevel)this.level;
   }

   @Override
   public Vec3 positionWorld() {
      return Vec3.atCenterOf(this.worldPosition);
   }

   @Override
   public Vec3 velocityWorld() {
      return Vec3.ZERO;
   }

   @Override
   public double boundingRadius() {
      return 0.75;
   }

   @Override
   public double radarCrossSection(double aspectAngleRad, double wavelengthMeters) {
      return this.rcs();
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putUUID("Id", this.id);
      tag.putInt("RcsIndex", this.rcsIndex);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      if (tag.hasUUID("Id")) {
         this.id = tag.getUUID("Id");
      }

      if (tag.contains("RcsIndex")) {
         int v = tag.getInt("RcsIndex");
         if (v >= 0 && v < RCS_PRESETS.length) {
            this.rcsIndex = v;
         }
      }
   }
}
