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
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderPhoneShell(graphics, BACKGROUND);
        renderStatusBar(graphics);
        renderHeader(
                graphics,
                "Settings",
                "Accessibility"
        );

        graphics.enableScissor(
                phoneX + DISPLAY_INSET,
                viewportTop,
                phoneX + PHONE_WIDTH - DISPLAY_INSET,
                viewportBottom
        );

        renderScrollable(graphics);

        graphics.disableScissor();
        renderHomeIndicator(graphics);
    }

    private void renderScrollable(
            GuiGraphics graphics
    ) {
        int introY = sy(0);

        roundedRect(
                graphics,
                contentX,
                introY,
                contentWidth,
                64,
                14,
                CARD
        );

        drawUiWrappedCentered(
                graphics,
                "Personalize the phone for vision, mobility, hearing, speech, and cognition.",
                phoneX + PHONE_WIDTH / 2,
                introY + 10,
                contentWidth - 24,
                11,
                4,
                TEXT
        );

        drawUiCentered(
                graphics,
                "Learn more...",
                phoneX + PHONE_WIDTH / 2,
                introY + 49,
                BLUE
        );

        int visionLabelY = sy(82);

        drawUiText(
                graphics,
                "VISION",
                contentX + 4,
                visionLabelY,
                MUTED
        );

        int visionY = sy(96);

        drawGroup(
                graphics,
                visionY,
                7
        );

        drawRow(
                graphics,
                visionY,
                "VoiceOver",
                "Off",
                false
        );

        drawRow(
                graphics,
                visionY + ROW_HEIGHT,
                "Zoom",
                "Off",
                false
        );

        drawRow(
                graphics,
                visionY + ROW_HEIGHT * 2,
                "Hover Text",
                "Off",
                false
        );

        drawRow(
                graphics,
                visionY + ROW_HEIGHT * 3,
                "Display & Text Size",
                "",
                true
        );

        drawRow(
                graphics,
                visionY + ROW_HEIGHT * 4,
                "Motion",
                "",
                true
        );

        drawRow(
                graphics,
                visionY + ROW_HEIGHT * 5,
                "Read & Speak",
                "",
                true
        );

        drawRow(
                graphics,
                visionY + ROW_HEIGHT * 6,
                "Audio Descriptions",
                "Off",
                false
        );

        int physicalLabelY = sy(332);

        drawUiText(
                graphics,
                "PHYSICAL AND MOTOR",
                contentX + 4,
                physicalLabelY,
                MUTED
        );

        int physicalY = sy(346);

        drawGroup(
                graphics,
                physicalY,
                2
        );

        drawRow(
                graphics,
                physicalY,
                "Touch",
                "",
                true
        );

        drawRow(
                graphics,
                physicalY + ROW_HEIGHT,
                "Face ID & Attention",
                "",
                true
        );
    }

    private void drawGroup(
            GuiGraphics graphics,
            int y,
            int rows
    ) {
        roundedRect(
                graphics,
                contentX,
                y,
                contentWidth,
                rows * ROW_HEIGHT,
                14,
                CARD_SECONDARY
        );

        for (int i = 1; i < rows; i++) {
            int dividerY = y + ROW_HEIGHT * i;

            graphics.fill(
                    contentX + 12,
                    dividerY,
                    contentX + contentWidth - 12,
                    dividerY + 1,
                    DIVIDER
            );
        }
    }

    private void drawRow(
            GuiGraphics graphics,
            int y,
            String left,
            String right,
            boolean chevron
    ) {
        drawUiText(
                graphics,
                fitUi(
                        left,
                        contentWidth - 58
                ),
                contentX + 12,
                y + 11,
                TEXT
        );

        if (right != null
                && !right.isBlank()) {
            String shown = fitUi(
                    right,
                    62
            );

            drawUiText(
                    graphics,
                    shown,
                    contentX
                            + contentWidth
                            - uiWidth(shown)
                            - (chevron ? 18 : 12),
                    y + 11,
                    SECONDARY
            );
        }

        if (chevron) {
            drawUiText(
                    graphics,
                    "›",
                    contentX + contentWidth - 11,
                    y + 11,
                    MUTED
            );
        }
    }

    private int sy(
            int y
    ) {
        return viewportTop
                + y
                - scrollOffset;
    }

    private int clampScroll(
            int value
    ) {
        int max = Math.max(
                0,
                CONTENT_HEIGHT - viewportHeight
        );

        return Math.max(
                0,
                Math.min(
                        value,
                        max
                )
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {
        scrollOffset = clampScroll(
                scrollOffset
                        - (int) (
                        delta * 18
                )
        );

        return true;
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            if (clickedBack(
                    mouseX,
                    mouseY
            )) {
                minecraft.setScreen(
                        new IPhoneSettingsScreen()
                );
                return true;
            }

            int displayY =
                    sy(
                            96
                                    + ROW_HEIGHT * 3
                    );

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    displayY,
                    contentWidth,
                    ROW_HEIGHT
            )) {
                minecraft.setScreen(
                        new IPhoneDisplayTextSizeScreen()
                );
                return true;
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }
}
