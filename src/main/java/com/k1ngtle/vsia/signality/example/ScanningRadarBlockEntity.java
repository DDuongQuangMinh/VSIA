package com.k1ngtle.vsia.signality.example;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.api.radar.RadarBand;
import com.k1ngtle.vsia.signality.api.radar.RadarMode;
import com.k1ngtle.vsia.signality.api.radar.RadarProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class ScanningRadarBlockEntity extends AbstractSweepRadarBlockEntity {
   public static final RadarProfile PROFILE = RadarProfile.builder()
      .peakPowerWatts(40000.0)
      .antennaGainDb(33.0)
      .band(RadarBand.S)
      .systemLossLinear(2.0)
      .noisePowerWatts(1.0E-13)
      .minDetectableSnrDb(10.0)
      .halfBeamWidthDeg(4.0)
      .maxRangeMeters(60000.0)
      .pulseRepetitionHz(10.0)
      .dopplerGateMps(30.0)
      .build();
   private static final int SWEEP_PERIOD_TICKS = 120;
   private static final double MAX_OFFSET_RAD = Math.toRadians(60.0);

   public ScanningRadarBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType<?>)SignalityBlocks.SCANNING_RADAR_BE.get(), pos, state);
   }

   @Override
   public RadarProfile profile() {
      return PROFILE;
   }

   @Override
   public RadarMode mode() {
      return RadarMode.TRACK_WHILE_SCAN;
   }

   @Override
   protected double azimuthOffsetRad(double gameTime) {
      double phase = gameTime % 120.0 / 120.0;
      return Math.sin(phase * 2.0 * Math.PI) * MAX_OFFSET_RAD;
   }
}
