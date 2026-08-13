package com.k1ngtle.vsia.signality.internet.server;

import com.k1ngtle.vsia.client.renderer.ServerRackItemRenderer;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class ServerRackItem extends BlockItem implements GeoItem {

    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);

    public ServerRackItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(
            Consumer<IClientItemExtensions> consumer
    ) {
        consumer.accept(new IClientItemExtensions() {

            private ServerRackItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new ServerRackItemRenderer();
                }

                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {
        // The inventory item does not need an animation controller.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }
}