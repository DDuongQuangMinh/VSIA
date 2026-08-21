package com.k1ngtle.vsia.world.inventory;

import com.k1ngtle.vsia.registry.ModMenuTypes;
import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.internet.router.RtAc68uRouterBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class RtAc68uRouterMenu extends AbstractContainerMenu {
    public final RtAc68uRouterBlockEntity blockEntity;

    public RtAc68uRouterMenu(int containerId, Inventory inv, FriendlyByteBuf data) {
        this(containerId, inv, inv.player.level().getBlockEntity(data.readBlockPos()));
    }

    public RtAc68uRouterMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.RT_AC68U_ROUTER_MENU.get(), containerId);
        if (!(entity instanceof RtAc68uRouterBlockEntity router)) {
            throw new IllegalStateException("RT-AC68U router block entity missing");
        }
        this.blockEntity = router;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player,
                SignalityBlocks.RT_AC68U_ROUTER.get()
        );
    }
}
