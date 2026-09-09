package com.k1ngtle.vsia.signality.engineering.wifi.integration.w1221;

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

public final class W1221RoamingSession {
    public static final String SSID =
            "VSIA-ROAM";

    public static final double ROAM_HYSTERESIS_DB =
            6.0D;

    private static final long INITIAL_SCAN_TIMEOUT_TICKS =
            240L;

    private static final long CONNECT_TIMEOUT_TICKS =
            240L;

    private static final long BASELINE_HTTP_TIMEOUT_TICKS =
            1_200L;

    private static final long ROAM_SCAN_TIMEOUT_TICKS =
            300L;

    private static final long ROAM_CONNECT_TIMEOUT_TICKS =
            240L;

    private static final long DATA_ACK_TIMEOUT_TICKS =
            160L;

    private static final long POST_ROAM_HTTP_TIMEOUT_TICKS =
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
    private final BlockPos ap1Pos;
    private final BlockPos ap2Pos;
    private final BlockPos switchPos;
    private final BlockPos serverPos;

    private W1221RoamingStage stage =
            W1221RoamingStage.SETUP;

    private W1221RoamingFailure failure =
            W1221RoamingFailure.NONE;

    private String detail =
            "Waiting for first server tick";

    private long startTick =
            -1L;

    private long stageStartTick =
            -1L;

    private String baselineIp =
            "";

    private long ap2SeenBeforeRoamScan =
            -1L;

    private long postRoamAckBaseline =
            0L;

    private boolean baselineWorkflowStarted;
    private boolean roamScanStarted;
    private boolean roamStarted;
    private boolean postRoamDataQueued;
    private boolean postRoamWorkflowStarted;

