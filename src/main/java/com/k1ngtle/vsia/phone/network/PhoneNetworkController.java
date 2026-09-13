package com.k1ngtle.vsia.phone.network;

import com.k1ngtle.vsia.phone.client.PhoneSystemSettings;
import com.k1ngtle.vsia.phone.browser.BrowserResponse;
import com.k1ngtle.vsia.phone.network.realism.packet.C2SPhoneWirelessActionPacket;
import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberClientState;
import com.k1ngtle.vsia.phone.subscriber.packet.C2SPhoneSubscriberActionPacket;
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

        int refreshInterval =
                PhoneSystemSettings.lowPowerMode()
                        ? 100
                        : 40;

        if (tickCounter == 1
                || tickCounter % refreshInterval == 0) {
            requestRefresh();
            FieldDeviceNetwork.sendToServer(
                    C2SPhoneSubscriberActionPacket.refresh()
            );
        }
    }

    public void requestRefresh() {
        FieldDeviceNetwork.sendToServer(
                C2SPhoneWirelessActionPacket.refresh()
        );
    }

    public void setWifiEnabled(boolean enabled) {
        FieldDeviceNetwork.sendToServer(
                C2SPhoneWirelessActionPacket.wifiEnabled(enabled)
        );
    }

    public void setCellularEnabled(boolean enabled) {
        FieldDeviceNetwork.sendToServer(
                C2SPhoneSubscriberActionPacket.dataEnabled(enabled)
        );
    }

    public void setCellularRadioEnabled(boolean enabled) {
        FieldDeviceNetwork.sendToServer(
                C2SPhoneWirelessActionPacket.cellularEnabled(enabled)
        );
    }

    public void connectWifi(String bssid) {
        connectWifi(bssid, "");
    }

    public void connectWifi(String bssid, String passphrase) {
        FieldDeviceNetwork.sendToServer(
                C2SPhoneWirelessActionPacket.wifiConnect(
                        bssid,
                        passphrase
                )
        );
    }

    public void disconnectWifi() {
        FieldDeviceNetwork.sendToServer(
                C2SPhoneWirelessActionPacket.wifiDisconnect()
        );
    }

    public void forgetWifi(String bssid) {
        FieldDeviceNetwork.sendToServer(
                C2SPhoneWirelessActionPacket.wifiForget(bssid)
        );
    }

    public void setWifiAutoJoin(String bssid, boolean enabled) {
        FieldDeviceNetwork.sendToServer(
                C2SPhoneWirelessActionPacket.wifiAutoJoin(
                        bssid,
                        enabled
                )
        );
    }

    public void connectWifi(String ssid, int rssi, boolean locked) {
        PhoneNetworkState.get()
                .getVisibleWifiNetworks()
                .stream()
                .filter(network -> network.ssid().equals(ssid))
                .max(java.util.Comparator.comparingInt(
                        PhoneNetworkState.VisibleWifiNetwork::rssiDbm
                ))
                .ifPresent(network -> connectWifi(network.bssid()));
    }

    public PhoneNetworkRoute selectBrowserRoute() {
        PhoneNetworkRoute wifiRoute = wifi.browserRoute();
        if (wifiRoute != null) {
            return wifiRoute;
        }

        if (!PhoneSubscriberClientState.get().canUseCellularData()) {
            return null;
        }

        return cellular.browserRoute();
    }

    public BrowserResponse browserUnavailable(String url) {
        return BrowserResponse.networkError(
                url,
                "Your iPhone has no usable radio access.",
                noRouteMessage(),
                PhoneNetworkState.get().getWifi().enabled()
        );
    }

    public String noRouteMessage() {
        PhoneNetworkState state = PhoneNetworkState.get();

        if (!state.getCellular().enabled()
                && !state.getWifi().enabled()) {
            return "Airplane Mode is on. Turn on Wi-Fi or turn off Airplane Mode to use network data.";
        }

        if (!PhoneSubscriberClientState.get().hasActiveSubscription()
                && !state.isWifiUsable()) {
            return "No active SIM or eSIM. Connect to Wi-Fi or install a subscriber profile before using cellular service.";
        }

        if (PhoneSubscriberClientState.get().hasActiveSubscription()
                && !PhoneSubscriberClientState.get().snapshot().cellularDataEnabled()
                && !state.isWifiUsable()) {
            return "Cellular Data is turned off. SMS remains available when cellular service is registered.";
        }

        if (state.getWifi().enabled()
                && !state.getWifi().connected()
                && state.getVisibleWifiNetworks().isEmpty()
                && !state.isCellularUsable()) {
            return "No local radio access. There is no usable Wi-Fi AP or authorized cellular service in range.";
        }

        if (state.getWifi().enabled() && !state.getWifi().connected()) {
            return "Wi-Fi is enabled but not associated. Open Wi-Fi settings and choose an in-range access point, or use cellular coverage.";
        }

        if (PhoneSubscriberClientState.get().hasActiveSubscription()
                && !state.isCellularUsable()) {
            return "Your SIM/eSIM is installed, but there is no usable cellular packet-data service at this location.";
        }

        return "No usable local phone interface. Enable Wi-Fi or Cellular Data.";
    }
}
