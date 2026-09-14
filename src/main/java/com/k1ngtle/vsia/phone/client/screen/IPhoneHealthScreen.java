package com.k1ngtle.vsia.phone.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class IPhoneHealthScreen extends IPhoneScreen {
    private static final int BG = 0xFFF2F2F7;
    private static final int CARD = 0xFFFFFFFF;
    private static final int TEXT = 0xFF151515;
    private static final int MUTED = 0xFF77777C;
    private static final int RED = 0xFFFF375F;
    private static final int BLUE = 0xFF0A84FF;

    private int x;
    private int w;
    private int y;
    private int fitnessY;

    public IPhoneHealthScreen() {
        super(Component.literal("Health"));
    }

    @Override
    protected void init() {
        super.init();
        x = phoneX + 14;
        w = PHONE_WIDTH - 28;
        y = phoneY + 80;
        fitnessY = phoneY + PHONE_HEIGHT - 72;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(g, BG);
        renderStatusBar(g);
        drawUiCentered(g, "Health", phoneX + PHONE_WIDTH / 2, phoneY + 51, TEXT);
        beginPhoneClip(g, 68);

        Player player = Minecraft.getInstance().player;

        if (player == null) {
            drawUiCentered(g, "Health data unavailable", phoneX + PHONE_WIDTH / 2, y + 40, MUTED);
        } else {
            metric(g, y, "Health", player.getHealth(), player.getMaxHealth(), RED);
            metric(g, y + 62, "Food", player.getFoodData().getFoodLevel(), 20.0F, 0xFFFF9F0A);
            metric(g, y + 124, "Armor", player.getArmorValue(), 20.0F, BLUE);
            metric(g, y + 186, "Air", player.getAirSupply(), player.getMaxAirSupply(), 0xFF64D2FF);

            drawUiText(g, "Experience Level", x + 10, y + 245, TEXT);
            drawUiText(g, Integer.toString(player.experienceLevel), x + w - 35, y + 245, MUTED);
        }

        roundedRect(g, x, fitnessY, w, 36, 12, CARD);
        drawUiCentered(g, "Open Fitness", phoneX + PHONE_WIDTH / 2, fitnessY + 13, BLUE);

        endPhoneClip(g);
        renderHomeIndicator(g);
    }

    private void metric(GuiGraphics g, int top, String label, float value, float max, int color) {
        roundedRect(g, x, top, w, 52, 13, CARD);
        drawUiText(g, label, x + 11, top + 10, TEXT);

        String valueText = Math.round(value) + " / " + Math.round(max);
        drawUiText(g, valueText, x + w - uiWidth(valueText) - 11, top + 10, MUTED);

        int barX = x + 11;
        int barY = top + 32;
        int barW = w - 22;

        roundedRect(g, barX, barY, barW, 8, 4, 0xFFE5E5EA);

        int fill = Math.round(barW * Math.max(0.0F, Math.min(1.0F, max <= 0.0F ? 0.0F : value / max)));
        roundedRect(g, barX, barY, fill, 8, 4, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY, x, fitnessY, w, 36)) {
            minecraft.setScreen(new IPhoneFitnessScreen());
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
