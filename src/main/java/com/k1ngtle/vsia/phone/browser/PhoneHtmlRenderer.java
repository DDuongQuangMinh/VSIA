package com.k1ngtle.vsia.phone.browser;

import com.k1ngtle.vsia.phone.client.PhoneText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class PhoneHtmlRenderer {
    private static final int COLOR_TEXT_PRIMARY = 0xFF111111;
    private static final int COLOR_TEXT_SECONDARY = 0xFF56575C;
    private static final int COLOR_TEXT_TERTIARY = 0xFF8E8E93;
    private static final int COLOR_LINK = 0xFF007AFF;
    private static final int COLOR_BUTTON = 0xFF007AFF;
    private static final int COLOR_BUTTON_TEXT = 0xFFFFFFFF;
    private static final int COLOR_CARD = 0xFFFFFFFF;
    private static final int COLOR_SOFT = 0xFFF2F2F7;
    private static final int COLOR_LINE = 0xFFD8D8DE;
    private static final int COLOR_CODE = 0xFFEDEDF3;

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
                document.backgroundColor() == 0 ? 0xFFFFFFFF : document.backgroundColor()
        );

        graphics.enableScissor(x, y, x + width, y + height);

        int logicalY = 12;
        List<LinkRegion> links = new ArrayList<>();

        for (PhoneHtmlDocument.Block block : document.blocks()) {
            logicalY += marginBefore(block.tag());

            if (block.kind() == PhoneHtmlDocument.Kind.HR) {
                int screenY = y + logicalY - scrollY;
                graphics.fill(x + 16, screenY, x + width - 16, screenY + 1, COLOR_LINE);
                logicalY += 10;
                continue;
            }

            if (block.kind() == PhoneHtmlDocument.Kind.IMAGE) {
                int screenY = y + logicalY - scrollY;
                int imageHeight = 76;

                roundedRect(
                        graphics,
                        x + 12,
                        screenY,
                        width - 24,
                        imageHeight,
                        14,
                        block.backgroundColor() != 0 ? block.backgroundColor() : COLOR_SOFT
                );

                PhoneText.drawCentered(
                        graphics,
                        font,
                        block.text().isBlank() ? "Image" : block.text(),
                        x + width / 2,
                        screenY + imageHeight / 2 - 4,
                        block.textColor() == 0 ? COLOR_TEXT_SECONDARY : block.textColor()
                );

                logicalY += imageHeight + marginAfter(block.tag());
                continue;
            }

            float scale = Math.max(0.72F, Math.min(2.0F, block.scale()));
            int outerLeft = x + 12;
            int outerWidth = width - 24;

            int horizontalPadding = horizontalPadding(block);
            int verticalPadding = verticalPadding(block);
            int backgroundColor = backgroundFor(block);

            int maxUnscaledWidth = Math.max(
                    1,
                    (int) ((outerWidth - horizontalPadding * 2) / scale)
            );

            Component styled = PhoneText.component(block.text());
            List<FormattedCharSequence> lines = font.split(styled, maxUnscaledWidth);

            if (lines.isEmpty()) {
                logicalY += marginAfter(block.tag());
                continue;
            }

            int lineHeight = font.lineHeight + 3;
            int textHeight = Math.max(1, Math.round(lines.size() * lineHeight * scale));
            int blockHeight = textHeight + verticalPadding * 2;

            if (block.kind() == PhoneHtmlDocument.Kind.BUTTON) {
                blockHeight = Math.max(blockHeight, 34);
            }

            int screenY = y + logicalY - scrollY;

            if (backgroundColor != 0) {
                roundedRect(
                        graphics,
                        outerLeft,
                        screenY,
                        outerWidth,
                        blockHeight,
                        radiusFor(block),
                        backgroundColor
                );
            }

            if (showBorder(block)) {
                drawBorder(
                        graphics,
                        outerLeft,
                        screenY,
                        outerWidth,
                        blockHeight,
                        borderColorFor(block)
                );
            }

            int textStartY = screenY + verticalPadding;

            graphics.pose().pushPose();
            graphics.pose().scale(scale, scale, 1.0F);

            int scaledBaseX = Math.round((outerLeft + horizontalPadding) / scale);
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

                int drawX = scaledBaseX + offset;
                int drawY = scaledY + i * lineHeight;

                int textColor = resolveTextColor(block);

                graphics.drawString(font, line, drawX, drawY, textColor, false);

                if (isBold(block)) {
                    graphics.drawString(font, line, drawX + 1, drawY, textColor, false);
                }
            }

            graphics.pose().popPose();

            if ((block.kind() == PhoneHtmlDocument.Kind.LINK || block.kind() == PhoneHtmlDocument.Kind.BUTTON)
                    && !block.href().isBlank()) {
                links.add(new LinkRegion(outerLeft, screenY, outerWidth, blockHeight, block.href()));
            }

            logicalY += blockHeight + marginAfter(block.tag());
        }

        graphics.disableScissor();

        return new RenderResult(logicalY + 14, List.copyOf(links));
    }

    private static int resolveTextColor(PhoneHtmlDocument.Block block) {
        if (block.kind() == PhoneHtmlDocument.Kind.BUTTON) {
            return COLOR_BUTTON_TEXT;
        }

        if (block.kind() == PhoneHtmlDocument.Kind.LINK) {
            return block.textColor() == 0 ? COLOR_LINK : block.textColor();
        }

        if (block.textColor() != 0) {
            return block.textColor();
        }

        return switch (block.tag()) {
            case "h1", "h2", "h3" -> COLOR_TEXT_PRIMARY;
            case "small" -> COLOR_TEXT_TERTIARY;
            default -> COLOR_TEXT_SECONDARY;
        };
    }

    private static int backgroundFor(PhoneHtmlDocument.Block block) {
        if (block.backgroundColor() != 0) {
            return block.backgroundColor();
        }

        if (block.kind() == PhoneHtmlDocument.Kind.BUTTON) {
            return COLOR_BUTTON;
        }

        return switch (block.tag()) {
            case "div", "section", "article", "header", "footer", "main" -> COLOR_CARD;
            case "pre" -> COLOR_CODE;
            default -> 0;
        };
    }

    private static int horizontalPadding(PhoneHtmlDocument.Block block) {
        if (block.kind() == PhoneHtmlDocument.Kind.BUTTON) {
            return 18;
        }

        return switch (block.tag()) {
            case "div", "section", "article", "header", "footer", "main" -> 14;
            case "pre" -> 10;
            default -> 8;
        };
    }

    private static int verticalPadding(PhoneHtmlDocument.Block block) {
        if (block.kind() == PhoneHtmlDocument.Kind.BUTTON) {
            return 9;
        }

        return switch (block.tag()) {
            case "div", "section", "article", "header", "footer", "main" -> 10;
            case "pre" -> 8;
            default -> 2;
        };
    }

    private static int radiusFor(PhoneHtmlDocument.Block block) {
        if (block.kind() == PhoneHtmlDocument.Kind.BUTTON) {
            return 12;
        }

        return switch (block.tag()) {
            case "div", "section", "article", "header", "footer", "main" -> 14;
            case "pre" -> 10;
            default -> 0;
        };
    }

    private static boolean showBorder(PhoneHtmlDocument.Block block) {
        return switch (block.tag()) {
            case "div", "section", "article", "header", "footer", "main", "pre" -> true;
            default -> false;
        };
    }

    private static int borderColorFor(PhoneHtmlDocument.Block block) {
        return block.tag().equals("pre") ? 0xFFDCDCE3 : 0xFFE8E8EF;
    }

    private static boolean isBold(PhoneHtmlDocument.Block block) {
        if (block.kind() == PhoneHtmlDocument.Kind.BUTTON) {
            return true;
        }

        return switch (block.tag()) {
            case "h1", "h2", "h3", "strong" -> true;
            default -> false;
        };
    }

    private static int marginBefore(String tag) {
        return switch (tag) {
            case "h1" -> 6;
            case "h2" -> 5;
            case "h3" -> 4;
            case "section", "article", "header", "footer", "main", "div" -> 4;
            default -> 0;
        };
    }

    private static int marginAfter(String tag) {
        return switch (tag) {
            case "h1" -> 12;
            case "h2", "h3" -> 10;
            case "button" -> 10;
            case "div", "section", "article", "header", "footer", "main" -> 8;
            case "pre" -> 10;
            case "p" -> 7;
            default -> 6;
        };
    }

    private static void drawBorder(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
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
        int safeRadius = Math.max(0, Math.min(Math.min(radius, width / 2), height / 2));

        if (safeRadius <= 0) {
            graphics.fill(x, y, x + width, y + height, color);
            return;
        }

        graphics.fill(x + safeRadius, y, x + width - safeRadius, y + height, color);
        graphics.fill(x, y + safeRadius, x + width, y + height - safeRadius, color);

        for (int i = 0; i < safeRadius; i++) {
            int dy = safeRadius - i;
            int inset = (int) Math.ceil(
                    safeRadius - Math.sqrt(
                            Math.max(0, safeRadius * safeRadius - dy * dy)
                    )
            );

            graphics.fill(x + inset, y + i, x + width - inset, y + i + 1, color);
            graphics.fill(x + inset, y + height - i - 1, x + width - inset, y + height - i, color);
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
            return new RenderResult(0, List.of());
        }
    }
}
