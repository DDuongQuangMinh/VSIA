package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.network.PhoneNetworkController;
import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class IPhoneCellularDiagnosticsScreen
        extends IPhoneScreen {

    public IPhoneCellularDiagnosticsScreen() {
        super(
                Component.literal(
                        "Cellular Diagnostics"
                )
        );
    }

    @Override
    protected void init() {
        super.init();

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
        renderPhoneBase(
                graphics
        );

        renderHeader(
                graphics,
                "Cellular",
                "Diagnostics"
        );

        PhoneNetworkState.CellularStatus cellular =
                PhoneNetworkState
                        .get()
                        .getCellular();

        int x =
                phoneX + 18;

        int y =
                phoneY + 80;

        y = pair(
                graphics,
                x,
                y,
                "RRC State",
                cellular.rrcState()
        );

        y = pair(
                graphics,
                x,
                y,
                "NAS State",
                cellular.nasState()
        );

        y = pair(
                graphics,
                x,
                y,
                "PDU Session",
                cellular.pduState()
        );

        y += 8;

        y = pair(
                graphics,
                x,
                y,
                "Carrier",
                cellular.carrier()
        );

        y = pair(
                graphics,
                x,
                y,
                "PLMN",
                cellular.plmn()
        );

        y = pair(
                graphics,
                x,
                y,
                "gNB ID",
                Integer.toString(
                        cellular.gnbId()
                )
        );

        y = pair(
                graphics,
                x,
                y,
                "PCI / Cell",
                Integer.toString(
                        cellular.cellId()
                )
        );

        y = pair(
                graphics,
                x,
                y,
                "Band",
                cellular.band()
        );

        y += 8;

        y = pair(
                graphics,
                x,
                y,
                "RSRP",
                cellular.rsrpDbm()
                        + " dBm"
        );

        y = pair(
                graphics,
                x,
                y,
                "RSRQ",
                String.format(
                        Locale.ROOT,
                        "%.1f dB",
                        cellular.rsrqDb()
                )
        );

        y = pair(
                graphics,
                x,
                y,
                "SINR",
                String.format(
                        Locale.ROOT,
                        "%.1f dB",
                        cellular.sinrDb()
                )
        );

        y = pair(
                graphics,
                x,
                y,
                "Site Distance",
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

        y = pair(
                graphics,
                x,
                y,
                "Est. Downlink",
                String.format(
                        Locale.ROOT,
                        "%.1f Mbps",
                        cellular.estimatedDownlinkMbps()
                )
        );

        y += 8;

        pair(
                graphics,
                x,
                y,
                "Satellite NTN",
                "Not provisioned"
        );

        graphics.drawCenteredString(
                font,
                fit(
                        cellular.status(),
                        PHONE_WIDTH - 40
                ),
                phoneX
                        + PHONE_WIDTH
                        / 2,
                phoneY + PHONE_HEIGHT - 48,
                0xFF8E8E93
        );

        renderHomeIndicator(
                graphics
        );
    }

    private int pair(
            GuiGraphics graphics,
            int x,
            int y,
            String key,
            String value
    ) {
        graphics.drawString(
                font,
                key,
                x,
                y,
                0xFFA8A8AD,
                false
        );

        String shown =
                fit(
                        value,
                        118
                );

        int width =
                font.width(
                        shown
                );

        graphics.drawString(
                font,
                shown,
                phoneX
                        + PHONE_WIDTH
                        - 18
                        - width,
                y,
                0xFFFFFFFF,
                false
        );

        return y + 22;
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
        if (button == 0
                && clickedBack(
                mouseX,
                mouseY
        )) {
            minecraft.setScreen(
                    new IPhoneCellularScreen()
            );

            return true;
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }
}