    public W1221RoamingSession(
            ServerLevel level,
            BlockPos stationPos,
            BlockPos ap1Pos,
            BlockPos ap2Pos,
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

        this.ap1Pos =
                ap1Pos.immutable();

        this.ap2Pos =
                ap2Pos.immutable();

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

            NetworkDeviceBlockEntity ap1 =
                    ap1();

            NetworkDeviceBlockEntity ap2 =
                    ap2();

            NetworkSwitchBlockEntity networkSwitch =
                    networkSwitch();

            ServerRackBlockEntity server =
                    server();

            if (station == null
                    || ap1 == null
                    || ap2 == null
                    || networkSwitch == null
                    || server == null) {
                fail(
                        W1221RoamingFailure.INVALID_TOPOLOGY,
                        topologyDetail()
                );
                return;
            }

            switch (stage) {
                case SETUP ->
                        setup(
                                station,
                                ap1,
                                ap2,
                                networkSwitch,
                                server
                        );

                case INITIAL_SCAN ->
                        initialScan(
                                station,
                                ap1,
                                ap2
                        );

                case CONNECT_AP1 ->
                        connectAp1(
                                station,
                                ap1
                        );

                case BASELINE_HTTP ->
                        baselineHttp(
                                station,
                                ap1,
                                ap2
                        );

                case ROAM_SCAN ->
                        roamScan(
                                station,
                                ap1,
                                ap2
                        );

                case ROAM_CONNECTING ->
                        roamConnecting(
                                station,
                                ap1,
                                ap2
                        );

                case POST_ROAM_DATA_ACK ->
                        postRoamDataAck(
                                station
                        );

                case POST_ROAM_HTTP ->
                        postRoamHttp(
                                station,
                                ap1,
                                ap2,
                                networkSwitch,
                                server
                        );

                case COMPLETE,
                     FAILED -> {
                }
            }
        } catch (Exception exception) {
            fail(
                    W1221RoamingFailure.INTERNAL_ERROR,
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
                == W1221RoamingStage.COMPLETE
                || stage
                == W1221RoamingStage.FAILED;
    }

    public boolean passed() {
        return stage
                == W1221RoamingStage.COMPLETE
                && failure
                == W1221RoamingFailure.NONE;
    }

    public W1221RoamingSnapshot snapshot() {
        NetworkDeviceBlockEntity station =
                station();

        NetworkDeviceBlockEntity ap1 =
                ap1();

        NetworkDeviceBlockEntity ap2 =
                ap2();

        List<WifiPacketTraceEvent> stationTrace =
                station == null
                        ? List.of()
                        : station.wifiPacketTraceSnapshot();

        List<WifiPacketTraceEvent> ap1Trace =
                ap1 == null
                        ? List.of()
                        : ap1.wifiPacketTraceSnapshot();

        List<WifiPacketTraceEvent> ap2Trace =
                ap2 == null
                        ? List.of()
                        : ap2.wifiPacketTraceSnapshot();

        long retries =
                stationTrace.stream()
                        .filter(
                                WifiPacketTraceEvent::retry
                        )
                        .count()
                        + ap1Trace.stream()
                        .filter(
                                WifiPacketTraceEvent::retry
                        )
                        .count()
                        + ap2Trace.stream()
                        .filter(
                                WifiPacketTraceEvent::retry
                        )
                        .count();

        long ackRx =
                ackRxCount(
                        stationTrace
                );

        WifiNetworkRecord candidate =
                station == null
                        ? null
                        : station.bestWifiRoamCandidate();

        WifiRawIpWorkflowSnapshot workflow =
                station == null
                        ? null
                        : station.wifiRawIpWorkflowSnapshot();

        String ap1Bridge =
                ap1 == null
                        ? ""
                        : safe(
                        ap1.w119BridgeStatus()
                );

        String ap2Bridge =
                ap2 == null
                        ? ""
                        : safe(
                        ap2.w119BridgeStatus()
                );

        long now =
                level.getGameTime();

        return new W1221RoamingSnapshot(
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
                        station.wifiSelectedSsid()
                ),
                station == null
                        ? ""
                        : safe(
                        station.wifiSelectedBssid()
                ),
                ap1 == null
                        ? ""
                        : safe(
                        ap1.wifiMacAddress()
                ),
                ap2 == null
                        ? ""
                        : safe(
                        ap2.wifiMacAddress()
                ),
                station == null
                        || ap1 == null
                        ? Double.NaN
                        : station.discoveredWifiNetworkSnrDb(
                        ap1.wifiMacAddress()
                ),
                station == null
                        || ap2 == null
                        ? Double.NaN
                        : station.discoveredWifiNetworkSnrDb(
                        ap2.wifiMacAddress()
                ),
                candidate == null
                        ? ""
                        : safe(
                        candidate.bssid()
                ),
                candidate == null
                        || station == null
                        ? Double.NaN
                        : station.discoveredWifiNetworkSnrDb(
                        candidate.bssid()
                ),
                station != null
                        && station.wifiBackgroundRoamingEnabled(),
                workflow == null
                        ? "N/A"
                        : workflow.state()
                        .name(),
                workflow == null
                        ? ""
                        : safe(
                        workflow.detail()
                ),
                stationTrace.size(),
                ap1Trace.size(),
                ap2Trace.size(),
                retries,
                ackRx,
                metric(
                        ap1Bridge,
                        DS_TX
                ),
                metric(
                        ap1Bridge,
                        DS_RX
                ),
                metric(
                        ap2Bridge,
                        DS_TX
                ),
                metric(
                        ap2Bridge,
                        DS_RX
                ),
                ap1Bridge,
                ap2Bridge
        );
    }

