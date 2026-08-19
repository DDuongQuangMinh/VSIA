package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.RtAc68uRouterItemModel;
import com.k1ngtle.vsia.signality.internet.router.RtAc68uRouterItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class RtAc68uRouterItemRenderer
        extends GeoItemRenderer<RtAc68uRouterItem> {

    public RtAc68uRouterItemRenderer() {
        super(new RtAc68uRouterItemModel());
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext transformType,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();

        if (transformType == ItemDisplayContext.GUI) {
            poseStack.translate(0.0D, 0.18D, 0.0D);
        }

        super.renderByItem(
                stack,
                transformType,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }
}