package com.k1ngtle.vsia.signality.example;

import com.k1ngtle.vsia.signality.api.radar.IRadarEmitter;
import com.k1ngtle.vsia.signality.api.radar.RadarContact;
import com.k1ngtle.vsia.signality.api.radar.RadarMode;
import com.k1ngtle.vsia.signality.api.radar.RadarProfile;
import com.k1ngtle.vsia.signality.api.radar.RadarRegistry;
import com.k1ngtle.vsia.signality.integration.vs.VsCompat;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractSweepRadarBlockEntity extends BlockEntity implements IRadarEmitter {
   private UUID emitterId = UUID.randomUUID();
   private List<RadarContact> lastContacts = List.of();
   @Nullable
   private Object cachedVsShip;
   private long cachedVsShipTick = Long.MIN_VALUE;

   protected AbstractSweepRadarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public abstract RadarProfile profile();

   @Override
   public abstract RadarMode mode();

   protected abstract double azimuthOffsetRad(double var1);

   protected double pitchRad() {
      return Math.toRadians(3.0);
   }

   public void onLoad() {
      super.onLoad();
      if (this.level != null && !this.level.isClientSide) {
         RadarRegistry.registerEmitter(this);
      }
   }

   public void setRemoved() {
      RadarRegistry.unregisterEmitter(this.emitterId);
      super.setRemoved();
   }

   public void serverTick() {
      if (this.level != null) {
         long now = this.level.getGameTime();
         if (now != this.cachedVsShipTick) {
            this.cachedVsShipTick = now;
            this.cachedVsShip = VsCompat.isLoaded() ? VsCompat.hook().shipManagingPos((ServerLevel)this.level, this.worldPosition) : null;
         }
      }
   }

   @Override
   public UUID id() {
      return this.emitterId;
   }

   @Override
   public ServerLevel level() {
      return (ServerLevel)Objects.requireNonNull(this.level);
   }

   @Override
   public Vec3 originWorld() {
      Vec3 local = Vec3.atCenterOf(this.worldPosition).add(0.0, 0.5, 0.0);
      return this.cachedVsShip != null ? VsCompat.hook().transformShipToWorld(this.cachedVsShip, local) : local;
   }

   @Override
   public Vec3 axisWorld() {
      return this.computeAxisAt(this.level == null ? 0.0 : (double)this.level.getGameTime());
   }

   @Override
   public Vec3 axisWorldAt(double subTickFraction) {
      double t = (this.level == null ? 0.0 : (double)this.level.getGameTime()) + subTickFraction;
      return this.computeAxisAt(t);
   }

   private Vec3 computeAxisAt(double gameTime) {
      Direction facing = (Direction)this.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
      double facingAngle = Math.atan2((double)facing.getStepZ(), (double)facing.getStepX());
      double finalAngle = facingAngle + this.azimuthOffsetRad(gameTime);
      double pitch = this.pitchRad();
      double cosPitch = Math.cos(pitch);
      double sinPitch = Math.sin(pitch);
      Vec3 local = new Vec3(Math.cos(finalAngle) * cosPitch, sinPitch, Math.sin(finalAngle) * cosPitch);
      return this.cachedVsShip != null ? VsCompat.hook().transformDirShipToWorld(this.cachedVsShip, local).normalize() : local;
   }

   @Override
   public Vec3 velocityWorld() {
      return this.cachedVsShip != null ? VsCompat.hook().shipVelocity(this.cachedVsShip) : Vec3.ZERO;
   }

   @Override
   public Object vsShip() {
      return this.cachedVsShip;
   }

   @Override
   public void onContacts(List<RadarContact> contacts) {
      this.lastContacts = contacts;
   }

   public List<RadarContact> lastContacts() {
      return Collections.unmodifiableList(this.lastContacts);
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putUUID("EmitterId", this.emitterId);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      if (tag.hasUUID("EmitterId")) {
         this.emitterId = tag.getUUID("EmitterId");
      }
   }
}
