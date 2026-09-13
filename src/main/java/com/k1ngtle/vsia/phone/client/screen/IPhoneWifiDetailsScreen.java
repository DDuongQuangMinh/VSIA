package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneWifiClientPreferences;
import com.k1ngtle.vsia.phone.network.PhoneNetworkController;
import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class IPhoneWifiDetailsScreen
        extends IPhoneScreen {

    private static final int BLUE =
            0xFF0A84FF;

    private static final int GREEN =
            0xFF30D158;

    private static final int TEXT =
            0xFFFFFFFF;

    private static final int SECONDARY =
            0xFFAEAEB2;

    private static final int MUTED =
            0xFF8E8E93;

    private static final int CARD =
            0xFF2C2C2E;

    private static final int DIVIDER =
            0xFF3A3A3C;

    private static final int ROW_HEIGHT =
            30;

    private static final int CONTENT_HEIGHT =
            557;

    private int contentX;
    private int contentWidth;

    private int viewportTop;
    private int viewportBottom;
    private int viewportHeight;

    private int scrollOffset;
    private boolean showPassword;

    public IPhoneWifiDetailsScreen() {
        super(
                Component.literal(
                        "Wi-Fi Network"
                )
        );
    }

    @Override
    protected void init() {
        super.init();

        contentX =
                phoneX + 14;

        contentWidth =
                PHONE_WIDTH - 28;

        viewportTop =
                phoneY + 73;

        viewportBottom =
                phoneY
                        + PHONE_HEIGHT
                        - 31;

        viewportHeight =
                viewportBottom
                        - viewportTop;

        scrollOffset =
                clampScroll(
                        scrollOffset
                );

        PhoneNetworkController
                .get()
                .requestRefresh();
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderPhoneShell(
                graphics,
                0xFF1C1C1E
        );

        renderStatusBar(
                graphics
        );

        PhoneNetworkState.WifiStatus wifi =
                PhoneNetworkState
                        .get()
                        .getWifi();

        renderHeader(
                graphics,
                "Wi-Fi",
                fit(
                        wifi.ssid()
                                .isBlank()
                                ? "Network"
                                : wifi.ssid(),
                        106
                )
        );

        graphics.enableScissor(
                phoneX + 5,
                viewportTop,
                phoneX
                        + PHONE_WIDTH
                        - 5,
                viewportBottom
        );

        renderScrollableContent(
                graphics,
                wifi,
                mouseX,
                mouseY
        );

        graphics.disableScissor();

        renderHomeIndicator(
                graphics
        );
    }

    private void renderScrollableContent(
            GuiGraphics graphics,
            PhoneNetworkState.WifiStatus wifi,
            int mouseX,
            int mouseY
    ) {
        int forgetY =
                sy(
                        0
                );

        roundedRect(
                graphics,
                contentX,
                forgetY,
                contentWidth,
                36,
                12,
                CARD
        );

        graphics.drawString(
                font,
                "Forget This Network",
                contentX + 13,
                forgetY + 14,
                BLUE,
                false
        );

        int preferencesY =
                sy(
                        52
                );

        roundedRect(
                graphics,
                contentX,
                preferencesY,
                contentWidth,
                ROW_HEIGHT * 3,
                12,
                CARD
        );

        rowLabel(
                graphics,
                preferencesY,
                "Auto-Join"
        );

        boolean autoJoin =
                PhoneWifiClientPreferences
                        .autoJoin(
                                wifi.bssid()
                        );

        drawToggle(
                graphics,
                contentX
                        + contentWidth
                        - 48,
                preferencesY + 5,
                autoJoin
        );

        divider(
                graphics,
                preferencesY
                        + ROW_HEIGHT
        );

        rowPair(
                graphics,
                preferencesY
                        + ROW_HEIGHT,
                "Security",
                securityLabel(
                        wifi.security()
                )
        );

        divider(
                graphics,
                preferencesY
                        + ROW_HEIGHT
                        * 2
        );

        rowLabel(
                graphics,
                preferencesY
                        + ROW_HEIGHT
                        * 2,
                "Password"
        );

        String rememberedPassword =
                PhoneWifiClientPreferences
                        .password(
                                wifi.ssid(),
                                wifi.security()
                        );

        String passwordValue;

        if (isOpenSecurity(
                wifi.security()
        )
                || rememberedPassword.isEmpty()) {
            passwordValue =
                    "";
        } else if (showPassword) {
            passwordValue =
                    rememberedPassword;
        } else {
            passwordValue =
                    "••••••••";
        }

        drawRightText(
                graphics,
                preferencesY
                        + ROW_HEIGHT
                        * 2,
                passwordValue,
                showPassword
                        ? TEXT
                        : SECONDARY,
                false
        );

        int ipv4LabelY =
                sy(
                        164
                );

        section(
                graphics,
                "IPV4 ADDRESS",
                ipv4LabelY
        );

        int ipv4Y =
                sy(
                        180
                );

        roundedRect(
                graphics,
                contentX,
                ipv4Y,
                contentWidth,
                ROW_HEIGHT * 5,
                12,
                CARD
        );

        rowPair(
                graphics,
                ipv4Y,
                "Configure IP",
                "Automatic"
        );

        divider(
                graphics,
                ipv4Y
                        + ROW_HEIGHT
        );

        rowPair(
                graphics,
                ipv4Y
                        + ROW_HEIGHT,
                "IP Address",
                emptyDash(
                        wifi.ipAddress()
                )
        );

        divider(
                graphics,
                ipv4Y
                        + ROW_HEIGHT
                        * 2
        );

        rowPair(
                graphics,
                ipv4Y
                        + ROW_HEIGHT
                        * 2,
                "Subnet Mask",
                emptyDash(
                        wifi.subnetMask()
                )
        );

        divider(
                graphics,
                ipv4Y
                        + ROW_HEIGHT
                        * 3
        );

        rowPair(
                graphics,
                ipv4Y
                        + ROW_HEIGHT
                        * 3,
                "Router",
                emptyDash(
                        wifi.gateway()
                )
        );

        divider(
                graphics,
                ipv4Y
                        + ROW_HEIGHT
                        * 4
        );

        rowPair(
                graphics,
                ipv4Y
                        + ROW_HEIGHT
                        * 4,
                "DNS",
                emptyDash(
                        wifi.dns()
                )
        );

        int rfLabelY =
                sy(
                        352
                );

        section(
                graphics,
                "RF DIAGNOSTICS",
                rfLabelY
        );

        int rfY =
                sy(
                        368
                );

        roundedRect(
                graphics,
                contentX,
                rfY,
                contentWidth,
                ROW_HEIGHT * 6,
                12,
                CARD
        );

        rowPair(
                graphics,
                rfY,
                "Signal",
                wifi.quality()
        );

        divider(
                graphics,
                rfY
                        + ROW_HEIGHT
        );

        rowPair(
                graphics,
                rfY
                        + ROW_HEIGHT,
                "RSSI",
                wifi.rssiDbm()
                        + " dBm"
        );

        divider(
                graphics,
                rfY
                        + ROW_HEIGHT
                        * 2
        );

        rowPair(
                graphics,
                rfY
                        + ROW_HEIGHT
                        * 2,
                "SINR",
                String.format(
                        Locale.ROOT,
                        "%.1f dB",
                        wifi.sinrDb()
                )
        );

        divider(
                graphics,
                rfY
                        + ROW_HEIGHT
                        * 3
        );

        rowPair(
                graphics,
                rfY
                        + ROW_HEIGHT
                        * 3,
                "Channel",
                wifi.channel()
                        + " / "
                        + String.format(
                        Locale.ROOT,
                        "%.3f GHz",
                        wifi.frequencyHz()
                                / 1.0E9
                )
        );

        divider(
                graphics,
                rfY
                        + ROW_HEIGHT
                        * 4
        );

        rowPair(
                graphics,
                rfY
                        + ROW_HEIGHT
                        * 4,
                "AP Distance",
                String.format(
                        Locale.ROOT,
                        "%.1f m",
                        wifi.distanceBlocks()
                )
        );

        divider(
                graphics,
                rfY
                        + ROW_HEIGHT
                        * 5
        );

        rowPair(
                graphics,
                rfY
                        + ROW_HEIGHT
                        * 5,
                "BSSID",
                emptyDash(
                        wifi.bssid()
                )
        );
    }

    private void rowLabel(
            GuiGraphics graphics,
            int y,
            String label
    ) {
        graphics.drawString(
                font,
                label,
                contentX + 13,
                y + 11,
                TEXT,
                false
        );
    }

    private void rowPair(
            GuiGraphics graphics,
            int y,
            String label,
            String value
    ) {
        rowLabel(
                graphics,
                y,
                label
        );

        drawRightText(
                graphics,
                y,
                value,
                SECONDARY,
                false
        );
    }

    private void drawRightText(
            GuiGraphics graphics,
            int y,
            String value,
            int color,
            boolean chevron
    ) {
        int rightPadding =
                chevron
                        ? 25
                        : 13;

        String shown =
                fit(
                        value,
                        102
                );

        int width =
                font.width(
                        shown
                );

        graphics.drawString(
                font,
                shown,
                contentX
                        + contentWidth
                        - rightPadding
                        - width,
                y + 11,
                color,
                false
        );

        if (chevron) {
            graphics.drawString(
                    font,
                    "›",
                    contentX
                            + contentWidth
                            - 15,
                    y + 11,
                    MUTED,
                    false
            );
        }
    }

    private void section(
            GuiGraphics graphics,
            String label,
            int y
    ) {
        graphics.drawString(
                font,
                label,
                contentX + 5,
                y,
                MUTED,
                false
        );
    }

    private void divider(
            GuiGraphics graphics,
            int y
    ) {
        graphics.fill(
                contentX + 13,
                y,
                contentX
                        + contentWidth
                        - 13,
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
                enabled
                        ? GREEN
                        : 0xFF636366
        );

        roundedRect(
                graphics,
                enabled
                        ? x + 19
                        : x + 3,
                y + 3,
                14,
                14,
                7,
                0xFFFFFFFF
        );
    }

    private int sy(
            int logicalY
    ) {
        return viewportTop
                + logicalY
                - scrollOffset;
    }

    private int logicalY(
            double mouseY
    ) {
        return (int) Math.floor(
                mouseY
                        - viewportTop
                        + scrollOffset
        );
    }

    private int clampScroll(
            int value
    ) {
        int max =
                Math.max(
                        0,
                        CONTENT_HEIGHT
                                - viewportHeight
                );

        return Math.max(
                0,
                Math.min(
                        max,
                        value
                )
        );
    }

    private String fit(
            String value,
            int maxWidth
    ) {
        String text =
                value == null
                        ? ""
                        : value;

        if (font.width(
                text
        ) <= maxWidth) {
            return text;
        }

        while (!text.isEmpty()
                && font.width(
                text + "..."
        ) > maxWidth) {
            text =
                    text.substring(
                            0,
                            text.length() - 1
                    );
        }

        return text + "...";
    }

    private String securityLabel(
            String value
    ) {
        if (isOpenSecurity(
                value
        )) {
            return "Open";
        }

        String text =
                value == null
                        ? ""
                        : value
                        .replace(
                                "signality:",
                                ""
                        )
                        .replace(
                                '_',
                                ' '
                        )
                        .trim();

        if (text.isBlank()) {
            return "Protected";
        }

        return text.toUpperCase(
                Locale.ROOT
        );
    }

    private boolean isOpenSecurity(
            String value
    ) {
        if (value == null
                || value.isBlank()) {
            return true;
        }

        String normalized =
                value.toLowerCase(
                        Locale.ROOT
                );

        return normalized.contains(
                "open"
        )
                || normalized.contains(
                "none"
        );
    }

    private String emptyDash(
            String value
    ) {
        return value == null
                || value.isBlank()
                ? "—"
                : value;
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button != 0) {
            return super.mouseClicked(
                    mouseX,
                    mouseY,
                    button
            );
        }

        if (clickedBack(
                mouseX,
                mouseY
        )) {
            minecraft.setScreen(
                    new IPhoneWifiScreen()
            );

            return true;
        }

        if (!inside(
                mouseX,
                mouseY,
                contentX,
                viewportTop,
                contentWidth,
                viewportHeight
        )) {
            return super.mouseClicked(
                    mouseX,
                    mouseY,
                    button
            );
        }

        PhoneNetworkState.WifiStatus wifi =
                PhoneNetworkState
                        .get()
                        .getWifi();

        int logicalY =
                logicalY(
                        mouseY
                );

        if (logicalY >= 0
                && logicalY < 36) {
            PhoneWifiClientPreferences
                    .forgetNetwork(
                            wifi.ssid(),
                            wifi.security(),
                            wifi.bssid()
                    );

            PhoneNetworkController
                    .get()
                    .forgetWifi(
                            wifi.bssid()
                    );

            minecraft.setScreen(
                    new IPhoneWifiScreen()
            );

            return true;
        }

        if (logicalY >= 52
                && logicalY
                < 52 + ROW_HEIGHT) {
            boolean next =
                    !PhoneWifiClientPreferences
                    .autoJoin(
                            wifi.bssid()
                    );

            PhoneWifiClientPreferences
                    .setAutoJoin(
                            wifi.bssid(),
                            next
                    );

            PhoneNetworkController
                    .get()
                    .setWifiAutoJoin(
                            wifi.bssid(),
                            next
                    );

            return true;
        }

        int passwordY =
                52
                        + ROW_HEIGHT
                        * 2;

        if (logicalY >= passwordY
                && logicalY
                < passwordY
                + ROW_HEIGHT
                && !isOpenSecurity(
                wifi.security()
        )) {
            String rememberedPassword =
                    PhoneWifiClientPreferences
                            .password(
                                    wifi.ssid(),
                                    wifi.security()
                            );

            if (!rememberedPassword.isEmpty()) {
                showPassword =
                        !showPassword;
            } else {
                showPassword =
                        false;
            }

            return true;
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
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
            scrollOffset =
                    clampScroll(
                            scrollOffset
                                    + (
                                    delta > 0.0
                                            ? -28
                                            : 28
                            )
                    );

            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                delta
        );
    }
}
