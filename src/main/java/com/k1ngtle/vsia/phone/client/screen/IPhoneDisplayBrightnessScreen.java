package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneSystemSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneDisplayBrightnessScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int BLUE = 0xFF0A84FF;
    private static final int DIVIDER = 0xFF3A3A3C;

    private int contentX;
    private int contentWidth;
    private int brightnessY;
    private int textY;

    public IPhoneDisplayBrightnessScreen() {
        super(Component.literal("Display & Brightness"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        brightnessY = phoneY + 88;
        textY = brightnessY + 92;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderPhoneShell(graphics, BG);
        renderStatusBar(graphics);
        renderHeader(
                graphics,
                "Settings",
                "Display & Brightness"
        );

        beginPhoneClip(graphics, 68);

        drawUiText(
                graphics,
                "BRIGHTNESS",
                contentX + 4,
                brightnessY - 16,
                MUTED
        );

        roundedRect(
                graphics,
                contentX,
                brightnessY,
                contentWidth,
                58,
                14,
                CARD
        );

        drawUiText(
                graphics,
                "☀",
                contentX + 13,
                brightnessY + 31,
                TEXT
        );

        drawBrightnessSlider(
                graphics,
                contentX + 31,
                brightnessY + 34,
                contentWidth - 62
        );

        drawUiText(
                graphics,
                "☀",
                contentX + contentWidth - 23,
                brightnessY + 31,
                TEXT
        );

        roundedRect(
                graphics,
                contentX,
                textY,
                contentWidth,
                76,
                14,
                CARD
        );

        navRow(
                graphics,
                textY,
                "Display & Text Size",
                ""
        );

        graphics.fill(
                contentX + 13,
                textY + 38,
                contentX + contentWidth - 13,
                textY + 39,
                DIVIDER
        );

        navRow(
                graphics,
                textY + 38,
                "Text Size",
                ""
        );

        drawUiWrappedCentered(
                graphics,
                "Brightness affects the whole simulated phone display.",
                phoneX + PHONE_WIDTH / 2,
                textY + 94,
                PHONE_WIDTH - 50,
                11,
                3,
                MUTED
        );

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    private void navRow(
            GuiGraphics graphics,
            int y,
            String label,
            String summary
    ) {
        drawUiText(
                graphics,
                label,
                contentX + 13,
                y + 14,
                TEXT
        );

        if (summary != null
                && !summary.isBlank()) {
            drawUiText(
                    graphics,
                    summary,
                    contentX
                            + contentWidth
                            - uiWidth(summary)
                            - 22,
                    y + 14,
                    MUTED
            );
        }

        drawUiText(
                graphics,
                "›",
                contentX + contentWidth - 11,
                y + 14,
                MUTED
        );
    }

    private void drawBrightnessSlider(
            GuiGraphics graphics,
            int x,
            int y,
            int width
    ) {
        graphics.fill(
                x,
                y,
                x + width,
                y + 3,
                0xFF55555A
        );

        int filled = Math.round(
                PhoneSystemSettings.brightness()
                        * width
        );

        graphics.fill(
                x,
                y,
                x + filled,
                y + 3,
                0xFFFFFFFF
        );

        roundedRect(
                graphics,
                x + filled - 4,
                y - 3,
                8,
                9,
                4,
                0xFFFFFFFF
        );
    }

    private void setBrightnessFromMouse(
            double mouseX
    ) {
        int left = contentX + 31;
        int width = contentWidth - 62;

        float value = (float) (
                (mouseX - left)
                        / width
        );

        PhoneSystemSettings.setBrightness(
                value
        );
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            if (clickedBack(mouseX, mouseY)) {
                minecraft.setScreen(
                        new IPhoneSettingsScreen()
                );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX + 24,
                    brightnessY + 20,
                    contentWidth - 48,
                    30
            )) {
                setBrightnessFromMouse(
                        mouseX
                );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    textY,
                    contentWidth,
                    38
            )) {
                minecraft.setScreen(
                        new IPhoneDisplayTextSizeScreen(
                                true
                        )
                );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    textY + 38,
                    contentWidth,
                    38
            )) {
                minecraft.setScreen(
                        new IPhoneDisplayTextSizeScreen(
                                true
                        )
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

    @Override
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        if (button == 0
                && inside(
                mouseX,
                mouseY,
                contentX + 24,
                brightnessY + 20,
                contentWidth - 48,
                30
        )) {
            setBrightnessFromMouse(
                    mouseX
            );
            return true;
        }

        return super.mouseDragged(
                mouseX,
                mouseY,
                button,
                dragX,
                dragY
        );
    }
}
