package com.k1ngtle.vsia.signality.engineering.wifi.integration.w123;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiContentionSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiNetworkRecord;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiSecurityState;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiStationState;
import com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119.W119Mac;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringProbe;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSnapshot;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class W123ContentionSession {
    public static final String SSID =
            "VSIA-W123-CONTENTION";

    private static final int MODERATE_FRAMES =
            32;

    private static final int SATURATION_FRAMES =
            96;

    private static final int PAYLOAD_BYTES =
            1024;

    private static final int EXPECTED_QUEUE_CAPACITY =
            64;

    private static final long SCAN_TIMEOUT_TICKS =
            240L;

    private static final long CONNECT_TIMEOUT_TICKS =
            240L;

    private static final long DRAIN_TIMEOUT_TICKS =
            800L;

    private final ServerLevel level;
    private final BlockPos stationPos;
    private final BlockPos apPos;

    private W123ClosureStage stage =
            W123ClosureStage.SETUP;

    private W123ClosureFailure failure =
            W123ClosureFailure.NONE;

    private String detail =
            "Waiting for first server tick";

    private long startTick =
            -1L;

    private long stageStartTick =
            -1L;

    private int acceptedModerate;
    private int acceptedSaturation;

    public W123ContentionSession(
            ServerLevel level,
            BlockPos stationPos,
            BlockPos apPos
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

        this.apPos =
                apPos.immutable();
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

            NetworkDeviceBlockEntity ap =
                    ap();

            if (station == null
                    || ap == null
                    || stationPos.equals(
                    apPos
            )) {
                fail(
                        W123ClosureFailure.INVALID_TOPOLOGY,
                        "Expected two distinct Wi-Fi NetworkDeviceBlockEntity targets | STA="
                                + stationPos.toShortString()
                                + " AP="
                                + apPos.toShortString()
                );
                return;
            }

            switch (stage) {
                case SETUP ->
                        setup(
                                station,
                                ap
                        );

                case INITIAL_SCAN ->
                        initialScan(
                                station,
                                ap
                        );

                case CONNECTING ->
                        connecting(
                                station,
                                ap
                        );

                case MODERATE_BURST ->
                        moderateBurst(
                                station
                        );

                case WAIT_MODERATE_DRAIN ->
                        waitModerateDrain(
                                station
                        );

                case SATURATION_BURST ->
                        saturationBurst(
                                station
                        );

                case WAIT_SATURATION_DRAIN ->
                        waitSaturationDrain(
                                station
                        );

                case COMPLETE,
                     FAILED -> {
                }
            }
        } catch (Exception exception) {
            fail(
                    W123ClosureFailure.INTERNAL_ERROR,
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
                == W123ClosureStage.COMPLETE
                || stage
                == W123ClosureStage.FAILED;
    }

    public boolean passed() {
        return stage
                == W123ClosureStage.COMPLETE
                && failure
                == W123ClosureFailure.NONE;
    }

    public W123ContentionSnapshot snapshot() {
        NetworkDeviceBlockEntity station =
                station();

        NetworkDeviceBlockEntity ap =
                ap();

        WifiContentionSnapshot contention =
                station == null
                        ? emptyContention()
                        : station.wifiContentionSnapshot();

        WifiEngineeringSnapshot engineering =
                station == null
                        ? null
                        : WifiEngineeringProbe.capture(
                        station
                );

        long now =
                level.getGameTime();

        return new W123ContentionSnapshot(
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
                        station.wifiSelectedBssid()
                ),
                station == null
                        ? -1
                        : station.wifiPendingDataTransmissions(),
                acceptedModerate,
                acceptedSaturation,
                contention.queueDepth(),
                contention.queueCapacity(),
                contention.queuePeak(),
                contention.enqueued(),
                contention.attempts(),
                contention.successes(),
                contention.retries(),
                contention.drops(),
                contention.deferrals(),
                contention.cwVoice(),
                contention.cwVideo(),
                contention.cwBestEffort(),
                contention.cwBackground(),
                engineering == null
                        ? Double.NaN
                        : engineering.receivedPowerDbm(),
                engineering == null
                        ? Double.NaN
                        : engineering.snrDb(),
                engineering == null
                        ? Double.NaN
                        : engineering.correctedSinrDb(),
                station != null
                        && station.usesDetailedPropagationModel(),
                station == null
                        ? 0L
                        : station.wifiPacketTraceSnapshot()
                        .size(),
                ap == null
                        ? 0L
                        : ap.wifiPacketTraceSnapshot()
                        .size()
        );
    }

    private void setup(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap
    ) {
        station.clearWifiPacketTrace();
        ap.clearWifiPacketTrace();

        station.clearWifiContentionMetrics();
        ap.clearWifiContentionMetrics();

        boolean apConfigured =
                ap.configureWifiAccessPoint(
                        SSID,
                        ""
                );

        if (!apConfigured) {
            fail(
                    W123ClosureFailure.AP_CONFIGURATION_FAILED,
                    "AP rejected Wi-Fi access-point configuration"
            );
            return;
        }

        boolean stationConfigured =
                station.configureWifiStation(
                        ""
                );

        if (!stationConfigured) {
            fail(
                    W123ClosureFailure.STATION_CONFIGURATION_FAILED,
                    "STA rejected Wi-Fi station configuration"
            );
            return;
        }

        station.setWifiBackgroundRoamingEnabled(
                false
        );

        ap.sendWifiBeacon();

        if (!station.scanWifi()) {
            fail(
                    W123ClosureFailure.SCAN_START_FAILED,
                    "STA could not start initial scan"
            );
            return;
        }

        transition(
                W123ClosureStage.INITIAL_SCAN,
                "W1.23 contention lab configured; scanning for "
                        + SSID
        );
    }

    private void initialScan(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap
    ) {
        long elapsed =
                elapsedStageTicks();

        if (elapsed % 20L == 0L) {
            ap.sendWifiBeacon();
        }

        WifiNetworkRecord network =
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
                                                ap.wifiMacAddress()
                                        )
                        )
                        .findFirst()
                        .orElse(
                                null
                        );

        boolean scanComplete =
                station.wifiSecurityDiagnostic()
                        .startsWith(
                                "SCAN_COMPLETE_APS_"
                        );

        if (network != null
                && scanComplete) {
            boolean started =
                    station.connectWifiBssid(
                            network.ssid(),
                            network.bssid()
                    );

            if (!started) {
                fail(
                        W123ClosureFailure.CONNECT_START_FAILED,
                        "Exact-BSSID connection to contention AP was rejected"
                );
                return;
            }

            transition(
                    W123ClosureStage.CONNECTING,
                    "AP discovered; waiting for association/security before contention bursts"
            );
            return;
        }

        if (elapsed > SCAN_TIMEOUT_TICKS) {
            fail(
                    W123ClosureFailure.SCAN_TIMEOUT,
                    "Scan timeout | found="
                            + (network != null)
                            + " diagnostic="
                            + station.wifiSecurityDiagnostic()
            );
        }
    }

    private void connecting(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap
    ) {
        if (station.wifiSecurityState()
                == WifiSecurityState.FAILED) {
            fail(
                    W123ClosureFailure.SECURITY_FAILED,
                    station.wifiSecurityDiagnostic()
            );
            return;
        }

        boolean associated =
                station.wifiStationState()
                        == WifiStationState.ASSOCIATED
                        && W119Mac.equals(
                        station.wifiSelectedBssid(),
                        ap.wifiMacAddress()
                );

        if (associated
                && station.wifiPendingDataTransmissions()
                == 0) {
            station.clearWifiContentionMetrics();

            transition(
                    W123ClosureStage.MODERATE_BURST,
                    "Associated and queue idle; starting 32-frame moderate burst"
            );
            return;
        }

        if (elapsedStageTicks()
                > CONNECT_TIMEOUT_TICKS) {
            fail(
                    W123ClosureFailure.ASSOCIATION_TIMEOUT,
                    "Association timeout | state="
                            + station.wifiStationState()
                            + " security="
                            + station.wifiSecurityState()
                            + " diagnostic="
                            + station.wifiSecurityDiagnostic()
            );
        }
    }

    private void moderateBurst(
            NetworkDeviceBlockEntity station
    ) {
        acceptedModerate =
                station.sendWifiContentionBurst(
                        MODERATE_FRAMES,
                        PAYLOAD_BYTES
                );

        if (acceptedModerate <= 0) {
            fail(
                    W123ClosureFailure.MODERATE_BURST_REJECTED,
                    "32-frame contention burst accepted no DATA frames"
            );
            return;
        }

        transition(
                W123ClosureStage.WAIT_MODERATE_DRAIN,
                "Moderate burst accepted "
                        + acceptedModerate
                        + "/"
                        + MODERATE_FRAMES
                        + "; waiting for queue drain"
        );
    }

    private void waitModerateDrain(
            NetworkDeviceBlockEntity station
    ) {
        if (station.wifiPendingDataTransmissions()
                == 0) {
            WifiContentionSnapshot result =
                    station.wifiContentionSnapshot();

            boolean valid =
                    result.queueCapacity()
                            == EXPECTED_QUEUE_CAPACITY
                            && result.queuePeak() > 0
                            && result.queuePeak()
                            <= result.queueCapacity()
                            && result.enqueued() > 0L
                            && result.attempts() > 0L
                            && result.successes() > 0L;

            if (!valid) {
                fail(
                        W123ClosureFailure.MODERATE_METRICS_INVALID,
                        "Moderate contention metrics invalid: "
                                + result.compact()
                );
                return;
            }

            station.clearWifiContentionMetrics();

            transition(
                    W123ClosureStage.SATURATION_BURST,
                    "Moderate burst drained successfully; starting 96-frame queue saturation burst"
            );
            return;
        }

        if (elapsedStageTicks()
                > DRAIN_TIMEOUT_TICKS) {
            fail(
                    W123ClosureFailure.MODERATE_DRAIN_TIMEOUT,
                    "Moderate burst queue did not drain | pending="
                            + station.wifiPendingDataTransmissions()
                            + " "
                            + station.wifiContentionDiagnostic()
            );
        }
    }

    private void saturationBurst(
            NetworkDeviceBlockEntity station
    ) {
        acceptedSaturation =
                station.sendWifiContentionBurst(
                        SATURATION_FRAMES,
                        PAYLOAD_BYTES
                );

        WifiContentionSnapshot immediate =
                station.wifiContentionSnapshot();

        if (acceptedSaturation <= 0) {
            fail(
                    W123ClosureFailure.SATURATION_BURST_REJECTED,
                    "96-frame saturation burst accepted no frames"
            );
            return;
        }

        if (immediate.queueCapacity()
                != EXPECTED_QUEUE_CAPACITY
                || immediate.queueDepth()
                > immediate.queueCapacity()
                || immediate.queuePeak()
                > immediate.queueCapacity()) {
            fail(
                    W123ClosureFailure.QUEUE_CAPACITY_INVALID,
                    "Contention queue exceeded W1.23 capacity invariant: "
                            + immediate.compact()
            );
            return;
        }

        if (immediate.drops() <= 0L
                || acceptedSaturation
                >= SATURATION_FRAMES) {
            fail(
                    W123ClosureFailure.QUEUE_OVERFLOW_NOT_OBSERVED,
                    "96 synchronous submissions should exercise the 64-frame queue cap"
                            + " | accepted="
                            + acceptedSaturation
                            + " | "
                            + immediate.compact()
            );
            return;
        }

        transition(
                W123ClosureStage.WAIT_SATURATION_DRAIN,
                "Queue saturation observed | accepted="
                        + acceptedSaturation
                        + "/"
                        + SATURATION_FRAMES
                        + " drops="
                        + immediate.drops()
                        + " peak="
                        + immediate.queuePeak()
                        + "/"
                        + immediate.queueCapacity()
                        + "; waiting for drain"
        );
    }

    private void waitSaturationDrain(
            NetworkDeviceBlockEntity station
    ) {
        if (station.wifiPendingDataTransmissions()
                == 0) {
            WifiContentionSnapshot result =
                    station.wifiContentionSnapshot();

            WifiEngineeringSnapshot engineering =
                    WifiEngineeringProbe.capture(
                            station
                    );

            boolean metricsValid =
                    result.queueDepth() == 0
                            && result.queueCapacity()
                            == EXPECTED_QUEUE_CAPACITY
                            && result.queuePeak()
                            <= result.queueCapacity()
                            && result.enqueued() > 0L
                            && result.attempts() > 0L
                            && result.successes() > 0L
                            && result.drops() > 0L;

            if (!metricsValid) {
                fail(
                        W123ClosureFailure.SATURATION_METRICS_INVALID,
                        "Final saturation metrics invalid: "
                                + result.compact()
                );
                return;
            }

            boolean rfValid =
                    station.usesDetailedPropagationModel()
                            && engineering.deviceId()
                            .equals(
                                    station.id()
                            )
                            && Double.isFinite(
                            engineering.receivedPowerDbm()
                    )
                            && Double.isFinite(
                            engineering.snrDb()
                    );

            if (!rfValid) {
                fail(
                        W123ClosureFailure.RF_PROBE_INVALID,
                        "Engineering RF probe did not produce finite RSSI/SNR or detailed propagation flag"
                                + " | detailed="
                                + station.usesDetailedPropagationModel()
                                + " rssi="
                                + engineering.receivedPowerDbm()
                                + " snr="
                                + engineering.snrDb()
                );
                return;
            }

            transition(
                    W123ClosureStage.COMPLETE,
                    "PASS: W1.23 contention queue cap="
                            + result.queueCapacity()
                            + " peak="
                            + result.queuePeak()
                            + " attempts="
                            + result.attempts()
                            + " successes="
                            + result.successes()
                            + " retries="
                            + result.retries()
                            + " drops="
                            + result.drops()
                            + " deferrals="
                            + result.deferrals()
                            + " | detailed RF probe RSSI="
                            + format(
                            engineering.receivedPowerDbm()
                    )
                            + " dBm SNR="
                            + format(
                            engineering.snrDb()
                    )
                            + " dB"
            );
            return;
        }

        if (elapsedStageTicks()
                > DRAIN_TIMEOUT_TICKS) {
            fail(
                    W123ClosureFailure.SATURATION_DRAIN_TIMEOUT,
                    "Saturation queue did not drain | pending="
                            + station.wifiPendingDataTransmissions()
                            + " "
                            + station.wifiContentionDiagnostic()
            );
        }
    }

    private NetworkDeviceBlockEntity station() {
        BlockEntity entity =
                level.getBlockEntity(
                        stationPos
                );

        return entity
                instanceof NetworkDeviceBlockEntity device
                ? device
                : null;
    }

    private NetworkDeviceBlockEntity ap() {
        BlockEntity entity =
                level.getBlockEntity(
                        apPos
                );

        return entity
                instanceof NetworkDeviceBlockEntity device
                ? device
                : null;
    }

    private long elapsedStageTicks() {
        return Math.max(
                0L,
                level.getGameTime()
                        - stageStartTick
        );
    }

    private void transition(
            W123ClosureStage next,
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
            W123ClosureFailure reason,
            String failureDetail
    ) {
        failure =
                reason == null
                        ? W123ClosureFailure.INTERNAL_ERROR
                        : reason;

        stage =
                W123ClosureStage.FAILED;

        detail =
                safe(
                        failureDetail
                );
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

    private String safe(
            String value
    ) {
        return value == null
                ? ""
                : value;
    }

    private String format(
            double value
    ) {
        return Double.isFinite(
                value
        )
                ? String.format(
                java.util.Locale.ROOT,
                "%.1f",
                value
        )
                : "n/a";
    }
}
