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
    private int controlsY;

    public IPhoneTVScreen() {
        super(Component.literal("TV"));
    }

    @Override
    protected void init() {
        super.init();
        x = phoneX + 14;
        w = PHONE_WIDTH - 28;
        previewY = phoneY + 82;
        controlsY = phoneY + 300;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(g, BG);
        renderStatusBar(g);
        drawUiCentered(g, "TV", phoneX + PHONE_WIDTH / 2, phoneY + 51, TEXT);
        beginPhoneClip(g, 68);

        roundedRect(g, x, previewY, w, 170, 14, 0xFF151515);

        Minecraft mc = Minecraft.getInstance();
        String channel = CHANNELS[PhoneCoreAppsState.tvChannel()];

        drawUiCentered(g, channel, phoneX + PHONE_WIDTH / 2, previewY + 16, TEXT);

        if (PhoneCoreAppsState.tvPlaying()) {
            if (PhoneCoreAppsState.tvChannel() == 0 && mc.player != null) {
                drawUiCentered(
                        g,
                        "LIVE",
                        phoneX + PHONE_WIDTH / 2,
                        previewY + 53,
                        0xFFFF453A
                );

                drawUiCentered(
                        g,
                        mc.player.getName().getString(),
                        phoneX + PHONE_WIDTH / 2,
                        previewY + 79,
                        TEXT
                );

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
            } else if (PhoneCoreAppsState.tvChannel() == 1 && mc.level != null) {
                String weather = mc.level.isThundering()
                        ? "Thunderstorm"
                        : mc.level.isRaining() ? "Rain" : "Clear";

                drawUiCentered(g, weather, phoneX + PHONE_WIDTH / 2, previewY + 72, TEXT);
                drawUiCentered(g, "Day " + (mc.level.getDayTime() / 24000L + 1L), phoneX + PHONE_WIDTH / 2, previewY + 94, MUTED);
            } else {
                drawUiCentered(g, "VS:IA Network Operations", phoneX + PHONE_WIDTH / 2, previewY + 72, TEXT);
                drawUiCentered(g, "Systems nominal", phoneX + PHONE_WIDTH / 2, previewY + 94, MUTED);
            }
        } else {
            drawUiCentered(g, "Paused", phoneX + PHONE_WIDTH / 2, previewY + 82, MUTED);
        }

        roundedRect(g, x, controlsY, w, 38, 12, CARD);
        drawUiText(g, "Next Channel", x + 12, controlsY + 14, BLUE);
        drawUiText(g, PhoneCoreAppsState.tvPlaying() ? "Pause" : "Play", x + w - 45, controlsY + 14, TEXT);

        endPhoneClip(g);
        renderHomeIndicator(g);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY, x, controlsY, w, 38)) {
            if (mouseX < x + w / 2.0D) {
                PhoneCoreAppsState.nextTvChannel();
            } else {
                PhoneCoreAppsState.toggleTv();
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
