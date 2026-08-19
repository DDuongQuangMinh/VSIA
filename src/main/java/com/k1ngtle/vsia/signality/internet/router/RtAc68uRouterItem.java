package com.k1ngtle.vsia.signality.internet.router;

import com.k1ngtle.vsia.client.renderer.RtAc68uRouterItemRenderer;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class RtAc68uRouterItem
        extends BlockItem
        implements GeoItem {

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public RtAc68uRouterItem(
            Block block,
            Properties properties
    ) {
        super(block, properties);
    }

    @Override
    public void initializeClient(
            Consumer<IClientItemExtensions> consumer
    ) {
        consumer.accept(
                new IClientItemExtensions() {
                    private RtAc68uRouterItemRenderer renderer;

                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        if (renderer == null) {
                            renderer =
                                    new RtAc68uRouterItemRenderer();
                        }

                        return renderer;
                    }
                }
        );
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
}