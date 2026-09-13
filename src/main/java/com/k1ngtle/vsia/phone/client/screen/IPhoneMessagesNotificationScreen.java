package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneNotificationSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneMessagesNotificationScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int DIVIDER = 0xFF3A3A3C;
    private static final int GREEN = 0xFF30D158;

    private int contentX;
    private int contentWidth;
    private int groupY;

    public IPhoneMessagesNotificationScreen() {
        super(Component.literal("Messages Notifications"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        groupY = phoneY + 82;
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
        renderHeader(graphics, "Notifications", "Messages");

        beginPhoneClip(graphics, 68);

        roundedRect(
                graphics,
                contentX,
                groupY,
                contentWidth,
                114,
                14,
                CARD
        );

        row(
                graphics,
                groupY,
                "Allow Notifications",
                PhoneNotificationSettings.messagesNotificationsEnabled()
        );
        divider(graphics, groupY + 38);

        row(
                graphics,
                groupY + 38,
                "Sounds",
                PhoneNotificationSettings.alertSoundEnabled()
        );
        divider(graphics, groupY + 76);

        row(
                graphics,
                groupY + 76,
                "Show Previews",
                PhoneNotificationSettings.showPreviews()
        );

        drawUiWrappedCentered(
                graphics,
                "Messages are still delivered when notifications are off.",
                phoneX + PHONE_WIDTH / 2,
                groupY + 132,
                PHONE_WIDTH - 48,
                12,
                3,
                MUTED
        );

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    private void row(
            GuiGraphics graphics,
            int y,
            String label,
            boolean enabled
    ) {
        drawUiText(
                graphics,
                label,
                contentX + 13,
                y + 14,
                TEXT
        );

        drawToggle(
                graphics,
                contentX + contentWidth - 47,
                y + 9,
                enabled
        );
    }

    private void divider(
            GuiGraphics graphics,
            int y
    ) {
        graphics.fill(
                contentX + 13,
                y,
                contentX + contentWidth - 13,
                y + 1,
                DIVIDER
        );
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
                minecraft.setScreen(new IPhoneNotificationsScreen());
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
                PhoneNotificationSettings.setMessagesNotificationsEnabled(
                        !PhoneNotificationSettings.messagesNotificationsEnabled()
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
                PhoneNotificationSettings.setAlertSoundEnabled(
                        !PhoneNotificationSettings.alertSoundEnabled()
                );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    groupY + 76,
                    contentWidth,
                    38
            )) {
                PhoneNotificationSettings.setShowPreviews(
                        !PhoneNotificationSettings.showPreviews()
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
