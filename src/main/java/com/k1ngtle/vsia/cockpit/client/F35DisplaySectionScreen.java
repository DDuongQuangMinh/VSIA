package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.cockpit.network.C2SF35DetectionFilterPacket;
import com.k1ngtle.vsia.network.VsiaNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class F35DisplaySectionScreen extends Screen {
    private static final float VIRTUAL_WIDTH = 640.0F;
    private static final float VIRTUAL_HEIGHT = 360.0F;

    private static final int BLACK = 0xF2333535;
    private static final int GRID = 0xFF6A7370;
    private static final int GREEN = 0xFFA0E0A0;
    private static final int CYAN = 0xFF78A8A8;
    private static final int MAGENTA = 0xFFA830F8;
    private static final int WHITE = 0xFFF0F0F0;
    private static final int DIM = 0xFF59615F;
    private static final int RED = 0xFFE05858;

    private int section;
    private float uiScale = 1.0F;
    private float uiLeft;
    private float uiTop;

    public F35DisplaySectionScreen(int section) {
        super(Component.literal("F-35 QUICK DISPLAY CONTROL"));
        this.section = clampSection(section);
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

        graphics.fill(0, 0, width, height, 0xB0000000);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(uiLeft, uiTop, 0.0F);
        pose.scale(uiScale, uiScale, 1.0F);

        graphics.fill(0, 0, (int) VIRTUAL_WIDTH, (int) VIRTUAL_HEIGHT, BLACK);
        drawFrame(graphics);
        drawTabs(graphics);

        switch (section) {
            case 1 -> drawDetectionSection(graphics);
            case 2 -> drawTargetSection(graphics);
            case 3 -> drawDisplaySection(graphics);
            default -> drawSystemSection(graphics);
        }

        pose.popPose();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        double x = (mouseX - uiLeft) / uiScale;
        double y = (mouseY - uiTop) / uiScale;

        if (!inside(x, y, 0, 0, 640, 360)) {
            return false;
        }

        if (y >= 38 && y <= 70) {
            if (x >= 18 && x <= 164) {
                section = 1;
                return true;
            }
            if (x >= 170 && x <= 316) {
                section = 2;
                return true;
            }
            if (x >= 322 && x <= 468) {
                section = 3;
                return true;
            }
            if (x >= 474 && x <= 620) {
                section = 4;
                return true;
            }
        }

        return switch (section) {
            case 1 -> clickDetection(x, y);
            case 2 -> clickTargets(x, y);
            case 3 -> clickDisplay(x, y);
            default -> clickSystem(x, y);
        };
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        int requestedSection = sectionForKey(keyCode);

        if (requestedSection > 0) {
            section = requestedSection;
            return true;
        }

        if (F35DisplayKeyMappings.CONFIGURE_DISPLAY.matches(keyCode, scanCode)) {
            if (minecraft != null) {
                minecraft.setScreen(new F35DisplayConfigScreen());
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static int sectionForKey(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_KP_1 -> 1;
            case GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_KP_2 -> 2;
            case GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_KP_3 -> 3;
            case GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_KP_4 -> 4;
            default -> 0;
        };
    }

    private boolean clickDetection(double x, double y) {
        if (inside(x, y, 90, 96, 460, 38)) {
            F35DisplayClientConfig.toggleDetectMobs();
            syncDetectionFilter();
            return true;
        }
        if (inside(x, y, 90, 142, 460, 38)) {
            F35DisplayClientConfig.toggleDetectPlayers();
            syncDetectionFilter();
            return true;
        }
        if (inside(x, y, 90, 188, 460, 38)) {
            F35DisplayClientConfig.toggleDetectShips();
            syncDetectionFilter();
            return true;
        }
        if (inside(x, y, 90, 234, 460, 38)) {
            F35DisplayClientConfig.toggleShowMissiles();
            syncDetectionFilter();
            return true;
        }
        if (inside(x, y, 90, 294, 220, 38)) {
            F35DisplayClientConfig.setAllDetection(true);
            setMissiles(true);
            syncDetectionFilter();
            return true;
        }
        if (inside(x, y, 330, 294, 220, 38)) {
            F35DisplayClientConfig.setAllDetection(false);
            setMissiles(false);
            syncDetectionFilter();
            return true;
        }
        return true;
    }

    private boolean clickTargets(double x, double y) {
        if (inside(x, y, 90, 96, 460, 38)) {
            F35DisplayClientConfig.toggleShowFriendlyTracks();
            return true;
        }
        if (inside(x, y, 90, 142, 460, 38)) {
            F35DisplayClientConfig.toggleShowHostileTracks();
            return true;
        }
        if (inside(x, y, 90, 188, 460, 38)) {
            F35DisplayClientConfig.toggleShowUnknownTracks();
            return true;
        }
        if (inside(x, y, 90, 234, 460, 38)) {
            F35DisplayClientConfig.toggleShowMissiles();
            syncDetectionFilter();
            return true;
        }
        if (inside(x, y, 90, 294, 220, 38)) {
            F35DisplayClientConfig.setAllRadarTargets(true);
            syncDetectionFilter();
            return true;
        }
        if (inside(x, y, 330, 294, 220, 38)) {
            F35DisplayClientConfig.setAllRadarTargets(false);
            syncDetectionFilter();
            return true;
        }
        return true;
    }

    private boolean clickDisplay(double x, double y) {
        if (inside(x, y, 90, 96, 460, 38)) {
            F35DisplayClientConfig.toggleTrackTrails();
            return true;
        }
        if (inside(x, y, 90, 142, 460, 38)) {
            F35DisplayClientConfig.toggleMissileTrails();
            return true;
        }
        if (inside(x, y, 90, 188, 460, 38)) {
            F35DisplayClientConfig.toggleVelocityVectors();
            return true;
        }
        if (inside(x, y, 90, 234, 460, 38)) {
            F35DisplayClientConfig.toggleTrackLabels();
            return true;
        }
        if (inside(x, y, 90, 294, 140, 38)) {
            F35DisplayClientConfig.cycleRadarRange();
            return true;
        }
        if (inside(x, y, 250, 294, 140, 38)) {
            F35DisplayClientConfig.cycleBrightness();
            return true;
        }
        if (inside(x, y, 410, 294, 140, 38)) {
            F35DisplayClientConfig.cycleDisplayScale();
            return true;
        }
        return true;
    }

    private boolean clickSystem(double x, double y) {
        if (inside(x, y, 90, 244, 140, 42)) {
            F35DisplayClientConfig.resetDefaults();
            syncDetectionFilter();
            return true;
        }
        if (inside(x, y, 250, 244, 140, 42)) {
            if (minecraft != null) {
                minecraft.setScreen(new F35DisplayConfigScreen());
            }
            return true;
        }
        if (inside(x, y, 410, 244, 140, 42)) {
            onClose();
            return true;
        }
        return true;
    }

    private void drawFrame(GuiGraphics graphics) {
        rect(graphics, 8, 8, 624, 344, GRID);
        lineH(graphics, 8, 632, 78, GRID);
        text(graphics, "F-35 COCKPIT QUICK CONTROL", 18, 16, GREEN);
        text(graphics, "1-4 SELECT SECTION   \\ FULL OVERVIEW   ESC CLOSE", 292, 16, CYAN);
    }

    private void drawTabs(GuiGraphics graphics) {
        tab(graphics, 18, 38, 146, "1  DETECTION", section == 1);
        tab(graphics, 170, 38, 146, "2  TARGET / IFF", section == 2);
        tab(graphics, 322, 38, 146, "3  DISPLAY", section == 3);
        tab(graphics, 474, 38, 146, "4  SYSTEM", section == 4);
    }

    private void drawDetectionSection(GuiGraphics graphics) {
        text(graphics, "SECTION 1 / DETECTION SOURCES", 90, 84, MAGENTA);
        toggleRow(graphics, 90, 96, "MOBS / LIVING ENTITIES", F35DisplayClientConfig.detectMobs(), CYAN);
        toggleRow(graphics, 90, 142, "PLAYERS", F35DisplayClientConfig.detectPlayers(), CYAN);
        toggleRow(graphics, 90, 188, "VALKYRIEN SKIES SHIPS", F35DisplayClientConfig.detectShips(), CYAN);
        toggleRow(graphics, 90, 234, "MISSILES / ROCKETS / MUNITIONS", F35DisplayClientConfig.showMissiles(), RED);
        actionBox(graphics, 90, 294, 220, 38, "ALL DETECTION ON", GREEN);
        actionBox(graphics, 330, 294, 220, 38, "CLEAR DETECTION", RED);
        text(graphics, "ACTIVE: " + F35DisplayClientConfig.detectionSummary(), 90, 338, GREEN);
    }

    private void drawTargetSection(GuiGraphics graphics) {
        text(graphics, "SECTION 2 / TSD TARGET + IFF FILTER", 90, 84, MAGENTA);
        toggleRow(graphics, 90, 96, "FRIENDLY / ALLIED AIRCRAFT", F35DisplayClientConfig.showFriendlyTracks(), GREEN);
        toggleRow(graphics, 90, 142, "HOSTILE TARGETS", F35DisplayClientConfig.showHostileTracks(), RED);
        toggleRow(graphics, 90, 188, "UNKNOWN / UNCLASSIFIED", F35DisplayClientConfig.showUnknownTracks(), MAGENTA);
        toggleRow(graphics, 90, 234, "MISSILES / ROCKETS", F35DisplayClientConfig.showMissiles(), RED);
        actionBox(graphics, 90, 294, 220, 38, "SHOW ALL TARGETS", GREEN);
        actionBox(graphics, 330, 294, 220, 38, "CLEAR TARGETS", RED);
        text(graphics, "TGT: " + F35DisplayClientConfig.targetSelectionSummary(), 90, 338, GREEN);
    }

    private void drawDisplaySection(GuiGraphics graphics) {
        text(graphics, "SECTION 3 / TRACK HISTORY + DISPLAY", 90, 84, MAGENTA);
        toggleRow(graphics, 90, 96, "TARGET HISTORY TRAILS", F35DisplayClientConfig.trackTrails(), GREEN);
        toggleRow(graphics, 90, 142, "MISSILE HISTORY TRAILS", F35DisplayClientConfig.missileTrails(), RED);
        toggleRow(graphics, 90, 188, "VELOCITY / PREDICTION VECTORS", F35DisplayClientConfig.velocityVectors(), CYAN);
        toggleRow(graphics, 90, 234, "TRACK LABELS", F35DisplayClientConfig.trackLabels(), CYAN);
        actionBox(graphics, 90, 294, 140, 38, "RANGE " + F35DisplayClientConfig.radarRangeLabel(), CYAN);
        actionBox(graphics, 250, 294, 140, 38, "BRIGHT " + F35DisplayClientConfig.brightnessPercent() + "%", GREEN);
        actionBox(graphics, 410, 294, 140, 38, "SCALE " + F35DisplayClientConfig.displayScalePercent() + "%", GREEN);
    }

    private void drawSystemSection(GuiGraphics graphics) {
        text(graphics, "SECTION 4 / SYSTEM STATUS", 90, 84, MAGENTA);
        statusLine(graphics, 90, 110, "DETECTION", F35DisplayClientConfig.detectionSummary(), GREEN);
        statusLine(graphics, 90, 140, "TARGET FILTER", F35DisplayClientConfig.targetSelectionSummary(), CYAN);
        statusLine(graphics, 90, 170, "RADAR RANGE", F35DisplayClientConfig.radarRangeLabel(), WHITE);
        statusLine(graphics, 90, 200, "TRAIL / VECTOR", onOff(F35DisplayClientConfig.trackTrails()) + " / " + onOff(F35DisplayClientConfig.velocityVectors()), GREEN);
        actionBox(graphics, 90, 244, 140, 42, "RESET", MAGENTA);
        actionBox(graphics, 250, 244, 140, 42, "FULL OVERVIEW", CYAN);
        actionBox(graphics, 410, 244, 140, 42, "CLOSE", GREEN);
        text(graphics, "SEATED HOTKEYS: 1 DET   2 TGT/IFF   3 DISP   4 SYS", 90, 318, DIM);
    }

    private void tab(GuiGraphics graphics, int x, int y, int width, String label, boolean active) {
        int color = active ? GREEN : GRID;
        rect(graphics, x, y, width, 32, color);
        text(graphics, label, x + 10, y + 12, active ? GREEN : WHITE);
    }

    private void toggleRow(GuiGraphics graphics, int x, int y, String label, boolean enabled, int accent) {
        rect(graphics, x, y, 460, 38, enabled ? accent : DIM);
        text(graphics, label, x + 14, y + 15, WHITE);
        text(graphics, enabled ? "ON" : "OFF", x + 410, y + 15, enabled ? accent : RED);
    }

    private void actionBox(GuiGraphics graphics, int x, int y, int width, int height, String label, int color) {
        rect(graphics, x, y, width, height, color);
        text(graphics, label, x + 12, y + 15, color);
    }

    private void statusLine(GuiGraphics graphics, int x, int y, String label, String value, int color) {
        text(graphics, label, x, y, DIM);
        text(graphics, value, x + 160, y, color);
    }


    private static void setMissiles(boolean enabled) {
        if (F35DisplayClientConfig.showMissiles() != enabled) {
            F35DisplayClientConfig.toggleShowMissiles();
        }
    }

    private void syncDetectionFilter() {
        if (minecraft == null || minecraft.getConnection() == null) {
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
        uiScale = Math.min(
                (width - 20.0F) / VIRTUAL_WIDTH,
                (height - 20.0F) / VIRTUAL_HEIGHT
        );
        uiScale = Math.max(0.25F, uiScale);
        uiLeft = (width - VIRTUAL_WIDTH * uiScale) / 2.0F;
        uiTop = (height - VIRTUAL_HEIGHT * uiScale) / 2.0F;
    }

    private static int clampSection(int value) {
        return Math.max(1, Math.min(4, value));
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && y >= top && x <= left + width && y <= top + height;
    }

    private void text(GuiGraphics graphics, String value, int x, int y, int color) {
        graphics.drawString(font, value, x, y, color, false);
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private void rect(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        lineH(graphics, x, x + width, y, color);
        lineH(graphics, x, x + width, y + height, color);
        lineV(graphics, x, y, y + height, color);
        lineV(graphics, x + width, y, y + height, color);
    }

    private void lineH(GuiGraphics graphics, int x1, int x2, int y, int color) {
        graphics.fill(Math.min(x1, x2), y, Math.max(x1, x2) + 1, y + 1, color);
    }

    private void lineV(GuiGraphics graphics, int x, int y1, int y2, int color) {
        graphics.fill(x, Math.min(y1, y2), x + 1, Math.max(y1, y2) + 1, color);
    }
}
