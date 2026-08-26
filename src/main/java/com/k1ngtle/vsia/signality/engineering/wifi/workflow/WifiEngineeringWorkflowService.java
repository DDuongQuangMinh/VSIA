package com.k1ngtle.vsia.signality.engineering.wifi.workflow;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiMode;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiNetworkRecord;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiStationState;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;

import java.util.List;

public final class WifiEngineeringWorkflowService {
    private WifiEngineeringWorkflowService() {
    }

    public static WifiEngineeringWorkflowSnapshot snapshot(
            NetworkDeviceBlockEntity device,
            String status
    ) {
        // W1.21.2 BEST AP WORKFLOW
        List<String> discovered =
                device.discoveredWifiNetworks()
                        .stream()
                        .filter(
                                value ->
                                        value != null
                                                && value.ssid() != null
                                                && !value.ssid().isBlank()
                        )
                        .map(
                                value ->
                                        value.ssid()
                                                + " ["
                                                + value.bssid()
                                                + "]"
                        )
                        .toList();

        return new WifiEngineeringWorkflowSnapshot(
                device.wifiMacAddress(),
                device.wifiMode(),
                device.wifiStationState(),
                device.wifiSecurityState(),
                discovered,
                List.copyOf(
                        device.wifiAssociatedStations()
                ),
                device.wifiPendingDataTransmissions(),
                device.wifiSecurityDiagnostic()
                        + " | "
                        + device.wifiContentionDiagnostic(),
                status
        );
    }

    public static WifiEngineeringWorkflowSnapshot execute(
            NetworkDeviceBlockEntity device,
            WifiEngineeringWorkflowAction action
    ) {
        if (device == null) {
            throw new IllegalArgumentException(
                    "device"
            );
        }

        if (action == null) {
            return snapshot(
                    device,
                    "No workflow action supplied"
            );
        }

        String status =
                switch (action) {
                    case CONFIGURE_AP ->
                            configureAp(
                                    device
                            );

                    case CONFIGURE_ROAM_AP ->
                            configureRoamAp(
                                    device
                            );

                    case CONFIGURE_STATION ->
                            configureStation(
                                    device
                            );

                    case SEND_BEACON ->
                            device.sendWifiBeacon()
                                    ? "Beacon queued through the real Wi-Fi MAC/RF path"
                                    : "Beacon rejected: device is not configured as an AP";

                    case SCAN ->
                            device.scanWifi()
                                    ? "Active scan started; watch PROBE_REQ / PROBE_RESP in View: PACKETS"
                                    : "Scan rejected: configure this endpoint as STATION first";

                    case CONNECT_FIRST ->
                            connectFirst(
                                    device
                            );

                    case ROAM_BEST ->
                            roamBest(
                                    device
                            );

                    case SEND_DATA ->
                            device.sendWifiEngineeringAssociatedData(
                                    WifiEngineeringWorkflowLogic
                                            .DEFAULT_DATA_BYTES
                            )
                                    ? "Associated unicast DATA queued; watch DATA / ACK / retry events"
                                    : "DATA rejected: endpoint is not associated or AP has no associated station";

                    case CONTENTION_BURST ->
                            contentionBurst(
                                    device
                            );

                    case LEGACY_DIRECT -> {
                        device.useLegacyWifiDirectMode();
                        yield "Returned Wi-Fi MAC to LEGACY_DIRECT";
                    }
                };

        return snapshot(
                device,
                status
        );
    }

    private static String configureAp(
            NetworkDeviceBlockEntity device
    ) {
        String ssid =
                WifiEngineeringWorkflowLogic
                        .defaultApSsid(
                                device.wifiMacAddress()
                        );

        boolean configured =
                device.configureWifiAccessPoint(
                        ssid,
                        ""
                );

        if (!configured) {
            return "AP configuration rejected: target is not a Wi-Fi profile";
        }

        device.sendWifiBeacon();

        return "ACCESS_POINT configured as "
                + ssid
                + "; initial beacon queued";
    }

    private static String configureRoamAp(
            NetworkDeviceBlockEntity device
    ) {
        boolean configured =
                device.configureWifiAccessPoint(
                        WifiEngineeringWorkflowLogic
                                .ROAM_LAB_SSID,
                        ""
                );

        if (!configured) {
            return "Roam AP configuration rejected: target is not a Wi-Fi profile";
        }

        device.sendWifiBeacon();

        return "ROAM AP configured as "
                + WifiEngineeringWorkflowLogic
                .ROAM_LAB_SSID
                + "; use the same SSID on two APs";
    }

