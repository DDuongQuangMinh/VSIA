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
        graphics.fill(x, y, x + width, y + height, document.backgroundColor());
        graphics.enableScissor(x, y, x + width, y + height);

        int logicalY = 9;
        List<LinkRegion> links = new ArrayList<>();

        for (PhoneHtmlDocument.Block block : document.blocks()) {
            if (block.kind() == PhoneHtmlDocument.Kind.HR) {
                int screenY = y + logicalY - scrollY;
                graphics.fill(x + 12, screenY, x + width - 12, screenY + 1, 0xFFD1D1D6);
                logicalY += 11;
                continue;
            }

            if (block.kind() == PhoneHtmlDocument.Kind.IMAGE) {
                int screenY = y + logicalY - scrollY;
                roundedRect(graphics, x + 10, screenY, width - 20, 62, 9,
                        block.backgroundColor() != 0 ? block.backgroundColor() : 0xFFE5E5EA);
                graphics.drawCenteredString(font, block.text(), x + width / 2, screenY + 27, block.textColor());
                logicalY += 72;
                continue;
            }

            float scale = Math.max(0.70F, Math.min(2.0F, block.scale()));
            int horizontalPadding = block.kind() == PhoneHtmlDocument.Kind.BUTTON ? 18 : 11;
            int verticalPadding = block.kind() == PhoneHtmlDocument.Kind.BUTTON ? 8 : 3;
            int maxUnscaledWidth = Math.max(1, (int) ((width - horizontalPadding * 2) / scale));

            List<FormattedCharSequence> lines = font.split(Component.literal(block.text()), maxUnscaledWidth);
            if (lines.isEmpty()) continue;

            int lineHeight = font.lineHeight + 2;
            int textHeight = Math.max(1, Math.round(lines.size() * lineHeight * scale));
            int blockHeight = textHeight + verticalPadding * 2;
            int screenY = y + logicalY - scrollY;

            if (block.backgroundColor() != 0) {
                int radius = block.kind() == PhoneHtmlDocument.Kind.BUTTON ? 9 : 8;
                roundedRect(graphics, x + 7, screenY, width - 14, blockHeight, radius, block.backgroundColor());
            }

            int textStartY = screenY + verticalPadding;
            graphics.pose().pushPose();
            graphics.pose().scale(scale, scale, 1.0F);

            int scaledBaseX = Math.round((x + horizontalPadding) / scale);
            int scaledY = Math.round(textStartY / scale);
            boolean bold = block.tag().equals("h1") || block.tag().equals("h2") || block.tag().equals("h3") || block.tag().equals("button");

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
                graphics.drawString(font, line, drawX, drawY, block.textColor(), false);
                if (bold) {
                    graphics.drawString(font, line, drawX + 1, drawY, block.textColor(), false);
                }
            }

            graphics.pose().popPose();

            if ((block.kind() == PhoneHtmlDocument.Kind.LINK || block.kind() == PhoneHtmlDocument.Kind.BUTTON)
                    && !block.href().isBlank()) {
                links.add(new LinkRegion(x + 7, screenY, width - 14, blockHeight, block.href()));
            }

            logicalY += blockHeight + marginAfter(block.tag());
        }

        graphics.disableScissor();
        return new RenderResult(logicalY + 9, List.copyOf(links));
    }

    private static int marginAfter(String tag) {
        return switch (tag) {
            case "h1" -> 12;
            case "h2", "h3" -> 9;
            case "button" -> 10;
            case "div", "section", "article", "header", "footer", "main" -> 6;
            default -> 6;
        };
    }

    private static void roundedRect(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        int safeRadius = Math.max(0, Math.min(radius, Math.min(width / 2, height / 2)));
        if (safeRadius == 0) {
            graphics.fill(x, y, x + width, y + height, color);
            return;
        }

        graphics.fill(x + safeRadius, y, x + width - safeRadius, y + height, color);
        graphics.fill(x, y + safeRadius, x + width, y + height - safeRadius, color);

        for (int i = 0; i < safeRadius; i++) {
            int dy = safeRadius - i;
            int inset = (int) Math.ceil(safeRadius - Math.sqrt(Math.max(0, safeRadius * safeRadius - dy * dy)));
            graphics.fill(x + inset, y + i, x + width - inset, y + i + 1, color);
            graphics.fill(x + inset, y + height - i - 1, x + width - inset, y + height - i, color);
        }
    }

    public record LinkRegion(int x, int y, int width, int height, String href) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    public record RenderResult(int totalHeight, List<LinkRegion> links) {
        public static RenderResult empty() {
            return new RenderResult(0, List.of());
        }
    }
}
