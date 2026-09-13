package com.k1ngtle.vsia.phone.network;

import java.util.List;

public final class PhoneWifiInterface {
    public PhoneNetworkRoute browserRoute() {
        PhoneNetworkState state =
                PhoneNetworkState.get();

        if (!state.isWifiUsable()) {
            return null;
        }

        PhoneNetworkState.WifiStatus wifi =
                state.getWifi();

        return new PhoneNetworkRoute(
                        PhoneNetworkRoute.Transport.WIFI,
                        List.of(
                                "Browser",
                                "Wi-Fi",
                                String.format(
                                        "%s (%d dBm)",
                                        wifi.ssid(),
                                        wifi.rssiDbm()
                                ),
                                "802.11 AP",
                                "Router"
                        )
                );
    }
}
