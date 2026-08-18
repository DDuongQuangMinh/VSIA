package com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.Ipv4Header;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpFlags;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpWireHeader;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.options.TcpOptionCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.options.TcpOptionSet;

public final class RawIpv4TcpCodec {
    private RawIpv4TcpCodec() {
    }

    public static byte[] encode(
            String sourceIp,
            String destinationIp,
            int sourcePort,
            int destinationPort,
            long sequenceNumber,
            long acknowledgementNumber,
            TcpFlags flags,
            int window,
            int ttl,
            int identification,
            boolean dontFragment,
            TcpOptionSet options,
            byte[] payload
    ) {
        byte[] body =
                payload == null
                        ? new byte[0]
                        : payload.clone();

        byte[] optionBytes =
                TcpOptionCodec.encode(
                        options
                );

        TcpWireHeader tcp =
                new TcpWireHeader(
                        sourcePort,
                        destinationPort,
                        sequenceNumber,
                        acknowledgementNumber,
                        flags,
                        window,
                        0
                );

        byte[] tcpHeader =
                tcp.encode(
                        sourceIp,
                        destinationIp,
                        body,
                        optionBytes
                );

        int tcpLength =
                tcpHeader.length
                        + body.length;

        Ipv4Header ipv4 =
                new Ipv4Header(
                        sourceIp,
                        destinationIp,
                        6,
                        ttl,
                        identification,
                        tcpLength,
                        dontFragment
                );

        byte[] ipHeader =
                ipv4.encode();

        byte[] raw =
                new byte[
                        ipHeader.length
                                + tcpLength
                ];

        System.arraycopy(
                ipHeader,
                0,
                raw,
                0,
                ipHeader.length
        );

        System.arraycopy(
                tcpHeader,
                0,
                raw,
                ipHeader.length,
                tcpHeader.length
        );

        System.arraycopy(
                body,
                0,
                raw,
                ipHeader.length
                        + tcpHeader.length,
                body.length
        );

        return raw;
    }

    public static RawIpv4TcpPacket decode(
            byte[] raw
    ) {
        RawIpv4Packet ipv4 =
                RawIpv4Decoder.decode(
                        raw
                );

        if (ipv4.protocol() != 6) {
            throw new IllegalArgumentException(
                    "IPv4 protocol is not TCP"
            );
        }

        if (ipv4.fragmentOffset() != 0
                || ipv4.moreFragments()) {
            throw new IllegalArgumentException(
                    "Fragmented IPv4 packets are not accepted by this TCP lab decoder"
            );
        }

        RawTcpPacket tcp =
                RawTcpDecoder.decode(
                        ipv4.sourceAddress(),
                        ipv4.destinationAddress(),
                        ipv4.payload()
                );

        return new RawIpv4TcpPacket(
                ipv4,
                tcp,
                raw
        );
    }
}