    private void setup(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2,
            NetworkSwitchBlockEntity networkSwitch,
            ServerRackBlockEntity server
    ) {
        String topologyFailure =
                validatePhysicalTopology(
                        ap1,
                        ap2,
                        networkSwitch,
                        server
                );

        if (!topologyFailure.isBlank()) {
            fail(
                    W1221RoamingFailure.INVALID_TOPOLOGY,
                    topologyFailure
            );
            return;
        }

        station.clearWifiPacketTrace();
        ap1.clearWifiPacketTrace();
        ap2.clearWifiPacketTrace();

        station.clearWifiIpMetrics();
        station.clearWifiTcpLive();

        station.clearWifiContentionMetrics();
        ap1.clearWifiContentionMetrics();
        ap2.clearWifiContentionMetrics();

        boolean ap1Configured =
                ap1.configureWifiAccessPoint(
                        SSID,
                        ""
                );

        boolean ap2Configured =
                ap2.configureWifiAccessPoint(
                        SSID,
                        ""
                );

        if (!ap1Configured
                || !ap2Configured) {
            fail(
                    W1221RoamingFailure.AP_CONFIGURATION_FAILED,
                    "AP configuration failed: ap1="
                            + ap1Configured
                            + " ap2="
                            + ap2Configured
            );
            return;
        }

        if (W119Mac.equals(
                ap1.wifiMacAddress(),
                ap2.wifiMacAddress()
        )) {
            fail(
                    W1221RoamingFailure.INVALID_TOPOLOGY,
                    "AP1 and AP2 must have different BSSIDs"
            );
            return;
        }

        boolean stationConfigured =
                station.configureWifiStation(
                        ""
                );

        if (!stationConfigured) {
            fail(
                    W1221RoamingFailure.STATION_CONFIGURATION_FAILED,
                    "Station target rejected Wi-Fi STATION configuration"
            );
            return;
        }

        station.setWifiBackgroundRoamingEnabled(
                false
        );

        ap1.sendWifiBeacon();
        ap2.sendWifiBeacon();

        boolean scanStarted =
                station.scanWifi();

        if (!scanStarted) {
            fail(
                    W1221RoamingFailure.INITIAL_SCAN_START_FAILED,
                    "Station could not start initial Wi-Fi scan"
            );
            return;
        }

        transition(
                W1221RoamingStage.INITIAL_SCAN,
                "Two-AP ESS configured as "
                        + SSID
                        + "; scanning for AP1 "
                        + ap1.wifiMacAddress()
                        + " and AP2 "
                        + ap2.wifiMacAddress()
        );
    }

    private void initialScan(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2
    ) {
        long elapsed =
                elapsedStageTicks();

        if (elapsed % 20L == 0L) {
            ap1.sendWifiBeacon();
            ap2.sendWifiBeacon();
        }

        WifiNetworkRecord ap1Record =
                discovered(
                        station,
                        ap1
                );

        WifiNetworkRecord ap2Record =
                discovered(
                        station,
                        ap2
                );

        if (ap1Record != null
                && ap2Record != null) {
            boolean started =
                    station.connectWifiBssid(
                            ap1Record.ssid(),
                            ap1Record.bssid()
                    );

            if (!started) {
                fail(
                        W1221RoamingFailure.AP1_CONNECT_START_FAILED,
                        "Exact-BSSID connection to AP1 was rejected"
                );
                return;
            }

            transition(
                    W1221RoamingStage.CONNECT_AP1,
                    "Both APs discovered. Forced initial association to AP1 "
                            + ap1Record.bssid()
            );
            return;
        }

        if (elapsed > INITIAL_SCAN_TIMEOUT_TICKS) {
            fail(
                    W1221RoamingFailure.INITIAL_SCAN_TIMEOUT,
                    "Initial scan did not discover both BSSIDs within "
                            + INITIAL_SCAN_TIMEOUT_TICKS
                            + " ticks | AP1="
                            + (ap1Record != null)
                            + " AP2="
                            + (ap2Record != null)
            );
        }
    }

    private void connectAp1(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap1
    ) {
        if (station.wifiSecurityState()
                == WifiSecurityState.FAILED) {
            fail(
                    W1221RoamingFailure.SECURITY_FAILED,
                    station.wifiSecurityDiagnostic()
            );
            return;
        }

        boolean associated =
                station.wifiStationState()
                        == WifiStationState.ASSOCIATED
                        && W119Mac.equals(
                        station.wifiSelectedBssid(),
                        ap1.wifiMacAddress()
                )
                        && ap1.wifiAssociatedStations()
                        .stream()
                        .anyMatch(
                                value ->
                                        W119Mac.equals(
                                                value,
                                                station.wifiMacAddress()
                                        )
                        );

        if (associated) {
            baselineWorkflowStarted =
                    station.startWifiRawHttpWorkflow(
                            "www.vsia-net.com",
                            "/"
                    );

            if (!baselineWorkflowStarted) {
                fail(
                        W1221RoamingFailure.BASELINE_HTTP_START_FAILED,
                        "Baseline DHCP->ARP->DNS->TCP->HTTP workflow could not start on AP1"
                );
                return;
            }

            transition(
                    W1221RoamingStage.BASELINE_HTTP,
                    "Associated to AP1; proving baseline HTTP 200 before roaming"
            );
            return;
        }

        if (elapsedStageTicks()
                > CONNECT_TIMEOUT_TICKS) {
            fail(
                    W1221RoamingFailure.AP1_ASSOCIATION_TIMEOUT,
                    "AP1 association timeout | state="
                            + station.wifiStationState()
                            + " selected="
                            + station.wifiSelectedBssid()
                            + " security="
                            + station.wifiSecurityState()
                            + " diagnostic="
                            + station.wifiSecurityDiagnostic()
            );
        }
    }

