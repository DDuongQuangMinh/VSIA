package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.Vsia;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side protection against breaking racks owned by another player. */
@Mod.EventBusSubscriber(modid = Vsia.MOD_ID)
public final class ServerRackSecurityEvents {
    private ServerRackSecurityEvents() {}

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof ServerRackBlock)) return;

        BlockPos clicked = event.getPos();
        BlockPos base = state.getValue(ServerRackBlock.HALF) == DoubleBlockHalf.UPPER
                ? clicked.below() : clicked;
        if (player.level().getBlockEntity(base) instanceof ServerRackBlockEntity rack
                && !rack.canConfigure(player)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal(
                    "You cannot break this server rack. Owner: " + rack.ownerName()), true);
        }
    }
}
