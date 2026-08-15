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
import net.minecraftforge.items.IItemHandlerModifiable;
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
    private final List<Integer> filteredSlots = new ArrayList<>();
    private String searchTerm = "";

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

                for(int i = 0; i < handler.getSlots(); i++) {
                    this.filteredSlots.add(i);
                }

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
    }

    public void removeFileClient(StoredFile file) {
        this.clientFiles.remove(file);
    }

    public void updateSearch(String term) {
        this.searchTerm = term == null ? "" : term.toLowerCase();
        this.filteredSlots.clear();
        List<Integer> emptySlots = new ArrayList<>();

        if (this.serverHandler != null) {
            for (int i = 0; i < this.serverHandler.getSlots(); i++) {
                ItemStack stack = this.serverHandler.getStackInSlot(i);
                if (stack.isEmpty()) {
                    emptySlots.add(i);
                } else if (stack.getHoverName().getString().toLowerCase().contains(this.searchTerm)) {
                    this.filteredSlots.add(i);
                }
            }
            this.filteredSlots.addAll(emptySlots);
        }
        this.scrollOffsetRow = 0;
        this.broadcastChanges();
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
        int maxRows = (this.filteredSlots.isEmpty() ? 40 : (this.filteredSlots.size() / 9)) - 6;
        if (maxRows < 0) maxRows = 0;
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
                // Stripping custom massive NBT tags so the player doesn't receive a bugged item
                ItemStack toMove = itemstack1.copy();
                int realCount = StorageServerBlockEntity.getRealCount(toMove);
                int amountToMove = Math.min(realCount, toMove.getMaxStackSize());

                toMove.setCount(amountToMove);
                if (toMove.hasTag()) {
                    toMove.getTag().remove("VSIA_Count");
                    if (toMove.getTag().isEmpty()) toMove.setTag(null);
                }

                if (!this.moveItemStackTo(toMove, 54, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }

                int moved = amountToMove - toMove.getCount();
                if (moved > 0) {
                    StorageServerBlockEntity.setRealCount(itemstack1, realCount - moved);
                    if (StorageServerBlockEntity.getRealCount(itemstack1) <= 0) {
                        slot.set(ItemStack.EMPTY);
                    } else {
                        slot.setChanged();
                    }
                }
                return ItemStack.EMPTY;
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
        if (serverHandler == null || !(serverHandler instanceof IItemHandlerModifiable)) return false;
        IItemHandlerModifiable modifiableHandler = (IItemHandlerModifiable) serverHandler;
        boolean changed = false;

        // Try merge into existing stacks
        for (int i = 0; i < serverHandler.getSlots() && !stack.isEmpty(); i++) {
            ItemStack existing = serverHandler.getStackInSlot(i);
            if (existing.isEmpty() || !StorageServerBlockEntity.canMergeItems(existing, stack)) {
                continue;
            }

            int maxSize = StorageServerBlockEntity.MAX_ITEM_CAPACITY;
            int currentCount = StorageServerBlockEntity.getRealCount(existing);
            if (currentCount >= maxSize) {
                continue;
            }

            int toAdd = Math.min(maxSize - currentCount, stack.getCount());
            StorageServerBlockEntity.setRealCount(existing, currentCount + toAdd);

            // Bypass slot.set() to avoid ClassCastExceptions, write directly to capability
            modifiableHandler.setStackInSlot(i, existing);
            stack.shrink(toAdd);
            changed = true;
        }

        // Try push into empty slots
        for (int i = 0; i < serverHandler.getSlots() && !stack.isEmpty(); i++) {
            ItemStack existing = serverHandler.getStackInSlot(i);
            if (!existing.isEmpty()) {
                continue;
            }

            int toPlace = Math.min(stack.getCount(), StorageServerBlockEntity.MAX_ITEM_CAPACITY);
            ItemStack placed = stack.copy();
            placed.setCount(1); // Set to 1 first to avoid vanilla processing bugs
            StorageServerBlockEntity.setRealCount(placed, toPlace);

            // Bypass slot.set() to avoid ClassCastExceptions, write directly to capability
            modifiableHandler.setStackInSlot(i, placed);
            stack.shrink(toPlace);
            changed = true;
        }

        if (changed) {
            this.broadcastChanges();
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
            int viewIndex = gridCol + ((gridRow + StorageServerMenu.this.scrollOffsetRow) * 9);
            if (viewIndex >= StorageServerMenu.this.filteredSlots.size()) {
                return 0;
            }
            return StorageServerMenu.this.filteredSlots.get(viewIndex);
        }

        @Override
        public boolean isActive() {
            return StorageServerMenu.this.slotsVisible && super.isActive();
        }

        @Override
        public ItemStack getItem() {
            return this.getItemHandler().getStackInSlot(getSlotIndex());
        }

        // Removed overridden set() method to rely securely on Forge's built-in parent validation to stop ClassCastException

        @Override
        public void setChanged() {
        }

        @Override
        public int getMaxStackSize() {
            return StorageServerBlockEntity.MAX_ITEM_CAPACITY;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return StorageServerBlockEntity.MAX_ITEM_CAPACITY;
        }

        @Override
        public ItemStack remove(int amount) {
            return this.getItemHandler().extractItem(getSlotIndex(), amount, false);
        }
    }
}