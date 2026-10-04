package com.k1ngtle.vsia.item;

import com.k1ngtle.vsia.cockpit.program.DisplayProgrammingServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Place on a surface; right-click air with a drive in the other hand retains handheld editing. */
public final class DisplayLaptopItem extends BlockItem {
    public DisplayLaptopItem(Properties properties){super(com.k1ngtle.vsia.cockpit.program.DisplayLaptopRegistry.LAPTOP.get(),properties);}
    // Preserve the existing item's translation instead of switching to an untranslated block key.
    @Override public String getDescriptionId(){return "item.vsia.display_laptop";}
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> tooltip,net.minecraft.world.item.TooltipFlag flag){
        tooltip.add(Component.literal("Place on a surface, then right-click with a display drive to insert it."));
        tooltip.add(Component.literal("Right-click: edit. Sneak + empty-hand right-click: eject drive."));
        super.appendHoverText(stack,level,tooltip,flag);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(player instanceof ServerPlayer server) {
            InteractionHand other=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
            if(DisplayHardDriveItem.isDisplayDrive(player.getItemInHand(other)))DisplayProgrammingServer.open(server,other);
            else player.displayClientMessage(Component.literal("Right-click a surface to place the laptop, or hold a display drive in the other hand to edit while carrying it."),true);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
