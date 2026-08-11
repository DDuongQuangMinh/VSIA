package com.k1ngtle.vsia.signality.example;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.api.radar.RadarBand;
import com.k1ngtle.vsia.signality.api.radar.RadarContact;
import com.k1ngtle.vsia.signality.api.radar.RadarMode;
import com.k1ngtle.vsia.signality.api.radar.RadarProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class TrackingRadarBlockEntity extends AbstractSweepRadarBlockEntity {
   public static final RadarProfile ACQ_PROFILE = RadarProfile.builder()
      .peakPowerWatts(100000.0)
      .antennaGainDb(35.0)
      .band(RadarBand.X)
      .systemLossLinear(2.0)
      .noisePowerWatts(1.0E-13)
      .minDetectableSnrDb(10.0)
      .halfBeamWidthDeg(5.0)
      .maxRangeMeters(80000.0)
      .pulseRepetitionHz(5.0)
      .dopplerGateMps(25.0)
      .build();
   public static final RadarProfile LOCK_PROFILE = RadarProfile.builder()
      .peakPowerWatts(100000.0)
      .antennaGainDb(40.0)
      .band(RadarBand.X)
      .systemLossLinear(2.0)
      .noisePowerWatts(1.0E-13)
      .minDetectableSnrDb(13.0)
      .halfBeamWidthDeg(4.0)
      .maxRangeMeters(120000.0)
      .pulseRepetitionHz(20.0)
      .dopplerGateMps(20.0)
      .build();
   private static final int LOCK_TIMEOUT_TICKS = 20;
   private static final double ACQ_SWEEP_HALF_ARC_RAD = Math.toRadians(30.0);
   private static final int ACQ_SWEEP_PERIOD_TICKS = 80;
   @Nullable
   private UUID lockedTargetId;
   @Nullable
   private Vec3 lockedTargetLastPos;
   private int ticksSinceLockUpdate = 0;

   public TrackingRadarBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType<?>)SignalityBlocks.TRACKING_RADAR_BE.get(), pos, state);
   }

   public boolean isLocked() {
      return this.lockedTargetId != null;
   }

   @Nullable
   public UUID lockedTargetId() {
      return this.lockedTargetId;
   }

   @Nullable
   public Vec3 lockedTargetPosition() {
      return this.lockedTargetLastPos;
   }

   @Override
   public RadarProfile profile() {
      return this.isLocked() ? LOCK_PROFILE : ACQ_PROFILE;
   }

   @Override
   public RadarMode mode() {
      return this.isLocked() ? RadarMode.SINGLE_TARGET_TRACK : RadarMode.SEARCH;
   }

   @Override
   public Vec3 axisWorld() {
      if (this.lockedTargetLastPos != null) {
         Vec3 origin = this.originWorld();
         Vec3 toTarget = this.lockedTargetLastPos.subtract(origin);
         double len = toTarget.length();
         if (len > 1.0E-6) {
            return toTarget.scale(1.0 / len);
         }
      }

      return super.axisWorld();
   }

   @Override
   protected double azimuthOffsetRad(double gameTime) {
      if (this.isLocked()) {
         return 0.0;
      } else {
         double phase = gameTime % 80.0 / 80.0;
         return Math.sin(phase * 2.0 * Math.PI) * ACQ_SWEEP_HALF_ARC_RAD;
      }
   }

   @Override
   public void onContacts(List<RadarContact> contacts) {
      super.onContacts(contacts);
      if (this.isLocked()) {
         RadarContact match = this.findLocked(contacts);
         if (match != null) {
            this.lockedTargetLastPos = match.positionWorld();
            this.ticksSinceLockUpdate = 0;
         } else {
            this.ticksSinceLockUpdate = this.ticksSinceLockUpdate + Math.max(1, this.profile().ticksPerScan());
            if (this.ticksSinceLockUpdate > 20) {
               this.dropLock();
            }
         }
      } else if (!contacts.isEmpty()) {
         RadarContact acquired = this.pickAcquisitionTarget(contacts);
         if (acquired != null) {
            this.lockedTargetId = acquired.targetId();
            this.lockedTargetLastPos = acquired.positionWorld();
            this.ticksSinceLockUpdate = 0;
         }
      }
   }

   @Nullable
   private RadarContact findLocked(List<RadarContact> contacts) {
      for (RadarContact c : contacts) {
         if (c.targetId().equals(this.lockedTargetId)) {
            return c;
         }
      }

      return null;
   }

   @Nullable
   protected RadarContact pickAcquisitionTarget(List<RadarContact> contacts) {
      RadarContact best = null;
      double bestSnr = Double.NEGATIVE_INFINITY;

      for (RadarContact c : contacts) {
         if (c.signalToNoiseRatio() > bestSnr) {
            bestSnr = c.signalToNoiseRatio();
            best = c;
         }
      }

      return best;
   }

   private void dropLock() {
      this.lockedTargetId = null;
      this.lockedTargetLastPos = null;
      this.ticksSinceLockUpdate = 0;
   }
}
