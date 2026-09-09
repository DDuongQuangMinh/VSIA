package com.k1ngtle.vsia.signality.engineering.wifi.integration.w121;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiNetworkRecord;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiSecurityState;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiStationState;
import com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119.W119Mac;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow.WifiRawIpWorkflowSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow.WifiRawIpWorkflowState;
import com.k1ngtle.vsia.signality.engineering.wifi.trace.WifiPacketDirection;
import com.k1ngtle.vsia.signality.engineering.wifi.trace.WifiPacketTraceEvent;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.router.RtAc68uRouterBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class W121IntegrationSession {
    public static final String SSID =
            "VSIA-W121";

    private static final long SCAN_TIMEOUT_TICKS =
            200L;

    private static final long CONNECT_TIMEOUT_TICKS =
            200L;

    private static final long DATA_TIMEOUT_TICKS =
            120L;

    private static final long RAW_WORKFLOW_TIMEOUT_TICKS =
            1_200L;

    private static final Pattern DS_TX =
            Pattern.compile(
                    "(?:^|\\s)dsTx=(\\d+)"
            );

    private static final Pattern DS_RX =
            Pattern.compile(
                    "(?:^|\\s)dsRx=(\\d+)"
            );

    private final ServerLevel level;

    private final BlockPos stationPos;
    private final BlockPos accessPointPos;
    private final BlockPos switchPos;
    private final BlockPos serverPos;

    private W121IntegrationStage stage =
            W121IntegrationStage.SETUP;

    private W121IntegrationFailure failure =
            W121IntegrationFailure.NONE;

    private String detail =
            "Waiting for first server tick";

    private long startTick =
            -1L;

    private long stageStartTick =
            -1L;

    private boolean dataQueued;
    private boolean rawWorkflowStarted;

    public W121IntegrationSession(
            ServerLevel level,
            BlockPos stationPos,
            BlockPos accessPointPos,
            BlockPos switchPos,
            BlockPos serverPos
    ) {
        if (level == null) {
            throw new IllegalArgumentException(
                    "level"
            );
        }

        this.level =
                level;

        this.stationPos =
                stationPos.immutable();

        this.accessPointPos =
                accessPointPos.immutable();

        this.switchPos =
                switchPos.immutable();

        this.serverPos =
                serverPos.immutable();
    }

    public void tick() {
        if (finished()) {
            return;
        }

        try {
            if (startTick < 0L) {
                startTick =
                        level.getGameTime();

                stageStartTick =
                        startTick;
            }

            NetworkDeviceBlockEntity station =
                    station();

            NetworkDeviceBlockEntity accessPoint =
                    accessPoint();

            NetworkSwitchBlockEntity networkSwitch =
                    networkSwitch();

            ServerRackBlockEntity server =
                    server();

            if (station == null
                    || accessPoint == null
                    || networkSwitch == null
                    || server == null) {
                fail(
                        W121IntegrationFailure.INVALID_TOPOLOGY,
                        topologyDetail()
                );
                return;
            }

            switch (stage) {
                case SETUP ->
                        setup(
                                station,
                                accessPoint,
                                networkSwitch,
                                server
                        );

                case SCANNING ->
                        scanning(
                                station,
                                accessPoint
                        );

                case CONNECTING ->
                        connecting(
                                station,
                                accessPoint
                        );

                case ASSOCIATED ->
                        associated(
                                station
                        );

                case DATA_ACK ->
                        dataAck(
                                station
                        );

                case DHCP,
                     DNS_ARP,
                     DNS_QUERY,
                     TARGET_ARP,
                     TCP_HTTP ->
                        rawWorkflow(
                                station,
                                accessPoint,
                                networkSwitch,
                                server
                        );

                case COMPLETE,
                     FAILED -> {
                }
            }
        } catch (Exception exception) {
            fail(
                    W121IntegrationFailure.INTERNAL_ERROR,
                    exception.getClass()
                            .getSimpleName()
                            + ": "
                            + safe(
                            exception.getMessage()
                    )
            );
        }
    }

    public boolean finished() {
        return stage
                == W121IntegrationStage.COMPLETE
                || stage
                == W121IntegrationStage.FAILED;
    }

    public boolean passed() {
        return stage
                == W121IntegrationStage.COMPLETE
                && failure
                == W121IntegrationFailure.NONE;
    }

    public W121IntegrationSnapshot snapshot() {
        NetworkDeviceBlockEntity station =
                station();

        NetworkDeviceBlockEntity accessPoint =
                accessPoint();

        String bridgeStatus =
                accessPoint == null
                        ? ""
                        : safe(
                        accessPoint.w119BridgeStatus()
                );

        List<WifiPacketTraceEvent> stationTrace =
                station == null
                        ? List.of()
                        : station.wifiPacketTraceSnapshot();

        List<WifiPacketTraceEvent> apTrace =
                accessPoint == null
                        ? List.of()
                        : accessPoint.wifiPacketTraceSnapshot();

        long retries =
                stationTrace.stream()
                        .filter(
                                WifiPacketTraceEvent::retry
                        )
                        .count()
                        + apTrace.stream()
                        .filter(
                                WifiPacketTraceEvent::retry
                        )
                        .count();

        long ackRx =
                stationTrace.stream()
                        .filter(
                                event ->
                                        event.direction()
                                                == WifiPacketDirection.RX
                                                && "CONTROL"
                                                .equalsIgnoreCase(
                                                        event.frameType()
                                                )
                                                && event.subtype()
                                                == 13
                        )
                        .count();

        WifiRawIpWorkflowSnapshot workflow =
                station == null
                        ? null
                        : station.wifiRawIpWorkflowSnapshot();

        long now =
                level.getGameTime();

        return new W121IntegrationSnapshot(
                stage,
                failure,
                finished(),
                passed(),
                detail,
                startTick < 0L
                        ? 0L
                        : Math.max(
                        0L,
                        now - startTick
                ),
                station == null
                        ? "MISSING"
                        : station.wifiStationState()
                        .name(),
                station == null
                        ? "MISSING"
                        : station.wifiSecurityState()
                        .name(),
                station == null
                        ? ""
                        : safe(
                        station.wifiIpAddress()
                ),
                station == null
                        ? ""
                        : safe(
                        station.wifiSelectedBssid()
                ),
                workflow == null
                        ? "N/A"
                        : workflow.state()
                        .name(),
                workflow == null
                        ? ""
                        : safe(
                        workflow.detail()
                ),
                bridgeStatus,
                stationTrace.size(),
                apTrace.size(),
                retries,
                ackRx,
                metric(
                        bridgeStatus,
                        DS_TX
                ),
                metric(
                        bridgeStatus,
                        DS_RX
                )
        );
    }

    private void setup(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity accessPoint,
            NetworkSwitchBlockEntity networkSwitch,
            ServerRackBlockEntity server
    ) {
        String topologyFailure =
                validatePhysicalTopology(
                        accessPoint,
                        networkSwitch,
                        server
                );

        if (!topologyFailure.isBlank()) {
            fail(
                    W121IntegrationFailure.INVALID_TOPOLOGY,
                    topologyFailure
            );
            return;
        }

        station.clearWifiPacketTrace();
        accessPoint.clearWifiPacketTrace();

        station.clearWifiIpMetrics();
        station.clearWifiTcpLive();

        station.clearWifiContentionMetrics();
        accessPoint.clearWifiContentionMetrics();

        boolean apConfigured =
                accessPoint.configureWifiAccessPoint(
                        SSID,
                        ""
                );

        if (!apConfigured) {
            fail(
                    W121IntegrationFailure.AP_CONFIGURATION_FAILED,
                    "AP target rejected Wi-Fi ACCESS_POINT configuration"
            );
            return;
        }

        boolean stationConfigured =
                station.configureWifiStation(
                        ""
                );

        if (!stationConfigured) {
            fail(
                    W121IntegrationFailure.STATION_CONFIGURATION_FAILED,
                    "Station target rejected Wi-Fi STATION configuration"
            );
            return;
        }

        station.setWifiBackgroundRoamingEnabled(
                false
        );

        accessPoint.sendWifiBeacon();

        boolean scanStarted =
                station.scanWifi();

        if (!scanStarted) {
            fail(
                    W121IntegrationFailure.SCAN_START_FAILED,
                    "Station could not start the live Wi-Fi scan"
            );
            return;
        }

        transition(
                W121IntegrationStage.SCANNING,
                "AP configured as "
                        + SSID
                        + "; station scanning for BSSID "
                        + accessPoint.wifiMacAddress()
        );
    }

    private void scanning(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity accessPoint
    ) {
        long elapsed =
                elapsedStageTicks();

        if (elapsed % 20L == 0L) {
            accessPoint.sendWifiBeacon();
        }

        WifiNetworkRecord discovered =
                station.discoveredWifiNetworks()
                        .stream()
                        .filter(
                                candidate ->
                                        candidate != null
                                                && SSID.equals(
                                                candidate.ssid()
                                        )
                                                && W119Mac.equals(
                                                candidate.bssid(),
                                                accessPoint.wifiMacAddress()
                                        )
                        )
                        .findFirst()
                        .orElse(
                                null
                        );

        if (discovered != null) {
            boolean started =
                    station.connectWifiBssid(
                            discovered.ssid(),
                            discovered.bssid()
                    );

            if (!started) {
                fail(
                        W121IntegrationFailure.CONNECT_START_FAILED,
                        "Exact-BSSID connect was rejected for "
                                + discovered.bssid()
                );
                return;
            }

            transition(
                    W121IntegrationStage.CONNECTING,
                    "Authentication/association started with "
                            + discovered.bssid()
            );
            return;
        }

        if (elapsed > SCAN_TIMEOUT_TICKS) {
            fail(
                    W121IntegrationFailure.SCAN_TIMEOUT,
                    "Station did not discover "
                            + SSID
                            + " / "
                            + accessPoint.wifiMacAddress()
                            + " within "
                            + SCAN_TIMEOUT_TICKS
                            + " ticks"
            );
        }
    }

    private void connecting(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity accessPoint
    ) {
        if (station.wifiSecurityState()
                == WifiSecurityState.FAILED) {
            fail(
                    W121IntegrationFailure.SECURITY_FAILED,
                    station.wifiSecurityDiagnostic()
            );
            return;
        }

        boolean associated =
                station.wifiStationState()
                        == WifiStationState.ASSOCIATED
                        && accessPoint
                        .wifiAssociatedStations()
                        .stream()
                        .anyMatch(
                                value ->
                                        W119Mac.equals(
                                                value,
                                                station.wifiMacAddress()
                                        )
                        );

        if (associated) {
            transition(
                    W121IntegrationStage.ASSOCIATED,
                    "Station associated with "
                            + station.wifiSelectedBssid()
                            + " security="
                            + station.wifiSecurityState()
            );
            return;
        }

        if (elapsedStageTicks()
                > CONNECT_TIMEOUT_TICKS) {
            fail(
                    W121IntegrationFailure.ASSOCIATION_TIMEOUT,
                    "Station state="
                            + station.wifiStationState()
                            + " security="
                            + station.wifiSecurityState()
                            + " diagnostic="
                            + station.wifiSecurityDiagnostic()
            );
        }
    }

    private void associated(
            NetworkDeviceBlockEntity station
    ) {
        if (!dataQueued) {
            dataQueued =
                    station.sendWifiEngineeringAssociatedData(
                            512
                    );

            if (!dataQueued) {
                fail(
                        W121IntegrationFailure.DATA_START_FAILED,
                        "Associated DATA frame was rejected"
                );
                return;
            }
        }

        transition(
                W121IntegrationStage.DATA_ACK,
                "Associated DATA queued; waiting for 802.11 ACK"
        );
    }

    private void dataAck(
            NetworkDeviceBlockEntity station
    ) {
        long ackCount =
                station.wifiPacketTraceSnapshot()
                        .stream()
                        .filter(
                                event ->
                                        event.direction()
                                                == WifiPacketDirection.RX
                                                && "CONTROL"
                                                .equalsIgnoreCase(
                                                        event.frameType()
                                                )
                                                && event.subtype()
                                                == 13
                        )
                        .count();

        if (ackCount > 0L) {
            rawWorkflowStarted =
                    station.startWifiRawHttpWorkflow(
                            "www.vsia-net.com",
                            "/"
                    );

            if (!rawWorkflowStarted) {
                fail(
                        W121IntegrationFailure.RAW_WORKFLOW_START_FAILED,
                        "Unified DHCP->ARP->DNS->TCP->HTTP workflow could not start"
                );
                return;
            }

            transition(
                    W121IntegrationStage.DHCP,
                    "802.11 DATA/ACK passed; unified raw HTTP workflow started"
            );
            return;
        }

        if (elapsedStageTicks()
                > DATA_TIMEOUT_TICKS) {
            fail(
                    W121IntegrationFailure.DATA_ACK_TIMEOUT,
                    "No RX CONTROL subtype 13 ACK observed after associated DATA"
            );
        }
    }

    private void rawWorkflow(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity accessPoint,
            NetworkSwitchBlockEntity networkSwitch,
            ServerRackBlockEntity server
    ) {
        if (!rawWorkflowStarted) {
            fail(
                    W121IntegrationFailure.RAW_WORKFLOW_START_FAILED,
                    "RAW workflow stage entered without a started controller"
            );
            return;
        }

        WifiRawIpWorkflowSnapshot workflow =
                station.wifiRawIpWorkflowSnapshot();

        if (workflow.state()
                == WifiRawIpWorkflowState.FAILED) {
            fail(
                    W121IntegrationFailure.RAW_WORKFLOW_FAILED,
                    workflow.detail()
            );
            return;
        }

        if (workflow.state()
                == WifiRawIpWorkflowState.COMPLETE) {
            finishWorkflow(
                    station,
                    accessPoint,
                    networkSwitch,
                    server,
                    workflow
            );
            return;
        }

        W121IntegrationStage mapped =
                switch (workflow.state()) {
                    case DHCP ->
                            W121IntegrationStage.DHCP;

                    case DNS_ARP ->
                            W121IntegrationStage.DNS_ARP;

                    case DNS_QUERY ->
                            W121IntegrationStage.DNS_QUERY;

                    case TARGET_ARP ->
                            W121IntegrationStage.TARGET_ARP;

                    case TCP_HTTP ->
                            W121IntegrationStage.TCP_HTTP;

                    case IDLE,
                         COMPLETE,
                         FAILED ->
                            stage;
                };

        if (mapped != stage) {
            transition(
                    mapped,
                    workflow.detail()
            );
        } else {
            detail =
                    workflow.detail();
        }

        if (totalWorkflowTicks()
                > RAW_WORKFLOW_TIMEOUT_TICKS) {
            fail(
                    W121IntegrationFailure.RAW_WORKFLOW_TIMEOUT,
                    "RAW workflow exceeded "
                            + RAW_WORKFLOW_TIMEOUT_TICKS
                            + " ticks in "
                            + workflow.state()
                            + ": "
                            + workflow.detail()
            );
        }
    }

    private void finishWorkflow(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity accessPoint,
            NetworkSwitchBlockEntity networkSwitch,
            ServerRackBlockEntity server,
            WifiRawIpWorkflowSnapshot workflow
    ) {
        if (!workflow.detail()
                .contains(
                        "HTTP 200"
                )) {
            fail(
                    W121IntegrationFailure.HTTP_NOT_200,
                    workflow.detail()
            );
            return;
        }

        String topologyFailure =
                validatePhysicalTopology(
                        accessPoint,
                        networkSwitch,
                        server
                );

        if (!topologyFailure.isBlank()) {
            fail(
                    W121IntegrationFailure.INVALID_TOPOLOGY,
                    topologyFailure
            );
            return;
        }

        String bridgeStatus =
                accessPoint.w119BridgeStatus();

        long dsTx =
                metric(
                        bridgeStatus,
                        DS_TX
                );

        long dsRx =
                metric(
                        bridgeStatus,
                        DS_RX
                );

        if (dsTx <= 0L
                || dsRx <= 0L) {
            fail(
                    W121IntegrationFailure.DISTRIBUTION_PATH_NOT_OBSERVED,
                    "HTTP completed but AP bridge did not prove both DS directions: "
                            + bridgeStatus
            );
            return;
        }

        if (!usableIpv4(
                station.wifiIpAddress()
        )) {
            fail(
                    W121IntegrationFailure.RAW_WORKFLOW_FAILED,
                    "Workflow completed without a usable station IPv4 address"
            );
            return;
        }

        transition(
                W121IntegrationStage.COMPLETE,
                "PASS: "
                        + workflow.detail()
                        + " | STA="
                        + station.wifiIpAddress()
                        + " | dsTx="
                        + dsTx
                        + " dsRx="
                        + dsRx
        );
    }

    private String validatePhysicalTopology(
            NetworkDeviceBlockEntity accessPoint,
            NetworkSwitchBlockEntity networkSwitch,
            ServerRackBlockEntity server
    ) {
        if (!server.dhcpEnabled()) {
            return "Server Rack DHCP service is disabled";
        }

        if (!server.dnsEnabled()) {
            return "Server Rack DNS service is disabled";
        }

        if (!server.httpEnabled()) {
            return "Server Rack HTTP service is disabled";
        }

        boolean apConnected =
                networkSwitch
                        .getConnectedDevices()
                        .contains(
                                accessPointPos
                        );

        if (!apConnected
                && accessPoint
                instanceof RtAc68uRouterBlockEntity router) {
            apConnected =
                    !router.w121InterfaceForPeer(
                            switchPos
                    ).isBlank();
        }

        if (!apConnected) {
            return "AP/router is not physically bound to the supplied switch";
        }

        if (!networkSwitch
                .getConnectedDevices()
                .contains(
                        serverPos
                )) {
            return "Server Rack is not connected to the supplied switch";
        }

        return "";
    }

    private void transition(
            W121IntegrationStage next,
            String newDetail
    ) {
        stage =
                next;

        stageStartTick =
                level.getGameTime();

        detail =
                safe(
                        newDetail
                );
    }

    private void fail(
            W121IntegrationFailure reason,
            String newDetail
    ) {
        failure =
                reason == null
                        ? W121IntegrationFailure.INTERNAL_ERROR
                        : reason;

        transition(
                W121IntegrationStage.FAILED,
                safe(
                        newDetail
                )
        );
    }

    private long elapsedStageTicks() {
        return Math.max(
                0L,
                level.getGameTime()
                        - stageStartTick
        );
    }

    private long totalWorkflowTicks() {
        return startTick < 0L
                ? 0L
                : Math.max(
                0L,
                level.getGameTime()
                        - startTick
        );
    }

    private NetworkDeviceBlockEntity station() {
        BlockEntity blockEntity =
                level.getBlockEntity(
                        stationPos
                );

        return blockEntity
                instanceof NetworkDeviceBlockEntity device
                ? device
                : null;
    }

    private NetworkDeviceBlockEntity accessPoint() {
        BlockEntity blockEntity =
                level.getBlockEntity(
                        accessPointPos
                );

        return blockEntity
                instanceof NetworkDeviceBlockEntity device
                ? device
                : null;
    }

    private NetworkSwitchBlockEntity networkSwitch() {
        BlockEntity blockEntity =
                level.getBlockEntity(
                        switchPos
                );

        return blockEntity
                instanceof NetworkSwitchBlockEntity value
                ? value
                : null;
    }

    private ServerRackBlockEntity server() {
        BlockEntity blockEntity =
                level.getBlockEntity(
                        serverPos
                );

        return blockEntity
                instanceof ServerRackBlockEntity value
                ? value
                : null;
    }

    private String topologyDetail() {
        return "Expected station NetworkDevice at "
                + stationPos.toShortString()
                + ", AP/router NetworkDevice at "
                + accessPointPos.toShortString()
                + ", NetworkSwitch at "
                + switchPos.toShortString()
                + ", ServerRack at "
                + serverPos.toShortString();
    }

    private static long metric(
            String status,
            Pattern pattern
    ) {
        if (status == null
                || status.isBlank()) {
            return 0L;
        }

        Matcher matcher =
                pattern.matcher(
                        status
                );

        if (!matcher.find()) {
            return 0L;
        }

        try {
            return Long.parseLong(
                    matcher.group(
                            1
                    )
            );
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static boolean usableIpv4(
            String value
    ) {
        if (value == null
                || value.isBlank()
                || value.equals(
                        "0.0.0.0"
                )
                || value.equals(
                        "255.255.255.255"
                )) {
            return false;
        }

        String[] parts =
                value.split(
                        "\\."
                );

        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int valuePart =
                        Integer.parseInt(
                                part
                        );

                if (valuePart < 0
                        || valuePart > 255) {
                    return false;
                }
            }
        } catch (NumberFormatException ignored) {
            return false;
        }

        return true;
    }

    private static String safe(
            String value
    ) {
        return value == null
                ? ""
                : value;
    }
}
