package com.k1ngtle.vsia.client.renderer;

import com.k1ngtle.vsia.client.model.FirewallItemModel;
import com.k1ngtle.vsia.signality.internet.server.FirewallItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class FirewallItemRenderer extends GeoItemRenderer<FirewallItem> {

    public FirewallItemRenderer() {
        super(new FirewallItemModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float scale = switch (displayContext) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> 0.48F;
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> 0.42F;
            case GUI -> 0.38F;
            case GROUND -> 0.40F;
            case FIXED -> 0.44F;
            default -> 0.45F;
        };

        poseStack.pushPose();

        switch (displayContext) {
            case FIRST_PERSON_RIGHT_HAND -> poseStack.translate(0.34D, -0.16D, 0.0D);
            case FIRST_PERSON_LEFT_HAND -> poseStack.translate(-0.34D, -0.16D, 0.0D);
            case THIRD_PERSON_RIGHT_HAND -> poseStack.translate(0.22D, 0.0D, 0.30D);
            case THIRD_PERSON_LEFT_HAND -> poseStack.translate(-0.22D, 0.0D, 0.30D);
            case GUI -> poseStack.translate(0.32D, -0.12D, 0.0D);
            case FIXED -> {
                poseStack.translate(0.65D, -0.18D, 0.52D);
                poseStack.mulPose(Axis.YP.rotationDegrees(-180.0F));
            }
            default -> {}
        }

        poseStack.scale(scale, scale, scale);
        super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}