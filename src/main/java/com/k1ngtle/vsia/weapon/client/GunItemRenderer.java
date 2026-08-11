package com.k1ngtle.vsia.weapon.client;

import com.k1ngtle.vsia.weapon.GunItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GunItemRenderer extends GeoItemRenderer<GunItem> {

    private static final float ADS_RIGHT_X = -0.455f;
    private static final float ADS_RIGHT_Y = 0.160f;
    private static final float ADS_RIGHT_Z = -0.120f;

    private static final float ADS_LEFT_X = 0.185f;
    private static final float ADS_LEFT_Y = 0.100f;
    private static final float ADS_LEFT_Z = -0.120f;

    private static final float ADS_RIGHT_ROT_X = -1.5f;
    private static final float ADS_RIGHT_ROT_Y = 0.78f;
    private static final float ADS_RIGHT_ROT_Z = -0.80f;

    private static final float ADS_LEFT_ROT_X = -1.5f;
    private static final float ADS_LEFT_ROT_Y = 0.0f;
    private static final float ADS_LEFT_ROT_Z = 6.0f;

    private ItemDisplayContext currentTransform = ItemDisplayContext.NONE;

    public GunItemRenderer() {
        super(new GunGeoModel());
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
        currentTransform = transformType;

        super.renderByItem(
                stack,
                transformType,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );
    }

    @Override
    public void preRender(
            PoseStack poseStack,
            GunItem animatable,
            BakedGeoModel model,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        super.preRender(
                poseStack,
                animatable,
                model,
                bufferSource,
                buffer,
                isReRender,
                partialTick,
                packedLight,
                packedOverlay,
                red,
                green,
                blue,
                alpha
        );

        boolean firstPersonRight =
                currentTransform == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;

        boolean firstPersonLeft =
                currentTransform == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;

        boolean firstPerson =
                firstPersonRight || firstPersonLeft;

        if (firstPerson) {
            final float ads = clamp01(
                    ClientGunHandler.getLerpedAds()
            );

            if (ads > 0.0f) {
                applyAdsTransform(
                        poseStack,
                        ads,
                        firstPersonLeft
                );
            }
        }

        model.getBone("rightarm").ifPresent(
                bone -> bone.setHidden(!firstPerson)
        );

        model.getBone("leftarm").ifPresent(
                bone -> bone.setHidden(!firstPerson)
        );

        model.getBone("scope").ifPresent(
                bone -> bone.setHidden(true)
        );

        model.getBone("_cb_scope").ifPresent(
                bone -> bone.setHidden(true)
        );

        model.getBone("_cb_canted").ifPresent(
                bone -> bone.setHidden(true)
        );

        model.getBone("_cb_suppressor").ifPresent(
                bone -> bone.setHidden(true)
        );
    }

    private void applyAdsTransform(
            PoseStack poseStack,
            float ads,
            boolean leftHand
    ) {
        float moveX = leftHand ? ADS_LEFT_X : ADS_RIGHT_X;
        float moveY = leftHand ? ADS_LEFT_Y : ADS_RIGHT_Y;
        float moveZ = leftHand ? ADS_LEFT_Z : ADS_RIGHT_Z;

        float rotX = leftHand ? ADS_LEFT_ROT_X : ADS_RIGHT_ROT_X;
        float rotY = leftHand ? ADS_LEFT_ROT_Y : ADS_RIGHT_ROT_Y;
        float rotZ = leftHand ? ADS_LEFT_ROT_Z : ADS_RIGHT_ROT_Z;

        poseStack.translate(
                moveX * ads,
                moveY * ads,
                moveZ * ads
        );

        if (rotX != 0.0f) {
            poseStack.mulPose(
                    Axis.XP.rotationDegrees(
                            rotX * ads
                    )
            );
        }

        if (rotY != 0.0f) {
            poseStack.mulPose(
                    Axis.YP.rotationDegrees(
                            rotY * ads
                    )
            );
        }

        if (rotZ != 0.0f) {
            poseStack.mulPose(
                    Axis.ZP.rotationDegrees(
                            rotZ * ads
                    )
            );
        }
    }

    private static float clamp01(float value) {
        return Math.max(
                0.0f,
                Math.min(
                        1.0f,
                        value
                )
        );
    }

    @Override
    public void renderRecursively(
            PoseStack poseStack,
            GunItem animatable,
            GeoBone bone,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        if (
                bone.getName().equals("rightarm")
                        ||
                        bone.getName().equals("leftarm")
        ) {
            Minecraft minecraft =
                    Minecraft.getInstance();

            if (minecraft.player != null) {
                ResourceLocation skinTexture =
                        minecraft.player
                                .getSkinTextureLocation();

                RenderType skinRenderType =
                        RenderType.entityCutout(
                                skinTexture
                        );

                VertexConsumer skinBuffer =
                        bufferSource.getBuffer(
                                skinRenderType
                        );

                super.renderRecursively(
                        poseStack,
                        animatable,
                        bone,
                        skinRenderType,
                        bufferSource,
                        skinBuffer,
                        isReRender,
                        partialTick,
                        packedLight,
                        packedOverlay,
                        red,
                        green,
                        blue,
                        alpha
                );

                bufferSource.getBuffer(
                        renderType
                );

                return;
            }
        }

        super.renderRecursively(
                poseStack,
                animatable,
                bone,
                renderType,
                bufferSource,
                buffer,
                isReRender,
                partialTick,
                packedLight,
                packedOverlay,
                red,
                green,
                blue,
                alpha
        );
    }
}