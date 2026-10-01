package com.k1ngtle.vsia.cockpit;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class F35CockpitSeatBlockEntity
        extends BlockEntity
        implements GeoBlockEntity {
    public static final int TERMINAL_WIDTH =
            27;

    public static final int TERMINAL_HEIGHT =
            6;

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(
                    this
            );

    private final CcTerminalBuffer terminal =
            new CcTerminalBuffer(
                    TERMINAL_WIDTH,
                    TERMINAL_HEIGHT
            );

    private boolean demoMode =
            true;

    private ItemStack displayDrive =
            ItemStack.EMPTY;

    public F35CockpitSeatBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                F35CockpitRegistry
                        .F35_COCKPIT_SEAT_BE
                        .get(),
                pos,
                state
        );
    }

    public CcTerminalBuffer terminal() {
        return terminal;
    }


    public ItemStack displayDrive() {
        return displayDrive;
    }

    public boolean hasDisplayDrive() {
        return !displayDrive.isEmpty();
    }

    public boolean installDisplayDrive(
            ItemStack stack
    ) {
        if (!(stack.getItem()
                instanceof com.k1ngtle.vsia.item.DisplayHardDriveItem)) {
            return false;
        }

        ItemStack installed =
                stack.copy();

        installed.setCount(
                1
        );

        displayDrive =
                installed;

        sync();
        return true;
    }

    public ItemStack takeDisplayDrive() {
        ItemStack removed =
                displayDrive;

        displayDrive =
                ItemStack.EMPTY;

        sync();
        return removed;
    }

    public boolean demoMode() {
        return demoMode;
    }

    public void setDemoMode(
            boolean value
    ) {
        demoMode =
                value;

        sync();
    }

    public void sync() {
        setChanged();

        if (level != null
                && !level.isClientSide) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    3
            );
        }
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag
    ) {
        super.saveAdditional(
                tag
        );

        tag.putBoolean(
                "MonitorDemoMode",
                demoMode
        );

        if (!displayDrive.isEmpty()) {
            tag.put(
                    "DisplayDrive",
                    displayDrive.save(
                            new CompoundTag()
                    )
            );
        }

        terminal.save(
                tag
        );
    }

    @Override
    public void load(
            CompoundTag tag
    ) {
        super.load(
                tag
        );

        if (tag.contains(
                "MonitorDemoMode"
        )) {
            demoMode =
                    tag.getBoolean(
                            "MonitorDemoMode"
                    );
        }

        if (tag.contains(
                "DisplayDrive"
        )) {
            displayDrive =
                    ItemStack.of(
                            tag.getCompound(
                                    "DisplayDrive"
                            )
                    );
        } else {
            displayDrive =
                    ItemStack.EMPTY;
        }

        terminal.load(
                tag
        );
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(
                this
        );
    }
}
