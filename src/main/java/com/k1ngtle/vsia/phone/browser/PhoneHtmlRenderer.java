package com.k1ngtle.vsia.phone.browser;

import com.k1ngtle.vsia.phone.client.PhoneText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class PhoneHtmlRenderer {
    private PhoneHtmlRenderer() {
    }

    public static RenderResult render(
            GuiGraphics graphics,
            Font font,
            PhoneHtmlDocument document,
            int x,
            int y,
            int width,
            int height,
            int scrollY
    ) {
        graphics.fill(
                x,
                y,
                x + width,
                y + height,
                document.backgroundColor()
        );

        graphics.enableScissor(
                x,
                y,
                x + width,
                y + height
        );

        int logicalY = 10;

        List<LinkRegion> links =
                new ArrayList<>();

        for (PhoneHtmlDocument.Block block
                : document.blocks()) {
            logicalY += marginBefore(
                    block.tag()
            );

            if (block.kind()
                    == PhoneHtmlDocument.Kind.HR) {
                int screenY =
                        y + logicalY - scrollY;

                graphics.fill(
                        x + 12,
                        screenY,
                        x + width - 12,
                        screenY + 1,
                        0xFFD1D1D6
                );

                logicalY += 9;
                continue;
            }

            if (block.kind()
                    == PhoneHtmlDocument.Kind.IMAGE) {
                int imageHeight = 68;
                int screenY =
                        y + logicalY - scrollY;

                int background =
                        block.backgroundColor() != 0
                                ? block.backgroundColor()
                                : 0xFFE5E5EA;

                roundedRect(
                        graphics,
                        x + 10,
                        screenY,
                        width - 20,
                        imageHeight,
                        10,
                        background
                );

                PhoneText.drawCentered(
                        graphics,
                        font,
                        block.text(),
                        x + width / 2,
                        screenY
                                + imageHeight / 2
                                - 4,
                        block.textColor()
                );

                logicalY +=
                        imageHeight
                                + marginAfter(
                                block.tag()
                        );

                continue;
            }

            float scale =
                    Math.max(
                            0.72F,
                            Math.min(
                                    2.1F,
                                    block.scale()
                            )
                    );

            int horizontalPadding =
                    horizontalPadding(
                            block
                    );

            int verticalPadding =
                    verticalPadding(
                            block
                    );

            int outerLeft =
                    x + 8;

            int outerWidth =
                    width - 16;

            int maxUnscaledWidth =
                    Math.max(
                            1,
                            (int) (
                                    (
                                            outerWidth
                                                    - horizontalPadding * 2
                                    )
                                            / scale
                            )
                    );

            Component styledText =
                    PhoneText.component(
                            block.text()
                    );

            List<FormattedCharSequence> lines =
                    font.split(
                            styledText,
                            maxUnscaledWidth
                    );

            if (lines.isEmpty()) {
                logicalY +=
                        marginAfter(
                                block.tag()
                        );

                continue;
            }

            int lineHeight =
                    font.lineHeight + 3;

            int textHeight =
                    Math.max(
                            1,
                            Math.round(
                                    lines.size()
                                            * lineHeight
                                            * scale
                            )
                    );

            int blockHeight =
                    textHeight
                            + verticalPadding * 2;

            if (block.kind()
                    == PhoneHtmlDocument.Kind.BUTTON) {
                blockHeight =
                        Math.max(
                                blockHeight,
                                30
                        );
            }

            int screenY =
                    y + logicalY - scrollY;

            int background =
                    resolvedBackground(
                            block
                    );

            if (background != 0) {
                roundedRect(
                        graphics,
                        outerLeft,
                        screenY,
                        outerWidth,
                        blockHeight,
                        radiusFor(
                                block
                        ),
                        background
                );
            }

            int textStartY =
                    screenY + verticalPadding;

            graphics.pose().pushPose();

            graphics.pose().scale(
                    scale,
                    scale,
                    1.0F
            );

            int scaledBaseX =
                    Math.round(
                            (
                                    outerLeft
                                            + horizontalPadding
                            )
                                    / scale
                    );

            int scaledY =
                    Math.round(
                            textStartY
                                    / scale
                    );

            for (int i = 0;
                 i < lines.size();
                 i++) {
                FormattedCharSequence line =
                        lines.get(i);

                int lineWidth =
                        font.width(line);

                int available =
                        maxUnscaledWidth;

                int offset =
                        switch (
                                block.align()
                                ) {
                            case CENTER ->
                                    Math.max(
                                            0,
                                            (
                                                    available
                                                            - lineWidth
                                            )
                                                    / 2
                                    );

                            case RIGHT ->
                                    Math.max(
                                            0,
                                            available
                                                    - lineWidth
                                    );

                            default -> 0;
                        };

                int drawX =
                        scaledBaseX + offset;

                int drawY =
                        scaledY
                                + i * lineHeight;

                graphics.drawString(
                        font,
                        line,
                        drawX,
                        drawY,
                        block.textColor(),
                        false
                );

                if (isBold(
                        block
                )) {
                    graphics.drawString(
                            font,
                            line,
                            drawX + 1,
                            drawY,
                            block.textColor(),
                            false
                    );
                }
            }

            graphics.pose().popPose();

            if ((
                    block.kind()
                            == PhoneHtmlDocument.Kind.LINK
                            || block.kind()
                            == PhoneHtmlDocument.Kind.BUTTON
            )
                    && !block.href().isBlank()) {
                links.add(
                        new LinkRegion(
                                outerLeft,
                                screenY,
                                outerWidth,
                                blockHeight,
                                block.href()
                        )
                );
            }

            logicalY +=
                    blockHeight
                            + marginAfter(
                            block.tag()
                    );
        }

        graphics.disableScissor();

        return new RenderResult(
                logicalY + 10,
                List.copyOf(links)
        );
    }

    private static int resolvedBackground(
            PhoneHtmlDocument.Block block
    ) {
        if (block.backgroundColor() != 0) {
            return block.backgroundColor();
        }

        if (block.kind()
                == PhoneHtmlDocument.Kind.BUTTON) {
            return 0xFF007AFF;
        }

        if ("pre".equals(
                block.tag()
        )) {
            return 0xFFF2F2F7;
        }

        return 0;
    }

    private static int horizontalPadding(
            PhoneHtmlDocument.Block block
    ) {
        if (block.kind()
                == PhoneHtmlDocument.Kind.BUTTON) {
            return 18;
        }

        return switch (
                block.tag()
                ) {
            case "div",
                 "section",
                 "article",
                 "header",
                 "footer",
                 "main" -> 12;

            case "pre" -> 10;

            default -> 10;
        };
    }

    private static int verticalPadding(
            PhoneHtmlDocument.Block block
    ) {
        if (block.kind()
                == PhoneHtmlDocument.Kind.BUTTON) {
            return 8;
        }

        if (block.backgroundColor() != 0) {
            return 7;
        }

        if ("pre".equals(
                block.tag()
        )) {
            return 7;
        }

        return 2;
    }

    private static int radiusFor(
            PhoneHtmlDocument.Block block
    ) {
        if (block.kind()
                == PhoneHtmlDocument.Kind.BUTTON) {
            return 9;
        }

        if ("pre".equals(
                block.tag()
        )) {
            return 7;
        }

        return block.backgroundColor() != 0
                ? 10
                : 0;
    }

    private static boolean isBold(
            PhoneHtmlDocument.Block block
    ) {
        if (block.kind()
                == PhoneHtmlDocument.Kind.BUTTON) {
            return true;
        }

        return switch (
                block.tag()
                ) {
            case "h1",
                 "h2",
                 "h3" -> true;

            default -> false;
        };
    }

    private static int marginBefore(
            String tag
    ) {
        return switch (tag) {
            case "h1" -> 6;
            case "h2" -> 5;
            case "h3" -> 4;
            case "section",
                 "article",
                 "header",
                 "footer",
                 "main",
                 "div" -> 3;
            default -> 0;
        };
    }

    private static int marginAfter(
            String tag
    ) {
        return switch (tag) {
            case "h1" -> 12;

            case "h2",
                 "h3" -> 9;

            case "button" -> 10;

            case "div",
                 "section",
                 "article",
                 "header",
                 "footer",
                 "main" -> 7;

            case "pre" -> 9;

            default -> 6;
        };
    }

    private static void roundedRect(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int color
    ) {
        int safeRadius =
                Math.max(
                        0,
                        Math.min(
                                Math.min(
                                        radius,
                                        width / 2
                                ),
                                height / 2
                        )
                );

        if (safeRadius <= 0) {
            graphics.fill(
                    x,
                    y,
                    x + width,
                    y + height,
                    color
            );

            return;
        }

        graphics.fill(
                x + safeRadius,
                y,
                x + width - safeRadius,
                y + height,
                color
        );

        graphics.fill(
                x,
                y + safeRadius,
                x + width,
                y + height - safeRadius,
                color
        );

        for (int i = 0;
             i < safeRadius;
             i++) {
            int dy =
                    safeRadius - i;

            int inset =
                    (int) Math.ceil(
                            safeRadius
                                    - Math.sqrt(
                                    Math.max(
                                            0,
                                            safeRadius
                                                    * safeRadius
                                                    - dy
                                                    * dy
                                    )
                            )
                    );

            graphics.fill(
                    x + inset,
                    y + i,
                    x + width - inset,
                    y + i + 1,
                    color
            );

            graphics.fill(
                    x + inset,
                    y + height - i - 1,
                    x + width - inset,
                    y + height - i,
                    color
            );
        }
    }

    public record LinkRegion(
            int x,
            int y,
            int width,
            int height,
            String href
    ) {
        public boolean contains(
                double mouseX,
                double mouseY
        ) {
            return mouseX >= x
                    && mouseX < x + width
                    && mouseY >= y
                    && mouseY < y + height;
        }
    }

    public record RenderResult(
            int totalHeight,
            List<LinkRegion> links
    ) {
        public static RenderResult empty() {
            return new RenderResult(
                    0,
                    List.of()
            );
        }
    }
}