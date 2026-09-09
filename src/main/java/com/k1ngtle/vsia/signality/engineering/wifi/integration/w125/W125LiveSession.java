package com.k1ngtle.vsia.signality.engineering.wifi.integration.w125;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiNetworkRecord;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiSecurityState;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiStationState;
import com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119.W119Mac;
import com.k1ngtle.vsia.signality.engineering.wifi.security.protocol.WifiSecurityProtocol;
import com.k1ngtle.vsia.signality.engineering.wifi.security.protocol.WifiSecurityProtocolRegistry;
import com.k1ngtle.vsia.signality.engineering.wifi.trace.WifiPacketDirection;
import com.k1ngtle.vsia.signality.engineering.wifi.trace.WifiPacketTraceEvent;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public final class W125LiveSession {
    private static final String SSID =
            "VSIA-W125";

    private static final String PASSPHRASE =
            "vsia-w125-test-password";

    private static final long SCAN_TIMEOUT =
            240L;

    private static final long CONNECT_TIMEOUT =
            300L;

    private static final long ACK_TIMEOUT =
            180L;

    private final ServerLevel level;
    private final BlockPos stationPos;
    private final BlockPos apPos;
    private final String protocolId;

    private W125LiveStage stage =
            W125LiveStage.SETUP;

    private boolean passed;
    private String detail =
            "Waiting for first tick";

    private long startTick =
            -1L;

    private long stageStartTick =
            -1L;

    private long ackBaseline;

    public W125LiveSession(
            ServerLevel level,
            BlockPos stationPos,
            BlockPos apPos,
            String protocolId
    ) {
        this.level =
                level;

        this.stationPos =
                stationPos.immutable();

        this.apPos =
                apPos.immutable();

        this.protocolId =
                WifiSecurityProtocolRegistry.canonicalId(
                        protocolId
                );
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

        NetworkDeviceBlockEntity ap =
                device(
                        apPos
                );

        if (station == null
                || ap == null
                || stationPos.equals(
                apPos
        )) {
            fail(
                    "Expected distinct Wi-Fi STA/AP devices"
            );
            return;
        }

        switch (stage) {
            case SETUP ->
                    setup(
                            station,
                            ap
                    );

            case SCAN ->
                    scan(
                            station,
                            ap
                    );

            case CONNECT ->
                    connect(
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

    public W125LiveSnapshot snapshot() {
        NetworkDeviceBlockEntity station =
                device(
                        stationPos
                );

        return new W125LiveSnapshot(
                stage,
                finished(),
                passed,
                protocolId,
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
                        : station.wifiSelectedSecurity(),
                station == null
                        ? 0L
                        : ackCount(
                        station.wifiPacketTraceSnapshot()
                )
        );
    }

    public boolean finished() {
        return stage
                == W125LiveStage.COMPLETE
                || stage
                == W125LiveStage.FAILED;
    }

    private void setup(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap
    ) {
        WifiSecurityProtocol protocol =
                WifiSecurityProtocolRegistry.resolve(
                        protocolId
                );

        station.clearWifiPacketTrace();
        ap.clearWifiPacketTrace();

        String passphrase =
                protocol.isOpen()
                        ? ""
                        : PASSPHRASE;

        if (!ap.configureWifiAccessPoint(
                SSID,
                passphrase
        )
                || !station.configureWifiStation(
                passphrase
        )) {
            fail(
                    "Unable to configure AP/STA for "
                            + protocolId
            );
            return;
        }

        // The normal AP helper uses the profile security ID.
        // W1.25 patch canonicalizes that through the registry.
        // For OPEN testing the profile's security is overridden by the helper path only
        // when the active profile is OPEN. Protected live tests expect current Wi-Fi 7
        // profile WPA3 or a compatible profile selected by the project.
        if (!station.scanWifi()) {
            fail(
                    "Scan could not start"
            );
            return;
        }

        ap.sendWifiBeacon();

        transition(
                W125LiveStage.SCAN,
                "Scanning for "
                        + SSID
                        + " using abstracted security "
                        + protocol.displayName()
        );
    }

    private void scan(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity ap
    ) {
        if (elapsed()
                % 20L == 0L) {
            ap.sendWifiBeacon();
        }

        WifiNetworkRecord network =
                station.discoveredWifiNetworks()
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

        if (network != null
                && station.wifiSecurityDiagnostic()
                .startsWith(
                        "SCAN_COMPLETE_APS_"
                )) {
            if (!WifiSecurityProtocolRegistry.supports(
                    network.security()
            )) {
                fail(
                        "Discovered AP advertises unsupported security "
                                + network.security()
                );
                return;
            }

            String discoveredProtocol =
                    WifiSecurityProtocolRegistry.canonicalId(
                            network.security()
                    );

            if (!discoveredProtocol.equals(
                    protocolId
            )) {
                fail(
                        "Requested live protocol "
                                + protocolId
                                + " but active NetworkProfile advertises "
                                + discoveredProtocol
                );
                return;
            }

            if (!station.connectWifiBssid(
                    network.ssid(),
                    network.bssid()
            )) {
                fail(
                        "Connection start rejected"
                );
                return;
            }

            transition(
                    W125LiveStage.CONNECT,
                    "Connecting to canonical security "
                            + WifiSecurityProtocolRegistry.canonicalId(
                            network.security()
                    )
            );
            return;
        }

        if (elapsed()
                > SCAN_TIMEOUT) {
            fail(
                    "Scan timeout"
            );
        }
    }

    private void connect(
            NetworkDeviceBlockEntity station
    ) {
        if (station.wifiSecurityState()
                == WifiSecurityState.FAILED) {
            fail(
                    "Security failed: "
                            + station.wifiSecurityDiagnostic()
            );
            return;
        }

        boolean securityReady =
                WifiSecurityProtocolRegistry.resolve(
                        station.wifiSelectedSecurity()
                ).isOpen()
                        || station.wifiSecurityState()
                        == WifiSecurityState.SECURED;

        if (station.wifiStationState()
                == WifiStationState.ASSOCIATED
                && securityReady) {
            ackBaseline =
                    ackCount(
                            station.wifiPacketTraceSnapshot()
                    );

            if (!station.sendWifiEngineeringAssociatedData(
                    512
            )) {
                fail(
                        "Associated DATA queue rejected"
                );
                return;
            }

            transition(
                    W125LiveStage.DATA_ACK,
                    "Associated/secured; waiting for DATA ACK"
            );
            return;
        }

        if (elapsed()
                > CONNECT_TIMEOUT) {
            fail(
                    "Connection/security timeout | state="
                            + station.wifiStationState()
                            + " security="
                            + station.wifiSecurityState()
                            + " diagnostic="
                            + station.wifiSecurityDiagnostic()
            );
        }
    }

    private void dataAck(
            NetworkDeviceBlockEntity station
    ) {
        if (ackCount(
                station.wifiPacketTraceSnapshot()
        ) > ackBaseline) {
            passed =
                    true;

            transition(
                    W125LiveStage.COMPLETE,
                    "PASS: "
                            + station.wifiSelectedSecurity()
                            + " association/security and protected DATA/ACK completed through the W1.25 protocol registry"
            );
            return;
        }

        if (elapsed()
                > ACK_TIMEOUT) {
            fail(
                    "No new DATA ACK"
            );
        }
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

    private long elapsed() {
        return Math.max(
                0L,
                level.getGameTime()
                        - stageStartTick
        );
    }

    private void transition(
            W125LiveStage next,
            String value
    ) {
        stage =
                next;

        stageStartTick =
                level.getGameTime();

        detail =
                value;
    }

    private void fail(
            String value
    ) {
        passed =
                false;

        stage =
                W125LiveStage.FAILED;

        detail =
                value;
    }
}
