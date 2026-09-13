package com.k1ngtle.vsia.phone.network;

import com.k1ngtle.vsia.phone.network.realism.packet.C2SPhoneWirelessActionPacket;
import com.k1ngtle.vsia.phone.browser.BrowserResponse;
import com.k1ngtle.vsia.signality.internet.field.FieldDeviceNetwork;


public final class PhoneNetworkController {
    private static final PhoneNetworkController INSTANCE =
            new PhoneNetworkController();

    private final PhoneWifiInterface wifi =
            new PhoneWifiInterface();

    private final PhoneCellularInterface cellular =
            new PhoneCellularInterface();

    private int tickCounter;

    private PhoneNetworkController() {
    }

    public static PhoneNetworkController get() {
        return INSTANCE;
    }

    public void tick() {
        tickCounter++;

        if (tickCounter == 1
                || tickCounter % 40 == 0) {
            requestRefresh();
        }
    }

    public void requestRefresh() {
        FieldDeviceNetwork.sendToServer(
                C2SPhoneWirelessActionPacket
                        .refresh()
        );
    }

    public void setWifiEnabled(
            boolean enabled
    ) {
        FieldDeviceNetwork.sendToServer(
                C2SPhoneWirelessActionPacket
                        .wifiEnabled(
                                enabled
                        )
        );
    }

    public void setCellularEnabled(
            boolean enabled
    ) {
        FieldDeviceNetwork.sendToServer(
                C2SPhoneWirelessActionPacket
                        .cellularEnabled(
                                enabled
                        )
        );
    }

    public void connectWifi(
            String bssid
    ) {
        connectWifi(
                bssid,
                ""
        );
    }

    public void connectWifi(
            String bssid,
            String passphrase
    ) {
        FieldDeviceNetwork.sendToServer(
                C2SPhoneWirelessActionPacket
                        .wifiConnect(
                                bssid,
                                passphrase
                        )
        );
    }

    public void disconnectWifi() {
        FieldDeviceNetwork.sendToServer(
                C2SPhoneWirelessActionPacket
                        .wifiDisconnect()
        );
    }

    public void connectWifi(
            String ssid,
            int rssi,
            boolean locked
    ) {
        PhoneNetworkState
                .get()
                .getVisibleWifiNetworks()
                .stream()
                .filter(
                        network ->
                                network.ssid()
                                        .equals(
                                                ssid
                                        )
                )
                .max(
                        java.util.Comparator.comparingInt(
                                PhoneNetworkState
                                        .VisibleWifiNetwork
                                        ::rssiDbm
                        )
                )
                .ifPresent(
                        network ->
                                connectWifi(
                                        network.bssid()
                                )
                );
    }

    public PhoneNetworkRoute selectBrowserRoute() {
        PhoneNetworkRoute wifiRoute =
                wifi.browserRoute();

        if (wifiRoute != null) {
            return wifiRoute;
        }

        return cellular.browserRoute();
    }

    public BrowserResponse browserUnavailable(
            String url
    ) {
        return BrowserResponse.networkError(
                url,
                "Your iPhone has no usable radio access.",
                noRouteMessage(),
                PhoneNetworkState
                        .get()
                        .getWifi()
                        .enabled()
        );
    }

    public String noRouteMessage() {
        PhoneNetworkState state =
                PhoneNetworkState.get();

        if (state.getWifi()
                .enabled()
                && !state.getWifi()
                .connected()
                && state.getVisibleWifiNetworks()
                .isEmpty()
                && state.getCellular()
                .enabled()
                && !state.getCellular()
                .registered()) {
            return "No local radio access. There is no usable Wi-Fi AP or cellular antenna in range. Satellite remains long-haul backhaul unless a direct-to-device NTN service is provisioned.";
        }

        if (state.getWifi()
                .enabled()
                && !state.getWifi()
                .connected()) {
            return "Wi-Fi is enabled but not associated. Open Wi-Fi settings and choose an in-range access point, or use cellular coverage.";
        }

        if (state.getCellular()
                .enabled()
                && !state.isCellularUsable()) {
            return "Cellular data has no usable serving cell or packet-data bearer. Move closer to a base station or use Wi-Fi.";
        }

        return "No usable local phone interface. Enable Wi-Fi or Cellular Data.";
    }
}
