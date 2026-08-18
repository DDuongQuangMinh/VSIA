package com.k1ngtle.vsia.signality.engineering.wifi;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class WifiMacController {
    public static final int FC_ASSOC_REQ = 0x0000;
    public static final int FC_ASSOC_RESP = 0x0010;
    public static final int FC_PROBE_REQ = 0x0040;
    public static final int FC_PROBE_RESP = 0x0050;
    public static final int FC_BEACON = 0x0080;
    public static final int FC_AUTH = 0x00B0;
    public static final int FC_DATA = 0x0008;
    public static final int FC_ACK = 0x00D4;

    public static final int FC_RETRY = 0x0800;

    public static final String BROADCAST =
            "FF:FF:FF:FF:FF:FF";

    private static final int MAX_RETRIES = 4;

    private static final long ACK_TIMEOUT_TICKS = 4L;

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
            long deadlineTick,
            boolean waitingForMedium
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

    private final Map<String, WifiNetworkRecord> discovered =
            new LinkedHashMap<>();

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

    public WifiMode mode() {
        return mode;
    }

    public WifiStationState stationState() {
        return stationState;
    }

    public WifiSecurityState securityState() {
        return securityState;
    }

    public Collection<WifiNetworkRecord> discoveredNetworks() {
        return Collections.unmodifiableCollection(
                discovered.values()
        );
    }

    public Set<String> associatedStations() {
        return Collections.unmodifiableSet(
                associated
        );
    }

    public int pendingDataTransmissions() {
        return pendingTransmissions.size();
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

    public void startScan(
            String ownMac,
            Sender sender
    ) {
        requireStation();

        discovered.clear();
        stationState =
                WifiStationState.SCANNING;

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

    public boolean connect(
            String ownMac,
            String ssid,
            Sender sender
    ) {
        requireStation();

        WifiNetworkRecord network =
                discovered.values()
                        .stream()
                        .filter(
                                value ->
                                        value.ssid()
                                                .equals(
                                                        ssid
                                                )
                        )
                        .findFirst()
                        .orElse(
                                null
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

        securityState =
                isOpenSecurity(
                        selectedSecurity
                )
                        ? WifiSecurityState.OPEN
                        : WifiSecurityState.WAITING_MESSAGE_1;

        stationState =
                WifiStationState.AUTHENTICATING;

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
                    FC_DATA,
                    selectedBssid,
                    ownMac,
                    selectedBssid,
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
                    FC_DATA,
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

        if (!macEquals(
                receiver,
                ownMac
        )
                && !macEquals(
                receiver,
                BROADCAST
        )) {
            return null;
        }

        int fc =
                frame.frameControl()
                        & 0x00FC;

        CompoundTag body =
                decode(
                        frame.payload()
                );

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

            return unprotectReceivedData(
                    frame,
                    body
            );
        }

        if (fc == (FC_ACK & 0x00FC)) {
            lastAckedSequence =
                    body.getInt(
                            "acked_sequence"
                    );

            PendingTransmission pending =
                    pendingTransmissions.remove(
                            lastAckedSequence
                    );

            if (pending != null) {
                edca.onSuccess(
                        pending.category()
                );
            }
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

        if (macEquals(
                address1,
                BROADCAST
        )) {
            edca.acquireLogicalMedium(
                    category
            );

            sender.send(
                    frame(
                            frameControl,
                            address1,
                            address2,
                            address3,
                            sequence,
                            body
                    )
            );

            edca.onSuccess(
                    category
            );

            return true;
        }

        PendingTransmission pending =
                new PendingTransmission(
                        sequence,
                        frameControl,
                        address1,
                        address2,
                        address3,
                        body.copy(),
                        category,
                        sender,
                        0,
                        WifiMacTimingScheduler.now()
                                + ACK_TIMEOUT_TICKS,
                        sender.mediumBusy()
                );

        pendingTransmissions.put(
                sequence,
                pending
        );

        WifiMacTimingScheduler.track(
                this
        );

        if (!pending.waitingForMedium()) {
            sendPendingAttempt(
                    pending
            );
        }

        return true;
    }

    boolean onTimingTick(
            long currentTick
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

        for (PendingTransmission pending : snapshot) {
            PendingTransmission current =
                    pendingTransmissions.get(
                            pending.sequence()
                    );

            if (current == null) {
                continue;
            }

            if (current.waitingForMedium()) {
                if (current.sender()
                        .mediumBusy()) {
                    pendingTransmissions.put(
                            current.sequence(),
                            new PendingTransmission(
                                    current.sequence(),
                                    current.frameControl(),
                                    current.address1(),
                                    current.address2(),
                                    current.address3(),
                                    current.body(),
                                    current.category(),
                                    current.sender(),
                                    current.attempt(),
                                    currentTick + 1L,
                                    true
                            )
                    );

                    continue;
                }

                PendingTransmission ready =
                        new PendingTransmission(
                                current.sequence(),
                                current.frameControl(),
                                current.address1(),
                                current.address2(),
                                current.address3(),
                                current.body(),
                                current.category(),
                                current.sender(),
                                current.attempt(),
                                currentTick
                                        + ACK_TIMEOUT_TICKS,
                                false
                        );

                pendingTransmissions.put(
                        ready.sequence(),
                        ready
                );

                sendPendingAttempt(
                        ready
                );

                continue;
            }

            if (currentTick
                    < current.deadlineTick()) {
                continue;
            }

            edca.onFailure(
                    current.category()
            );

            if (current.attempt()
                    >= MAX_RETRIES) {
                pendingTransmissions.remove(
                        current.sequence()
                );

                continue;
            }

            int nextAttempt =
                    current.attempt() + 1;

            boolean mediumBusy =
                    current.sender()
                            .mediumBusy();

            PendingTransmission retry =
                    new PendingTransmission(
                            current.sequence(),
                            current.frameControl(),
                            current.address1(),
                            current.address2(),
                            current.address3(),
                            current.body(),
                            current.category(),
                            current.sender(),
                            nextAttempt,
                            currentTick
                                    + (
                                    mediumBusy
                                            ? 1L
                                            : ACK_TIMEOUT_TICKS
                            ),
                            mediumBusy
                    );

            pendingTransmissions.put(
                    retry.sequence(),
                    retry
            );

            if (!mediumBusy) {
                sendPendingAttempt(
                        retry
                );
            }
        }

        return !pendingTransmissions.isEmpty();
    }

    private void sendPendingAttempt(
            PendingTransmission pending
    ) {
        edca.acquireLogicalMedium(
                pending.category()
        );

        int control =
                pending.attempt() == 0
                        ? pending.frameControl()
                        : pending.frameControl()
                        | FC_RETRY;

        WifiMacFrame frame =
                frame(
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

        discovered.put(
                normalizeMac(
                        record.bssid()
                ),
                record
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
        } else {
            stationState =
                    WifiStationState.FOUR_WAY_HANDSHAKE;

            securityState =
                    WifiSecurityState.WAITING_MESSAGE_1;
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

        sender.send(
                newFrame(
                        FC_DATA,
                        stationMac,
                        apMac,
                        apMac,
                        message
                )
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
                    ("M2|"
                            + selectedBssid
                            + "|"
                            + ownMac)
                            .getBytes(
                                    StandardCharsets.UTF_8
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

            sender.send(
                    newFrame(
                            FC_DATA,
                            selectedBssid,
                            ownMac,
                            selectedBssid,
                            message2
                    )
            );

            return;
        }

        if (message == 3
                && securityState
                == WifiSecurityState.WAITING_MESSAGE_3) {
            byte[] micData =
                    ("M3|"
                            + selectedBssid
                            + "|"
                            + ownMac)
                            .getBytes(
                                    StandardCharsets.UTF_8
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
                    ("M4|"
                            + selectedBssid
                            + "|"
                            + ownMac)
                            .getBytes(
                                    StandardCharsets.UTF_8
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

            sender.send(
                    newFrame(
                            FC_DATA,
                            selectedBssid,
                            ownMac,
                            selectedBssid,
                            message4
                    )
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
                    ("M2|"
                            + ownMac
                            + "|"
                            + station)
                            .getBytes(
                                    StandardCharsets.UTF_8
                            );

            if (!WifiSecurityEngine.verifyMic(
                    ptk,
                    micData,
                    body.getByteArray(
                            "mic"
                    )
            )) {
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
                    ("M3|"
                            + ownMac
                            + "|"
                            + station)
                            .getBytes(
                                    StandardCharsets.UTF_8
                            );

            message3.putByteArray(
                    "mic",
                    WifiSecurityEngine.mic(
                            ptk,
                            message3Data
                    )
            );

            sender.send(
                    newFrame(
                            FC_DATA,
                            station,
                            ownMac,
                            ownMac,
                            message3
                    )
            );

            return;
        }

        if (message == 4) {
            byte[] ptk =
                    apPtkByStation.get(
                            key
                    );

            if (ptk == null) {
                return;
            }

            byte[] micData =
                    ("M4|"
                            + ownMac
                            + "|"
                            + station)
                            .getBytes(
                                    StandardCharsets.UTF_8
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
            }
        }
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
            return isAssociated()
                    && macEquals(
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
        CompoundTag ack =
                new CompoundTag();

        ack.putInt(
                "acked_sequence",
                (received.sequenceControl()
                        >>> 4)
                        & 0x0FFF
        );

        sender.send(
                newFrame(
                        FC_ACK,
                        formatMac(
                                received.address2()
                        ),
                        ownMac,
                        mode
                                == WifiMode.ACCESS_POINT
                                ? ownMac
                                : selectedBssid,
                        ack
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
