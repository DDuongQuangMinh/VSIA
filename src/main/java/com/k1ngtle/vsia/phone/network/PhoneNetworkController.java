package com.k1ngtle.vsia.phone.network;

import com.k1ngtle.vsia.phone.browser.BrowserRequest;
import com.k1ngtle.vsia.phone.browser.BrowserResponse;

import java.util.List;

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

    public BrowserResponse request(BrowserRequest request) {
        PhoneNetworkState state = PhoneNetworkState.get();

        if (state.isWifiUsable()) {
            return wifi.request(request);
        }

        if (state.isCellularUsable() || state.getCellular().enabled()) {
            return cellular.request(request);
        }

        return BrowserResponse.networkError(
                "Your iPhone is not connected",
                List.of("to the Internet."),
                true
        );
    }
}
