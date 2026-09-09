package com.k1ngtle.vsia.signality.engineering.wifi.integration.w1222;

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

public final class W1222AutoRoamSession {
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

    private static final long MAC_DRAIN_TIMEOUT_TICKS =
            240L;

    private static final long BACKGROUND_SCAN_OBSERVE_TIMEOUT_TICKS =
            240L;

    private static final long AUTO_ROAM_TIMEOUT_TICKS =
            600L;

    private static final long OLD_AP_CLEANUP_TIMEOUT_TICKS =
            100L;

    private static final long DATA_ACK_TIMEOUT_TICKS =
            160L;

    private static final long POST_ROAM_HTTP_TIMEOUT_TICKS =
            1_200L;

    private static final long COOLDOWN_STABILITY_TICKS =
            140L;

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

    private W1222AutoRoamStage stage =
            W1222AutoRoamStage.SETUP;

    private W1222AutoRoamFailure failure =
            W1222AutoRoamFailure.NONE;

    private String detail =
            "Waiting for first server tick";

    private long startTick =
            -1L;

    private long stageStartTick =
            -1L;

    private String baselineIp =
            "";

    private long baselineHttpCompleteTick =
            -1L;

    private long postRoamAckBaseline;

    private boolean baselineWorkflowStarted;
    private boolean associatedRoamScanObserved;
    private boolean postRoamDataQueued;
    private boolean postRoamWorkflowStarted;

