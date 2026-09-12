package com.k1ngtle.vsia.phone.browser;

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

        int logicalY = 8;
        List<LinkRegion> links = new ArrayList<>();

        for (PhoneHtmlDocument.Block block : document.blocks()) {
            if (block.kind() == PhoneHtmlDocument.Kind.HR) {
                int screenY = y + logicalY - scrollY;

                graphics.fill(
                        x + 10,
                        screenY,
                        x + width - 10,
                        screenY + 1,
                        block.textColor()
                );

                logicalY += 10;
                continue;
            }

            if (block.kind() == PhoneHtmlDocument.Kind.IMAGE) {
                int screenY = y + logicalY - scrollY;

                graphics.fill(
                        x + 10,
                        screenY,
                        x + width - 10,
                        screenY + 52,
                        0xFFE5E5EA
                );

                graphics.drawCenteredString(
                        font,
                        block.text(),
                        x + width / 2,
                        screenY + 22,
                        0xFF636366
                );

                logicalY += 60;
                continue;
            }

            float scale = Math.max(
                    0.7F,
                    Math.min(2.0F, block.scale())
            );

            int horizontalPadding =
                    block.kind() == PhoneHtmlDocument.Kind.BUTTON
                            ? 18
                            : 10;

            int maxUnscaledWidth = Math.max(
                    1,
                    (int) ((width - horizontalPadding * 2) / scale)
            );

            List<FormattedCharSequence> lines = font.split(
                    Component.literal(block.text()),
                    maxUnscaledWidth
            );

            if (lines.isEmpty()) {
                continue;
            }

            int lineHeight = font.lineHeight + 2;

            int textHeight = Math.max(
                    1,
                    Math.round(lines.size() * lineHeight * scale)
            );

            int verticalPadding =
                    block.kind() == PhoneHtmlDocument.Kind.BUTTON
                            ? 7
                            : 2;

            int blockHeight = textHeight + verticalPadding * 2;
            int screenY = y + logicalY - scrollY;

            if (block.backgroundColor() != 0) {
                graphics.fill(
                        x + 7,
                        screenY,
                        x + width - 7,
                        screenY + blockHeight,
                        block.backgroundColor()
                );
            }

            int textStartY = screenY + verticalPadding;

            graphics.pose().pushPose();
            graphics.pose().scale(scale, scale, 1.0F);

            int scaledBaseX = Math.round((x + horizontalPadding) / scale);
            int scaledY = Math.round(textStartY / scale);

            for (int i = 0; i < lines.size(); i++) {
                FormattedCharSequence line = lines.get(i);

                int lineWidth = font.width(line);
                int available = maxUnscaledWidth;

                int offset = switch (block.align()) {
                    case CENTER -> Math.max(0, (available - lineWidth) / 2);
                    case RIGHT -> Math.max(0, available - lineWidth);
                    default -> 0;
                };

                graphics.drawString(
                        font,
                        line,
                        scaledBaseX + offset,
                        scaledY + i * lineHeight,
                        block.textColor(),
                        false
                );
            }

            graphics.pose().popPose();

            if ((block.kind() == PhoneHtmlDocument.Kind.LINK
                    || block.kind() == PhoneHtmlDocument.Kind.BUTTON)
                    && !block.href().isBlank()) {
                links.add(
                        new LinkRegion(
                                x + 7,
                                screenY,
                                width - 14,
                                blockHeight,
                                block.href()
                        )
                );
            }

            logicalY += blockHeight + marginAfter(block.tag());
        }

        graphics.disableScissor();

        return new RenderResult(
                logicalY + 8,
                List.copyOf(links)
        );
    }

    private static int marginAfter(String tag) {
        return switch (tag) {
            case "h1" -> 12;
            case "h2", "h3" -> 9;
            case "button" -> 9;
            case "div", "section", "article" -> 5;
            default -> 6;
        };
    }

    public record LinkRegion(
            int x,
            int y,
            int width,
            int height,
            String href
    ) {
        public boolean contains(double mouseX, double mouseY) {
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
            return new RenderResult(0, List.of());
        }
    }
}
