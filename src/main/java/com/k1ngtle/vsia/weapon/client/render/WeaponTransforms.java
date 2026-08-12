package com.k1ngtle.vsia.weapon.client.render;

import com.k1ngtle.vsia.weapon.client.gameplay.ClientWeaponContext;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class WeaponTransforms {
    private WeaponTransforms() {}

    public static void apply(PoseStack pose, ItemDisplayContext context, ItemStack stack) {
        ResourceLocation id = WeaponAssetResolver.weaponId(stack);
        if (id.getNamespace().equals("vsia") && id.getPath().equals("ak74m")) {
            applyAk74m(pose, context);
        } else {
            applyDefault(pose, context);
        }
    }

    private static void applyAk74m(PoseStack pose, ItemDisplayContext context) {
        if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            float ads = smoothAimProgress();
            pose.translate(
                    lerp(0.44D, -0.47D, ads),
                    lerp(-1.10D, -0.94D, ads),
                    lerp(-0.05D, 0.20D, ads));
            pose.scale(0.82F, 0.82F, 0.82F);
        } else if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            pose.translate(-0.44D, -1.10D, -0.05D);
            pose.scale(0.82F, 0.82F, 0.82F);
        } else if (context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            float side = context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ? -1.0F : 1.0F;
            pose.translate(0.35D * side, 0.30D, 0.50D);
            pose.mulPose(Axis.YP.rotationDegrees(0.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(-12.0F * side));
            pose.scale(0.18F, 0.18F, 0.18F);
        } else if (context == ItemDisplayContext.GUI) {
            pose.translate(0.24D, -0.01D, 0.0D);
            pose.mulPose(Axis.YP.rotationDegrees(58.0F));
            pose.mulPose(Axis.XP.rotationDegrees(-22.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(-38.0F));
            pose.scale(0.21F, 0.21F, 0.21F);
        } else if (context == ItemDisplayContext.GROUND) {
            pose.translate(0.0D, 0.10D, 0.0D);
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            pose.scale(0.20F, 0.20F, 0.20F);
        } else if (context == ItemDisplayContext.FIXED) {
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(-20.0F));
            pose.scale(0.18F, 0.18F, 0.18F);
        }
    }

    private static void applyDefault(PoseStack pose, ItemDisplayContext context) {
        if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            float ads = smoothAimProgress();
            pose.translate(0.35D * (1.0F - ads), -0.28D + 0.08D * ads,
                    -0.55D + 0.18D * ads);
            pose.scale(1.15F, 1.15F, 1.15F);
        } else if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            pose.translate(-0.35D, -0.28D, -0.55D);
            pose.scale(1.15F, 1.15F, 1.15F);
        } else if (context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            pose.translate(0.0D, 0.1D, -0.15D);
            pose.scale(0.8F, 0.8F, 0.8F);
        }
    }

    private static double lerp(double start, double end, float progress) {
        return start + (end - start) * progress;
    }

    private static float smoothAimProgress() {
        float partialTick = Minecraft.getInstance().getFrameTime();
        float progress = ClientWeaponContext.getInstance().getInterpolatedAimProgress(partialTick);
        return progress * progress * (3.0F - 2.0F * progress);
    }
}
