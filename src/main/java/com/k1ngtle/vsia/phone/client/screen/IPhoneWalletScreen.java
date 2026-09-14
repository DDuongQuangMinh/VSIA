package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneCoreAppsState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneWalletScreen extends IPhoneScreen {
    private static final int BG = 0xFF111113;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int GREEN = 0xFF30D158;
    private static final int RED = 0xFFFF453A;

    private int x;
    private int w;
    private int y;

    public IPhoneWalletScreen() {
        super(Component.literal("Wallet"));
    }

    @Override
    protected void init() {
        super.init();
        PhoneCoreAppsState.ensureLoaded();
        x = phoneX + 14;
        w = PHONE_WIDTH - 28;
        y = phoneY + 84;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(g, BG);
        renderStatusBar(g);
        drawUiCentered(g, "Wallet", phoneX + PHONE_WIDTH / 2, phoneY + 51, TEXT);
        beginPhoneClip(g, 68);

        roundedRect(g, x, y, w, 112, 16, 0xFF303238);
        g.fill(x + 15, y + 17, x + w - 15, y + 23, 0xFFFF453A);
        g.fill(x + 15, y + 24, x + w - 15, y + 30, 0xFFFFD60A);
        g.fill(x + 15, y + 31, x + w - 15, y + 37, GREEN);
        drawUiText(g, "VS:IA Wallet", x + 15, y + 51, TEXT);
        drawUiText(g, "Available Balance", x + 15, y + 70, MUTED);
        drawUiText(g, String.format("%.2f credits", PhoneCoreAppsState.walletBalance()), x + 15, y + 88, TEXT);

        roundedRect(g, x, y + 132, w, 44, 12, CARD);
        drawUiText(g, "Test Purchase", x + 12, y + 147, TEXT);
        drawUiText(g, "4.25", x + w - 40, y + 147, GREEN);

        roundedRect(g, x, y + 186, w, 44, 12, CARD);
        drawUiText(g, "Large Purchase", x + 12, y + 201, TEXT);
        drawUiText(g, "25.00", x + w - 46, y + 201, RED);

        endPhoneClip(g);
        renderHomeIndicator(g);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, x, y + 132, w, 44)) {
                PhoneCoreAppsState.walletPurchase(4.25D);
                return true;
            }

            if (inside(mouseX, mouseY, x, y + 186, w, 44)) {
                PhoneCoreAppsState.walletPurchase(25.0D);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
