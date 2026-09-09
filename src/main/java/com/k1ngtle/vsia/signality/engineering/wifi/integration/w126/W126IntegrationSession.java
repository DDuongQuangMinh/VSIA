package com.k1ngtle.vsia.signality.engineering.wifi.integration.w126;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiAccessCategory;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiContentionSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiNetworkRecord;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiSecurityState;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiStationState;
import com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119.W119Mac;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow.WifiRawIpWorkflowSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow.WifiRawIpWorkflowState;
import com.k1ngtle.vsia.signality.engineering.wifi.trace.WifiPacketDirection;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.network.NetworkKind;
import com.k1ngtle.vsia.signality.internet.network.NetworkProfile;
import com.k1ngtle.vsia.signality.internet.network.NetworkProfileRegistry;
import com.k1ngtle.vsia.signality.internet.provider.InternetProviderRegistry;
import com.k1ngtle.vsia.signality.internet.router.RtAc68uRouterBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class W126IntegrationSession {
    public static final String SSID = "VSIA-W126";
    public static final String PASSPHRASE = "vsia-w126-wpa2";

    private static final long SCAN_TIMEOUT = 700L;
    private static final long ASSOC_TIMEOUT = 900L;
    private static final long ACK_TIMEOUT = 220L;
    private static final long SERVICE_TIMEOUT = 1500L;
    private static final long QOS_TIMEOUT = 600L;
    private static final long ROAM_TIMEOUT = 800L;
    private static final long STABILITY_TICKS = 160L;
    private static final double ROAM_HYSTERESIS_DB = 6.0D;

    private static final Pattern DS_TX = Pattern.compile("(?:^|\\s)dsTx=(\\d+)");
    private static final Pattern DS_RX = Pattern.compile("(?:^|\\s)dsRx=(\\d+)");

    private final ServerLevel level;
    private final BlockPos sta1Pos;
    private final BlockPos sta2Pos;
    private final BlockPos ap1Pos;
    private final BlockPos ap2Pos;
    private final BlockPos switchPos;
    private final BlockPos serverPos;

    private W126Stage stage = W126Stage.SETUP;
    private W126Failure failure = W126Failure.NONE;
    private String detail = "Waiting for first tick";
    private long startTick = -1L;
    private long stageStartTick = -1L;

    private long sta1AckBaseline;
    private long sta2AckBaseline;
    private String sta1BaselineIp = "";

    private boolean sta1HttpStarted;
    private boolean sta2HttpStarted;
    private boolean postRoamHttpStarted;
    private boolean qosQueued;
    private boolean providerDomainMatches;

    private boolean sta2ScanStarted;
    private int sta1ScanAttempts;
    private int sta2ScanAttempts;
    private long sta1LastScanStartTick = -1L;
    private long sta2LastScanStartTick = -1L;

    private boolean sta1ConnectStarted;
    private boolean sta2ConnectStarted;
    private long sta1ConnectStartTick = -1L;
    private long sta2ConnectStartTick = -1L;
    private String sta2TargetSsid = "";
    private String sta2TargetBssid = "";

    public W126IntegrationSession(
            ServerLevel level,
            BlockPos sta1Pos,
            BlockPos sta2Pos,
            BlockPos ap1Pos,
            BlockPos ap2Pos,
            BlockPos switchPos,
            BlockPos serverPos
    ) {
        this.level = level;
        this.sta1Pos = sta1Pos.immutable();
        this.sta2Pos = sta2Pos.immutable();
        this.ap1Pos = ap1Pos.immutable();
        this.ap2Pos = ap2Pos.immutable();
        this.switchPos = switchPos.immutable();
        this.serverPos = serverPos.immutable();
    }

    public void tick() {
        if (finished()) return;

        try {
            if (startTick < 0L) {
                startTick = level.getGameTime();
                stageStartTick = startTick;
            }

            NetworkDeviceBlockEntity sta1 = device(sta1Pos);
            NetworkDeviceBlockEntity sta2 = device(sta2Pos);
            NetworkDeviceBlockEntity ap1 = device(ap1Pos);
            NetworkDeviceBlockEntity ap2 = device(ap2Pos);
            NetworkSwitchBlockEntity networkSwitch = networkSwitch();
            ServerRackBlockEntity server = server();

            if (sta1 == null || sta2 == null || ap1 == null || ap2 == null || networkSwitch == null || server == null) {
                fail(W126Failure.INVALID_TOPOLOGY, "Expected 2 STAs, 2 APs, NetworkSwitch and ServerRack");
                return;
            }

            switch (stage) {
                case SETUP -> setup(sta1, sta2, ap1, ap2, networkSwitch, server);
                case SCANNING -> scanning(sta1, sta2, ap1, ap2);
                case ASSOCIATING -> associating(sta1, sta2, ap1, ap2);
                case LINK_ACK -> linkAck(sta1, sta2);
                case SERVICE_INTEGRATION -> serviceIntegration(sta1, sta2);
                case QOS_INTEGRATION -> qosIntegration(sta1, sta2);
                case MOBILITY_INTEGRATION -> mobilityIntegration(sta1, ap1, ap2);
                case POST_MOBILITY_SERVICE -> postMobilityService(sta1);
                case SCALE_STABILITY -> scaleStability(sta1, sta2, ap1, ap2, networkSwitch, server);
                case COMPLETE, FAILED -> {
                }
            }
        } catch (Exception exception) {
            fail(W126Failure.INTERNAL_ERROR, exception.getClass().getSimpleName() + ": " + safe(exception.getMessage()));
        }
    }

    public boolean finished() {
        return stage == W126Stage.COMPLETE || stage == W126Stage.FAILED;
    }

    public boolean passed() {
        return stage == W126Stage.COMPLETE && failure == W126Failure.NONE;
    }

    public W126Snapshot snapshot() {
        NetworkDeviceBlockEntity sta1 = device(sta1Pos);
        NetworkDeviceBlockEntity sta2 = device(sta2Pos);
        NetworkDeviceBlockEntity ap1 = device(ap1Pos);
        NetworkDeviceBlockEntity ap2 = device(ap2Pos);

        WifiRawIpWorkflowSnapshot flow1 = sta1 == null ? null : sta1.wifiRawIpWorkflowSnapshot();
        WifiRawIpWorkflowSnapshot flow2 = sta2 == null ? null : sta2.wifiRawIpWorkflowSnapshot();
        WifiContentionSnapshot qos1 = sta1 == null ? emptyContention() : sta1.wifiContentionSnapshot();
        WifiContentionSnapshot qos2 = sta2 == null ? emptyContention() : sta2.wifiContentionSnapshot();

        String bridge1 = ap1 == null ? "" : ap1.w119BridgeStatus();
        String bridge2 = ap2 == null ? "" : ap2.w119BridgeStatus();
        String providerIp = InternetProviderRegistry.resolveA(InternetProviderRegistry.DEFAULT_WEB_DOMAIN).orElse("");

        return new W126Snapshot(
                stage,
                failure,
                finished(),
                passed(),
                detail,
                startTick < 0L ? 0L : Math.max(0L, level.getGameTime() - startTick),
                state(sta1),
                state(sta2),
                security(sta1),
                security(sta2),
                ip(sta1),
                ip(sta2),
                bssid(sta1),
                bssid(sta2),
                flow1 == null ? "N/A" : flow1.state().name(),
                flow2 == null ? "N/A" : flow2.state().name(),
                flow1 == null ? "" : flow1.detail(),
                flow2 == null ? "" : flow2.detail(),
                sta1 == null ? -1 : sta1.wifiPendingDataTransmissions(),
                sta2 == null ? -1 : sta2.wifiPendingDataTransmissions(),
                ackCount(sta1),
                ackCount(sta2),
                qos1.successes(),
                qos2.successes(),
                qos1.drops(),
                qos2.drops(),
                ap1 == null ? -1 : ap1.wifiAssociatedStations().size(),
                ap2 == null ? -1 : ap2.wifiAssociatedStations().size(),
                metric(bridge1, DS_TX),
                metric(bridge1, DS_RX),
                metric(bridge2, DS_TX),
                metric(bridge2, DS_RX),
                providerDomainMatches,
                providerIp
        );
    }

    private void setup(
            NetworkDeviceBlockEntity sta1,
            NetworkDeviceBlockEntity sta2,
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2,
            NetworkSwitchBlockEntity networkSwitch,
            ServerRackBlockEntity server
    ) {
        String topology = validateTopology(ap1, ap2, networkSwitch, server);
        if (!topology.isBlank()) {
            fail(W126Failure.INVALID_TOPOLOGY, topology);
            return;
        }

        NetworkProfile wifi5 = NetworkProfileRegistry.values()
                .stream()
                .filter(profile -> profile.kind() == NetworkKind.WIFI
                        && "wifi_5".equals(profile.id().getPath())
                        && "signality:wpa2".equals(profile.security()))
                .findFirst()
                .orElse(null);

        if (wifi5 == null) {
            fail(W126Failure.PROFILE_CONFIGURATION_FAILED, "Built-in wifi_5 WPA2 profile is missing");
            return;
        }

        boolean profiles = sta1.configureNetworkProfile(wifi5.id())
                && sta2.configureNetworkProfile(wifi5.id())
                && ap1.configureNetworkProfile(wifi5.id())
                && ap2.configureNetworkProfile(wifi5.id());

        if (!profiles) {
            fail(W126Failure.PROFILE_CONFIGURATION_FAILED, "One or more endpoints rejected wifi_5/WPA2");
            return;
        }

        clear(sta1);
        clear(sta2);
        clear(ap1);
        clear(ap2);

        if (!ap1.configureWifiAccessPoint(SSID, PASSPHRASE)
                || !ap2.configureWifiAccessPoint(SSID, PASSPHRASE)) {
            fail(W126Failure.AP_CONFIGURATION_FAILED, "AP configuration failed");
            return;
        }

        if (!sta1.configureWifiStation(PASSPHRASE)
                || !sta2.configureWifiStation(PASSPHRASE)) {
            fail(W126Failure.STATION_CONFIGURATION_FAILED, "STA configuration failed");
            return;
        }

        sta1.setWifiBackgroundRoamingEnabled(false);
        sta2.setWifiBackgroundRoamingEnabled(false);

        ap1.sendWifiBeacon();

        if (!sta1.scanWifi()) {
            fail(W126Failure.SCAN_START_FAILED, "STA1 scan failed to start");
            return;
        }

        sta1ScanAttempts = 1;
        sta1LastScanStartTick = level.getGameTime();
        sta2ScanStarted = false;
        sta2ScanAttempts = 0;
        sta2LastScanStartTick = -1L;

        transition(
                W126Stage.SCANNING,
                "STA1 scan started; STA2 will start 3 ticks later. AP beacons are interleaved to avoid synchronized co-channel scan loss."
        );
    }

    private void scanning(
            NetworkDeviceBlockEntity sta1,
            NetworkDeviceBlockEntity sta2,
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2
    ) {
        long elapsed =
                elapsedStage();

        /*
         * Both W1.26 APs intentionally share the same Wi-Fi 5 channel.
         * Do not inject both engineering beacons on the same server tick:
         * alternate them so a station's 120 ms scan dwell sees a beacon
         * without the test harness creating artificial synchronized
         * co-channel collisions.
         */
        if ((elapsed & 1L) == 0L) {
            ap1.sendWifiBeacon();
        } else {
            ap2.sendWifiBeacon();
        }

        /*
         * Stagger the second station's scan by three ticks.  The eventual
         * scale test is still concurrent at DATA/DHCP/HTTP/QoS; this merely
         * prevents the scan harness from phase-locking both receivers.
         */
        if (!sta2ScanStarted
                && elapsed >= 3L) {
            if (!sta2.scanWifi()) {
                fail(
                        W126Failure.SCAN_START_FAILED,
                        "STA2 staggered scan failed to start"
                );
                return;
            }

            sta2ScanStarted =
                    true;

            sta2ScanAttempts =
                    1;

            sta2LastScanStartTick =
                    level.getGameTime();
        }

        WifiNetworkRecord sta1Ap1 =
                discovered(
                        sta1,
                        ap1
                );

        WifiNetworkRecord sta2Ap2 =
                discovered(
                        sta2,
                        ap2
                );

        boolean sta1Complete =
                sta1.wifiSecurityDiagnostic()
                        .startsWith(
                                "SCAN_COMPLETE_APS_"
                        );

        boolean sta2Complete =
                sta2ScanStarted
                        && sta2.wifiSecurityDiagnostic()
                        .startsWith(
                                "SCAN_COMPLETE_APS_"
                        );

        /*
         * A completed scan with no required BSSID is retried a bounded
         * number of times.  This handles stochastic RF loss while still
         * failing cleanly if the BSS is genuinely unreachable.
         */
        if (sta1Complete
                && sta1Ap1 == null
                && sta1ScanAttempts < 4
                && level.getGameTime()
                - sta1LastScanStartTick >= 8L) {
            if (sta1.scanWifi()) {
                sta1ScanAttempts++;

                sta1LastScanStartTick =
                        level.getGameTime();

                detail =
                        "STA1 retry scan "
                                + sta1ScanAttempts
                                + "/4; AP beacons remain interleaved";
            }
        }

        if (sta2Complete
                && sta2Ap2 == null
                && sta2ScanAttempts < 4
                && level.getGameTime()
                - sta2LastScanStartTick >= 8L) {
            if (sta2.scanWifi()) {
                sta2ScanAttempts++;

                sta2LastScanStartTick =
                        level.getGameTime();

                detail =
                        "STA2 retry scan "
                                + sta2ScanAttempts
                                + "/4; AP beacons remain interleaved";
            }
        }

        if (sta1Ap1 != null
                && sta2Ap2 != null
                && sta1Complete
                && sta2Complete) {
            sta2TargetSsid =
                    sta2Ap2.ssid();

            sta2TargetBssid =
                    sta2Ap2.bssid();

            if (!sta1.connectWifiBssid(
                    sta1Ap1.ssid(),
                    sta1Ap1.bssid()
            )) {
                fail(
                        W126Failure.CONNECT_START_FAILED,
                        "STA1 exact-BSSID connection start failed"
                );
                return;
            }

            sta1ConnectStarted =
                    true;

            sta2ConnectStarted =
                    false;

            sta1ConnectStartTick =
                    level.getGameTime();

            sta2ConnectStartTick =
                    -1L;

            transition(
                    W126Stage.ASSOCIATING,
                    "Both staggered scans completed | STA1 AP1 attempts="
                            + sta1ScanAttempts
                            + " | STA2 AP2 attempts="
                            + sta2ScanAttempts
                            + " | STA1 WPA2 association started first; STA2 waits for STA1 SECURED + management queue drain"
            );
            return;
        }

        if (elapsed > SCAN_TIMEOUT) {
            fail(
                    W126Failure.SCAN_TIMEOUT,
                    "Two-station scan timeout"
                            + " | STA1 target="
                            + (sta1Ap1 != null)
                            + " complete="
                            + sta1Complete
                            + " attempts="
                            + sta1ScanAttempts
                            + " diag="
                            + sta1.wifiSecurityDiagnostic()
                            + " | STA2 target="
                            + (sta2Ap2 != null)
                            + " complete="
                            + sta2Complete
                            + " attempts="
                            + sta2ScanAttempts
                            + " diag="
                            + sta2.wifiSecurityDiagnostic()
            );
        }
    }

    private void associating(
            NetworkDeviceBlockEntity sta1,
            NetworkDeviceBlockEntity sta2,
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2
    ) {
        if (!sta1ConnectStarted) {
            fail(
                    W126Failure.INTERNAL_ERROR,
                    "ASSOCIATING entered before STA1 connection was started"
            );
            return;
        }

        if (sta1.wifiSecurityState()
                == WifiSecurityState.FAILED) {
            fail(
                    W126Failure.SECURITY_FAILED,
                    "STA1 security failure | "
                            + sta1.wifiSecurityDiagnostic()
            );
            return;
        }

        /*
         * W1.26 scale begins after link establishment.  The original
         * closure harness launched both AUTH exchanges on the same tick,
         * on the same 5 GHz channel, which can phase-lock two independent
         * management exchanges.  Complete STA1's WPA2 management/security
         * exchange first, then start STA2.  DATA/DHCP/HTTP/QoS remain
         * concurrent later in the test.
         */
        boolean sta1Ready =
                sta1.wifiStationState()
                        == WifiStationState.ASSOCIATED
                        && sta1.wifiSecurityState()
                        == WifiSecurityState.SECURED
                        && W119Mac.equals(
                        sta1.wifiSelectedBssid(),
                        ap1.wifiMacAddress()
                )
                        && contains(
                        ap1,
                        sta1
                )
                        && sta1.wifiPendingDataTransmissions()
                        == 0;

        if (!sta2ConnectStarted) {
            if (sta1Ready) {
                WifiNetworkRecord target =
                        discovered(
                                sta2,
                                ap2
                        );

                String targetSsid =
                        target != null
                                ? target.ssid()
                                : sta2TargetSsid;

                String targetBssid =
                        target != null
                                ? target.bssid()
                                : sta2TargetBssid;

                if (targetSsid == null
                        || targetSsid.isBlank()
                        || targetBssid == null
                        || targetBssid.isBlank()) {
                    fail(
                            W126Failure.CONNECT_START_FAILED,
                            "STA2 AP2 scan record disappeared before its serialized association could start"
                    );
                    return;
                }

                if (!sta2.connectWifiBssid(
                        targetSsid,
                        targetBssid
                )) {
                    fail(
                            W126Failure.CONNECT_START_FAILED,
                            "STA2 exact-BSSID connection start failed after STA1 became SECURED"
                    );
                    return;
                }

                sta2ConnectStarted =
                        true;

                sta2ConnectStartTick =
                        level.getGameTime();

                detail =
                        "STA1 ASSOCIATED/SECURED on AP1; STA2 WPA2 association now started on AP2";

                return;
            }

            if (level.getGameTime()
                    - sta1ConnectStartTick > 420L) {
                fail(
                        W126Failure.ASSOCIATION_TIMEOUT,
                        "STA1 serialized association timeout"
                                + " | state="
                                + sta1.wifiStationState()
                                + "/"
                                + sta1.wifiSecurityState()
                                + " | selectedSecurity="
                                + sta1.wifiSelectedSecurity()
                                + " | diagnostic="
                                + sta1.wifiSecurityDiagnostic()
                                + " | pending="
                                + sta1.wifiPendingDataTransmissions()
                );
            }

            return;
        }

        if (sta2.wifiSecurityState()
                == WifiSecurityState.FAILED) {
            fail(
                    W126Failure.SECURITY_FAILED,
                    "STA2 security failure | "
                            + sta2.wifiSecurityDiagnostic()
            );
            return;
        }

        boolean sta2Ready =
                sta2.wifiStationState()
                        == WifiStationState.ASSOCIATED
                        && sta2.wifiSecurityState()
                        == WifiSecurityState.SECURED
                        && W119Mac.equals(
                        sta2.wifiSelectedBssid(),
                        ap2.wifiMacAddress()
                )
                        && contains(
                        ap2,
                        sta2
                )
                        && sta2.wifiPendingDataTransmissions()
                        == 0;

        if (sta1Ready
                && sta2Ready) {
            sta1AckBaseline =
                    ackCount(
                            sta1
                    );

            sta2AckBaseline =
                    ackCount(
                            sta2
                    );

            if (!sta1.sendWifiEngineeringAssociatedData(
                    512
            )
                    || !sta2.sendWifiEngineeringAssociatedData(
                    512
            )) {
                fail(
                        W126Failure.LINK_ACK_TIMEOUT,
                        "Initial associated DATA could not be queued"
                );
                return;
            }

            transition(
                    W126Stage.LINK_ACK,
                    "Both serialized WPA2 associations are ASSOCIATED/SECURED; concurrent DATA/ACK started"
            );
            return;
        }

        if (level.getGameTime()
                - sta2ConnectStartTick > 420L
                || elapsedStage()
                > ASSOC_TIMEOUT) {
            fail(
                    W126Failure.ASSOCIATION_TIMEOUT,
                    "Serialized association timeout"
                            + " | STA1="
                            + sta1.wifiStationState()
                            + "/"
                            + sta1.wifiSecurityState()
                            + " security="
                            + sta1.wifiSelectedSecurity()
                            + " diag="
                            + sta1.wifiSecurityDiagnostic()
                            + " pending="
                            + sta1.wifiPendingDataTransmissions()
                            + " | STA2="
                            + sta2.wifiStationState()
                            + "/"
                            + sta2.wifiSecurityState()
                            + " security="
                            + sta2.wifiSelectedSecurity()
                            + " diag="
                            + sta2.wifiSecurityDiagnostic()
                            + " pending="
                            + sta2.wifiPendingDataTransmissions()
            );
        }
    }

    private void linkAck(
            NetworkDeviceBlockEntity sta1,
            NetworkDeviceBlockEntity sta2
    ) {
        boolean acked = ackCount(sta1) > sta1AckBaseline
                && ackCount(sta2) > sta2AckBaseline;

        if (acked && sta1.wifiPendingDataTransmissions() == 0 && sta2.wifiPendingDataTransmissions() == 0) {
            sta1HttpStarted = sta1.startWifiRawHttpWorkflow(InternetProviderRegistry.DEFAULT_WEB_DOMAIN, "/");
            sta2HttpStarted = sta2.startWifiRawHttpWorkflow(InternetProviderRegistry.DEFAULT_WEB_DOMAIN, "/");

            if (!sta1HttpStarted || !sta2HttpStarted) {
                fail(W126Failure.SERVICE_START_FAILED, "Concurrent RAW HTTP workflows failed to start");
                return;
            }

            transition(W126Stage.SERVICE_INTEGRATION, "Both DATA/ACK paths passed; concurrent DHCP/ARP/DNS/TCP/HTTP started");
            return;
        }

        if (elapsedStage() > ACK_TIMEOUT) {
            fail(W126Failure.LINK_ACK_TIMEOUT, "Both independent ACKs/queue drains were not observed");
        }
    }

    private void serviceIntegration(
            NetworkDeviceBlockEntity sta1,
            NetworkDeviceBlockEntity sta2
    ) {
        WifiRawIpWorkflowSnapshot one = sta1.wifiRawIpWorkflowSnapshot();
        WifiRawIpWorkflowSnapshot two = sta2.wifiRawIpWorkflowSnapshot();

        if (one.state() == WifiRawIpWorkflowState.FAILED || two.state() == WifiRawIpWorkflowState.FAILED) {
            fail(W126Failure.SERVICE_FAILED, "Service failure | STA1=" + one.detail() + " | STA2=" + two.detail());
            return;
        }

        if (one.state() == WifiRawIpWorkflowState.COMPLETE && two.state() == WifiRawIpWorkflowState.COMPLETE) {
            if (!one.detail().contains("HTTP 200") || !two.detail().contains("HTTP 200")) {
                fail(W126Failure.SERVICE_FAILED, "Both workflows must finish HTTP 200");
                return;
            }

            if (!usableIpv4(sta1.wifiIpAddress())
                    || !usableIpv4(sta2.wifiIpAddress())
                    || sta1.wifiIpAddress().equals(sta2.wifiIpAddress())) {
                fail(
                        W126Failure.DUPLICATE_IPV4,
                        "DHCP scale invariant failed | STA1=" + sta1.wifiIpAddress() + " STA2=" + sta2.wifiIpAddress()
                );
                return;
            }

            String providerIp = InternetProviderRegistry.resolveA(InternetProviderRegistry.DEFAULT_WEB_DOMAIN).orElse("");
            providerDomainMatches = !providerIp.isBlank()
                    && one.detail().contains(providerIp)
                    && two.detail().contains(providerIp);

            if (!providerDomainMatches) {
                fail(
                        W126Failure.PROVIDER_DOMAIN_MISMATCH,
                        "ISP1 registry says " + InternetProviderRegistry.DEFAULT_WEB_DOMAIN + " -> " + providerIp + " but workflow details disagree"
                );
                return;
            }

            sta1BaselineIp = sta1.wifiIpAddress();
            sta1.clearWifiContentionMetrics();
            sta2.clearWifiContentionMetrics();

            transition(W126Stage.QOS_INTEGRATION, "Concurrent HTTP 200 passed with unique DHCP leases and ISP1 domain agreement");
            return;
        }

        if (elapsedStage() > SERVICE_TIMEOUT) {
            fail(
                    W126Failure.SERVICE_FAILED,
                    "Concurrent service integration timeout | STA1=" + one.state() + " STA2=" + two.state()
            );
        }
    }

    private void qosIntegration(
            NetworkDeviceBlockEntity sta1,
            NetworkDeviceBlockEntity sta2
    ) {
        if (!qosQueued) {
            boolean all = true;

            for (int index = 0; index < 4; index++) {
                all &= sta1.sendWifiEngineeringQosData(512, WifiAccessCategory.VOICE);
                all &= sta1.sendWifiEngineeringQosData(512, WifiAccessCategory.BEST_EFFORT);
                all &= sta2.sendWifiEngineeringQosData(512, WifiAccessCategory.VIDEO);
                all &= sta2.sendWifiEngineeringQosData(512, WifiAccessCategory.BACKGROUND);
            }

            if (!all) {
                fail(W126Failure.QOS_QUEUE_FAILED, "One or more QoS frames were rejected");
                return;
            }

            qosQueued = true;
            detail = "Mixed QoS queued across both stations; waiting for drain";
            return;
        }

        if (sta1.wifiPendingDataTransmissions() == 0 && sta2.wifiPendingDataTransmissions() == 0) {
            WifiContentionSnapshot one = sta1.wifiContentionSnapshot();
            WifiContentionSnapshot two = sta2.wifiContentionSnapshot();

            if (one.successes() < 8L || two.successes() < 8L || one.drops() > 0L || two.drops() > 0L) {
                fail(
                        W126Failure.QOS_DELIVERY_FAILED,
                        "QoS scale failure | STA1=" + one.compact() + " | STA2=" + two.compact()
                );
                return;
            }

            sta1.setWifiBackgroundRoamingEnabled(true);
            transition(W126Stage.MOBILITY_INTEGRATION, "Concurrent QoS passed; automatic mobility enabled for STA1");
            return;
        }

        if (elapsedStage() > QOS_TIMEOUT) {
            fail(W126Failure.QOS_DELIVERY_FAILED, "QoS queues did not drain");
        }
    }

    private void mobilityIntegration(
            NetworkDeviceBlockEntity sta1,
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2
    ) {
        if (W119Mac.equals(sta1.wifiSelectedBssid(), ap2.wifiMacAddress())
                && sta1.wifiStationState() == WifiStationState.ASSOCIATED) {
            if (!sta1BaselineIp.equals(sta1.wifiIpAddress())) {
                fail(
                        W126Failure.IPV4_CHANGED_DURING_MOBILITY,
                        "STA1 IPv4 changed " + sta1BaselineIp + " -> " + sta1.wifiIpAddress()
                );
                return;
            }

            if (sta1.wifiPendingDataTransmissions() != 0) {
                detail = "Roam complete; waiting for MAC queue drain before post-mobility HTTP";
                return;
            }

            postRoamHttpStarted = sta1.startWifiRawHttpWorkflow(InternetProviderRegistry.DEFAULT_WEB_DOMAIN, "/");

            if (!postRoamHttpStarted) {
                fail(W126Failure.POST_MOBILITY_SERVICE_FAILED, "Post-roam HTTP workflow could not start");
                return;
            }

            transition(
                    W126Stage.POST_MOBILITY_SERVICE,
                    "Automatic AP1 -> AP2 handoff completed with IPv4 preserved; repeating Internet service"
            );
            return;
        }

        double ap1Snr = sta1.discoveredWifiNetworkSnrDb(ap1.wifiMacAddress());
        double ap2Snr = sta1.discoveredWifiNetworkSnrDb(ap2.wifiMacAddress());

        if (elapsedStage() > 180L
                && Double.isFinite(ap1Snr)
                && Double.isFinite(ap2Snr)
                && ap2Snr < ap1Snr + ROAM_HYSTERESIS_DB) {
            fail(
                    W126Failure.ROAM_POLICY_NOT_MET,
                    String.format(
                            java.util.Locale.ROOT,
                            "AP2 must be at least +%.1f dB stronger | AP1=%.1f AP2=%.1f",
                            ROAM_HYSTERESIS_DB,
                            ap1Snr,
                            ap2Snr
                    )
            );
            return;
        }

        if (elapsedStage() > ROAM_TIMEOUT) {
            fail(
                    W126Failure.ROAM_TIMEOUT,
                    "Automatic mobility timeout | selected=" + sta1.wifiSelectedBssid() + " diagnostic=" + sta1.wifiSecurityDiagnostic()
            );
        }
    }

    private void postMobilityService(
            NetworkDeviceBlockEntity sta1
    ) {
        WifiRawIpWorkflowSnapshot workflow = sta1.wifiRawIpWorkflowSnapshot();

        if (workflow.state() == WifiRawIpWorkflowState.FAILED) {
            fail(W126Failure.POST_MOBILITY_SERVICE_FAILED, workflow.detail());
            return;
        }

        if (workflow.state() == WifiRawIpWorkflowState.COMPLETE) {
            if (!workflow.detail().contains("HTTP 200") || !sta1BaselineIp.equals(sta1.wifiIpAddress())) {
                fail(
                        W126Failure.POST_MOBILITY_SERVICE_FAILED,
                        "Post-mobility HTTP/IPv4 invariant failed | " + workflow.detail()
                );
                return;
            }

            transition(
                    W126Stage.SCALE_STABILITY,
                    "HTTP 200 passed after handoff; entering final W1.26.4 stability window"
            );
            return;
        }

        if (elapsedStage() > SERVICE_TIMEOUT) {
            fail(W126Failure.POST_MOBILITY_SERVICE_FAILED, "Post-mobility service timeout");
        }
    }

    private void scaleStability(
            NetworkDeviceBlockEntity sta1,
            NetworkDeviceBlockEntity sta2,
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2,
            NetworkSwitchBlockEntity networkSwitch,
            ServerRackBlockEntity server
    ) {
        if (sta1.wifiStationState() != WifiStationState.ASSOCIATED
                || sta2.wifiStationState() != WifiStationState.ASSOCIATED
                || !W119Mac.equals(sta1.wifiSelectedBssid(), ap2.wifiMacAddress())
                || !W119Mac.equals(sta2.wifiSelectedBssid(), ap2.wifiMacAddress())) {
            fail(W126Failure.SCALE_STABILITY_FAILED, "Station ownership changed during final scale window");
            return;
        }

        if (!sta1BaselineIp.equals(sta1.wifiIpAddress())
                || sta1.wifiIpAddress().equals(sta2.wifiIpAddress())) {
            fail(W126Failure.SCALE_STABILITY_FAILED, "IP stability/uniqueness failed during final window");
            return;
        }

        if (sta1.wifiPendingDataTransmissions() != 0 || sta2.wifiPendingDataTransmissions() != 0) {
            detail = "Waiting for final queues to drain";
            return;
        }

        if (elapsedStage() >= STABILITY_TICKS) {
            String topology = validateTopology(ap1, ap2, networkSwitch, server);
            if (!topology.isBlank()) {
                fail(W126Failure.INVALID_TOPOLOGY, topology);
                return;
            }

            String bridge1 = ap1.w119BridgeStatus();
            String bridge2 = ap2.w119BridgeStatus();

            if (metric(bridge1, DS_TX) <= 0L
                    || metric(bridge1, DS_RX) <= 0L
                    || metric(bridge2, DS_TX) <= 0L
                    || metric(bridge2, DS_RX) <= 0L) {
                fail(W126Failure.DISTRIBUTION_PATH_NOT_OBSERVED, "Both AP distribution paths must show bidirectional traffic");
                return;
            }

            transition(
                    W126Stage.COMPLETE,
                    "PASS: Wi-Fi 1 / W1.26.4 — Wi-Fi Integration & Scale Validation | 2 WPA2 STAs | unique DHCP leases | concurrent HTTP 200 | ISP1 domain agreement | concurrent QoS | automatic AP handoff with IPv4 preserved | HTTP 200 after mobility | both AP DS paths"
            );
        }
    }

    private String validateTopology(
            NetworkDeviceBlockEntity ap1,
            NetworkDeviceBlockEntity ap2,
            NetworkSwitchBlockEntity networkSwitch,
            ServerRackBlockEntity server
    ) {
        java.util.HashSet<BlockPos> positions = new java.util.HashSet<>();
        positions.add(sta1Pos);
        positions.add(sta2Pos);
        positions.add(ap1Pos);
        positions.add(ap2Pos);
        positions.add(switchPos);
        positions.add(serverPos);

        if (positions.size() != 6) return "All six W1.26 targets must be distinct blocks";
        if (!switchConnectedToAp(networkSwitch, ap1Pos, ap1)) return "AP1 is not attached to the supplied Network Switch";
        if (!switchConnectedToAp(networkSwitch, ap2Pos, ap2)) return "AP2 is not attached to the supplied Network Switch";
        if (!networkSwitch.getConnectedDevices().contains(serverPos)) return "Server Rack is not directly attached to the supplied Network Switch";
        if (!server.dhcpEnabled() || !server.dnsEnabled() || !server.httpEnabled()) return "Server Rack must have DHCP, DNS and HTTP enabled";
        return "";
    }

    private boolean switchConnectedToAp(
            NetworkSwitchBlockEntity networkSwitch,
            BlockPos apPos,
            NetworkDeviceBlockEntity ap
    ) {
        if (networkSwitch.getConnectedDevices().contains(apPos)) return true;

        if (ap instanceof RtAc68uRouterBlockEntity router) {
            return !router.w121InterfaceForPeer(switchPos).isBlank();
        }

        return false;
    }

    private void clear(NetworkDeviceBlockEntity device) {
        device.clearWifiPacketTrace();
        device.clearWifiContentionMetrics();
        device.clearWifiIpMetrics();
        device.clearWifiTcpLive();
    }

    private WifiNetworkRecord discovered(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap
    ) {
        return station.discoveredWifiNetworks()
                .stream()
                .filter(value -> value != null
                        && SSID.equals(value.ssid())
                        && W119Mac.equals(value.bssid(), ap.wifiMacAddress()))
                .findFirst()
                .orElse(null);
    }

    private boolean contains(
            NetworkDeviceBlockEntity ap,
            NetworkDeviceBlockEntity station
    ) {
        return ap.wifiAssociatedStations()
                .stream()
                .anyMatch(value -> W119Mac.equals(value, station.wifiMacAddress()));
    }

    private long ackCount(NetworkDeviceBlockEntity station) {
        if (station == null) return 0L;

        return station.wifiPacketTraceSnapshot()
                .stream()
                .filter(event -> event.direction() == WifiPacketDirection.RX
                        && "CONTROL".equalsIgnoreCase(event.frameType())
                        && event.subtype() == 13)
                .count();
    }

    private NetworkDeviceBlockEntity device(BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entity instanceof NetworkDeviceBlockEntity device ? device : null;
    }

    private NetworkSwitchBlockEntity networkSwitch() {
        BlockEntity entity = level.getBlockEntity(switchPos);
        return entity instanceof NetworkSwitchBlockEntity networkSwitch ? networkSwitch : null;
    }

    private ServerRackBlockEntity server() {
        BlockEntity entity = level.getBlockEntity(serverPos);
        return entity instanceof ServerRackBlockEntity server ? server : null;
    }

    private long elapsedStage() {
        return Math.max(0L, level.getGameTime() - stageStartTick);
    }

    private void transition(W126Stage next, String nextDetail) {
        stage = next;
        stageStartTick = level.getGameTime();
        detail = safe(nextDetail);
    }

    private void fail(W126Failure reason, String failureDetail) {
        failure = reason;
        stage = W126Stage.FAILED;
        detail = safe(failureDetail);
    }

    private boolean usableIpv4(String value) {
        if (value == null || value.isBlank() || "0.0.0.0".equals(value) || "255.255.255.255".equals(value)) return false;

        String[] parts = value.split("\\.");
        if (parts.length != 4) return false;

        try {
            for (String part : parts) {
                int octet = Integer.parseInt(part);
                if (octet < 0 || octet > 255) return false;
            }
        } catch (NumberFormatException exception) {
            return false;
        }

        return true;
    }

    private long metric(String value, Pattern pattern) {
        if (value == null || value.isBlank()) return 0L;
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : 0L;
    }

    private String state(NetworkDeviceBlockEntity value) {
        return value == null ? "MISSING" : value.wifiStationState().name();
    }

    private String security(NetworkDeviceBlockEntity value) {
        return value == null ? "MISSING" : value.wifiSecurityState().name();
    }

    private String ip(NetworkDeviceBlockEntity value) {
        return value == null ? "" : safe(value.wifiIpAddress());
    }

    private String bssid(NetworkDeviceBlockEntity value) {
        return value == null ? "" : safe(value.wifiSelectedBssid());
    }

    private WifiContentionSnapshot emptyContention() {
        return new WifiContentionSnapshot(
                0,
                0,
                0,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0,
                0,
                0,
                0
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
