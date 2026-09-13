package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneAccessibilityClientPreferences;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IPhoneDisplayTextSizeScreen extends IPhoneScreen {
    private static final int BACKGROUND = 0xFF111216;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int SECONDARY = 0xFFAEAEB2;
    private static final int MUTED = 0xFF8E8E93;
    private static final int DIVIDER = 0xFF3A3A3C;
    private static final int BLUE = 0xFF0A84FF;
    private static final int ROW_HEIGHT = 32;
    private static final int CONTENT_HEIGHT = 442;

    private final boolean fromDisplayBrightness;

    private int contentX;
    private int contentWidth;
    private int viewportTop;
    private int viewportBottom;
    private int viewportHeight;
    private int scrollOffset;

    public IPhoneDisplayTextSizeScreen() {
        this(false);
    }

    public IPhoneDisplayTextSizeScreen(
            boolean fromDisplayBrightness
    ) {
        super(Component.literal("Display & Text Size"));
        this.fromDisplayBrightness =
                fromDisplayBrightness;
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
                fromDisplayBrightness
                        ? "Display"
                        : "Accessibility",
                "Display & Text Size"
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
        int group1Y = sy(0);

        drawGroup(
                graphics,
                group1Y,
                2
        );

        drawToggleRow(
                graphics,
                group1Y,
                "Bold Text",
                PhoneAccessibilityClientPreferences.boldText()
        );

        drawNavigationSummaryRow(
                graphics,
                group1Y + ROW_HEIGHT,
                "Larger Text",
                PhoneAccessibilityClientPreferences.largerText()
                        ? "On"
                        : "Off"
        );

        int group2Y = sy(80);

        drawGroup(
                graphics,
                group2Y,
                2
        );

        drawToggleRow(
                graphics,
                group2Y,
                "Button Shapes",
                PhoneAccessibilityClientPreferences.buttonShapes()
        );

        drawToggleRow(
                graphics,
                group2Y + ROW_HEIGHT,
                "On/Off Labels",
                PhoneAccessibilityClientPreferences.onOffLabels()
        );

        drawUiText(
                graphics,
                "What's new in Display & Text Size...",
                contentX + 10,
                sy(149),
                BLUE
        );

        int group3Y = sy(174);

        drawGroup(
                graphics,
                group3Y,
                4
        );

        drawToggleRow(
                graphics,
                group3Y,
                "Reduce Transparency",
                PhoneAccessibilityClientPreferences.reduceTransparency()
        );

        drawToggleRow(
                graphics,
                group3Y + ROW_HEIGHT,
                "Increase Contrast",
                PhoneAccessibilityClientPreferences.increaseContrast()
        );

        drawToggleRow(
                graphics,
                group3Y + ROW_HEIGHT * 2,
                "Differentiate Without Color",
                PhoneAccessibilityClientPreferences.differentiateWithoutColor()
        );

        drawToggleRow(
                graphics,
                group3Y + ROW_HEIGHT * 3,
                "Prefer Horizontal Text",
                PhoneAccessibilityClientPreferences.preferHorizontalText()
        );

        drawUiWrappedCentered(
                graphics,
                "These options control the VS:IA phone interface accessibility state.",
                phoneX + PHONE_WIDTH / 2,
                sy(315),
                contentWidth - 24,
                11,
                3,
                MUTED
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
                CARD
        );

        for (int i = 1; i < rows; i++) {
            int dividerY =
                    y + ROW_HEIGHT * i;

            graphics.fill(
                    contentX + 12,
                    dividerY,
                    contentX + contentWidth - 12,
                    dividerY + 1,
                    DIVIDER
            );
        }
    }

    private void drawToggleRow(
            GuiGraphics graphics,
            int y,
            String left,
            boolean enabled
    ) {
        drawUiText(
                graphics,
                fitUi(
                        left,
                        contentWidth - 62
                ),
                contentX + 12,
                y + 11,
                TEXT
        );

        drawToggle(
                graphics,
                contentX + contentWidth - 42,
                y + 6,
                enabled
        );
    }

    private void drawNavigationSummaryRow(
            GuiGraphics graphics,
            int y,
            String left,
            String right
    ) {
        drawUiText(
                graphics,
                left,
                contentX + 12,
                y + 11,
                TEXT
        );

        drawUiText(
                graphics,
                right,
                contentX
                        + contentWidth
                        - uiWidth(right)
                        - 18,
                y + 11,
                SECONDARY
        );

        drawUiText(
                graphics,
                "›",
                contentX + contentWidth - 11,
                y + 11,
                MUTED
        );
    }

    private void drawToggle(
            GuiGraphics graphics,
            int x,
            int y,
            boolean enabled
    ) {
        int bg =
                enabled
                        ? 0xFF34C759
                        : 0xFF636366;

        roundedRect(
                graphics,
                x,
                y,
                32,
                18,
                9,
                bg
        );

        int knobX =
                enabled
                        ? x + 17
                        : x + 2;

        roundedRect(
                graphics,
                knobX,
                y + 2,
                13,
                14,
                7,
                0xFFFFFFFF
        );
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
        int max =
                Math.max(
                        0,
                        CONTENT_HEIGHT
                                - viewportHeight
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
        scrollOffset =
                clampScroll(
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
                        fromDisplayBrightness
                                ? new IPhoneDisplayBrightnessScreen()
                                : new IPhoneAccessibilityScreen()
                );
                return true;
            }

            if (toggleHit(
                    mouseX,
                    mouseY,
                    sy(0)
            )) {
                PhoneAccessibilityClientPreferences
                        .toggleBoldText();
                return true;
            }

            if (toggleHit(
                    mouseX,
                    mouseY,
                    sy(80)
            )) {
                PhoneAccessibilityClientPreferences
                        .toggleButtonShapes();
                return true;
            }

            if (toggleHit(
                    mouseX,
                    mouseY,
                    sy(80 + ROW_HEIGHT)
            )) {
                PhoneAccessibilityClientPreferences
                        .toggleOnOffLabels();
                return true;
            }

            if (toggleHit(
                    mouseX,
                    mouseY,
                    sy(174)
            )) {
                PhoneAccessibilityClientPreferences
                        .toggleReduceTransparency();
                return true;
            }

            if (toggleHit(
                    mouseX,
                    mouseY,
                    sy(174 + ROW_HEIGHT)
            )) {
                PhoneAccessibilityClientPreferences
                        .toggleIncreaseContrast();
                return true;
            }

            if (toggleHit(
                    mouseX,
                    mouseY,
                    sy(
                            174
                                    + ROW_HEIGHT * 2
                    )
            )) {
                PhoneAccessibilityClientPreferences
                        .toggleDifferentiateWithoutColor();
                return true;
            }

            if (toggleHit(
                    mouseX,
                    mouseY,
                    sy(
                            174
                                    + ROW_HEIGHT * 3
                    )
            )) {
                PhoneAccessibilityClientPreferences
                        .togglePreferHorizontalText();
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    sy(ROW_HEIGHT),
                    contentWidth,
                    ROW_HEIGHT
            )) {
                PhoneAccessibilityClientPreferences
                        .toggleLargerText();
                return true;
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    private boolean toggleHit(
            double mouseX,
            double mouseY,
            int rowY
    ) {
        return inside(
                mouseX,
                mouseY,
                contentX + contentWidth - 46,
                rowY + 4,
                38,
                24
        );
    }
}
