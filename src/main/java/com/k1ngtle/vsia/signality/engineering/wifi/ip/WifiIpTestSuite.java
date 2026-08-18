package com.k1ngtle.vsia.signality.engineering.wifi.ip;

import java.util.List;

public final class WifiIpTestSuite {
    private WifiIpTestSuite() {
    }

    public static List<WifiIpTestResult> runAll() {
        return List.of(
                rfc1071Checksum(),
                ipv4HeaderSelfCheck(),
                icmpEchoStructure(),
                udpChecksumDeterministic(),
                flowRttAndJitter()
        );
    }

    private static WifiIpTestResult rfc1071Checksum() {
        byte[] bytes =
                new byte[] {
                        0x00, 0x01,
                        (byte) 0xF2, 0x03,
                        (byte) 0xF4, (byte) 0xF5,
                        (byte) 0xF6, (byte) 0xF7
                };

        return result(
                "wifi-w19-rfc1071-checksum",
                InternetChecksum.compute(
                        bytes
                ) == 0x220D,
                "RFC 1071 numerical example sum 0xDDF2 must complement to checksum 0x220D"
        );
    }

    private static WifiIpTestResult ipv4HeaderSelfCheck() {
        Ipv4Header header =
                new Ipv4Header(
                        "192.168.1.100",
                        "192.168.1.2",
                        17,
                        64,
                        0x1234,
                        32,
                        true
                );

        byte[] encoded =
                header.encode();

        return result(
                "wifi-w19-ipv4-header-checksum",
                encoded.length == 20
                        && InternetChecksum.compute(
                        encoded
                ) == 0,
                "Encoded IPv4 header must be 20 bytes and verify to a zero complement checksum"
        );
    }

    private static WifiIpTestResult icmpEchoStructure() {
        IcmpEchoMessage echo =
                new IcmpEchoMessage(
                        false,
                        0x5653,
                        7,
                        new byte[] {
                                1,
                                2,
                                3,
                                4
                        }
                );

        byte[] encoded =
                echo.encode();

        return result(
                "wifi-w19-icmp-echo",
                encoded[0] == 8
                        && encoded[1] == 0
                        && InternetChecksum.compute(
                        encoded
                ) == 0,
                "ICMP Echo Request must use type 8/code 0 and a valid Internet checksum"
        );
    }

    private static WifiIpTestResult udpChecksumDeterministic() {
        UdpDatagram udp =
                new UdpDatagram(
                        49152,
                        40001,
                        new byte[] {
                                10,
                                20,
                                30,
                                40,
                                50
                        }
                );

        int a =
                udp.checksum(
                        "192.168.1.100",
                        "192.168.1.2"
                );

        int b =
                udp.checksum(
                        "192.168.1.100",
                        "192.168.1.2"
                );

        return result(
                "wifi-w19-udp-checksum",
                a != 0
                        && a == b
                        && udp.encode(
                        "192.168.1.100",
                        "192.168.1.2"
                ).length == 13,
                "UDP checksum must include pseudo-header/address material and remain deterministic"
        );
    }

    private static WifiIpTestResult flowRttAndJitter() {
        WifiIpFlowTracker tracker =
                new WifiIpFlowTracker();

        tracker.recordTx(
                1L,
                1_000_000L,
                64,
                "ICMP"
        );

        tracker.recordRx(
                1L,
                1_025_000L,
                64,
                "ICMP"
        );

        tracker.recordTx(
                2L,
                2_000_000L,
                64,
                "ICMP"
        );

        tracker.recordRx(
                2L,
                2_035_000L,
                64,
                "ICMP"
        );

        WifiIpFlowSnapshot snapshot =
                tracker.snapshot(
                        "192.168.1.100",
                        "192.168.1.2",
                        "AA:BB:CC:DD:EE:FF"
                );

        return result(
                "wifi-w19-flow-rtt-jitter",
                Math.abs(
                        snapshot.lastRttMs()
                                - 35.0
                ) < 1.0E-9
                        && Double.isFinite(
                        snapshot.jitterMs()
                )
                        && snapshot.txPackets() == 2
                        && snapshot.rxPackets() == 2,
                "Flow tracker must derive RTT/jitter and packet counters from request/response timestamps"
        );
    }

    private static WifiIpTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new WifiIpTestResult(
                id,
                passed,
                detail
        );
    }
}
