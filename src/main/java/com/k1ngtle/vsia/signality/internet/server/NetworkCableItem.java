package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Placeholder cable-link tool. A later milestone can replace it with rendered cable blocks. */
public final class NetworkCableItem extends Item {
    private static final String LINK_TAG = "VsiaCableLink";

    public NetworkCableItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null)
            return InteractionResult.PASS;

        BlockPos rackPos = baseRackPosition(level, context.getClickedPos());
        if (rackPos == null || !(level.getBlockEntity(rackPos) instanceof ServerRackBlockEntity rack))
            return InteractionResult.PASS;

        if (!(context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                || !rack.canConfigure(serverPlayer)) {
            context.getPlayer().displayClientMessage(Component.literal("Access denied. This rack is owned by "
                    + rack.ownerName()).withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        ItemStack stack = context.getItemInHand();
        CompoundTag root = stack.getOrCreateTag();

        if (context.getPlayer().isShiftKeyDown()) {
            for (BlockPos linkedPos : rack.cableLinks()) {
                if (level.getBlockEntity(linkedPos) instanceof ServerRackBlockEntity linked)
                    linked.disconnectCable(rackPos);
            }
            rack.clearCableLinks();
            root.remove(LINK_TAG);
            context.getPlayer().displayClientMessage(
                    Component.literal("All physical links removed from " + rack.displayName()).withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.CONSUME;
        }

        if (!root.contains(LINK_TAG)) {
            CompoundTag link = new CompoundTag();
            link.putLong("Position", rackPos.asLong());
            link.putString("Dimension", level.dimension().location().toString());
            root.put(LINK_TAG, link);
            context.getPlayer().displayClientMessage(
                    Component.literal("Cable start selected: " + rack.displayName()).withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.CONSUME;
        }

        CompoundTag link = root.getCompound(LINK_TAG);
        if (!level.dimension().location().toString().equals(link.getString("Dimension"))) {
            root.remove(LINK_TAG);
            context.getPlayer().displayClientMessage(Component.literal("Cable endpoints must be in the same dimension.")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        BlockPos firstPos = BlockPos.of(link.getLong("Position"));
        if (firstPos.equals(rackPos)) {
            root.remove(LINK_TAG);
            context.getPlayer().displayClientMessage(Component.literal("Cable selection cancelled.")
                    .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.CONSUME;
        }
        if (!(level.getBlockEntity(firstPos) instanceof ServerRackBlockEntity first)) {
            root.remove(LINK_TAG);
            context.getPlayer().displayClientMessage(Component.literal("The first rack is not currently loaded.")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (!first.canConfigure(serverPlayer)) {
            root.remove(LINK_TAG);
            context.getPlayer().displayClientMessage(Component.literal("Access denied to the first rack owned by "
                    + first.ownerName()).withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        first.connectCable(rackPos);
        rack.connectCable(firstPos);
        root.remove(LINK_TAG);
        context.getPlayer().displayClientMessage(Component.literal("Physical link created: " + first.displayName()
                + " <-> " + rack.displayName()).withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.CONSUME;
    }

    private static BlockPos baseRackPosition(ServerLevel level, BlockPos clicked) {
        BlockState state = level.getBlockState(clicked);
        if (!(state.getBlock() instanceof ServerRackBlock)) return null;
        return state.getValue(ServerRackBlock.HALF) == DoubleBlockHalf.UPPER ? clicked.below() : clicked;
    }
}
