package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.network.PhoneNetworkController;
import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class IPhoneCellularScreen
        extends IPhoneScreen {

    private int contentX;
    private int contentWidth;
    private int firstY;
    private int serviceY;
    private int dataY;
    private int diagnosticsY;

    public IPhoneCellularScreen() {
        super(
                Component.literal(
                        "Cellular"
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

        firstY =
                phoneY + 76;

        serviceY =
                firstY + 66;

        dataY =
                serviceY + 136;

        diagnosticsY =
                dataY + 108;

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

        renderHeader(
                graphics,
                "Settings",
                "Cellular"
        );

        PhoneNetworkState.CellularStatus cellular =
                PhoneNetworkState
                        .get()
                        .getCellular();

        roundedRect(
                graphics,
                contentX,
                firstY,
                contentWidth,
                48,
                14,
                0xFF2C2C2E
        );

        graphics.drawString(
                font,
                "Cellular Data",
                contentX + 13,
                firstY + 18,
                0xFFFFFFFF,
                false
        );

        drawToggle(
                graphics,
                contentX
                        + contentWidth
                        - 47,
                firstY + 14,
                cellular.enabled()
        );

        graphics.drawString(
                font,
                "NETWORK",
                contentX + 4,
                serviceY - 16,
                0xFF8E8E93,
                false
        );

        roundedRect(
                graphics,
                contentX,
                serviceY,
                contentWidth,
                116,
                14,
                0xFF2C2C2E
        );

        pair(
                graphics,
                serviceY,
                "Carrier",
                cellular.registered()
                        ? cellular.carrier()
                        : "No Service"
        );

        divider(
                graphics,
                serviceY + 29
        );

        pair(
                graphics,
                serviceY + 29,
                "Radio",
                cellular.radioLabel()
                        .isBlank()
                        ? "-"
                        : cellular.radioLabel()
                        + " "
                        + cellular.band()
        );

        divider(
                graphics,
                serviceY + 58
        );

        pair(
                graphics,
                serviceY + 58,
                "Signal",
                cellular.quality()
        );

        divider(
                graphics,
                serviceY + 87
        );

        pair(
                graphics,
                serviceY + 87,
                "Distance",
                Double.isFinite(
                        cellular.distanceBlocks()
                )
                        ? String.format(
                        Locale.ROOT,
                        "%.0f m",
                        cellular.distanceBlocks()
                )
                        : "-"
        );

        graphics.drawString(
                font,
                "PACKET DATA",
                contentX + 4,
                dataY - 16,
                0xFF8E8E93,
                false
        );

        roundedRect(
                graphics,
                contentX,
                dataY,
                contentWidth,
                90,
                14,
                0xFF2C2C2E
        );

        pair(
                graphics,
                dataY,
                "PDU Session",
                cellular.pduState()
        );

        divider(
                graphics,
                dataY + 30
        );

        pair(
                graphics,
                dataY + 30,
                "Estimated DL",
                String.format(
                        Locale.ROOT,
                        "%.1f Mbps",
                        cellular.estimatedDownlinkMbps()
                )
        );

        divider(
                graphics,
                dataY + 60
        );

        pair(
                graphics,
                dataY + 60,
                "Satellite NTN",
                "Not Provisioned"
        );

        roundedRect(
                graphics,
                contentX,
                diagnosticsY,
                contentWidth,
                28,
                12,
                0xFF2C2C2E
        );

        graphics.drawCenteredString(
                font,
                "Diagnostics >",
                phoneX
                        + PHONE_WIDTH
                        / 2,
                diagnosticsY + 10,
                0xFF0A84FF
        );

        renderHomeIndicator(
                graphics
        );
    }

    private void pair(
            GuiGraphics graphics,
            int y,
            String key,
            String value
    ) {
        graphics.drawString(
                font,
                key,
                contentX + 13,
                y + 10,
                0xFFFFFFFF,
                false
        );

        String shown =
                fit(
                        value,
                        98
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
                        - width
                        - 13,
                y + 10,
                0xFFAEAEB2,
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
                0xFF3A3A3C
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
                        ? 0xFF30D158
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

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            if (clickedBack(
                    mouseX,
                    mouseY
            )) {
                minecraft.setScreen(
                        new IPhoneSettingsScreen()
                );

                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX
                            + contentWidth
                            - 54,
                    firstY + 5,
                    50,
                    38
            )) {
                PhoneNetworkState.CellularStatus cellular =
                        PhoneNetworkState
                                .get()
                                .getCellular();

                PhoneNetworkController
                        .get()
                        .setCellularEnabled(
                                !cellular.enabled()
                        );

                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    diagnosticsY,
                    contentWidth,
                    28
            )) {
                minecraft.setScreen(
                        new IPhoneCellularDiagnosticsScreen()
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
