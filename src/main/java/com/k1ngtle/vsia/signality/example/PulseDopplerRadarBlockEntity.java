package com.k1ngtle.vsia.signality.example;

import com.k1ngtle.vsia.signality.Signality;
import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.api.radar.IRadarEmitter;
import com.k1ngtle.vsia.signality.api.radar.RadarBand;
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

public final class PulseDopplerRadarBlockEntity extends BlockEntity implements IRadarEmitter {
   public static final RadarProfile DEFAULT_PROFILE = RadarProfile.builder()
      .peakPowerWatts(50000.0)
      .antennaGainDb(35.0)
      .band(RadarBand.X)
      .systemLossLinear(2.0)
      .noisePowerWatts(1.0E-13)
      .minDetectableSnrDb(10.0)
      .halfBeamWidthDeg(2.5)
      .maxRangeMeters(80000.0)
      .pulseRepetitionHz(5.0)
      .dopplerGateMps(30.0)
      .build();
   private static final double DEFAULT_PITCH_RAD = Math.toRadians(5.0);
   private UUID emitterId = UUID.randomUUID();
   private RadarProfile profile = DEFAULT_PROFILE;
   private RadarMode mode = RadarMode.SEARCH;
   private double pitchRad = DEFAULT_PITCH_RAD;
   private List<RadarContact> lastContacts = List.of();
   @Nullable
   private Object cachedVsShip;
   private long cachedVsShipTick = Long.MIN_VALUE;

   public PulseDopplerRadarBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)SignalityBlocks.PULSE_DOPPLER_RADAR_BE.get(), pos, state);
   }

   public void onLoad() {
      super.onLoad();
      if (this.level != null && !this.level.isClientSide) {
         RadarRegistry.registerEmitter(this);
      }
   }

   public void setRemoved() {
      if (this.emitterId != null) {
         RadarRegistry.unregisterEmitter(this.emitterId);
      }

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
      Direction facing = (Direction)this.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
      double cosPitch = Math.cos(this.pitchRad);
      double sinPitch = Math.sin(this.pitchRad);
      Vec3 local = new Vec3((double)facing.getStepX() * cosPitch, sinPitch, (double)facing.getStepZ() * cosPitch);
      return this.cachedVsShip != null ? VsCompat.hook().transformDirShipToWorld(this.cachedVsShip, local).normalize() : local;
   }

   @Override
   public Vec3 velocityWorld() {
      return this.cachedVsShip != null ? VsCompat.hook().shipVelocity(this.cachedVsShip) : Vec3.ZERO;
   }

   @Override
   public RadarProfile profile() {
      return this.profile;
   }

   @Override
   public RadarMode mode() {
      return this.mode;
   }

   @Override
   public Object vsShip() {
      return this.cachedVsShip;
   }

   @Override
   public void onContacts(List<RadarContact> contacts) {
      this.lastContacts = contacts;
      if (Signality.LOGGER.isDebugEnabled() && !contacts.isEmpty()) {
         Signality.LOGGER.debug("Radar {} at {} produced {} contacts", new Object[]{this.emitterId, this.worldPosition, contacts.size()});
      }
   }

   public List<RadarContact> lastContacts() {
      return Collections.unmodifiableList(this.lastContacts);
   }

   public void setMode(RadarMode mode) {
      this.mode = Objects.requireNonNull(mode);
      this.setChanged();
   }

   public void setProfile(RadarProfile profile) {
      this.profile = Objects.requireNonNull(profile);
      this.setChanged();
   }

   public void setPitchRad(double pitchRad) {
      this.pitchRad = pitchRad;
      this.setChanged();
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      if (this.emitterId != null) {
         tag.putUUID("EmitterId", this.emitterId);
      }

      tag.putString("Mode", this.mode.name());
      tag.putDouble("PitchRad", this.pitchRad);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      if (tag.hasUUID("EmitterId")) {
         this.emitterId = tag.getUUID("EmitterId");
      }

      if (tag.contains("Mode")) {
         try {
            this.mode = RadarMode.valueOf(tag.getString("Mode"));
         } catch (IllegalArgumentException var3) {
            this.mode = RadarMode.SEARCH;
         }
      }

      if (tag.contains("PitchRad")) {
         this.pitchRad = tag.getDouble("PitchRad");
      }
   }
}
