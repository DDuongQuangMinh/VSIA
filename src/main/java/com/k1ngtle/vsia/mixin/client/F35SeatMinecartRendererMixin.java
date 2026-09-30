package com.k1ngtle.vsia.mixin.client;

import com.k1ngtle.vsia.cockpit.F35SeatController;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecartRenderer.class)
public abstract class F35SeatMinecartRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/world/entity/vehicle/AbstractMinecart;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void vsia$hideF35SeatCarrier(
            AbstractMinecart minecart,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo callbackInfo
    ) {
        Component customName =
                minecart.getCustomName();

        if (customName == null) {
            return;
        }

        if (F35SeatController.SEAT_RENDER_MARKER.equals(
                customName.getString()
        )) {
            callbackInfo.cancel();
        }
    }
}
