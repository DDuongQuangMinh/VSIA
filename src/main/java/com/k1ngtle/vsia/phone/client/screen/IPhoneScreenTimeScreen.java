package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneSystemSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneScreenTimeScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int BLUE = 0xFF0A84FF;
    private static final int GREEN = 0xFF30D158;

    private int contentX;
    private int contentWidth;
    private int summaryY;
    private int toggleY;

    public IPhoneScreenTimeScreen() {
        super(Component.literal("Screen Time"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        summaryY = phoneY + 82;
        toggleY = summaryY + 92;
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
        renderHeader(graphics, "Settings", "Screen Time");

        beginPhoneClip(graphics, 68);

        roundedRect(
                graphics,
                contentX,
                summaryY,
                contentWidth,
                72,
                14,
                CARD
        );

        drawUiText(
                graphics,
                "Today",
                contentX + 13,
                summaryY + 12,
                MUTED
        );

        drawUiCentered(
                graphics,
                PhoneSystemSettings.formattedScreenTime(),
                phoneX + PHONE_WIDTH / 2,
                summaryY + 34,
                BLUE
        );

        drawUiWrappedCentered(
                graphics,
                "Time spent with the VS:IA phone UI open.",
                phoneX + PHONE_WIDTH / 2,
                summaryY + 51,
                PHONE_WIDTH - 52,
                11,
                2,
                MUTED
        );

        roundedRect(
                graphics,
                contentX,
                toggleY,
                contentWidth,
                50,
                14,
                CARD
        );

        drawUiText(
                graphics,
                "Screen Time",
                contentX + 13,
                toggleY + 18,
                TEXT
        );

        drawToggle(
                graphics,
                contentX + contentWidth - 47,
                toggleY + 15,
                PhoneSystemSettings.screenTimeEnabled()
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
                    toggleY,
                    contentWidth,
                    50
            )) {
                PhoneSystemSettings.setScreenTimeEnabled(
                        !PhoneSystemSettings.screenTimeEnabled()
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
