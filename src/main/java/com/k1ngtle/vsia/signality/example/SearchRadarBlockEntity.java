package com.k1ngtle.vsia.signality.example;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.api.radar.RadarBand;
import com.k1ngtle.vsia.signality.api.radar.RadarMode;
import com.k1ngtle.vsia.signality.api.radar.RadarProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class SearchRadarBlockEntity extends AbstractSweepRadarBlockEntity {
   public static final RadarProfile PROFILE = RadarProfile.builder()
      .peakPowerWatts(100000.0)
      .antennaGainDb(30.0)
      .band(RadarBand.L)
      .systemLossLinear(2.5)
      .noisePowerWatts(1.0E-13)
      .minDetectableSnrDb(8.0)
      .halfBeamWidthDeg(15.0)
      .maxRangeMeters(40000.0)
      .pulseRepetitionHz(6.0)
      .dopplerGateMps(50.0)
      .build();
   private static final int ROTATION_PERIOD_TICKS = 120;

   public SearchRadarBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType<?>)SignalityBlocks.SEARCH_RADAR_BE.get(), pos, state);
   }

   @Override
   public RadarProfile profile() {
      return PROFILE;
   }

   @Override
   public RadarMode mode() {
      return RadarMode.SEARCH;
   }

   @Override
   protected double azimuthOffsetRad(double gameTime) {
      double phase = gameTime % 120.0 / 120.0;
      return phase * 2.0 * Math.PI;
   }

   @Override
   protected double pitchRad() {
      return Math.toRadians(8.0);
   }
}
