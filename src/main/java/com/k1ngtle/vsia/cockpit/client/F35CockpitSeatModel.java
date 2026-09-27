package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class F35CockpitSeatModel
        extends GeoModel<F35CockpitSeatBlockEntity> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(
                    Vsia.MOD_ID,
                    "geo/f35_display.geo.json"
            );

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(
                    Vsia.MOD_ID,
                    "textures/block/f35_display.png"
            );

    private static final ResourceLocation ANIMATION =
            new ResourceLocation(
                    Vsia.MOD_ID,
                    "animations/f35_display.animation.json"
            );

    @Override
    public ResourceLocation getModelResource(
            F35CockpitSeatBlockEntity animatable
    ) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(
            F35CockpitSeatBlockEntity animatable
    ) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(
            F35CockpitSeatBlockEntity animatable
    ) {
        return ANIMATION;
    }
}
