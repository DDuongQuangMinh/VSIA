package com.k1ngtle.vsia.phone.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneNewsScreen extends IPhoneScreen {
    private static final int BG = 0xFFF2F2F7;
    private static final int CARD = 0xFFFFFFFF;
    private static final int TEXT = 0xFF151515;
    private static final int MUTED = 0xFF77777C;
    private static final int RED = 0xFFFF375F;

    private int x;
    private int w;
    private int y;

    public IPhoneNewsScreen() {
        super(Component.literal("News"));
    }

    @Override
    protected void init() {
        super.init();
        x = phoneX + 14;
        w = PHONE_WIDTH - 28;
        y = phoneY + 80;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(g, BG);
        renderStatusBar(g);
        drawUiCentered(g, "News", phoneX + PHONE_WIDTH / 2, phoneY + 51, TEXT);
        beginPhoneClip(g, 68);

        Minecraft mc = Minecraft.getInstance();
        String dimension = mc.level == null ? "No World" : mc.level.dimension().location().toString();
        String weather = "Clear";
        long day = 0L;

        if (mc.level != null) {
            if (mc.level.isThundering()) {
                weather = "Thunderstorm";
            } else if (mc.level.isRaining()) {
                weather = "Rain";
            }

            day = mc.level.getDayTime() / 24000L + 1L;
        }

        article(g, y, "WORLD", "Day " + day + " in " + dimension, "Live world conditions from the current Minecraft session.");
        article(g, y + 78, "WEATHER", weather + " conditions", "Weather updates directly from the active client world.");

        if (mc.player != null) {
            article(
                    g,
                    y + 156,
                    "LOCAL",
                    "Player at " + mc.player.blockPosition().getX() + ", "
                            + mc.player.blockPosition().getY() + ", "
                            + mc.player.blockPosition().getZ(),
                    "Position and world state refresh while News is open."
            );
        }

        endPhoneClip(g);
        renderHomeIndicator(g);
    }

    private void article(GuiGraphics g, int top, String section, String headline, String body) {
        roundedRect(g, x, top, w, 68, 14, CARD);
        drawUiText(g, section, x + 12, top + 9, RED);
        drawUiText(g, fitUi(headline, w - 24), x + 12, top + 25, TEXT);
        drawUiWrappedCentered(g, body, phoneX + PHONE_WIDTH / 2, top + 42, w - 24, 11, 2, MUTED);
    }
}
