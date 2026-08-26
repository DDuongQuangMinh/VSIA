package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.network.wifi.WifiEngineeringSnapshotRequestPacket;
import com.k1ngtle.vsia.network.wifi.WifiEngineeringWorkflowActionPacket;
import com.k1ngtle.vsia.network.wifi.WifiEngineeringWorkflowRequestPacket;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.workflow.WifiEngineeringWorkflowAction;
import com.k1ngtle.vsia.signality.engineering.wifi.workflow.WifiEngineeringWorkflowSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

public final class WifiMultiEngineeringScreen extends Screen {
    private static final int DEVICE_COUNT = 4;
    private static final int POLL_INTERVAL_TICKS = 10;
    private static final int OUTER_MARGIN = 10;
    private static final int TOP_AREA = 28;
    private static final int PANEL_GAP = 8;
    private static final int PANEL_PADDING = 8;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;

    private final BlockPos[] targetPositions = new BlockPos[DEVICE_COUNT];
    private final WifiEngineeringSnapshot[] snapshots = new WifiEngineeringSnapshot[DEVICE_COUNT];
    private final WifiEngineeringWorkflowSnapshot[] workflowSnapshots = new WifiEngineeringWorkflowSnapshot[DEVICE_COUNT];
    private final String[] workflowStatus = new String[DEVICE_COUNT];

    private int pollTicker;

    public WifiMultiEngineeringScreen(
            List<BlockPos> positions,
            List<WifiEngineeringSnapshot> initialSnapshots
    ) {
        super(Component.literal("W1.23 Multi-Device Wi-Fi Analyzer"));

        if (positions == null
                || initialSnapshots == null
                || positions.size() != DEVICE_COUNT
                || initialSnapshots.size() != DEVICE_COUNT) {
            throw new IllegalArgumentException(
                    "W1.23 multi analyzer requires exactly four positions and four snapshots"
            );
        }

        for (int index = 0; index < DEVICE_COUNT; index++) {
            BlockPos position = positions.get(index);
            WifiEngineeringSnapshot snapshot = initialSnapshots.get(index);

            if (position == null || snapshot == null) {
                throw new IllegalArgumentException(
                        "W1.23 multi analyzer positions/snapshots cannot contain null"
                );
            }

            targetPositions[index] = position.immutable();
            snapshots[index] = snapshot;
            workflowStatus[index] = "Ready";
        }
    }

    @Override
    protected void init() {
        clearWidgets();

        for (int index = 0; index < DEVICE_COUNT; index++) {
            addPanelButtons(index);
        }

        requestAll();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        pollTicker++;

        if (pollTicker >= POLL_INTERVAL_TICKS) {
            pollTicker = 0;
            requestAll();
        }
    }

    public boolean accepts(BlockPos pos) {
        return indexOf(pos) >= 0;
    }

    public void acceptSnapshot(
            BlockPos pos,
            WifiEngineeringSnapshot snapshot
    ) {
        int index = indexOf(pos);

        if (index < 0 || snapshot == null) {
            return;
        }

        snapshots[index] = snapshot;
    }

