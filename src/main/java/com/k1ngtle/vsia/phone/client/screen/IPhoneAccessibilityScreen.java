package com.k1ngtle.vsia.phone.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IPhoneAccessibilityScreen extends IPhoneScreen {
    private static final int BACKGROUND = 0xFF111216;
    private static final int CARD = 0xFF1E1F23;
    private static final int CARD_SECONDARY = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int SECONDARY = 0xFFAEAEB2;
    private static final int MUTED = 0xFF8E8E93;
    private static final int DIVIDER = 0xFF3A3A3C;
    private static final int BLUE = 0xFF0A84FF;
    private static final int ROW_HEIGHT = 32;
    private static final int CONTENT_HEIGHT = 420;

    private int contentX;
    private int contentWidth;
    private int viewportTop;
    private int viewportBottom;
    private int viewportHeight;
    private int scrollOffset;

    public IPhoneAccessibilityScreen() {
        super(Component.literal("Accessibility"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 12;
        contentWidth = PHONE_WIDTH - 24;
        viewportTop = phoneY + 70;
        viewportBottom = phoneY + PHONE_HEIGHT - 28;
        viewportHeight = viewportBottom - viewportTop;
        scrollOffset = clampScroll(scrollOffset);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(graphics, BACKGROUND);
        renderStatusBar(graphics);
        renderHeader(graphics, "Settings", "Accessibility");

        graphics.enableScissor(phoneX + 4, viewportTop, phoneX + PHONE_WIDTH - 4, viewportBottom);
        renderScrollable(graphics);
        graphics.disableScissor();

        renderHomeIndicator(graphics);
    }

    private void renderScrollable(GuiGraphics graphics) {
        int introY = sy(0);
        roundedRect(graphics, contentX, introY, contentWidth, 56, 14, CARD);
        graphics.drawString(font, "Personalize iPhone in ways that", contentX + 10, introY + 12, TEXT, false);
        graphics.drawString(font, "work best for vision, mobility,", contentX + 10, introY + 23, TEXT, false);
        graphics.drawString(font, "hearing, speech, and cognition.", contentX + 10, introY + 34, SECONDARY, false);
        graphics.drawString(font, "Learn more...", contentX + 94, introY + 44, BLUE, false);

        int visionLabelY = sy(74);
        graphics.drawString(font, "Vision", contentX + 4, visionLabelY, MUTED, false);
        int visionY = sy(88);
        drawGroup(graphics, visionY, 7);
        drawRow(graphics, visionY + ROW_HEIGHT * 0, "VoiceOver", "Off", false);
        drawRow(graphics, visionY + ROW_HEIGHT * 1, "Zoom", "Off", false);
        drawRow(graphics, visionY + ROW_HEIGHT * 2, "Hover Text", "Off", false);
        drawRow(graphics, visionY + ROW_HEIGHT * 3, "Display & Text Size", "", true);
        drawRow(graphics, visionY + ROW_HEIGHT * 4, "Motion", "", true);
        drawRow(graphics, visionY + ROW_HEIGHT * 5, "Read & Speak", "", true);
        drawRow(graphics, visionY + ROW_HEIGHT * 6, "Audio Descriptions", "Off", false);

        int physicalLabelY = sy(324);
        graphics.drawString(font, "Physical and Motor", contentX + 4, physicalLabelY, MUTED, false);
        int physicalY = sy(338);
        drawGroup(graphics, physicalY, 2);
        drawRow(graphics, physicalY + ROW_HEIGHT * 0, "Touch", "", true);
        drawRow(graphics, physicalY + ROW_HEIGHT * 1, "Face ID & Attention", "", true);
    }

    private void drawGroup(GuiGraphics graphics, int y, int rows) {
        roundedRect(graphics, contentX, y, contentWidth, rows * ROW_HEIGHT, 14, CARD_SECONDARY);
        for (int i = 1; i < rows; i++) {
            int dividerY = y + ROW_HEIGHT * i;
            graphics.fill(contentX + 12, dividerY, contentX + contentWidth - 12, dividerY + 1, DIVIDER);
        }
    }

    private void drawRow(GuiGraphics graphics, int y, String left, String right, boolean chevron) {
        graphics.drawString(font, left, contentX + 12, y + 11, TEXT, false);
        if (right != null && !right.isBlank()) {
            int rightWidth = font.width(right);
            graphics.drawString(font, right, contentX + contentWidth - rightWidth - (chevron ? 18 : 12), y + 11, SECONDARY, false);
        }
        if (chevron) {
            graphics.drawString(font, ">", contentX + contentWidth - 11, y + 11, MUTED, false);
        }
    }

    private int sy(int y) {
        return viewportTop + y - scrollOffset;
    }

    private int clampScroll(int value) {
        int max = Math.max(0, CONTENT_HEIGHT - viewportHeight);
        if (value < 0) {
            return 0;
        }
        return Math.min(value, max);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollOffset = clampScroll(scrollOffset - (int) (delta * 18));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (clickedBack(mouseX, mouseY)) {
                minecraft.setScreen(new IPhoneSettingsScreen());
                return true;
            }

            int displayY = sy(88 + ROW_HEIGHT * 3);
            if (inside(mouseX, mouseY, contentX, displayY, contentWidth, ROW_HEIGHT)) {
                minecraft.setScreen(new IPhoneDisplayTextSizeScreen());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
