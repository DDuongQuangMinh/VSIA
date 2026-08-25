package com.k1ngtle.vsia.signality.engineering.wifi.ip;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.live.TcpLiveSnapshot;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.ServerRackDirectory;
import net.minecraft.server.level.ServerLevel;

public final class WifiIpEngineeringService {
    private static final String DEFAULT_SERVER_IP =
            "192.168.1.2";

    private static final String DEFAULT_SERVER_HOST =
            "www.vsia-net.com";

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

        if (action == WifiIpAction.TCP_CLOSE) {
            device.closeWifiTcpLive();

            return snapshot(device);
        }

        if (action == WifiIpAction.DHCP_DISCOVER) {
            device.requestDynamicIp();

            return snapshot(device);
        }

        if (action == WifiIpAction.RAW_HTTP_WORKFLOW) {
            device.startWifiRawHttpWorkflow(
                    DEFAULT_SERVER_HOST,
                    "/"
            );

            return snapshot(device);
        }

        ServerRackBlockEntity server =
                resolveEngineeringServer(
                        device
                );

        if (server == null) {
            device.setWifiIpStatus(
                    "No Server Rack endpoint found. Expected "
                            + DEFAULT_SERVER_HOST
                            + " or "
                            + DEFAULT_SERVER_IP
                            + "; connect AP -> Switch -> Server Rack"
            );

            return snapshot(device);
        }

        String serverIp =
                server.wifiIpAddress();

        String serverMac =
                server.wifiMacAddress();

        device.configureWifiIpPeer(
                serverIp,
                serverMac
        );

        switch (action) {
            case ARP_RESOLVE -> {
                boolean queued =
                        device.sendWifiArpRequest(
                                serverIp
                        );

                device.setWifiIpStatus(
                        queued
                                ? "ARP to Server Rack "
                                + serverIp
                                + " queued"
                                : "ARP to Server Rack rejected"
                );
            }

            case ICMP_ECHO -> {
                boolean queued =
                        device.sendWifiIcmpEcho(
                                serverMac,
                                serverIp,
                                32
                        );

                device.setWifiIpStatus(
                        queued
                                ? "ICMP to Server Rack "
                                + serverIp
                                + " queued"
                                : "ICMP to Server Rack rejected"
                );
            }

            case UDP_ECHO -> {
                boolean queued =
                        device.sendWifiUdpEcho(
                                serverMac,
                                serverIp,
                                512
                        );

                device.setWifiIpStatus(
                        queued
                                ? "UDP Echo to Server Rack "
                                + serverIp
                                + " queued"
                                : "UDP Echo to Server Rack rejected"
                );
            }

            case HTTP_GET -> {
                boolean queued =
                        device.sendWifiHttpGet(
                                serverMac,
                                serverIp,
                                "/"
                        );

                device.setWifiIpStatus(
                        queued
                                ? "HTTP Direct to Server Rack "
                                + serverIp
                                + " queued"
                                : "HTTP Direct to Server Rack rejected"
                );
            }

            case TCP_HTTP_GET -> {
                boolean started =
                        device.startWifiTcpHttpGet(
                                serverMac,
                                serverIp,
                                "/"
                        );

                device.setWifiIpStatus(
                        started
                                ? "TCP HTTP to Server Rack "
                                + serverIp
                                + " started"
                                : "TCP HTTP to Server Rack rejected"
                );
            }

            case DHCP_DISCOVER,
                 RAW_HTTP_WORKFLOW,
                 TCP_CLOSE,
                 CLEAR_METRICS -> {
            }
        }

        return snapshot(device);
    }

    private static ServerRackBlockEntity resolveEngineeringServer(
            NetworkDeviceBlockEntity device
    ) {
        ServerLevel serverLevel =
                device.level();

        if (serverLevel == null) {
            return null;
        }

        String resolvedIp =
                ServerRackDirectory.resolve(
                        serverLevel,
                        DEFAULT_SERVER_HOST
                );

        if (resolvedIp != null
                && !resolvedIp.isBlank()) {
            ServerRackBlockEntity byDns =
                    ServerRackDirectory.byIp(
                            serverLevel,
                            resolvedIp
                    );

            if (byDns != null) {
                return byDns;
            }
        }

        return ServerRackDirectory.byIp(
                serverLevel,
                DEFAULT_SERVER_IP
        );
    }
}