    public void acceptWorkflowSnapshot(
            BlockPos pos,
            WifiEngineeringWorkflowSnapshot snapshot
    ) {
        int index = indexOf(pos);

        if (index < 0 || snapshot == null) {
            return;
        }

        workflowSnapshots[index] = snapshot;

        if (snapshot.status() != null
                && !snapshot.status().isBlank()) {
            workflowStatus[index] = snapshot.status();
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
                9,
                0xE8F3FF
        );

        graphics.drawString(
                font,
                "Four persistent targets | each panel sends actions only to its own BlockPos",
                OUTER_MARGIN,
                20,
                0x8FA9BD,
                false
        );

        for (int index = 0; index < DEVICE_COUNT; index++) {
            renderPanel(graphics, index);
        }

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    private void renderPanel(
            GuiGraphics graphics,
            int index
    ) {
        int left = panelLeft(index);
        int top = panelTop(index);
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        graphics.fill(
                left,
                top,
                right,
                bottom,
                0xD00B1219
        );

        graphics.fill(
                left,
                top,
                right,
                top + 18,
                0xD0182732
        );

        BlockPos pos = targetPositions[index];
        WifiEngineeringSnapshot snapshot = snapshots[index];
        WifiEngineeringWorkflowSnapshot workflow = workflowSnapshots[index];

        int textX = left + PANEL_PADDING;
        int y = top + 5;

        graphics.drawString(
                font,
                "DEVICE " + (char) ('A' + index)
                        + "  @ " + pos.toShortString(),
                textX,
                y,
                0x6FD7FF,
                false
        );

        y += 18;

        drawLine(
                graphics,
                textX,
                y,
                "Device " + shortDevice(snapshot)
                        + " | " + truncate(snapshot.networkProfile(), 34)
        );
        y += 11;

        drawLine(
                graphics,
                textX,
                y,
                "Wi-Fi " + safe(snapshot.wifiMode())
                        + " | station " + safe(snapshot.stationState())
        );
        y += 11;

        drawLine(
                graphics,
                textX,
                y,
                "Security " + safe(snapshot.securityState())
                        + " | MCS " + snapshot.mcsIndex()
        );
        y += 11;

        drawLine(
                graphics,
                textX,
                y,
                "Freq " + frequency(snapshot.frequencyHz())
                        + " | RSSI " + metric(snapshot.receivedPowerDbm(), " dBm")
        );
        y += 11;

        drawLine(
                graphics,
                textX,
                y,
                "SNR " + metric(snapshot.snrDb(), " dB")
                        + " | SINR " + metric(snapshot.correctedSinrDb(), " dB")
        );
        y += 11;

        drawLine(
                graphics,
                textX,
                y,
                "Medium " + (snapshot.mediumBusy() ? "BUSY" : "IDLE")
                        + " | overlap " + snapshot.overlappingTransmitters()
        );
        y += 11;

        if (workflow == null) {
            drawLine(graphics, textX, y, "MAC n/a | APs n/a");
            y += 11;
            drawLine(graphics, textX, y, "Association n/a | pending DATA n/a");
        } else {
            drawLine(
                    graphics,
                    textX,
                    y,
                    "MAC " + truncate(workflow.macAddress(), 24)
                            + " | APs " + discovered(workflow)
            );
            y += 11;

            drawLine(
                    graphics,
                    textX,
                    y,
                    "Associated " + workflow.associatedStations().size()
                            + " | pending DATA " + workflow.pendingDataTransmissions()
            );
        }

        y += 13;

        graphics.drawString(
                font,
                "Status: " + truncate(workflowStatus[index], statusCharacters()),
                textX,
                y,
                0xB9DCA6,
                false
        );
    }

    private void drawLine(
            GuiGraphics graphics,
            int x,
            int y,
            String text
    ) {
        graphics.drawString(
                font,
                truncate(text, statusCharacters()),
                x,
                y,
                0xD5E2EA,
                false
        );
    }

    private void addPanelButtons(int index) {
        int left = panelLeft(index) + PANEL_PADDING;
        int top = panelTop(index);
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int innerWidth = Math.max(1, panelWidth - PANEL_PADDING * 2);
        int buttonWidth = Math.max(
                48,
                (innerWidth - BUTTON_GAP * 2) / 3
        );

        int firstRowY = top + panelHeight
                - PANEL_PADDING
                - BUTTON_HEIGHT * 2
                - BUTTON_GAP;
        int secondRowY = firstRowY + BUTTON_HEIGHT + BUTTON_GAP;

        addActionButton(
                index,
                "Roam AP",
                left,
                firstRowY,
                buttonWidth,
                WifiEngineeringWorkflowAction.CONFIGURE_ROAM_AP
        );
        addActionButton(
                index,
                "Station",
                left + buttonWidth + BUTTON_GAP,
                firstRowY,
                buttonWidth,
                WifiEngineeringWorkflowAction.CONFIGURE_STATION
        );
        addActionButton(
                index,
                "Scan",
                left + (buttonWidth + BUTTON_GAP) * 2,
                firstRowY,
                buttonWidth,
                WifiEngineeringWorkflowAction.SCAN
        );

        addActionButton(
                index,
                "Connect",
                left,
                secondRowY,
                buttonWidth,
                WifiEngineeringWorkflowAction.CONNECT_FIRST
        );
        addActionButton(
                index,
                "Roam",
                left + buttonWidth + BUTTON_GAP,
                secondRowY,
                buttonWidth,
                WifiEngineeringWorkflowAction.ROAM_BEST
        );
        addActionButton(
                index,
                "Burst x32",
                left + (buttonWidth + BUTTON_GAP) * 2,
                secondRowY,
                buttonWidth,
                WifiEngineeringWorkflowAction.CONTENTION_BURST
        );
    }

    private void addActionButton(
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
                        .bounds(
                                x,
                                y,
                                buttonWidth,
                                BUTTON_HEIGHT
                        )
                        .build()
        );
    }

