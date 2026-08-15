package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.world.inventory.StorageServerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

public class StorageServerBlockEntity extends BlockEntity implements GeoBlockEntity, MenuProvider {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static final int MAX_FILE_STORAGE_BYTES = 10 * 1024 * 1024;
    public static final int MAX_ITEM_CAPACITY = 600000; // Updated to 600,000

    private final List<StoredFile> storedFiles = new ArrayList<>();

    public static int getRealCount(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.hasTag() && stack.getTag().contains("VSIA_Count")) {
            return stack.getTag().getInt("VSIA_Count");
        }
        return stack.getCount();
    }

    public static void setRealCount(ItemStack stack, int count) {
        if (count <= 0) {
            stack.setCount(0);
            if (stack.hasTag()) {
                stack.getTag().remove("VSIA_Count");
                if (stack.getTag().isEmpty()) stack.setTag(null);
            }
            return;
        }
        if (count <= stack.getMaxStackSize()) {
            stack.setCount(count);
            if (stack.hasTag()) {
                stack.getTag().remove("VSIA_Count");
                if (stack.getTag().isEmpty()) stack.setTag(null);
            }
        } else {
            stack.setCount(stack.getMaxStackSize());
            stack.getOrCreateTag().putInt("VSIA_Count", count);
        }
    }

    public static boolean canMergeItems(ItemStack stack1, ItemStack stack2) {
        if (stack1.isEmpty() || stack2.isEmpty()) return false;
        if (stack1.getItem() != stack2.getItem()) return false;

        CompoundTag tag1 = stack1.getTag();
        CompoundTag tag2 = stack2.getTag();

        if (tag1 == null && tag2 == null) return true;

        CompoundTag copy1 = tag1 != null ? tag1.copy() : new CompoundTag();
        copy1.remove("VSIA_Count");
        if (copy1.isEmpty()) copy1 = null;

        CompoundTag copy2 = tag2 != null ? tag2.copy() : new CompoundTag();
        copy2.remove("VSIA_Count");
        if (copy2.isEmpty()) copy2 = null;

        if (copy1 == null && copy2 == null) return true;
        if (copy1 == null || copy2 == null) return false;

        return copy1.equals(copy2);
    }

    private final IItemHandlerModifiable itemHandler = new IItemHandlerModifiable() {
        private net.minecraft.core.NonNullList<ItemStack> stacks = net.minecraft.core.NonNullList.withSize(360, ItemStack.EMPTY);

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            stacks.set(slot, stack);
            setChanged();
        }

        @Override
        public int getSlots() { return 360; }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) { return stacks.get(slot); }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack existing = stacks.get(slot);
            int limit = MAX_ITEM_CAPACITY;

            if (!existing.isEmpty()) {
                if (!canMergeItems(stack, existing)) return stack;
                limit -= getRealCount(existing);
            }

            if (limit <= 0) return stack;
            int insert = Math.min(stack.getCount(), limit);

            if (!simulate) {
                if (existing.isEmpty()) {
                    ItemStack copy = stack.copy();
                    setRealCount(copy, insert);
                    stacks.set(slot, copy);
                } else {
                    setRealCount(existing, getRealCount(existing) + insert);
                }
                setChanged();
            }

            ItemStack remainder = stack.copy();
            remainder.shrink(insert);
            return remainder;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0) return ItemStack.EMPTY;
            ItemStack existing = stacks.get(slot);
            if (existing.isEmpty()) return ItemStack.EMPTY;

            int existingCount = getRealCount(existing);
            int extract = Math.min(amount, existingCount);

            ItemStack extracted = existing.copy();
            extracted.setCount(Math.min(extract, extracted.getMaxStackSize()));
            setRealCount(extracted, extract);

            if (!simulate) {
                setRealCount(existing, existingCount - extract);
                if (getRealCount(existing) <= 0) stacks.set(slot, ItemStack.EMPTY);
                setChanged();
            }

            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) { return MAX_ITEM_CAPACITY; }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) { return true; }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public StorageServerBlockEntity(BlockPos pos, BlockState state) {
        super(SignalityBlocks.STORAGE_SERVER_BE.get(), pos, state);
        initDefaultFilesIfEmpty();
    }

    private void initDefaultFilesIfEmpty() {
        if (storedFiles.isEmpty()) {
            storedFiles.add(new StoredFile("main.py", "python", "# Main Python Script\nprint('Storage Server Active')\nimport time\ntime.sleep(1)"));
            storedFiles.add(new StoredFile("core.cpp", "c++", "#include <iostream>\nint main() {\n    std::cout << \"VSIA Storage Online\" << std::endl;\n    return 0;\n}"));
            storedFiles.add(new StoredFile("api.cs", "c#", "using System;\nnamespace Server {\n    class Program {\n        static void Main() { Console.WriteLine(\"C# API Ready\"); }\n    }\n}"));
            storedFiles.add(new StoredFile("script.lua", "lua", "-- Server Control Script\nfunction init()\n    print(\"Lua Engine Initialized\")\nend"));
            storedFiles.add(new StoredFile("index.html", "web", "<!DOCTYPE html>\n<html>\n<head><title>Storage Console</title></head>\n<body><h1>Storage Server</h1></body>\n</html>"));
            storedFiles.add(new StoredFile("app.js", "web", "console.log('App running...');\nconst totalSlots = 360;\nconst maxCap = '10MB';"));
            storedFiles.add(new StoredFile("style.css", "web", "body { background: #18191B; color: #0092C8; font-family: monospace; }"));
        }
    }

    public List<StoredFile> getStoredFiles() {
        return storedFiles;
    }

    public int getTotalUsedFileBytes() {
        int total = 0;
        for (StoredFile file : storedFiles) {
            total += file.getSizeInBytes();
        }
        return total;
    }

    public boolean addFile(StoredFile file) {
        if (getTotalUsedFileBytes() + file.getSizeInBytes() <= MAX_FILE_STORAGE_BYTES) {
            storedFiles.add(file);
            setChanged();
            return true;
        }
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> PlayState.CONTINUE)
                .triggerableAnim("place", RawAnimation.begin().thenPlay("checking")));
    }

    public void playPlacementAnimation() {
        this.triggerAnim("controller", "place");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        ListTag nbtTagList = new ListTag();
        for (int i = 0; i < 360; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                stack.save(itemTag);
                // EXPLICITLY save the giant integer to bypass Vanilla byte truncation on reload
                itemTag.putInt("ExtendedCount", getRealCount(stack));
                nbtTagList.add(itemTag);
            }
        }
        CompoundTag inventoryTag = new CompoundTag();
        inventoryTag.put("Items", nbtTagList);
        tag.put("inventory", inventoryTag);

        ListTag fileList = new ListTag();
        for (StoredFile file : storedFiles) {
            fileList.add(file.serializeNBT());
        }
        tag.put("StoredFiles", fileList);

        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        CompoundTag inventoryTag = tag.getCompound("inventory");
        ListTag tagList = inventoryTag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTags = tagList.getCompound(i);
            int slot = itemTags.getInt("Slot");
            if (slot >= 0 && slot < 360) {
                ItemStack loadedStack = ItemStack.of(itemTags);

                // EXPLICITLY restore the giant integer from our safe save
                if (itemTags.contains("ExtendedCount")) {
                    setRealCount(loadedStack, itemTags.getInt("ExtendedCount"));
                }

                ((IItemHandlerModifiable) itemHandler).setStackInSlot(slot, loadedStack);
            }
        }

        storedFiles.clear();
        if (tag.contains("StoredFiles", Tag.TAG_LIST)) {
            ListTag fileList = tag.getList("StoredFiles", Tag.TAG_COMPOUND);
            for (int i = 0; i < fileList.size(); i++) {
                storedFiles.add(StoredFile.deserializeNBT(fileList.getCompound(i)));
            }
        } else {
            initDefaultFilesIfEmpty();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.vsia.storage_server");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StorageServerMenu(containerId, playerInventory, this);
    }
}