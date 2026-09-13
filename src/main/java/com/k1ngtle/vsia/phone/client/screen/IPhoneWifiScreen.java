package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.network.PhoneNetworkController;
import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public class IPhoneWifiScreen extends IPhoneScreen {
    private static final int ROW_HEIGHT = 40;
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF8E8E93;
    private static final int SECONDARY = 0xFFAEAEB2;
    private static final int DIVIDER = 0xFF3A3A3C;
    private static final int BLUE = 0xFF0A84FF;
    private static final int GREEN = 0xFF30D158;

    private int contentX;
    private int contentWidth;
    private int listY;
    private int scroll;

    public IPhoneWifiScreen() {
        super(Component.literal("Wi-Fi"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        listY = phoneY + 184;
        PhoneNetworkController.get().requestRefresh();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(graphics, BG);
        renderStatusBar(graphics);
        renderHeader(graphics, "Settings", "Wi-Fi");

        PhoneNetworkState state = PhoneNetworkState.get();
        PhoneNetworkState.WifiStatus wifi = state.getWifi();

        beginPhoneClip(graphics, 68);

        roundedRect(
                graphics,
                contentX,
                phoneY + 78,
                contentWidth,
                52,
                14,
                CARD
        );

        drawUiText(
                graphics,
                "Wi-Fi",
                contentX + 13,
                phoneY + 97,
                TEXT
        );

        drawToggle(
                graphics,
                contentX + contentWidth - 47,
                phoneY + 91,
                wifi.enabled()
        );

        drawUiText(
                graphics,
                wifi.enabled() ? statusLine(wifi) : "Wi-Fi is off",
                contentX + 4,
                phoneY + 143,
                statusColor(wifi)
        );

        if (wifi.enabled()) {
            drawUiText(
                    graphics,
                    "NETWORKS",
                    contentX + 4,
                    listY - 18,
                    MUTED
            );
            renderNetworks(graphics, mouseX, mouseY);
        }

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    private void renderNetworks(GuiGraphics graphics, int mouseX, int mouseY) {
        List<PhoneNetworkState.VisibleWifiNetwork> networks =
                PhoneNetworkState.get().getVisibleWifiNetworks();

        int visibleRows = 5;
        scroll = Math.max(
                0,
                Math.min(
                        scroll,
                        Math.max(0, networks.size() - visibleRows)
                )
        );

        if (networks.isEmpty()) {
            roundedRect(
                    graphics,
                    contentX,
                    listY,
                    contentWidth,
                    60,
                    14,
                    CARD
            );

            drawUiCentered(
                    graphics,
                    "No networks in range",
                    phoneX + PHONE_WIDTH / 2,
                    listY + 19,
                    SECONDARY
            );

            drawUiCentered(
                    graphics,
                    "Move closer to an access point",
                    phoneX + PHONE_WIDTH / 2,
                    listY + 36,
                    0xFF636366
            );
            return;
        }

        int rows = Math.min(visibleRows, networks.size() - scroll);

        roundedRect(
                graphics,
                contentX,
                listY,
                contentWidth,
                rows * ROW_HEIGHT,
                14,
                CARD
        );

        PhoneNetworkState.WifiStatus wifi = PhoneNetworkState.get().getWifi();

        for (int row = 0; row < rows; row++) {
            int index = scroll + row;
            PhoneNetworkState.VisibleWifiNetwork network = networks.get(index);
            int y = listY + row * ROW_HEIGHT;

            boolean hover = inside(
                    mouseX,
                    mouseY,
                    contentX,
                    y,
                    contentWidth,
                    ROW_HEIGHT
            );

            if (hover) {
                graphics.fill(
                        contentX + 2,
                        y + 2,
                        contentX + contentWidth - 2,
                        y + ROW_HEIGHT - 2,
                        0xFF353538
                );
            }

            if (row > 0) {
                graphics.fill(
                        contentX + 13,
                        y,
                        contentX + contentWidth - 13,
                        y + 1,
                        DIVIDER
                );
            }

            boolean selected =
                    wifi.connected()
                            && wifi.bssid().equalsIgnoreCase(network.bssid());

            drawUiText(
                    graphics,
                    selected ? "✓" : "",
                    contentX + 10,
                    y + 14,
                    BLUE
            );

            int right = contentX + contentWidth - 14;
            int reservedRight = network.locked() ? 52 : 36;
            int nameMax = Math.max(58, contentWidth - 29 - reservedRight);

            drawUiText(
                    graphics,
                    fitUi(network.ssid(), nameMax),
                    contentX + 29,
                    y + 8,
                    TEXT
            );

            String quality = network.quality()
                    + "  "
                    + network.rssiDbm()
                    + " dBm";

            drawUiText(
                    graphics,
                    fitUi(quality, nameMax),
                    contentX + 29,
                    y + 23,
                    qualityColor(network.quality())
            );

            drawWifiBars(
                    graphics,
                    right - 22,
                    y + 13,
                    barsForRssi(network.rssiDbm())
            );

            if (network.locked()) {
                drawLock(
                        graphics,
                        right - 35,
                        y + 13
                );
            }
        }
    }

    private String statusLine(PhoneNetworkState.WifiStatus wifi) {
        if (wifi.connected()) {
            return fitUi(
                    wifi.ssid() + " · " + wifi.quality(),
                    contentWidth - 8
            );
        }

        if (!wifi.status().isBlank()) {
            return fitUi(
                    wifi.status(),
                    contentWidth - 8
            );
        }

        return "Scanning...";
    }

    private int statusColor(PhoneNetworkState.WifiStatus wifi) {
        if (wifi.connected()) {
            return GREEN;
        }

        if ("FAILED".equalsIgnoreCase(wifi.stage())) {
            return 0xFFFF453A;
        }

        if ("AUTHENTICATING".equalsIgnoreCase(wifi.stage())
                || "ASSOCIATING".equalsIgnoreCase(wifi.stage())
                || "DHCP".equalsIgnoreCase(wifi.stage())
                || "GATEWAY".equalsIgnoreCase(wifi.stage())
                || "DNS".equalsIgnoreCase(wifi.stage())) {
            return 0xFFFFD60A;
        }

        return MUTED;
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

        int knobX = enabled ? x + 19 : x + 3;

        roundedRect(
                graphics,
                knobX,
                y + 3,
                14,
                14,
                7,
                0xFFFFFFFF
        );
    }

    private void drawWifiBars(
            GuiGraphics graphics,
            int x,
            int y,
            int bars
    ) {
        int active = 0xFFFFFFFF;
        int inactive = 0xFF636366;

        graphics.fill(
                x,
                y + 3,
                x + 14,
                y + 5,
                bars >= 3 ? active : inactive
        );

        graphics.fill(
                x + 2,
                y + 7,
                x + 12,
                y + 9,
                bars >= 2 ? active : inactive
        );

        graphics.fill(
                x + 5,
                y + 11,
                x + 9,
                y + 13,
                bars >= 1 ? active : inactive
        );
    }

    private void drawLock(
            GuiGraphics graphics,
            int x,
            int y
    ) {
        graphics.fill(
                x + 2,
                y + 6,
                x + 9,
                y + 13,
                SECONDARY
        );

        graphics.fill(
                x + 3,
                y + 2,
                x + 8,
                y + 4,
                SECONDARY
        );

        graphics.fill(
                x + 2,
                y + 3,
                x + 4,
                y + 7,
                SECONDARY
        );

        graphics.fill(
                x + 7,
                y + 3,
                x + 9,
                y + 7,
                SECONDARY
        );
    }

    private int barsForRssi(int rssi) {
        if (rssi >= -55) {
            return 3;
        }
        if (rssi >= -67) {
            return 2;
        }
        if (rssi >= -80) {
            return 1;
        }
        return 0;
    }

    private int qualityColor(String quality) {
        if ("EXCELLENT".equals(quality)
                || "GOOD".equals(quality)) {
            return GREEN;
        }

        if ("FAIR".equals(quality)) {
            return 0xFFFFD60A;
        }

        return 0xFFFF9F0A;
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            PhoneNetworkState state = PhoneNetworkState.get();

            if (clickedBack(mouseX, mouseY)) {
                minecraft.setScreen(new IPhoneSettingsScreen());
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX + contentWidth - 54,
                    phoneY + 82,
                    50,
                    38
            )) {
                PhoneNetworkController.get().setWifiEnabled(
                        !state.getWifi().enabled()
                );
                return true;
            }

            if (state.getWifi().enabled()
                    && inside(
                    mouseX,
                    mouseY,
                    contentX,
                    listY,
                    contentWidth,
                    ROW_HEIGHT * 5
            )) {
                List<PhoneNetworkState.VisibleWifiNetwork> networks =
                        state.getVisibleWifiNetworks();

                int row = ((int) mouseY - listY) / ROW_HEIGHT;
                int index = scroll + row;

                if (row >= 0
                        && row < 5
                        && index >= 0
                        && index < networks.size()) {
                    PhoneNetworkState.VisibleWifiNetwork network =
                            networks.get(index);

                    if (state.getWifi().connected()
                            && state.getWifi().bssid()
                            .equalsIgnoreCase(network.bssid())) {
                        minecraft.setScreen(new IPhoneWifiDetailsScreen());
                    } else if (network.locked()) {
                        minecraft.setScreen(
                                new IPhoneWifiPasswordScreen(
                                        network.ssid(),
                                        network.bssid(),
                                        network.security()
                                )
                        );
                    } else {
                        PhoneNetworkController.get().connectWifi(
                                network.bssid(),
                                ""
                        );
                    }

                    return true;
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
                listY,
                contentWidth,
                ROW_HEIGHT * 5
        )) {
            List<PhoneNetworkState.VisibleWifiNetwork> networks =
                    PhoneNetworkState.get().getVisibleWifiNetworks();

            int max = Math.max(0, networks.size() - 5);

            scroll = Math.max(
                    0,
                    Math.min(
                            max,
                            scroll + (delta > 0 ? -1 : 1)
                    )
            );

            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
