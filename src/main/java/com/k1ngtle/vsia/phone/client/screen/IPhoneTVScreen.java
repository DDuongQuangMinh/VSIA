package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneCoreAppsState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneTVScreen extends IPhoneScreen {
    private static final int BG = 0xFF000000;
    private static final int CARD = 0xFF1C1C1E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int BLUE = 0xFF0A84FF;

    private static final String[] CHANNELS = {
            "VS:IA Live",
            "World Weather",
            "Network Ops"
    };

    private int x;
    private int w;
    private int previewY;
    private int controlY;
    private int volumeY;

    public IPhoneTVScreen() {
        super(Component.literal("TV"));
    }

    @Override
    protected void init() {
        super.init();
        x = phoneX + 14;
        w = PHONE_WIDTH - 28;
        previewY = phoneY + 82;
        controlY = previewY + 182;
        volumeY = controlY + 58;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(g, BG);
        renderStatusBar(g);
        drawUiCentered(g, "TV", phoneX + PHONE_WIDTH / 2, phoneY + 51, TEXT);
        beginPhoneClip(g, 68);

        roundedRect(g, x, previewY, w, 170, 14, 0xFF151515);
        Minecraft mc = Minecraft.getInstance();
        int channel = PhoneCoreAppsState.tvChannel();

        drawUiCentered(g, CHANNELS[channel], phoneX + PHONE_WIDTH / 2, previewY + 16, TEXT);

        if (PhoneCoreAppsState.tvPlaying()) {
            switch (channel) {
                case 0 -> {
                    drawUiCentered(g, "LIVE", phoneX + PHONE_WIDTH / 2, previewY + 52, 0xFFFF453A);
                    if (mc.player != null) {
                        drawUiCentered(g, mc.player.getName().getString(), phoneX + PHONE_WIDTH / 2, previewY + 79, TEXT);
                        drawUiCentered(
                                g,
                                mc.player.blockPosition().getX()
                                        + ", "
                                        + mc.player.blockPosition().getY()
                                        + ", "
                                        + mc.player.blockPosition().getZ(),
                                phoneX + PHONE_WIDTH / 2,
                                previewY + 101,
                                MUTED
                        );
                    }
                }
                case 1 -> {
                    if (mc.level != null) {
                        String weather = mc.level.isThundering() ? "Thunderstorm" : mc.level.isRaining() ? "Rain" : "Clear";
                        drawUiCentered(g, weather, phoneX + PHONE_WIDTH / 2, previewY + 75, TEXT);
                        drawUiCentered(g, "Day " + (mc.level.getDayTime() / 24000L + 1L), phoneX + PHONE_WIDTH / 2, previewY + 97, MUTED);
                    }
                }
                default -> {
                    drawUiCentered(g, "Signals nominal", phoneX + PHONE_WIDTH / 2, previewY + 75, TEXT);
                    drawUiCentered(g, "Cellular / Wi-Fi / Media services online", phoneX + PHONE_WIDTH / 2, previewY + 97, MUTED);
                }
            }
        } else {
            drawUiCentered(g, "Paused", phoneX + PHONE_WIDTH / 2, previewY + 82, MUTED);
        }

        roundedRect(g, x, controlY, w, 42, 12, CARD);
        drawUiCentered(g, "‹ CH", x + 35, controlY + 15, BLUE);
        drawUiCentered(g, PhoneCoreAppsState.tvPlaying() ? "Pause" : "Play", phoneX + PHONE_WIDTH / 2, controlY + 15, TEXT);
        drawUiCentered(g, "CH ›", x + w - 35, controlY + 15, BLUE);

        roundedRect(g, x, volumeY, w, 48, 12, CARD);
        drawUiText(g, "Volume", x + 12, volumeY + 10, TEXT);
        drawUiText(g, Integer.toString(PhoneCoreAppsState.tvVolume()), x + w - 26, volumeY + 10, MUTED);
        roundedRect(g, x + 46, volumeY + 26, w - 92, 8, 4, 0xFF2F2F31);
        int fill = Math.max(0, Math.min(w - 92, (w - 92) * PhoneCoreAppsState.tvVolume() / 100));
        roundedRect(g, x + 46, volumeY + 26, fill, 8, 4, BLUE);
        drawUiText(g, "-", x + 12, volumeY + 24, TEXT);
        drawUiText(g, "+", x + w - 14, volumeY + 24, TEXT);

        endPhoneClip(g);
        renderHomeIndicator(g);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, x, controlY, w, 42)) {
                if (mouseX < x + w / 3.0D) {
                    PhoneCoreAppsState.nextTvChannel();
                } else if (mouseX > x + w * 2.0D / 3.0D) {
                    PhoneCoreAppsState.nextTvChannel();
                } else {
                    PhoneCoreAppsState.toggleTv();
                }
                return true;
            }

            if (inside(mouseX, mouseY, x, volumeY, w, 48)) {
                if (mouseX < x + 30) {
                    PhoneCoreAppsState.adjustTvVolume(-5);
                } else if (mouseX > x + w - 30) {
                    PhoneCoreAppsState.adjustTvVolume(5);
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
