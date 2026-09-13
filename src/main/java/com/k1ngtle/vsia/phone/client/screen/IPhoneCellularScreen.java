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

        int firstY =
                phoneY + 78;

        roundedRect(
                graphics,
                contentX,
                firstY,
                contentWidth,
                52,
                14,
                0xFF2C2C2E
        );

        graphics.drawString(
                font,
                "Cellular Data",
                contentX + 13,
                firstY + 19,
                0xFFFFFFFF,
                false
        );

        drawToggle(
                graphics,
                contentX
                        + contentWidth
                        - 47,
                firstY + 16,
                cellular.enabled()
        );

        int serviceY =
                firstY + 76;

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
                132,
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
                serviceY + 33
        );

        pair(
                graphics,
                serviceY + 33,
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
                serviceY + 66
        );

        pair(
                graphics,
                serviceY + 66,
                "Signal",
                cellular.quality()
        );

        divider(
                graphics,
                serviceY + 99
        );

        pair(
                graphics,
                serviceY + 99,
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

        int dataY =
                serviceY + 154;

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
                99,
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
                dataY + 33
        );

        pair(
                graphics,
                dataY + 33,
                "Estimated DL",
                String.format(
                        Locale.ROOT,
                        "%.1f Mbps",
                        cellular.estimatedDownlinkMbps()
                )
        );

        divider(
                graphics,
                dataY + 66
        );

        pair(
                graphics,
                dataY + 66,
                "Satellite NTN",
                "Not Provisioned"
        );

        graphics.drawCenteredString(
                font,
                "Diagnostics >",
                phoneX
                        + PHONE_WIDTH
                        / 2,
                dataY + 120,
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
                y + 12,
                0xFFFFFFFF,
                false
        );

        String shown =
                fit(
                        value,
                        104
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
                y + 12,
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
                    phoneY + 84,
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

            int diagnosticsY =
                    phoneY + 78
                            + 76
                            + 154
                            + 108;

            if (mouseY >= diagnosticsY
                    && mouseY <= diagnosticsY + 35) {
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
