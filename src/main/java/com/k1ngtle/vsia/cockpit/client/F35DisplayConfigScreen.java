package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.cockpit.network.C2SF35DetectionFilterPacket;
import com.k1ngtle.vsia.network.VsiaNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class F35DisplayConfigScreen
        extends Screen {
    private static final float VIRTUAL_WIDTH =
            860.0F;

    private static final float VIRTUAL_HEIGHT =
            343.0F;

    private static final int BLACK =
            0xF2333535;

    private static final int GRID =
            0xFF6A7370;

    private static final int GREEN =
            0xFFA0E0A0;

    private static final int CYAN =
            0xFF78A8A8;

    private static final int MAGENTA =
            0xFFA830F8;

    private static final int WHITE =
            0xFFF0F0F0;

    private static final int DIM =
            0xFF59615F;

    private static final int RED =
            0xFFE05858;

    private float uiScale =
            1.0F;

    private float uiLeft =
            0.0F;

    private float uiTop =
            0.0F;

    public F35DisplayConfigScreen() {
        super(
                Component.literal(
                        "F-35 SENSOR / DISPLAY CONTROL"
                )
        );
    }

    @Override
    protected void init() {
        F35DisplayClientConfig.ensureLoaded();
        syncDetectionFilter();
        updateGeometry();
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        updateGeometry();

        graphics.fill(
                0,
                0,
                width,
                height,
                0xB0000000
        );

        PoseStack pose =
                graphics.pose();

        pose.pushPose();
        pose.translate(
                uiLeft,
                uiTop,
                0.0F
        );
        pose.scale(
                uiScale,
                uiScale,
                1.0F
        );

        graphics.fill(
                0,
                0,
                (int) VIRTUAL_WIDTH,
                (int) VIRTUAL_HEIGHT,
                BLACK
        );

        drawFrame(
                graphics
        );

        drawTopBar(
                graphics
        );

        drawDetectionPanel(
                graphics
        );

        drawRadarPanel(
                graphics
        );

        drawDisplayPanel(
                graphics
        );

        drawStatusPanel(
                graphics
        );

        pose.popPose();

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button != 0) {
            return super.mouseClicked(
                    mouseX,
                    mouseY,
                    button
            );
        }

        double x =
                (mouseX - uiLeft)
                        / uiScale;

        double y =
                (mouseY - uiTop)
                        / uiScale;

        if (!insideDisplay(
                x,
                y
        )) {
            return false;
        }

        if (inside(x, y, 20, 82, 185, 24)) {
            F35DisplayClientConfig.toggleDetectMobs();
            syncDetectionFilter();
            return true;
        }

        if (inside(x, y, 20, 110, 185, 24)) {
            F35DisplayClientConfig.toggleDetectPlayers();
            syncDetectionFilter();
            return true;
        }

        if (inside(x, y, 20, 138, 185, 24)) {
            F35DisplayClientConfig.toggleDetectShips();
            syncDetectionFilter();
            return true;
        }

        if (inside(x, y, 20, 166, 185, 24)) {
            F35DisplayClientConfig.toggleShowMissiles();
            syncDetectionFilter();
            return true;
        }

        if (inside(x, y, 20, 202, 88, 24)) {
            F35DisplayClientConfig.setAllDetection(
                    true
            );
            F35DisplayClientConfig.setAllRadarTargets(
                    true
            );
            syncDetectionFilter();
            return true;
        }

        if (inside(x, y, 116, 202, 88, 24)) {
            F35DisplayClientConfig.setAllDetection(
                    false
            );
            F35DisplayClientConfig.setAllRadarTargets(
                    false
            );
            syncDetectionFilter();
            return true;
        }

        if (inside(x, y, 232, 82, 180, 26)) {
            F35DisplayClientConfig.toggleShowFriendlyTracks();
            return true;
        }

        if (inside(x, y, 232, 114, 180, 26)) {
            F35DisplayClientConfig.toggleShowHostileTracks();
            return true;
        }

        if (inside(x, y, 232, 146, 180, 26)) {
            F35DisplayClientConfig.toggleShowUnknownTracks();
            return true;
        }

        if (inside(x, y, 232, 186, 180, 30)) {
            F35DisplayClientConfig.cycleRadarRange();
            return true;
        }

        if (inside(x, y, 232, 222, 180, 30)) {
            F35DisplayClientConfig.toggleTrackLabels();
            return true;
        }

        if (inside(x, y, 448, 82, 180, 26)) {
            F35DisplayClientConfig.toggleTrackTrails();
            return true;
        }

        if (inside(x, y, 448, 114, 180, 26)) {
            F35DisplayClientConfig.toggleMissileTrails();
            return true;
        }

        if (inside(x, y, 448, 146, 180, 26)) {
            F35DisplayClientConfig.toggleVelocityVectors();
            return true;
        }

        if (inside(x, y, 448, 186, 180, 30)) {
            F35DisplayClientConfig.cycleBrightness();
            return true;
        }

        if (inside(x, y, 448, 222, 180, 30)) {
            F35DisplayClientConfig.cycleDisplayScale();
            return true;
        }

        if (inside(x, y, 675, 247, 156, 28)) {
            F35DisplayClientConfig.resetDefaults();
            syncDetectionFilter();
            return true;
        }

        if (inside(x, y, 675, 286, 156, 28)) {
            onClose();
            return true;
        }

        return true;
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (F35DisplayKeyMappings
                .CONFIGURE_DISPLAY
                .matches(
                        keyCode,
                        scanCode
                )) {
            onClose();
            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawFrame(
            GuiGraphics graphics
    ) {
        lineH(
                graphics,
                0,
                860,
                43,
                GRID
        );

        lineV(
                graphics,
                214,
                43,
                343,
                GRID
        );

        lineV(
                graphics,
                430,
                43,
                343,
                GRID
        );

        lineV(
                graphics,
                645,
                43,
                343,
                GRID
        );
    }

    private void drawTopBar(
            GuiGraphics graphics
    ) {
        text(
                graphics,
                "F-35 SENSOR / TARGET CONTROL",
                18,
                9,
                GREEN
        );

        text(graphics, "ICAWS", 270, 9, GREEN);
        text(graphics, "PHM", 330, 9, GREEN);
        text(graphics, "AP", 382, 9, CYAN);

        text(
                graphics,
                "TGT "
                        + F35DisplayClientConfig.targetSelectionSummary(),
                458,
                9,
                CYAN
        );

        text(
                graphics,
                "DET "
                        + F35DisplayClientConfig.detectionSummary(),
                585,
                9,
                GREEN
        );

        text(graphics, "IFF / TSD", 704, 9, WHITE);
        text(graphics, "\\ CLOSE", 785, 27, DIM);
    }

    private void drawDetectionPanel(
            GuiGraphics graphics
    ) {
        text(graphics, "DETECTION", 10, 51, MAGENTA);
        text(graphics, "LOCAL / ENTITY TARGETS", 10, 67, CYAN);

        toggleBox(graphics, 20, 82, 185, 24, "MOBS / LIVING",
                F35DisplayClientConfig.detectMobs());
        toggleBox(graphics, 20, 110, 185, 24, "PLAYERS",
                F35DisplayClientConfig.detectPlayers());
        toggleBox(graphics, 20, 138, 185, 24, "VS SHIPS",
                F35DisplayClientConfig.detectShips());
        toggleBox(graphics, 20, 166, 185, 24, "MISSILES / ROCKETS",
                F35DisplayClientConfig.showMissiles());

        actionBox(graphics, 20, 202, 88, 24, "ALL", GREEN);
        actionBox(graphics, 116, 202, 88, 24, "CLR", RED);

        text(graphics, "ENTITY FILTER", 20, 246, DIM);
        text(graphics, F35DisplayClientConfig.detectionSummary(), 20, 261, GREEN);
        text(graphics, "MISSILE " + onOff(F35DisplayClientConfig.showMissiles()),
                20, 279, F35DisplayClientConfig.showMissiles() ? RED : DIM);
        text(graphics, "MOB  PLY  SHIP  MSL", 20, 306, CYAN);
        text(graphics, "SMS", 177, 329, GREEN);
    }

    private void drawRadarPanel(
            GuiGraphics graphics
    ) {
        text(graphics, "RADAR / IFF", 226, 51, CYAN);
        text(graphics, "TSD TARGET SELECTION", 226, 67, GREEN);

        toggleBox(graphics, 232, 82, 180, 26, "FRIENDLY / ALLY",
                F35DisplayClientConfig.showFriendlyTracks());
        toggleBox(graphics, 232, 114, 180, 26, "HOSTILE",
                F35DisplayClientConfig.showHostileTracks());
        toggleBox(graphics, 232, 146, 180, 26, "UNKNOWN",
                F35DisplayClientConfig.showUnknownTracks());

        actionBox(
                graphics,
                232,
                186,
                180,
                30,
                "RANGE  " + F35DisplayClientConfig.radarRangeLabel(),
                CYAN
        );

        actionBox(
                graphics,
                232,
                222,
                180,
                30,
                "TRACK LABEL  " + onOff(F35DisplayClientConfig.trackLabels()),
                F35DisplayClientConfig.trackLabels() ? GREEN : DIM
        );

        text(graphics, "GREEN AIRCRAFT = ALLY", 232, 270, GREEN);
        text(graphics, "RED TRIANGLE = HOSTILE", 232, 286, RED);
        text(graphics, "AMBER DIAMOND = UNKNOWN", 232, 302, MAGENTA);
        text(graphics, "IDS", 391, 329, GREEN);
    }

    private void drawDisplayPanel(
            GuiGraphics graphics
    ) {
        text(graphics, "DISPLAY", 442, 51, CYAN);
        text(graphics, "TRAIL / VECTOR CONTROL", 442, 67, GREEN);

        toggleBox(graphics, 448, 82, 180, 26, "TARGET TRAILS",
                F35DisplayClientConfig.trackTrails());
        toggleBox(graphics, 448, 114, 180, 26, "MISSILE TRAILS",
                F35DisplayClientConfig.missileTrails());
        toggleBox(graphics, 448, 146, 180, 26, "VELOCITY VECTORS",
                F35DisplayClientConfig.velocityVectors());

        actionBox(
                graphics,
                448,
                186,
                180,
                30,
                "BRIGHT  "
                        + F35DisplayClientConfig.brightnessPercent()
                        + "%",
                CYAN
        );

        actionBox(
                graphics,
                448,
                222,
                180,
                30,
                "SCALE   "
                        + F35DisplayClientConfig.displayScalePercent()
                        + "%",
                GREEN
        );

        text(graphics, "10 SEC HISTORY", 448, 276, DIM);
        text(graphics, "FRIENDLY TRACKS USE GREEN", 448, 292, GREEN);
        text(graphics, "MISSILE VECTOR = RED", 448, 308, RED);
        text(graphics, "HSD", 615, 329, GREEN);
    }

    private void drawStatusPanel(
            GuiGraphics graphics
    ) {
        text(graphics, "SYSTEM", 657, 51, CYAN);
        text(graphics, "TACTICAL DISPLAY STATUS", 657, 67, GREEN);

        statusLine(graphics, 675, 94, "FRIENDLY",
                F35DisplayClientConfig.showFriendlyTracks());
        statusLine(graphics, 675, 116, "HOSTILE",
                F35DisplayClientConfig.showHostileTracks());
        statusLine(graphics, 675, 138, "UNKNOWN",
                F35DisplayClientConfig.showUnknownTracks());
        statusLine(graphics, 675, 160, "MISSILE",
                F35DisplayClientConfig.showMissiles());
        statusLine(graphics, 675, 182, "TRAILS",
                F35DisplayClientConfig.trackTrails());
        statusLine(graphics, 675, 204, "VECTORS",
                F35DisplayClientConfig.velocityVectors());

        text(graphics, "RANGE", 675, 228, DIM);
        text(graphics, F35DisplayClientConfig.radarRangeLabel(), 750, 228, WHITE);

        actionBox(graphics, 675, 247, 156, 28, "RESET", MAGENTA);
        actionBox(graphics, 675, 286, 156, 28, "DONE", GREEN);

        text(graphics, "HSI", 735, 329, GREEN);
        text(graphics, "VAR", 811, 329, GREEN);
    }

    private void drawRadarPreview(
            GuiGraphics graphics,
            int cx,
            int cy
    ) {
        rect(
                graphics,
                cx - 58,
                cy - 58,
                116,
                116,
                GRID
        );

        lineH(
                graphics,
                cx - 45,
                cx + 45,
                cy,
                GRID
        );

        lineV(
                graphics,
                cx,
                cy - 45,
                cy + 45,
                GRID
        );

        text(
                graphics,
                "+",
                cx - 2,
                cy - 4,
                GREEN
        );

        text(
                graphics,
                "S",
                cx - 38,
                cy - 30,
                CYAN
        );

        text(
                graphics,
                "P",
                cx + 30,
                cy - 12,
                GREEN
        );

        text(
                graphics,
                "M",
                cx - 8,
                cy + 34,
                MAGENTA
        );
    }

    private void toggleBox(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            String label,
            boolean enabled
    ) {
        int color =
                enabled
                        ? GREEN
                        : DIM;

        rect(
                graphics,
                x,
                y,
                width,
                height,
                color
        );

        text(
                graphics,
                label,
                x + 8,
                y + 10,
                WHITE
        );

        text(
                graphics,
                enabled
                        ? "ON"
                        : "OFF",
                x + width - 29,
                y + 10,
                enabled
                        ? GREEN
                        : RED
        );
    }

    private void actionBox(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            String label,
            int color
    ) {
        rect(
                graphics,
                x,
                y,
                width,
                height,
                color
        );

        text(
                graphics,
                label,
                x + 8,
                y + 9,
                color
        );
    }

    private void statusLine(
            GuiGraphics graphics,
            int x,
            int y,
            String label,
            boolean enabled
    ) {
        text(
                graphics,
                label,
                x,
                y,
                WHITE
        );

        text(
                graphics,
                enabled
                        ? "ON"
                        : "OFF",
                x + 104,
                y,
                enabled
                        ? GREEN
                        : RED
        );
    }

    private void syncDetectionFilter() {
        if (minecraft == null
                || minecraft.getConnection()
                == null) {
            return;
        }

        VsiaNetwork.sendToServer(
                new C2SF35DetectionFilterPacket(
                        F35DisplayClientConfig.detectMobs(),
                        F35DisplayClientConfig.detectPlayers(),
                        F35DisplayClientConfig.detectShips(),
                        F35DisplayClientConfig.showMissiles()
                )
        );
    }

    private void updateGeometry() {
        uiScale =
                Math.min(
                        (
                                width - 20.0F
                        ) / VIRTUAL_WIDTH,
                        (
                                height - 20.0F
                        ) / VIRTUAL_HEIGHT
                );

        uiScale =
                Math.max(
                        0.25F,
                        uiScale
                );

        uiLeft =
                (
                        width
                                - VIRTUAL_WIDTH
                                * uiScale
                ) / 2.0F;

        uiTop =
                (
                        height
                                - VIRTUAL_HEIGHT
                                * uiScale
                ) / 2.0F;
    }

    private boolean insideDisplay(
            double x,
            double y
    ) {
        return x >= 0.0
                && y >= 0.0
                && x <= VIRTUAL_WIDTH
                && y <= VIRTUAL_HEIGHT;
    }

    private static boolean inside(
            double x,
            double y,
            int left,
            int top,
            int width,
            int height
    ) {
        return x >= left
                && y >= top
                && x <= left + width
                && y <= top + height;
    }

    private void text(
            GuiGraphics graphics,
            String value,
            int x,
            int y,
            int color
    ) {
        graphics.drawString(
                font,
                value,
                x,
                y,
                color,
                false
        );
    }

    private static String onOff(
            boolean value
    ) {
        return value
                ? "ON"
                : "OFF";
    }

    private void rect(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        lineH(
                graphics,
                x,
                x + width,
                y,
                color
        );
        lineH(
                graphics,
                x,
                x + width,
                y + height,
                color
        );
        lineV(
                graphics,
                x,
                y,
                y + height,
                color
        );
        lineV(
                graphics,
                x + width,
                y,
                y + height,
                color
        );
    }

    private void lineH(
            GuiGraphics graphics,
            int x1,
            int x2,
            int y,
            int color
    ) {
        graphics.fill(
                Math.min(
                        x1,
                        x2
                ),
                y,
                Math.max(
                        x1,
                        x2
                ) + 1,
                y + 1,
                color
        );
    }

    private void lineV(
            GuiGraphics graphics,
            int x,
            int y1,
            int y2,
            int color
    ) {
        graphics.fill(
                x,
                Math.min(
                        y1,
                        y2
                ),
                x + 1,
                Math.max(
                        y1,
                        y2
                ) + 1,
                color
        );
    }
}
