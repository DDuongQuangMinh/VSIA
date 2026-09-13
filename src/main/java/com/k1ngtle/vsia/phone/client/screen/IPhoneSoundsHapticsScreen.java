package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneNotificationManager;
import com.k1ngtle.vsia.phone.client.PhoneNotificationSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneSoundsHapticsScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int DIVIDER = 0xFF3A3A3C;
    private static final int GREEN = 0xFF30D158;
    private static final int BLUE = 0xFF0A84FF;

    private int contentX;
    private int contentWidth;
    private int volumeY;
    private int optionsY;
    private int previewY;

    public IPhoneSoundsHapticsScreen() {
        super(Component.literal("Sounds & Haptics"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        volumeY = phoneY + 82;
        optionsY = volumeY + 78;
        previewY = optionsY + 92;
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
        renderHeader(graphics, "Settings", "Sounds & Haptics");

        beginPhoneClip(graphics, 68);

        drawUiText(
                graphics,
                "RINGTONE AND ALERTS",
                contentX + 4,
                volumeY - 16,
                MUTED
        );

        roundedRect(
                graphics,
                contentX,
                volumeY,
                contentWidth,
                58,
                14,
                CARD
        );

        drawUiText(
                graphics,
                "Alert Volume",
                contentX + 13,
                volumeY + 10,
                TEXT
        );

        drawVolumeSlider(
                graphics,
                contentX + 13,
                volumeY + 34,
                contentWidth - 26
        );

        roundedRect(
                graphics,
                contentX,
                optionsY,
                contentWidth,
                76,
                14,
                CARD
        );

        drawUiText(
                graphics,
                "Text Tone",
                contentX + 13,
                optionsY + 14,
                TEXT
        );

        String tone = PhoneNotificationSettings.alertSoundEnabled()
                ? "VSIA Chime"
                : "None";

        drawUiText(
                graphics,
                tone,
                contentX + contentWidth - uiWidth(tone) - 13,
                optionsY + 14,
                MUTED
        );

        graphics.fill(
                contentX + 13,
                optionsY + 38,
                contentX + contentWidth - 13,
                optionsY + 39,
                DIVIDER
        );

        drawUiText(
                graphics,
                "Haptics",
                contentX + 13,
                optionsY + 52,
                TEXT
        );

        drawToggle(
                graphics,
                contentX + contentWidth - 47,
                optionsY + 47,
                PhoneNotificationSettings.hapticsEnabled()
        );

        roundedRect(
                graphics,
                contentX,
                previewY,
                contentWidth,
                36,
                12,
                0xFF343438
        );

        drawUiCentered(
                graphics,
                "Preview Alert",
                phoneX + PHONE_WIDTH / 2,
                previewY + 13,
                BLUE
        );

        drawUiWrappedCentered(
                graphics,
                "Desktop Minecraft cannot drive a phone vibration motor, so haptics are represented by a short UI pulse.",
                phoneX + PHONE_WIDTH / 2,
                previewY + 50,
                PHONE_WIDTH - 50,
                11,
                4,
                MUTED
        );

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    private void drawVolumeSlider(
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
                PhoneNotificationSettings.alertVolume()
                        * width
        );

        graphics.fill(
                x,
                y,
                x + filled,
                y + 3,
                0xFFFFFFFF
        );

        int knobX = x + filled - 4;

        roundedRect(
                graphics,
                knobX,
                y - 3,
                8,
                9,
                4,
                0xFFFFFFFF
        );
    }

    private void setVolumeFromMouse(
            double mouseX
    ) {
        int left = contentX + 13;
        int width = contentWidth - 26;

        float value = (float) (
                (mouseX - left)
                        / width
        );

        PhoneNotificationSettings.setAlertVolume(
                value
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
                minecraft.setScreen(
                        new IPhoneSettingsScreen()
                );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX + 8,
                    volumeY + 25,
                    contentWidth - 16,
                    24
            )) {
                setVolumeFromMouse(mouseX);
                PhoneNotificationManager.get()
                        .previewAlert();
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    optionsY,
                    contentWidth,
                    38
            )) {
                PhoneNotificationSettings
                        .setAlertSoundEnabled(
                                !PhoneNotificationSettings
                                        .alertSoundEnabled()
                        );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    optionsY + 38,
                    contentWidth,
                    38
            )) {
                PhoneNotificationSettings
                        .setHapticsEnabled(
                                !PhoneNotificationSettings
                                        .hapticsEnabled()
                        );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    previewY,
                    contentWidth,
                    36
            )) {
                PhoneNotificationManager.get()
                        .previewAlert();
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
                contentX + 8,
                volumeY + 25,
                contentWidth - 16,
                24
        )) {
            setVolumeFromMouse(mouseX);
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
