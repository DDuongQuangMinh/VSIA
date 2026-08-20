package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Decoder;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Packet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class WifiRawFragmentTestSuite {
    private WifiRawFragmentTestSuite() {
    }

    public static List<WifiRawFragmentTestResult> runAll() {
        List<WifiRawFragmentTestResult> out =
                new ArrayList<>();

        byte[] udpPayload =
                payload(
                        4000
                );

        byte[] udp =
                RawUdpCodec.encode(
                        "192.0.2.10",
                        "198.51.100.20",
                        50000,
                        40001,
                        udpPayload
                );

        byte[] raw =
                RawIpv4Encoder.encode(
                        "192.0.2.10",
                        "198.51.100.20",
                        0,
                        0x4242,
                        false,
                        false,
                        0,
                        64,
                        17,
                        udp
                );

        RawIpv4Packet base =
                RawIpv4Decoder.decode(
                        raw
                );

        out.add(
                result(
                        "wifi-w1111-ipv4-encode",
                        base.checksumValid()
                                && base.totalLength()
                                == raw.length
                                && base.protocol()
                                == 17,
                        "General raw IPv4 encoder must create a valid header"
                )
        );

        RawUdpPacket decodedUdp =
                RawUdpCodec.decode(
                        base.sourceAddress(),
                        base.destinationAddress(),
                        base.payload()
                );

        out.add(
                result(
                        "wifi-w1111-udp-checksum",
                        decodedUdp.checksumValid()
                                && Arrays.equals(
                                decodedUdp.payload(),
                                udpPayload
                        ),
                        "UDP pseudo-header checksum and payload must round-trip"
                )
        );

        List<byte[]> fragments =
                RawIpv4Fragmenter.fragment(
                        raw,
                        1500
                );

        out.add(
                result(
                        "wifi-w1111-fragment-count",
                        fragments.size() == 3,
                        "4008-byte UDP datagram should require three IPv4 fragments at MTU 1500"
                )
        );

        RawIpv4Packet f0 =
                RawIpv4Decoder.decode(
                        fragments.get(0)
                );

        RawIpv4Packet f1 =
                RawIpv4Decoder.decode(
                        fragments.get(1)
                );

        RawIpv4Packet f2 =
                RawIpv4Decoder.decode(
                        fragments.get(2)
                );

        out.add(
                result(
                        "wifi-w1111-fragment-offsets",
                        f0.fragmentOffset() == 0
                                && f1.fragmentOffset() == 185
                                && f2.fragmentOffset() == 370,
                        "Fragment offsets must be encoded in 8-byte units"
                )
        );

        out.add(
                result(
                        "wifi-w1111-mf-flags",
                        f0.moreFragments()
                                && f1.moreFragments()
                                && !f2.moreFragments(),
                        "MF must be set on all non-final fragments"
                )
        );

        out.add(
                result(
                        "wifi-w1111-fragment-mtu",
                        fragments.stream()
                                .allMatch(
                                        fragment ->
                                                fragment.length
                                                        <= 1500
                                ),
                        "Every live raw fragment must fit the egress MTU"
                )
        );

        RawIpv4ReassemblyTable table =
                new RawIpv4ReassemblyTable();

        RawIpv4ReassemblyTable.ReassemblyResult r2 =
                table.accept(
                        fragments.get(2),
                        100L
                );

        RawIpv4ReassemblyTable.ReassemblyResult r0 =
                table.accept(
                        fragments.get(0),
                        200L
                );

        RawIpv4ReassemblyTable.ReassemblyResult r1 =
                table.accept(
                        fragments.get(1),
                        300L
                );

        out.add(
                result(
                        "wifi-w1111-out-of-order-reassembly",
                        r2.status()
                                == RawIpv4ReassemblyTable.Status.WAITING
                                && r0.status()
                                == RawIpv4ReassemblyTable.Status.WAITING
                                && r1.status()
                                == RawIpv4ReassemblyTable.Status.COMPLETE,
                        "Fragments must reassemble after out-of-order arrival"
                )
        );

        RawIpv4Transport.Decoded reassembled =
                RawIpv4Transport.decode(
                        r1.rawPacket()
                );

        out.add(
                result(
                        "wifi-w1111-udp-after-reassembly",
                        reassembled.udp() != null
                                && reassembled.udp()
                                .checksumValid()
                                && Arrays.equals(
                                reassembled.udp()
                                        .payload(),
                                udpPayload
                        ),
                        "UDP must only decode after complete IPv4 reassembly"
                )
        );

        RawIpv4ReassemblyTable overlap =
                new RawIpv4ReassemblyTable();

        overlap.accept(
                fragments.get(0),
                0L
        );

        RawIpv4ReassemblyTable.ReassemblyResult overlapResult =
                overlap.accept(
                        fragments.get(0),
                        1L
                );

        out.add(
                result(
                        "wifi-w1111-overlap-reject",
                        overlapResult.status()
                                == RawIpv4ReassemblyTable.Status.REJECTED
                                && "OVERLAP".equals(
                                overlapResult.reason()
                        ),
                        "Overlapping/duplicate fragment ranges are rejected"
                )
        );

        RawIpv4ReassemblyTable expiring =
                new RawIpv4ReassemblyTable(
                        1000L
                );

        expiring.accept(
                fragments.get(0),
                100L
        );

        int expired =
                expiring.expire(
                        1200L
                );

        out.add(
                result(
                        "wifi-w1111-reassembly-timeout",
                        expired == 1
                                && expiring.pendingAssemblies()
                                == 0,
                        "Incomplete fragment assemblies must expire"
                )
        );

        byte[] dfRaw =
                RawIpv4Encoder.encode(
                        "192.0.2.10",
                        "198.51.100.20",
                        0,
                        0x5151,
                        true,
                        false,
                        0,
                        64,
                        17,
                        udp
                );

        boolean dfRejected =
                false;

        try {
            RawIpv4Fragmenter.fragment(
                    dfRaw,
                    1200
            );
        } catch (RawIpv4Fragmenter.FragmentationNeededException expected) {
            dfRejected =
                    expected.nextHopMtu()
                            == 1200;
        }

        out.add(
                result(
                        "wifi-w1111-df-frag-needed",
                        dfRejected,
                        "DF datagram larger than MTU must request Fragmentation Needed"
                )
        );

        byte[] icmpEcho =
                RawIcmpCodec.encodeEcho(
                        false,
                        0x5653,
                        7,
                        payload(
                                32
                        )
                );

        RawIcmpPacket icmpEchoDecoded =
                RawIcmpCodec.decode(
                        icmpEcho
                );

        out.add(
                result(
                        "wifi-w1111-icmp-echo",
                        icmpEchoDecoded.checksumValid()
                                && icmpEchoDecoded.type()
                                == 8
                                && ((icmpEchoDecoded.restOfHeader() >>> 16)
                                & 0xFFFF) == 0x5653
                                && (icmpEchoDecoded.restOfHeader()
                                & 0xFFFF) == 7,
                        "ICMP Echo must preserve identifier/sequence and checksum"
                )
        );

        byte[] icmpCode4 =
                RawIcmpCodec.encodeError(
                        3,
                        4,
                        1200,
                        Arrays.copyOf(
                                raw,
                                Math.min(
                                        raw.length,
                                        28
                                )
                        )
                );

        RawIcmpPacket code4 =
                RawIcmpCodec.decode(
                        icmpCode4
                );

        out.add(
                result(
                        "wifi-w1111-icmp-code4",
                        code4.checksumValid()
                                && code4.type() == 3
                                && code4.code() == 4
                                && code4.nextHopMtu()
                                == 1200,
                        "ICMP Type 3 Code 4 must expose the next-hop MTU"
                )
        );

        byte[] single =
                RawIpv4Encoder.encode(
                        "192.0.2.10",
                        "198.51.100.20",
                        0,
                        1,
                        false,
                        false,
                        0,
                        64,
                        1,
                        icmpEcho
                );

        RawIpv4ReassemblyTable.ReassemblyResult singleResult =
                new RawIpv4ReassemblyTable()
                        .accept(
                                single,
                                0L
                        );

        out.add(
                result(
                        "wifi-w1111-unfragmented-fast-path",
                        singleResult.status()
                                == RawIpv4ReassemblyTable.Status.COMPLETE
                                && Arrays.equals(
                                single,
                                singleResult.rawPacket()
                        ),
                        "Unfragmented IPv4 must bypass fragment buffering"
                )
        );

        return List.copyOf(
                out
        );
    }

    private static byte[] payload(
            int length
    ) {
        byte[] out =
                new byte[length];

        for (int i = 0;
             i < out.length;
             i++) {
            out[i] =
                    (byte) (
                            i * 37
                                    + 11
                    );
        }

        return out;
    }

    private static WifiRawFragmentTestResult result(
            String name,
            boolean passed,
            String detail
    ) {
        return new WifiRawFragmentTestResult(
                name,
                passed,
                detail
        );
    }
}
