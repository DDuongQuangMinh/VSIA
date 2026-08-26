package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.network.wifi.WifiMultiEngineeringDeviceActionPacket;
import com.k1ngtle.vsia.network.wifi.WifiMultiEngineeringDeviceRequestPacket;
import com.k1ngtle.vsia.network.wifi.WifiMultiEngineeringDeviceSnapshotPacket;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.workflow.WifiEngineeringWorkflowAction;
import com.k1ngtle.vsia.signality.engineering.wifi.workflow.WifiEngineeringWorkflowSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class WifiMultiEngineeringScreen extends Screen {
    private static final int COUNT = 4;
    private static final int POLL_TICKS = 10;
    private static final int MARGIN = 10;
    private static final int GAP = 8;
    private static final int TOP = 30;
    private static final int BUTTON_H = 20;
    private static final int BUTTON_GAP = 4;

    private final UUID[] ids = new UUID[COUNT];
    private final WifiEngineeringSnapshot[] snapshots =
            new WifiEngineeringSnapshot[COUNT];
    private final WifiEngineeringWorkflowSnapshot[] workflows =
            new WifiEngineeringWorkflowSnapshot[COUNT];
    private final String[] status = new String[COUNT];
    private final double[] x = new double[COUNT];
    private final double[] y = new double[COUNT];
    private final double[] z = new double[COUNT];
    private final boolean[] resolved = new boolean[COUNT];

    private int pollTicker;

    public WifiMultiEngineeringScreen(List<UUID> deviceIds) {
        super(Component.literal("W1.23.3 Multi-Device Wi-Fi Analyzer"));

        if (deviceIds == null || deviceIds.size() != COUNT) {
            throw new IllegalArgumentException(
                    "W1.23.3 requires exactly four device UUIDs"
            );
        }

        for (int i = 0; i < COUNT; i++) {
            ids[i] = deviceIds.get(i);
            status[i] = "Resolving UUID...";
        }
    }

    @Override
    protected void init() {
        clearWidgets();

        for (int i = 0; i < COUNT; i++) {
            addButtons(i);
        }

        requestAll();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (++pollTicker >= POLL_TICKS) {
            pollTicker = 0;
            requestAll();
        }
    }

    public void acceptDeviceSnapshot(
            WifiMultiEngineeringDeviceSnapshotPacket packet
    ) {
        int index = indexOf(packet.deviceId());

        if (index < 0) {
            return;
        }

        resolved[index] = packet.resolved();

        if (!packet.resolved()) {
            status[index] = packet.status();
            return;
        }

        snapshots[index] = packet.snapshot();
        workflows[index] = packet.workflow();
        x[index] = packet.worldX();
        y[index] = packet.worldY();
        z[index] = packet.worldZ();

        if (packet.status() != null && !packet.status().isBlank()) {
            status[index] = packet.status();
        } else {
            status[index] = "Live";
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);

        graphics.drawCenteredString(
                font,
                title,
                width / 2,
                10,
                0xE8F3FF
        );

        graphics.drawString(
                font,
                "Persistent UUID identity; world position is telemetry only",
                MARGIN,
                21,
                0x8FA9BD,
                false
        );

        for (int i = 0; i < COUNT; i++) {
            renderPanel(graphics, i);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPanel(GuiGraphics graphics, int index) {
        int left = panelLeft(index);
        int top = panelTop(index);
        int width = panelWidth();
        int height = panelHeight();

        graphics.fill(
                left,
                top,
                left + width,
                top + height,
                0xD00B1219
        );

        graphics.fill(
                left,
                top,
                left + width,
                top + 18,
                0xD0182732
        );

        int textX = left + 8;
        int textY = top + 5;

        graphics.drawString(
                font,
                "DEVICE " + (char) ('A' + index)
                        + " | UUID " + shortId(ids[index]),
                textX,
                textY,
                0x6FD7FF,
                false
        );

        textY += 18;

        if (!resolved[index] || snapshots[index] == null) {
            draw(graphics, textX, textY, "Target unavailable");
            textY += 12;
            draw(graphics, textX, textY, status[index]);
            return;
        }

        WifiEngineeringSnapshot snapshot = snapshots[index];
        WifiEngineeringWorkflowSnapshot workflow = workflows[index];

        draw(
                graphics,
                textX,
                textY,
                String.format(
                        Locale.ROOT,
                        "World %.1f, %.1f, %.1f",
                        x[index],
                        y[index],
                        z[index]
                )
        );
        textY += 11;

        draw(
                graphics,
                textX,
                textY,
                "Profile " + safe(snapshot.networkProfile())
                        + " | " + safe(snapshot.wifiMode())
        );
        textY += 11;

        draw(
                graphics,
                textX,
                textY,
                "Station " + safe(snapshot.stationState())
                        + " | Security " + safe(snapshot.securityState())
        );
        textY += 11;

        draw(
                graphics,
                textX,
                textY,
                String.format(
                        Locale.ROOT,
                        "RSSI %.1f dBm | SNR %.1f dB",
                        snapshot.receivedPowerDbm(),
                        snapshot.snrDb()
                )
        );
        textY += 11;

        draw(
                graphics,
                textX,
                textY,
                String.format(
                        Locale.ROOT,
                        "SINR %.1f dB | MCS %d",
                        snapshot.correctedSinrDb(),
                        snapshot.mcsIndex()
                )
        );
        textY += 11;

        if (workflow != null) {
            draw(
                    graphics,
                    textX,
                    textY,
                    "MAC " + workflow.macAddress()
            );
            textY += 11;

            draw(
                    graphics,
                    textX,
                    textY,
                    "APs " + workflow.discoveredSsids().size()
                            + " | associated "
                            + workflow.associatedStations().size()
                            + " | DATA "
                            + workflow.pendingDataTransmissions()
            );
            textY += 11;
        }

        graphics.drawString(
                font,
                truncate("Status: " + status[index], maxChars()),
                textX,
                textY + 2,
                0xB9DCA6,
                false
        );
    }

    private void addButtons(int index) {
        int panelLeft = panelLeft(index) + 8;
        int panelTop = panelTop(index);
        int innerWidth = panelWidth() - 16;
        int buttonWidth = Math.max(
                48,
                (innerWidth - BUTTON_GAP * 2) / 3
        );

        int row1 = panelTop + panelHeight()
                - 8 - BUTTON_H * 2 - BUTTON_GAP;
        int row2 = row1 + BUTTON_H + BUTTON_GAP;

        addButton(index, "Roam AP", panelLeft, row1, buttonWidth,
                WifiEngineeringWorkflowAction.CONFIGURE_ROAM_AP);
        addButton(index, "Station", panelLeft + buttonWidth + BUTTON_GAP,
                row1, buttonWidth,
                WifiEngineeringWorkflowAction.CONFIGURE_STATION);
        addButton(index, "Scan", panelLeft + (buttonWidth + BUTTON_GAP) * 2,
                row1, buttonWidth,
                WifiEngineeringWorkflowAction.SCAN);

        addButton(index, "Connect", panelLeft, row2, buttonWidth,
                WifiEngineeringWorkflowAction.CONNECT_FIRST);
        addButton(index, "Roam", panelLeft + buttonWidth + BUTTON_GAP,
                row2, buttonWidth,
                WifiEngineeringWorkflowAction.ROAM_BEST);
        addButton(index, "Burst x32",
                panelLeft + (buttonWidth + BUTTON_GAP) * 2,
                row2, buttonWidth,
                WifiEngineeringWorkflowAction.CONTENTION_BURST);
    }

    private void addButton(
            int index,
            String label,
            int x,
            int y,
            int buttonWidth,
            WifiEngineeringWorkflowAction action
    ) {
        addRenderableWidget(
                Button.builder(
                                Component.literal(label),
                                button -> sendAction(index, action)
                        )
                        .bounds(x, y, buttonWidth, BUTTON_H)
                        .build()
        );
    }

    private void sendAction(
            int index,
            WifiEngineeringWorkflowAction action
    ) {
        status[index] = "Requesting " + action + "...";

        VsiaNetwork.sendToServer(
                new WifiMultiEngineeringDeviceActionPacket(
                        ids[index],
                        action
                )
        );
    }

    private void requestAll() {
        for (UUID id : ids) {
            VsiaNetwork.sendToServer(
                    new WifiMultiEngineeringDeviceRequestPacket(id)
            );
        }
    }

    private int indexOf(UUID id) {
        if (id == null) {
            return -1;
        }

        for (int i = 0; i < COUNT; i++) {
            if (id.equals(ids[i])) {
                return i;
            }
        }

        return -1;
    }

    private int panelWidth() {
        return Math.max(1, (width - MARGIN * 2 - GAP) / 2);
    }

    private int panelHeight() {
        return Math.max(1, (height - TOP - MARGIN - GAP) / 2);
    }

    private int panelLeft(int index) {
        return MARGIN + (index % 2) * (panelWidth() + GAP);
    }

    private int panelTop(int index) {
        return TOP + (index / 2) * (panelHeight() + GAP);
    }

    private void draw(
            GuiGraphics graphics,
            int x,
            int y,
            String value
    ) {
        graphics.drawString(
                font,
                truncate(value, maxChars()),
                x,
                y,
                0xD5E2EA,
                false
        );
    }

    private int maxChars() {
        return Math.max(24, panelWidth() / 6);
    }

    private static String shortId(UUID id) {
        if (id == null) {
            return "n/a";
        }

        String value = id.toString();
        return value.substring(0, Math.min(8, value.length()));
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "n/a" : value;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }

        if (value.length() <= max) {
            return value;
        }

        if (max <= 3) {
            return value.substring(0, Math.max(0, max));
        }

        return value.substring(0, max - 3) + "...";
    }
}
