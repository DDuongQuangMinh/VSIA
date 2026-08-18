package com.k1ngtle.vsia.signality.engineering.wifi.workflow;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiMode;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiNetworkRecord;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;

import java.util.List;

public final class WifiEngineeringWorkflowService {
    private WifiEngineeringWorkflowService() {
    }

    public static WifiEngineeringWorkflowSnapshot snapshot(
            NetworkDeviceBlockEntity device,
            String status
    ) {
        List<String> discovered =
                device.discoveredWifiNetworks()
                        .stream()
                        .map(
                                WifiNetworkRecord::ssid
                        )
                        .filter(
                                value ->
                                        value != null
                                                && !value.isBlank()
                        )
                        .distinct()
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
                device.wifiSecurityDiagnostic(),
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

                    case SEND_DATA ->
                            device.sendWifiEngineeringAssociatedData(
                                    WifiEngineeringWorkflowLogic
                                            .DEFAULT_DATA_BYTES
                            )
                                    ? "Associated unicast DATA queued; watch DATA / ACK / retry events"
                                    : "DATA rejected: endpoint is not associated or AP has no associated station";

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

    private static String connectFirst(
            NetworkDeviceBlockEntity device
    ) {
        if (device.wifiMode()
                != WifiMode.STATION) {
            return "Connect rejected: configure this endpoint as STATION first";
        }

        String ssid =
                WifiEngineeringWorkflowLogic
                        .firstDiscoveredSsid(
                                device.discoveredWifiNetworks()
                                        .stream()
                                        .map(
                                                WifiNetworkRecord::ssid
                                        )
                                        .toList()
                        );

        if (ssid.isBlank()) {
            return "No discovered AP. Press Scan, wait for PROBE_RESP, then Connect";
        }

        return device.connectWifi(
                ssid
        )
                ? "Authentication started with "
                + ssid
                + "; watch AUTH / ASSOC in View: PACKETS"
                : "Connection request to "
                + ssid
                + " was rejected";
    }
}
