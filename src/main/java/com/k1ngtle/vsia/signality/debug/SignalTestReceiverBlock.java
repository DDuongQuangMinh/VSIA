package com.k1ngtle.vsia.signality.debug;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class SignalTestReceiverBlock extends Block implements EntityBlock {
   public SignalTestReceiverBlock(Properties properties) {
      super(properties);
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new SignalTestReceiverBlockEntity(pos, state);
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!level.isClientSide && level.getBlockEntity(pos) instanceof SignalTestReceiverBlockEntity be) {
         if (player.isShiftKeyDown()) {
            boolean en = be.toggleChat();
            player.displayClientMessage(Component.literal("Receiver chat: " + (en ? "on" : "off")), true);
         } else {
            SignalTestEmitterBlockEntity.Channel ch = be.cycleChannel();
            player.displayClientMessage(Component.literal("Receiver tuned: " + ch.label() + "  (" + ch.frequencyHz() / 1000000.0 + " MHz)"), true);
         }
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }
}
