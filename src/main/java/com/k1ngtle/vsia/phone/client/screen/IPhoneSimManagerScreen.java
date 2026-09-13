package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberClientState;
import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberSnapshot;
import com.k1ngtle.vsia.phone.subscriber.packet.C2SPhoneSubscriberActionPacket;
import com.k1ngtle.vsia.signality.internet.field.FieldDeviceNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneSimManagerScreen extends IPhoneScreen {
    private static final int BG = 0xFF111216;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int BLUE = 0xFF0A84FF;
    private static final int GREEN = 0xFF30D158;
    private static final int RED = 0xFFFF453A;

    private int x;
    private int w;
    private int statusY;
    private int physicalY;
    private int esimY;
    private int dataY;

    public IPhoneSimManagerScreen() {
        super(Component.literal("SIMs"));
    }

    @Override
    protected void init() {
        super.init();
        x = phoneX + 14;
        w = PHONE_WIDTH - 28;
        statusY = phoneY + 77;
        physicalY = statusY + 89;
        esimY = physicalY + 78;
        dataY = esimY + 78;
        PhoneSubscriberClientState.get().requestRefresh();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(graphics, BG);
        renderStatusBar(graphics);
        renderHeader(graphics, "Cellular", "SIMs");

        PhoneSubscriberSnapshot s = PhoneSubscriberClientState.get().snapshot();

        roundedRect(graphics, x, statusY, w, 72, 14, CARD);
        pair(graphics, statusY + 6, "Carrier", s.carrier().isBlank() ? "No SIM" : s.carrier());
        pair(graphics, statusY + 25, "Number", s.msisdn().isBlank() ? "-" : s.msisdn());
        pair(graphics, statusY + 44, "Status", s.serviceStatus());

        drawUiText(graphics, "PHYSICAL SIM", x + 4, physicalY - 14, 0xFF8E8E93);
        roundedRect(graphics, x, physicalY, w, 60, 14, CARD);
        drawUiText(graphics,
                s.physicalSimPresent() ? "Physical SIM installed" : "No physical SIM",
                x + 12,
                physicalY + 11,
                TEXT);
        drawUiText(graphics,
                fitUi(s.physicalSimPresent() ? "Tap to select / remove" : "Insert from inventory", w - 42),
                x + 12,
                physicalY + 31,
                MUTED);
        drawUiText(graphics, ">", x + w - 15, physicalY + 25, BLUE);

        drawUiText(graphics, "eSIM", x + 4, esimY - 14, 0xFF8E8E93);
        roundedRect(graphics, x, esimY, w, 60, 14, CARD);
        drawUiText(graphics,
                s.esimPresent() ? "VSIA Mobile eSIM" : "Add eSIM",
                x + 12,
                esimY + 11,
                TEXT);
        drawUiText(graphics,
                fitUi(s.esimPresent() ? "Tap to select profile" : "Download through SM-DP+ over Wi-Fi", w - 42),
                x + 12,
                esimY + 31,
                MUTED);
        drawUiText(graphics, ">", x + w - 15, esimY + 25, BLUE);

        roundedRect(graphics, x, dataY, w, 46, 14, CARD);
        drawUiText(graphics, "Cellular Data", x + 12, dataY + 18, TEXT);
        drawToggle(graphics, x + w - 45, dataY + 13, s.cellularDataEnabled());

        drawUiCentered(
                graphics,
                "DEV: Provision Test SIM",
                phoneX + PHONE_WIDTH / 2,
                dataY + 61,
                BLUE
        );

        drawUiCentered(
                graphics,
                "EID " + fitUi(s.eid(), 150),
                phoneX + PHONE_WIDTH / 2,
                dataY + 81,
                0xFF6F6F73
        );

        renderHomeIndicator(graphics);
    }

    private void pair(GuiGraphics graphics, int y, String key, String value) {
        drawUiText(graphics, key, x + 12, y + 7, TEXT);
        int right = x + w - 12;
        int max = Math.max(44, right - (x + 12 + uiWidth(key) + 10));
        String shown = fitUi(value, max);
        drawUiText(graphics, shown, right - uiWidth(shown), y + 7, MUTED);
    }

    private void drawToggle(GuiGraphics graphics, int tx, int ty, boolean enabled) {
        roundedRect(graphics, tx, ty, 34, 20, 10, enabled ? GREEN : 0xFF636366);
        roundedRect(graphics, enabled ? tx + 17 : tx + 3, ty + 3, 14, 14, 7, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (clickedBack(mouseX, mouseY)) {
                minecraft.setScreen(new IPhoneCellularScreen());
                return true;
            }

            PhoneSubscriberSnapshot s = PhoneSubscriberClientState.get().snapshot();

            if (inside(mouseX, mouseY, x, physicalY, w, 60)) {
                if (s.physicalSimPresent()) {
                    if ("PHYSICAL".equalsIgnoreCase(s.activeType())) {
                        FieldDeviceNetwork.sendToServer(C2SPhoneSubscriberActionPacket.removePhysical());
                    } else {
                        FieldDeviceNetwork.sendToServer(C2SPhoneSubscriberActionPacket.selectPhysical());
                    }
                } else {
                    FieldDeviceNetwork.sendToServer(C2SPhoneSubscriberActionPacket.insertPhysical());
                }
                return true;
            }

            if (inside(mouseX, mouseY, x, esimY, w, 60)) {
                if (s.esimPresent()) {
                    FieldDeviceNetwork.sendToServer(C2SPhoneSubscriberActionPacket.selectEsim());
                } else {
                    minecraft.setScreen(new IPhoneEsimActivationScreen());
                }
                return true;
            }

            if (inside(mouseX, mouseY, x + w - 52, dataY + 4, 48, 38)) {
                FieldDeviceNetwork.sendToServer(
                        C2SPhoneSubscriberActionPacket.dataEnabled(!s.cellularDataEnabled())
                );
                return true;
            }

            if (inside(mouseX, mouseY, x + 20, dataY + 52, w - 40, 28)) {
                FieldDeviceNetwork.sendToServer(C2SPhoneSubscriberActionPacket.provisionTestSim());
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
