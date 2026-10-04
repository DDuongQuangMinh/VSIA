package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.cockpit.program.DisplayLaptopBlock;
import com.k1ngtle.vsia.cockpit.program.DisplayLaptopBlockEntity;
import com.k1ngtle.vsia.cockpit.program.DisplayLaptopRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Render the actual inserted item model in a visible side caddy, not a decorative fake drive. */
@Mod.EventBusSubscriber(modid=Vsia.MOD_ID, bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class DisplayLaptopRenderer implements BlockEntityRenderer<DisplayLaptopBlockEntity> {
    private final net.minecraft.client.renderer.entity.ItemRenderer items;
    public DisplayLaptopRenderer(BlockEntityRendererProvider.Context context) { items = context.getItemRenderer(); }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) { event.registerBlockEntityRenderer(DisplayLaptopRegistry.LAPTOP_ENTITY.get(), DisplayLaptopRenderer::new); }
    public static float rotation(Direction direction) { return switch(direction) { case SOUTH -> 180; case EAST -> 90; case WEST -> 270; default -> 0; }; }
    @Override public void render(DisplayLaptopBlockEntity laptop, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!laptop.hasDrive()) return;
        pose.pushPose();
        try {
            pose.translate(0.5, 0, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-rotation(laptop.getBlockState().getValue(DisplayLaptopBlock.FACING))));
            pose.translate(0.38, 0.15, -0.03);
            pose.scale(0.28F, 0.28F, 0.28F);
            items.renderStatic(laptop.displayDrive(), ItemDisplayContext.NONE, light, overlay, pose, buffers, laptop.getLevel(), 0);
        } finally { pose.popPose(); }
    }
}
