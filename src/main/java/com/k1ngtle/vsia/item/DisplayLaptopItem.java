package com.k1ngtle.vsia.item;

import com.k1ngtle.vsia.cockpit.program.DisplayProgrammingServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Right-click opens the editor; nothing is erased or overwritten just by interacting. */
public final class DisplayLaptopItem extends Item {
    public DisplayLaptopItem(Properties properties){super(properties);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(player instanceof ServerPlayer server) {
            InteractionHand other=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
            if(DisplayHardDriveItem.isDisplayDrive(player.getItemInHand(other)))DisplayProgrammingServer.open(server,other);
            else player.displayClientMessage(Component.literal("Hold the display drive in the other hand, then use the laptop"),true);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