    public W1222AutoRoamSession(
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
                        W1222AutoRoamFailure.INVALID_TOPOLOGY,
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

                case PREPARE_AUTO_ROAM ->
                        prepareAutoRoam(
                                station,
                                ap1,
                                ap2
                        );

                case WAIT_BACKGROUND_SCAN ->
                        waitBackgroundScan(
                                station,
                                ap1,
                                ap2
                        );

                case WAIT_AUTO_ROAM ->
                        waitAutoRoam(
                                station,
                                ap1,
                                ap2
                        );

                case OLD_AP_CLEANUP ->
                        oldApCleanup(
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

                case COOLDOWN_STABILITY ->
                        cooldownStability(
                                station,
                                ap1,
                                ap2
                        );

                case COMPLETE,
                     FAILED -> {
                }
            }
        } catch (Exception exception) {
            fail(
                    W1222AutoRoamFailure.INTERNAL_ERROR,
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
                == W1222AutoRoamStage.COMPLETE
                || stage
                == W1222AutoRoamStage.FAILED;
    }

    public boolean passed() {
        return stage
                == W1222AutoRoamStage.COMPLETE
                && failure
                == W1222AutoRoamFailure.NONE;
    }

    public W1222AutoRoamSnapshot snapshot() {
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

        return new W1222AutoRoamSnapshot(
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
                associatedRoamScanObserved,
                station == null
                        ? -1
                        : station.wifiPendingDataTransmissions(),
                station == null
                        ? ""
                        : safe(
                        station.wifiSecurityDiagnostic()
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
                stationTrace.size(),
                ap1Trace.size(),
                ap2Trace.size(),
                retries,
                ackRx,
                ap1 == null
                        ? -1
                        : ap1.wifiAssociatedStations()
                        .size(),
                ap2 == null
                        ? -1
                        : ap2.wifiAssociatedStations()
                        .size(),
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
                    W1222AutoRoamFailure.INVALID_TOPOLOGY,
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
                    W1222AutoRoamFailure.AP_CONFIGURATION_FAILED,
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
                    W1222AutoRoamFailure.INVALID_TOPOLOGY,
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
                    W1222AutoRoamFailure.STATION_CONFIGURATION_FAILED,
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
                    W1222AutoRoamFailure.INITIAL_SCAN_START_FAILED,
                    "Station could not start initial Wi-Fi scan"
            );
            return;
        }

        transition(
                W1222AutoRoamStage.INITIAL_SCAN,
                "W1.22 automatic-roam test configured two APs as "
                        + SSID
                        + "; initial normal scan started with background roaming disabled"
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

        boolean scanComplete =
                station.wifiSecurityDiagnostic()
                        .startsWith(
                                "SCAN_COMPLETE_APS_"
                        );

        if (ap1Record != null
                && ap2Record != null
                && scanComplete) {
            boolean started =
                    station.connectWifiBssid(
                            ap1Record.ssid(),
                            ap1Record.bssid()
                    );

            if (!started) {
                fail(
                        W1222AutoRoamFailure.AP1_CONNECT_START_FAILED,
                        "Exact-BSSID initial connection to AP1 was rejected"
                );
                return;
            }

            transition(
                    W1222AutoRoamStage.CONNECT_AP1,
                    "Both APs discovered; exact-BSSID association to AP1 started"
            );
            return;
        }

        if (elapsed
                > INITIAL_SCAN_TIMEOUT_TICKS) {
            fail(
                    W1222AutoRoamFailure.INITIAL_SCAN_TIMEOUT,
                    "Initial scan did not finish with both BSSIDs"
                            + " | AP1="
                            + (ap1Record != null)
                            + " AP2="
                            + (ap2Record != null)
                            + " diagnostic="
                            + station.wifiSecurityDiagnostic()
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
                    W1222AutoRoamFailure.SECURITY_FAILED,
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
                        && containsStation(
                        ap1,
                        station
                );

        if (associated) {
            baselineWorkflowStarted =
                    station.startWifiRawHttpWorkflow(
                            "www.vsia-net.com",
                            "/"
                    );

            if (!baselineWorkflowStarted) {
                fail(
                        W1222AutoRoamFailure.BASELINE_HTTP_START_FAILED,
                        "Baseline RAW HTTP workflow could not start on AP1"
                );
                return;
            }

            transition(
                    W1222AutoRoamStage.BASELINE_HTTP,
                    "Associated and secured on AP1; proving baseline HTTP 200"
            );
            return;
        }

        if (elapsedStageTicks()
                > CONNECT_TIMEOUT_TICKS) {
            fail(
                    W1222AutoRoamFailure.AP1_ASSOCIATION_TIMEOUT,
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
        WifiRawIpWorkflowSnapshot workflow =
                station.wifiRawIpWorkflowSnapshot();

        if (workflow.state()
                == WifiRawIpWorkflowState.FAILED) {
            fail(
                    W1222AutoRoamFailure.BASELINE_HTTP_FAILED,
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
                        W1222AutoRoamFailure.BASELINE_HTTP_FAILED,
                        workflow.detail()
                );
                return;
            }

            if (baselineHttpCompleteTick < 0L) {
                baselineHttpCompleteTick =
                        level.getGameTime();

                baselineIp =
                        safe(
                                station.wifiIpAddress()
                        );

                if (!usableIpv4(
                        baselineIp
                )) {
                    fail(
                            W1222AutoRoamFailure.BASELINE_HTTP_FAILED,
                            "Baseline HTTP completed without a usable station IPv4 address"
                    );
                    return;
                }
            }

            transition(
                    W1222AutoRoamStage.PREPARE_AUTO_ROAM,
                    "Baseline HTTP 200 passed through AP1 with IPv4 "
                            + baselineIp
                            + "; waiting for MAC queue drain before enabling automatic roaming"
            );
            return;
        }

        if (elapsedStageTicks()
                > BASELINE_HTTP_TIMEOUT_TICKS) {
            fail(
                    W1222AutoRoamFailure.BASELINE_HTTP_FAILED,
                    "Baseline RAW HTTP exceeded "
                            + BASELINE_HTTP_TIMEOUT_TICKS
                            + " ticks in "
                            + workflow.state()
                            + ": "
                            + workflow.detail()
            );
        }
    }

    private void prepareAutoRoam(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2
    ) {
        int pending =
                station.wifiPendingDataTransmissions();

        if (pending > 0) {
            detail =
                    "Waiting for Wi-Fi MAC queue to drain before auto-roam enable"
                            + " | pendingData="
                            + pending;

            if (elapsedStageTicks()
                    > MAC_DRAIN_TIMEOUT_TICKS) {
                fail(
                        W1222AutoRoamFailure.MAC_QUEUE_DRAIN_TIMEOUT,
                        detail
                                + " | diagnostic="
                                + station.wifiSecurityDiagnostic()
                );
            }

            return;
        }

        WifiNetworkRecord candidate =
                station.bestWifiRoamCandidate();

        if (candidate == null) {
            fail(
                    W1222AutoRoamFailure.ROAM_CANDIDATE_MISSING,
                    "No alternate same-SSID/security roaming candidate is available before enabling auto roam"
            );
            return;
        }

        if (!W119Mac.equals(
                candidate.bssid(),
                ap2.wifiMacAddress()
        )) {
            fail(
                    W1222AutoRoamFailure.ROAM_CANDIDATE_WRONG,
                    "Expected AP2 as candidate "
                            + ap2.wifiMacAddress()
                            + " but got "
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
                    W1222AutoRoamFailure.HYSTERESIS_NOT_MET,
                    "AP2 must be at least +"
                            + formatDb(
                            ROAM_HYSTERESIS_DB
                    )
                            + " dB stronger before automatic roaming is enabled"
                            + " | AP1="
                            + formatDb(
                            currentSnr
                    )
                            + " dB AP2="
                            + formatDb(
                            candidateSnr
                    )
                            + " dB"
            );
            return;
        }

        associatedRoamScanObserved =
                false;

        station.setWifiBackgroundRoamingEnabled(
                true
        );

        transition(
                W1222AutoRoamStage.WAIT_BACKGROUND_SCAN,
                "Background roaming enabled. No manual scan/roam call will be made from this point."
                        + " Waiting for production 2-second associated roam scan."
        );
    }

    private void waitBackgroundScan(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2
    ) {
        if (elapsedStageTicks()
                % 20L == 0L) {
            ap1.sendWifiBeacon();
            ap2.sendWifiBeacon();
        }

        String diagnostic =
                safe(
                        station.wifiSecurityDiagnostic()
                );

        if (diagnostic.startsWith(
                "ROAM_SCAN_ACTIVE"
        )) {
            associatedRoamScanObserved =
                    true;

            if (station.wifiStationState()
                    != WifiStationState.ASSOCIATED
                    || !W119Mac.equals(
                    station.wifiSelectedBssid(),
                    ap1.wifiMacAddress()
            )) {
                fail(
                        W1222AutoRoamFailure.BACKGROUND_SCAN_BROKE_ASSOCIATION,
                        "Production background scan entered but AP1 association was not preserved"
                                + " | state="
                                + station.wifiStationState()
                                + " selected="
                                + station.wifiSelectedBssid()
                );
                return;
            }

            transition(
                    W1222AutoRoamStage.WAIT_AUTO_ROAM,
                    "Observed ROAM_SCAN_ACTIVE while still associated to AP1; waiting for production auto-roam decision"
            );
            return;
        }

        if (station.wifiStationState()
                == WifiStationState.ASSOCIATED
                && W119Mac.equals(
                station.wifiSelectedBssid(),
                ap2.wifiMacAddress()
        )) {
            fail(
                    W1222AutoRoamFailure.BACKGROUND_SCAN_NOT_OBSERVED,
                    "Station reached AP2 before test observed ROAM_SCAN_ACTIVE; automatic roam occurred but association-preserving scan could not be validated"
            );
            return;
        }

        if (elapsedStageTicks()
                > BACKGROUND_SCAN_OBSERVE_TIMEOUT_TICKS) {
            fail(
                    W1222AutoRoamFailure.BACKGROUND_SCAN_NOT_OBSERVED,
                    "No production ROAM_SCAN_ACTIVE observed within "
                            + BACKGROUND_SCAN_OBSERVE_TIMEOUT_TICKS
                            + " ticks"
                            + " | background="
                            + station.wifiBackgroundRoamingEnabled()
                            + " pendingData="
                            + station.wifiPendingDataTransmissions()
                            + " diagnostic="
                            + diagnostic
            );
        }
    }

    private void waitAutoRoam(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2
    ) {
        if (elapsedStageTicks()
                % 20L == 0L) {
            ap1.sendWifiBeacon();
            ap2.sendWifiBeacon();
        }

        if (station.wifiSecurityState()
                == WifiSecurityState.FAILED) {
            fail(
                    W1222AutoRoamFailure.SECURITY_FAILED,
                    station.wifiSecurityDiagnostic()
            );
            return;
        }

        if (station.wifiStationState()
                == WifiStationState.ASSOCIATED
                && W119Mac.equals(
                station.wifiSelectedBssid(),
                ap2.wifiMacAddress()
        )) {
            if (!baselineIp.equals(
                    station.wifiIpAddress()
            )) {
                fail(
                        W1222AutoRoamFailure.IP_CHANGED_DURING_ROAM,
                        "Automatic Layer-2 roam changed station IPv4 "
                                + baselineIp
                                + " -> "
                                + station.wifiIpAddress()
                );
                return;
            }

            transition(
                    W1222AutoRoamStage.OLD_AP_CLEANUP,
                    "Automatic AP1 -> AP2 roam completed with IPv4 preserved; validating old-AP cleanup"
            );
            return;
        }

        if (station.wifiStationState()
                == WifiStationState.ASSOCIATED
                && !W119Mac.equals(
                station.wifiSelectedBssid(),
                ap1.wifiMacAddress()
        )
                && !W119Mac.equals(
                station.wifiSelectedBssid(),
                ap2.wifiMacAddress()
        )) {
            fail(
                    W1222AutoRoamFailure.AUTO_ROAM_WRONG_TARGET,
                    "Automatic roam selected unexpected BSSID "
                            + station.wifiSelectedBssid()
            );
            return;
        }

        if (elapsedStageTicks()
                > AUTO_ROAM_TIMEOUT_TICKS) {
            fail(
                    W1222AutoRoamFailure.AUTO_ROAM_TIMEOUT,
                    "Automatic roaming did not move AP1 -> AP2 within "
                            + AUTO_ROAM_TIMEOUT_TICKS
                            + " ticks"
                            + " | selected="
                            + station.wifiSelectedBssid()
                            + " diagnostic="
                            + station.wifiSecurityDiagnostic()
            );
        }
    }

    private void oldApCleanup(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2
    ) {
        boolean oldReleased =
                !containsStation(
                        ap1,
                        station
                );

        boolean newOwns =
                containsStation(
                        ap2,
                        station
                );

        if (oldReleased
                && newOwns) {
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
                        W1222AutoRoamFailure.POST_ROAM_DATA_START_FAILED,
                        "Associated DATA could not be queued after automatic roam"
                );
                return;
            }

            transition(
                    W1222AutoRoamStage.POST_ROAM_DATA_ACK,
                    "Old AP released STA and AP2 owns association; waiting for post-auto-roam DATA ACK"
            );
            return;
        }

        if (elapsedStageTicks()
                > OLD_AP_CLEANUP_TIMEOUT_TICKS) {
            if (!oldReleased) {
                fail(
                        W1222AutoRoamFailure.OLD_AP_CLEANUP_TIMEOUT,
                        "AP1 still contains roaming station after automatic AP2 association"
                                + " | AP1 stations="
                                + ap1.wifiAssociatedStations()
                );
            } else {
                fail(
                        W1222AutoRoamFailure.NEW_AP_ASSOCIATION_MISSING,
                        "AP2 does not contain the station after automatic roam"
                                + " | AP2 stations="
                                + ap2.wifiAssociatedStations()
                );
            }
        }
    }

    private void postRoamDataAck(
            NetworkDeviceBlockEntity station
    ) {
        long ackNow =
                ackRxCount(
                        station.wifiPacketTraceSnapshot()
                );

        if (ackNow
                > postRoamAckBaseline) {
            postRoamWorkflowStarted =
                    station.startWifiRawHttpWorkflow(
                            "www.vsia-net.com",
                            "/"
                    );

            if (!postRoamWorkflowStarted) {
                fail(
                        W1222AutoRoamFailure.POST_ROAM_HTTP_START_FAILED,
                        "Post-auto-roam RAW HTTP workflow could not start"
                );
                return;
            }

            transition(
                    W1222AutoRoamStage.POST_ROAM_HTTP,
                    "Post-auto-roam DATA/ACK passed; proving HTTP 200 through AP2"
            );
            return;
        }

        if (elapsedStageTicks()
                > DATA_ACK_TIMEOUT_TICKS) {
            fail(
                    W1222AutoRoamFailure.POST_ROAM_ACK_TIMEOUT,
                    "No new RX ACK observed after automatic roam to AP2"
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
        WifiRawIpWorkflowSnapshot workflow =
                station.wifiRawIpWorkflowSnapshot();

        if (workflow.state()
                == WifiRawIpWorkflowState.FAILED) {
            fail(
                    W1222AutoRoamFailure.POST_ROAM_HTTP_FAILED,
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
                        W1222AutoRoamFailure.POST_ROAM_HTTP_FAILED,
                        workflow.detail()
                );
                return;
            }

            if (!baselineIp.equals(
                    station.wifiIpAddress()
            )) {
                fail(
                        W1222AutoRoamFailure.IP_CHANGED_DURING_ROAM,
                        "Post-roam HTTP changed station IPv4 "
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
                        W1222AutoRoamFailure.INVALID_TOPOLOGY,
                        topologyFailure
                );
                return;
            }

            if (!bothDistributionPathsObserved(
                    ap1,
                    ap2
            )) {
                fail(
                        W1222AutoRoamFailure.DISTRIBUTION_PATH_NOT_OBSERVED,
                        "Both APs must show bidirectional DS traffic before W1.22 closure"
                );
                return;
            }

            transition(
                    W1222AutoRoamStage.COOLDOWN_STABILITY,
                    "HTTP 200 passed through AP2. Waiting "
                            + COOLDOWN_STABILITY_TICKS
                            + " ticks to validate cooldown/no ping-pong."
            );
            return;
        }

        if (elapsedStageTicks()
                > POST_ROAM_HTTP_TIMEOUT_TICKS) {
            fail(
                    W1222AutoRoamFailure.POST_ROAM_HTTP_FAILED,
                    "Post-roam RAW HTTP exceeded "
                            + POST_ROAM_HTTP_TIMEOUT_TICKS
                            + " ticks in "
                            + workflow.state()
                            + ": "
                            + workflow.detail()
            );
        }
    }

    private void cooldownStability(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2
    ) {
        if (elapsedStageTicks()
                % 20L == 0L) {
            ap1.sendWifiBeacon();
            ap2.sendWifiBeacon();
        }

        if (station.wifiStationState()
                != WifiStationState.ASSOCIATED
                || !W119Mac.equals(
                station.wifiSelectedBssid(),
                ap2.wifiMacAddress()
        )) {
            fail(
                    W1222AutoRoamFailure.PING_PONG_DETECTED,
                    "Station left AP2 during cooldown/stability window"
                            + " | state="
                            + station.wifiStationState()
                            + " selected="
                            + station.wifiSelectedBssid()
            );
            return;
        }

        if (!baselineIp.equals(
                station.wifiIpAddress()
        )) {
            fail(
                    W1222AutoRoamFailure.IP_CHANGED_DURING_ROAM,
                    "IPv4 changed during cooldown/stability window "
                            + baselineIp
                            + " -> "
                            + station.wifiIpAddress()
            );
            return;
        }

        if (containsStation(
                ap1,
                station
        )) {
            fail(
                    W1222AutoRoamFailure.PING_PONG_DETECTED,
                    "Old AP regained association state during cooldown/stability window"
            );
            return;
        }

        if (!containsStation(
                ap2,
                station
        )) {
            fail(
                    W1222AutoRoamFailure.NEW_AP_ASSOCIATION_MISSING,
                    "AP2 lost association state during cooldown/stability window"
            );
            return;
        }

        if (elapsedStageTicks()
                >= COOLDOWN_STABILITY_TICKS) {
            transition(
                    W1222AutoRoamStage.COMPLETE,
                    "PASS: automatic roaming AP1 "
                            + ap1.wifiMacAddress()
                            + " -> AP2 "
                            + ap2.wifiMacAddress()
                            + " | associated background scan observed"
                            + " | +6 dB policy satisfied"
                            + " | IPv4="
                            + baselineIp
                            + " preserved"
                            + " | old AP cleaned"
                            + " | DATA/ACK + HTTP 200 after roam"
                            + " | no ping-pong through cooldown"
                            + " | both AP DS paths observed"
            );
        }
    }

    private boolean bothDistributionPathsObserved(
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2
    ) {
        String ap1Bridge =
                safe(
                        ap1.w119BridgeStatus()
                );

        String ap2Bridge =
                safe(
                        ap2.w119BridgeStatus()
                );

        return metric(
                ap1Bridge,
                DS_TX
        ) > 0L
                && metric(
                ap1Bridge,
                DS_RX
        ) > 0L
                && metric(
                ap2Bridge,
                DS_TX
        ) > 0L
                && metric(
                ap2Bridge,
                DS_RX
        ) > 0L;
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

    private boolean containsStation(
            NetworkDeviceBlockEntity ap,
            NetworkDeviceBlockEntity station
    ) {
        return ap.wifiAssociatedStations()
                .stream()
                .anyMatch(
                        value ->
                                W119Mac.equals(
                                        value,
                                        station.wifiMacAddress()
                                )
                );
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
        return "Expected NetworkDevice STA at "
                + stationPos.toShortString()
                + ", NetworkDevice AP1 at "
                + ap1Pos.toShortString()
                + ", NetworkDevice AP2 at "
                + ap2Pos.toShortString()
                + ", NetworkSwitch at "
                + switchPos.toShortString()
                + ", ServerRack at "
                + serverPos.toShortString();
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
            W1222AutoRoamStage next,
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
            W1222AutoRoamFailure reason,
            String failureDetail
    ) {
        failure =
                reason == null
                        ? W1222AutoRoamFailure.INTERNAL_ERROR
                        : reason;

        stage =
                W1222AutoRoamStage.FAILED;

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
