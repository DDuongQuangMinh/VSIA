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
            0xF20B0F0F;

    private static final int GRID =
            0xFF667A73;

    private static final int GREEN =
            0xFF8CEA63;

    private static final int CYAN =
            0xFF65D8C3;

    private static final int MAGENTA =
            0xFFC95BE4;

    private static final int WHITE =
            0xFFE8EFEA;

    private static final int DIM =
            0xFF6B7773;

    private static final int RED =
            0xFFFF5B66;

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

        if (inside(
                x,
                y,
                28,
                92,
                177,
                28
        )) {
            F35DisplayClientConfig.toggleDetectMobs();
            syncDetectionFilter();
            return true;
        }

        if (inside(
                x,
                y,
                28,
                128,
                177,
                28
        )) {
            F35DisplayClientConfig.toggleDetectPlayers();
            syncDetectionFilter();
            return true;
        }

        if (inside(
                x,
                y,
                28,
                164,
                177,
                28
        )) {
            F35DisplayClientConfig.toggleDetectShips();
            syncDetectionFilter();
            return true;
        }

        if (inside(
                x,
                y,
                28,
                210,
                83,
                26
        )) {
            F35DisplayClientConfig.setAllDetection(
                    true
            );
            syncDetectionFilter();
            return true;
        }

        if (inside(
                x,
                y,
                122,
                210,
                83,
                26
        )) {
            F35DisplayClientConfig.setAllDetection(
                    false
            );
            syncDetectionFilter();
            return true;
        }

        if (inside(
                x,
                y,
                242,
                92,
                160,
                34
        )) {
            F35DisplayClientConfig.cycleRadarRange();
            return true;
        }

        if (inside(
                x,
                y,
                242,
                140,
                160,
                34
        )) {
            F35DisplayClientConfig.toggleTrackLabels();
            return true;
        }

        if (inside(
                x,
                y,
                459,
                92,
                158,
                34
        )) {
            F35DisplayClientConfig.cycleBrightness();
            return true;
        }

        if (inside(
                x,
                y,
                459,
                140,
                158,
                34
        )) {
            F35DisplayClientConfig.cycleDisplayScale();
            return true;
        }

        if (inside(
                x,
                y,
                675,
                247,
                156,
                28
        )) {
            F35DisplayClientConfig.resetDefaults();
            syncDetectionFilter();
            return true;
        }

        if (inside(
                x,
                y,
                675,
                286,
                156,
                28
        )) {
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
                "F-35 SENSOR CONFIG",
                18,
                9,
                GREEN
        );

        text(
                graphics,
                "ICAWS",
                270,
                9,
                GREEN
        );

        text(
                graphics,
                "PHM",
                330,
                9,
                GREEN
        );

        text(
                graphics,
                "AP",
                382,
                9,
                CYAN
        );

        text(
                graphics,
                "DETECT "
                        + F35DisplayClientConfig.detectionSummary(),
                458,
                9,
                CYAN
        );

        text(
                graphics,
                "IFF / SENSOR",
                680,
                9,
                WHITE
        );

        text(
                graphics,
                "\\ CLOSE",
                785,
                27,
                DIM
        );
    }

    private void drawDetectionPanel(
            GuiGraphics graphics
    ) {
        text(
                graphics,
                "DETECTION",
                10,
                51,
                MAGENTA
        );

        text(
                graphics,
                "RADAR RETURN FILTER",
                10,
                67,
                CYAN
        );

        toggleBox(
                graphics,
                28,
                92,
                177,
                28,
                "MOBS / LIVING TARGETS",
                F35DisplayClientConfig.detectMobs()
        );

        toggleBox(
                graphics,
                28,
                128,
                177,
                28,
                "PLAYERS",
                F35DisplayClientConfig.detectPlayers()
        );

        toggleBox(
                graphics,
                28,
                164,
                177,
                28,
                "VS SHIPS",
                F35DisplayClientConfig.detectShips()
        );

        actionBox(
                graphics,
                28,
                210,
                83,
                26,
                "ALL",
                GREEN
        );

        actionBox(
                graphics,
                122,
                210,
                83,
                26,
                "CLR",
                RED
        );

        text(
                graphics,
                "COMBINED FILTER",
                28,
                257,
                DIM
        );

        text(
                graphics,
                F35DisplayClientConfig.detectionSummary(),
                28,
                273,
                GREEN
        );

        text(
                graphics,
                "MOB  PLY  SHIP",
                28,
                307,
                CYAN
        );

        text(
                graphics,
                "SMS",
                177,
                329,
                GREEN
        );
    }

    private void drawRadarPanel(
            GuiGraphics graphics
    ) {
        text(
                graphics,
                "RADAR",
                226,
                51,
                CYAN
        );

        text(
                graphics,
                "A-S / TWS",
                226,
                67,
                GREEN
        );

        actionBox(
                graphics,
                242,
                92,
                160,
                34,
                "RANGE  "
                        + F35DisplayClientConfig.radarRangeLabel(),
                CYAN
        );

        actionBox(
                graphics,
                242,
                140,
                160,
                34,
                "TRACK LABEL  "
                        + onOff(
                        F35DisplayClientConfig.trackLabels()
                ),
                F35DisplayClientConfig.trackLabels()
                        ? GREEN
                        : DIM
        );

        drawRadarPreview(
                graphics,
                322,
                247
        );

        text(
                graphics,
                "CONTACT TYPES",
                242,
                300,
                WHITE
        );

        text(
                graphics,
                "SHIP  PLAYER  MOB",
                242,
                316,
                GREEN
        );

        text(
                graphics,
                "IDS",
                391,
                329,
                GREEN
        );
    }

    private void drawDisplayPanel(
            GuiGraphics graphics
    ) {
        text(
                graphics,
                "DISPLAY",
                442,
                51,
                CYAN
        );

        text(
                graphics,
                "MFD CONTROL",
                442,
                67,
                GREEN
        );

        actionBox(
                graphics,
                459,
                92,
                158,
                34,
                "BRIGHT  "
                        + F35DisplayClientConfig.brightnessPercent()
                        + "%",
                CYAN
        );

        actionBox(
                graphics,
                459,
                140,
                158,
                34,
                "SCALE   "
                        + F35DisplayClientConfig.displayScalePercent()
                        + "%",
                GREEN
        );

        rect(
                graphics,
                474,
                205,
                128,
                76,
                GRID
        );

        lineH(
                graphics,
                487,
                589,
                224,
                CYAN
        );

        lineH(
                graphics,
                487,
                575,
                242,
                GREEN
        );

        lineH(
                graphics,
                487,
                561,
                260,
                MAGENTA
        );

        text(
                graphics,
                "PANORAMIC DISPLAY",
                477,
                291,
                WHITE
        );

        text(
                graphics,
                "HSD",
                615,
                329,
                GREEN
        );
    }

    private void drawStatusPanel(
            GuiGraphics graphics
    ) {
        text(
                graphics,
                "SYSTEM",
                657,
                51,
                CYAN
        );

        text(
                graphics,
                "CONFIG STATUS",
                657,
                67,
                GREEN
        );

        statusLine(
                graphics,
                675,
                98,
                "MOBS",
                F35DisplayClientConfig.detectMobs()
        );

        statusLine(
                graphics,
                675,
                120,
                "PLAYERS",
                F35DisplayClientConfig.detectPlayers()
        );

        statusLine(
                graphics,
                675,
                142,
                "SHIPS",
                F35DisplayClientConfig.detectShips()
        );

        text(
                graphics,
                "RANGE",
                675,
                177,
                DIM
        );

        text(
                graphics,
                F35DisplayClientConfig.radarRangeLabel(),
                750,
                177,
                WHITE
        );

        text(
                graphics,
                "Loaded entities only",
                675,
                205,
                DIM
        );

        text(
                graphics,
                "Loaded VS ships",
                675,
                219,
                DIM
        );

        actionBox(
                graphics,
                675,
                247,
                156,
                28,
                "RESET",
                MAGENTA
        );

        actionBox(
                graphics,
                675,
                286,
                156,
                28,
                "DONE",
                GREEN
        );

        text(
                graphics,
                "HSI",
                735,
                329,
                GREEN
        );

        text(
                graphics,
                "VAR",
                811,
                329,
                GREEN
        );
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
                        F35DisplayClientConfig.detectShips()
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
