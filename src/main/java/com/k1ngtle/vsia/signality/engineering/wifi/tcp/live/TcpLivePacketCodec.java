package com.k1ngtle.vsia.signality.engineering.wifi.tcp.live;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpFlags;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpSegment;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpWireHeader;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

public final class TcpLivePacketCodec {
    private TcpLivePacketCodec() {
    }

    public static OSINetworkPacket encode(
            String sessionId,
            String sourceMac,
            String targetMac,
            String sourceIp,
            String targetIp,
            TcpSegment segment,
            byte[] chunk,
            int applicationOffset,
            int applicationTotalBytes
    ) {
        byte[] payload =
                chunk == null
                        ? new byte[0]
                        : chunk.clone();

        TcpWireHeader header =
                new TcpWireHeader(
                        segment.sourcePort(),
                        segment.destinationPort(),
                        segment.sequenceNumber(),
                        segment.acknowledgementNumber(),
                        segment.flags(),
                        segment.window(),
                        0
                );

        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.sourceMac =
                sourceMac;

        packet.targetMac =
                targetMac;

        packet.sourceIp =
                sourceIp;

        packet.targetIp =
                targetIp;

        packet.sourcePort =
                segment.sourcePort();

        packet.targetPort =
                segment.destinationPort();

        packet.ipProtocol =
                6;

        packet.applicationProtocol =
                "TCP";

        packet.sessionId =
                sessionId;

        packet.transportChecksum =
                header.checksum(
                        sourceIp,
                        targetIp,
                        payload
                );

        packet.ipPacketLength =
                20
                        + TcpWireHeader.HEADER_BYTES
                        + payload.length;

        packet.dontFragment =
                true;

        CompoundTag body =
                packet.payload;

        body.putLong(
                "tcp_seq",
                segment.sequenceNumber()
        );

        body.putLong(
                "tcp_ack",
                segment.acknowledgementNumber()
        );

        body.putInt(
                "tcp_window",
                segment.window()
        );

        body.putBoolean(
                "tcp_fin",
                segment.flags().fin()
        );

        body.putBoolean(
                "tcp_syn",
                segment.flags().syn()
        );

        body.putBoolean(
                "tcp_rst",
                segment.flags().rst()
        );

        body.putBoolean(
                "tcp_psh",
                segment.flags().psh()
        );

        body.putBoolean(
                "tcp_ack_flag",
                segment.flags().ack()
        );

        body.putLong(
                "tcp_sent_us",
                segment.sentAtMicros()
        );

        body.putBoolean(
                "tcp_retransmission",
                segment.retransmission()
        );

        if (payload.length > 0) {
            body.putByteArray(
                    "tcp_app_chunk",
                    payload
            );

            body.putInt(
                    "tcp_app_offset",
                    applicationOffset
            );

            body.putInt(
                    "tcp_app_total",
                    applicationTotalBytes
            );
        }

        return packet;
    }

    public static TcpSegment decode(
            OSINetworkPacket packet
    ) {
        CompoundTag body =
                packet.payload;

        byte[] chunk =
                body.getByteArray(
                        "tcp_app_chunk"
                );

        return new TcpSegment(
                packet.sourcePort,
                packet.targetPort,
                body.getLong(
                        "tcp_seq"
                ),
                body.getLong(
                        "tcp_ack"
                ),
                new TcpFlags(
                        body.getBoolean(
                                "tcp_fin"
                        ),
                        body.getBoolean(
                                "tcp_syn"
                        ),
                        body.getBoolean(
                                "tcp_rst"
                        ),
                        body.getBoolean(
                                "tcp_psh"
                        ),
                        body.getBoolean(
                                "tcp_ack_flag"
                        )
                ),
                body.contains(
                        "tcp_window"
                )
                        ? body.getInt(
                        "tcp_window"
                )
                        : 65_535,
                chunk.length,
                body.getLong(
                        "tcp_sent_us"
                ),
                body.getBoolean(
                        "tcp_retransmission"
                )
        );
    }

    public static boolean checksumValid(
            OSINetworkPacket packet
    ) {
        TcpSegment segment =
                decode(
                        packet
                );

        byte[] chunk =
                packet.payload.getByteArray(
                        "tcp_app_chunk"
                );

        TcpWireHeader header =
                new TcpWireHeader(
                        segment.sourcePort(),
                        segment.destinationPort(),
                        segment.sequenceNumber(),
                        segment.acknowledgementNumber(),
                        segment.flags(),
                        segment.window(),
                        0
                );

        return packet.transportChecksum
                == header.checksum(
                packet.sourceIp,
                packet.targetIp,
                chunk
        );
    }
}
