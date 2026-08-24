package com.k1ngtle.vsia.signality.engineering.wifi.ip;

import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.live.TcpLiveSnapshot;

public final class WifiIpEngineeringService {
    private WifiIpEngineeringService() {
    }

    public static WifiIpEngineeringSnapshot snapshot(
            NetworkDeviceBlockEntity device
    ) {
        WifiIpFlowSnapshot flow =
                device.wifiIpFlowSnapshot();

        TcpLiveSnapshot tcp =
                device.wifiTcpLiveSnapshot();

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
                tcp.state(),
                tcp.localPort(),
                tcp.remotePort(),
                tcp.congestionWindowBytes(),
                tcp.slowStartThresholdBytes(),
                tcp.bytesInFlight(),
                tcp.srttMs(),
                tcp.rtoMs(),
                tcp.retransmissions(),
                tcp.status(),
                flow.lastStatus(),
                device.wifiRouterEngineeringSnapshot()
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

        if (action == null) {
            device.setWifiIpStatus(
                    "Wi-Fi IP action is null"
            );
            return snapshot(device);
        }

        if (action == WifiIpAction.CLEAR_METRICS) {
            device.clearWifiIpMetrics();
            device.clearWifiTcpLive();

            return snapshot(device);
        }

        if (action == WifiIpAction.RAW_HTTP_WORKFLOW) {
            device.startWifiRawHttpWorkflow(
                    "www.vsia-net.com",
                    "/"
            );

            return snapshot(device);
        }

        NetworkDeviceBlockEntity peer =
                WifiIpPeerResolver.resolve(
                        device
                );

        if (peer == null) {
            device.setWifiIpStatus(
                    "No associated Wi-Fi peer found"
            );

            return snapshot(device);
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

            case TCP_HTTP_GET ->
                    device.startWifiTcpHttpGet(
                            peer.wifiMacAddress(),
                            peer.wifiIpAddress(),
                            "/"
                    );

            case TCP_CLOSE ->
                    device.closeWifiTcpLive();

            case RAW_HTTP_WORKFLOW, CLEAR_METRICS -> {
            }
        }

        return snapshot(device);
    }
}