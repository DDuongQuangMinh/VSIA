package com.k1ngtle.vsia.phone.network;

import java.util.List;

public final class PhoneWifiInterface {
    private int ticksInStage;

    public void beginConnect(String ssid, int rssiDbm, boolean locked) {
        PhoneNetworkState.get().beginWifiConnection(ssid, rssiDbm, locked);
        ticksInStage = 0;
    }

    public void tick() {
        PhoneNetworkState state = PhoneNetworkState.get();
        PhoneNetworkState.WifiState wifi = state.getWifi();

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

    public PhoneNetworkRoute browserRoute() {
        if (!PhoneNetworkState.get().isWifiUsable()) {
            return null;
        }

        return new PhoneNetworkRoute(
                PhoneNetworkRoute.Transport.WIFI,
                List.of(
                        "Browser",
                        "Wi-Fi",
                        "802.11",
                        "AP",
                        "Router",
                        "DNS",
                        "HTTP"
                )
        );
    }
}
