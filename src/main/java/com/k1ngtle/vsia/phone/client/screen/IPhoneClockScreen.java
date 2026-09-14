package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneClockClientState;
import com.k1ngtle.vsia.phone.client.PhoneLocaleSettings;
import com.k1ngtle.vsia.phone.client.PhoneSystemSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneClockScreen extends IPhoneScreen {
    private static final int BG = 0xFF000000;
    private static final int CARD = 0xFF1C1C1E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF8E8E93;
    private static final int ORANGE = 0xFFFF9F0A;
    private static final int GREEN = 0xFF30D158;
    private static final int RED = 0xFFFF453A;

    private int contentX;
    private int contentWidth;
    private int clockY;
    private int stopwatchY;
    private int leftButtonX;
    private int rightButtonX;

    public IPhoneClockScreen() {
        super(Component.literal("Clock"));
    }

    @Override
    protected void init() {
        super.init();

        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        clockY = phoneY + 82;
        stopwatchY = phoneY + 220;
        leftButtonX = contentX + 14;
        rightButtonX = contentX + contentWidth - 74;
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

        drawUiCentered(
                graphics,
                "Clock",
                phoneX + PHONE_WIDTH / 2,
                phoneY + 51,
                TEXT
        );

        beginPhoneClip(graphics, 68);

        roundedRect(
                graphics,
                contentX,
                clockY,
                contentWidth,
                110,
                14,
                CARD
        );

        drawUiText(
                graphics,
                "WORLD CLOCK",
                contentX + 13,
                clockY + 10,
                MUTED
        );

        drawUiCentered(
                graphics,
                PhoneLocaleSettings.formatTime(
                        PhoneLocaleSettings.currentTime(),
                        PhoneSystemSettings.use24HourTime()
                ),
                phoneX + PHONE_WIDTH / 2,
                clockY + 36,
                ORANGE
        );

        drawUiCentered(
                graphics,
                PhoneLocaleSettings.formatDate(
                        PhoneLocaleSettings.currentDate()
                ),
                phoneX + PHONE_WIDTH / 2,
                clockY + 60,
                TEXT
        );

        drawUiCentered(
                graphics,
                PhoneLocaleSettings.timeZoneId(),
                phoneX + PHONE_WIDTH / 2,
                clockY + 81,
                MUTED
        );

        drawUiText(
                graphics,
                "STOPWATCH",
                contentX + 4,
                stopwatchY - 16,
                MUTED
        );

        roundedRect(
                graphics,
                contentX,
                stopwatchY,
                contentWidth,
                126,
                14,
                CARD
        );

        drawUiCentered(
                graphics,
                PhoneClockClientState.formattedElapsed(),
                phoneX + PHONE_WIDTH / 2,
                stopwatchY + 26,
                TEXT
        );

        roundedRect(
                graphics,
                leftButtonX,
                stopwatchY + 70,
                60,
                32,
                16,
                0xFF3A3A3C
        );

        drawUiCentered(
                graphics,
                "Reset",
                leftButtonX + 30,
                stopwatchY + 81,
                MUTED
        );

        roundedRect(
                graphics,
                rightButtonX,
                stopwatchY + 70,
                60,
                32,
                16,
                PhoneClockClientState.running()
                        ? 0xFF4A1E20
                        : 0xFF12361F
        );

        drawUiCentered(
                graphics,
                PhoneClockClientState.running()
                        ? "Stop"
                        : "Start",
                rightButtonX + 30,
                stopwatchY + 81,
                PhoneClockClientState.running()
                        ? RED
                        : GREEN
        );

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            if (inside(
                    mouseX,
                    mouseY,
                    leftButtonX,
                    stopwatchY + 70,
                    60,
                    32
            )) {
                PhoneClockClientState.reset();
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    rightButtonX,
                    stopwatchY + 70,
                    60,
                    32
            )) {
                if (PhoneClockClientState.running()) {
                    PhoneClockClientState.stop();
                } else {
                    PhoneClockClientState.start();
                }

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
