package com.k1ngtle.vsia.phone.network;

import com.k1ngtle.vsia.phone.browser.BrowserResponse;

public final class PhoneNetworkController {
    private static final PhoneNetworkController INSTANCE = new PhoneNetworkController();

    private final PhoneWifiInterface wifi = new PhoneWifiInterface();
    private final PhoneCellularInterface cellular = new PhoneCellularInterface();

    private PhoneNetworkController() {
    }

    public static PhoneNetworkController get() {
        return INSTANCE;
    }

    public void tick() {
        wifi.tick();
    }

    public void connectWifi(String ssid, int rssiDbm, boolean locked) {
        wifi.beginConnect(ssid, rssiDbm, locked);
    }

    public void setWifiEnabled(boolean enabled) {
        PhoneNetworkState.get().setWifiEnabled(enabled);
    }

    public void setCellularEnabled(boolean enabled) {
        PhoneNetworkState.get().setCellularEnabled(enabled);
    }

    public PhoneNetworkRoute selectBrowserRoute() {
        PhoneNetworkRoute wifiRoute = wifi.browserRoute();

        if (wifiRoute != null) {
            return wifiRoute;
        }

        return cellular.browserRoute();
    }

    public BrowserResponse browserUnavailable(String url) {
        PhoneNetworkState state = PhoneNetworkState.get();

        if (state.getWifi().enabled()
                && !state.isWifiUsable()
                && !state.isCellularUsable()) {
            return BrowserResponse.networkError(
                    url,
                    "Your iPhone is not connected to the Internet.",
                    "Wi-Fi is enabled but is not connected to a usable network.",
                    true
            );
        }

        if (state.getCellular().enabled() && !state.isCellularUsable()) {
            return BrowserResponse.networkError(
                    url,
                    "Cellular data is unavailable.",
                    "RRC: " + state.getCellular().rrcState()
                            + "\nNAS: " + state.getCellular().nasState()
                            + "\nPDU: " + state.getCellular().pduState(),
                    false
            );
        }

        return BrowserResponse.networkError(
                url,
                "Your iPhone is not connected to the Internet.",
                "Turn on Wi-Fi or Cellular Data and connect to a network.",
                true
        );
    }
}
