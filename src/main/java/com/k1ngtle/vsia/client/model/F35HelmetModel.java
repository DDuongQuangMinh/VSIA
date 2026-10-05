package com.k1ngtle.vsia.client.model;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.item.F35HelmetItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** User-supplied geometry and UVs are preserved; optional texture can arrive later. */
public final class F35HelmetModel extends GeoModel<F35HelmetItem> {
    public static final ResourceLocation TEXTURE=new ResourceLocation(Vsia.MOD_ID,"textures/armor/f35_helmet.png");
    private static final ResourceLocation PLACEHOLDER=new ResourceLocation("minecraft","textures/block/gray_concrete.png");
    @Override public ResourceLocation getModelResource(F35HelmetItem item){return new ResourceLocation(Vsia.MOD_ID,"geo/armor/f35_helmet.geo.json");}
    @Override public ResourceLocation getTextureResource(F35HelmetItem item){return Minecraft.getInstance().getResourceManager().getResource(TEXTURE).isPresent()?TEXTURE:PLACEHOLDER;}
    @Override public ResourceLocation getAnimationResource(F35HelmetItem item){return new ResourceLocation(Vsia.MOD_ID,"animations/armor/f35_helmet.animation.json");}
}
