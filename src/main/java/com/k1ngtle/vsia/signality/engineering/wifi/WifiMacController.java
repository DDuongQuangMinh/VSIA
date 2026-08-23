package com.k1ngtle.vsia.signality.engineering.wifi;

import com.k1ngtle.vsia.signality.engineering.wifi.security.WifiHandshakeMicMaterial;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class WifiMacController {
    public static final int FC_ASSOC_REQ = 0x0000;
    public static final int FC_ASSOC_RESP = 0x0010;
    public static final int FC_PROBE_REQ = 0x0040;
    public static final int FC_PROBE_RESP = 0x0050;
    public static final int FC_BEACON = 0x0080;
    public static final int FC_AUTH = 0x00B0;
    public static final int FC_DATA = 0x0008;
    public static final int FC_RTS = 0x00B4;
    public static final int FC_CTS = 0x00C4;
    public static final int FC_ACK = 0x00D4;

    public static final int FC_TO_DS = 0x0100;
    public static final int FC_FROM_DS = 0x0200;
    public static final int FC_RETRY = 0x0800;

    public static final String BROADCAST =
            "FF:FF:FF:FF:FF:FF";

    private static final int MAX_RETRIES = 4;

    private enum PendingStage {
        WAIT_AIFS,
        BACKOFF,
        WAIT_CTS,
        WAIT_ACK
    }

    private record PendingTransmission(
            int sequence,
            int frameControl,
            String address1,
            String address2,
            String address3,
            CompoundTag body,
            WifiAccessCategory category,
            Sender sender,
            int attempt,
            PendingStage stage,
            long nextActionMicros,
            int backoffSlots,
            boolean useRts
    ) {
    }

    @FunctionalInterface
    public interface Sender {
        void send(WifiMacFrame frame);

        default boolean mediumBusy() {
            return false;
        }
    }

    private final Random random =
            new Random();

    private final EdcaController edca =
            new EdcaController(
                    random
            );

    private WifiMacTimingProfile timing =
            WifiMacTimingProfile.ofdmDefault();

    private final WifiNavState nav =
            new WifiNavState();

    private final Map<String, WifiNetworkRecord> discovered =
            new LinkedHashMap<>();

    // W1.21.2 DETERMINISTIC BSSID + AP AGING
    // Scan results are transient BSS observations, not permanent configuration.
    private static final long DISCOVERED_NETWORK_MAX_AGE_NANOS =
            5_000_000_000L;

    private final Map<String, Double> discoveredSnrDb =
            new HashMap<>();

    private final Set<String> authenticated =
            new LinkedHashSet<>();

    private final Set<String> associated =
            new LinkedHashSet<>();

    private final Set<String> securedStations =
            new LinkedHashSet<>();

    private final Map<String, byte[]> apPtkByStation =
            new HashMap<>();

    private final Map<String, byte[]> pendingAnonceByStation =
            new HashMap<>();

    private WifiMode mode =
            WifiMode.LEGACY_DIRECT;

    private WifiStationState stationState =
            WifiStationState.DISCONNECTED;

    private WifiSecurityState securityState =
            WifiSecurityState.OPEN;

    private String apSsid = "";
    private String apSecurity = "signality:open";
    private String apPassphrase = "";

    private String stationPassphrase = "";

    private String selectedSsid = "";
    private String selectedBssid = "";
    private String selectedSecurity = "signality:open";

    private byte[] stationAnonce;
    private byte[] stationSnonce;
    private byte[] stationPtk;

    private int sequenceNumber;
    private int lastAckedSequence = -1;

    private final Map<Integer, PendingTransmission> pendingTransmissions =
            new LinkedHashMap<>();

    private final Map<String, Integer> lastDeliveredSequenceBySender =
            new HashMap<>();

    private double lastObservedSnrDb =
            Double.NEGATIVE_INFINITY;

    private int currentMcsIndex;

    private String lastSecurityDiagnostic =
            "IDLE";

    public WifiMode mode() {
        return mode;
    }

    public WifiStationState stationState() {
        return stationState;
    }

    public WifiSecurityState securityState() {
        return securityState;
    }

    public String lastSecurityDiagnostic() {
        return lastSecurityDiagnostic;
    }

    public Collection<WifiNetworkRecord> discoveredNetworks() {
        pruneStaleDiscoveredNetworks();

        return List.copyOf(
                discovered.values()
        );
    }

    public WifiNetworkRecord bestDiscoveredNetwork() {
        pruneStaleDiscoveredNetworks();

        return discovered.values()
                .stream()
                .sorted(
                        this::compareDiscoveredNetworks
                )
                .findFirst()
                .orElse(
                        null
                );
    }

    public WifiNetworkRecord bestDiscoveredNetworkForSsid(
            String ssid
    ) {
        pruneStaleDiscoveredNetworks();

        if (ssid == null
                || ssid.isBlank()) {
            return null;
        }

        return discovered.values()
                .stream()
                .filter(
                        value ->
                                ssid.equals(
                                        value.ssid()
                                )
                )
                .sorted(
                        this::compareDiscoveredNetworks
                )
                .findFirst()
                .orElse(
                        null
                );
    }

    public double discoveredNetworkSnrDb(
            String bssid
    ) {
        if (bssid == null
                || bssid.isBlank()) {
            return Double.NEGATIVE_INFINITY;
        }

        try {
            return discoveredSnrDb.getOrDefault(
                    normalizeMac(
                            bssid
                    ),
                    Double.NEGATIVE_INFINITY
            );
        } catch (IllegalArgumentException ignored) {
            return Double.NEGATIVE_INFINITY;
        }
    }

    public void rememberKnownNetwork(
            WifiNetworkRecord network
    ) {
        requireStation();

        if (network == null
                || network.ssid() == null
                || network.ssid().isBlank()
                || network.bssid() == null
                || network.bssid().isBlank()) {
            throw new IllegalArgumentException(
                    "Known Wi-Fi network is incomplete"
            );
        }

        discovered.put(
                normalizeMac(
                        network.bssid()
                ),
                network
        );

        if (stationState
                == WifiStationState.DISCONNECTED
                || stationState
                == WifiStationState.SCANNING) {
            stationState =
                    WifiStationState.SCANNING;
        }

        lastSecurityDiagnostic =
                "KNOWN_AP_PROVISIONED";
    }

    public Set<String> associatedStations() {
        return Collections.unmodifiableSet(
                associated
        );
    }

    public int pendingDataTransmissions() {
        return pendingTransmissions.size();
    }

    public List<String> pendingTransmissionDiagnostics() {
        if (pendingTransmissions.isEmpty()) {
            return List.of(
                    "PENDING none"
            );
        }

        long nowMicros =
                WifiMacTimingScheduler.nowMicros();

        List<String> out =
                new ArrayList<>();

        for (PendingTransmission pending
                : pendingTransmissions.values()) {
            if (out.size() >= 6) {
                break;
            }

            out.add(
                    "seq="
                            + pending.sequence()
                            + " "
                            + pendingFrameLabel(
                                    pending
                            )
                            + " AC="
                            + pending.category()
                            + " stage="
                            + pending.stage()
                            + " try="
                            + pending.attempt()
                            + " bo="
                            + pending.backoffSlots()
                            + " dueUs="
                            + Math.max(
                                    0L,
                                    pending.nextActionMicros()
                                            - nowMicros
                            )
            );
        }

        if (pendingTransmissions.size()
                > out.size()) {
            out.add(
                    "... +"
                            + (
                            pendingTransmissions.size()
                                    - out.size()
                    )
            );
        }

        return List.copyOf(
                out
        );
    }

    private String pendingFrameLabel(
            PendingTransmission pending
    ) {
        int fc =
                pending.frameControl()
                        & 0x00FC;

        if (fc == (FC_DATA & 0x00FC)) {
            if ("EAPOL_KEY".equals(
                    pending.body()
                            .getString(
                                    "wifi_control"
                            )
            )) {
                return "EAPOL_M"
                        + pending.body()
                        .getInt(
                                "message"
                        );
            }

            return "DATA";
        }

        if (fc == (FC_AUTH & 0x00FC)) {
            return "AUTH";
        }

        if (fc == (FC_ASSOC_REQ & 0x00FC)) {
            return "ASSOC_REQ";
        }

        if (fc == (FC_ASSOC_RESP & 0x00FC)) {
            return "ASSOC_RESP";
        }

        return String.format(
                Locale.ROOT,
                "FC_%02X",
                fc
        );
    }

    public boolean accessPointSecurityHandshakeInProgress() {
        return mode == WifiMode.ACCESS_POINT
                && !pendingAnonceByStation.isEmpty();
    }

    public WifiMacTimingProfile timingProfile() {
        return timing;
    }

    public long navRemainingMicros() {
        return nav.remainingMicros(
                WifiMacTimingScheduler.nowMicros()
        );
    }

    public boolean isAssociated() {
        return mode == WifiMode.STATION
                && stationState == WifiStationState.ASSOCIATED;
    }

    public boolean isSecured() {
        if (isOpenSecurity(selectedSecurity)) {
            return isAssociated();
        }

        return isAssociated()
                && securityState == WifiSecurityState.SECURED;
    }

    public void provisionLabAccessPoint(
            String ssid
    ) {
        configureAccessPoint(
                ssid,
                "signality:open",
                ""
        );

        lastSecurityDiagnostic =
                "LAB_AP_PROVISIONED_OPEN";
    }

    public void provisionLabStationLink(
            String ssid,
            String bssid
    ) {
        requireStation();

        if (ssid == null
                || ssid.isBlank()
                || bssid == null
                || bssid.isBlank()) {
            throw new IllegalArgumentException(
                    "Lab station link requires SSID and BSSID"
            );
        }

        selectedSsid =
                ssid;

        selectedBssid =
                normalizeMac(
                        bssid
                );

        selectedSecurity =
                "signality:open";

        stationState =
                WifiStationState.ASSOCIATED;

        securityState =
                WifiSecurityState.OPEN;

        lastSecurityDiagnostic =
                "LAB_STATION_LINK_PROVISIONED";
    }

    public void provisionLabAssociatedStation(
            String stationMac
    ) {
        if (mode != WifiMode.ACCESS_POINT) {
            throw new IllegalStateException(
                    "Lab AP association requires ACCESS_POINT mode"
            );
        }

        String normalized =
                normalizeMac(
                        stationMac
                );

        authenticated.add(
                normalized
        );

        associated.add(
                normalized
        );

        securedStations.remove(
                normalized
        );

        lastSecurityDiagnostic =
                "LAB_AP_STATION_PROVISIONED";
    }

    public double lastObservedSnrDb() {
        return lastObservedSnrDb;
    }

    public int currentMcsIndex() {
        return currentMcsIndex;
    }

    public void observeSnr(
            String protocol,
            double snrDb
    ) {
        lastObservedSnrDb =
                snrDb;

        timing =
                WifiMacTimingProfile.forProtocol(
                        protocol
                );

        currentMcsIndex =
                WifiMcsTable
                        .select(
                                protocol,
                                snrDb
                        )
                        .index();
    }

    public void useLegacyDirectMode() {
        mode = WifiMode.LEGACY_DIRECT;
        resetStation();
        lastSecurityDiagnostic =
                "LEGACY_DIRECT";
        authenticated.clear();
        associated.clear();
        securedStations.clear();
        apPtkByStation.clear();
        pendingAnonceByStation.clear();
    }

    public void configureStation() {
        configureStation("");
    }

    public void configureStation(
            String passphrase
    ) {
        mode = WifiMode.STATION;
        lastSecurityDiagnostic =
                "STATION_CONFIGURED";
        stationPassphrase =
                passphrase == null
                        ? ""
                        : passphrase;

        resetStation();
        discovered.clear();
    }

    public void configureAccessPoint(
            String ssid,
            String security
    ) {
        configureAccessPoint(
                ssid,
                security,
                ""
        );
    }

    public void configureAccessPoint(
            String ssid,
            String security,
            String passphrase
    ) {
        if (ssid == null || ssid.isBlank()) {
            throw new IllegalArgumentException(
                    "SSID cannot be blank"
            );
        }

        mode = WifiMode.ACCESS_POINT;
        lastSecurityDiagnostic =
                "AP_CONFIGURED";
        apSsid = ssid;
        apSecurity =
                security == null || security.isBlank()
                        ? "signality:open"
                        : security;

        apPassphrase =
                passphrase == null
                        ? ""
                        : passphrase;

        resetStation();

        authenticated.clear();
        associated.clear();
        securedStations.clear();
        apPtkByStation.clear();
        pendingAnonceByStation.clear();
    }

    // W1.21 FULL V6.5 SINGLE-PLAYER WIFI
    public void beginScan() {
        requireStation();

        discovered.clear();
        discoveredSnrDb.clear();

        stationState =
                WifiStationState.SCANNING;

        lastSecurityDiagnostic =
                "SCAN_ACTIVE";
    }

    public void sendScanProbe(
            String ownMac,
            Sender sender
    ) {
        requireStation();

        if (stationState != WifiStationState.SCANNING) {
            stationState =
                    WifiStationState.SCANNING;
        }

        CompoundTag body =
                new CompoundTag();

        body.putString(
                "ssid",
                ""
        );

        sender.send(
                newFrame(
                        FC_PROBE_REQ,
                        BROADCAST,
                        ownMac,
                        BROADCAST,
                        body
                )
        );
    }

    public void finishScan() {
        requireStation();

        if (stationState == WifiStationState.SCANNING) {
            stationState =
                    WifiStationState.DISCONNECTED;
        }

        lastSecurityDiagnostic =
                "SCAN_COMPLETE_APS_"
                        + discovered.size();
    }

    public void startScan(
            String ownMac,
            Sender sender
    ) {
        beginScan();
        sendScanProbe(
                ownMac,
                sender
        );
    }

    public boolean connect(
            String ownMac,
            String ssid,
            Sender sender
    ) {
        requireStation();

        WifiNetworkRecord network =
                bestDiscoveredNetworkForSsid(
                        ssid
                );

        if (network == null) {
            return false;
        }

        selectedSsid =
                network.ssid();

        selectedBssid =
                network.bssid();

        selectedSecurity =
                network.security();

        // W1.21.1 AUTH-ASSOC ORDER FIX
        // WPA/EAPOL starts only after the AP accepts 802.11 association.
        securityState =
                WifiSecurityState.OPEN;

        stationState =
                WifiStationState.AUTHENTICATING;

        lastSecurityDiagnostic =
                "AUTH_TX_WAITING_RESPONSE";

        CompoundTag body =
                new CompoundTag();

        body.putInt(
                "algorithm",
                0
        );

        body.putInt(
                "transaction_sequence",
                1
        );

        body.putInt(
                "status_code",
                0
        );

        sender.send(
                newFrame(
                        FC_AUTH,
                        selectedBssid,
                        ownMac,
                        selectedBssid,
                        body
                )
        );

        return true;
    }

    public boolean connect(
            String ownMac,
            String ssid,
            String preferredBssid,
            Sender sender
    ) {
        requireStation();
        pruneStaleDiscoveredNetworks();

        if (preferredBssid == null
                || preferredBssid.isBlank()) {
            return connect(
                    ownMac,
                    ssid,
                    sender
            );
        }

        final String normalizedBssid;

        try {
            normalizedBssid =
                    normalizeMac(
                            preferredBssid
                    );
        } catch (IllegalArgumentException ignored) {
            lastSecurityDiagnostic =
                    "CONNECT_BSSID_INVALID";
            return false;
        }

        WifiNetworkRecord network =
                discovered.get(
                        normalizedBssid
                );

        if (network == null
                || ssid == null
                || !ssid.equals(
                        network.ssid()
                )) {
            lastSecurityDiagnostic =
                    "CONNECT_BSSID_NOT_DISCOVERED";
            return false;
        }

        selectedSsid =
                network.ssid();

        selectedBssid =
                network.bssid();

        selectedSecurity =
                network.security();

        securityState =
                WifiSecurityState.OPEN;

        stationState =
                WifiStationState.AUTHENTICATING;

        lastSecurityDiagnostic =
                "AUTH_TX_BSSID_"
                        + normalizedBssid.replace(
                        ":",
                        ""
                );

        CompoundTag body =
                new CompoundTag();

        body.putInt(
                "algorithm",
                0
        );

        body.putInt(
                "transaction_sequence",
                1
        );

        body.putInt(
                "status_code",
                0
        );

        sender.send(
                newFrame(
                        FC_AUTH,
                        selectedBssid,
                        ownMac,
                        selectedBssid,
                        body
                )
        );

        return true;
    }

    public void sendBeacon(
            String ownMac,
            String profileId,
            double frequencyHz,
            Sender sender
    ) {
        if (mode
                != WifiMode.ACCESS_POINT) {
            return;
        }

        sender.send(
                newFrame(
                        FC_BEACON,
                        BROADCAST,
                        ownMac,
                        ownMac,
                        advertisementBody(
                                profileId,
                                frequencyHz
                        )
                )
        );
    }

    public boolean sendData(
            String ownMac,
            String targetMac,
            CompoundTag data,
            WifiAccessCategory category,
            Sender sender
    ) {
        if (mode == WifiMode.STATION) {
            if (!isAssociated()) {
                return false;
            }

            if (!isOpenSecurity(selectedSecurity)
                    && securityState
                    != WifiSecurityState.SECURED) {
                return false;
            }

            CompoundTag protectedBody =
                    protectStationData(
                            data
                    );

            return transmitWithRetry(
                    FC_DATA | FC_TO_DS,
                    selectedBssid,
                    ownMac,
                    targetMac,
                    protectedBody,
                    category,
                    sender
            );
        }

        if (mode == WifiMode.ACCESS_POINT) {
            String normalized =
                    normalizeMac(
                            targetMac
                    );

            if (!macEquals(
                    targetMac,
                    BROADCAST
            )
                    && !associated.contains(
                    normalized
            )) {
                return false;
            }

            CompoundTag protectedBody =
                    protectApData(
                            targetMac,
                            data
                    );

            return transmitWithRetry(
                    FC_DATA | FC_FROM_DS,
                    targetMac,
                    ownMac,
                    ownMac,
                    protectedBody,
                    category,
                    sender
            );
        }

        return false;
    }

    public CompoundTag receive(
            String ownMac,
            WifiMacFrame frame,
            String profileId,
            double frequencyHz,
            Sender sender
    ) {
        String receiver =
                formatMac(
                        frame.address1()
                );

        boolean addressedToUs =
                macEquals(
                        receiver,
                        ownMac
                );

        boolean broadcast =
                macEquals(
                        receiver,
                        BROADCAST
                );

        if (!addressedToUs
                && !broadcast
                && frame.durationId() > 0) {
            nav.observe(
                    WifiMacTimingScheduler.nowMicros(),
                    frame.durationId()
            );
        }

        int fc =
                frame.frameControl()
                        & 0x00FC;

        if (!addressedToUs
                && !broadcast) {
            return null;
        }

        if (fc == (FC_RTS & 0x00FC)) {
            if (addressedToUs) {
                sendCts(
                        ownMac,
                        frame,
                        sender
                );
            }

            return null;
        }

        if (fc == (FC_CTS & 0x00FC)) {
            PendingTransmission pending =
                    oldestPendingInStage(
                            PendingStage.WAIT_CTS
                    );

            if (pending != null) {
                sendPendingDataAfterCts(
                        pending
                );
            }

            return null;
        }

        if (fc == (FC_ACK & 0x00FC)) {
            PendingTransmission pending =
                    oldestPendingInStage(
                            PendingStage.WAIT_ACK
                    );

            if (pending != null) {
                lastAckedSequence =
                        pending.sequence();

                pendingTransmissions.remove(
                        pending.sequence()
                );

                edca.onSuccess(
                        pending.category()
                );
            }

            return null;
        }

        CompoundTag body;

        try {
            if (WifiManagementCodec.isManagementSubtype(
                    frame.frameControl()
            )) {
                body =
                        WifiManagementCodec.decodeBody(
                                frame.frameControl(),
                                frame.payload()
                        );
            } else {
                body =
                        decode(
                                frame.payload()
                        );
            }
        } catch (Exception ignored) {
            return null;
        }

        if (fc == (FC_BEACON & 0x00FC)
                || fc
                == (FC_PROBE_RESP & 0x00FC)) {
            handleAdvertisement(
                    frame,
                    body
            );

            return null;
        }

        if (fc == (FC_PROBE_REQ & 0x00FC)
                && mode
                == WifiMode.ACCESS_POINT) {
            handleProbeRequest(
                    ownMac,
                    frame,
                    body,
                    profileId,
                    frequencyHz,
                    sender
            );

            return null;
        }

        if (fc == (FC_AUTH & 0x00FC)) {
            handleAuthentication(
                    ownMac,
                    frame,
                    body,
                    sender
            );

            return null;
        }

        if (fc == (FC_ASSOC_REQ & 0x00FC)
                && mode
                == WifiMode.ACCESS_POINT) {
            handleAssociationRequest(
                    ownMac,
                    frame,
                    body,
                    sender
            );

            return null;
        }

        if (fc == (FC_ASSOC_RESP & 0x00FC)
                && mode
                == WifiMode.STATION) {
            handleAssociationResponse(
                    frame,
                    body
            );

            return null;
        }

        if (fc == (FC_DATA & 0x00FC)) {
            if (body.getString(
                    "wifi_control"
            ).equals(
                    "EAPOL_KEY"
            )) {
                sendAck(
                        ownMac,
                        frame,
                        sender
                );

                handleEapolKey(
                        ownMac,
                        frame,
                        body,
                        sender
                );

                return null;
            }

            if (!canReceiveData(
                    frame
            )) {
                return null;
            }

            String dataSender =
                    normalizeMac(
                            formatMac(
                                    frame.address2()
                            )
                    );

            int receivedSequence =
                    (frame.sequenceControl()
                            >>> 4)
                            & 0x0FFF;

            boolean retry =
                    (frame.frameControl()
                            & FC_RETRY)
                            != 0;

            boolean duplicateRetry =
                    retry
                            && lastDeliveredSequenceBySender
                            .getOrDefault(
                                    dataSender,
                                    -1
                            )
                            == receivedSequence;

            sendAck(
                    ownMac,
                    frame,
                    sender
            );

            if (duplicateRetry) {
                return null;
            }

            lastDeliveredSequenceBySender.put(
                    dataSender,
                    receivedSequence
            );

            CompoundTag receivedData =
                    unprotectReceivedData(
                            frame,
                            body
                    );

            if (receivedData == null) {
                return null;
            }

            boolean toDistributionSystem =
                    (
                            frame.frameControl()
                                    & FC_TO_DS
                    ) != 0;

            if (mode == WifiMode.ACCESS_POINT
                    && toDistributionSystem) {
                /*
                 * W1.20.6.1 EXTERNAL AP DISTRIBUTION BRIDGE
                 *
                 * Do not consume or internally relay STA -> DS DATA here.
                 * NetworkDeviceBlockEntity owns the W1.19 AP bridge and must
                 * receive the decoded payload together with the original
                 * 802.11 frame so that W1.20.2 can preserve Address2/Address3
                 * and W1.19 can make the authoritative forwarding decision.
                 *
                 * The previous implementation relayed non-local unicast frames
                 * inside WifiMacController and then returned null. That meant
                 * processWifiMacEnvelope(...) never called
                 * w119HandleWirelessData(...), so generated ARP/ICMP replies
                 * were visible at the station but never incremented AP
                 * wirelessRx/dsTx.
                 */
                receivedData.putBoolean(
                        "w12061_external_ap_bridge",
                        true
                );
            }

            return receivedData;
        }

        return null;
    }

    private boolean transmitWithRetry(
            int frameControl,
            String address1,
            String address2,
            String address3,
            CompoundTag body,
            WifiAccessCategory category,
            Sender sender
    ) {
        int sequence =
                sequenceNumber++ & 0x0FFF;

        int effectiveFrameControl =
                body.getBoolean(
                        "protected"
                )
                        ? frameControl
                        | 0x4000
                        : frameControl;

        boolean broadcast =
                macEquals(
                        address1,
                        BROADCAST
                );

        int bodyBytes =
                encode(
                        body
                ).length;

        boolean useRts =
                !broadcast
                        && bodyBytes
                        >= timing.rtsThresholdBytes();

        int backoffSlots =
                random.nextInt(
                        edca.contentionWindow(
                                category
                        ) + 1
                );

        long now =
                WifiMacTimingScheduler.nowMicros();

        PendingTransmission pending =
                new PendingTransmission(
                        sequence,
                        effectiveFrameControl,
                        address1,
                        address2,
                        address3,
                        body.copy(),
                        category,
                        sender,
                        0,
                        PendingStage.WAIT_AIFS,
                        now
                                + timing.aifsUs(
                                category
                        ),
                        backoffSlots,
                        useRts
                );

        pendingTransmissions.put(
                sequence,
                pending
        );

        WifiMacTimingScheduler.track(
                this
        );

        return true;
    }

    boolean onTimingTick(
            long currentMicros
    ) {
        if (pendingTransmissions.isEmpty()) {
            return false;
        }

        PendingTransmission[] snapshot =
                pendingTransmissions
                        .values()
                        .toArray(
                                PendingTransmission[]::new
                        );

        for (PendingTransmission snapshotPending : snapshot) {
            PendingTransmission current =
                    pendingTransmissions.get(
                            snapshotPending.sequence()
                    );

            if (current == null) {
                continue;
            }

            switch (current.stage()) {
                case WAIT_AIFS ->
                        processAifs(
                                current,
                                currentMicros
                        );

                case BACKOFF ->
                        processBackoff(
                                current,
                                currentMicros
                        );

                case WAIT_CTS,
                     WAIT_ACK ->
                        processResponseTimeout(
                                current,
                                currentMicros
                        );
            }
        }

        return !pendingTransmissions.isEmpty();
    }

    private void processAifs(
            PendingTransmission pending,
            long nowMicros
    ) {
        if (mediumUnavailable(
                pending,
                nowMicros
        )) {
            pendingTransmissions.put(
                    pending.sequence(),
                    copyPending(
                            pending,
                            PendingStage.WAIT_AIFS,
                            nowMicros
                                    + timing.aifsUs(
                                    pending.category()
                            ),
                            pending.backoffSlots()
                    )
            );

            return;
        }

        if (nowMicros
                < pending.nextActionMicros()) {
            return;
        }

        if (pending.backoffSlots()
                <= 0) {
            sendPendingAttempt(
                    pending
            );

            return;
        }

        PendingTransmission backoff =
                copyPending(
                        pending,
                        PendingStage.BACKOFF,
                        nowMicros
                                + timing.slotTimeUs(),
                        pending.backoffSlots()
                );

        pendingTransmissions.put(
                backoff.sequence(),
                backoff
        );
    }

    private void processBackoff(
            PendingTransmission pending,
            long nowMicros
    ) {
        if (mediumUnavailable(
                pending,
                nowMicros
        )) {
            pendingTransmissions.put(
                    pending.sequence(),
                    copyPending(
                            pending,
                            PendingStage.WAIT_AIFS,
                            nowMicros
                                    + timing.aifsUs(
                                    pending.category()
                            ),
                            pending.backoffSlots()
                    )
            );

            return;
        }

        if (nowMicros
                < pending.nextActionMicros()) {
            return;
        }

        long elapsed =
                nowMicros
                        - pending.nextActionMicros();

        int elapsedSlots =
                1
                        + (int) (
                        elapsed
                                / Math.max(
                                1,
                                timing.slotTimeUs()
                        )
                );

        int remaining =
                Math.max(
                        0,
                        pending.backoffSlots()
                                - elapsedSlots
                );

        if (remaining == 0) {
            sendPendingAttempt(
                    copyPending(
                            pending,
                            PendingStage.BACKOFF,
                            nowMicros,
                            0
                    )
            );

            return;
        }

        pendingTransmissions.put(
                pending.sequence(),
                copyPending(
                        pending,
                        PendingStage.BACKOFF,
                        pending.nextActionMicros()
                                + (
                                (long) elapsedSlots
                                        * timing.slotTimeUs()
                        ),
                        remaining
                )
        );
    }

    private void processResponseTimeout(
            PendingTransmission pending,
            long nowMicros
    ) {
        if (nowMicros
                < pending.nextActionMicros()) {
            return;
        }

        edca.onFailure(
                pending.category()
        );

        if (pending.attempt()
                >= MAX_RETRIES) {
            pendingTransmissions.remove(
                    pending.sequence()
            );

            if (isEapolKeyBody(
                    pending.body()
            )) {
                int message =
                        pending.body()
                                .getInt(
                                        "message"
                                );

                lastSecurityDiagnostic =
                        "EAPOL_M"
                                + message
                                + "_RETRY_LIMIT";

                if (mode == WifiMode.ACCESS_POINT
                        && (message == 1
                        || message == 3)) {
                    String station =
                            normalizeMac(
                                    pending.address1()
                            );

                    pendingAnonceByStation.remove(
                            station
                    );

                    apPtkByStation.remove(
                            station
                    );

                    securedStations.remove(
                            station
                    );

                    associated.remove(
                            station
                    );

                    authenticated.remove(
                            station
                    );
                }
            }

            return;
        }

        int nextAttempt =
                pending.attempt() + 1;

        int nextBackoff =
                random.nextInt(
                        edca.contentionWindow(
                                pending.category()
                        ) + 1
                );

        PendingTransmission retry =
                new PendingTransmission(
                        pending.sequence(),
                        pending.frameControl(),
                        pending.address1(),
                        pending.address2(),
                        pending.address3(),
                        pending.body(),
                        pending.category(),
                        pending.sender(),
                        nextAttempt,
                        PendingStage.WAIT_AIFS,
                        nowMicros
                                + timing.aifsUs(
                                pending.category()
                        ),
                        nextBackoff,
                        pending.useRts()
                );

        pendingTransmissions.put(
                retry.sequence(),
                retry
        );
    }

    private void sendPendingAttempt(
            PendingTransmission pending
    ) {
        if (pending.useRts()) {
            sendRts(
                    pending
            );

            return;
        }

        sendPendingData(
                pending
        );
    }

    private void sendRts(
            PendingTransmission pending
    ) {
        WifiMacFrame rts =
                new WifiMacFrame(
                        FC_RTS,
                        WifiDurationCalculator.rtsDurationUs(
                                timing
                        ),
                        parseMac(
                                pending.address1()
                        ),
                        parseMac(
                                pending.address2()
                        ),
                        new byte[6],
                        0,
                        new byte[0]
                );

        pending.sender()
                .send(
                        rts
                );

        long deadline =
                WifiMacTimingScheduler
                        .quantizedResponseDeadlineMicros(
                                WifiMacTimingScheduler.nowMicros(),
                                timing.ctsTimeoutUs()
                        );

        pendingTransmissions.put(
                pending.sequence(),
                new PendingTransmission(
                        pending.sequence(),
                        pending.frameControl(),
                        pending.address1(),
                        pending.address2(),
                        pending.address3(),
                        pending.body(),
                        pending.category(),
                        pending.sender(),
                        pending.attempt(),
                        PendingStage.WAIT_CTS,
                        deadline,
                        pending.backoffSlots(),
                        pending.useRts()
                )
        );
    }

    private void sendPendingDataAfterCts(
            PendingTransmission pending
    ) {
        PendingTransmission current =
                pendingTransmissions.get(
                        pending.sequence()
                );

        if (current == null
                || current.stage()
                != PendingStage.WAIT_CTS) {
            return;
        }

        sendPendingData(
                current
        );
    }

    private void sendPendingData(
            PendingTransmission pending
    ) {
        int control =
                pending.attempt() == 0
                        ? pending.frameControl()
                        : pending.frameControl()
                        | FC_RETRY;

        WifiMacFrame frame =
                dataFrame(
                        control,
                        pending.address1(),
                        pending.address2(),
                        pending.address3(),
                        pending.sequence(),
                        pending.body()
                );

        pending.sender()
                .send(
                        frame
                );

        if (macEquals(
                pending.address1(),
                BROADCAST
        )) {
            pendingTransmissions.remove(
                    pending.sequence()
            );

            edca.onSuccess(
                    pending.category()
            );

            lastSecurityDiagnostic =
                    "BROADCAST_EDCA_QUEUED";

            return;
        }

        long deadline =
                WifiMacTimingScheduler
                        .quantizedResponseDeadlineMicros(
                                WifiMacTimingScheduler.nowMicros(),
                                timing.ackTimeoutUs()
                        );

        pendingTransmissions.put(
                pending.sequence(),
                new PendingTransmission(
                        pending.sequence(),
                        pending.frameControl(),
                        pending.address1(),
                        pending.address2(),
                        pending.address3(),
                        pending.body(),
                        pending.category(),
                        pending.sender(),
                        pending.attempt(),
                        PendingStage.WAIT_ACK,
                        deadline,
                        pending.backoffSlots(),
                        pending.useRts()
                )
        );
    }

    private boolean mediumUnavailable(
            PendingTransmission pending,
            long nowMicros
    ) {
        return pending.sender()
                .mediumBusy()
                || nav.active(
                nowMicros
        )
                || hasOutstandingExchangeOtherThan(
                pending.sequence()
        );
    }

    private boolean hasOutstandingExchangeOtherThan(
            int sequence
    ) {
        for (PendingTransmission candidate
                : pendingTransmissions.values()) {
            if (candidate.sequence()
                    == sequence) {
                continue;
            }

            if (candidate.stage()
                    == PendingStage.WAIT_CTS
                    || candidate.stage()
                    == PendingStage.WAIT_ACK) {
                return true;
            }
        }

        return false;
    }

    private PendingTransmission copyPending(
            PendingTransmission source,
            PendingStage stage,
            long nextActionMicros,
            int backoffSlots
    ) {
        return new PendingTransmission(
                source.sequence(),
                source.frameControl(),
                source.address1(),
                source.address2(),
                source.address3(),
                source.body(),
                source.category(),
                source.sender(),
                source.attempt(),
                stage,
                nextActionMicros,
                backoffSlots,
                source.useRts()
        );
    }

    private PendingTransmission oldestPendingInStage(
            PendingStage stage
    ) {
        for (PendingTransmission pending
                : pendingTransmissions.values()) {
            if (pending.stage()
                    == stage) {
                return pending;
            }
        }

        return null;
    }

    private void handleAdvertisement(
            WifiMacFrame frame,
            CompoundTag body
    ) {
        if (mode != WifiMode.STATION) {
            return;
        }

        String ssid =
                body.getString(
                        "ssid"
                );

        if (ssid.isBlank()) {
            return;
        }

        WifiNetworkRecord record =
                new WifiNetworkRecord(
                        ssid,
                        formatMac(
                                frame.address3()
                        ),
                        body.getString(
                                "security"
                        ),
                        body.getString(
                                "network_profile"
                        ),
                        body.getDouble(
                                "frequency_hz"
                        ),
                        System.nanoTime()
                );

        String discoveredBssid =
                normalizeMac(
                        record.bssid()
                );

        discovered.put(
                discoveredBssid,
                record
        );

        discoveredSnrDb.put(
                discoveredBssid,
                Double.isFinite(
                        lastObservedSnrDb
                )
                        ? lastObservedSnrDb
                        : Double.NEGATIVE_INFINITY
        );
    }

    private void pruneStaleDiscoveredNetworks() {
        long now =
                System.nanoTime();

        discovered.entrySet()
                .removeIf(
                        entry ->
                                entry.getValue() == null
                                        || now
                                        - entry.getValue()
                                        .lastSeenNanos()
                                        > DISCOVERED_NETWORK_MAX_AGE_NANOS
                );

        discoveredSnrDb.keySet()
                .retainAll(
                        discovered.keySet()
                );
    }

    private int compareDiscoveredNetworks(
            WifiNetworkRecord left,
            WifiNetworkRecord right
    ) {
        double leftSnr =
                discoveredSnrDb.getOrDefault(
                        normalizeMac(
                                left.bssid()
                        ),
                        Double.NEGATIVE_INFINITY
                );

        double rightSnr =
                discoveredSnrDb.getOrDefault(
                        normalizeMac(
                                right.bssid()
                        ),
                        Double.NEGATIVE_INFINITY
                );

        int bySnr =
                Double.compare(
                        rightSnr,
                        leftSnr
                );

        if (bySnr != 0) {
            return bySnr;
        }

        int byFreshness =
                Long.compare(
                        right.lastSeenNanos(),
                        left.lastSeenNanos()
                );

        if (byFreshness != 0) {
            return byFreshness;
        }

        return normalizeMac(
                left.bssid()
        ).compareTo(
                normalizeMac(
                        right.bssid()
                )
        );
    }

    private void handleProbeRequest(
            String ownMac,
            WifiMacFrame frame,
            CompoundTag body,
            String profileId,
            double frequencyHz,
            Sender sender
    ) {
        String requested =
                body.getString(
                        "ssid"
                );

        if (!requested.isBlank()
                && !requested.equals(
                apSsid
        )) {
            return;
        }

        sender.send(
                newFrame(
                        FC_PROBE_RESP,
                        formatMac(
                                frame.address2()
                        ),
                        ownMac,
                        ownMac,
                        advertisementBody(
                                profileId,
                                frequencyHz
                        )
                )
        );
    }

    private void handleAuthentication(
            String ownMac,
            WifiMacFrame frame,
            CompoundTag body,
            Sender sender
    ) {
        int algorithm =
                body.getInt(
                        "algorithm"
                );

        int transaction =
                body.getInt(
                        "transaction_sequence"
                );

        int status =
                body.getInt(
                        "status_code"
                );

        if (algorithm != 0) {
            return;
        }

        if (mode == WifiMode.ACCESS_POINT
                && transaction == 1) {
            String station =
                    formatMac(
                            frame.address2()
                    );

            authenticated.add(
                    normalizeMac(
                            station
                    )
            );

            CompoundTag response =
                    new CompoundTag();

            response.putInt(
                    "algorithm",
                    0
            );

            response.putInt(
                    "transaction_sequence",
                    2
            );

            response.putInt(
                    "status_code",
                    0
            );

            sender.send(
                    newFrame(
                            FC_AUTH,
                            station,
                            ownMac,
                            ownMac,
                            response
                    )
            );

            return;
        }

        if (mode == WifiMode.STATION
                && transaction == 2
                && status == 0
                && macEquals(
                formatMac(
                        frame.address3()
                ),
                selectedBssid
        )) {
            stationState =
                    WifiStationState.ASSOCIATING;

            lastSecurityDiagnostic =
                    "AUTH_OK_ASSOC_REQ_TX";

            CompoundTag association =
                    new CompoundTag();

            association.putString(
                    "ssid",
                    selectedSsid
            );

            sender.send(
                    newFrame(
                            FC_ASSOC_REQ,
                            selectedBssid,
                            ownMac,
                            selectedBssid,
                            association
                    )
            );
        }
    }

    private void handleAssociationRequest(
            String ownMac,
            WifiMacFrame frame,
            CompoundTag body,
            Sender sender
    ) {
        String station =
                formatMac(
                        frame.address2()
                );

        boolean ok =
                authenticated.contains(
                        normalizeMac(
                                station
                        )
                )
                        && body.getString(
                        "ssid"
                ).equals(
                        apSsid
                );

        if (ok) {
            associated.add(
                    normalizeMac(
                            station
                    )
            );
        }

        CompoundTag response =
                new CompoundTag();

        response.putInt(
                "status_code",
                ok ? 0 : 1
        );

        response.putInt(
                "association_id",
                ok
                        ? associated.size()
                        : 0
        );

        sender.send(
                newFrame(
                        FC_ASSOC_RESP,
                        station,
                        ownMac,
                        ownMac,
                        response
                )
        );

        if (ok
                && !isOpenSecurity(
                apSecurity
        )) {
            beginApFourWayHandshake(
                    ownMac,
                    station,
                    sender
            );
        }
    }

    private void handleAssociationResponse(
            WifiMacFrame frame,
            CompoundTag body
    ) {
        if (!macEquals(
                formatMac(
                        frame.address3()
                ),
                selectedBssid
        )) {
            return;
        }

        if (body.getInt(
                "status_code"
        ) != 0) {
            stationState =
                    WifiStationState.DISCONNECTED;

            securityState =
                    WifiSecurityState.FAILED;

            return;
        }

        if (isOpenSecurity(
                selectedSecurity
        )) {
            stationState =
                    WifiStationState.ASSOCIATED;

            securityState =
                    WifiSecurityState.OPEN;

            lastSecurityDiagnostic =
                    "ASSOC_OK_OPEN";
        } else {
            stationState =
                    WifiStationState.FOUR_WAY_HANDSHAKE;

            securityState =
                    WifiSecurityState.WAITING_MESSAGE_1;

            lastSecurityDiagnostic =
                    "ASSOC_OK_WAIT_EAPOL_M1";
        }
    }

    private void beginApFourWayHandshake(
            String apMac,
            String stationMac,
            Sender sender
    ) {
        byte[] anonce =
                WifiSecurityEngine.randomNonce();

        pendingAnonceByStation.put(
                normalizeMac(
                        stationMac
                ),
                anonce
        );

        CompoundTag message =
                new CompoundTag();

        message.putString(
                "wifi_control",
                "EAPOL_KEY"
        );

        message.putInt(
                "message",
                1
        );

        message.putByteArray(
                "anonce",
                anonce
        );

        lastSecurityDiagnostic =
                "AP_EAPOL_M1_READY";

        queueEapolKeyFrame(
                apMac,
                stationMac,
                apMac,
                message,
                sender
        );
    }

    private void handleEapolKey(
            String ownMac,
            WifiMacFrame frame,
            CompoundTag body,
            Sender sender
    ) {
        int message =
                body.getInt(
                        "message"
                );

        if (mode == WifiMode.STATION) {
            handleStationEapol(
                    ownMac,
                    frame,
                    body,
                    message,
                    sender
            );

            return;
        }

        if (mode == WifiMode.ACCESS_POINT) {
            handleApEapol(
                    ownMac,
                    frame,
                    body,
                    message,
                    sender
            );
        }
    }

    private void handleStationEapol(
            String ownMac,
            WifiMacFrame frame,
            CompoundTag body,
            int message,
            Sender sender
    ) {
        if (message == 1
                && securityState
                == WifiSecurityState.WAITING_MESSAGE_1) {
            stationAnonce =
                    body.getByteArray(
                            "anonce"
                    );

            stationSnonce =
                    WifiSecurityEngine.randomNonce();

            byte[] pmk =
                    WifiSecurityEngine.derivePmk(
                            stationPassphrase,
                            selectedSsid
                    );

            stationPtk =
                    WifiSecurityEngine.derivePtk(
                            pmk,
                            selectedBssid,
                            ownMac,
                            stationAnonce,
                            stationSnonce
                    );

            CompoundTag message2 =
                    new CompoundTag();

            message2.putString(
                    "wifi_control",
                    "EAPOL_KEY"
            );

            message2.putInt(
                    "message",
                    2
            );

            message2.putByteArray(
                    "snonce",
                    stationSnonce
            );

            byte[] micData =
                    WifiHandshakeMicMaterial.micData(
                            "M2",
                            selectedBssid,
                            ownMac
                    );

            message2.putByteArray(
                    "mic",
                    WifiSecurityEngine.mic(
                            stationPtk,
                            micData
                    )
            );

            securityState =
                    WifiSecurityState.WAITING_MESSAGE_3;

            lastSecurityDiagnostic =
                    "STATION_EAPOL_M1_OK_M2_READY";

            queueEapolKeyFrame(
                    ownMac,
                    selectedBssid,
                    selectedBssid,
                    message2,
                    sender
            );

            return;
        }

        if (message == 3
                && securityState
                == WifiSecurityState.WAITING_MESSAGE_3) {
            byte[] micData =
                    WifiHandshakeMicMaterial.micData(
                            "M3",
                            selectedBssid,
                            ownMac
                    );

            if (!WifiSecurityEngine.verifyMic(
                    stationPtk,
                    micData,
                    body.getByteArray(
                            "mic"
                    )
            )) {
                securityState =
                        WifiSecurityState.FAILED;

                stationState =
                        WifiStationState.DISCONNECTED;

                lastSecurityDiagnostic =
                        "STATION_EAPOL_M3_MIC_INVALID";

                return;
            }

            CompoundTag message4 =
                    new CompoundTag();

            message4.putString(
                    "wifi_control",
                    "EAPOL_KEY"
            );

            message4.putInt(
                    "message",
                    4
            );

            byte[] message4Data =
                    WifiHandshakeMicMaterial.micData(
                            "M4",
                            selectedBssid,
                            ownMac
                    );

            message4.putByteArray(
                    "mic",
                    WifiSecurityEngine.mic(
                            stationPtk,
                            message4Data
                    )
            );

            securityState =
                    WifiSecurityState.SECURED;

            stationState =
                    WifiStationState.ASSOCIATED;

            lastSecurityDiagnostic =
                    "STATION_EAPOL_M3_OK_M4_READY";

            queueEapolKeyFrame(
                    ownMac,
                    selectedBssid,
                    selectedBssid,
                    message4,
                    sender
            );
        }
    }

    private void handleApEapol(
            String ownMac,
            WifiMacFrame frame,
            CompoundTag body,
            int message,
            Sender sender
    ) {
        String station =
                formatMac(
                        frame.address2()
                );

        String key =
                normalizeMac(
                        station
                );

        if (message == 2) {
            byte[] anonce =
                    pendingAnonceByStation.get(
                            key
                    );

            if (anonce == null) {
                lastSecurityDiagnostic =
                        "AP_EAPOL_M2_NO_ANONCE";
                return;
            }

            byte[] snonce =
                    body.getByteArray(
                            "snonce"
                    );

            byte[] pmk =
                    WifiSecurityEngine.derivePmk(
                            apPassphrase,
                            apSsid
                    );

            byte[] ptk =
                    WifiSecurityEngine.derivePtk(
                            pmk,
                            ownMac,
                            station,
                            anonce,
                            snonce
                    );

            byte[] micData =
                    WifiHandshakeMicMaterial.micData(
                            "M2",
                            ownMac,
                            station
                    );

            if (!WifiSecurityEngine.verifyMic(
                    ptk,
                    micData,
                    body.getByteArray(
                            "mic"
                    )
            )) {
                lastSecurityDiagnostic =
                        "AP_EAPOL_M2_MIC_INVALID";
                return;
            }

            apPtkByStation.put(
                    key,
                    ptk
            );

            CompoundTag message3 =
                    new CompoundTag();

            message3.putString(
                    "wifi_control",
                    "EAPOL_KEY"
            );

            message3.putInt(
                    "message",
                    3
            );

            byte[] message3Data =
                    WifiHandshakeMicMaterial.micData(
                            "M3",
                            ownMac,
                            station
                    );

            message3.putByteArray(
                    "mic",
                    WifiSecurityEngine.mic(
                            ptk,
                            message3Data
                    )
            );

            lastSecurityDiagnostic =
                    "AP_EAPOL_M2_OK_M3_READY";

            queueEapolKeyFrame(
                    ownMac,
                    station,
                    ownMac,
                    message3,
                    sender
            );

            return;
        }

        if (message == 4) {
            byte[] ptk =
                    apPtkByStation.get(
                            key
                    );

            if (ptk == null) {
                lastSecurityDiagnostic =
                        "AP_EAPOL_M4_NO_PTK";
                return;
            }

            byte[] micData =
                    WifiHandshakeMicMaterial.micData(
                            "M4",
                            ownMac,
                            station
                    );

            if (WifiSecurityEngine.verifyMic(
                    ptk,
                    micData,
                    body.getByteArray(
                            "mic"
                    )
            )) {
                securedStations.add(
                        key
                );

                pendingAnonceByStation.remove(
                        key
                );

                lastSecurityDiagnostic =
                        "AP_EAPOL_M4_OK_SECURED";
            } else {
                lastSecurityDiagnostic =
                        "AP_EAPOL_M4_MIC_INVALID";
            }
        }
    }

    private boolean queueEapolKeyFrame(
            String sourceMac,
            String destinationMac,
            String bssid,
            CompoundTag body,
            Sender sender
    ) {
        int message =
                body.getInt(
                        "message"
                );

        boolean queued =
                transmitWithRetry(
                        FC_DATA,
                        destinationMac,
                        sourceMac,
                        bssid,
                        body,
                        WifiAccessCategory.BEST_EFFORT,
                        sender
                );

        lastSecurityDiagnostic =
                queued
                        ? "EAPOL_M"
                        + message
                        + "_QUEUED"
                        : "EAPOL_M"
                        + message
                        + "_QUEUE_FAILED";

        return queued;
    }

    private boolean isEapolKeyBody(
            CompoundTag body
    ) {
        return body != null
                && "EAPOL_KEY".equals(
                body.getString(
                        "wifi_control"
                )
        );
    }

    private boolean relayInfrastructureData(
            String apMac,
            String originalSource,
            String destination,
            CompoundTag data,
            Sender sender
    ) {
        if (mode != WifiMode.ACCESS_POINT) {
            return false;
        }

        boolean broadcast =
                macEquals(
                        destination,
                        BROADCAST
                );

        String normalizedDestination =
                normalizeMac(
                        destination
                );

        if (!broadcast
                && !associated.contains(
                normalizedDestination
        )) {
            lastSecurityDiagnostic =
                    "DS_RELAY_DESTINATION_NOT_ASSOCIATED";

            return false;
        }

        CompoundTag protectedBody =
                protectApData(
                        destination,
                        data
                );

        boolean queued =
                transmitWithRetry(
                        FC_DATA | FC_FROM_DS,
                        destination,
                        apMac,
                        originalSource,
                        protectedBody,
                        WifiAccessCategory.BEST_EFFORT,
                        sender
                );

        lastSecurityDiagnostic =
                queued
                        ? "DS_RELAY_QUEUED"
                        : "DS_RELAY_QUEUE_FAILED";

        return queued;
    }
    private CompoundTag protectStationData(
            CompoundTag data
    ) {
        if (isOpenSecurity(
                selectedSecurity
        )) {
            return data;
        }

        CompoundTag result =
                new CompoundTag();

        result.putBoolean(
                "protected",
                true
        );

        result.putByteArray(
                "protected_data",
                WifiSecurityEngine.protect(
                        stationPtk,
                        encode(
                                data
                        )
                )
        );

        return result;
    }

    private CompoundTag protectApData(
            String targetMac,
            CompoundTag data
    ) {
        if (isOpenSecurity(
                apSecurity
        )
                || macEquals(
                targetMac,
                BROADCAST
        )) {
            return data;
        }

        byte[] ptk =
                apPtkByStation.get(
                        normalizeMac(
                                targetMac
                        )
                );

        if (ptk == null) {
            return new CompoundTag();
        }

        CompoundTag result =
                new CompoundTag();

        result.putBoolean(
                "protected",
                true
        );

        result.putByteArray(
                "protected_data",
                WifiSecurityEngine.protect(
                        ptk,
                        encode(
                                data
                        )
                )
        );

        return result;
    }

    private CompoundTag unprotectReceivedData(
            WifiMacFrame frame,
            CompoundTag body
    ) {
        if (!body.getBoolean(
                "protected"
        )) {
            return body;
        }

        try {
            byte[] ptk;

            if (mode == WifiMode.STATION) {
                ptk =
                        stationPtk;
            } else {
                ptk =
                        apPtkByStation.get(
                                normalizeMac(
                                        formatMac(
                                                frame.address2()
                                        )
                                )
                        );
            }

            if (ptk == null) {
                return null;
            }

            return decode(
                    WifiSecurityEngine.unprotect(
                            ptk,
                            body.getByteArray(
                                    "protected_data"
                            )
                    )
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean canReceiveData(
            WifiMacFrame frame
    ) {
        if (mode == WifiMode.ACCESS_POINT) {
            String station =
                    normalizeMac(
                            formatMac(
                                    frame.address2()
                            )
                    );

            if (!associated.contains(
                    station
            )) {
                return false;
            }

            return isOpenSecurity(
                    apSecurity
            )
                    || securedStations.contains(
                    station
            );
        }

        if (mode == WifiMode.STATION) {
            if (!isAssociated()) {
                return false;
            }

            boolean fromDistributionSystem =
                    (
                            frame.frameControl()
                                    & FC_FROM_DS
                    ) != 0;

            if (fromDistributionSystem) {
                return macEquals(
                        formatMac(
                                frame.address2()
                        ),
                        selectedBssid
                );
            }

            return macEquals(
                    formatMac(
                            frame.address3()
                    ),
                    selectedBssid
            );
        }

        return false;
    }

    private void sendAck(
            String ownMac,
            WifiMacFrame received,
            Sender sender
    ) {
        sender.send(
                new WifiMacFrame(
                        FC_ACK,
                        0,
                        received.address2(),
                        new byte[6],
                        new byte[6],
                        0,
                        new byte[0]
                )
        );
    }

    private void sendCts(
            String ownMac,
            WifiMacFrame receivedRts,
            Sender sender
    ) {
        int remainingDuration =
                WifiDurationCalculator.ctsDurationUs(
                        receivedRts.durationId(),
                        timing
                );

        sender.send(
                new WifiMacFrame(
                        FC_CTS,
                        remainingDuration,
                        receivedRts.address2(),
                        new byte[6],
                        new byte[6],
                        0,
                        new byte[0]
                )
        );
    }

    private CompoundTag advertisementBody(
            String profileId,
            double frequencyHz
    ) {
        CompoundTag body =
                new CompoundTag();

        body.putString(
                "ssid",
                apSsid
        );

        body.putString(
                "security",
                apSecurity
        );

        body.putString(
                "network_profile",
                profileId
        );

        body.putDouble(
                "frequency_hz",
                frequencyHz
        );

        return body;
    }

    private WifiMacFrame newFrame(
            int frameControl,
            String address1,
            String address2,
            String address3,
            CompoundTag body
    ) {
        int sequence =
                sequenceNumber++
                        & 0x0FFF;

        return frame(
                frameControl,
                address1,
                address2,
                address3,
                sequence,
                body
        );
    }

    private WifiMacFrame frame(
            int frameControl,
            String address1,
            String address2,
            String address3,
            int sequence,
            CompoundTag body
    ) {
        byte[] payload =
                WifiManagementCodec.isManagementSubtype(
                        frameControl
                )
                        ? WifiManagementCodec.encodeBody(
                        frameControl,
                        body
                )
                        : encode(
                        body
                );

        return new WifiMacFrame(
                frameControl,
                0,
                parseMac(
                        address1
                ),
                parseMac(
                        address2
                ),
                parseMac(
                        address3
                ),
                (sequence & 0x0FFF)
                        << 4,
                payload
        );
    }

    private WifiMacFrame dataFrame(
            int frameControl,
            String address1,
            String address2,
            String address3,
            int sequence,
            CompoundTag body
    ) {
        return new WifiMacFrame(
                frameControl,
                WifiDurationCalculator.dataDurationUs(
                        timing
                ),
                parseMac(
                        address1
                ),
                parseMac(
                        address2
                ),
                parseMac(
                        address3
                ),
                (sequence & 0x0FFF)
                        << 4,
                encode(
                        body
                )
        );
    }

    private void requireStation() {
        if (mode
                != WifiMode.STATION) {
            throw new IllegalStateException(
                    "Wi-Fi interface is not in STATION mode"
            );
        }
    }

    private void resetStation() {
        pendingTransmissions.clear();
        lastDeliveredSequenceBySender.clear();
        nav.clear();

        WifiMacTimingScheduler.untrack(
                this
        );

        stationState =
                WifiStationState.DISCONNECTED;

        securityState =
                WifiSecurityState.OPEN;

        selectedSsid = "";
        selectedBssid = "";
        selectedSecurity =
                "signality:open";

        stationAnonce = null;
        stationSnonce = null;
        stationPtk = null;

        if (mode == WifiMode.STATION) {
            lastSecurityDiagnostic =
                    "STATION_RESET";
        }
    }

    private static boolean isOpenSecurity(
            String security
    ) {
        if (security == null) {
            return true;
        }

        String value =
                security.toLowerCase();

        return value.isBlank()
                || value.contains(
                "open"
        )
                || value.contains(
                "none"
        );
    }

    private static byte[] encode(
            CompoundTag tag
    ) {
        try {
            ByteArrayOutputStream bytes =
                    new ByteArrayOutputStream();

            DataOutputStream out =
                    new DataOutputStream(
                            bytes
                    );

            NbtIo.write(
                    tag,
                    out
            );

            return bytes.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to encode Wi-Fi body",
                    exception
            );
        }
    }

    private static CompoundTag decode(
            byte[] bytes
    ) {
        try {
            CompoundTag result =
                    NbtIo.read(
                            new DataInputStream(
                                    new ByteArrayInputStream(
                                            bytes
                                    )
                            )
                    );

            return result == null
                    ? new CompoundTag()
                    : result;
        } catch (Exception ignored) {
            return new CompoundTag();
        }
    }

    private static byte[] parseMac(
            String mac
    ) {
        String normalized =
                mac.replace(":", "")
                        .replace("-", "");

        if (normalized.length()
                != 12) {
            throw new IllegalArgumentException(
                    "Invalid MAC address: "
                            + mac
            );
        }

        byte[] result =
                new byte[6];

        for (int i = 0;
             i < 6;
             i++) {
            result[i] =
                    (byte) Integer.parseInt(
                            normalized.substring(
                                    i * 2,
                                    i * 2 + 2
                            ),
                            16
                    );
        }

        return result;
    }

    private static String formatMac(
            byte[] mac
    ) {
        StringBuilder result =
                new StringBuilder(
                        17
                );

        for (int i = 0;
             i < mac.length;
             i++) {
            if (i > 0) {
                result.append(
                        ':'
                );
            }

            result.append(
                    String.format(
                            "%02X",
                            mac[i] & 0xFF
                    )
            );
        }

        return result.toString();
    }

    private static String normalizeMac(
            String mac
    ) {
        return formatMac(
                parseMac(
                        mac
                )
        );
    }

    private static boolean macEquals(
            String a,
            String b
    ) {
        return normalizeMac(a)
                .equalsIgnoreCase(
                        normalizeMac(b)
                );
    }

    public CompoundTag save() {
        CompoundTag tag =
                new CompoundTag();

        tag.putString(
                "Mode",
                mode.name()
        );

        tag.putString(
                "StationState",
                stationState.name()
        );

        tag.putString(
                "SecurityState",
                securityState.name()
        );

        tag.putString(
                "ApSsid",
                apSsid
        );

        tag.putString(
                "ApSecurity",
                apSecurity
        );

        tag.putString(
                "SelectedSsid",
                selectedSsid
        );

        tag.putString(
                "SelectedBssid",
                selectedBssid
        );

        tag.putString(
                "SelectedSecurity",
                selectedSecurity
        );

        tag.putInt(
                "SequenceNumber",
                sequenceNumber
        );

        tag.putInt(
                "LastAckedSequence",
                lastAckedSequence
        );

        tag.putDouble(
                "LastObservedSnrDb",
                lastObservedSnrDb
        );

        tag.putInt(
                "CurrentMcsIndex",
                currentMcsIndex
        );

        return tag;
    }

    public void load(
            CompoundTag tag
    ) {
        try {
            mode =
                    WifiMode.valueOf(
                            tag.getString(
                                    "Mode"
                            )
                    );
        } catch (Exception ignored) {
            mode =
                    WifiMode.LEGACY_DIRECT;
        }

        try {
            stationState =
                    WifiStationState.valueOf(
                            tag.getString(
                                    "StationState"
                            )
                    );
        } catch (Exception ignored) {
            stationState =
                    WifiStationState.DISCONNECTED;
        }

        try {
            securityState =
                    WifiSecurityState.valueOf(
                            tag.getString(
                                    "SecurityState"
                            )
                    );
        } catch (Exception ignored) {
            securityState =
                    WifiSecurityState.OPEN;
        }

        apSsid =
                tag.getString(
                        "ApSsid"
                );

        apSecurity =
                tag.getString(
                        "ApSecurity"
                );

        selectedSsid =
                tag.getString(
                        "SelectedSsid"
                );

        selectedBssid =
                tag.getString(
                        "SelectedBssid"
                );

        selectedSecurity =
                tag.getString(
                        "SelectedSecurity"
                );

        sequenceNumber =
                tag.getInt(
                        "SequenceNumber"
                );

        lastAckedSequence =
                tag.getInt(
                        "LastAckedSequence"
                );

        lastObservedSnrDb =
                tag.getDouble(
                        "LastObservedSnrDb"
                );

        currentMcsIndex =
                tag.getInt(
                        "CurrentMcsIndex"
                );

        discovered.clear();
        discoveredSnrDb.clear();
        authenticated.clear();
        associated.clear();
        securedStations.clear();
        apPtkByStation.clear();
        pendingAnonceByStation.clear();

        apPassphrase = "";
        stationPassphrase = "";

        if (mode == WifiMode.STATION) {
            resetStation();
        }
    }
}
