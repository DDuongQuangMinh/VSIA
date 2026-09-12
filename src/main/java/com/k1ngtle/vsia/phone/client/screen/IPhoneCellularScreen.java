package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.network.PhoneNetworkController;
import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IPhoneCellularScreen extends IPhoneScreen {
    public IPhoneCellularScreen() {
        super(Component.literal("Cellular"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneBase(graphics);
        renderHeader(graphics, "Settings", "Cellular");

        var c = PhoneNetworkState.get().getCellular();
        int x = phoneX + 18;
        int y = phoneY + 82;

        y = pair(graphics, x, y, "Cellular Data", c.enabled() ? "ON" : "OFF");
        y += 12;

        graphics.drawString(font, "NETWORK", x, y, 0xFF8E8E93, false);
        y += 20;
        y = pair(graphics, x, y, "Carrier", c.carrier());
        y = pair(graphics, x, y, "Network Selection", "Automatic");

        y += 12;
        graphics.drawString(font, "STATUS", x, y, 0xFF8E8E93, false);
        y += 20;
        y = pair(graphics, x, y, "Radio", c.radioLabel() + " " + c.architecture());
        y = pair(graphics, x, y, "Serving Cell", Integer.toString(c.cellId()));
        y = pair(graphics, x, y, "Band", c.band());

        y += 12;
        graphics.drawString(font, "CELLULAR DATA", x, y, 0xFF8E8E93, false);
        y += 20;
        y = pair(graphics, x, y, "IP Address", c.ipAddress());
        y = pair(graphics, x, y, "DNN", c.dnn());
        y = pair(graphics, x, y, "5QI", Integer.toString(c.fiveQi()));

        y += 8;
        graphics.drawString(font, "Tap lower area for diagnostics", x, y, 0xFF8E8E93, false);

        renderHomeIndicator(graphics);
    }

    private int pair(GuiGraphics g, int x, int y, String key, String value) {
        g.drawString(font, key, x, y, 0xFFA8A8AD, false);
        int vw = font.width(value);
        g.drawString(font, value, phoneX + PHONE_WIDTH - 18 - vw, y, 0xFFFFFFFF, false);
        return y + 21;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (clickedBack(mouseX, mouseY)) {
                minecraft.setScreen(new IPhoneSettingsScreen());
                return true;
            }

            if (inside(mouseX, mouseY, phoneX + 12, phoneY + 76, PHONE_WIDTH - 24, 40)) {
                PhoneNetworkController.get().setCellularEnabled(!PhoneNetworkState.get().getCellular().enabled());
                return true;
            }

            if (inside(mouseX, mouseY, phoneX + 12, phoneY + 300, PHONE_WIDTH - 24, 90)) {
                minecraft.setScreen(new IPhoneCellularDiagnosticsScreen());
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
