package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneCoreAppsState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneAppStoreScreen extends IPhoneScreen {
    private static final int BG = 0xFFF2F2F7;
    private static final int CARD = 0xFFFFFFFF;
    private static final int TEXT = 0xFF151515;
    private static final int MUTED = 0xFF77777C;
    private static final int BLUE = 0xFF007AFF;

    private int x;
    private int w;
    private int y;

    public IPhoneAppStoreScreen() {
        super(Component.literal("App Store"));
    }

    @Override
    protected void init() {
        super.init();
        PhoneCoreAppsState.ensureLoaded();
        x = phoneX + 14;
        w = PHONE_WIDTH - 28;
        y = phoneY + 82;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(g, BG);
        renderStatusBar(g);
        drawUiCentered(g, "App Store", phoneX + PHONE_WIDTH / 2, phoneY + 51, TEXT);
        beginPhoneClip(g, 68);

        drawUiText(g, "FEATURED", x + 4, y - 17, MUTED);
        appRow(g, y, "Fitness", "Workout tracking", PhoneCoreAppsState.fitnessInstalled());
        appRow(g, y + 58, "VS:IA Maps", "Live world coordinates", true);
        appRow(g, y + 116, "VS:IA News", "World status briefings", true);
        appRow(g, y + 174, "VS:IA Podcasts", "Engineering episodes", true);

        endPhoneClip(g);
        renderHomeIndicator(g);
    }

    private void appRow(GuiGraphics g, int top, String name, String detail, boolean installed) {
        roundedRect(g, x, top, w, 50, 12, CARD);
        roundedRect(g, x + 8, top + 8, 34, 34, 9, 0xFF0A84FF);
        drawUiCentered(g, name.substring(0, 1), x + 25, top + 20, 0xFFFFFFFF);
        drawUiText(g, name, x + 50, top + 10, TEXT);
        drawUiText(g, fitUi(detail, 95), x + 50, top + 27, MUTED);

        String action = installed ? "OPEN" : "GET";
        drawUiText(g, action, x + w - uiWidth(action) - 12, top + 20, BLUE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, x, y, w, 50)) {
                if (!PhoneCoreAppsState.fitnessInstalled()) {
                    PhoneCoreAppsState.setFitnessInstalled(true);
                } else {
                    minecraft.setScreen(new IPhoneFitnessScreen());
                }
                return true;
            }

            if (inside(mouseX, mouseY, x, y + 58, w, 50)) {
                minecraft.setScreen(new IPhoneMapsScreen());
                return true;
            }

            if (inside(mouseX, mouseY, x, y + 116, w, 50)) {
                minecraft.setScreen(new IPhoneNewsScreen());
                return true;
            }

            if (inside(mouseX, mouseY, x, y + 174, w, 50)) {
                minecraft.setScreen(new IPhonePodcastsScreen());
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
