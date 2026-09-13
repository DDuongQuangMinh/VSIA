package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneNotificationManager;
import com.k1ngtle.vsia.phone.client.PhoneNotificationSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneNotificationsScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int SECONDARY = 0xFFAEAEB2;
    private static final int BLUE = 0xFF0A84FF;

    private int contentX;
    private int contentWidth;
    private int messagesY;
    private int historyY;

    public IPhoneNotificationsScreen() {
        super(Component.literal("Notifications"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        messagesY = phoneY + 83;
        historyY = messagesY + 70;
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
        renderHeader(graphics, "Settings", "Notifications");

        beginPhoneClip(graphics, 68);

        roundedRect(
                graphics,
                contentX,
                messagesY,
                contentWidth,
                46,
                14,
                CARD
        );

        drawUiText(
                graphics,
                "Messages",
                contentX + 13,
                messagesY + 10,
                TEXT
        );

        String state = PhoneNotificationSettings.messagesNotificationsEnabled()
                ? "On"
                : "Off";

        int stateWidth = uiWidth(state);

        drawUiText(
                graphics,
                state,
                contentX + contentWidth - stateWidth - 24,
                messagesY + 10,
                SECONDARY
        );

        drawUiText(
                graphics,
                "›",
                contentX + contentWidth - 12,
                messagesY + 10,
                SECONDARY
        );

        drawUiText(
                graphics,
                "BANNERS & HISTORY",
                contentX + 4,
                historyY - 16,
                SECONDARY
        );

        roundedRect(
                graphics,
                contentX,
                historyY,
                contentWidth,
                58,
                14,
                CARD
        );

        drawUiText(
                graphics,
                "Notification History",
                contentX + 13,
                historyY + 10,
                TEXT
        );

        String count = Integer.toString(
                PhoneNotificationManager.get().historyCount()
        );

        drawUiText(
                graphics,
                count,
                contentX + contentWidth - uiWidth(count) - 13,
                historyY + 10,
                BLUE
        );

        drawUiWrappedCentered(
                graphics,
                "Focus can silence banners without blocking message delivery.",
                phoneX + PHONE_WIDTH / 2,
                historyY + 30,
                PHONE_WIDTH - 56,
                11,
                2,
                SECONDARY
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
            if (clickedBack(mouseX, mouseY)) {
                minecraft.setScreen(new IPhoneSettingsScreen());
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    messagesY,
                    contentWidth,
                    46
            )) {
                minecraft.setScreen(
                        new IPhoneMessagesNotificationScreen()
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