    private void sendAction(
            int index,
            WifiEngineeringWorkflowAction action
    ) {
        workflowStatus[index] = "Requesting " + action + "...";

        VsiaNetwork.sendToServer(
                new WifiEngineeringWorkflowActionPacket(
                        targetPositions[index],
                        action
                )
        );
    }

    private void requestAll() {
        for (BlockPos pos : targetPositions) {
            VsiaNetwork.sendToServer(
                    new WifiEngineeringSnapshotRequestPacket(pos)
            );

            VsiaNetwork.sendToServer(
                    new WifiEngineeringWorkflowRequestPacket(pos)
            );
        }
    }

    private int indexOf(BlockPos pos) {
        if (pos == null) {
            return -1;
        }

        for (int index = 0; index < DEVICE_COUNT; index++) {
            if (targetPositions[index].equals(pos)) {
                return index;
            }
        }

        return -1;
    }

    private int panelWidth() {
        return Math.max(
                1,
                (width - OUTER_MARGIN * 2 - PANEL_GAP) / 2
        );
    }

    private int panelHeight() {
        return Math.max(
                1,
                (height - TOP_AREA - OUTER_MARGIN - PANEL_GAP) / 2
        );
    }

    private int panelLeft(int index) {
        int column = index % 2;
        return OUTER_MARGIN
                + column * (panelWidth() + PANEL_GAP);
    }

    private int panelTop(int index) {
        int row = index / 2;
        return TOP_AREA
                + row * (panelHeight() + PANEL_GAP);
    }

    private int statusCharacters() {
        return Math.max(
                24,
                panelWidth() / 6
        );
    }

    private static String discovered(
            WifiEngineeringWorkflowSnapshot workflow
    ) {
        if (workflow.discoveredSsids().isEmpty()) {
            return "none";
        }

        return truncate(
                String.join(
                        ", ",
                        workflow.discoveredSsids()
                                .stream()
                                .limit(2)
                                .toList()
                ),
                24
        );
    }

    private static String shortDevice(
            WifiEngineeringSnapshot snapshot
    ) {
        if (snapshot.deviceId() == null) {
            return "n/a";
        }

        String value = snapshot.deviceId().toString();
        return value.substring(0, Math.min(8, value.length()));
    }

    private static String frequency(double hz) {
        if (!Double.isFinite(hz)) {
            return "n/a";
        }

        return String.format(
                Locale.ROOT,
                "%.4f GHz",
                hz / 1.0E9
        );
    }

    private static String metric(
            double value,
            String suffix
    ) {
        if (!Double.isFinite(value)) {
            return "n/a";
        }

        return String.format(
                Locale.ROOT,
                "%.1f%s",
                value,
                suffix
        );
    }

    private static String safe(String value) {
        return value == null || value.isBlank()
                ? "n/a"
                : value;
    }

    private static String truncate(
            String value,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            return "n/a";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        if (maxLength <= 3) {
            return value.substring(0, Math.max(0, maxLength));
        }

        return value.substring(0, maxLength - 3) + "...";
    }
}
