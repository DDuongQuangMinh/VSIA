package com.k1ngtle.vsia.world.inventory;

import com.k1ngtle.vsia.registry.ModMenuTypes; // Ensure NETWORK_SWITCH_MENU is registered here
import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class NetworkSwitchMenu extends AbstractContainerMenu {

    public final NetworkSwitchBlockEntity blockEntity;
    private final Player player;

    public NetworkSwitchMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public NetworkSwitchMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.NETWORK_SWITCH_MENU.get(), containerId);
        this.blockEntity = (NetworkSwitchBlockEntity) entity;
        this.player = inv.player;

        // This menu doesn't strictly need slots unless you want a UI to insert upgrades/modules
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, com.k1ngtle.vsia.signality.SignalityBlocks.NETWORK_SWITCH.get());
    }
}