    private void baselineHttp(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2
    ) {
        if (!baselineWorkflowStarted) {
            fail(
                    W1221RoamingFailure.BASELINE_HTTP_START_FAILED,
                    "Baseline HTTP stage entered without a running workflow"
            );
            return;
        }

        WifiRawIpWorkflowSnapshot workflow =
                station.wifiRawIpWorkflowSnapshot();

        if (workflow.state()
                == WifiRawIpWorkflowState.FAILED) {
            fail(
                    W1221RoamingFailure.BASELINE_HTTP_FAILED,
                    workflow.detail()
            );
            return;
        }

        if (workflow.state()
                == WifiRawIpWorkflowState.COMPLETE) {
            if (!workflow.detail()
                    .contains(
                            "HTTP 200"
                    )) {
                fail(
                        W1221RoamingFailure.BASELINE_HTTP_FAILED,
                        workflow.detail()
                );
                return;
            }

            baselineIp =
                    safe(
                            station.wifiIpAddress()
                    );

            if (!usableIpv4(
                    baselineIp
            )) {
                fail(
                        W1221RoamingFailure.BASELINE_HTTP_FAILED,
                        "Baseline HTTP completed without a usable station IPv4 address"
                );
                return;
            }

            WifiNetworkRecord ap2Record =
                    discovered(
                            station,
                            ap2
                    );

            ap2SeenBeforeRoamScan =
                    ap2Record == null
                            ? -1L
                            : ap2Record.lastSeenNanos();

            roamScanStarted =
                    station.scanWifi();

            if (!roamScanStarted) {
                fail(
                        W1221RoamingFailure.ROAM_SCAN_START_FAILED,
                        "Associated roaming scan could not start"
                );
                return;
            }

            transition(
                    W1221RoamingStage.ROAM_SCAN,
                    "Baseline HTTP 200 passed on AP1 with IP "
                            + baselineIp
                            + "; associated scan started while preserving AP1 link"
            );
            return;
        }

        if (elapsedStageTicks()
                > BASELINE_HTTP_TIMEOUT_TICKS) {
            fail(
                    W1221RoamingFailure.BASELINE_HTTP_FAILED,
                    "Baseline RAW HTTP exceeded "
                            + BASELINE_HTTP_TIMEOUT_TICKS
                            + " ticks in "
                            + workflow.state()
                            + ": "
                            + workflow.detail()
            );
        }
    }

