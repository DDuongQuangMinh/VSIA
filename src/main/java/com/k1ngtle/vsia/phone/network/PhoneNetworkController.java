package com.k1ngtle.vsia.phone.network;

import com.k1ngtle.vsia.phone.browser.BrowserResponse;

public final class PhoneNetworkController {
    private static final PhoneNetworkController INSTANCE =
            new PhoneNetworkController();

    private final PhoneWifiInterface wifi =
            new PhoneWifiInterface();

    private final PhoneCellularInterface cellular =
            new PhoneCellularInterface();

    private final PhoneSatelliteInterface satellite =
            new PhoneSatelliteInterface();

    private PhoneNetworkController() {
    }

    public static PhoneNetworkController get() {
        return INSTANCE;
    }

    public void tick() {
        wifi.tick();
    }

    public void connectWifi(
            String ssid,
            int rssiDbm,
            boolean locked
    ) {
        wifi.beginConnect(
                ssid,
                rssiDbm,
                locked
        );
    }

    public void setWifiEnabled(
            boolean enabled
    ) {
        PhoneNetworkState.get()
                .setWifiEnabled(
                        enabled
                );
    }

    public void setCellularEnabled(
            boolean enabled
    ) {
        PhoneNetworkState.get()
                .setCellularEnabled(
                        enabled
                );
    }

    public PhoneNetworkRoute selectBrowserRoute() {
        PhoneNetworkRoute wifiRoute =
                wifi.browserRoute();

        if (wifiRoute != null) {
            return wifiRoute;
        }

        PhoneNetworkRoute cellularRoute =
                cellular.browserRoute();

        if (cellularRoute != null) {
            return cellularRoute;
        }

        return satellite.browserRoute();
    }

    public BrowserResponse browserUnavailable(
            String url
    ) {
        PhoneNetworkState state =
                PhoneNetworkState.get();

        if (state.getWifi()
                .enabled()
                && !state.isWifiUsable()
                && !state.isCellularUsable()) {
            return BrowserResponse.networkError(
                    url,
                    "Your iPhone is not connected to the Internet.",
                    "Wi-Fi is enabled but is not connected to a usable network. Satellite service also requires a nearby VS:IA terminal and a visible gateway path.",
                    true
            );
        }

        if (state.getCellular()
                .enabled()
                && !state.isCellularUsable()) {
            return BrowserResponse.networkError(
                    url,
                    "Cellular data is unavailable.",
                    "RRC: "
                            + state.getCellular()
                            .rrcState()
                            + "\nNAS: "
                            + state.getCellular()
                            .nasState()
                            + "\nPDU: "
                            + state.getCellular()
                            .pduState()
                            + "\nSatellite fallback requires a nearby terminal.",
                    false
            );
        }

        return BrowserResponse.networkError(
                url,
                "No Internet route is available.",
                "Use Wi-Fi, Cellular Data, or stand near a satellite user terminal with a reachable Internet gateway.",
                true
        );
    }
}
