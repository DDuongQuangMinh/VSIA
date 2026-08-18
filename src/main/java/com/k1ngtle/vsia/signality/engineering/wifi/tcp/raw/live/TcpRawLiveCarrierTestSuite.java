package com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.live;

import com.k1ngtle.vsia.signality.engineering.ExecutionMode;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpFlags;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpSegment;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.live.TcpLivePacketCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.options.TcpOptionSet;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4TcpCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4TcpPacket;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public final class TcpRawLiveCarrierTestSuite {
    private TcpRawLiveCarrierTestSuite() {
    }

    public static List<TcpRawLiveCarrierTestResult> runAll() {
        return List.of(
                executionModeContract(),
                synCarrierRoundTrip(),
                dataCarrierRoundTrip(),
                rawChecksumsOnCarrier(),
                corruptedRawRejected(),
                simulationPacketUnaffected()
        );
    }

    private static TcpRawLiveCarrierTestResult executionModeContract() {
        return result(
                "wifi-w196-execution-mode",
                ExecutionMode.valueOf(
                        "SIMULATION"
                ) == ExecutionMode.SIMULATION
                        && ExecutionMode.valueOf(
                        "CONFORMANCE"
                ) == ExecutionMode.CONFORMANCE,
                "W1.9.6 must reuse the project's existing SIMULATION / CONFORMANCE execution-mode contract"
        );
    }

    private static TcpRawLiveCarrierTestResult synCarrierRoundTrip() {
        OSINetworkPacket logical =
                logicalTcp(
                        new TcpSegment(
                                50000,
                                80,
                                0x12345678L,
                                0L,
                                TcpFlags.synOnly(),
                                65535,
                                0,
                                1_000_000L,
                                false
                        ),
                        TcpOptionSet.synOffer(
                                1200,
                                2,
                                true,
                                1000L
                        ),
                        new byte[0],
                        0,
                        0
                );

        CompoundTag carrier =
                TcpRawLiveCarrierCodec.encode(
                        logical
                );

        OSINetworkPacket decoded =
                TcpRawLiveCarrierCodec.decode(
                        carrier
                );

        TcpSegment segment =
                TcpLivePacketCodec.decode(
                        decoded
                );

        TcpOptionSet options =
                TcpLivePacketCodec.decodeOptions(
                        decoded
                );

        return result(
                "wifi-w196-syn-roundtrip",
                TcpRawLiveCarrierCodec.isRawCarrier(
                        carrier
                )
                        && ExecutionMode.CONFORMANCE.name()
                        .equals(
                        carrier.getString(
                                "execution_mode"
                        )
                )
                        && segment.flags().syn()
                        && segment.sequenceNumber()
                        == 0x12345678L
                        && options.mss() == 1200
                        && options.windowScale() == 2
                        && options.sackPermitted()
                        && decoded.sessionId.equals(
                        logical.sessionId
                ),
                "A logical live SYN must become literal raw IPv4/TCP bytes and reconstruct the TCP/session state"
        );
    }

    private static TcpRawLiveCarrierTestResult dataCarrierRoundTrip() {
        byte[] chunk =
                new byte[] {
                        10,
                        20,
                        30,
                        40,
                        50,
                        60
                };

        OSINetworkPacket logical =
                logicalTcp(
                        new TcpSegment(
                                50000,
                                80,
                                2001L,
                                7001L,
                                TcpFlags.data(),
                                32768,
                                chunk.length,
                                2_000_000L,
                                true
                        ),
                        new TcpOptionSet(
                                TcpOptionSet.ABSENT,
                                TcpOptionSet.ABSENT,
                                false,
                                List.of(),
                                2000L,
                                1900L
                        ),
                        chunk,
                        1200,
                        5000
                );

        CompoundTag carrier =
                TcpRawLiveCarrierCodec.encode(
                        logical
                );

        OSINetworkPacket decoded =
                TcpRawLiveCarrierCodec.decode(
                        carrier
                );

        return result(
                "wifi-w196-data-roundtrip",
                java.util.Arrays.equals(
                        chunk,
                        decoded.payload.getByteArray(
                                "tcp_app_chunk"
                        )
                )
                        && decoded.payload.getInt(
                        "tcp_app_offset"
                ) == 1200
                        && decoded.payload.getInt(
                        "tcp_app_total"
                ) == 5000
                        && decoded.payload.getBoolean(
                        "tcp_retransmission"
                ),
                "Raw live DATA must preserve literal TCP payload plus the minimal stream-reassembly metadata"
        );
    }

    private static TcpRawLiveCarrierTestResult rawChecksumsOnCarrier() {
        OSINetworkPacket logical =
                logicalTcp(
                        new TcpSegment(
                                40000,
                                443,
                                9000L,
                                5000L,
                                TcpFlags.ackOnly(),
                                16384,
                                0,
                                3_000_000L,
                                false
                        ),
                        TcpOptionSet.none(),
                        new byte[0],
                        0,
                        0
                );

        CompoundTag carrier =
                TcpRawLiveCarrierCodec.encode(
                        logical
                );

        RawIpv4TcpPacket raw =
                RawIpv4TcpCodec.decode(
                        carrier.getByteArray(
                                TcpRawLiveCarrierCodec.RAW_PACKET_KEY
                        )
                );

        return result(
                "wifi-w196-raw-checksums",
                raw.valid()
                        && raw.ipv4().protocol() == 6
                        && raw.ipv4().totalLength()
                        == carrier.getByteArray(
                        TcpRawLiveCarrierCodec.RAW_PACKET_KEY
                ).length
                        && raw.tcp().sourcePort() == 40000
                        && raw.tcp().destinationPort() == 443,
                "The bytes that cross the conformance carrier must independently pass raw IPv4 and TCP checksum decoding"
        );
    }

    private static TcpRawLiveCarrierTestResult corruptedRawRejected() {
        OSINetworkPacket logical =
                logicalTcp(
                        new TcpSegment(
                                50000,
                                80,
                                100L,
                                0L,
                                TcpFlags.synOnly(),
                                65535,
                                0,
                                4_000_000L,
                                false
                        ),
                        TcpOptionSet.none(),
                        new byte[0],
                        0,
                        0
                );

        CompoundTag carrier =
                TcpRawLiveCarrierCodec.encode(
                        logical
                );

        byte[] raw =
                carrier.getByteArray(
                        TcpRawLiveCarrierCodec.RAW_PACKET_KEY
                );

        raw[raw.length - 1] ^=
                0x01;

        carrier.putByteArray(
                TcpRawLiveCarrierCodec.RAW_PACKET_KEY,
                raw
        );

        boolean rejected =
                false;

        try {
            TcpRawLiveCarrierCodec.decode(
                    carrier
            );
        } catch (IllegalArgumentException expected) {
            rejected =
                    true;
        }

        return result(
                "wifi-w196-corruption-drop",
                rejected,
                "A corrupted raw live carrier must be rejected before it reaches TcpLiveController"
        );
    }

    private static TcpRawLiveCarrierTestResult simulationPacketUnaffected() {
        CompoundTag normal =
                new CompoundTag();

        normal.put(
                "osi_packet",
                new CompoundTag()
        );

        return result(
                "wifi-w196-simulation-unaffected",
                !TcpRawLiveCarrierCodec.isRawCarrier(
                        normal
                ),
                "The existing SIMULATION/NBT osi_packet path must not be mistaken for the raw conformance carrier"
        );
    }

    private static OSINetworkPacket logicalTcp(
            TcpSegment segment,
            TcpOptionSet options,
            byte[] chunk,
            int offset,
            int total
    ) {
        return TcpLivePacketCodec.encode(
                "tcp-w196-test",
                "AA:BB:CC:00:00:01",
                "AA:BB:CC:00:00:02",
                "192.0.2.10",
                "198.51.100.20",
                segment,
                chunk,
                offset,
                total,
                options
        );
    }

    private static TcpRawLiveCarrierTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new TcpRawLiveCarrierTestResult(
                id,
                passed,
                detail
        );
    }
}
