package com.k1ngtle.vsia.signality.engineering.wifi.integration.mb1;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiNetworkRecord;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiStationState;
import com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119.W119Mac;
import com.k1ngtle.vsia.signality.engineering.wifi.smartconnect.WifiBand;
import com.k1ngtle.vsia.signality.engineering.wifi.smartconnect.WifiBandUtil;
import com.k1ngtle.vsia.signality.engineering.wifi.smartconnect.WifiSmartConnectDecision;
import com.k1ngtle.vsia.signality.engineering.wifi.trace.WifiPacketDirection;
import com.k1ngtle.vsia.signality.engineering.wifi.trace.WifiPacketTraceEvent;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public final class Mb1LiveSession {
    public static final String SSID =
            "VSIA-SMART";

    public static final double TWO_FOUR_FREQUENCY_HZ =
            2_437_000_000.0D;

    public static final double FIVE_FREQUENCY_HZ =
            5_200_000_000.0D;

    private static final long SCAN_TIMEOUT_TICKS =
            300L;

    private static final long ASSOC_TIMEOUT_TICKS =
            240L;

    private static final long ACK_TIMEOUT_TICKS =
            180L;

    private final ServerLevel level;
    private final BlockPos stationPos;
    private final BlockPos ap24Pos;
    private final BlockPos ap5Pos;
    private final WifiBand expectedBand;

    private Mb1LiveStage stage =
            Mb1LiveStage.SETUP;

    private boolean passed;
    private String detail =
            "Waiting for first tick";

    private long startTick =
            -1L;

    private long stageStartTick =
            -1L;

    private long ackBaseline;

    private WifiSmartConnectDecision decision =
            WifiSmartConnectDecision.unavailable(
                    "Not evaluated"
            );

    public Mb1LiveSession(
            ServerLevel level,
            BlockPos stationPos,
            BlockPos ap24Pos,
            BlockPos ap5Pos,
            WifiBand expectedBand
    ) {
        this.level =
                level;

        this.stationPos =
                stationPos.immutable();

        this.ap24Pos =
                ap24Pos.immutable();

        this.ap5Pos =
                ap5Pos.immutable();

        this.expectedBand =
                expectedBand;
    }

    public void tick() {
        if (finished()) {
            return;
        }

        if (startTick < 0L) {
            startTick =
                    level.getGameTime();

            stageStartTick =
                    startTick;
        }

        NetworkDeviceBlockEntity station =
                device(
                        stationPos
                );

        NetworkDeviceBlockEntity ap24 =
                device(
                        ap24Pos
                );

        NetworkDeviceBlockEntity ap5 =
                device(
                        ap5Pos
                );

        if (station == null
                || ap24 == null
                || ap5 == null
                || stationPos.equals(
                ap24Pos
        )
                || stationPos.equals(
                ap5Pos
        )
                || ap24Pos.equals(
                ap5Pos
        )) {
            fail(
                    "Expected three distinct Wi-Fi NetworkDeviceBlockEntity targets"
            );
            return;
        }

        switch (stage) {
            case SETUP ->
                    setup(
                            station,
                            ap24,
                            ap5
                    );

            case SCANNING ->
                    scanning(
                            station,
                            ap24,
                            ap5
                    );

            case SMART_CONNECT ->
                    smartConnect(
                            station
                    );

            case ASSOCIATING ->
                    associating(
                            station
                    );

            case DATA_ACK ->
                    dataAck(
                            station
                    );

            case COMPLETE,
                 FAILED -> {
            }
        }
    }

    public boolean finished() {
        return stage
                == Mb1LiveStage.COMPLETE
                || stage
                == Mb1LiveStage.FAILED;
    }

    public Mb1LiveSnapshot snapshot() {
        NetworkDeviceBlockEntity station =
                device(
                        stationPos
                );

        NetworkDeviceBlockEntity ap24 =
                device(
                        ap24Pos
                );

        NetworkDeviceBlockEntity ap5 =
                device(
                        ap5Pos
                );

        double active =
                station == null
                        ? Double.NaN
                        : station.activeFrequencyHz();

        return new Mb1LiveSnapshot(
                stage,
                finished(),
                passed,
                detail,
                startTick < 0L
                        ? 0L
                        : Math.max(
                        0L,
                        level.getGameTime()
                                - startTick
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
                        : station.wifiSelectedBssid(),
                active,
                WifiBandUtil.bandForFrequency(
                        active
                ),
                station == null
                        || ap24 == null
                        ? Double.NaN
                        : station.discoveredWifiNetworkSnrDb(
                        ap24.wifiMacAddress()
                ),
                station == null
                        || ap5 == null
                        ? Double.NaN
                        : station.discoveredWifiNetworkSnrDb(
                        ap5.wifiMacAddress()
                ),
                decision.network() == null
                        ? ""
                        : decision.network()
                        .bssid(),
                decision.band(),
                decision.score(),
                station == null
                        ? 0L
                        : ackCount(
                        station.wifiPacketTraceSnapshot()
                )
        );
    }

    private void setup(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap24,
            NetworkDeviceBlockEntity ap5
    ) {
        station.clearWifiPacketTrace();
        ap24.clearWifiPacketTrace();
        ap5.clearWifiPacketTrace();

        if (!ap24.configureWifiActiveFrequency(
                TWO_FOUR_FREQUENCY_HZ
        )) {
            fail(
                    "2.4 GHz AP profile does not support 2.437 GHz"
            );
            return;
        }

        if (!ap5.configureWifiActiveFrequency(
                FIVE_FREQUENCY_HZ
        )) {
            fail(
                    "5 GHz AP profile does not support 5.200 GHz"
            );
            return;
        }

        if (!ap24.configureWifiAccessPoint(
                SSID,
                ""
        )
                || !ap5.configureWifiAccessPoint(
                SSID,
                ""
        )
                || !station.configureWifiStation(
                ""
        )) {
            fail(
                    "Unable to configure MB1 AP/STA roles"
            );
            return;
        }

        station.setWifiBackgroundRoamingEnabled(
                false
        );

        ap24.sendWifiBeacon();
        ap5.sendWifiBeacon();

        if (!station.scanWifi()) {
            fail(
                    "MB1 dual-band scan could not start"
            );
            return;
        }

        transition(
                Mb1LiveStage.SCANNING,
                "Scanning one Smart Connect ESS on 2.437 and 5.200 GHz"
        );
    }

    private void scanning(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap24,
            NetworkDeviceBlockEntity ap5
    ) {
        if (elapsedStage()
                % 20L == 0L) {
            ap24.sendWifiBeacon();
            ap5.sendWifiBeacon();
        }

        WifiNetworkRecord twoFour =
                find(
                        station,
                        ap24
                );

        WifiNetworkRecord five =
                find(
                        station,
                        ap5
                );

        boolean complete =
                station.wifiSecurityDiagnostic()
                        .startsWith(
                                "SCAN_COMPLETE_APS_"
                        );

        if (twoFour != null
                && five != null
                && complete) {
            transition(
                    Mb1LiveStage.SMART_CONNECT,
                    "Both Smart Connect BSSs discovered"
            );
            return;
        }

        if (elapsedStage()
                > SCAN_TIMEOUT_TICKS) {
            fail(
                    "Dual-band scan timeout | 2.4="
                            + (twoFour != null)
                            + " 5="
                            + (five != null)
                            + " diagnostic="
                            + station.wifiSecurityDiagnostic()
            );
        }
    }

    private void smartConnect(
            NetworkDeviceBlockEntity station
    ) {
        decision =
                station.wifiSmartConnectDecision(
                        SSID
                );

        if (!decision.available()
                || decision.network() == null) {
            fail(
                    "Smart Connect produced no candidate: "
                            + decision.reason()
            );
            return;
        }

        if (decision.band()
                != expectedBand) {
            fail(
                    "Smart Connect selected "
                            + WifiBandUtil.displayName(
                            decision.band()
                    )
                            + " but this run expects "
                            + WifiBandUtil.displayName(
                            expectedBand
                    )
                            + " | "
                            + decision.reason()
            );
            return;
        }

        if (!station.smartConnectWifi(
                SSID
        )) {
            fail(
                    "Smart Connect decision was valid but connection start failed"
            );
            return;
        }

        transition(
                Mb1LiveStage.ASSOCIATING,
                "Smart Connect selected "
                        + WifiBandUtil.displayName(
                        decision.band()
                )
                        + " BSSID "
                        + decision.network()
                        .bssid()
                        + " score="
                        + format(
                        decision.score()
                )
        );
    }

    private void associating(
            NetworkDeviceBlockEntity station
    ) {
        if (station.wifiStationState()
                == WifiStationState.ASSOCIATED) {
            WifiBand activeBand =
                    WifiBandUtil.bandForFrequency(
                            station.activeFrequencyHz()
                    );

            if (activeBand
                    != expectedBand) {
                fail(
                        "Associated on unexpected active band "
                                + activeBand
                );
                return;
            }

            ackBaseline =
                    ackCount(
                            station.wifiPacketTraceSnapshot()
                    );

            if (!station.sendWifiEngineeringAssociatedData(
                    512
            )) {
                fail(
                        "Post-Smart-Connect DATA could not be queued"
                );
                return;
            }

            transition(
                    Mb1LiveStage.DATA_ACK,
                    "Associated on expected band; waiting for DATA ACK"
            );
            return;
        }

        if (elapsedStage()
                > ASSOC_TIMEOUT_TICKS) {
            fail(
                    "Smart Connect association timeout | state="
                            + station.wifiStationState()
                            + " security="
                            + station.wifiSecurityState()
            );
        }
    }

    private void dataAck(
            NetworkDeviceBlockEntity station
    ) {
        long now =
                ackCount(
                        station.wifiPacketTraceSnapshot()
                );

        if (now > ackBaseline) {
            passed =
                    true;

            transition(
                    Mb1LiveStage.COMPLETE,
                    "PASS: dual-band Smart Connect selected "
                            + WifiBandUtil.displayName(
                            expectedBand
                    )
                            + ", associated successfully, and DATA/ACK completed"
            );
            return;
        }

        if (elapsedStage()
                > ACK_TIMEOUT_TICKS) {
            fail(
                    "No new DATA ACK after Smart Connect association"
            );
        }
    }

    private WifiNetworkRecord find(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap
    ) {
        return station.discoveredWifiNetworks()
                .stream()
                .filter(
                        value ->
                                value != null
                                        && W119Mac.equals(
                                        value.bssid(),
                                        ap.wifiMacAddress()
                                )
                )
                .findFirst()
                .orElse(
                        null
                );
    }

    private NetworkDeviceBlockEntity device(
            BlockPos pos
    ) {
        BlockEntity entity =
                level.getBlockEntity(
                        pos
                );

        return entity
                instanceof NetworkDeviceBlockEntity device
                ? device
                : null;
    }

    private long ackCount(
            List<WifiPacketTraceEvent> trace
    ) {
        return trace.stream()
                .filter(
                        event ->
                                event.direction()
                                        == WifiPacketDirection.RX
                                        && "CONTROL".equalsIgnoreCase(
                                        event.frameType()
                                )
                                        && event.subtype()
                                        == 13
                )
                .count();
    }

    private long elapsedStage() {
        return Math.max(
                0L,
                level.getGameTime()
                        - stageStartTick
        );
    }

    private void transition(
            Mb1LiveStage next,
            String nextDetail
    ) {
        stage =
                next;

        stageStartTick =
                level.getGameTime();

        detail =
                nextDetail;
    }

    private void fail(
            String failureDetail
    ) {
        passed =
                false;

        stage =
                Mb1LiveStage.FAILED;

        detail =
                failureDetail;
    }

    private String format(
            double value
    ) {
        return String.format(
                java.util.Locale.ROOT,
                "%.1f",
                value
        );
    }
}
