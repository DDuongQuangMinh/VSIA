package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IPhoneCellularDiagnosticsScreen extends IPhoneScreen {
    public IPhoneCellularDiagnosticsScreen() {
        super(Component.literal("Cellular Diagnostics"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneBase(graphics);
        renderHeader(graphics, "Cellular", "Diagnostics");

        var c = PhoneNetworkState.get().getCellular();
        int x = phoneX + 18;
        int y = phoneY + 82;

        y = pair(graphics, x, y, "RRC State", c.rrcState());
        y = pair(graphics, x, y, "NAS State", c.nasState());
        y = pair(graphics, x, y, "PDU Session", c.pduState());

        y += 12;
        y = pair(graphics, x, y, "gNB ID", Integer.toString(c.gnbId()));
        y = pair(graphics, x, y, "Cell ID", Integer.toString(c.cellId()));
        y = pair(graphics, x, y, "TAC", Integer.toString(c.tac()));
        y = pair(graphics, x, y, "PLMN", c.plmn());

        y += 12;
        y = pair(graphics, x, y, "RSRP", c.rsrpDbm() + " dBm");
        y = pair(graphics, x, y, "RSRQ", c.rsrqDb() + " dB");
        y = pair(graphics, x, y, "SINR", c.sinrDb() + " dB");

        renderHomeIndicator(graphics);
    }

    private int pair(GuiGraphics g, int x, int y, String key, String value) {
        g.drawString(font, key, x, y, 0xFFA8A8AD, false);
        int vw = font.width(value);
        g.drawString(font, value, phoneX + PHONE_WIDTH - 18 - vw, y, 0xFFFFFFFF, false);
        return y + 23;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && clickedBack(mouseX, mouseY)) {
            minecraft.setScreen(new IPhoneCellularScreen());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
