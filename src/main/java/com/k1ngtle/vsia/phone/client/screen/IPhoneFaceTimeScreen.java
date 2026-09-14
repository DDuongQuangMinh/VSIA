package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneCoreAppsState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class IPhoneFaceTimeScreen extends IPhoneScreen {
    private static final int BG = 0xFF111113;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int GREEN = 0xFF30D158;
    private static final int RED = 0xFFFF453A;
    private static final int ROW = 42;

    private int x;
    private int w;
    private int listY;
    private int actionY;

    public IPhoneFaceTimeScreen() {
        super(Component.literal("FaceTime"));
    }

    @Override
    protected void init() {
        super.init();
        x = phoneX + 14;
        w = PHONE_WIDTH - 28;
        listY = phoneY + 84;
        actionY = phoneY + PHONE_HEIGHT - 72;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(g, BG);
        renderStatusBar(g);
        drawUiCentered(g, "FaceTime", phoneX + PHONE_WIDTH / 2, phoneY + 51, TEXT);
        beginPhoneClip(g, 68);

        if (PhoneCoreAppsState.faceTimeActive()) {
            roundedRect(g, x, listY, w, 150, 16, CARD);
            drawUiCentered(g, "Connected", phoneX + PHONE_WIDTH / 2, listY + 18, GREEN);
            drawUiCentered(g, PhoneCoreAppsState.faceTimeTarget(), phoneX + PHONE_WIDTH / 2, listY + 47, TEXT);
            drawUiCentered(g, formatDuration(PhoneCoreAppsState.faceTimeSeconds()), phoneX + PHONE_WIDTH / 2, listY + 70, MUTED);
            drawUiWrappedCentered(
                    g,
                    "FaceTime is linked to the current multiplayer player list. Video transport is simulated locally.",
                    phoneX + PHONE_WIDTH / 2,
                    listY + 95,
                    w - 28,
                    11,
                    4,
                    MUTED
            );

            roundedRect(g, x, actionY, w, 36, 12, 0xFF3A2022);
            drawUiCentered(g, "End Call", phoneX + PHONE_WIDTH / 2, actionY + 13, RED);
        } else {
            List<String> players = onlinePlayers();

            drawUiText(g, "CONTACTS", x + 4, listY - 17, MUTED);

            if (players.isEmpty()) {
                roundedRect(g, x, listY, w, 70, 14, CARD);
                drawUiCentered(g, "No other players online", phoneX + PHONE_WIDTH / 2, listY + 28, MUTED);
            } else {
                int visible = Math.min(6, players.size());
                roundedRect(g, x, listY, w, visible * ROW, 14, CARD);

                for (int i = 0; i < visible; i++) {
                    int y = listY + i * ROW;
                    roundedRect(g, x + 10, y + 8, 25, 25, 13, GREEN);
                    drawUiCentered(g, players.get(i).substring(0, 1).toUpperCase(), x + 22, y + 17, TEXT);
                    drawUiText(g, fitUi(players.get(i), w - 80), x + 44, y + 15, TEXT);
                    drawUiText(g, "Video", x + w - 40, y + 15, GREEN);

                    if (i + 1 < visible) {
                        g.fill(x + 44, y + ROW - 1, x + w - 12, y + ROW, 0xFF3A3A3C);
                    }
                }
            }
        }

        endPhoneClip(g);
        renderHomeIndicator(g);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (PhoneCoreAppsState.faceTimeActive()) {
                if (inside(mouseX, mouseY, x, actionY, w, 36)) {
                    PhoneCoreAppsState.endFaceTime();
                    return true;
                }
            } else {
                List<String> players = onlinePlayers();
                int visible = Math.min(6, players.size());

                if (inside(mouseX, mouseY, x, listY, w, visible * ROW)) {
                    int index = ((int) mouseY - listY) / ROW;

                    if (index >= 0 && index < visible) {
                        PhoneCoreAppsState.startFaceTime(players.get(index));
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private List<String> onlinePlayers() {
        Minecraft minecraft = Minecraft.getInstance();
        List<String> result = new ArrayList<>();

        if (minecraft.getConnection() == null) {
            return result;
        }

        String self = minecraft.player == null ? "" : minecraft.player.getGameProfile().getName();

        for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
            String name = info.getProfile().getName();

            if (!name.equals(self)) {
                result.add(name);
            }
        }

        return result;
    }

    private static String formatDuration(long seconds) {
        return String.format("%02d:%02d", seconds / 60L, seconds % 60L);
    }
}
