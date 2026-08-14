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
import net.minecraftforge.items.ItemStackHandler;
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
    public static final int MAX_FILE_STORAGE_BYTES = 10 * 1024 * 1024; // 10 Megabytes

    private final List<StoredFile> storedFiles = new ArrayList<>();

    private final ItemStackHandler itemHandler = new ItemStackHandler(360) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 2000000;
        }

        @Override
        protected int getStackLimit(int slot, @NotNull ItemStack stack) {
            return 2000000;
        }
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
        tag.put("inventory", itemHandler.serializeNBT());

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
        itemHandler.deserializeNBT(tag.getCompound("inventory"));

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