    private void roamScan(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2
    ) {
        long elapsed =
                elapsedStageTicks();

        if (elapsed % 20L == 0L) {
            ap1.sendWifiBeacon();
            ap2.sendWifiBeacon();
        }

        if (station.wifiStationState()
                != WifiStationState.ASSOCIATED
                || !W119Mac.equals(
                station.wifiSelectedBssid(),
                ap1.wifiMacAddress()
        )) {
            fail(
                    W1221RoamingFailure.ROAM_SCAN_BROKE_ASSOCIATION,
                    "Associated scan did not preserve AP1 association | state="
                            + station.wifiStationState()
                            + " selected="
                            + station.wifiSelectedBssid()
            );
            return;
        }

        WifiNetworkRecord ap2Record =
                discovered(
                        station,
                        ap2
                );

        boolean refreshed =
                ap2Record != null
                        && (
                        ap2SeenBeforeRoamScan < 0L
                                || ap2Record.lastSeenNanos()
                                > ap2SeenBeforeRoamScan
                );

        boolean scanComplete =
                station.wifiSecurityDiagnostic()
                        .startsWith(
                                "ROAM_SCAN_COMPLETE"
                        );

        if (refreshed
                && scanComplete) {
            WifiNetworkRecord candidate =
                    station.bestWifiRoamCandidate();

            if (candidate == null) {
                fail(
                        W1221RoamingFailure.ROAM_CANDIDATE_MISSING,
                        "Roam scan completed but no same-SSID/security alternate candidate was selected"
                );
                return;
            }

            if (!W119Mac.equals(
                    candidate.bssid(),
                    ap2.wifiMacAddress()
            )) {
                fail(
                        W1221RoamingFailure.ROAM_CANDIDATE_WRONG,
                        "Expected AP2 candidate "
                                + ap2.wifiMacAddress()
                                + " but selected "
                                + candidate.bssid()
                );
                return;
            }

            double currentSnr =
                    station.wifiSelectedApSnrDb();

            double candidateSnr =
                    station.discoveredWifiNetworkSnrDb(
                            candidate.bssid()
                    );

            if (!Double.isFinite(
                    currentSnr
            )
                    || !Double.isFinite(
                    candidateSnr
            )
                    || candidateSnr
                    < currentSnr
                    + ROAM_HYSTERESIS_DB) {
                fail(
                        W1221RoamingFailure.HYSTERESIS_NOT_MET,
                        "AP2 is not at least +"
                                + formatDb(
                                ROAM_HYSTERESIS_DB
                        )
                                + " dB better than AP1 | AP1="
                                + formatDb(
                                currentSnr
                        )
                                + " dB AP2="
                                + formatDb(
                                candidateSnr
                        )
                                + " dB. Move STA closer to AP2 or AP1 farther away, then rerun."
                );
                return;
            }

            roamStarted =
                    station.roamWifiToBestCandidate(
                            ROAM_HYSTERESIS_DB
                    );

            if (!roamStarted) {
                fail(
                        W1221RoamingFailure.ROAM_START_FAILED,
                        "Production roamWifiToBestCandidate(+6 dB) rejected AP2"
                );
                return;
            }

            transition(
                    W1221RoamingStage.ROAM_CONNECTING,
                    "Roam started AP1 "
                            + ap1.wifiMacAddress()
                            + " -> AP2 "
                            + ap2.wifiMacAddress()
                            + " | "
                            + formatDb(
                            currentSnr
                    )
                            + " -> "
                            + formatDb(
                            candidateSnr
                    )
                            + " dB"
            );
            return;
        }

        if (elapsed > ROAM_SCAN_TIMEOUT_TICKS) {
            fail(
                    W1221RoamingFailure.ROAM_SCAN_TIMEOUT,
                    "Associated roam scan did not complete/refesh AP2 within "
                            + ROAM_SCAN_TIMEOUT_TICKS
                            + " ticks | diagnostic="
                            + station.wifiSecurityDiagnostic()
            );
        }
    }

