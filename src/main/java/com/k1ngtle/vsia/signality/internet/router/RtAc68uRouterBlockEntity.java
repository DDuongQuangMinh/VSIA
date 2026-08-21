package com.k1ngtle.vsia.signality.internet.router;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.world.inventory.RtAc68uRouterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class RtAc68uRouterBlockEntity
        extends NetworkDeviceBlockEntity
        implements GeoBlockEntity, MenuProvider {

    private static final RawAnimation POWER_UP =
            RawAnimation.begin()
                    .thenPlay("power_up");

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public final RouterOsSimulator routerOs =
            new RouterOsSimulator(this::onRouterOsChanged);

    private boolean routerManagementLoaded = false;

    public RtAc68uRouterBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                SignalityBlocks.RT_AC68U_ROUTER_BE.get(),
                pos,
                state
        );
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (level instanceof ServerLevel) {
            if (routerManagementLoaded) {
                applyRouterOsToLiveNetwork();
            } else if (!wifiRouterHasConfiguration()) {
                applyRouterDefaults();
                syncRouterOsWithDefaults();
            } else {
                syncRouterOsWithDefaults();
            }
        }
    }

    private void applyRouterDefaults() {
        configureWifiStaticIpv4(
                "192.168.1.1",
                "255.255.255.0",
                ""
        );

        configureWifiLiveRouterInterface(
                "lan0",
                "192.168.1.1",
                24
        );

        configureWifiLiveRouterInterface(
                "lan1",
                "192.168.2.1",
                24
        );

        setWifiLiveRouterEnabled(true);
    }


    private void syncRouterOsWithDefaults() {
        if (routerManagementLoaded) {
            return;
        }

        routerOs.wlanIp = wifiIpAddress();
        routerOs.wlanMask = "255.255.255.0";
        routerOs.wlanGateway = "";
        routerOs.lan0Ip = "192.168.1.1";
        routerOs.lan0Mask = "255.255.255.0";
        routerOs.lan1Ip = "192.168.2.1";
        routerOs.lan1Mask = "255.255.255.0";
        routerOs.forwardingEnabled = wifiLiveRouterEnabled();
    }

    private void onRouterOsChanged() {
        routerManagementLoaded = true;
        applyRouterOsToLiveNetwork();
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(
                    getBlockPos(),
                    getBlockState(),
                    getBlockState(),
                    3
            );
        }
    }

    public void applyRouterOsToLiveNetwork() {
        configureWifiStaticIpv4(
                routerOs.wlanIp,
                routerOs.wlanMask,
                routerOs.wlanGateway == null ? "" : routerOs.wlanGateway
        );

        configureWifiLiveRouterInterface(
                "lan0",
                routerOs.lan0Ip,
                RouterOsSimulator.maskToPrefix(routerOs.lan0Mask)
        );

        configureWifiLiveRouterInterface(
                "lan1",
                routerOs.lan1Ip,
                RouterOsSimulator.maskToPrefix(routerOs.lan1Mask)
        );

        setWifiLiveRouterEnabled(
                routerOs.forwardingEnabled
        );
    }


    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {
        controllers.add(
                new AnimationController<>(
                        this,
                        "router_power",
                        0,
                        state -> {
                            state.setAnimation(POWER_UP);
                            return PlayState.CONTINUE;
                        }
                )
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("W1204RouterOs", routerOs.save());
        tag.putBoolean("W1204RouterManagementLoaded", routerManagementLoaded);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains("W1204RouterOs")) {
            routerOs.load(
                    tag.getCompound("W1204RouterOs")
            );
            routerManagementLoaded = true;
        } else {
            routerManagementLoaded =
                    tag.getBoolean("W1204RouterManagementLoaded");
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(routerOs.displayName);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory playerInventory,
            Player player
    ) {
        return new RtAc68uRouterMenu(
                containerId,
                playerInventory,
                this
        );
    }

}
