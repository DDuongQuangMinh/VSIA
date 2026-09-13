package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneNotificationSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneFocusScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int DIVIDER = 0xFF3A3A3C;
    private static final int GREEN = 0xFF30D158;

    private int contentX;
    private int contentWidth;
    private int groupY;

    public IPhoneFocusScreen() {
        super(Component.literal("Focus"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        groupY = phoneY + 84;
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
        renderHeader(graphics, "Settings", "Focus");

        beginPhoneClip(graphics, 68);

        drawUiText(
                graphics,
                "DO NOT DISTURB",
                contentX + 4,
                groupY - 16,
                MUTED
        );

        roundedRect(
                graphics,
                contentX,
                groupY,
                contentWidth,
                76,
                14,
                CARD
        );

        drawUiText(
                graphics,
                "Do Not Disturb",
                contentX + 13,
                groupY + 14,
                TEXT
        );

        drawToggle(
                graphics,
                contentX + contentWidth - 47,
                groupY + 9,
                PhoneNotificationSettings
                        .doNotDisturbEnabled()
        );

        graphics.fill(
                contentX + 13,
                groupY + 38,
                contentX + contentWidth - 13,
                groupY + 39,
                DIVIDER
        );

        drawUiText(
                graphics,
                "Allow Messages",
                contentX + 13,
                groupY + 52,
                TEXT
        );

        drawToggle(
                graphics,
                contentX + contentWidth - 47,
                groupY + 47,
                PhoneNotificationSettings
                        .allowMessagesInFocus()
        );

        drawUiWrappedCentered(
                graphics,
                "Do Not Disturb silences Messages banners and alert sounds unless Messages is allowed. Messages still arrive.",
                phoneX + PHONE_WIDTH / 2,
                groupY + 96,
                PHONE_WIDTH - 48,
                11,
                5,
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
                    groupY,
                    contentWidth,
                    38
            )) {
                PhoneNotificationSettings
                        .setDoNotDisturbEnabled(
                                !PhoneNotificationSettings
                                        .doNotDisturbEnabled()
                        );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    groupY + 38,
                    contentWidth,
                    38
            )) {
                PhoneNotificationSettings
                        .setAllowMessagesInFocus(
                                !PhoneNotificationSettings
                                        .allowMessagesInFocus()
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
