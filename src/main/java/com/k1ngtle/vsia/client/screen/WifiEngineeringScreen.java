package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.network.wifi.WifiEngineeringModePacket;
import com.k1ngtle.vsia.network.wifi.WifiEngineeringSnapshotRequestPacket;
import com.k1ngtle.vsia.network.wifi.WifiEngineeringTestLinkPacket;
import com.k1ngtle.vsia.network.wifi.WifiPacketTraceRequestPacket;
import com.k1ngtle.vsia.network.wifi.WifiPacketTraceClearPacket;
import com.k1ngtle.vsia.network.wifi.WifiEngineeringWorkflowRequestPacket;
import com.k1ngtle.vsia.network.wifi.WifiEngineeringWorkflowActionPacket;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringHistory;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSample;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTestLinkResult;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTestLinkService;
import com.k1ngtle.vsia.signality.engineering.wifi.live.WifiLivePhyMode;
import com.k1ngtle.vsia.signality.engineering.wifi.trace.WifiPacketTraceEvent;
import com.k1ngtle.vsia.signality.engineering.wifi.trace.WifiPacketTraceFormatter;
import com.k1ngtle.vsia.signality.engineering.wifi.workflow.WifiEngineeringWorkflowAction;
import com.k1ngtle.vsia.signality.engineering.wifi.workflow.WifiEngineeringWorkflowSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

