package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneSystemSettings;
import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneBatteryScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int GREEN = 0xFF30D158;
    private static final int YELLOW = 0xFFFFD60A;
    private static final int DIVIDER = 0xFF3A3A3C;

    private int contentX;
    private int contentWidth;
    private int statusY;
    private int powerY;

    public IPhoneBatteryScreen() {
        super(Component.literal("Battery"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        statusY = phoneY + 82;
        powerY = statusY + 78;
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
        renderHeader(graphics, "Settings", "Battery");

        beginPhoneClip(graphics, 68);

        roundedRect(
                graphics,
                contentX,
                statusY,
                contentWidth,
                58,
                14,
                CARD
        );

        drawUiText(
                graphics,
                "Battery Level",
                contentX + 13,
                statusY + 10,
                TEXT
        );

        int percent = PhoneNetworkState.get()
                .getBatteryPercent();

        String value = percent + "%";

        drawUiText(
                graphics,
                value,
                contentX + contentWidth - uiWidth(value) - 13,
                statusY + 10,
                PhoneSystemSettings.lowPowerMode()
                        ? YELLOW
                        : GREEN
        );

        graphics.fill(
                contentX + 13,
                statusY + 34,
                contentX + contentWidth - 13,
                statusY + 39,
                0xFF48484C
        );

        int barWidth = Math.round(
                (contentWidth - 26)
                        * percent
                        / 100.0F
        );

        graphics.fill(
                contentX + 13,
                statusY + 34,
                contentX + 13 + barWidth,
                statusY + 39,
                PhoneSystemSettings.lowPowerMode()
                        ? YELLOW
                        : GREEN
        );

        roundedRect(
                graphics,
                contentX,
                powerY,
                contentWidth,
                76,
                14,
                CARD
        );

        drawUiText(
                graphics,
                "Low Power Mode",
                contentX + 13,
                powerY + 14,
                TEXT
        );

        drawToggle(
                graphics,
                contentX + contentWidth - 47,
                powerY + 9,
                PhoneSystemSettings.lowPowerMode()
        );

        graphics.fill(
                contentX + 13,
                powerY + 38,
                contentX + contentWidth - 13,
                powerY + 39,
                DIVIDER
        );

        drawUiText(
                graphics,
                "Battery Health",
                contentX + 13,
                powerY + 52,
                TEXT
        );

        drawUiText(
                graphics,
                "Normal",
                contentX + contentWidth - uiWidth("Normal") - 13,
                powerY + 52,
                MUTED
        );

        drawUiWrappedCentered(
                graphics,
                "Low Power Mode reduces background wireless refresh frequency.",
                phoneX + PHONE_WIDTH / 2,
                powerY + 96,
                PHONE_WIDTH - 50,
                11,
                3,
                MUTED
        );

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    private void drawToggle(
            GuiGraphics graphics,
            int x,
            int y,
            boolean enabled
    ) {
        roundedRect(
                graphics,
                x,
                y,
                36,
                20,
                10,
                enabled ? GREEN : 0xFF636366
        );

        roundedRect(
                graphics,
                enabled ? x + 19 : x + 3,
                y + 3,
                14,
                14,
                7,
                0xFFFFFFFF
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
                    contentX,
                    powerY,
                    contentWidth,
                    38
            )) {
                PhoneSystemSettings.setLowPowerMode(
                        !PhoneSystemSettings.lowPowerMode()
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
