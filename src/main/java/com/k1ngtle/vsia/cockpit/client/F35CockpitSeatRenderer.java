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
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.util.RenderUtils;

public final class F35CockpitSeatRenderer
        extends GeoBlockRenderer<F35CockpitSeatBlockEntity> {
    private static final String MONITOR_PARENT_BONE =
            "range_finder2";

    private static final double EXPECTED_MONITOR_WIDTH =
            9.74204D;

    private static final double EXPECTED_MONITOR_HEIGHT =
            3.24824D;

    private static final double EXPECTED_MONITOR_DEPTH =
            0.14804D;

    private static final double CUBE_SIZE_EPSILON =
            0.001D;

    private static final float MODEL_UNIT =
            1.0F / 16.0F;

    private static final float SCREEN_WIDTH_FILL =
            0.92F;

    private static final float SCREEN_HEIGHT_FILL =
            0.90F;

    private static final float SURFACE_EPSILON =
            0.0010F;

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
        boolean monitorBone =
                !isReRender
                        && MONITOR_PARENT_BONE.equals(
                        bone.getName()
                );

        GeoCube monitorCube =
                monitorBone
                        ? findMonitorCube(
                        bone
                )
                        : null;

        /*
         * Render GeckoLib geometry FIRST.
         *
         * v1.0.4 rendered Font.drawInBatch before GeckoLib rendered the
         * physical monitor cube. Font rendering asks MultiBufferSource for a
         * different RenderType and can switch the active BufferBuilder. The
         * GeoBlockRenderer then continued with the old VertexConsumer, which
         * is why the black monitor surface disappeared in the screenshot.
         */
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

        if (monitorCube == null) {
            return;
        }

        F35CockpitDemoPage.update(
                animatable
        );

        /*
         * super.renderRecursively has popped the bone-local PoseStack, so the
         * stack is back at the exact parent transform. Reapply only the real
         * range_finder2 bone and real monitor GeoCube transforms, then draw
         * the terminal after the physical monitor surface already exists.
         */
        renderTerminalOnMonitorCube(
                poseStack,
                bufferSource,
                animatable.terminal(),
                bone,
                monitorCube
        );

        /*
         * Font.drawInBatch switches MultiBufferSource to the font RenderType.
         * Re-select the GeckoLib model RenderType before returning so later
         * child/sibling cubes do not continue through a stale VertexConsumer.
         */
        bufferSource.getBuffer(
                renderType
        );
    }

    private GeoCube findMonitorCube(
            GeoBone bone
    ) {
        for (GeoCube cube :
                bone.getCubes()) {
            if (approximately(
                    cube.size().x(),
                    EXPECTED_MONITOR_WIDTH
            )
                    && approximately(
                    cube.size().y(),
                    EXPECTED_MONITOR_HEIGHT
            )
                    && approximately(
                    cube.size().z(),
                    EXPECTED_MONITOR_DEPTH
            )) {
                return cube;
            }
        }

        return null;
    }

    private boolean approximately(
            double actual,
            double expected
    ) {
        return Math.abs(
                actual - expected
        ) <= CUBE_SIZE_EPSILON;
    }

    private void renderTerminalOnMonitorCube(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            CcTerminalBuffer terminal,
            GeoBone monitorParentBone,
            GeoCube monitorCube
    ) {
        GeoQuad frontFace =
                findFrontFace(
                        monitorCube
                );

        if (frontFace == null) {
            return;
        }

        Vector3f faceCenter =
                calculateFaceCenter(
                        frontFace
                );

        float monitorWidth =
                (float) monitorCube
                        .size()
                        .x()
                        * MODEL_UNIT
                        * SCREEN_WIDTH_FILL;

        float monitorHeight =
                (float) monitorCube
                        .size()
                        .y()
                        * MODEL_UNIT
                        * SCREEN_HEIGHT_FILL;

        poseStack.pushPose();

        RenderUtils.prepMatrixForBone(
                poseStack,
                monitorParentBone
        );

        RenderUtils.translateToPivotPoint(
                poseStack,
                monitorCube
        );

        RenderUtils.rotateMatrixAroundCube(
                poseStack,
                monitorCube
        );

        RenderUtils.translateAwayFromPivotPoint(
                poseStack,
                monitorCube
        );

        poseStack.translate(
                faceCenter.x(),
                faceCenter.y(),
                faceCenter.z()
                        - SURFACE_EPSILON
        );

        renderTerminal(
                poseStack,
                bufferSource,
                terminal,
                monitorWidth,
                monitorHeight
        );

        poseStack.popPose();
    }

    private GeoQuad findFrontFace(
            GeoCube cube
    ) {
        GeoQuad[] quads =
                cube.quads();

        if (quads == null) {
            return null;
        }

        for (GeoQuad quad :
                quads) {
            if (quad != null
                    && quad.direction()
                    == Direction.NORTH) {
                return quad;
            }
        }

        return null;
    }

    private Vector3f calculateFaceCenter(
            GeoQuad face
    ) {
        GeoVertex[] vertices =
                face.vertices();

        Vector3f center =
                new Vector3f();

        if (vertices == null
                || vertices.length == 0) {
            return center;
        }

        for (GeoVertex vertex :
                vertices) {
            center.add(
                    vertex.position()
            );
        }

        center.div(
                vertices.length
        );

        return center;
    }

    private void renderTerminal(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            CcTerminalBuffer terminal,
            float monitorWidth,
            float monitorHeight
    ) {
        float textPixelWidth =
                terminal.width()
                        * CELL_WIDTH;

        float textPixelHeight =
                terminal.height()
                        * CELL_HEIGHT;

        float scale =
                Math.min(
                        monitorWidth
                                / textPixelWidth,
                        monitorHeight
                                / textPixelHeight
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
