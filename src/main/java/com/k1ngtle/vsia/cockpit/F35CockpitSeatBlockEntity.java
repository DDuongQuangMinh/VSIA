package com.k1ngtle.vsia.cockpit;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