    private void roamConnecting(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2
    ) {
        if (station.wifiSecurityState()
                == WifiSecurityState.FAILED) {
            fail(
                    W1221RoamingFailure.SECURITY_FAILED,
                    station.wifiSecurityDiagnostic()
            );
            return;
        }

        boolean associatedAp2 =
                station.wifiStationState()
                        == WifiStationState.ASSOCIATED
                        && W119Mac.equals(
                        station.wifiSelectedBssid(),
                        ap2.wifiMacAddress()
                )
                        && ap2.wifiAssociatedStations()
                        .stream()
                        .anyMatch(
                                value ->
                                        W119Mac.equals(
                                                value,
                                                station.wifiMacAddress()
                                        )
                        );

        if (associatedAp2) {
            if (!baselineIp.equals(
                    station.wifiIpAddress()
            )) {
                fail(
                        W1221RoamingFailure.IP_CHANGED_DURING_ROAM,
                        "Layer-2 roam changed station IPv4 "
                                + baselineIp
                                + " -> "
                                + station.wifiIpAddress()
                );
                return;
            }

            postRoamAckBaseline =
                    ackRxCount(
                            station.wifiPacketTraceSnapshot()
                    );

            postRoamDataQueued =
                    station.sendWifiEngineeringAssociatedData(
                            512
                    );

            if (!postRoamDataQueued) {
                fail(
                        W1221RoamingFailure.POST_ROAM_DATA_START_FAILED,
                        "Associated DATA could not be queued after roaming to AP2"
                );
                return;
            }

            transition(
                    W1221RoamingStage.POST_ROAM_DATA_ACK,
                    "Associated to AP2 with IPv4 preserved at "
                            + baselineIp
                            + "; waiting for post-roam DATA ACK"
            );
            return;
        }

        if (elapsedStageTicks()
                > ROAM_CONNECT_TIMEOUT_TICKS) {
            fail(
                    W1221RoamingFailure.ROAM_TIMEOUT,
                    "Roam association timeout | state="
                            + station.wifiStationState()
                            + " selected="
                            + station.wifiSelectedBssid()
                            + " AP2="
                            + ap2.wifiMacAddress()
                            + " security="
                            + station.wifiSecurityState()
                            + " diagnostic="
                            + station.wifiSecurityDiagnostic()
            );
        }
    }

    private void postRoamDataAck(
            NetworkDeviceBlockEntity station
    ) {
        long ackNow =
                ackRxCount(
                        station.wifiPacketTraceSnapshot()
                );

        if (ackNow > postRoamAckBaseline) {
            postRoamWorkflowStarted =
                    station.startWifiRawHttpWorkflow(
                            "www.vsia-net.com",
                            "/"
                    );

            if (!postRoamWorkflowStarted) {
                fail(
                        W1221RoamingFailure.POST_ROAM_HTTP_START_FAILED,
                        "Post-roam DHCP->ARP->DNS->TCP->HTTP workflow could not start"
                );
                return;
            }

            transition(
                    W1221RoamingStage.POST_ROAM_HTTP,
                    "Post-roam DATA/ACK passed; proving HTTP 200 through AP2"
            );
            return;
        }

        if (elapsedStageTicks()
                > DATA_ACK_TIMEOUT_TICKS) {
            fail(
                    W1221RoamingFailure.POST_ROAM_ACK_TIMEOUT,
                    "No new RX CONTROL subtype 13 ACK observed after AP2 roam"
            );
        }
    }

