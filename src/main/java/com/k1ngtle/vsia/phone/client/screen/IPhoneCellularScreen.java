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
    private static final int SECTION = 0xFF8E8E93;

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

        dataY = phoneY + 72;
        simY = dataY + 52;
        networkY = simY + 60;
        packetY = networkY + 105;
        diagnosticsY = packetY + 78;

        PhoneNetworkController.get().requestRefresh();
        PhoneSubscriberClientState.get().requestRefresh();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(graphics, BG);
        renderStatusBar(graphics);
        renderHeader(graphics, "Settings", "Cellular");

        PhoneNetworkState.CellularStatus cellular = PhoneNetworkState.get().getCellular();
        PhoneSubscriberSnapshot subscriber = PhoneSubscriberClientState.get().snapshot();

        beginPhoneClip(graphics, 68);

        roundedRect(graphics, contentX, dataY, contentWidth, 44, 14, CARD);
        drawUiText(graphics, "Cellular Data", contentX + 13, dataY + 16, TEXT);
        drawToggle(
                graphics,
                contentX + contentWidth - 47,
                dataY + 12,
                subscriber.cellularDataEnabled()
        );

        roundedRect(graphics, contentX, simY, contentWidth, 46, 14, CARD);
        drawUiText(graphics, "SIMs", contentX + 13, simY + 8, TEXT);
        String simSummary = subscriber.hasActiveSubscription()
                ? subscriber.carrier() + "  " + subscriber.activeType()
                : "No SIM";
        drawUiText(
                graphics,
                fitUi(simSummary, contentWidth - 46),
                contentX + 13,
                simY + 25,
                subscriber.hasActiveSubscription() ? MUTED : 0xFFFF9F0A
        );
        drawUiText(graphics, ">", contentX + contentWidth - 15, simY + 17, BLUE);

        drawUiText(graphics, "NETWORK", contentX + 4, networkY - 14, SECTION);
        roundedRect(graphics, contentX, networkY, contentWidth, 89, 14, CARD);

        pair(graphics, networkY, "Carrier",
                subscriber.hasActiveSubscription()
                        ? subscriber.carrier()
                        : "No Service");
        divider(graphics, networkY + 22);
        pair(graphics, networkY + 22, "Radio",
                subscriber.hasActiveSubscription() && cellular.registered()
                        ? cellular.radioLabel() + " " + cellular.band()
                        : "-");
        divider(graphics, networkY + 44);
        pair(graphics, networkY + 44, "Signal",
                subscriber.hasActiveSubscription() && cellular.registered()
                        ? cellular.quality()
                        : "NO SERVICE");
        divider(graphics, networkY + 66);
        pair(graphics, networkY + 66, "Distance",
                subscriber.hasActiveSubscription() && Double.isFinite(cellular.distanceBlocks())
                        ? String.format(Locale.ROOT, "%.0f m", cellular.distanceBlocks())
                        : "-");

        drawUiText(graphics, "PACKET DATA", contentX + 4, packetY - 14, SECTION);
        roundedRect(graphics, contentX, packetY, contentWidth, 67, 14, CARD);
        pair(graphics, packetY, "PDU Session",
                PhoneNetworkState.get().isCellularUsable()
                        ? cellular.pduState()
                        : "INACTIVE");
        divider(graphics, packetY + 22);
        pair(graphics, packetY + 22, "Estimated DL",
                PhoneNetworkState.get().isCellularUsable()
                        ? String.format(Locale.ROOT, "%.1f Mbps", cellular.estimatedDownlinkMbps())
                        : "0.0 Mbps");
        divider(graphics, packetY + 44);
        pair(graphics, packetY + 44, "Subscriber",
                subscriber.authenticated() ? "AUTHENTICATED" : "NOT AUTHENTICATED");

        roundedRect(graphics, contentX, diagnosticsY, contentWidth, 28, 12, CARD);
        drawUiCentered(
                graphics,
                "Diagnostics >",
                phoneX + PHONE_WIDTH / 2,
                diagnosticsY + 9,
                BLUE
        );

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    private void pair(GuiGraphics graphics, int y, String key, String value) {
        int left = contentX + 13;
        int right = contentX + contentWidth - 13;
        drawUiText(graphics, key, left, y + 7, TEXT);

        int valueWidth = Math.max(42, right - (left + uiWidth(key) + 10));
        String shown = fitUi(value, valueWidth);
        drawUiText(
                graphics,
                shown,
                right - uiWidth(shown),
                y + 7,
                MUTED
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
                    dataY + 3,
                    50,
                    38
            )) {
                boolean enabled = PhoneSubscriberClientState.get()
                        .snapshot()
                        .cellularDataEnabled();
                PhoneNetworkController.get().setCellularEnabled(!enabled);
                return true;
            }

            if (inside(mouseX, mouseY, contentX, simY, contentWidth, 46)) {
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
