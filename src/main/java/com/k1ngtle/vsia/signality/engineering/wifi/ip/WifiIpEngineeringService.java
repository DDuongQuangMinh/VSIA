package com.k1ngtle.vsia.signality.engineering.wifi.ip;

import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;

public final class WifiIpEngineeringService {
    private WifiIpEngineeringService() {
    }

    public static WifiIpEngineeringSnapshot snapshot(
            NetworkDeviceBlockEntity device
    ) {
        NetworkDeviceBlockEntity peer =
                WifiIpPeerResolver.resolve(
                        device
                );

        if (peer != null) {
            device.configureWifiIpPeer(
                    peer.wifiIpAddress(),
                    peer.wifiMacAddress()
            );
        }

        WifiIpFlowSnapshot flow =
                device.wifiIpFlowSnapshot();

        return new WifiIpEngineeringSnapshot(
                device.wifiIpAddress(),
                device.wifiMacAddress(),
                flow.peerIp(),
                flow.peerMac(),
                device.wifiIpNeighborCount(),
                flow.txPackets(),
                flow.rxPackets(),
                flow.txBytes(),
                flow.rxBytes(),
                flow.lostPackets(),
                flow.lastRttMs(),
                flow.averageRttMs(),
                flow.jitterMs(),
                flow.goodputKbps(),
                flow.lastProtocol(),
                flow.lastStatus()
        );
    }

    public static WifiIpEngineeringSnapshot execute(
            NetworkDeviceBlockEntity device,
            WifiIpAction action
    ) {
        if (device == null) {
            throw new IllegalArgumentException(
                    "device"
            );
        }

        if (action == WifiIpAction.CLEAR_METRICS) {
            device.clearWifiIpMetrics();
            return snapshot(
                    device
            );
        }

        NetworkDeviceBlockEntity peer =
                WifiIpPeerResolver.resolve(
                        device
                );

        if (peer == null) {
            device.setWifiIpStatus(
                    "No associated Wi-Fi peer found"
            );

            return snapshot(
                    device
            );
        }

        device.configureWifiIpPeer(
                peer.wifiIpAddress(),
                peer.wifiMacAddress()
        );

        switch (action) {
            case DHCP_DISCOVER ->
                    device.requestDynamicIp();

            case ARP_RESOLVE ->
                    device.sendWifiArpRequest(
                            peer.wifiIpAddress()
                    );

            case ICMP_ECHO ->
                    device.sendWifiIcmpEcho(
                            peer.wifiMacAddress(),
                            peer.wifiIpAddress(),
                            32
                    );

            case UDP_ECHO ->
                    device.sendWifiUdpEcho(
                            peer.wifiMacAddress(),
                            peer.wifiIpAddress(),
                            512
                    );

            case HTTP_GET ->
                    device.sendWifiHttpGet(
                            peer.wifiMacAddress(),
                            peer.wifiIpAddress(),
                            "/"
                    );

            case CLEAR_METRICS -> {
            }
        }

        return snapshot(
                device
        );
    }
}
