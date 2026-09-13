package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneDeviceSettingsClientState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneBluetoothScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF8E8E93;
    private static final int SECONDARY = 0xFFAEAEB2;
    private static final int GREEN = 0xFF30D158;

    private int contentX;
    private int contentWidth;
    private int toggleY;
    private int devicesY;

    public IPhoneBluetoothScreen() {
        super(Component.literal("Bluetooth"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        toggleY = phoneY + 78;
        devicesY = phoneY + 198;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(graphics, BG);
        renderStatusBar(graphics);
        renderHeader(graphics, "Settings", "Bluetooth");
        beginPhoneClip(graphics, 68);

        roundedRect(graphics, contentX, toggleY, contentWidth, 50, 14, CARD);
        drawUiText(graphics, "Bluetooth", contentX + 13, toggleY + 19, TEXT);
        drawToggle(graphics, contentX + contentWidth - 47, toggleY + 15,
                PhoneDeviceSettingsClientState.bluetoothEnabled());

        drawUiWrappedCentered(graphics,
                "Bluetooth stays available in Airplane Mode unless you turn it off.",
                phoneX + PHONE_WIDTH / 2, toggleY + 61, PHONE_WIDTH - 58, 11, 2, SECONDARY);

        drawUiText(graphics, "MY DEVICES", contentX + 4, devicesY - 16, MUTED);
        roundedRect(graphics, contentX, devicesY, contentWidth, 60, 14, CARD);

        if (PhoneDeviceSettingsClientState.bluetoothEnabled()) {
            drawUiCentered(graphics, "No Bluetooth devices registered",
                    phoneX + PHONE_WIDTH / 2, devicesY + 18, SECONDARY);
            drawUiCentered(graphics, "Radio is on",
                    phoneX + PHONE_WIDTH / 2, devicesY + 35, GREEN);
        } else {
            drawUiCentered(graphics, "Bluetooth is Off",
                    phoneX + PHONE_WIDTH / 2, devicesY + 25, SECONDARY);
        }

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    private void drawToggle(GuiGraphics graphics, int x, int y, boolean enabled) {
        roundedRect(graphics, x, y, 36, 20, 10, enabled ? GREEN : 0xFF636366);
        roundedRect(graphics, enabled ? x + 19 : x + 3, y + 3, 14, 14, 7, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (clickedBack(mouseX, mouseY)) {
                minecraft.setScreen(new IPhoneSettingsScreen());
                return true;
            }
            if (inside(mouseX, mouseY, contentX, toggleY, contentWidth, 50)) {
                PhoneDeviceSettingsClientState.setBluetoothEnabled(
                        !PhoneDeviceSettingsClientState.bluetoothEnabled());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
