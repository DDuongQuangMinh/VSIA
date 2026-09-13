package com.k1ngtle.vsia.phone.network.realism;

import com.k1ngtle.vsia.phone.network.realism.packet.S2CPhoneWirelessSnapshotPacket;
import com.k1ngtle.vsia.signality.api.signal.ISignalReceiver;
import com.k1ngtle.vsia.signality.core.signal.SignalBus;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularBand;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularBandCatalog;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularGeneration;
import com.k1ngtle.vsia.signality.engineering.cellular.CellularMode;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiMode;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiMacController;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.field.FieldDeviceNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PhoneWirelessServerService {
    public static final double WIFI_SCAN_MIN_RSSI_DBM =
            -96.0;

    public static final double WIFI_ASSOC_MIN_RSSI_DBM =
            -88.0;

    public static final double WIFI_DISCONNECT_RSSI_DBM =
            -90.0;

    public static final double WIFI_DISCONNECT_MIN_SINR_DB =
            -5.0;

    public static final double WIFI_ROAM_HYSTERESIS_DB =
            6.0;

    public static final double CELL_MIN_RSRP_DBM =
            -125.0;

    public static final double CELL_DATA_MIN_RSRP_DBM =
            -120.0;

    public static final double CELL_DATA_MIN_SINR_DB =
            -6.0;

    private static final Map<UUID, Session>
            SESSIONS =
            new HashMap<>();

    private PhoneWirelessServerService() {
    }

    public static void tick(
            ServerPlayer player
    ) {
        if (player == null) {
            return;
        }

        if (player.serverLevel()
                .getGameTime()
                % 10L != 0L) {
            return;
        }

        refresh(
                player,
                true
        );
    }

    public static void refresh(
            ServerPlayer player,
            boolean sendSnapshot
    ) {
        if (player == null) {
            return;
        }

        Session session =
                session(
                        player
                );

        List<WifiCandidate> wifi =
                scanWifi(
                        player
                );

        updateWifi(
                player,
                session,
                wifi
        );

        List<CellCandidate> cells =
                scanCells(
                        player
                );

        updateCellular(
                player,
                session,
                cells
        );

        session.lastSnapshot =
                snapshot(
                        session,
                        wifi
                );

        if (sendSnapshot) {
            FieldDeviceNetwork.sendToPlayer(
                    player,
                    new S2CPhoneWirelessSnapshotPacket(
                            session.lastSnapshot
                    )
            );
        }
    }

    public static void setWifiEnabled(
            ServerPlayer player,
            boolean enabled
    ) {
        Session session =
                session(
                        player
                );

        session.wifiEnabled =
                enabled;

        if (!enabled) {
            disconnectWifi(
                    session,
                    "Wi-Fi is off"
            );
        } else {
            session.wifiStatus =
                    "Scanning for nearby access points";
        }

        refresh(
                player,
                true
        );
    }

    public static void setCellularEnabled(
            ServerPlayer player,
            boolean enabled
    ) {
        Session session =
                session(
                        player
                );

        session.cellularEnabled =
                enabled;

        if (!enabled) {
            resetCellular(
                    session,
                    "Cellular data is off"
            );
        }

        refresh(
                player,
                true
        );
    }

    public static void connectWifi(
            ServerPlayer player,
            String bssid,
            String passphrase
    ) {
        Session session =
                session(
                        player
                );

        if (!session.wifiEnabled) {
            session.wifiStatus =
                    "Turn Wi-Fi on first";

            refresh(
                    player,
                    true
            );

            return;
        }

        WifiCandidate candidate =
                scanWifi(
                        player
                )
                        .stream()
                        .filter(
                                value ->
                                        value.bssid()
                                                .equalsIgnoreCase(
                                                        safe(
                                                                bssid
                                                        )
                                                )
                        )
                        .findFirst()
                        .orElse(
                                null
                        );

        if (candidate == null) {
            disconnectWifi(
                    session,
                    "Access point is no longer in range"
            );

            session.wifiStage =
                    "FAILED";

            refresh(
                    player,
                    true
            );

            return;
        }

        if (candidate.locked()) {
            String supplied =
                    passphrase == null
                            ? ""
                            : passphrase;

            if (supplied.isBlank()) {
                supplied =
                        session.rememberedWifiPassphrases
                                .getOrDefault(
                                        wifiCredentialKey(
                                                candidate
                                        ),
                                        ""
                                );
            }

            if (!wifiCredentialMatches(
                    candidate.device(),
                    supplied
            )) {
                disconnectWifi(
                        session,
                        supplied.isBlank()
                                ? "Password required for "
                                + candidate.ssid()
                                : "Incorrect Wi-Fi password"
                );

                session.wifiStage =
                        "FAILED";

                refresh(
                        player,
                        true
                );

                return;
            }

            session.rememberedWifiPassphrases.put(
                    wifiCredentialKey(
                            candidate
                    ),
                    supplied
            );
        }

        if (candidate.rssiDbm()
                < WIFI_ASSOC_MIN_RSSI_DBM
                || candidate.sinrDb()
                < WIFI_DISCONNECT_MIN_SINR_DB) {
            disconnectWifi(
                    session,
                    "Signal is too weak to associate"
            );

            session.wifiStage =
                    "FAILED";

            refresh(
                    player,
                    true
            );

            return;
        }

        session.pendingBssid =
                candidate.bssid();

        session.connectedBssid =
                "";

        session.wifiStage =
                "AUTHENTICATING";

        session.wifiStageStartTick =
                player.serverLevel()
                        .getGameTime();

        session.wifiStatus =
                "802.11 authentication";

        session.wifiMisses =
                0;

        refresh(
                player,
                true
        );
    }

    public static void disconnectWifi(
            ServerPlayer player
    ) {
        if (player == null) {
            return;
        }

        Session session =
                session(
                        player
                );

        disconnectWifi(
                session,
                "Disconnected"
        );

        refresh(
                player,
                true
        );
    }

    public static void forgetWifi(
            ServerPlayer player,
            String bssid
    ) {
        if (player == null) {
            return;
        }

        Session session =
                session(
                        player
                );

        WifiCandidate candidate =
                scanWifi(
                        player
                )
                        .stream()
                        .filter(
                                value ->
                                        value.bssid()
                                                .equalsIgnoreCase(
                                                        safe(
                                                                bssid
                                                        )
                                                )
                        )
                        .findFirst()
                        .orElse(
                                null
                        );

        String key =
                candidate != null
                        ? wifiCredentialKey(
                        candidate
                )
                        : session.connectedNetworkKey;

        if (key != null
                && !key.isBlank()) {
            session.rememberedWifiPassphrases
                    .remove(
                            key
                    );

            session.knownWifiNetworks
                    .remove(
                            key
                    );

            session.autoJoinPreferences
                    .remove(
                            key
                    );
        }

        disconnectWifi(
                session,
                "Forgot network"
        );

        refresh(
                player,
                true
        );
    }

    public static void setWifiAutoJoin(
            ServerPlayer player,
            String bssid,
            boolean enabled
    ) {
        if (player == null) {
            return;
        }

        Session session =
                session(
                        player
                );

        WifiCandidate candidate =
                scanWifi(
                        player
                )
                        .stream()
                        .filter(
                                value ->
                                        value.bssid()
                                                .equalsIgnoreCase(
                                                        safe(
                                                                bssid
                                                        )
                                                )
                        )
                        .findFirst()
                        .orElse(
                                null
                        );

        String key =
                candidate != null
                        ? wifiCredentialKey(
                        candidate
                )
                        : session.connectedNetworkKey;

        if (key == null
                || key.isBlank()) {
            refresh(
                    player,
                    true
            );

            return;
        }

        session.knownWifiNetworks
                .add(
                        key
                );

        session.autoJoinPreferences
                .put(
                        key,
                        enabled
                );

        refresh(
                player,
                true
        );
    }

    public static AccessDecision validateDataAccess(
            ServerPlayer player,
            String transport
    ) {
        if (player == null) {
            return AccessDecision.reject(
                    "No player session"
            );
        }

        refresh(
                player,
                false
        );

        Session session =
                session(
                        player
                );

        if ("WIFI".equalsIgnoreCase(
                transport
        )) {
            PhoneWirelessSnapshot.WifiStatus wifi =
                    session.lastSnapshot
                            .wifi();

            if (!wifi.enabled()) {
                return AccessDecision.reject(
                        "Wi-Fi is turned off"
                );
            }

            if (!wifi.connected()) {
                return AccessDecision.reject(
                        "Wi-Fi is not associated with an access point"
                );
            }

            if (wifi.rssiDbm()
                    < WIFI_DISCONNECT_RSSI_DBM
                    || wifi.sinrDb()
                    < WIFI_DISCONNECT_MIN_SINR_DB) {
                return AccessDecision.reject(
                        String.format(
                                Locale.ROOT,
                                "Wi-Fi link is below usable threshold: RSSI %d dBm, SINR %.1f dB",
                                wifi.rssiDbm(),
                                wifi.sinrDb()
                        )
                );
            }

            return AccessDecision.allow(
                    String.format(
                            Locale.ROOT,
                            "Wi-Fi %s | RSSI %d dBm | SINR %.1f dB | %.0f m",
                            wifi.ssid(),
                            wifi.rssiDbm(),
                            wifi.sinrDb(),
                            wifi.distanceBlocks()
                    )
            );
        }

        if ("CELLULAR".equalsIgnoreCase(
                transport
        )) {
            PhoneWirelessSnapshot.CellularStatus cellular =
                    session.lastSnapshot
                            .cellular();

            if (!cellular.enabled()) {
                return AccessDecision.reject(
                        "Cellular data is turned off"
                );
            }

            if (!cellular.registered()
                    || !"ACTIVE".equalsIgnoreCase(
                    cellular.pduState()
            )) {
                return AccessDecision.reject(
                        cellular.status()
                );
            }

            if (cellular.rsrpDbm()
                    < CELL_DATA_MIN_RSRP_DBM
                    || cellular.sinrDb()
                    < CELL_DATA_MIN_SINR_DB) {
                return AccessDecision.reject(
                        String.format(
                                Locale.ROOT,
                                "Cellular radio link is too weak for packet data: RSRP %d dBm, SINR %.1f dB",
                                cellular.rsrpDbm(),
                                cellular.sinrDb()
                        )
                );
            }

            return AccessDecision.allow(
                    String.format(
                            Locale.ROOT,
                            "%s %s | RSRP %d dBm | SINR %.1f dB | %.0f m",
                            cellular.carrier(),
                            cellular.band(),
                            cellular.rsrpDbm(),
                            cellular.sinrDb(),
                            cellular.distanceBlocks()
                    )
            );
        }

        return AccessDecision.reject(
                "Unknown local phone transport"
        );
    }

    public static void remove(
            UUID playerId
    ) {
        if (playerId != null) {
            SESSIONS.remove(
                    playerId
            );
        }
    }

    public static void clear() {
        SESSIONS.clear();
    }

    private static Session session(
            ServerPlayer player
    ) {
        return SESSIONS
                .computeIfAbsent(
                        player.getUUID(),
                        ignored ->
                                new Session()
                );
    }

    private static void updateWifi(
            ServerPlayer player,
            Session session,
            List<WifiCandidate> candidates
    ) {
        if (!session.wifiEnabled) {
            disconnectWifi(
                    session,
                    "Wi-Fi is off"
            );

            return;
        }

        if (!session.pendingBssid
                .isBlank()) {
            WifiCandidate pending =
                    findWifi(
                            candidates,
                            session.pendingBssid
                    );

            if (pending == null) {
                disconnectWifi(
                        session,
                        "Access point disappeared during association"
                );

                session.wifiStage =
                        "FAILED";

                return;
            }

            long elapsed =
                    player.serverLevel()
                            .getGameTime()
                            - session.wifiStageStartTick;

            if (elapsed < 5L) {
                session.wifiStage =
                        "AUTHENTICATING";

                session.wifiStatus =
                        "802.11 authentication";

                return;
            }

            if (elapsed < 10L) {
                session.wifiStage =
                        "ASSOCIATING";

                session.wifiStatus =
                        "Associating with "
                                + pending.ssid();

                return;
            }

            if (elapsed < 15L) {
                session.wifiStage =
                        "DHCP";

                session.wifiStatus =
                        "Requesting IPv4 lease";

                return;
            }

            if (elapsed < 20L) {
                session.wifiStage =
                        "GATEWAY";

                session.wifiStatus =
                        "Validating default gateway";

                return;
            }

            if (elapsed < 25L) {
                session.wifiStage =
                        "DNS";

                session.wifiStatus =
                        "Configuring DNS";

                return;
            }

            session.connectedBssid =
                    pending.bssid();

            session.pendingBssid =
                    "";

            session.wifiStage =
                    "CONNECTED";

            session.wifiStatus =
                    "Connected";

            session.wifiMisses =
                    0;

            assignWifiLease(
                    player,
                    session,
                    pending
            );

            String connectedKey =
                    wifiCredentialKey(
                            pending
                    );

            session.connectedNetworkKey =
                    connectedKey;

            session.knownWifiNetworks
                    .add(
                            connectedKey
                    );

            session.autoJoinPreferences
                    .putIfAbsent(
                            connectedKey,
                            true
                    );
        }

        if (session.connectedBssid
                .isBlank()) {
            WifiCandidate autoJoin =
                    candidates
                            .stream()
                            .filter(
                                    candidate ->
                                            candidate.rssiDbm()
                                                    >= WIFI_ASSOC_MIN_RSSI_DBM
                                                    && candidate.sinrDb()
                                                    >= WIFI_DISCONNECT_MIN_SINR_DB
                            )
                            .filter(
                                    candidate ->
                                            session.knownWifiNetworks
                                                    .contains(
                                                            wifiCredentialKey(
                                                                    candidate
                                                            )
                                                    )
                            )
                            .filter(
                                    candidate ->
                                            session.autoJoinPreferences
                                                    .getOrDefault(
                                                            wifiCredentialKey(
                                                                    candidate
                                                            ),
                                                            true
                                                    )
                            )
                            .filter(
                                    candidate ->
                                            !candidate.locked()
                                                    || wifiCredentialMatches(
                                                    candidate.device(),
                                                    session.rememberedWifiPassphrases
                                                            .getOrDefault(
                                                                    wifiCredentialKey(
                                                                            candidate
                                                                    ),
                                                                    ""
                                                            )
                                            )
                            )
                            .max(
                                    Comparator.comparingInt(
                                            WifiCandidate::rssiDbm
                                    )
                            )
                            .orElse(
                                    null
                            );

            if (autoJoin != null
                    && !"FAILED".equals(
                    session.wifiStage
            )) {
                session.pendingBssid =
                        autoJoin.bssid();

                session.connectedBssid =
                        "";

                session.wifiStage =
                        "AUTHENTICATING";

                session.wifiStageStartTick =
                        player.serverLevel()
                                .getGameTime();

                session.wifiStatus =
                        "Auto-joining "
                                + autoJoin.ssid();

                session.wifiMisses =
                        0;

                return;
            }

            if (!"FAILED".equals(
                    session.wifiStage
            )) {
                session.wifiStage =
                        "SCANNING";

                session.wifiStatus =
                        candidates.isEmpty()
                                ? "No Wi-Fi networks in range"
                                : candidates.size()
                                + " network(s) in range";
            }

            return;
        }

        WifiCandidate current =
                findWifi(
                        candidates,
                        session.connectedBssid
                );

        if (current == null
                || current.rssiDbm()
                < WIFI_DISCONNECT_RSSI_DBM
                || current.sinrDb()
                < WIFI_DISCONNECT_MIN_SINR_DB) {
            session.wifiMisses++;

            if (session.wifiMisses >= 3) {
                disconnectWifi(
                        session,
                        "Wi-Fi signal lost"
                );
            } else {
                session.wifiStatus =
                        "Weak signal - link recovery";
            }

            return;
        }

        session.wifiMisses =
                0;

        WifiCandidate roam =
                candidates
                        .stream()
                        .filter(
                                candidate ->
                                        !candidate.bssid()
                                                .equalsIgnoreCase(
                                                        current.bssid()
                                                )
                                                && candidate.ssid()
                                                .equals(
                                                        current.ssid()
                                                )
                                                && candidate.security()
                                                .equals(
                                                        current.security()
                                                )
                        )
                        .filter(
                                candidate ->
                                        candidate.rssiDbm()
                                                >= current.rssiDbm()
                                                + WIFI_ROAM_HYSTERESIS_DB
                        )
                        .filter(
                                candidate ->
                                        !candidate.locked()
                                                || wifiCredentialMatches(
                                                candidate.device(),
                                                session.rememberedWifiPassphrases
                                                        .getOrDefault(
                                                                wifiCredentialKey(
                                                                        candidate
                                                                ),
                                                                ""
                                                        )
                                        )
                        )
                        .max(
                                Comparator.comparingInt(
                                        WifiCandidate::rssiDbm
                                )
                        )
                        .orElse(
                                null
                        );

        if (roam != null) {
            session.connectedBssid =
                    roam.bssid();

            session.wifiStatus =
                    "Roamed to stronger BSSID";

            assignWifiLease(
                    player,
                    session,
                    roam
            );
        } else {
            session.wifiStatus =
                    PhoneWirelessLinkMath
                            .wifiQuality(
                                    current.rssiDbm(),
                                    current.sinrDb()
                            );
        }
    }

    private static void updateCellular(
            ServerPlayer player,
            Session session,
            List<CellCandidate> cells
    ) {
        if (!session.cellularEnabled) {
            resetCellular(
                    session,
                    "Cellular data is off"
            );

            return;
        }

        CellCandidate best =
                cells.stream()
                        .filter(
                                value ->
                                        value.rsrpDbm()
                                                >= CELL_MIN_RSRP_DBM
                                                && value.sinrDb()
                                                >= -8.0
                        )
                        .max(
                                Comparator.comparingDouble(
                                        CellCandidate::selectionScore
                                )
                        )
                        .orElse(
                                null
                        );

        if (best == null) {
            resetCellular(
                    session,
                    "No cellular antenna in usable radio range"
            );

            session.cellularStatus =
                    "No Service";

            return;
        }

        if (session.servingCellId == null
                || !session.servingCellId
                .equals(
                        best.deviceId()
                )) {
            session.servingCellId =
                    best.deviceId();

            session.cellAttachStartTick =
                    player.serverLevel()
                            .getGameTime();

            session.cellularStatus =
                    "Cell selection";
        }

        long elapsed =
                player.serverLevel()
                        .getGameTime()
                        - session.cellAttachStartTick;

        session.cellCandidate =
                best;

        if (elapsed < 8L) {
            session.rrc =
                    "CELL_SEARCH";

            session.nas =
                    "DEREGISTERED";

            session.pdu =
                    "INACTIVE";

            session.cellularRegistered =
                    false;

            session.cellularStatus =
                    "Searching / synchronizing SSB";

            return;
        }

        if (elapsed < 16L) {
            session.rrc =
                    "CAMPED";

            session.nas =
                    "DEREGISTERED";

            session.pdu =
                    "INACTIVE";

            session.cellularRegistered =
                    false;

            session.cellularStatus =
                    "Camped on serving cell";

            return;
        }

        if (elapsed < 26L) {
            session.rrc =
                    "RRC_CONNECTING";

            session.nas =
                    "REGISTERING";

            session.pdu =
                    "INACTIVE";

            session.cellularRegistered =
                    false;

            session.cellularStatus =
                    "RACH / RRC / NAS registration";

            return;
        }

        session.rrc =
                "RRC_CONNECTED";

        session.nas =
                "REGISTERED";

        session.cellularRegistered =
                true;

        if (best.rsrpDbm()
                >= CELL_DATA_MIN_RSRP_DBM
                && best.sinrDb()
                >= CELL_DATA_MIN_SINR_DB
                && best.generation()
                .packetData()) {
            session.pdu =
                    "ACTIVE";

            session.cellularStatus =
                    PhoneWirelessLinkMath
                            .cellularQuality(
                                    best.rsrpDbm(),
                                    best.sinrDb()
                            );
        } else {
            session.pdu =
                    "INACTIVE";

            session.cellularStatus =
                    "Registered, but radio quality is insufficient for packet data";
        }
    }

    private static List<WifiCandidate> scanWifi(
            ServerPlayer player
    ) {
        ServerLevel level =
                player.serverLevel();

        Vec3 receiver =
                phonePosition(
                        player
                );

        UUID phoneRadioId =
                phoneRadioId(
                        player,
                        "wifi"
                );

        List<WifiSeed> seeds =
                new ArrayList<>();

        for (ISignalReceiver receiverDevice
                : SignalBus.receiversInLevel(
                level
        )) {
            if (!(receiverDevice
                    instanceof NetworkDeviceBlockEntity device)
                    || device.wifiMode()
                    != WifiMode.ACCESS_POINT) {
                continue;
            }

            CompoundTag mac =
                    device.getUpdateTag()
                            .getCompound(
                                    "WifiMac"
                            );

            String ssid =
                    mac.getString(
                            "ApSsid"
                    );

            if (ssid.isBlank()) {
                continue;
            }

            String security =
                    mac.getString(
                            "ApSecurity"
                    );

            PhoneWirelessLinkMath.RawLink link =
                    PhoneWirelessLinkMath
                            .measureWifi(
                                    level,
                                    device,
                                    receiver,
                                    phoneRadioId
                            );

            seeds.add(
                    new WifiSeed(
                            device,
                            ssid,
                            security.isBlank()
                                    ? "signality:open"
                                    : security,
                            link
                    )
            );
        }

        List<PhoneWirelessLinkMath.RawLink> allLinks =
                seeds.stream()
                        .map(
                                WifiSeed::link
                        )
                        .toList();

        List<WifiCandidate> result =
                new ArrayList<>();

        for (WifiSeed seed
                : seeds) {
            PhoneWirelessLinkMath.RawLink link =
                    seed.link();

            double sinr =
                    PhoneWirelessLinkMath
                            .sinrDb(
                                    link,
                                    allLinks
                            );

            int rssi =
                    (int) Math.round(
                            link.receivedPowerDbm()
                    );

            if (link.distanceBlocks()
                    > link.maximumRangeBlocks()
                    || rssi < WIFI_SCAN_MIN_RSSI_DBM
                    || sinr < -10.0) {
                continue;
            }

            String bssid =
                    safe(
                            seed.device()
                                    .wifiMacAddress()
                    );

            if (bssid.isBlank()) {
                bssid =
                        syntheticBssid(
                                seed.device()
                                        .id()
                        );
            }

            String gateway =
                    safe(
                            seed.device()
                                    .wifiIpAddress()
                    );

            result.add(
                    new WifiCandidate(
                            seed.device()
                                    .id(),
                            seed.device(),
                            seed.ssid(),
                            bssid,
                            seed.security(),
                            !isOpenSecurity(
                                    seed.security()
                            ),
                            rssi,
                            sinr,
                            PhoneWirelessLinkMath
                                    .wifiChannel(
                                            link.frequencyHz()
                                    ),
                            link.frequencyHz(),
                            seed.device()
                                    .networkProfile()
                                    .protocol(),
                            link.distanceBlocks(),
                            PhoneWirelessLinkMath
                                    .wifiQuality(
                                            rssi,
                                            sinr
                                    ),
                            gateway
                    )
            );
        }

        result.sort(
                Comparator.comparingInt(
                        WifiCandidate::rssiDbm
                )
                        .reversed()
        );

        return List.copyOf(
                result
        );
    }

    private static List<CellCandidate> scanCells(
            ServerPlayer player
    ) {
        ServerLevel level =
                player.serverLevel();

        Vec3 receiver =
                phonePosition(
                        player
                );

        UUID phoneRadioId =
                phoneRadioId(
                        player,
                        "cellular"
                );

        List<CellSeed> seeds =
                new ArrayList<>();

        for (ISignalReceiver receiverDevice
                : SignalBus.receiversInLevel(
                level
        )) {
            if (!(receiverDevice
                    instanceof NetworkDeviceBlockEntity device)
                    || device.cellularMode()
                    != CellularMode.BASE_STATION) {
                continue;
            }

            CompoundTag ran =
                    device.getUpdateTag()
                            .getCompound(
                                    "CellularRan"
                            );

            PhoneWirelessLinkMath.RawLink link =
                    PhoneWirelessLinkMath
                            .measureCellular(
                                    level,
                                    device,
                                    receiver,
                                    phoneRadioId
                            );

            CellularGeneration generation =
                    device.cellularGeneration();

            CellularBand band =
                    CellularBandCatalog
                            .bestMatch(
                                    generation,
                                    link.frequencyHz()
                            );

            seeds.add(
                    new CellSeed(
                            device,
                            ran,
                            generation,
                            band,
                            link
                    )
            );
        }

        List<PhoneWirelessLinkMath.RawLink> allLinks =
                seeds.stream()
                        .map(
                                CellSeed::link
                        )
                        .toList();

        List<CellCandidate> result =
                new ArrayList<>();

        for (CellSeed seed
                : seeds) {
            PhoneWirelessLinkMath.RawLink link =
                    seed.link();

            if (link.distanceBlocks()
                    > link.maximumRangeBlocks()) {
                continue;
            }

            double sinr =
                    PhoneWirelessLinkMath
                            .sinrDb(
                                    link,
                                    allLinks
                            );

            int rsrp =
                    (int) Math.round(
                            PhoneWirelessLinkMath
                                    .rsrpDbm(
                                            link
                                    )
                    );

            double rsrq =
                    PhoneWirelessLinkMath
                            .rsrqDb(
                                    link,
                                    rsrp
                            );

            if (rsrp < -135
                    || sinr < -15.0) {
                continue;
            }

            long identity =
                    seed.ran()
                            .getLong(
                                    "CellIdentity"
                            );

            int pci =
                    seed.ran()
                            .getInt(
                                    "PhysicalCellId"
                            );

            String plmn =
                    seed.ran()
                            .getString(
                                    "Plmn"
                            );

            if (plmn.isBlank()) {
                plmn =
                        "00101";
            }

            String band =
                    seed.band() == null
                            ? "UNKNOWN"
                            : seed.band()
                            .id();

            double estimated =
                    PhoneWirelessLinkMath
                            .estimatedDownlinkMbps(
                                    link,
                                    sinr
                            );

            result.add(
                    new CellCandidate(
                            seed.device()
                                    .id(),
                            seed.generation(),
                            band,
                            pci,
                            identity,
                            plmn,
                            rsrp,
                            rsrq,
                            sinr,
                            link.distanceBlocks(),
                            PhoneWirelessLinkMath
                                    .cellularQuality(
                                            rsrp,
                                            sinr
                                    ),
                            estimated
                    )
            );
        }

        return List.copyOf(
                result
        );
    }

    private static PhoneWirelessSnapshot snapshot(
            Session session,
            List<WifiCandidate> wifiCandidates
    ) {
        List<PhoneWirelessSnapshot.WifiNetwork> networks =
                wifiCandidates
                        .stream()
                        .limit(
                                32
                        )
                        .map(
                                value ->
                                        new PhoneWirelessSnapshot.WifiNetwork(
                                                value.ssid(),
                                                value.bssid(),
                                                value.security(),
                                                value.locked(),
                                                value.rssiDbm(),
                                                value.sinrDb(),
                                                value.channel(),
                                                value.frequencyHz(),
                                                value.phy(),
                                                value.distanceBlocks(),
                                                value.quality()
                                        )
                        )
                        .toList();

        PhoneWirelessSnapshot.WifiStatus wifi =
                wifiStatus(
                        session,
                        wifiCandidates
                );

        PhoneWirelessSnapshot.CellularStatus cellular =
                cellularStatus(
                        session
                );

        return new PhoneWirelessSnapshot(
                networks,
                wifi,
                cellular
        );
    }

    private static PhoneWirelessSnapshot.WifiStatus wifiStatus(
            Session session,
            List<WifiCandidate> candidates
    ) {
        if (!session.wifiEnabled) {
            return PhoneWirelessSnapshot
                    .WifiStatus
                    .off();
        }

        String activeBssid =
                !session.connectedBssid
                        .isBlank()
                        ? session.connectedBssid
                        : session.pendingBssid;

        WifiCandidate active =
                findWifi(
                        candidates,
                        activeBssid
                );

        if (active == null) {
            return new PhoneWirelessSnapshot.WifiStatus(
                    true,
                    false,
                    session.wifiStage,
                    "",
                    "",
                    "",
                    -127,
                    Double.NEGATIVE_INFINITY,
                    0,
                    0.0,
                    "",
                    Double.POSITIVE_INFINITY,
                    "NO SIGNAL",
                    "",
                    "",
                    "",
                    "",
                    session.wifiStatus
            );
        }

        boolean connected =
                !session.connectedBssid
                        .isBlank()
                        && session.wifiStage
                        .equals(
                                "CONNECTED"
                        );

        return new PhoneWirelessSnapshot.WifiStatus(
                true,
                connected,
                session.wifiStage,
                active.ssid(),
                active.bssid(),
                active.security(),
                active.rssiDbm(),
                active.sinrDb(),
                active.channel(),
                active.frequencyHz(),
                active.phy(),
                active.distanceBlocks(),
                active.quality(),
                connected
                        ? session.wifiIp
                        : "",
                connected
                        ? session.wifiSubnet
                        : "",
                connected
                        ? session.wifiGateway
                        : "",
                connected
                        ? session.wifiDns
                        : "",
                session.wifiStatus
        );
    }

    private static PhoneWirelessSnapshot.CellularStatus cellularStatus(
            Session session
    ) {
        if (!session.cellularEnabled) {
            return PhoneWirelessSnapshot
                    .CellularStatus
                    .off();
        }

        CellCandidate cell =
                session.cellCandidate;

        if (cell == null) {
            return new PhoneWirelessSnapshot.CellularStatus(
                    true,
                    false,
                    "No Service",
                    "",
                    "",
                    0,
                    0,
                    0,
                    "",
                    "",
                    "",
                    "",
                    0,
                    session.rrc,
                    session.nas,
                    session.pdu,
                    -140,
                    -30.0,
                    -30.0,
                    Double.POSITIVE_INFINITY,
                    "NO SERVICE",
                    0.0,
                    "Not provisioned - ground-terminal SATCOM backhaul only",
                    session.cellularStatus
            );
        }

        String carrier =
                carrierName(
                        cell.plmn()
                );

        String radio =
                radioLabel(
                        cell.generation()
                );

        String architecture =
                architecture(
                        cell.generation()
                );

        int gnbId =
                (int) (
                        Math.abs(
                                cell.cellIdentity()
                        )
                                % 1_000_000L
                );

        int tac =
                100
                        + (
                        int
                        ) (
                        Math.abs(
                                cell.cellIdentity()
                        )
                                % 900L
                );

        String ip =
                session.cellularRegistered
                        && "ACTIVE".equals(
                        session.pdu
                )
                        ? session.cellularIp
                        : "";

        return new PhoneWirelessSnapshot.CellularStatus(
                true,
                session.cellularRegistered,
                carrier,
                radio,
                architecture,
                gnbId,
                cell.physicalCellId(),
                tac,
                cell.plmn(),
                cell.band(),
                ip,
                "internet",
                9,
                session.rrc,
                session.nas,
                session.pdu,
                cell.rsrpDbm(),
                cell.rsrqDb(),
                cell.sinrDb(),
                cell.distanceBlocks(),
                cell.quality(),
                cell.estimatedDownlinkMbps(),
                "Not provisioned - ground-terminal SATCOM backhaul only",
                session.cellularStatus
        );
    }

    private static void assignWifiLease(
            ServerPlayer player,
            Session session,
            WifiCandidate candidate
    ) {
        String gateway =
                candidate.gateway();

        String[] parts =
                gateway == null
                        ? new String[0]
                        : gateway.split(
                        "\\."
                );

        int host =
                100
                        + Math.floorMod(
                        player.getUUID()
                                .hashCode(),
                        120
                );

        if (parts.length == 4) {
            session.wifiIp =
                    parts[0]
                            + "."
                            + parts[1]
                            + "."
                            + parts[2]
                            + "."
                            + host;

            session.wifiGateway =
                    gateway;

            session.wifiDns =
                    gateway;
        } else {
            session.wifiIp =
                    "10.0.1."
                            + host;

            session.wifiGateway =
                    "10.0.1.1";

            session.wifiDns =
                    "10.0.1.1";
        }

        session.wifiSubnet =
                "255.255.255.0";
    }

    private static void disconnectWifi(
            Session session,
            String status
    ) {
        session.connectedBssid =
                "";

        session.pendingBssid =
                "";

        session.connectedNetworkKey =
                "";

        session.wifiStage =
                session.wifiEnabled
                        ? "SCANNING"
                        : "IDLE";

        session.wifiStatus =
                safe(
                        status
                );

        session.wifiIp =
                "";

        session.wifiSubnet =
                "";

        session.wifiGateway =
                "";

        session.wifiDns =
                "";

        session.wifiMisses =
                0;
    }

    private static void resetCellular(
            Session session,
            String status
    ) {
        session.servingCellId =
                null;

        session.cellCandidate =
                null;

        session.cellularRegistered =
                false;

        session.rrc =
                session.cellularEnabled
                        ? "IDLE"
                        : "OFF";

        session.nas =
                "DEREGISTERED";

        session.pdu =
                "INACTIVE";

        session.cellularStatus =
                safe(
                        status
                );
    }

    private static WifiCandidate findWifi(
            List<WifiCandidate> candidates,
            String bssid
    ) {
        if (bssid == null
                || bssid.isBlank()) {
            return null;
        }

        return candidates.stream()
                .filter(
                        value ->
                                value.bssid()
                                        .equalsIgnoreCase(
                                                bssid
                                        )
                )
                .findFirst()
                .orElse(
                        null
                );
    }

    private static Vec3 phonePosition(
            ServerPlayer player
    ) {
        return player.position()
                .add(
                        0.0,
                        1.35,
                        0.0
                );
    }

    private static UUID phoneRadioId(
            ServerPlayer player,
            String suffix
    ) {
        return UUID.nameUUIDFromBytes(
                (
                        "vsia-phone-"
                                + suffix
                                + ":"
                                + player.getUUID()
                )
                        .getBytes(
                                StandardCharsets.UTF_8
                        )
        );
    }

    private static String wifiCredentialKey(
            WifiCandidate candidate
    ) {
        if (candidate == null) {
            return "";
        }

        return safe(
                candidate.ssid()
        )
                + "\u0000"
                + safe(
                candidate.security()
        );
    }

    private static boolean wifiCredentialMatches(
            NetworkDeviceBlockEntity device,
            String supplied
    ) {
        if (device == null) {
            return false;
        }

        try {
            Field wifiMacField =
                    NetworkDeviceBlockEntity.class
                            .getDeclaredField(
                                    "wifiMac"
                            );

            wifiMacField.setAccessible(
                    true
            );

            Object controller =
                    wifiMacField.get(
                            device
                    );

            if (!(controller
                    instanceof WifiMacController)) {
                return false;
            }

            Field passphraseField =
                    WifiMacController.class
                            .getDeclaredField(
                                    "apPassphrase"
                            );

            passphraseField.setAccessible(
                    true
            );

            Object configuredValue =
                    passphraseField.get(
                            controller
                    );

            String configured =
                    configuredValue instanceof String
                            ? (String) configuredValue
                            : "";

            byte[] expected =
                    configured.getBytes(
                            StandardCharsets.UTF_8
                    );

            byte[] actual =
                    safe(
                            supplied
                    )
                            .getBytes(
                                    StandardCharsets.UTF_8
                            );

            return MessageDigest.isEqual(
                    expected,
                    actual
            );
        } catch (ReflectiveOperationException
                 | RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isOpenSecurity(
            String security
    ) {
        String normalized =
                safe(
                        security
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        return normalized.isBlank()
                || normalized.contains(
                "open"
        )
                || normalized.contains(
                "none"
        );
    }

    private static String syntheticBssid(
            UUID id
    ) {
        long value =
                id == null
                        ? 0L
                        : id.getLeastSignificantBits();

        return String.format(
                Locale.ROOT,
                "02:%02X:%02X:%02X:%02X:%02X",
                (
                        value >>> 32
                )
                        & 0xFF,
                (
                        value >>> 24
                )
                        & 0xFF,
                (
                        value >>> 16
                )
                        & 0xFF,
                (
                        value >>> 8
                )
                        & 0xFF,
                value
                        & 0xFF
        );
    }

    private static String carrierName(
            String plmn
    ) {
        if ("00101".equals(
                plmn
        )) {
            return "VSIA Mobile";
        }

        return plmn == null
                || plmn.isBlank()
                ? "Unknown Carrier"
                : "PLMN "
                + plmn;
    }

    private static String radioLabel(
            CellularGeneration generation
    ) {
        return switch (generation) {
            case G1_ANALOG -> "1G";
            case G2_GSM -> "2G";
            case G3_UMTS -> "3G";
            case G4_LTE -> "LTE";
            case G5_NR -> "5G";
        };
    }

    private static String architecture(
            CellularGeneration generation
    ) {
        return switch (generation) {
            case G1_ANALOG -> "AMPS";
            case G2_GSM -> "GSM";
            case G3_UMTS -> "UMTS";
            case G4_LTE -> "EPC";
            case G5_NR -> "SA";
        };
    }

    private static String safe(
            String value
    ) {
        return value == null
                ? ""
                : value;
    }

    public record AccessDecision(
            boolean allowed,
            String detail
    ) {
        public static AccessDecision allow(
                String detail
        ) {
            return new AccessDecision(
                    true,
                    safe(
                            detail
                    )
            );
        }

        public static AccessDecision reject(
                String detail
        ) {
            return new AccessDecision(
                    false,
                    safe(
                            detail
                    )
            );
        }
    }

    private record WifiSeed(
            NetworkDeviceBlockEntity device,
            String ssid,
            String security,
            PhoneWirelessLinkMath.RawLink link
    ) {
    }

    private record WifiCandidate(
            UUID deviceId,
            NetworkDeviceBlockEntity device,
            String ssid,
            String bssid,
            String security,
            boolean locked,
            int rssiDbm,
            double sinrDb,
            int channel,
            double frequencyHz,
            String phy,
            double distanceBlocks,
            String quality,
            String gateway
    ) {
    }

    private record CellSeed(
            NetworkDeviceBlockEntity device,
            CompoundTag ran,
            CellularGeneration generation,
            CellularBand band,
            PhoneWirelessLinkMath.RawLink link
    ) {
    }

    private record CellCandidate(
            UUID deviceId,
            CellularGeneration generation,
            String band,
            int physicalCellId,
            long cellIdentity,
            String plmn,
            int rsrpDbm,
            double rsrqDb,
            double sinrDb,
            double distanceBlocks,
            String quality,
            double estimatedDownlinkMbps
    ) {
        private double selectionScore() {
            return rsrpDbm
                    + Math.max(
                    -10.0,
                    Math.min(
                            20.0,
                            sinrDb
                    )
            )
                    * 0.25;
        }
    }

    private static final class Session {
        private boolean wifiEnabled =
                true;

        private boolean cellularEnabled =
                true;

        private String pendingBssid =
                "";

        private String connectedBssid =
                "";

        private String wifiStage =
                "SCANNING";

        private long wifiStageStartTick;

        private String wifiStatus =
                "Scanning for nearby access points";

        private int wifiMisses;

        private String wifiIp =
                "";

        private String wifiSubnet =
                "";

        private String wifiGateway =
                "";

        private String wifiDns =
                "";

        private final Map<String, String>
                rememberedWifiPassphrases =
                new HashMap<>();

        private final Set<String>
                knownWifiNetworks =
                new HashSet<>();

        private final Map<String, Boolean>
                autoJoinPreferences =
                new HashMap<>();

        private String connectedNetworkKey =
                "";

        private UUID servingCellId;

        private long cellAttachStartTick;

        private CellCandidate cellCandidate;

        private boolean cellularRegistered;

        private String rrc =
                "IDLE";

        private String nas =
                "DEREGISTERED";

        private String pdu =
                "INACTIVE";

        private String cellularStatus =
                "Searching for cellular service";

        private String cellularIp =
                "10.44.0.18";

        private PhoneWirelessSnapshot lastSnapshot =
                new PhoneWirelessSnapshot(
                        List.of(),
                        PhoneWirelessSnapshot
                                .WifiStatus
                                .off(),
                        PhoneWirelessSnapshot
                                .CellularStatus
                                .off()
                );
    }
}
