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
import com.k1ngtle.vsia.cockpit.iff.F35IffConfig;
import com.k1ngtle.vsia.cockpit.display.F35DisplaySettings;
import java.util.UUID;

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

    private final F35IffConfig iff = new F35IffConfig();
    private UUID cockpitId;
    private final F35DisplaySettings displaySettings = new F35DisplaySettings();
    private final com.k1ngtle.vsia.cockpit.detection.F35StructureMonitor structure = new com.k1ngtle.vsia.cockpit.detection.F35StructureMonitor();
    public com.k1ngtle.vsia.cockpit.detection.F35StructureMonitor structureMonitor(){return structure;}

    public F35IffConfig iff() { return iff; }
    public UUID cockpitId() { return cockpitId; }
    public F35DisplaySettings displaySettings() { return displaySettings; }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && cockpitId == null) {
            cockpitId = UUID.randomUUID();
            sync();
        }
        if (level != null && !level.isClientSide) com.k1ngtle.vsia.cockpit.iff.F35IffService.register(this);
    }

    @Override public void setRemoved() { com.k1ngtle.vsia.cockpit.iff.F35IffService.unregister(this); super.setRemoved(); }
    @Override public void onChunkUnloaded() { com.k1ngtle.vsia.cockpit.iff.F35IffService.unregister(this); super.onChunkUnloaded(); }

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

        // Full save is server-side storage. getUpdateTag removes this field so secrets never reach clients.
        tag.put("F35Iff", iff.save());
        if (cockpitId != null) tag.putUUID("F35CockpitId", cockpitId);
        tag.put("F35DisplaySettings", displaySettings.save());
        tag.put("F35Structure",structure.save());
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

        if (tag.contains("F35Iff")) iff.load(tag.getCompound("F35Iff"));
        if (tag.hasUUID("F35CockpitId")) cockpitId = tag.getUUID("F35CockpitId");
        if (tag.contains("F35DisplaySettings")) displaySettings.load(tag.getCompound("F35DisplaySettings"));
        if (tag.contains("F35Structure")) structure.load(tag.getCompound("F35Structure"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = saveWithoutMetadata();
        tag.remove("F35Iff");
        tag.remove("F35Structure");
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(
                this
        );
    }
}
