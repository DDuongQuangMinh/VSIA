package com.k1ngtle.vsia.signality.example;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import org.jetbrains.annotations.Nullable;

public final class ScanningRadarBlock extends AbstractRadarBlock {
   public ScanningRadarBlock(Properties properties) {
      super(properties, SignalityBlocks.SCANNING_RADAR_BE);
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new ScanningRadarBlockEntity(pos, state);
   }
}