    private void postRoamHttp(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2,
            NetworkSwitchBlockEntity networkSwitch,
            ServerRackBlockEntity server
    ) {
        if (!postRoamWorkflowStarted) {
            fail(
                    W1221RoamingFailure.POST_ROAM_HTTP_START_FAILED,
                    "Post-roam HTTP stage entered without a running workflow"
            );
            return;
        }

        WifiRawIpWorkflowSnapshot workflow =
                station.wifiRawIpWorkflowSnapshot();

        if (workflow.state()
                == WifiRawIpWorkflowState.FAILED) {
            fail(
                    W1221RoamingFailure.POST_ROAM_HTTP_FAILED,
                    workflow.detail()
            );
            return;
        }

        if (workflow.state()
                == WifiRawIpWorkflowState.COMPLETE) {
            if (!workflow.detail()
                    .contains(
                            "HTTP 200"
                    )) {
                fail(
                        W1221RoamingFailure.POST_ROAM_HTTP_FAILED,
                        workflow.detail()
                );
                return;
            }

            if (!baselineIp.equals(
                    station.wifiIpAddress()
            )) {
                fail(
                        W1221RoamingFailure.IP_CHANGED_DURING_ROAM,
                        "Post-roam workflow changed station IPv4 "
                                + baselineIp
                                + " -> "
                                + station.wifiIpAddress()
                );
                return;
            }

            String topologyFailure =
                    validatePhysicalTopology(
                            ap1,
                            ap2,
                            networkSwitch,
                            server
                    );

            if (!topologyFailure.isBlank()) {
                fail(
                        W1221RoamingFailure.INVALID_TOPOLOGY,
                        topologyFailure
                );
                return;
            }

            String ap1Bridge =
                    ap1.w119BridgeStatus();

            String ap2Bridge =
                    ap2.w119BridgeStatus();

            long ap1DsTx =
                    metric(
                            ap1Bridge,
                            DS_TX
                    );

            long ap1DsRx =
                    metric(
                            ap1Bridge,
                            DS_RX
                    );

            long ap2DsTx =
                    metric(
                            ap2Bridge,
                            DS_TX
                    );

            long ap2DsRx =
                    metric(
                            ap2Bridge,
                            DS_RX
                    );

            if (ap1DsTx <= 0L
                    || ap1DsRx <= 0L
                    || ap2DsTx <= 0L
                    || ap2DsRx <= 0L) {
                fail(
                        W1221RoamingFailure.DISTRIBUTION_PATH_NOT_OBSERVED,
                        "Expected bidirectional DS traffic through both APs | AP1 dsTx="
                                + ap1DsTx
                                + " dsRx="
                                + ap1DsRx
                                + " AP2 dsTx="
                                + ap2DsTx
                                + " dsRx="
                                + ap2DsRx
                );
                return;
            }

            transition(
                    W1221RoamingStage.COMPLETE,
                    "PASS: manual roam "
                            + ap1.wifiMacAddress()
                            + " -> "
                            + ap2.wifiMacAddress()
                            + " | IPv4="
                            + baselineIp
                            + " preserved | HTTP 200 before+after roam | both AP DS paths observed"
            );
            return;
        }

        if (elapsedStageTicks()
                > POST_ROAM_HTTP_TIMEOUT_TICKS) {
            fail(
                    W1221RoamingFailure.POST_ROAM_HTTP_FAILED,
                    "Post-roam RAW HTTP exceeded "
                            + POST_ROAM_HTTP_TIMEOUT_TICKS
                            + " ticks in "
                            + workflow.state()
                            + ": "
                            + workflow.detail()
            );
        }
    }

    private String validatePhysicalTopology(
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2,
            NetworkSwitchBlockEntity networkSwitch,
            ServerRackBlockEntity server
    ) {
        if (stationPos.equals(
                ap1Pos
        )
                || stationPos.equals(
                ap2Pos
        )
                || ap1Pos.equals(
                ap2Pos
        )
                || switchPos.equals(
                serverPos
        )) {
            return "STA, AP1, AP2, Switch and Server must be distinct blocks";
        }

        if (!switchConnectedToAp(
                networkSwitch,
                ap1Pos,
                ap1,
                switchPos
        )) {
            return "AP1 is not physically attached to the supplied Network Switch";
        }

        if (!switchConnectedToAp(
                networkSwitch,
                ap2Pos,
                ap2,
                switchPos
        )) {
            return "AP2 is not physically attached to the supplied Network Switch";
        }

        if (!networkSwitch
                .getConnectedDevices()
                .contains(
                        serverPos
                )) {
            return "Server Rack is not directly attached to the supplied Network Switch";
        }

        if (!server.dhcpEnabled()
                || !server.dnsEnabled()
                || !server.httpEnabled()) {
            return "Server Rack must have DHCP, DNS and HTTP enabled";
        }

        return "";
    }

    private boolean switchConnectedToAp(
            NetworkSwitchBlockEntity networkSwitch,
            BlockPos apPos,
            NetworkDeviceBlockEntity accessPoint,
            BlockPos switchPos
    ) {
        if (networkSwitch
                .getConnectedDevices()
                .contains(
                        apPos
                )) {
            return true;
        }

        if (accessPoint
                instanceof RtAc68uRouterBlockEntity router) {
            return !router
                    .w121InterfaceForPeer(
                            switchPos
                    )
                    .isBlank();
        }

        return false;
    }

