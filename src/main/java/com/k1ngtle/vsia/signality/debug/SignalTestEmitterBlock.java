package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class SignalTestEmitterBlock extends Block implements EntityBlock {
   public SignalTestEmitterBlock(Properties properties) {
      super(properties);
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new SignalTestEmitterBlockEntity(pos, state);
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!level.isClientSide && level.getBlockEntity(pos) instanceof SignalTestEmitterBlockEntity be) {
         SignalTestEmitterBlockEntity.Channel ch = be.cycleChannel();
         player.displayClientMessage(Component.literal("Emitter channel: " + ch.label() + "  (" + ch.frequencyHz() / 1000000.0 + " MHz, " + ch.powerW() + " W)"), true);
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return !level.isClientSide && type == SignalityBlocks.SIGNAL_TEST_EMITTER_BE.get() ? (lvl, pos, st, be) -> {
         if (be instanceof SignalTestEmitterBlockEntity emitter) {
            emitter.serverTick();
         }
      } : null;
   }
}
