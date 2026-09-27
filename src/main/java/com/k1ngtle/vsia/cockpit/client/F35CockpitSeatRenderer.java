package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.cockpit.CcTerminalBuffer;
import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class F35CockpitSeatRenderer
        extends GeoBlockRenderer<F35CockpitSeatBlockEntity> {
    private static final String MONITOR_ANCHOR =
            "monitor_center_surface";

    private static final float MODEL_UNIT =
            1.0F / 16.0F;

    private static final float MONITOR_OFFSET_X =
            0.13634000F * MODEL_UNIT;

    private static final float MONITOR_OFFSET_Y =
            0.64848000F * MODEL_UNIT;

    private static final float MONITOR_FRONT_Z =
            -0.42219000F * MODEL_UNIT
                    - 0.0010F;

    private static final float MONITOR_WIDTH =
            9.74204F
                    * MODEL_UNIT
                    * 0.92F;

    private static final float MONITOR_HEIGHT =
            3.24824F
                    * MODEL_UNIT
                    * 0.90F;

    private static final float CELL_WIDTH =
            6.0F;

    private static final float CELL_HEIGHT =
            9.0F;

    private final Font font;

    public F35CockpitSeatRenderer(
            BlockEntityRendererProvider.Context context
    ) {
        super(
                new F35CockpitSeatModel()
        );

        font =
                context.getFont();
    }

    @Override
    public void renderRecursively(
            PoseStack poseStack,
            F35CockpitSeatBlockEntity animatable,
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
        if (MONITOR_ANCHOR.equals(
                bone.getName()
        )) {
            F35CockpitDemoPage.update(
                    animatable
            );

            poseStack.pushPose();

            /*
             * renderRecursively is already executing with poseStack positioned
             * for this GeoBone. Applying prepMatrixForBone here a second time
             * double-transforms the monitor anchor and is what pushed the text
             * far below monitor_center in v1.0.1.
             */
            renderTerminal(
                    poseStack,
                    bufferSource,
                    animatable.terminal()
            );

            poseStack.popPose();
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

    private void renderTerminal(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            CcTerminalBuffer terminal
    ) {
        float textPixelWidth =
                terminal.width()
                        * CELL_WIDTH;

        float textPixelHeight =
                terminal.height()
                        * CELL_HEIGHT;

        float scale =
                Math.min(
                        MONITOR_WIDTH / textPixelWidth,
                        MONITOR_HEIGHT / textPixelHeight
                );

        poseStack.translate(
                MONITOR_OFFSET_X,
                MONITOR_OFFSET_Y,
                MONITOR_FRONT_Z
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        180.0F
                )
        );

        poseStack.translate(
                -textPixelWidth
                        * scale
                        / 2.0F,
                textPixelHeight
                        * scale
                        / 2.0F,
                0.0F
        );

        poseStack.scale(
                scale,
                -scale,
                scale
        );

        for (int row = 0;
             row < terminal.height();
             row++) {
            for (int column = 0;
                 column < terminal.width();
                 column++) {
                char character =
                        terminal.characterAt(
                                column,
                                row
                        );

                int foreground =
                        terminal.foregroundAt(
                                column,
                                row
                        )
                                .argb();

                font.drawInBatch(
                        String.valueOf(
                                character
                        ),
                        column
                                * CELL_WIDTH,
                        row
                                * CELL_HEIGHT,
                        foreground,
                        false,
                        poseStack
                                .last()
                                .pose(),
                        bufferSource,
                        Font.DisplayMode.POLYGON_OFFSET,
                        0,
                        LightTexture.FULL_BRIGHT
                );
            }
        }
    }
}
