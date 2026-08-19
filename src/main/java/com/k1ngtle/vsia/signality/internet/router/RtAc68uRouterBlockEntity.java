package com.k1ngtle.vsia.signality.internet.router;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class RtAc68uRouterBlockEntity
        extends NetworkDeviceBlockEntity
        implements GeoBlockEntity {

    private static final RawAnimation POWER_UP =
            RawAnimation.begin()
                    .thenPlay("power_up");

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

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

        if (level instanceof ServerLevel
                && !wifiRouterHasConfiguration()) {
            applyRouterDefaults();
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
}
