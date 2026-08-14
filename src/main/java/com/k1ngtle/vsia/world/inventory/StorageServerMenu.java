package com.k1ngtle.vsia.world.inventory;

import com.k1ngtle.vsia.registry.ModMenuTypes;
import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.internet.server.StorageServerBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.StoredFile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;

public class StorageServerMenu extends AbstractContainerMenu {

    public final StorageServerBlockEntity blockEntity;
    private final Player player;
    private IItemHandler serverHandler;

    private int scrollOffsetRow = 0;
    public boolean slotsVisible = true;

    private final List<StoredFile> clientFiles = new ArrayList<>();

    public StorageServerMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public StorageServerMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.STORAGE_SERVER_MENU.get(), containerId);
        this.blockEntity = (StorageServerBlockEntity) entity;
        this.player = inv.player;

        if (this.blockEntity != null) {
            this.clientFiles.addAll(this.blockEntity.getStoredFiles());

            this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                this.serverHandler = handler;
                for (int row = 0; row < 6; row++) {
                    for (int col = 0; col < 9; col++) {
                        this.addSlot(new ScrollingSlotItemHandler(handler, col + row * 9, 8 + col * 18, 21 + row * 18, row, col));
                    }
                }
            });
        }

        // Player Main Inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new VisibilityAwareSlot(player.getInventory(), col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }

        // Player Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new VisibilityAwareSlot(player.getInventory(), col, 8 + col * 18, 198));
        }
    }

    public List<StoredFile> getFiles() {
        return clientFiles;
    }

    public void addFileClient(String name, String language, String content) {
        StoredFile newFile = new StoredFile(name, language, content);
        this.clientFiles.add(newFile);

        // In a complete implementation, you'd send a packet to the server here:
        // ModMessages.sendToServer(new AddFilePacket(name, language, content, blockEntity.getBlockPos()));
    }

    public void removeFileClient(StoredFile file) {
        this.clientFiles.remove(file);

        // In a complete implementation, you'd send a packet to the server here:
        // ModMessages.sendToServer(new RemoveFilePacket(file.getName(), blockEntity.getBlockPos()));
    }

    private class VisibilityAwareSlot extends Slot {
        public VisibilityAwareSlot(net.minecraft.world.Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean isActive() {
            return StorageServerMenu.this.slotsVisible && super.isActive();
        }
    }

    public void scrollTo(float scrollPercentage) {
        int maxRows = 40 - 6;
        int rowOffset = (int) (scrollPercentage * (float) maxRows + 0.5F);
        if (rowOffset < 0) rowOffset = 0;
        if (rowOffset > maxRows) rowOffset = maxRows;

        if (rowOffset != this.scrollOffsetRow) {
            this.scrollOffsetRow = rowOffset;
            this.broadcastChanges();
        }
    }

    public int getScrollOffsetRow() {
        return this.scrollOffsetRow;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < 54) {
                if (!this.moveItemStackTo(itemstack1, 54, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!mergeIntoServerSlots(itemstack1)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    private boolean mergeIntoServerSlots(ItemStack stack) {
        boolean changed = false;

        for (int i = 0; i < 54 && !stack.isEmpty(); i++) {
            Slot slot = this.slots.get(i);
            ItemStack existing = slot.getItem();
            if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, stack)) {
                continue;
            }

            int maxSize = slot.getMaxStackSize(stack);
            if (existing.getCount() >= maxSize) {
                continue;
            }

            int toAdd = Math.min(maxSize - existing.getCount(), stack.getCount());
            existing.grow(toAdd);
            stack.shrink(toAdd);
            slot.setChanged();
            changed = true;
        }

        for (int i = 0; i < 54 && !stack.isEmpty(); i++) {
            Slot slot = this.slots.get(i);
            if (slot.hasItem() || !slot.mayPlace(stack)) {
                continue;
            }

            int maxSize = slot.getMaxStackSize(stack);
            int toPlace = Math.min(stack.getCount(), maxSize);
            ItemStack placed = stack.copy();
            placed.setCount(toPlace);
            slot.set(placed);
            stack.shrink(toPlace);
            changed = true;
        }

        return changed;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, SignalityBlocks.STORAGE_SERVER.get());
    }

    public class ScrollingSlotItemHandler extends SlotItemHandler {
        private final int gridRow;
        private final int gridCol;

        public ScrollingSlotItemHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition, int gridRow, int gridCol) {
            super(itemHandler, index, xPosition, yPosition);
            this.gridRow = gridRow;
            this.gridCol = gridCol;
        }

        @Override
        public int getSlotIndex() {
            return gridCol + ((gridRow + StorageServerMenu.this.scrollOffsetRow) * 9);
        }

        @Override
        public boolean isActive() {
            return StorageServerMenu.this.slotsVisible && super.isActive();
        }

        @Override
        public ItemStack getItem() {
            return this.getItemHandler().getStackInSlot(getSlotIndex());
        }

        @Override
        public void set(ItemStack stack) {
            ((net.minecraftforge.items.ItemStackHandler)this.getItemHandler()).setStackInSlot(getSlotIndex(), stack);
            this.setChanged();
        }

        @Override
        public void setChanged() {
        }

        @Override
        public int getMaxStackSize() {
            return this.getItemHandler().getSlotLimit(getSlotIndex());
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            IItemHandler handler = this.getItemHandler();
            int slotIndex = getSlotIndex();
            int slotLimit = handler.getSlotLimit(slotIndex);

            ItemStack maxAdd = stack.copy();
            maxAdd.setCount(slotLimit);

            ItemStack currentStack = handler.getStackInSlot(slotIndex);
            if (handler instanceof net.minecraftforge.items.IItemHandlerModifiable) {
                ItemStack remainder = handler.insertItem(slotIndex, maxAdd, true);

                int current = currentStack.isEmpty() ? 0 : currentStack.getCount();
                int added = slotLimit - remainder.getCount();
                return current + added;
            }
            return super.getMaxStackSize(stack);
        }

        @Override
        public ItemStack remove(int amount) {
            return this.getItemHandler().extractItem(getSlotIndex(), amount, false);
        }
    }
}