public final class WifiEngineeringScreen
        extends Screen {
    private static final int POLL_INTERVAL_TICKS =
            10;

    private static final int HISTORY_CAPACITY =
            60;

    private final BlockPos targetPos;

    private final WifiEngineeringHistory history =
            new WifiEngineeringHistory(
                    HISTORY_CAPACITY
            );

    private WifiEngineeringSnapshot snapshot;

    private int pollTicker;

    private Button modeButton;

    private String testLinkStatus =
            "No test frame requested";

    private List<WifiPacketTraceEvent> packetTrace =
            List.of();

    private boolean packetView;

    private Button viewButton;

    private WifiEngineeringWorkflowSnapshot workflowSnapshot;

    private String workflowStatus =
            "Configure one endpoint as AP and another as STATION";

    public WifiEngineeringScreen(
            BlockPos targetPos,
            WifiEngineeringSnapshot snapshot
    ) {
        super(
                Component.literal(
                        "Wi-Fi Engineering Analyzer"
                )
        );

        this.targetPos =
                targetPos.immutable();

        this.snapshot =
                snapshot;

        history.add(
                snapshot
        );
    }

    @Override
    protected void init() {
        int buttonY =
                height - 30;

        int workflowY =
                height - 54;

        modeButton =
                addRenderableWidget(
                        Button.builder(
                                        modeText(),
                                        button ->
                                                toggleMode()
                                )
                                .bounds(
                                        12,
                                        buttonY,
                                        150,
                                        20
                                )
                                .build()
                );

        addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "Refresh"
                                ),
                                button ->
                                        requestSnapshot()
                        )
                        .bounds(
                                168,
                                buttonY,
                                80,
                                20
                        )
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "Clear history"
                                ),
                                button ->
                                        history.clear()
                        )
                        .bounds(
                                254,
                                buttonY,
                                100,
                                20
                        )
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "Run link test"
                                ),
                                button ->
                                        runTestLink()
                        )
                        .bounds(
                                360,
                                buttonY,
                                110,
                                20
                        )
                        .build()
        );

        viewButton =
                addRenderableWidget(
                        Button.builder(
                                        viewText(),
                                        button ->
                                                toggleAnalyzerView()
                                )
                                .bounds(
                                        476,
                                        buttonY,
                                        120,
                                        20
                                )
                                .build()
                );

        addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "Clear packets"
                                ),
                                button ->
                                        clearPacketTrace()
                        )
                        .bounds(
                                602,
                                buttonY,
                                110,
                                20
                        )
                        .build()
        );

        addWorkflowButton(
                "Make AP",
                12,
                workflowY,
                78,
                WifiEngineeringWorkflowAction.CONFIGURE_AP
        );

        addWorkflowButton(
                "Station",
                96,
                workflowY,
                78,
                WifiEngineeringWorkflowAction.CONFIGURE_STATION
        );

        addWorkflowButton(
                "Beacon",
                180,
                workflowY,
                72,
                WifiEngineeringWorkflowAction.SEND_BEACON
        );

        addWorkflowButton(
                "Scan",
                258,
                workflowY,
                64,
                WifiEngineeringWorkflowAction.SCAN
        );

        addWorkflowButton(
                "Connect",
                328,
                workflowY,
                78,
                WifiEngineeringWorkflowAction.CONNECT_FIRST
        );

        addWorkflowButton(
                "Send DATA",
                412,
                workflowY,
                90,
                WifiEngineeringWorkflowAction.SEND_DATA
        );

        addWorkflowButton(
                "Legacy",
                508,
                workflowY,
                72,
                WifiEngineeringWorkflowAction.LEGACY_DIRECT
        );
    }

    @Override
    public void tick() {
        pollTicker++;

        if (pollTicker >= POLL_INTERVAL_TICKS) {
            pollTicker =
                    0;

            requestSnapshot();
        }
    }

    public BlockPos targetPos() {
        return targetPos;
    }

    public void acceptSnapshot(
            WifiEngineeringSnapshot value
    ) {
        snapshot =
                value;

        history.add(
                value
        );

        if (modeButton != null) {
            modeButton.setMessage(
                    modeText()
            );
        }
    }

    public void acceptTestLinkResult(
            WifiEngineeringTestLinkResult result
    ) {
        if (result.success()) {
            testLinkStatus =
                    "Peer "
                            + result.peerPos()
                            .toShortString()
                            + " | "
                            + number(
                            result.distanceBlocks(),
                            2
                    )
                            + " blocks | "
                            + result.frameBytes()
                            + " B queued";
        } else {
            testLinkStatus =
                    "FAILED: "
                            + result.detail();
        }

        requestSnapshot();
    }

    public void acceptPacketTrace(
            List<WifiPacketTraceEvent> events
    ) {
        packetTrace =
                events == null
                        ? List.of()
                        : List.copyOf(
                                events
                        );
    }

    public void acceptWorkflowSnapshot(
            WifiEngineeringWorkflowSnapshot value
    ) {
        workflowSnapshot =
                value;

        if (value != null
                && value.status() != null
                && !value.status()
                .isBlank()) {
            workflowStatus =
                    value.status();
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(
                graphics
        );

        int left =
                12;

        int top =
                12;

        int panelWidth =
                Math.max(
                        330,
                        width / 2 - 18
                );

        graphics.fill(
                left - 4,
                top - 4,
                left + panelWidth,
                height - 66,
                0xCC101820
        );

        graphics.drawString(
                font,
                title,
                left,
                top,
                0xFFFFFF,
                false
        );

        graphics.drawString(
                font,
                Component.literal(
                        "Target "
                                + targetPos.toShortString()
                ),
                left,
                top + 14,
                0x9FB7C9,
                false
        );

        int y =
                top + 34;

        y =
                drawSection(
                        graphics,
                        left,
                        y,
                        "LINK",
                        List.of(
                                "Profile: "
                                        + snapshot.networkProfile(),
                                "Frequency: "
                                        + frequency(
                                        snapshot.frequencyHz()
                                ),
                                "Mode: "
                                        + snapshot.wifiMode()
                                        + " | Station: "
                                        + snapshot.stationState(),
                                "Security: "
                                        + snapshot.securityState()
                        )
                );

        y =
                drawSection(
                        graphics,
                        left,
                        y,
                        "PHY",
                        List.of(
                                "Generation: "
                                        + generation(),
                                "MCS "
                                        + snapshot.mcsIndex()
                                        + " | "
                                        + snapshot.channelWidthMhz()
                                        + " MHz | "
                                        + snapshot.spatialStreams()
                                        + " SS",
                                "GI "
                                        + number(
                                        snapshot.guardIntervalUs(),
                                        2
                                )
                                        + " us",
                                "Rate "
                                        + rate(
                                        snapshot.estimatedPhyRateBps()
                                ),
                                "Doppler ICI "
                                        + percent(
                                        snapshot.dopplerIciFraction()
                                )
                        )
                );

        y =
                drawSection(
                        graphics,
                        left,
                        y,
                        "RF / ERROR",
                        List.of(
                                "RSSI "
                                        + db(
                                        snapshot.receivedPowerDbm()
                                )
                                        + " dBm",
                                "SNR "
                                        + db(
                                        snapshot.snrDb()
                                )
                                        + " dB | SINR "
                                        + db(
                                        snapshot.correctedSinrDb()
                                )
                                        + " dB",
                                "BER "
                                        + scientific(
                                        snapshot.bitErrorRate()
                                )
                                        + " | FER "
                                        + scientific(
                                        snapshot.frameErrorRate()
                                ),
                                "Medium "
                                        + (
                                        snapshot.mediumBusy()
                                                ? "BUSY"
                                                : "IDLE"
                                )
                                        + " | "
                                        + db(
                                        snapshot.mediumEnergyDbm()
                                )
                                        + " dBm | TX "
                                        + snapshot.overlappingTransmitters()
                        )
                );

        y =
                drawSection(
                        graphics,
                        left,
                        y,
                        "DETAILED PHY",
                        List.of(
                                "Mode "
                                        + snapshot.liveMode()
                                        + " | Path "
                                        + snapshot.livePath(),
                                "Evaluated "
                                        + yesNo(
                                        snapshot.liveEvaluated()
                                )
                                        + " | Delivered "
                                        + yesNo(
                                        snapshot.liveDelivered()
                                ),
                                "Codewords "
                                        + snapshot.liveCodewords()
                                        + " | Decoder iterations "
                                        + snapshot.liveDecoderIterations(),
                                truncate(
                                        snapshot.liveDetail(),
                                        52
                                )
                        )
                );

        y =
                drawSection(
                        graphics,
                        left,
                        y,
                        "TEST LINK",
                        List.of(
                                truncate(
                                        testLinkStatus,
                                        64
                                )
                        )
                );

        drawSection(
                graphics,
                left,
                y,
                "WIFI WORKFLOW",
                workflowLines()
        );

        int chartLeft =
                left + panelWidth + 10;

        int chartRight =
                width - 12;

        if (chartRight - chartLeft >= 180) {
            int chartTop =
                    top;

            int chartBottom =
                    height - 68;

            drawAnalyzer(
                    graphics,
                    chartLeft,
                    chartTop,
                    chartRight,
                    chartBottom
            );
        }

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    private int drawSection(
            GuiGraphics graphics,
            int x,
            int y,
            String name,
            List<String> lines
    ) {
        graphics.drawString(
                font,
                name,
                x,
                y,
                0x6FD7FF,
                false
        );

        y +=
                12;

        for (String line : lines) {
            graphics.drawString(
                    font,
                    line,
                    x + 4,
                    y,
                    0xD8E2E8,
                    false
            );

            y +=
                    11;
        }

        return y + 5;
    }

    private void drawAnalyzer(
            GuiGraphics graphics,
            int left,
            int top,
            int right,
            int bottom
    ) {
        if (packetView) {
            drawPacketAnalyzer(
                    graphics,
                    left,
                    top,
                    right,
                    bottom
            );
            return;
        }

        graphics.fill(
                left,
                top,
                right,
                bottom,
                0xCC0A1016
        );

        graphics.drawString(
                font,
                "LIVE HISTORY",
                left + 8,
                top + 8,
                0x6FD7FF,
                false
        );

        List<WifiEngineeringSample> samples =
                history.samples();

        graphics.drawString(
                font,
                "samples "
                        + samples.size()
                        + "/"
                        + history.capacity(),
                left + 8,
                top + 20,
                0x8295A3,
                false
        );

        if (samples.size() < 2) {
            graphics.drawString(
                    font,
                    "Waiting for samples...",
                    left + 8,
                    top + 42,
                    0xA8B7C1,
                    false
            );

            return;
        }

        int plotLeft =
                left + 10;

        int plotRight =
                right - 10;

        int firstTop =
                top + 44;

        int plotHeight =
                Math.max(
                        70,
                        (
                                bottom
                                        - firstTop
                                        - 30
                        )
                                / 2
                );

        drawMetricPlot(
                graphics,
                samples,
                plotLeft,
                firstTop,
                plotRight,
                firstTop + plotHeight,
                "SNR / SINR dB",
                true
        );

        drawMetricPlot(
                graphics,
                samples,
                plotLeft,
                firstTop + plotHeight + 20,
                plotRight,
                Math.min(
                        bottom - 10,
                        firstTop
                                + 2 * plotHeight
                                + 20
                ),
                "FER / decoder load",
                false
        );
    }

    private void drawPacketAnalyzer(
            GuiGraphics graphics,
            int left,
            int top,
            int right,
            int bottom
    ) {
        graphics.fill(
                left,
                top,
                right,
                bottom,
                0xCC0A1016
        );

        graphics.drawString(
                font,
                "PACKET / PROTOCOL TRACE",
                left + 8,
                top + 8,
                0x6FD7FF,
                false
        );

        graphics.drawString(
                font,
                "events "
                        + packetTrace.size()
                        + "/128 | newest last",
                left + 8,
                top + 20,
                0x8295A3,
                false
        );

        int rowY =
                top + 38;

        int maxRows =
                Math.max(
                        1,
                        (bottom - rowY - 48) / 11
                );

        int start =
                Math.max(
                        0,
                        packetTrace.size()
                                - maxRows
                );

        for (int i = start;
             i < packetTrace.size();
             i++) {
            WifiPacketTraceEvent event =
                    packetTrace.get(i);

            int color =
                    switch (event.outcome()) {
                        case DELIVERED ->
                                0xFF7BE495;
                        case QUEUED ->
                                0xFF59D6FF;
                        case CAPTURE_DROP,
                                ANALYTICAL_PHY_DROP,
                                DETAILED_PHY_DROP,
                                DECODE_DROP ->
                                0xFFFF6B6B;
                    };

            graphics.drawString(
                    font,
                    truncate(
                            WifiPacketTraceFormatter
                                    .compact(
                                            event
                                    ),
                            100
                    ),
                    left + 8,
                    rowY,
                    color,
                    false
            );

            rowY +=
                    11;
        }

        if (!packetTrace.isEmpty()) {
            WifiPacketTraceEvent newest =
                    packetTrace.get(
                            packetTrace.size() - 1
                    );

            graphics.drawString(
                    font,
                    "LATEST",
                    left + 8,
                    bottom - 34,
                    0x6FD7FF,
                    false
            );

            graphics.drawString(
                    font,
                    truncate(
                            WifiPacketTraceFormatter
                                    .detail(
                                            newest
                                    ),
                            115
                    ),
                    left + 8,
                    bottom - 22,
                    0xD8E2E8,
                    false
            );
        } else {
            graphics.drawString(
                    font,
                    "No Wi-Fi TX/RX events captured yet.",
                    left + 8,
                    rowY,
                    0xA8B7C1,
                    false
            );
        }
    }

    private void drawMetricPlot(
            GuiGraphics graphics,
            List<WifiEngineeringSample> samples,
            int left,
            int top,
            int right,
            int bottom,
            String label,
            boolean linkPlot
    ) {
        graphics.drawString(
                font,
                label,
                left,
                top,
                0xA8B7C1,
                false
        );

        int plotTop =
                top + 12;

        graphics.fill(
                left,
                plotTop,
                right,
                bottom,
                0x66131D24
        );

        int count =
                samples.size();

        for (int i = 1;
             i < count;
             i++) {
            int x0 =
                    left
                            + (
                            i - 1
                    )
                            * (
                            right - left - 1
                    )
                            / Math.max(
                            1,
                            count - 1
                    );

            int x1 =
                    left
                            + i
                            * (
                            right - left - 1
                    )
                            / Math.max(
                            1,
                            count - 1
                    );

            WifiEngineeringSample a =
                    samples.get(
                            i - 1
                    );

            WifiEngineeringSample b =
                    samples.get(
                            i
                    );

            if (linkPlot) {
                line(
                        graphics,
                        x0,
                        mapDb(
                                a.snrDb(),
                                plotTop,
                                bottom
                        ),
                        x1,
                        mapDb(
                                b.snrDb(),
                                plotTop,
                                bottom
                        ),
                        0xFF59D6FF
                );

                line(
                        graphics,
                        x0,
                        mapDb(
                                a.sinrDb(),
                                plotTop,
                                bottom
                        ),
                        x1,
                        mapDb(
                                b.sinrDb(),
                                plotTop,
                                bottom
                        ),
                        0xFFFFC857
                );
            } else {
                line(
                        graphics,
                        x0,
                        mapFer(
                                a.fer(),
                                plotTop,
                                bottom
                        ),
                        x1,
                        mapFer(
                                b.fer(),
                                plotTop,
                                bottom
                        ),
                        0xFFFF6B6B
                );

                line(
                        graphics,
                        x0,
                        mapIterations(
                                a.decoderIterations(),
                                plotTop,
                                bottom
                        ),
                        x1,
                        mapIterations(
                                b.decoderIterations(),
                                plotTop,
                                bottom
                        ),
                        0xFF7BE495
                );
            }
        }
    }

    private void line(
            GuiGraphics graphics,
            int x0,
            int y0,
            int x1,
            int y1,
            int color
    ) {
        int dx =
                Math.abs(
                        x1 - x0
                );

        int dy =
                Math.abs(
                        y1 - y0
                );

        int steps =
                Math.max(
                        1,
                        Math.max(
                                dx,
                                dy
                        )
                );

        for (int i = 0;
             i <= steps;
             i++) {
            int x =
                    x0
                            + (
                            x1 - x0
                    )
                            * i
                            / steps;

            int y =
                    y0
                            + (
                            y1 - y0
                    )
                            * i
                            / steps;

            graphics.fill(
                    x,
                    y,
                    x + 1,
                    y + 1,
                    color
            );
        }
    }

    private int mapDb(
            double value,
            int top,
            int bottom
    ) {
        if (!Double.isFinite(
                value
        )) {
            return bottom;
        }

        double clamped =
                Math.max(
                        -10.0,
                        Math.min(
                                50.0,
                                value
                        )
                );

        return bottom
                - (
                int
        ) Math.round(
                (
                        clamped + 10.0
                )
                        / 60.0
                        * (
                        bottom - top
                )
        );
    }

    private int mapFer(
            double value,
            int top,
            int bottom
    ) {
        if (!Double.isFinite(
                value
        )
                || value <= 0.0) {
            return bottom;
        }

        double log =
                Math.log10(
                        Math.max(
                                1.0E-8,
                                Math.min(
                                        1.0,
                                        value
                                )
                        )
                );

        return bottom
                - (
                int
        ) Math.round(
                (
                        log + 8.0
                )
                        / 8.0
                        * (
                        bottom - top
                )
        );
    }

    private int mapIterations(
            int iterations,
            int top,
            int bottom
    ) {
        int clamped =
                Math.max(
                        0,
                        Math.min(
                                70,
                                iterations
                        )
                );

        return bottom
                - clamped
                * (
                bottom - top
        )
                / 70;
    }

    private void addWorkflowButton(
            String label,
            int x,
            int y,
            int width,
            WifiEngineeringWorkflowAction action
    ) {
        addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        label
                                ),
                                button ->
                                        sendWorkflowAction(
                                                action
                                        )
                        )
                        .bounds(
                                x,
                                y,
                                width,
                                20
                        )
                        .build()
        );
    }

    private void sendWorkflowAction(
            WifiEngineeringWorkflowAction action
    ) {
        workflowStatus =
                "Requesting "
                        + action
                        + "...";

        VsiaNetwork.sendToServer(
                new WifiEngineeringWorkflowActionPacket(
                        targetPos,
                        action
                )
        );
    }

    private List<String> workflowLines() {
        if (workflowSnapshot == null) {
            return List.of(
                    "MAC: n/a",
                    "Discovered APs: n/a | Associated: n/a",
                    truncate(
                            workflowStatus,
                            64
                    )
            );
        }

        String discovered =
                workflowSnapshot
                        .discoveredSsids()
                        .isEmpty()
                        ? "none"
                        : String.join(
                                ", ",
                                workflowSnapshot
                                        .discoveredSsids()
                                        .stream()
                                        .limit(3)
                                        .toList()
                        );

        String associated =
                workflowSnapshot
                        .associatedStations()
                        .isEmpty()
                        ? "none"
                        : String.join(
                                ", ",
                                workflowSnapshot
                                        .associatedStations()
                                        .stream()
                                        .limit(2)
                                        .toList()
                        );

        return List.of(
                "MAC "
                        + workflowSnapshot.macAddress()
                        + " | "
                        + workflowSnapshot.mode(),
                "State "
                        + workflowSnapshot.stationState()
                        + " | security "
                        + workflowSnapshot.securityState(),
                "APs "
                        + truncate(
                        discovered,
                        44
                ),
                "Associated "
                        + truncate(
                        associated,
                        30
                )
                        + " | pending DATA "
                        + workflowSnapshot
                        .pendingDataTransmissions(),
                "Security diag "
                        + truncate(
                        workflowSnapshot
                                .securityDiagnostic(),
                        48
                ),
                truncate(
                        workflowStatus,
                        64
                )
        );
    }

    private void toggleAnalyzerView() {
        packetView =
                !packetView;

        if (viewButton != null) {
            viewButton.setMessage(
                    viewText()
            );
        }

        if (packetView) {
            requestPacketTrace();
        }
    }

    private void clearPacketTrace() {
        packetTrace =
                List.of();

        VsiaNetwork.sendToServer(
                new WifiPacketTraceClearPacket(
                        targetPos
                )
        );
    }

    private void requestPacketTrace() {
        VsiaNetwork.sendToServer(
                new WifiPacketTraceRequestPacket(
                        targetPos
                )
        );
    }

    private void runTestLink() {
        testLinkStatus =
                "Requesting peer and queuing test frame...";

        VsiaNetwork.sendToServer(
                new WifiEngineeringTestLinkPacket(
                        targetPos,
                        WifiEngineeringTestLinkService
                                .DEFAULT_TEST_FRAME_BYTES
                )
        );
    }

    private void toggleMode() {
        WifiLivePhyMode next =
                snapshot.liveMode()
                        == WifiLivePhyMode.BIT_LEVEL_AUTO
                        ? WifiLivePhyMode.ANALYTICAL
                        : WifiLivePhyMode.BIT_LEVEL_AUTO;

        VsiaNetwork.sendToServer(
                new WifiEngineeringModePacket(
                        targetPos,
                        next
                )
        );
    }

    private void requestSnapshot() {
        VsiaNetwork.sendToServer(
                new WifiEngineeringSnapshotRequestPacket(
                        targetPos
                )
        );

        requestPacketTrace();

        VsiaNetwork.sendToServer(
                new WifiEngineeringWorkflowRequestPacket(
                        targetPos
                )
        );
    }

    private Component viewText() {
        return Component.literal(
                packetView
                        ? "View: PACKETS"
                        : "View: HISTORY"
        );
    }

    private Component modeText() {
        return Component.literal(
                snapshot.liveMode()
                        == WifiLivePhyMode.BIT_LEVEL_AUTO
                        ? "Mode: BIT-LEVEL"
                        : "Mode: ANALYTICAL"
        );
    }

    private String generation() {
        return snapshot.generation() == null
                ? "UNKNOWN"
                : snapshot.generation()
                .name();
    }

    private static String frequency(
            double hz
    ) {
        if (!Double.isFinite(
                hz
        )) {
            return "n/a";
        }

        return String.format(
                Locale.ROOT,
                "%.6f GHz",
                hz / 1.0E9
        );
    }

    private static String rate(
            double bps
    ) {
        if (!Double.isFinite(
                bps
        )) {
            return "n/a";
        }

        if (bps >= 1.0E9) {
            return String.format(
                    Locale.ROOT,
                    "%.3f Gbit/s",
                    bps / 1.0E9
            );
        }

        return String.format(
                Locale.ROOT,
                "%.3f Mbit/s",
                bps / 1.0E6
        );
    }

    private static String percent(
            double value
    ) {
        if (!Double.isFinite(
                value
        )) {
            return "n/a";
        }

        return String.format(
                Locale.ROOT,
                "%.3f%%",
                value * 100.0
        );
    }

    private static String db(
            double value
    ) {
        return number(
                value,
                3
        );
    }

    private static String scientific(
            double value
    ) {
        if (!Double.isFinite(
                value
        )) {
            return "n/a";
        }

        return String.format(
                Locale.ROOT,
                "%.3e",
                value
        );
    }

    private static String number(
            double value,
            int decimals
    ) {
        if (!Double.isFinite(
                value
        )) {
            return "n/a";
        }

        return String.format(
                Locale.ROOT,
                "%."
                        + decimals
                        + "f",
                value
        );
    }

    private static String yesNo(
            boolean value
    ) {
        return value
                ? "yes"
                : "no";
    }

    private static String truncate(
            String value,
            int max
    ) {
        if (value == null) {
            return "";
        }

        if (value.length() <= max) {
            return value;
        }

        return value.substring(
                0,
                Math.max(
                        0,
                        max - 3
                )
        )
                + "...";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
