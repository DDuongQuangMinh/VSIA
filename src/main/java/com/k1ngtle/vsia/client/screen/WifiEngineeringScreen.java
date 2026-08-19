package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.network.wifi.WifiEngineeringModePacket;
import com.k1ngtle.vsia.network.wifi.WifiEngineeringSnapshotRequestPacket;
import com.k1ngtle.vsia.network.wifi.WifiEngineeringTestLinkPacket;
import com.k1ngtle.vsia.network.wifi.WifiPacketTraceRequestPacket;
import com.k1ngtle.vsia.network.wifi.WifiPacketTraceClearPacket;
import com.k1ngtle.vsia.network.wifi.WifiEngineeringWorkflowRequestPacket;
import com.k1ngtle.vsia.network.wifi.WifiEngineeringWorkflowActionPacket;
import com.k1ngtle.vsia.network.wifi.WifiIpEngineeringRequestPacket;
import com.k1ngtle.vsia.network.wifi.WifiIpEngineeringActionPacket;
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
import com.k1ngtle.vsia.signality.engineering.wifi.ip.WifiIpAction;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.WifiIpEngineeringSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WifiEngineeringScreen
        extends Screen {
    private static final int POLL_INTERVAL_TICKS =
            10;

    private static final int HISTORY_CAPACITY =
            60;

    private static final int OUTER_MARGIN =
            12;

    private static final int PANEL_GAP =
            10;

    private static final int CONTROL_ROW_HEIGHT =
            20;

    private static final int CONTROL_ROW_GAP =
            4;

    private static final int CONTROL_ROWS =
            4;

    private static final int CONTROL_AREA_PADDING =
            8;

    private static final int CONTROL_AREA_HEIGHT =
            CONTROL_ROWS * CONTROL_ROW_HEIGHT
                    + (CONTROL_ROWS - 1) * CONTROL_ROW_GAP
                    + CONTROL_AREA_PADDING * 2;

    private static final int CONTENT_TOP_PADDING =
            8;

    private static final int CONTENT_BOTTOM_PADDING =
            8;

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

    private WifiIpEngineeringSnapshot ipSnapshot;

    private String ipStatus =
            "Use DHCP or configure an IPv4 address, then ARP / Ping / UDP / HTTP";

    private int leftScroll;

    private int leftContentHeight;

    private int leftViewportTop;

    private int leftViewportBottom;

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
        clearWidgets();

        int controlsTop =
                height
                        - OUTER_MARGIN
                        - CONTROL_AREA_HEIGHT;

        int tcpY =
                controlsTop
                        + CONTROL_AREA_PADDING;

        int ipY =
                tcpY
                        + CONTROL_ROW_HEIGHT
                        + CONTROL_ROW_GAP;

        int workflowY =
                ipY
                        + CONTROL_ROW_HEIGHT
                        + CONTROL_ROW_GAP;

        int utilityY =
                workflowY
                        + CONTROL_ROW_HEIGHT
                        + CONTROL_ROW_GAP;

        int usableWidth =
                Math.max(
                        1,
                        width
                                - OUTER_MARGIN * 2
                );

        int utilityGap =
                6;

        int utilityWidth =
                Math.max(
                        58,
                        (
                                usableWidth
                                        - utilityGap * 5
                        )
                                / 6
                );

        modeButton =
                addRenderableWidget(
                        Button.builder(
                                        modeText(),
                                        button ->
                                                toggleMode()
                                )
                                .bounds(
                                        OUTER_MARGIN,
                                        utilityY,
                                        utilityWidth,
                                        CONTROL_ROW_HEIGHT
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
                                OUTER_MARGIN
                                        + (
                                        utilityWidth
                                                + utilityGap
                                ),
                                utilityY,
                                utilityWidth,
                                CONTROL_ROW_HEIGHT
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
                                OUTER_MARGIN
                                        + 2
                                        * (
                                        utilityWidth
                                                + utilityGap
                                ),
                                utilityY,
                                utilityWidth,
                                CONTROL_ROW_HEIGHT
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
                                OUTER_MARGIN
                                        + 3
                                        * (
                                        utilityWidth
                                                + utilityGap
                                ),
                                utilityY,
                                utilityWidth,
                                CONTROL_ROW_HEIGHT
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
                                        OUTER_MARGIN
                                                + 4
                                                * (
                                                utilityWidth
                                                        + utilityGap
                                        ),
                                        utilityY,
                                        utilityWidth,
                                        CONTROL_ROW_HEIGHT
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
                                OUTER_MARGIN
                                        + 5
                                        * (
                                        utilityWidth
                                                + utilityGap
                                ),
                                utilityY,
                                utilityWidth,
                                CONTROL_ROW_HEIGHT
                        )
                        .build()
        );

        int tcpGap =
                6;

        int tcpWidth =
                Math.max(
                        80,
                        (
                                usableWidth
                                        - tcpGap * 3
                        )
                                / 4
                );

        addIpButton(
                "Auto Web",
                OUTER_MARGIN,
                tcpY,
                tcpWidth,
                WifiIpAction.RAW_HTTP_WORKFLOW
        );

        addIpButton(
                "TCP HTTP",
                OUTER_MARGIN
                        + (
                        tcpWidth
                                + tcpGap
                ),
                tcpY,
                tcpWidth,
                WifiIpAction.TCP_HTTP_GET
        );

        addIpButton(
                "TCP Close",
                OUTER_MARGIN
                        + 2
                        * (
                        tcpWidth
                                + tcpGap
                ),
                tcpY,
                tcpWidth,
                WifiIpAction.TCP_CLOSE
        );

        addIpButton(
                "Clear IP/TCP",
                OUTER_MARGIN
                        + 3
                        * (
                        tcpWidth
                                + tcpGap
                ),
                tcpY,
                tcpWidth,
                WifiIpAction.CLEAR_METRICS
        );

        int ipGap =
                6;

        int ipWidth =
                Math.max(
                        54,
                        (
                                usableWidth
                                        - ipGap * 5
                        )
                                / 6
                );

        addIpButton(
                "DHCP",
                OUTER_MARGIN,
                ipY,
                ipWidth,
                WifiIpAction.DHCP_DISCOVER
        );

        addIpButton(
                "ARP",
                OUTER_MARGIN
                        + (
                        ipWidth
                                + ipGap
                ),
                ipY,
                ipWidth,
                WifiIpAction.ARP_RESOLVE
        );

        addIpButton(
                "Ping",
                OUTER_MARGIN
                        + 2
                        * (
                        ipWidth
                                + ipGap
                ),
                ipY,
                ipWidth,
                WifiIpAction.ICMP_ECHO
        );

        addIpButton(
                "UDP Echo",
                OUTER_MARGIN
                        + 3
                        * (
                        ipWidth
                                + ipGap
                ),
                ipY,
                ipWidth,
                WifiIpAction.UDP_ECHO
        );

        addIpButton(
                "HTTP Direct",
                OUTER_MARGIN
                        + 4
                        * (
                        ipWidth
                                + ipGap
                ),
                ipY,
                ipWidth,
                WifiIpAction.HTTP_GET
        );

        addIpButton(
                "Clear IP",
                OUTER_MARGIN
                        + 5
                        * (
                        ipWidth
                                + ipGap
                ),
                ipY,
                ipWidth,
                WifiIpAction.CLEAR_METRICS
        );

        int workflowGap =
                6;

        int workflowWidth =
                Math.max(
                        52,
                        (
                                usableWidth
                                        - workflowGap * 6
                        )
                                / 7
                );

        addWorkflowButton(
                "Make AP",
                OUTER_MARGIN,
                workflowY,
                workflowWidth,
                WifiEngineeringWorkflowAction.CONFIGURE_AP
        );

        addWorkflowButton(
                "Station",
                OUTER_MARGIN
                        + (
                        workflowWidth
                                + workflowGap
                ),
                workflowY,
                workflowWidth,
                WifiEngineeringWorkflowAction.CONFIGURE_STATION
        );

        addWorkflowButton(
                "Beacon",
                OUTER_MARGIN
                        + 2
                        * (
                        workflowWidth
                                + workflowGap
                ),
                workflowY,
                workflowWidth,
                WifiEngineeringWorkflowAction.SEND_BEACON
        );

        addWorkflowButton(
                "Scan",
                OUTER_MARGIN
                        + 3
                        * (
                        workflowWidth
                                + workflowGap
                ),
                workflowY,
                workflowWidth,
                WifiEngineeringWorkflowAction.SCAN
        );

        addWorkflowButton(
                "Connect",
                OUTER_MARGIN
                        + 4
                        * (
                        workflowWidth
                                + workflowGap
                ),
                workflowY,
                workflowWidth,
                WifiEngineeringWorkflowAction.CONNECT_FIRST
        );

        addWorkflowButton(
                "Send DATA",
                OUTER_MARGIN
                        + 5
                        * (
                        workflowWidth
                                + workflowGap
                ),
                workflowY,
                workflowWidth,
                WifiEngineeringWorkflowAction.SEND_DATA
        );

        addWorkflowButton(
                "Legacy",
                OUTER_MARGIN
                        + 6
                        * (
                        workflowWidth
                                + workflowGap
                ),
                workflowY,
                workflowWidth,
                WifiEngineeringWorkflowAction.LEGACY_DIRECT
        );

        clampLeftScroll();
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

    public void acceptIpSnapshot(
            WifiIpEngineeringSnapshot value
    ) {
        ipSnapshot =
                value;

        if (value != null
                && value.status() != null
                && !value.status()
                .isBlank()) {
            ipStatus =
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

        int controlsTop =
                height
                        - OUTER_MARGIN
                        - CONTROL_AREA_HEIGHT;

        int contentBottom =
                controlsTop
                        - PANEL_GAP;

        int left =
                OUTER_MARGIN;

        int top =
                OUTER_MARGIN;

        int availableWidth =
                Math.max(
                        360,
                        width
                                - OUTER_MARGIN * 2
                );

        int leftPanelWidth =
                Math.max(
                        330,
                        Math.min(
                                440,
                                availableWidth
                                        * 47
                                        / 100
                        )
                );

        int rightLeft =
                left
                        + leftPanelWidth
                        + PANEL_GAP;

        int right =
                width
                        - OUTER_MARGIN;

        graphics.fill(
                left - 4,
                top - 4,
                left + leftPanelWidth,
                contentBottom,
                0xCC101820
        );

        graphics.fill(
                rightLeft,
                top - 4,
                right,
                contentBottom,
                0xCC0A1016
        );

        graphics.fill(
                OUTER_MARGIN - 4,
                controlsTop - 4,
                width - OUTER_MARGIN + 4,
                height - OUTER_MARGIN + 4,
                0xE00A1016
        );

        graphics.fill(
                OUTER_MARGIN - 4,
                controlsTop - 5,
                width - OUTER_MARGIN + 4,
                controlsTop - 4,
                0xFF35505E
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

        leftViewportTop =
                top
                        + 30;

        leftViewportBottom =
                contentBottom
                        - CONTENT_BOTTOM_PADDING;

        int viewportHeight =
                Math.max(
                        1,
                        leftViewportBottom
                                - leftViewportTop
                );

        int y =
                leftViewportTop
                        + CONTENT_TOP_PADDING
                        - leftScroll;

        graphics.enableScissor(
                left - 1,
                leftViewportTop,
                left + leftPanelWidth - 5,
                leftViewportBottom
        );

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
                                        56
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
                                        68
                                )
                        )
                );

        y =
                drawSection(
                        graphics,
                        left,
                        y,
                        "WIFI WORKFLOW",
                        workflowLines()
                );

        y =
                drawSection(
                        graphics,
                        left,
                        y,
                        "IP / APPLICATION",
                        ipLines()
                );

        if (ipSnapshot != null
                && ipSnapshot.router() != null
                && (
                ipSnapshot.router().enabled()
                        || !ipSnapshot.router().interfaces().isEmpty()
                        || !ipSnapshot.router().routes().isEmpty()
        )) {
            y =
                    drawSection(
                            graphics,
                            left,
                            y,
                            "ROUTER",
                            routerLines()
                    );
        }

        graphics.disableScissor();

        leftContentHeight =
                Math.max(
                        viewportHeight,
                        y
                                + leftScroll
                                - (
                                leftViewportTop
                                        + CONTENT_TOP_PADDING
                        )
                );

        clampLeftScroll();

        drawLeftScrollbar(
                graphics,
                left + leftPanelWidth - 8,
                leftViewportTop,
                leftViewportBottom,
                viewportHeight
        );

        if (right - rightLeft >= 180) {
            drawAnalyzer(
                    graphics,
                    rightLeft,
                    top,
                    right,
                    contentBottom
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

    private void drawLeftScrollbar(
            GuiGraphics graphics,
            int x,
            int top,
            int bottom,
            int viewportHeight
    ) {
        int maxScroll =
                maxLeftScroll(
                        viewportHeight
                );

        if (maxScroll <= 0) {
            return;
        }

        int trackHeight =
                Math.max(
                        1,
                        bottom - top
                );

        int thumbHeight =
                Math.max(
                        18,
                        trackHeight
                                * viewportHeight
                                / Math.max(
                                viewportHeight,
                                leftContentHeight
                        )
                );

        int travel =
                Math.max(
                        1,
                        trackHeight
                                - thumbHeight
                );

        int thumbTop =
                top
                        + (
                        int
                        ) Math.round(
                        travel
                                * (
                                leftScroll
                                        / (
                                        double
                                        ) maxScroll
                        )
                );

        graphics.fill(
                x,
                top,
                x + 2,
                bottom,
                0x553A4A54
        );

        graphics.fill(
                x,
                thumbTop,
                x + 2,
                Math.min(
                        bottom,
                        thumbTop
                                + thumbHeight
                ),
                0xFF6FD7FF
        );
    }

    private int maxLeftScroll(
            int viewportHeight
    ) {
        return Math.max(
                0,
                leftContentHeight
                        - viewportHeight
        );
    }

    private void clampLeftScroll() {
        int viewportHeight =
                Math.max(
                        1,
                        leftViewportBottom
                                - leftViewportTop
                );

        leftScroll =
                Math.max(
                        0,
                        Math.min(
                                leftScroll,
                                maxLeftScroll(
                                        viewportHeight
                                )
                        )
                );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {
        int controlsTop =
                height
                        - OUTER_MARGIN
                        - CONTROL_AREA_HEIGHT;

        int availableWidth =
                Math.max(
                        360,
                        width
                                - OUTER_MARGIN * 2
                );

        int leftPanelWidth =
                Math.max(
                        330,
                        Math.min(
                                440,
                                availableWidth
                                        * 47
                                        / 100
                        )
                );

        boolean overLeftPanel =
                mouseX >= OUTER_MARGIN - 4
                        && mouseX <= OUTER_MARGIN + leftPanelWidth
                        && mouseY >= OUTER_MARGIN - 4
                        && mouseY < controlsTop - PANEL_GAP;

        if (overLeftPanel) {
            int viewportHeight =
                    Math.max(
                            1,
                            leftViewportBottom
                                    - leftViewportTop
                    );

            int maxScroll =
                    maxLeftScroll(
                            viewportHeight
                    );

            if (maxScroll > 0) {
                leftScroll -=
                        (
                                int
                                ) Math.round(
                                delta
                                        * 22.0
                        );

                leftScroll =
                        Math.max(
                                0,
                                Math.min(
                                        leftScroll,
                                        maxScroll
                                )
                        );

                return true;
            }
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                delta
        );
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

        int latestBoxHeight =
                54;

        int latestTop =
                bottom - latestBoxHeight;

        int latestInnerLeft =
                left + 8;

        int latestInnerRight =
                right - 8;

        int packetListBottom =
                latestTop - 10;

        int maxRows =
                Math.max(
                        1,
                        (packetListBottom - rowY) / 11
                );

        int start =
                Math.max(
                        0,
                        packetTrace.size()
                                - maxRows
                );

        graphics.enableScissor(
                left + 4,
                rowY - 1,
                right - 4,
                Math.max(
                        rowY,
                        packetListBottom
                )
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
                    trimToPixelWidth(
                            WifiPacketTraceFormatter
                                    .compact(
                                            event
                                    ),
                            right - left - 20
                    ),
                    left + 8,
                    rowY,
                    color,
                    false
            );

            rowY +=
                    11;
        }

        graphics.disableScissor();

        graphics.fill(
                latestInnerLeft,
                latestTop - 2,
                latestInnerRight,
                bottom - 8,
                0x66131D24
        );

        graphics.fill(
                latestInnerLeft,
                latestTop - 2,
                latestInnerRight,
                latestTop - 1,
                0xFF35505E
        );

        if (!packetTrace.isEmpty()) {
            WifiPacketTraceEvent newest =
                    packetTrace.get(
                            packetTrace.size() - 1
                    );

            graphics.drawString(
                    font,
                    "LATEST",
                    latestInnerLeft + 4,
                    latestTop + 4,
                    0x6FD7FF,
                    false
            );

            drawWrappedLines(
                    graphics,
                    wrapToWidth(
                            WifiPacketTraceFormatter
                                    .detail(
                                            newest
                                    ),
                            latestInnerRight
                                    - latestInnerLeft
                                    - 8,
                            3
                    ),
                    latestInnerLeft + 4,
                    latestTop + 16,
                    0xD8E2E8,
                    10
            );
        } else {
            graphics.drawString(
                    font,
                    "No Wi-Fi TX/RX events captured yet.",
                    latestInnerLeft + 4,
                    latestTop + 16,
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

    private void addIpButton(
            String label,
            int x,
            int y,
            int width,
            WifiIpAction action
    ) {
        addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        label
                                ),
                                button ->
                                        sendIpAction(
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

    private void sendIpAction(
            WifiIpAction action
    ) {
        ipStatus =
                "Requesting "
                        + action
                        + "...";

        VsiaNetwork.sendToServer(
                new WifiIpEngineeringActionPacket(
                        targetPos,
                        action
                )
        );
    }

    private List<String> ipLines() {
        if (ipSnapshot == null) {
            return List.of(
                    "IPv4 n/a | peer n/a",
                    "TX/RX n/a | loss n/a",
                    "RTT n/a | jitter n/a | goodput n/a",
                    truncate(
                            ipStatus,
                            64
                    )
            );
        }

        return List.of(
                "IPv4 "
                        + ipSnapshot.localIp()
                        + " | peer "
                        + (
                        ipSnapshot.peerIp()
                                .isBlank()
                                ? "n/a"
                                : ipSnapshot.peerIp()
                ),
                "Neighbor "
                        + (
                        ipSnapshot.peerMac()
                                .isBlank()
                                ? "n/a"
                                : ipSnapshot.peerMac()
                )
                        + " | ARP entries "
                        + ipSnapshot.neighborCount(),
                "TX "
                        + ipSnapshot.txPackets()
                        + "/"
                        + ipSnapshot.txBytes()
                        + "B | RX "
                        + ipSnapshot.rxPackets()
                        + "/"
                        + ipSnapshot.rxBytes()
                        + "B | loss "
                        + ipSnapshot.lostPackets(),
                "RTT "
                        + metric(
                        ipSnapshot.lastRttMs(),
                        " ms"
                )
                        + " | avg "
                        + metric(
                        ipSnapshot.averageRttMs(),
                        " ms"
                )
                        + " | jitter "
                        + metric(
                        ipSnapshot.jitterMs(),
                        " ms"
                ),
                "Goodput "
                        + metric(
                        ipSnapshot.goodputKbps(),
                        " kbit/s"
                )
                        + " | protocol "
                        + ipSnapshot.lastProtocol(),
                "TCP "
                        + ipSnapshot.tcpState()
                        + " "
                        + ipSnapshot.tcpLocalPort()
                        + "->"
                        + ipSnapshot.tcpRemotePort()
                        + " | cwnd "
                        + ipSnapshot.tcpCongestionWindowBytes()
                        + "B | flight "
                        + ipSnapshot.tcpBytesInFlight()
                        + "B",
                "TCP SRTT "
                        + metric(
                        ipSnapshot.tcpSrttMs(),
                        " ms"
                )
                        + " | RTO "
                        + metric(
                        ipSnapshot.tcpRtoMs(),
                        " ms"
                )
                        + " | retrans "
                        + ipSnapshot.tcpRetransmissions(),
                "TCP "
                        + truncate(
                        ipSnapshot.tcpStatus(),
                        54
                ),
                truncate(
                        ipStatus,
                        64
                )
        );
    }

    private static String metric(
            double value,
            String suffix
    ) {
        if (!Double.isFinite(
                value
        )) {
            return "n/a";
        }

        return String.format(
                Locale.ROOT,
                "%.3f%s",
                value,
                suffix
        );
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

    private List<String> routerLines() {
        if (ipSnapshot == null
                || ipSnapshot.router() == null) {
            return List.of(
                    "Forwarding n/a"
            );
        }

        var router =
                ipSnapshot.router();

        List<String> lines =
                new ArrayList<>();

        lines.add(
                "Forwarding "
                        + (router.enabled() ? "ON" : "OFF")
                        + " | interfaces "
                        + router.interfaces().size()
                        + " | routes "
                        + router.routes().size()
                        + " | neighbors "
                        + router.neighborCount()
        );

        router.interfaces().stream()
                .limit(4)
                .forEach(value ->
                        lines.add(
                                "IF "
                                        + truncate(value, 64)
                        )
                );

        router.routes().stream()
                .limit(5)
                .forEach(value ->
                        lines.add(
                                "RT "
                                        + truncate(value, 64)
                        )
                );

        router.diagnostics().stream()
                .skip(
                        Math.max(
                                0,
                                router.diagnostics().size() - 3
                        )
                )
                .forEach(value ->
                        lines.add(
                                "DBG "
                                        + truncate(value, 64)
                        )
                );

        return List.copyOf(lines);
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

        VsiaNetwork.sendToServer(
                new WifiIpEngineeringRequestPacket(
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

    private void drawWrappedLines(
            GuiGraphics graphics,
            List<String> lines,
            int x,
            int y,
            int color,
            int lineHeight
    ) {
        for (String line : lines) {
            graphics.drawString(
                    font,
                    line,
                    x,
                    y,
                    color,
                    false
            );

            y +=
                    lineHeight;
        }
    }

    private List<String> wrapToWidth(
            String value,
            int maxWidth,
            int maxLines
    ) {
        if (value == null
                || value.isBlank()) {
            return List.of("");
        }

        List<String> lines =
                new ArrayList<>();

        String remaining =
                value.trim();

        while (!remaining.isEmpty()
                && lines.size() < maxLines) {
            if (lines.size() == maxLines - 1) {
                lines.add(
                        trimToPixelWidth(
                                remaining,
                                maxWidth
                        )
                );
                break;
            }

            String candidate =
                    font.plainSubstrByWidth(
                            remaining,
                            maxWidth
                    );

            if (candidate.isEmpty()) {
                break;
            }

            int cut =
                    candidate.length();

            if (cut < remaining.length()) {
                int split =
                        candidate.lastIndexOf(' ');

                if (split >= 8) {
                    candidate =
                            candidate.substring(
                                    0,
                                    split
                            );

                    cut =
                            split;
                }
            }

            lines.add(
                    candidate.trim()
            );

            remaining =
                    remaining.substring(
                                    Math.min(
                                            cut,
                                            remaining.length()
                                    )
                            )
                            .trim();
        }

        if (lines.isEmpty()) {
            lines.add(
                    trimToPixelWidth(
                            value,
                            maxWidth
                    )
            );
        }

        return lines;
    }

    private String trimToPixelWidth(
            String value,
            int maxWidth
    ) {
        if (value == null) {
            return "";
        }

        if (font.width(
                value
        ) <= maxWidth) {
            return value;
        }

        int ellipsisWidth =
                font.width(
                        "..."
                );

        String body =
                font.plainSubstrByWidth(
                        value,
                        Math.max(
                                0,
                                maxWidth - ellipsisWidth
                        )
                );

        return body + "...";
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
