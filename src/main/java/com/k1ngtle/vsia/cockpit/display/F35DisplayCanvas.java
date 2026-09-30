package com.k1ngtle.vsia.cockpit.display;

import com.k1ngtle.vsia.cockpit.client.F35DisplayClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public final class F35DisplayCanvas {
    private final PoseStack poseStack;
    private final MultiBufferSource buffers;

    private float clipMinX =
            Float.NEGATIVE_INFINITY;

    private float clipMinY =
            Float.NEGATIVE_INFINITY;

    private float clipMaxX =
            Float.POSITIVE_INFINITY;

    private float clipMaxY =
            Float.POSITIVE_INFINITY;

    public F35DisplayCanvas(
            PoseStack poseStack,
            MultiBufferSource buffers
    ) {
        this.poseStack =
                poseStack;

        this.buffers =
                buffers;
    }

    public void setClip(
            float minX,
            float minY,
            float maxX,
            float maxY
    ) {
        clipMinX =
                Math.min(
                        minX,
                        maxX
                );

        clipMinY =
                Math.min(
                        minY,
                        maxY
                );

        clipMaxX =
                Math.max(
                        minX,
                        maxX
                );

        clipMaxY =
                Math.max(
                        minY,
                        maxY
                );
    }

    public void clearClip() {
        clipMinX =
                Float.NEGATIVE_INFINITY;

        clipMinY =
                Float.NEGATIVE_INFINITY;

        clipMaxX =
                Float.POSITIVE_INFINITY;

        clipMaxY =
                Float.POSITIVE_INFINITY;
    }

    public void line(
            float x1,
            float y1,
            float x2,
            float y2,
            int color
    ) {
        float[] clipped =
                clipLine(
                        x1,
                        y1,
                        x2,
                        y2
                );

        if (clipped == null) {
            return;
        }

        VertexConsumer consumer =
                buffers.getBuffer(
                        RenderType.lines()
                );

        int adjustedColor =
                F35DisplayClientConfig.applyBrightness(
                        color
                );

        int alpha =
                adjustedColor >>> 24
                        & 0xFF;

        int red =
                adjustedColor >>> 16
                        & 0xFF;

        int green =
                adjustedColor >>> 8
                        & 0xFF;

        int blue =
                adjustedColor
                        & 0xFF;

        PoseStack.Pose pose =
                poseStack.last();

        consumer.vertex(
                        pose.pose(),
                        clipped[0],
                        clipped[1],
                        0.0F
                )
                .color(
                        red,
                        green,
                        blue,
                        alpha
                )
                .normal(
                        pose.normal(),
                        0.0F,
                        0.0F,
                        1.0F
                )
                .endVertex();

        consumer.vertex(
                        pose.pose(),
                        clipped[2],
                        clipped[3],
                        0.0F
                )
                .color(
                        red,
                        green,
                        blue,
                        alpha
                )
                .normal(
                        pose.normal(),
                        0.0F,
                        0.0F,
                        1.0F
                )
                .endVertex();
    }

    public void rect(
            float x,
            float y,
            float width,
            float height,
            int color
    ) {
        line(
                x,
                y,
                x + width,
                y,
                color
        );

        line(
                x + width,
                y,
                x + width,
                y + height,
                color
        );

        line(
                x + width,
                y + height,
                x,
                y + height,
                color
        );

        line(
                x,
                y + height,
                x,
                y,
                color
        );
    }

    public void cross(
            float x,
            float y,
            float radius,
            int color
    ) {
        line(
                x - radius,
                y,
                x + radius,
                y,
                color
        );

        line(
                x,
                y - radius,
                x,
                y + radius,
                color
        );
    }

    public void circle(
            float centerX,
            float centerY,
            float radius,
            int color
    ) {
        arc(
                centerX,
                centerY,
                radius,
                0.0F,
                360.0F,
                48,
                color
        );
    }

    public void arc(
            float centerX,
            float centerY,
            float radius,
            float startDegrees,
            float endDegrees,
            int segments,
            int color
    ) {
        int count =
                Math.max(
                        2,
                        segments
                );

        double start =
                Math.toRadians(
                        startDegrees
                );

        double end =
                Math.toRadians(
                        endDegrees
                );

        float lastX =
                centerX
                        + (float) Math.cos(
                        start
                )
                        * radius;

        float lastY =
                centerY
                        + (float) Math.sin(
                        start
                )
                        * radius;

        for (int i = 1;
             i <= count;
             i++) {
            double t =
                    start
                            + (
                            end - start
                    )
                            * (
                            i
                                    / (double) count
                    );

            float x =
                    centerX
                            + (float) Math.cos(
                            t
                    )
                            * radius;

            float y =
                    centerY
                            + (float) Math.sin(
                            t
                    )
                            * radius;

            line(
                    lastX,
                    lastY,
                    x,
                    y,
                    color
            );

            lastX =
                    x;

            lastY =
                    y;
        }
    }

    public void triangle(
            float centerX,
            float centerY,
            float radius,
            int color
    ) {
        line(
                centerX,
                centerY - radius,
                centerX - radius,
                centerY + radius,
                color
        );

        line(
                centerX - radius,
                centerY + radius,
                centerX + radius,
                centerY + radius,
                color
        );

        line(
                centerX + radius,
                centerY + radius,
                centerX,
                centerY - radius,
                color
        );
    }

    public void diamond(
            float centerX,
            float centerY,
            float radius,
            int color
    ) {
        line(
                centerX,
                centerY - radius,
                centerX + radius,
                centerY,
                color
        );

        line(
                centerX + radius,
                centerY,
                centerX,
                centerY + radius,
                color
        );

        line(
                centerX,
                centerY + radius,
                centerX - radius,
                centerY,
                color
        );

        line(
                centerX - radius,
                centerY,
                centerX,
                centerY - radius,
                color
        );
    }

    public void aircraft(
            float centerX,
            float centerY,
            float size,
            int color
    ) {
        line(
                centerX,
                centerY - size,
                centerX,
                centerY + size,
                color
        );

        line(
                centerX - size
                        * 0.75F,
                centerY,
                centerX + size
                        * 0.75F,
                centerY,
                color
        );

        line(
                centerX - size
                        * 0.35F,
                centerY + size
                        * 0.45F,
                centerX,
                centerY + size
                        * 0.2F,
                color
        );

        line(
                centerX,
                centerY + size
                        * 0.2F,
                centerX + size
                        * 0.35F,
                centerY + size
                        * 0.45F,
                color
        );
    }

    public void text(
            String text,
            float x,
            float y,
            float scale,
            int color
    ) {
        F35AvionicsFont.draw(
                this,
                text,
                x,
                y,
                scale,
                color
        );
    }

    private float[] clipLine(
            float x1,
            float y1,
            float x2,
            float y2
    ) {
        float dx =
                x2 - x1;

        float dy =
                y2 - y1;

        float t0 =
                0.0F;

        float t1 =
                1.0F;

        float[] p =
                new float[]{
                        -dx,
                        dx,
                        -dy,
                        dy
                };

        float[] q =
                new float[]{
                        x1 - clipMinX,
                        clipMaxX - x1,
                        y1 - clipMinY,
                        clipMaxY - y1
                };

        for (int i = 0;
             i < 4;
             i++) {
            if (Math.abs(
                    p[i]
            )
                    < 1.0E-6F) {
                if (q[i]
                        < 0.0F) {
                    return null;
                }

                continue;
            }

            float r =
                    q[i]
                            / p[i];

            if (p[i]
                    < 0.0F) {
                if (r
                        > t1) {
                    return null;
                }

                t0 =
                        Math.max(
                                t0,
                                r
                        );
            } else {
                if (r
                        < t0) {
                    return null;
                }

                t1 =
                        Math.min(
                                t1,
                                r
                        );
            }
        }

        return new float[]{
                x1 + t0
                        * dx,
                y1 + t0
                        * dy,
                x1 + t1
                        * dx,
                y1 + t1
                        * dy
        };
    }
}
