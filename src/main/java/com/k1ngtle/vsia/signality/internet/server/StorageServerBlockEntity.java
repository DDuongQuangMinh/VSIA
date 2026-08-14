package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.world.inventory.StorageServerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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

public class StorageServerBlockEntity extends BlockEntity implements GeoBlockEntity, MenuProvider {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Expand to 360 Slots (40 rows of 9)
    // Overriding the stack limit to 2,000,000 requires custom logic within the handler
    private final ItemStackHandler itemHandler = new ItemStackHandler(360) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        // Increase the maximum stack size allowed in these slots
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
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // No default/idle animation is set here - the controller just holds whatever
        // pose it's currently in until "place" is triggered below.
        controllers.add(new AnimationController<>(this, "controller", 0, event -> PlayState.CONTINUE)
                .triggerableAnim("place", RawAnimation.begin().thenPlay("checking")));
    }

    /**
     * Plays the "checking" animation once. GeckoLib automatically syncs triggered
     * animations to nearby clients, so this only needs to be called server-side.
     * Call this from StorageServerBlock#setPlacedBy so it only ever fires once,
     * right when the block is placed - never on world load/chunk reload.
     */
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
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
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