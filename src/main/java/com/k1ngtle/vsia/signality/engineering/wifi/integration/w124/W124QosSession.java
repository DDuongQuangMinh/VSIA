package com.k1ngtle.vsia.signality.engineering.wifi.integration.w124;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiAccessCategory;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiContentionSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiNetworkRecord;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiSecurityState;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiStationState;
import com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119.W119Mac;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public final class W124QosSession {
    public static final String SSID =
            "VSIA-W124-QOS";

    private static final int FRAMES_PER_CLASS =
            8;

    private static final int BYTES_PER_FRAME =
            512;

    private static final int MIXED_FRAMES_PER_CLASS =
            4;

    private static final long SCAN_TIMEOUT_TICKS =
            240L;

    private static final long CONNECT_TIMEOUT_TICKS =
            240L;

    private static final long DRAIN_TIMEOUT_TICKS =
            500L;

    private final ServerLevel level;
    private final BlockPos stationPos;
    private final BlockPos apPos;

    private W124QosStage stage =
            W124QosStage.SETUP;

    private W124QosFailure failure =
            W124QosFailure.NONE;

    private String detail =
            "Waiting for first server tick";

    private long startTick =
            -1L;

    private long stageStartTick =
            -1L;

    private final List<W124QosClassResult> results =
            new ArrayList<>();

    private WifiAccessCategory activeCategory;
    private int activeAccepted;
    private boolean activeCategoryObserved;
    private boolean stageBurstQueued;

    private int mixedAccepted;
    private boolean mixedQueued;

    public W124QosSession(
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
                        W124QosFailure.INVALID_TOPOLOGY,
                        "Expected distinct Wi-Fi NetworkDeviceBlockEntity STA/AP targets"
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

                case VOICE ->
                        classStage(
                                station,
                                WifiAccessCategory.VOICE,
                                W124QosStage.VIDEO
                        );

                case VIDEO ->
                        classStage(
                                station,
                                WifiAccessCategory.VIDEO,
                                W124QosStage.BEST_EFFORT
                        );

                case BEST_EFFORT ->
                        classStage(
                                station,
                                WifiAccessCategory.BEST_EFFORT,
                                W124QosStage.BACKGROUND
                        );

                case BACKGROUND ->
                        classStage(
                                station,
                                WifiAccessCategory.BACKGROUND,
                                W124QosStage.MIXED_LOAD
                        );

                case MIXED_LOAD ->
                        mixedLoad(
                                station
                        );

                case COMPLETE,
                     FAILED -> {
                }
            }
        } catch (Exception exception) {
            fail(
                    W124QosFailure.INTERNAL_ERROR,
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
                == W124QosStage.COMPLETE
                || stage
                == W124QosStage.FAILED;
    }

    public boolean passed() {
        return stage
                == W124QosStage.COMPLETE
                && failure
                == W124QosFailure.NONE;
    }

    public W124QosSnapshot snapshot() {
        NetworkDeviceBlockEntity station =
                station();

        WifiContentionSnapshot contention =
                station == null
                        ? emptyContention()
                        : station.wifiContentionSnapshot();

        long now =
                level.getGameTime();

        return new W124QosSnapshot(
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
                List.copyOf(
                        results
                ),
                mixedAccepted,
                contention.successes(),
                contention.retries(),
                contention.drops(),
                contention.deferrals(),
                contention.queuePeak(),
                contention.cwVoice(),
                contention.cwVideo(),
                contention.cwBestEffort(),
                contention.cwBackground()
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
                    W124QosFailure.AP_CONFIGURATION_FAILED,
                    "AP rejected W1.24 access-point configuration"
            );
            return;
        }

        boolean stationConfigured =
                station.configureWifiStation(
                        ""
                );

        if (!stationConfigured) {
            fail(
                    W124QosFailure.STATION_CONFIGURATION_FAILED,
                    "STA rejected W1.24 station configuration"
            );
            return;
        }

        station.setWifiBackgroundRoamingEnabled(
                false
        );

        ap.sendWifiBeacon();

        if (!station.scanWifi()) {
            fail(
                    W124QosFailure.SCAN_START_FAILED,
                    "STA could not start the W1.24 scan"
            );
            return;
        }

        transition(
                W124QosStage.INITIAL_SCAN,
                "W1.24 QoS lab configured; scanning for "
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
                        W124QosFailure.CONNECT_START_FAILED,
                        "Exact-BSSID connection to W1.24 AP was rejected"
                );
                return;
            }

            transition(
                    W124QosStage.CONNECTING,
                    "AP discovered; waiting for association/security"
            );
            return;
        }

        if (elapsed > SCAN_TIMEOUT_TICKS) {
            fail(
                    W124QosFailure.SCAN_TIMEOUT,
                    "W1.24 scan did not complete with the target AP"
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
                    W124QosFailure.SECURITY_FAILED,
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
            resetClassState();

            transition(
                    W124QosStage.VOICE,
                    "Associated. Starting live WMM/EDCA access-category validation."
            );
            return;
        }

        if (elapsedStageTicks()
                > CONNECT_TIMEOUT_TICKS) {
            fail(
                    W124QosFailure.ASSOCIATION_TIMEOUT,
                    "Association timeout | state="
                            + station.wifiStationState()
                            + " security="
                            + station.wifiSecurityState()
            );
        }
    }

    private void classStage(
            NetworkDeviceBlockEntity station,
            WifiAccessCategory category,
            W124QosStage next
    ) {
        if (!stageBurstQueued) {
            station.clearWifiContentionMetrics();

            activeCategory =
                    category;

            activeAccepted =
                    0;

            for (int index = 0;
                 index < FRAMES_PER_CLASS;
                 index++) {
                if (station.sendWifiEngineeringQosData(
                        BYTES_PER_FRAME,
                        category
                )) {
                    activeAccepted++;
                }
            }

            if (activeAccepted
                    != FRAMES_PER_CLASS) {
                fail(
                        W124QosFailure.CATEGORY_QUEUE_REJECTED,
                        category
                                + " accepted "
                                + activeAccepted
                                + "/"
                                + FRAMES_PER_CLASS
                );
                return;
            }

            String expected =
                    "AC="
                            + category.name();

            activeCategoryObserved =
                    station.wifiPendingMacDiagnostics()
                            .stream()
                            .anyMatch(
                                    line ->
                                            line.contains(
                                                    expected
                                            )
                            );

            if (!activeCategoryObserved) {
                fail(
                        W124QosFailure.CATEGORY_NOT_VISIBLE_AT_MAC,
                        category
                                + " was accepted but pending MAC diagnostics did not expose "
                                + expected
                                + " | "
                                + String.join(
                                " | ",
                                station.wifiPendingMacDiagnostics()
                        )
                );
                return;
            }

            stageBurstQueued =
                    true;

            detail =
                    category
                            + " live burst queued "
                            + activeAccepted
                            + "/"
                            + FRAMES_PER_CLASS
                            + "; MAC exposed "
                            + expected
                            + "; waiting for DATA/ACK drain";

            return;
        }

        if (station.wifiPendingDataTransmissions()
                == 0) {
            WifiContentionSnapshot contention =
                    station.wifiContentionSnapshot();

            if (contention.successes()
                    < activeAccepted
                    || contention.drops()
                    > 0L) {
                fail(
                        W124QosFailure.CATEGORY_DELIVERY_FAILED,
                        category
                                + " delivery failed | accepted="
                                + activeAccepted
                                + " successes="
                                + contention.successes()
                                + " drops="
                                + contention.drops()
                                + " retries="
                                + contention.retries()
                );
                return;
            }

            results.add(
                    new W124QosClassResult(
                            category,
                            activeAccepted,
                            contention.successes(),
                            contention.retries(),
                            contention.drops(),
                            contention.deferrals(),
                            contention.queuePeak(),
                            activeCategoryObserved
                    )
            );

            resetClassState();

            transition(
                    next,
                    category
                            + " passed; moving to "
                            + next
            );

            return;
        }

        if (elapsedStageTicks()
                > DRAIN_TIMEOUT_TICKS) {
            fail(
                    W124QosFailure.CATEGORY_DRAIN_TIMEOUT,
                    activeCategory
                            + " queue did not drain | pending="
                            + station.wifiPendingDataTransmissions()
            );
        }
    }

    private void mixedLoad(
            NetworkDeviceBlockEntity station
    ) {
        if (!mixedQueued) {
            station.clearWifiContentionMetrics();

            mixedAccepted =
                    0;

            WifiAccessCategory[] categories =
                    new WifiAccessCategory[] {
                            WifiAccessCategory.BACKGROUND,
                            WifiAccessCategory.BEST_EFFORT,
                            WifiAccessCategory.VIDEO,
                            WifiAccessCategory.VOICE
                    };

            for (WifiAccessCategory category
                    : categories) {
                for (int index = 0;
                     index < MIXED_FRAMES_PER_CLASS;
                     index++) {
                    if (station.sendWifiEngineeringQosData(
                            BYTES_PER_FRAME,
                            category
                    )) {
                        mixedAccepted++;
                    }
                }
            }

            int expected =
                    categories.length
                            * MIXED_FRAMES_PER_CLASS;

            if (mixedAccepted
                    != expected) {
                fail(
                        W124QosFailure.MIXED_LOAD_REJECTED,
                        "Mixed load accepted "
                                + mixedAccepted
                                + "/"
                                + expected
                );
                return;
            }

            mixedQueued =
                    true;

            detail =
                    "Mixed VO/VI/BE/BK load queued "
                            + mixedAccepted
                            + " frames; waiting for drain";

            return;
        }

        if (station.wifiPendingDataTransmissions()
                == 0) {
            WifiContentionSnapshot contention =
                    station.wifiContentionSnapshot();

            if (contention.successes()
                    < mixedAccepted
                    || contention.drops()
                    > 0L) {
                fail(
                        W124QosFailure.MIXED_LOAD_DELIVERY_FAILED,
                        "Mixed load failed | accepted="
                                + mixedAccepted
                                + " successes="
                                + contention.successes()
                                + " retries="
                                + contention.retries()
                                + " drops="
                                + contention.drops()
                );
                return;
            }

            boolean cwRecovered =
                    contention.cwVoice()
                            == WifiAccessCategory.VOICE.cwMin()
                            && contention.cwVideo()
                            == WifiAccessCategory.VIDEO.cwMin()
                            && contention.cwBestEffort()
                            == WifiAccessCategory.BEST_EFFORT.cwMin()
                            && contention.cwBackground()
                            == WifiAccessCategory.BACKGROUND.cwMin();

            if (!cwRecovered) {
                fail(
                        W124QosFailure.MIXED_LOAD_DELIVERY_FAILED,
                        "EDCA contention windows did not recover to CWmin after successful mixed load"
                                + " | VO="
                                + contention.cwVoice()
                                + " VI="
                                + contention.cwVideo()
                                + " BE="
                                + contention.cwBestEffort()
                                + " BK="
                                + contention.cwBackground()
                );
                return;
            }

            transition(
                    W124QosStage.COMPLETE,
                    "PASS: all four WMM access categories reached the live MAC, completed DATA/ACK delivery, mixed load drained, and EDCA CW state recovered"
            );

            return;
        }

        if (elapsedStageTicks()
                > DRAIN_TIMEOUT_TICKS) {
            fail(
                    W124QosFailure.MIXED_LOAD_DRAIN_TIMEOUT,
                    "Mixed QoS queue did not drain | pending="
                            + station.wifiPendingDataTransmissions()
            );
        }
    }

    private void resetClassState() {
        activeCategory =
                null;

        activeAccepted =
                0;

        activeCategoryObserved =
                false;

        stageBurstQueued =
                false;
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
            W124QosStage next,
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
            W124QosFailure reason,
            String failureDetail
    ) {
        failure =
                reason == null
                        ? W124QosFailure.INTERNAL_ERROR
                        : reason;

        stage =
                W124QosStage.FAILED;

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
}