    private WifiNetworkRecord discovered(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap
    ) {
        return station.discoveredWifiNetworks()
                .stream()
                .filter(
                        candidate ->
                                candidate != null
                                        && SSID.equals(
                                        candidate.ssid()
                                )
                                        && W119Mac.equals(
                                        candidate.bssid(),
                                        ap.wifiMacAddress()
                                )
                )
                .findFirst()
                .orElse(
                        null
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

    private NetworkDeviceBlockEntity ap1() {
        BlockEntity blockEntity =
                level.getBlockEntity(
                        ap1Pos
                );

        return blockEntity
                instanceof NetworkDeviceBlockEntity device
                ? device
                : null;
    }

    private NetworkDeviceBlockEntity ap2() {
        BlockEntity blockEntity =
                level.getBlockEntity(
                        ap2Pos
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
                instanceof NetworkSwitchBlockEntity networkSwitch
                ? networkSwitch
                : null;
    }

    private ServerRackBlockEntity server() {
        BlockEntity blockEntity =
                level.getBlockEntity(
                        serverPos
                );

        return blockEntity
                instanceof ServerRackBlockEntity server
                ? server
                : null;
    }

    private String topologyDetail() {
        BlockEntity stationEntity =
                level.getBlockEntity(
                        stationPos
                );

        BlockEntity ap1Entity =
                level.getBlockEntity(
                        ap1Pos
                );

        BlockEntity ap2Entity =
                level.getBlockEntity(
                        ap2Pos
                );

        BlockEntity switchEntity =
                level.getBlockEntity(
                        switchPos
                );

        BlockEntity serverEntity =
                level.getBlockEntity(
                        serverPos
                );

        return "Expected NetworkDevice STA at "
                + stationPos.toShortString()
                + ", NetworkDevice AP1 at "
                + ap1Pos.toShortString()
                + ", NetworkDevice AP2 at "
                + ap2Pos.toShortString()
                + ", NetworkSwitch at "
                + switchPos.toShortString()
                + ", ServerRack at "
                + serverPos.toShortString()
                + " | actual="
                + typeName(
                stationEntity
        )
                + ","
                + typeName(
                ap1Entity
        )
                + ","
                + typeName(
                ap2Entity
        )
                + ","
                + typeName(
                switchEntity
        )
                + ","
                + typeName(
                serverEntity
        );
    }

    private String typeName(
            BlockEntity blockEntity
    ) {
        return blockEntity == null
                ? "MISSING"
                : blockEntity.getClass()
                .getSimpleName();
    }

    private long ackRxCount(
            List<WifiPacketTraceEvent> trace
    ) {
        return trace.stream()
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
    }

    private long elapsedStageTicks() {
        return Math.max(
                0L,
                level.getGameTime()
                        - stageStartTick
        );
    }

    private void transition(
            W1221RoamingStage next,
            String nextDetail
    ) {
        stage =
                next;

        stageStartTick =
                level.getGameTime();

        detail =
                safe(
                        nextDetail
                );
    }

    private void fail(
            W1221RoamingFailure reason,
            String failureDetail
    ) {
        failure =
                reason == null
                        ? W1221RoamingFailure.INTERNAL_ERROR
                        : reason;

        stage =
                W1221RoamingStage.FAILED;

        detail =
                safe(
                        failureDetail
                );
    }

    private long metric(
            String value,
            Pattern pattern
    ) {
        if (value == null
                || value.isBlank()) {
            return 0L;
        }

        Matcher matcher =
                pattern.matcher(
                        value
                );

        return matcher.find()
                ? Long.parseLong(
                matcher.group(
                        1
                )
        )
                : 0L;
    }

    private boolean usableIpv4(
            String value
    ) {
        if (value == null
                || value.isBlank()
                || "0.0.0.0".equals(
                value
        )
                || "255.255.255.255".equals(
                value
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
                int octet =
                        Integer.parseInt(
                                part
                        );

                if (octet < 0
                        || octet > 255) {
                    return false;
                }
            }
        } catch (NumberFormatException exception) {
            return false;
        }

        return true;
    }

    private String formatDb(
            double value
    ) {
        return Double.isFinite(
                value
        )
                ? String.format(
                Locale.ROOT,
                "%.1f",
                value
        )
                : "n/a";
    }

    private String safe(
            String value
    ) {
        return value == null
                ? ""
                : value;
    }
}