    private static String configureStation(
            NetworkDeviceBlockEntity device
    ) {
        boolean configured =
                device.configureWifiStation(
                        ""
                );

        return configured
                ? "STATION configured; press Scan to discover APs"
                : "Station configuration rejected: target is not a Wi-Fi profile";
    }

    private static String roamBest(
            NetworkDeviceBlockEntity device
    ) {
        if (device.wifiMode()
                != WifiMode.STATION
                || device.wifiStationState()
                != WifiStationState.ASSOCIATED) {
            return "Roam rejected: station must already be ASSOCIATED";
        }

        WifiNetworkRecord candidate =
                device.bestWifiRoamCandidate();

        if (candidate == null) {
            return "Manual roam held: no fresh alternate AP with the same SSID/security";
        }

        String currentBssid =
                device.wifiSelectedBssid();

        double currentSnr =
                device.wifiSelectedApSnrDb();

        double candidateSnr =
                device.discoveredWifiNetworkSnrDb(
                        candidate.bssid()
                );

        double hysteresisDb =
                device.wifiRoamHysteresisDb();

        String currentQuality =
                Double.isFinite(currentSnr)
                        ? String.format(
                        java.util.Locale.ROOT,
                        "%.1f dB",
                        currentSnr
                )
                        : "n/a";

        String candidateQuality =
                Double.isFinite(candidateSnr)
                        ? String.format(
                        java.util.Locale.ROOT,
                        "%.1f dB",
                        candidateSnr
                )
                        : "n/a";

        boolean meetsHysteresis =
                !Double.isFinite(
                        currentSnr
                )
                        || (
                        Double.isFinite(
                                candidateSnr
                        )
                                && candidateSnr
                                >= currentSnr
                                + hysteresisDb
                );

        if (!meetsHysteresis) {
            return "Manual roam held "
                    + shortBssid(currentBssid)
                    + " -> "
                    + shortBssid(candidate.bssid())
                    + " | "
                    + currentQuality
                    + " -> "
                    + candidateQuality
                    + " | requires +"
                    + String.format(
                    java.util.Locale.ROOT,
                    "%.1f dB",
                    hysteresisDb
            );
        }

        boolean started =
                device.roamWifiToBestCandidate(
                        hysteresisDb
                );

        return started
                ? "Manual roam started "
                + shortBssid(currentBssid)
                + " -> "
                + shortBssid(candidate.bssid())
                + " | "
                + currentQuality
                + " -> "
                + candidateQuality
                + " | hysteresis +"
                + String.format(
                java.util.Locale.ROOT,
                "%.1f dB",
                hysteresisDb
        )
                + " satisfied"
                : "Manual roam request to "
                + shortBssid(candidate.bssid())
                + " was rejected";
    }

    private static String shortBssid(
            String bssid
    ) {
        if (bssid == null
                || bssid.isBlank()) {
            return "n/a";
        }

        String normalized =
                bssid.toUpperCase(
                        java.util.Locale.ROOT
                );

        return normalized.length() <= 8
                ? normalized
                : normalized.substring(
                normalized.length() - 8
        );
    }

    private static String contentionBurst(
            NetworkDeviceBlockEntity device
    ) {
        int requested =
                32;

        int accepted =
                device.sendWifiContentionBurst(
                        requested,
                        1024
                );

        return "W1.23 contention burst "
                + accepted
                + "/"
                + requested
                + " accepted | "
                + device.wifiContentionDiagnostic();
    }

    private static String connectFirst(
            NetworkDeviceBlockEntity device
    ) {
        if (device.wifiMode()
                != WifiMode.STATION) {
            return "Connect rejected: configure this endpoint as STATION first";
        }

        WifiNetworkRecord selected =
                device.bestDiscoveredWifiNetwork();

        if (selected == null
                || selected.ssid() == null
                || selected.ssid().isBlank()
                || selected.bssid() == null
                || selected.bssid().isBlank()) {
            return "No fresh discovered AP. Press Scan, wait for PROBE_RESP, then Connect";
        }

        double selectedSnrDb =
                device.discoveredWifiNetworkSnrDb(
                        selected.bssid()
                );

        String quality =
                Double.isFinite(
                        selectedSnrDb
                )
                        ? String.format(
                        java.util.Locale.ROOT,
                        "%.1f dB",
                        selectedSnrDb
                )
                        : "SNR n/a";

        return device.connectWifiBssid(
                selected.ssid(),
                selected.bssid()
        )
                ? "Authentication started with "
                + selected.ssid()
                + " BSSID "
                + selected.bssid()
                + " | "
                + quality
                + "; watch AUTH / ASSOC in View: PACKETS"
                : "Connection request to "
                + selected.ssid()
                + " BSSID "
                + selected.bssid()
                + " was rejected";
    }
}
