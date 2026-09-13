package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.network.PhoneNetworkController;
import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberClientState;
import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class IPhoneCellularScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int DIVIDER = 0xFF3A3A3C;
    private static final int BLUE = 0xFF0A84FF;
    private static final int GREEN = 0xFF30D158;

    private int contentX;
    private int contentWidth;
    private int dataY;
    private int simY;
    private int networkY;
    private int packetY;
    private int diagnosticsY;

    public IPhoneCellularScreen() {
        super(Component.literal("Cellular"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        dataY = phoneY + 76;
        simY = dataY + 61;
        networkY = simY + 63;
        packetY = networkY + 117;
        diagnosticsY = packetY + 95;

        PhoneNetworkController.get().requestRefresh();
        PhoneSubscriberClientState.get().requestRefresh();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(graphics, BG);
        renderStatusBar(graphics);
        renderHeader(graphics, "Settings", "Cellular");

        PhoneNetworkState.CellularStatus cellular =
                PhoneNetworkState.get().getCellular();
        PhoneSubscriberSnapshot subscriber =
                PhoneSubscriberClientState.get().snapshot();

        roundedRect(graphics, contentX, dataY, contentWidth, 48, 14, CARD);
        graphics.drawString(font, "Cellular Data", contentX + 13, dataY + 18, TEXT, false);
        drawToggle(
                graphics,
                contentX + contentWidth - 47,
                dataY + 14,
                subscriber.cellularDataEnabled()
        );

        roundedRect(graphics, contentX, simY, contentWidth, 48, 14, CARD);
        graphics.drawString(font, "SIMs", contentX + 13, simY + 10, TEXT, false);
        String simSummary = subscriber.hasActiveSubscription()
                ? subscriber.carrier() + "  " + subscriber.activeType()
                : "No SIM";
        graphics.drawString(
                font,
                fit(simSummary, 115),
                contentX + 13,
                simY + 27,
                subscriber.hasActiveSubscription() ? MUTED : 0xFFFF9F0A,
                false
        );
        graphics.drawString(font, ">", contentX + contentWidth - 15, simY + 19, BLUE, false);

        graphics.drawString(font, "NETWORK", contentX + 4, networkY - 16, 0xFF8E8E93, false);
        roundedRect(graphics, contentX, networkY, contentWidth, 101, 14, CARD);

        pair(graphics, networkY, "Carrier",
                subscriber.hasActiveSubscription()
                        ? subscriber.carrier()
                        : "No Service");
        divider(graphics, networkY + 25);
        pair(graphics, networkY + 25, "Radio",
                subscriber.hasActiveSubscription() && cellular.registered()
                        ? fit(cellular.radioLabel() + " " + cellular.band(), 95)
                        : "-");
        divider(graphics, networkY + 50);
        pair(graphics, networkY + 50, "Signal",
                subscriber.hasActiveSubscription() && cellular.registered()
                        ? cellular.quality()
                        : "NO SERVICE");
        divider(graphics, networkY + 75);
        pair(graphics, networkY + 75, "Distance",
                subscriber.hasActiveSubscription()
                        && Double.isFinite(cellular.distanceBlocks())
                        ? String.format(Locale.ROOT, "%.0f m", cellular.distanceBlocks())
                        : "-");

        graphics.drawString(font, "PACKET DATA", contentX + 4, packetY - 16, 0xFF8E8E93, false);
        roundedRect(graphics, contentX, packetY, contentWidth, 79, 14, CARD);
        pair(graphics, packetY, "PDU Session",
                PhoneNetworkState.get().isCellularUsable()
                        ? cellular.pduState()
                        : "INACTIVE");
        divider(graphics, packetY + 26);
        pair(graphics, packetY + 26, "Estimated DL",
                PhoneNetworkState.get().isCellularUsable()
                        ? String.format(Locale.ROOT, "%.1f Mbps", cellular.estimatedDownlinkMbps())
                        : "0.0 Mbps");
        divider(graphics, packetY + 52);
        pair(graphics, packetY + 52, "Subscriber",
                subscriber.authenticated() ? "AUTHENTICATED" : "NOT AUTHENTICATED");

        roundedRect(graphics, contentX, diagnosticsY, contentWidth, 28, 12, CARD);
        graphics.drawCenteredString(
                font,
                "Diagnostics >",
                phoneX + PHONE_WIDTH / 2,
                diagnosticsY + 10,
                BLUE
        );

        renderHomeIndicator(graphics);
    }

    private void pair(GuiGraphics graphics, int y, String key, String value) {
        graphics.drawString(font, key, contentX + 13, y + 9, TEXT, false);
        String shown = fit(value, 98);
        graphics.drawString(
                font,
                shown,
                contentX + contentWidth - font.width(shown) - 13,
                y + 9,
                MUTED,
                false
        );
    }

    private void divider(GuiGraphics graphics, int y) {
        graphics.fill(
                contentX + 13,
                y,
                contentX + contentWidth - 13,
                y + 1,
                DIVIDER
        );
    }

    private void drawToggle(GuiGraphics graphics, int x, int y, boolean enabled) {
        roundedRect(graphics, x, y, 36, 20, 10, enabled ? GREEN : 0xFF636366);
        roundedRect(graphics, enabled ? x + 19 : x + 3, y + 3, 14, 14, 7, 0xFFFFFFFF);
    }

    private String fit(String value, int maxWidth) {
        String text = value == null ? "" : value;
        if (font.width(text) <= maxWidth) {
            return text;
        }
        while (!text.isEmpty() && font.width(text + "...") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (clickedBack(mouseX, mouseY)) {
                minecraft.setScreen(new IPhoneSettingsScreen());
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX + contentWidth - 54,
                    dataY + 5,
                    50,
                    38
            )) {
                boolean enabled = PhoneSubscriberClientState.get()
                        .snapshot()
                        .cellularDataEnabled();
                PhoneNetworkController.get().setCellularEnabled(!enabled);
                return true;
            }

            if (inside(mouseX, mouseY, contentX, simY, contentWidth, 48)) {
                minecraft.setScreen(new IPhoneSimManagerScreen());
                return true;
            }

            if (inside(mouseX, mouseY, contentX, diagnosticsY, contentWidth, 28)) {
                minecraft.setScreen(new IPhoneCellularDiagnosticsScreen());
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
