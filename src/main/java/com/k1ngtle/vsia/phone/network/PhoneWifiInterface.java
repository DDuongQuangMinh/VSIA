package com.k1ngtle.vsia.phone.network;

import com.k1ngtle.vsia.phone.browser.BrowserRequest;
import com.k1ngtle.vsia.phone.browser.BrowserResponse;
import com.k1ngtle.vsia.phone.browser.WebsiteRenderer;

import java.util.List;

public final class PhoneWifiInterface {
    private int ticksInStage;

    public void beginConnect(String ssid, int rssiDbm, boolean locked) {
        PhoneNetworkState.get().beginWifiConnection(ssid, rssiDbm, locked);
        ticksInStage = 0;
    }

    public void tick() {
        var state = PhoneNetworkState.get();
        var wifi = state.getWifi();

        if (!wifi.enabled()) {
            return;
        }

        PhoneNetworkState.WifiStage stage = wifi.stage();
        if (stage == PhoneNetworkState.WifiStage.IDLE
                || stage == PhoneNetworkState.WifiStage.CONNECTED
                || stage == PhoneNetworkState.WifiStage.FAILED) {
            return;
        }

        ticksInStage++;
        if (ticksInStage < 8) {
            return;
        }
        ticksInStage = 0;

        switch (stage) {
            case SCAN -> state.setWifiStage(PhoneNetworkState.WifiStage.AUTHENTICATION);
            case AUTHENTICATION -> state.setWifiStage(PhoneNetworkState.WifiStage.ASSOCIATION);
            case ASSOCIATION -> {
                state.completeWifiAssociation();
                state.setWifiStage(PhoneNetworkState.WifiStage.DHCP);
            }
            case DHCP -> {
                state.completeWifiDhcp();
                state.setWifiStage(PhoneNetworkState.WifiStage.GATEWAY);
            }
            case GATEWAY -> {
                state.completeWifiGateway();
                state.setWifiStage(PhoneNetworkState.WifiStage.DNS);
            }
            case DNS -> {
                state.completeWifiDns();
                state.completeWifiConnection();
            }
            default -> {
            }
        }
    }

    public BrowserResponse request(BrowserRequest request) {
        PhoneNetworkState state = PhoneNetworkState.get();

        if (!state.isWifiUsable()) {
            return BrowserResponse.networkError(
                    "Your iPhone is not connected",
                    List.of(
                            "to the Internet.",
                            "",
                            "Wi-Fi is not connected."
                    ),
                    true
            );
        }

        PhoneNetworkRoute route = new PhoneNetworkRoute(
                PhoneNetworkRoute.Transport.WIFI,
                List.of("Browser", "Wi-Fi", "802.11", "AP", "Router", "DNS", "HTTP")
        );

        return WebsiteRenderer.handle(request, route);
    }
}
