package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneAccessibilityClientPreferences;
import com.k1ngtle.vsia.phone.client.PhoneDeviceSettingsClientState;
import com.k1ngtle.vsia.phone.client.PhoneNotificationSettings;
import com.k1ngtle.vsia.phone.client.PhonePrivacyClientState;
import com.k1ngtle.vsia.phone.client.PhoneSystemSettings;
import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberClientState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IPhoneSettingsScreen extends IPhoneScreen {
    private static final int BACKGROUND = 0xFF111216;
    private static final int CARD = 0xFF2C2C2E;
    private static final int CARD_ALT = 0xFF232428;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int SECONDARY = 0xFFAEAEB2;
    private static final int TERTIARY = 0xFF8E8E93;
    private static final int DIVIDER = 0xFF3A3A3C;

    private static final int BLUE = 0xFF0A84FF;
    private static final int GREEN = 0xFF30D158;
    private static final int ORANGE = 0xFFFF9F0A;
    private static final int RED = 0xFFFF453A;
    private static final int PINK = 0xFFFF2D55;
    private static final int PURPLE = 0xFFBF5AF2;
    private static final int INDIGO = 0xFF5E5CE6;
    private static final int TEAL = 0xFF64D2FF;
    private static final int GREY = 0xFF6D6D72;
    private static final int BLACK = 0xFF1C1C1E;

    private static final int ROW_HEIGHT = 38;
    private static final int GROUP_SPACING = 14;

    private static final Row[][] GROUPS = new Row[][]{
            {
                    Row.AIRPLANE_MODE,
                    Row.WIFI,
                    Row.CELLULAR,
                    Row.BLUETOOTH
            },
            {
                    Row.NOTIFICATIONS,
                    Row.SOUNDS_HAPTICS,
                    Row.FOCUS,
                    Row.SCREEN_TIME
            },
            {
                    Row.GENERAL,
                    Row.ACCESSIBILITY,
                    Row.ACTION_BUTTON,
                    Row.DISPLAY_BRIGHTNESS,
                    Row.HOME_SCREEN,
                    Row.WALLPAPER,
                    Row.SIRI,
                    Row.FACE_ID,
                    Row.EMERGENCY_SOS
            },
            {
                    Row.PRIVACY_SECURITY,
                    Row.APP_STORE,
                    Row.WALLET,
                    Row.BATTERY,
                    Row.APPS
            }
    };

    private int contentX;
    private int contentWidth;
    private int titleY;
    private int searchY;
    private int viewportTop;
    private int viewportBottom;
    private int viewportHeight;
    private int scrollOffset;
    private int contentHeight;

    public IPhoneSettingsScreen() {
        super(Component.literal("Settings"));
    }

    @Override
    protected void init() {
        super.init();

        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        titleY = phoneY + 49;
        searchY = phoneY + 73;
        viewportTop = searchY + 36;
        viewportBottom = phoneY + PHONE_HEIGHT - 31;
        viewportHeight = viewportBottom - viewportTop;
        contentHeight = computeContentHeight();
        scrollOffset = clampScroll(scrollOffset);

        PhoneSubscriberClientState.get().requestRefresh();
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

        drawUiCentered(
                graphics,
                "Settings",
                phoneX + PHONE_WIDTH / 2,
                titleY,
                TEXT
        );

        drawSearchBar(graphics, searchY);

        graphics.enableScissor(
                phoneX + DISPLAY_INSET,
                viewportTop,
                phoneX + PHONE_WIDTH - DISPLAY_INSET,
                viewportBottom
        );

        int logicalY = 0;

        for (Row[] group : GROUPS) {
            int y = sy(logicalY);
            drawGroup(graphics, y, group);
            logicalY += group.length * ROW_HEIGHT + GROUP_SPACING;
        }

        graphics.disableScissor();
        renderHomeIndicator(graphics);
    }

    private void drawSearchBar(
            GuiGraphics graphics,
            int y
    ) {
        roundedRect(graphics, contentX, y, contentWidth, 26, 10, CARD_ALT);
        drawUiText(graphics, "Search", contentX + 34, y + 9, TERTIARY);
        drawSearchIcon(graphics, contentX + 12, y + 8, TERTIARY);
    }

    private void drawGroup(
            GuiGraphics graphics,
            int y,
            Row[] rows
    ) {
        int height = rows.length * ROW_HEIGHT;

        roundedRect(
                graphics,
                contentX,
                y,
                contentWidth,
                height,
                14,
                CARD
        );

        for (int i = 0; i < rows.length; i++) {
            int rowY = y + i * ROW_HEIGHT;
            drawSettingsRow(graphics, rowY, rows[i]);

            if (i + 1 < rows.length) {
                int dividerY = rowY + ROW_HEIGHT;
                graphics.fill(
                        contentX + 48,
                        dividerY,
                        contentX + contentWidth - 12,
                        dividerY + 1,
                        DIVIDER
                );
            }
        }
    }

    private void drawSettingsRow(
            GuiGraphics graphics,
            int y,
            Row row
    ) {
        int iconX = contentX + 10;
        int iconY = y + 8;

        drawRowIcon(graphics, row, iconX, iconY);
        drawUiText(graphics, row.title, contentX + 46, y + 14, TEXT);

        if (row == Row.AIRPLANE_MODE) {
            drawToggle(
                    graphics,
                    contentX + contentWidth - 47,
                    y + 9,
                    PhoneDeviceSettingsClientState.airplaneMode()
            );
            return;
        }

        String summary = summaryFor(row);

        if (!summary.isBlank()) {
            String fitSummary = fitUi(summary, 104);
            int summaryWidth = uiWidth(fitSummary);
            drawUiText(
                    graphics,
                    fitSummary,
                    contentX + contentWidth - summaryWidth - 18,
                    y + 14,
                    SECONDARY
            );
        }

        drawUiText(
                graphics,
                "›",
                contentX + contentWidth - 11,
                y + 14,
                TERTIARY
        );
    }

    private void drawRowIcon(
            GuiGraphics graphics,
            Row row,
            int x,
            int y
    ) {
        roundedRect(graphics, x, y, 22, 22, 6, row.color);

        switch (row) {
            case AIRPLANE_MODE -> drawPlaneIcon(graphics, x + 4, y + 5);
            case WIFI -> drawWifiIcon(graphics, x + 5, y + 5);
            case CELLULAR -> drawCellularIcon(graphics, x + 5, y + 4);
            case BLUETOOTH -> drawBluetoothIcon(graphics, x + 6, y + 3);
            case NOTIFICATIONS -> drawBellIcon(graphics, x + 5, y + 4);
            case SOUNDS_HAPTICS -> drawSpeakerIcon(graphics, x + 4, y + 5);
            case FOCUS -> drawMoonIcon(graphics, x + 5, y + 5);
            case SCREEN_TIME -> drawHourglassIcon(graphics, x + 5, y + 4);
            case GENERAL -> drawGearIcon(graphics, x + 4, y + 4);
            case ACCESSIBILITY -> drawAccessibilityIcon(graphics, x + 4, y + 3);
            case ACTION_BUTTON -> drawActionButtonIcon(graphics, x + 5, y + 6);
            case DISPLAY_BRIGHTNESS -> drawDisplayBrightnessIcon(graphics, x + 3, y + 3);
            case HOME_SCREEN -> drawHomeGridIcon(graphics, x + 4, y + 4);
            case WALLPAPER -> drawWallpaperIcon(graphics, x + 4, y + 4);
            case SIRI -> drawSiriIcon(graphics, x + 5, y + 5);
            case FACE_ID -> drawFaceIdIcon(graphics, x + 4, y + 4);
            case EMERGENCY_SOS -> drawSosIcon(graphics, x + 4, y + 6);
            case PRIVACY_SECURITY -> drawLockIcon(graphics, x + 5, y + 4);
            case APP_STORE -> drawAppStoreIcon(graphics, x + 5, y + 4);
            case WALLET -> drawWalletIcon(graphics, x + 4, y + 5);
            case BATTERY -> drawBatteryIcon(graphics, x + 4, y + 6);
            case APPS -> drawAppsIcon(graphics, x + 4, y + 4);
        }
    }

    private void drawSearchIcon(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(x + 1, y + 1, x + 7, y + 2, color);
        graphics.fill(x, y + 2, x + 1, y + 6, color);
        graphics.fill(x + 7, y + 2, x + 8, y + 6, color);
        graphics.fill(x + 1, y + 6, x + 7, y + 7, color);
        graphics.fill(x + 6, y + 6, x + 10, y + 10, color);
    }

    private void drawWifiIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(x + 1, y, x + 9, y + 1, TEXT);
        graphics.fill(x + 2, y + 3, x + 8, y + 4, TEXT);
        graphics.fill(x + 3, y + 6, x + 7, y + 7, TEXT);
        graphics.fill(x + 4, y + 9, x + 6, y + 10, TEXT);
    }

    private void drawCellularIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(x, y + 8, x + 2, y + 12, TEXT);
        graphics.fill(x + 3, y + 6, x + 5, y + 12, TEXT);
        graphics.fill(x + 6, y + 3, x + 8, y + 12, TEXT);
        graphics.fill(x + 9, y, x + 11, y + 12, TEXT);
    }

    private void drawPlaneIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(x + 2, y + 4, x + 10, y + 6, TEXT);
        graphics.fill(x + 5, y + 1, x + 7, y + 10, TEXT);
        graphics.fill(x + 1, y + 5, x + 4, y + 8, TEXT);
        graphics.fill(x + 8, y + 3, x + 12, y + 5, TEXT);
    }

    private void drawBluetoothIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(x + 4, y, x + 5, y + 14, TEXT);
        graphics.fill(x + 4, y + 7, x + 10, y + 8, TEXT);
        graphics.fill(x + 4, y, x + 9, y + 5, TEXT);
        graphics.fill(x + 4, y + 9, x + 9, y + 14, TEXT);
        graphics.fill(x + 1, y + 3, x + 5, y + 7, TEXT);
        graphics.fill(x + 1, y + 8, x + 5, y + 12, TEXT);
    }

    private void drawBellIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(x + 2, y + 2, x + 8, y + 3, TEXT);
        graphics.fill(x + 1, y + 3, x + 9, y + 8, TEXT);
        graphics.fill(x, y + 8, x + 10, y + 9, TEXT);
        graphics.fill(x + 4, y + 9, x + 6, y + 11, TEXT);
    }

    private void drawSpeakerIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(x, y + 3, x + 3, y + 9, TEXT);
        graphics.fill(x + 3, y + 2, x + 5, y + 10, TEXT);
        graphics.fill(x + 5, y + 4, x + 7, y + 8, TEXT);
        graphics.fill(x + 8, y + 2, x + 9, y + 10, TEXT);
        graphics.fill(x + 10, y + 3, x + 11, y + 9, TEXT);
    }

    private void drawMoonIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        roundedRect(graphics, x + 1, y + 1, 10, 10, 5, TEXT);
        roundedRect(graphics, x + 5, y, 7, 10, 4, PURPLE);
    }

    private void drawHourglassIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(x + 1, y, x + 9, y + 1, TEXT);
        graphics.fill(x + 1, y + 10, x + 9, y + 11, TEXT);
        graphics.fill(x + 2, y + 1, x + 4, y + 3, TEXT);
        graphics.fill(x + 6, y + 1, x + 8, y + 3, TEXT);
        graphics.fill(x + 4, y + 3, x + 6, y + 5, TEXT);
        graphics.fill(x + 4, y + 6, x + 6, y + 8, TEXT);
        graphics.fill(x + 2, y + 8, x + 4, y + 10, TEXT);
        graphics.fill(x + 6, y + 8, x + 8, y + 10, TEXT);
    }

    private void drawGearIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        roundedRect(graphics, x + 2, y + 2, 8, 8, 4, TEXT);
        graphics.fill(x + 4, y, x + 6, y + 12, TEXT);
        graphics.fill(x, y + 4, x + 12, y + 6, TEXT);
    }

    private void drawAccessibilityIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(x + 4, y, x + 6, y + 2, TEXT);
        graphics.fill(x + 3, y + 3, x + 7, y + 4, TEXT);
        graphics.fill(x, y + 4, x + 10, y + 5, TEXT);
        graphics.fill(x + 4, y + 4, x + 6, y + 10, TEXT);
        graphics.fill(x + 1, y + 10, x + 4, y + 13, TEXT);
        graphics.fill(x + 6, y + 10, x + 9, y + 13, TEXT);
    }

    private void drawActionButtonIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        roundedRect(graphics, x, y, 12, 6, 3, TEXT);
        graphics.fill(x + 4, y + 2, x + 8, y + 4, GREY);
    }

    private void drawDisplayBrightnessIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(x + 2, y + 7, x + 4, y + 8, TEXT);
        graphics.fill(x + 1, y + 8, x + 2, y + 12, TEXT);
        graphics.fill(x + 4, y + 8, x + 5, y + 12, TEXT);
        graphics.fill(x + 1, y + 10, x + 5, y + 11, TEXT);

        graphics.fill(x + 8, y + 4, x + 11, y + 5, TEXT);
        graphics.fill(x + 7, y + 5, x + 8, y + 12, TEXT);
        graphics.fill(x + 11, y + 5, x + 12, y + 12, TEXT);
        graphics.fill(x + 8, y + 8, x + 11, y + 9, TEXT);
    }

    private void drawHomeGridIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        roundedRect(graphics, x, y, 4, 4, 2, TEXT);
        roundedRect(graphics, x + 6, y, 4, 4, 2, TEXT);
        roundedRect(graphics, x, y + 6, 4, 4, 2, TEXT);
        roundedRect(graphics, x + 6, y + 6, 4, 4, 2, TEXT);
    }

    private void drawWallpaperIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(x, y, x + 11, y + 12, TEXT);
        graphics.fill(x + 1, y + 1, x + 10, y + 11, TEAL);
        graphics.fill(x + 2, y + 6, x + 9, y + 7, TEXT);
    }

    private void drawSiriIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        roundedRect(graphics, x, y + 2, 4, 4, 2, TEAL);
        roundedRect(graphics, x + 6, y, 4, 4, 2, PINK);
        roundedRect(graphics, x + 7, y + 7, 4, 4, 2, BLUE);
        roundedRect(graphics, x + 1, y + 8, 4, 4, 2, PURPLE);
    }

    private void drawFaceIdIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(x, y + 2, x + 2, y + 8, TEXT);
        graphics.fill(x + 8, y + 2, x + 10, y + 8, TEXT);
        graphics.fill(x + 2, y, x + 8, y + 2, TEXT);
        graphics.fill(x + 2, y + 8, x + 8, y + 10, TEXT);
    }

    private void drawSosIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        drawUiText(graphics, "SOS", x - 1, y, TEXT);
    }

    private void drawLockIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(x + 2, y + 5, x + 9, y + 12, TEXT);
        graphics.fill(x + 3, y + 1, x + 8, y + 3, TEXT);
        graphics.fill(x + 2, y + 2, x + 4, y + 6, TEXT);
        graphics.fill(x + 7, y + 2, x + 9, y + 6, TEXT);
    }

    private void drawAppStoreIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(x + 4, y, x + 6, y + 10, TEXT);
        graphics.fill(x, y + 8, x + 10, y + 10, TEXT);
        graphics.fill(x + 1, y + 3, x + 3, y + 5, TEXT);
        graphics.fill(x + 7, y + 3, x + 9, y + 5, TEXT);
    }

    private void drawWalletIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(x, y + 2, x + 12, y + 10, TEXT);
        graphics.fill(x + 1, y + 3, x + 11, y + 9, BLACK);
        graphics.fill(x + 7, y + 5, x + 10, y + 7, 0xFFFFD60A);
    }

    private void drawBatteryIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(x, y, x + 10, y + 6, TEXT);
        graphics.fill(x + 1, y + 1, x + 9, y + 5, GREEN);
        graphics.fill(x + 10, y + 2, x + 12, y + 4, TEXT);
    }

    private void drawAppsIcon(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        roundedRect(graphics, x, y, 11, 11, 4, TEXT);
        roundedRect(graphics, x + 2, y + 2, 7, 7, 3, GREY);
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

    private String summaryFor(
            Row row
    ) {
        return switch (row) {
            case AIRPLANE_MODE -> PhoneDeviceSettingsClientState.airplaneMode() ? "On" : "Off";
            case WIFI -> wifiSummary();
            case CELLULAR -> cellularSummary();
            case BLUETOOTH -> PhoneDeviceSettingsClientState.bluetoothEnabled() ? "On" : "Off";
            case NOTIFICATIONS -> PhoneNotificationSettings.messagesNotificationsEnabled() ? "On" : "Off";
            case SOUNDS_HAPTICS -> PhoneNotificationSettings.alertSoundEnabled()
                    ? Math.round(PhoneNotificationSettings.alertVolume() * 100.0F) + "%"
                    : "Silent";
            case FOCUS -> PhoneNotificationSettings.doNotDisturbEnabled() ? "Do Not Disturb" : "Off";
            case SCREEN_TIME -> PhoneSystemSettings.screenTimeEnabled()
                    ? PhoneSystemSettings.formattedScreenTime()
                    : "Off";
            case GENERAL -> "";
            case ACCESSIBILITY -> "";
            case ACTION_BUTTON -> "";
            case DISPLAY_BRIGHTNESS -> Math.round(PhoneSystemSettings.brightness() * 100.0F) + "%";
            case HOME_SCREEN -> PhoneSystemSettings.showHomeSearch() ? "Search On" : "Search Off";
            case WALLPAPER -> PhoneSystemSettings.wallpaper().displayName();
            case SIRI -> "";
            case FACE_ID -> "";
            case EMERGENCY_SOS -> "";
            case PRIVACY_SECURITY -> PhonePrivacyClientState.localNetworkAllowed() ? "On" : "Restricted";
            case APP_STORE -> "";
            case WALLET -> "";
            case BATTERY -> PhoneSystemSettings.lowPowerMode()
                    ? PhoneNetworkState.get().getBatteryPercent() + "% · Low Power"
                    : PhoneNetworkState.get().getBatteryPercent() + "%";
            case APPS -> "";
        };
    }

    private String wifiSummary() {
        PhoneNetworkState.WifiStatus wifi =
                PhoneNetworkState.get().getWifi();

        if (!wifi.enabled()) {
            return "Off";
        }

        if (wifi.connected()) {
            return wifi.ssid();
        }

        return "Not Connected";
    }

    private String cellularSummary() {
        if (PhoneDeviceSettingsClientState.airplaneMode()) {
            return "Airplane Mode";
        }

        if (!PhoneSubscriberClientState.get().hasActiveSubscription()) {
            return "No SIM";
        }

        if (!PhoneSubscriberClientState.get().snapshot().cellularDataEnabled()) {
            return "Data Off";
        }

        PhoneNetworkState.CellularStatus cellular =
                PhoneNetworkState.get().getCellular();

        if (cellular.registered()) {
            return cellular.carrier();
        }

        return "No Service";
    }

    private int computeContentHeight() {
        int total = 0;

        for (int i = 0; i < GROUPS.length; i++) {
            total += GROUPS[i].length * ROW_HEIGHT;
            if (i + 1 < GROUPS.length) {
                total += GROUP_SPACING;
            }
        }

        return total;
    }

    private int sy(
            int logicalY
    ) {
        return viewportTop + logicalY - scrollOffset;
    }

    private int clampScroll(
            int value
    ) {
        int max = Math.max(0, contentHeight - viewportHeight);
        return Math.max(0, Math.min(max, value));
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
                    contentX,
                    viewportTop,
                    contentWidth,
                    viewportHeight
            )) {
                int logicalY = (int) Math.floor(mouseY - viewportTop + scrollOffset);
                RowHit hit = rowAt(logicalY);

                if (hit != null) {
                    if (hit.row == Row.AIRPLANE_MODE) {
                        PhoneDeviceSettingsClientState.toggleAirplaneMode();
                        return true;
                    }

                    switch (hit.row) {
                        case WIFI -> {
                            minecraft.setScreen(new IPhoneWifiScreen());
                            return true;
                        }
                        case CELLULAR -> {
                            minecraft.setScreen(new IPhoneCellularScreen());
                            return true;
                        }
                        case BLUETOOTH -> {
                            minecraft.setScreen(new IPhoneBluetoothScreen());
                            return true;
                        }
                        case NOTIFICATIONS -> {
                            minecraft.setScreen(new IPhoneNotificationsScreen());
                            return true;
                        }
                        case SOUNDS_HAPTICS -> {
                            minecraft.setScreen(new IPhoneSoundsHapticsScreen());
                            return true;
                        }
                        case FOCUS -> {
                            minecraft.setScreen(new IPhoneFocusScreen());
                            return true;
                        }
                        case SCREEN_TIME -> {
                            minecraft.setScreen(new IPhoneScreenTimeScreen());
                            return true;
                        }
                        case GENERAL -> {
                            minecraft.setScreen(new IPhoneGeneralScreen());
                            return true;
                        }
                        case ACCESSIBILITY -> {
                            minecraft.setScreen(new IPhoneAccessibilityScreen());
                            return true;
                        }
                        case DISPLAY_BRIGHTNESS -> {
                            minecraft.setScreen(new IPhoneDisplayBrightnessScreen());
                            return true;
                        }
                        case HOME_SCREEN -> {
                            minecraft.setScreen(new IPhoneHomeScreenSettingsScreen());
                            return true;
                        }
                        case WALLPAPER -> {
                            minecraft.setScreen(new IPhoneWallpaperScreen());
                            return true;
                        }
                        case BATTERY -> {
                            minecraft.setScreen(new IPhoneBatteryScreen());
                            return true;
                        }
                        case PRIVACY_SECURITY -> {
                            minecraft.setScreen(new IPhonePrivacySecurityScreen());
                            return true;
                        }
                        default -> {
                            return true;
                        }
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {
        if (inside(
                mouseX,
                mouseY,
                contentX,
                viewportTop,
                contentWidth,
                viewportHeight
        )) {
            scrollOffset = clampScroll(
                    scrollOffset + (delta > 0.0 ? -28 : 28)
            );
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private RowHit rowAt(
            int logicalY
    ) {
        int base = 0;

        for (int gi = 0; gi < GROUPS.length; gi++) {
            Row[] group = GROUPS[gi];
            int height = group.length * ROW_HEIGHT;

            if (logicalY >= base && logicalY < base + height) {
                int index = (logicalY - base) / ROW_HEIGHT;
                if (index >= 0 && index < group.length) {
                    return new RowHit(group[index]);
                }
            }

            base += height;
            if (gi + 1 < GROUPS.length) {
                base += GROUP_SPACING;
            }
        }

        return null;
    }

    private record RowHit(Row row) {
    }

    private enum Row {
        AIRPLANE_MODE("Airplane Mode", ORANGE),
        WIFI("Wi-Fi", BLUE),
        CELLULAR("Cellular", GREEN),
        BLUETOOTH("Bluetooth", BLUE),
        NOTIFICATIONS("Notifications", RED),
        SOUNDS_HAPTICS("Sounds & Haptics", PINK),
        FOCUS("Focus", PURPLE),
        SCREEN_TIME("Screen Time", INDIGO),
        GENERAL("General", GREY),
        ACCESSIBILITY("Accessibility", BLUE),
        ACTION_BUTTON("Action Button", GREY),
        DISPLAY_BRIGHTNESS("Display & Brightness", BLUE),
        HOME_SCREEN("Home Screen & App Library", INDIGO),
        WALLPAPER("Wallpaper", TEAL),
        SIRI("Siri", PURPLE),
        FACE_ID("Face ID & Passcode", GREEN),
        EMERGENCY_SOS("Emergency SOS", RED),
        PRIVACY_SECURITY("Privacy & Security", BLUE),
        APP_STORE("App Store", BLUE),
        WALLET("Wallet & Apple Pay", BLACK),
        BATTERY("Battery", GREEN),
        APPS("Apps", GREY);

        private final String title;
        private final int color;

        Row(String title, int color) {
            this.title = title;
            this.color = color;
        }
    }
}
