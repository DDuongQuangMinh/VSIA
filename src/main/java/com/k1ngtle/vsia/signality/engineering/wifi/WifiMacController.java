package com.k1ngtle.vsia.signality.engineering.wifi;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Collection;
import java.util.Collections;
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

    public static final String BROADCAST = "FF:FF:FF:FF:FF:FF";

    @FunctionalInterface
    public interface Sender {
        void send(WifiMacFrame frame);
    }

    private final Random random = new Random();
    private final CsmaCaController csma =
            new CsmaCaController(15, 1023, random);

    private final Map<String, WifiNetworkRecord> discovered =
            new LinkedHashMap<>();

    private final Set<String> authenticated =
            new LinkedHashSet<>();

    private final Set<String> associated =
            new LinkedHashSet<>();

    private WifiMode mode = WifiMode.LEGACY_DIRECT;
    private WifiStationState stationState = WifiStationState.DISCONNECTED;

    private String apSsid = "";
    private String apSecurity = "signality:open";

    private String selectedSsid = "";
    private String selectedBssid = "";

    private int sequenceNumber;
    private int lastAckedSequence = -1;

    public WifiMode mode() {
        return mode;
    }

    public WifiStationState stationState() {
        return stationState;
    }

    public Collection<WifiNetworkRecord> discoveredNetworks() {
        return Collections.unmodifiableCollection(discovered.values());
    }

    public Set<String> associatedStations() {
        return Collections.unmodifiableSet(associated);
    }

    public boolean isAssociated() {
        return mode == WifiMode.STATION
                && stationState == WifiStationState.ASSOCIATED;
    }

    public void useLegacyDirectMode() {
        mode = WifiMode.LEGACY_DIRECT;
        resetStation();
        authenticated.clear();
        associated.clear();
    }

    public void configureStation() {
        mode = WifiMode.STATION;
        resetStation();
        discovered.clear();
    }

    public void configureAccessPoint(String ssid, String security) {
        if (ssid == null || ssid.isBlank()) {
            throw new IllegalArgumentException("SSID cannot be blank");
        }

        mode = WifiMode.ACCESS_POINT;
        apSsid = ssid;
        apSecurity = security == null || security.isBlank()
                ? "signality:open"
                : security;

        resetStation();
        authenticated.clear();
        associated.clear();
    }

    public void startScan(String ownMac, Sender sender) {
        requireStation();

        discovered.clear();
        stationState = WifiStationState.SCANNING;

        CompoundTag body = new CompoundTag();
        body.putString("ssid", "");

        sender.send(frame(
                FC_PROBE_REQ,
                BROADCAST,
                ownMac,
                BROADCAST,
                body
        ));
    }

    public boolean connect(String ownMac, String ssid, Sender sender) {
        requireStation();

        WifiNetworkRecord network = null;

        for (WifiNetworkRecord candidate : discovered.values()) {
            if (candidate.ssid().equals(ssid)) {
                network = candidate;
                break;
            }
        }

        if (network == null) {
            return false;
        }

        selectedSsid = network.ssid();
        selectedBssid = network.bssid();
        stationState = WifiStationState.AUTHENTICATING;

        CompoundTag body = new CompoundTag();
        body.putInt("algorithm", 0);
        body.putInt("transaction_sequence", 1);
        body.putInt("status_code", 0);

        sender.send(frame(
                FC_AUTH,
                selectedBssid,
                ownMac,
                selectedBssid,
                body
        ));

        return true;
    }

    public void sendBeacon(
            String ownMac,
            String profileId,
            double frequencyHz,
            Sender sender
    ) {
        if (mode != WifiMode.ACCESS_POINT) {
            return;
        }

        sender.send(frame(
                FC_BEACON,
                BROADCAST,
                ownMac,
                ownMac,
                advertisementBody(profileId, frequencyHz)
        ));
    }

    public boolean sendData(
            String ownMac,
            String targetMac,
            CompoundTag data,
            Sender sender
    ) {
        if (mode == WifiMode.STATION) {
            if (!isAssociated()) {
                return false;
            }

            sender.send(frame(
                    FC_DATA,
                    selectedBssid,
                    ownMac,
                    selectedBssid,
                    data
            ));

            return true;
        }

        if (mode == WifiMode.ACCESS_POINT) {
            if (!macEquals(targetMac, BROADCAST)
                    && !associated.contains(normalizeMac(targetMac))) {
                return false;
            }

            sender.send(frame(
                    FC_DATA,
                    targetMac,
                    ownMac,
                    ownMac,
                    data
            ));

            return true;
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
        String receiver = formatMac(frame.address1());

        if (!macEquals(receiver, ownMac)
                && !macEquals(receiver, BROADCAST)) {
            return null;
        }

        int fc = frame.frameControl() & 0x00FC;
        CompoundTag body = decode(frame.payload());

        if (fc == (FC_BEACON & 0x00FC)
                || fc == (FC_PROBE_RESP & 0x00FC)) {
            if (mode == WifiMode.STATION) {
                String ssid = body.getString("ssid");

                if (!ssid.isBlank()) {
                    WifiNetworkRecord record =
                            new WifiNetworkRecord(
                                    ssid,
                                    formatMac(frame.address3()),
                                    body.getString("security"),
                                    body.getString("network_profile"),
                                    body.getDouble("frequency_hz"),
                                    System.nanoTime()
                            );

                    discovered.put(
                            normalizeMac(record.bssid()),
                            record
                    );
                }
            }

            return null;
        }

        if (fc == (FC_PROBE_REQ & 0x00FC)
                && mode == WifiMode.ACCESS_POINT) {
            String requested = body.getString("ssid");

            if (requested.isBlank() || requested.equals(apSsid)) {
                sender.send(frame(
                        FC_PROBE_RESP,
                        formatMac(frame.address2()),
                        ownMac,
                        ownMac,
                        advertisementBody(profileId, frequencyHz)
                ));
            }

            return null;
        }

        if (fc == (FC_AUTH & 0x00FC)) {
            handleAuthentication(ownMac, frame, body, sender);
            return null;
        }

        if (fc == (FC_ASSOC_REQ & 0x00FC)
                && mode == WifiMode.ACCESS_POINT) {
            handleAssociationRequest(ownMac, frame, body, sender);
            return null;
        }

        if (fc == (FC_ASSOC_RESP & 0x00FC)
                && mode == WifiMode.STATION) {
            if (macEquals(formatMac(frame.address3()), selectedBssid)) {
                stationState =
                        body.getInt("status_code") == 0
                                ? WifiStationState.ASSOCIATED
                                : WifiStationState.DISCONNECTED;
            }

            return null;
        }

        if (fc == (FC_DATA & 0x00FC)) {
            if (!canReceiveData(frame)) {
                return null;
            }

            CompoundTag ack = new CompoundTag();
            ack.putInt(
                    "acked_sequence",
                    (frame.sequenceControl() >>> 4) & 0x0FFF
            );

            sender.send(frame(
                    FC_ACK,
                    formatMac(frame.address2()),
                    ownMac,
                    mode == WifiMode.ACCESS_POINT
                            ? ownMac
                            : selectedBssid,
                    ack
            ));

            return body;
        }

        if (fc == (FC_ACK & 0x00FC)) {
            lastAckedSequence = body.getInt("acked_sequence");
            csma.onSuccess(random);
        }

        return null;
    }

    private void handleAuthentication(
            String ownMac,
            WifiMacFrame frame,
            CompoundTag body,
            Sender sender
    ) {
        int algorithm = body.getInt("algorithm");
        int transaction = body.getInt("transaction_sequence");
        int status = body.getInt("status_code");

        if (algorithm != 0) {
            return;
        }

        if (mode == WifiMode.ACCESS_POINT && transaction == 1) {
            String station = formatMac(frame.address2());
            authenticated.add(normalizeMac(station));

            CompoundTag response = new CompoundTag();
            response.putInt("algorithm", 0);
            response.putInt("transaction_sequence", 2);
            response.putInt("status_code", 0);

            sender.send(frame(
                    FC_AUTH,
                    station,
                    ownMac,
                    ownMac,
                    response
            ));

            return;
        }

        if (mode == WifiMode.STATION
                && transaction == 2
                && status == 0
                && macEquals(formatMac(frame.address3()), selectedBssid)) {

            stationState = WifiStationState.ASSOCIATING;

            CompoundTag association = new CompoundTag();
            association.putString("ssid", selectedSsid);

            sender.send(frame(
                    FC_ASSOC_REQ,
                    selectedBssid,
                    ownMac,
                    selectedBssid,
                    association
            ));
        }
    }

    private void handleAssociationRequest(
            String ownMac,
            WifiMacFrame frame,
            CompoundTag body,
            Sender sender
    ) {
        String station = formatMac(frame.address2());

        boolean ok =
                authenticated.contains(normalizeMac(station))
                        && body.getString("ssid").equals(apSsid);

        if (ok) {
            associated.add(normalizeMac(station));
        }

        CompoundTag response = new CompoundTag();
        response.putInt("status_code", ok ? 0 : 1);
        response.putInt("association_id", ok ? associated.size() : 0);

        sender.send(frame(
                FC_ASSOC_RESP,
                station,
                ownMac,
                ownMac,
                response
        ));
    }

    private boolean canReceiveData(WifiMacFrame frame) {
        if (mode == WifiMode.ACCESS_POINT) {
            return associated.contains(
                    normalizeMac(formatMac(frame.address2()))
            );
        }

        if (mode == WifiMode.STATION) {
            return isAssociated()
                    && macEquals(
                    formatMac(frame.address3()),
                    selectedBssid
            );
        }

        return false;
    }

    private CompoundTag advertisementBody(
            String profileId,
            double frequencyHz
    ) {
        CompoundTag body = new CompoundTag();
        body.putString("ssid", apSsid);
        body.putString("security", apSecurity);
        body.putString("network_profile", profileId);
        body.putDouble("frequency_hz", frequencyHz);
        return body;
    }

    private WifiMacFrame frame(
            int frameControl,
            String address1,
            String address2,
            String address3,
            CompoundTag body
    ) {
        csma.consumeBackoffForSimplifiedExecution();

        int seq = sequenceNumber++ & 0x0FFF;

        return new WifiMacFrame(
                frameControl,
                0,
                parseMac(address1),
                parseMac(address2),
                parseMac(address3),
                seq << 4,
                encode(body)
        );
    }

    private static byte[] encode(CompoundTag tag) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            NbtIo.write(tag, out);
            return bytes.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to encode Wi-Fi MAC body",
                    exception
            );
        }
    }

    private static CompoundTag decode(byte[] bytes) {
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(bytes);
            DataInputStream in = new DataInputStream(input);
            CompoundTag tag = NbtIo.read(in);
            return tag == null ? new CompoundTag() : tag;
        } catch (Exception exception) {
            return new CompoundTag();
        }
    }

    private static byte[] parseMac(String mac) {
        String normalized =
                mac.replace(":", "")
                        .replace("-", "");

        if (normalized.length() != 12) {
            throw new IllegalArgumentException("Invalid MAC: " + mac);
        }

        byte[] result = new byte[6];

        for (int i = 0; i < 6; i++) {
            result[i] = (byte) Integer.parseInt(
                    normalized.substring(i * 2, i * 2 + 2),
                    16
            );
        }

        return result;
    }

    private static String formatMac(byte[] mac) {
        StringBuilder result = new StringBuilder(17);

        for (int i = 0; i < mac.length; i++) {
            if (i > 0) {
                result.append(':');
            }

            result.append(String.format("%02X", mac[i] & 0xFF));
        }

        return result.toString();
    }

    private static String normalizeMac(String mac) {
        return formatMac(parseMac(mac));
    }

    private static boolean macEquals(String a, String b) {
        return normalizeMac(a).equalsIgnoreCase(normalizeMac(b));
    }

    private void requireStation() {
        if (mode != WifiMode.STATION) {
            throw new IllegalStateException(
                    "Wi-Fi interface is not in STATION mode"
            );
        }
    }

    private void resetStation() {
        stationState = WifiStationState.DISCONNECTED;
        selectedSsid = "";
        selectedBssid = "";
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Mode", mode.name());
        tag.putString("StationState", stationState.name());
        tag.putString("ApSsid", apSsid);
        tag.putString("ApSecurity", apSecurity);
        tag.putString("SelectedSsid", selectedSsid);
        tag.putString("SelectedBssid", selectedBssid);
        tag.putInt("SequenceNumber", sequenceNumber);
        tag.putInt("LastAckedSequence", lastAckedSequence);
        return tag;
    }

    public void load(CompoundTag tag) {
        try {
            mode = WifiMode.valueOf(tag.getString("Mode"));
        } catch (Exception ignored) {
            mode = WifiMode.LEGACY_DIRECT;
        }

        try {
            stationState =
                    WifiStationState.valueOf(
                            tag.getString("StationState")
                    );
        } catch (Exception ignored) {
            stationState = WifiStationState.DISCONNECTED;
        }

        apSsid = tag.getString("ApSsid");
        apSecurity = tag.getString("ApSecurity");
        selectedSsid = tag.getString("SelectedSsid");
        selectedBssid = tag.getString("SelectedBssid");
        sequenceNumber = tag.getInt("SequenceNumber");
        lastAckedSequence = tag.getInt("LastAckedSequence");

        discovered.clear();
        authenticated.clear();
        associated.clear();

        if (mode == WifiMode.STATION) {
            stationState = WifiStationState.DISCONNECTED;
            selectedSsid = "";
            selectedBssid = "";
        }
    }
}
