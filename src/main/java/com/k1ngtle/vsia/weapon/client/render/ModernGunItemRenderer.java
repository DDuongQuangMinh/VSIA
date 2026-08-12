package com.k1ngtle.vsia.weapon.client.render;

import com.k1ngtle.vsia.weapon.item.ModernGunItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class ModernGunItemRenderer extends GeoItemRenderer<ModernGunItem> {
    private final PlayerArmItemRenderer armRenderer = new PlayerArmItemRenderer();

    public ModernGunItemRenderer() { super(new ModernGunGeoModel()); }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        WeaponRenderContext.set(stack);
        pose.pushPose();
        try {
            hideExportedArmCubes(stack);
            WeaponTransforms.apply(pose, context, stack);
            super.renderByItem(stack, context, pose, buffers, light, overlay);
            if (shouldRenderPlayerArms(stack, context)) {
                armRenderer.renderByItem(stack, context, pose, buffers, light, overlay);
            }
        } finally {
            pose.popPose();
            WeaponRenderContext.clear();
        }
    }

    private void hideExportedArmCubes(ItemStack stack) {
        boolean ak74m = WeaponAssetResolver.weaponId(stack).getPath().equals("ak74m");
        getGeoModel().getBone("rightarm").ifPresent(bone -> bone.setHidden(ak74m));
        getGeoModel().getBone("leftarm").ifPresent(bone -> bone.setHidden(ak74m));
    }

    private boolean shouldRenderPlayerArms(ItemStack stack, ItemDisplayContext context) {
        return WeaponAssetResolver.weaponId(stack).getPath().equals("ak74m")
                && (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
    }
}
