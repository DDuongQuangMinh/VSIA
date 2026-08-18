package com.k1ngtle.vsia.signality.engineering.wifi.dns.live;

import com.k1ngtle.vsia.signality.engineering.ExecutionMode;
import com.k1ngtle.vsia.signality.engineering.wifi.dhcp.RawUdpCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.dhcp.RawUdpPacket;
import com.k1ngtle.vsia.signality.engineering.wifi.dns.DnsCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.dns.DnsMessage;
import com.k1ngtle.vsia.signality.engineering.wifi.dns.DnsResourceRecord;
import com.k1ngtle.vsia.signality.engineering.wifi.dns.DnsType;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.Ipv4Header;
import com.k1ngtle.vsia.signality.engineering.wifi.link.EtherType;
import com.k1ngtle.vsia.signality.engineering.wifi.link.LlcSnapCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.link.LlcSnapFrame;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Decoder;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Packet;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

public final class DnsRawLiveCarrierCodec {
    public static final String CONTROL_KEY =
            "vsia_raw_network_control";

    public static final String CONTROL_VALUE =
            "LLC_SNAP_DNS_V1";

    public static final String RAW_MSDU_KEY =
            "raw_llc_snap_msdu";

    private DnsRawLiveCarrierCodec() {
    }

    public static boolean isRawDnsCarrier(
            CompoundTag body
    ) {
        return body != null
                && CONTROL_VALUE.equals(
                body.getString(
                        CONTROL_KEY
                )
        )
                && body.contains(
                RAW_MSDU_KEY
        );
    }

    public static CompoundTag encode(
            OSINetworkPacket logical
    ) {
        if (logical == null
                || !"DNS".equalsIgnoreCase(
                logical.applicationProtocol
        )) {
            throw new IllegalArgumentException(
                    "Raw DNS carrier requires DNS"
            );
        }

        int dnsId =
                logical.payload.getInt(
                        "dns_id"
                )
                        & 0xFFFF;

        String domain =
                DnsCodec.normalizeName(
                        logical.payload.getString(
                                "domain"
                        )
                );

        DnsType type =
                DnsType.byName(
                        logical.payload.contains(
                                "query_type"
                        )
                                ? logical.payload.getString(
                                "query_type"
                        )
                                : logical.payload.getString(
                                "record_type"
                        )
                );

        byte[] dns;

        if (logical.isResponse) {
            String answer =
                    logical.payload.contains(
                            "answer"
                    )
                            ? logical.payload.getString(
                            "answer"
                    )
                            : logical.payload.getString(
                            "resolved_ip"
                    );

            int rcode =
                    logical.payload.contains(
                            "rcode"
                    )
                            ? logical.payload.getInt(
                            "rcode"
                    )
                            : (
                            answer == null
                                    || answer.isBlank()
                                    || "0.0.0.0".equals(
                                    answer
                            )
                    )
                            ? DnsCodec.RCODE_NXDOMAIN
                            : DnsCodec.RCODE_NOERROR;

            dns =
                    rcode == DnsCodec.RCODE_NXDOMAIN
                            ? DnsCodec.encodeResponse(
                            dnsId,
                            domain,
                            type,
                            null,
                            0,
                            true
                    )
                            : DnsCodec.encodeResponse(
                            dnsId,
                            domain,
                            type,
                            answer,
                            logical.payload.contains(
                                    "ttl"
                            )
                                    ? logical.payload.getInt(
                                    "ttl"
                            )
                                    : 300,
                            true
                    );
        } else {
            dns =
                    DnsCodec.encodeQuery(
                            dnsId,
                            domain,
                            type
                    );
        }

        String sourceIp =
                logical.sourceIp;

        String targetIp =
                logical.targetIp;

        int sourcePort =
                logical.sourcePort <= 0
                        ? logical.isResponse
                        ? 53
                        : 53000
                        + (
                        dnsId
                                % 1000
                )
                        : logical.sourcePort;

        int targetPort =
                logical.targetPort <= 0
                        ? logical.isResponse
                        ? 53000
                        + (
                        dnsId
                                % 1000
                )
                        : 53
                        : logical.targetPort;

        byte[] udp =
                RawUdpCodec.encode(
                        sourceIp,
                        targetIp,
                        sourcePort,
                        targetPort,
                        dns
                );

        Ipv4Header ipv4 =
                new Ipv4Header(
                        sourceIp,
                        targetIp,
                        17,
                        logical.ttl <= 0
                                ? 64
                                : logical.ttl,
                        dnsId,
                        udp.length,
                        true
                );

        byte[] ipHeader =
                ipv4.encode();

        byte[] rawIpv4 =
                new byte[
                        ipHeader.length
                                + udp.length
                ];

        System.arraycopy(
                ipHeader,
                0,
                rawIpv4,
                0,
                ipHeader.length
        );

        System.arraycopy(
                udp,
                0,
                rawIpv4,
                ipHeader.length,
                udp.length
        );

        byte[] msdu =
                LlcSnapCodec.encodeRfc1042(
                        EtherType.IPV4,
                        rawIpv4
                );

        CompoundTag body =
                new CompoundTag();

        body.putString(
                CONTROL_KEY,
                CONTROL_VALUE
        );

        body.putString(
                "execution_mode",
                ExecutionMode.CONFORMANCE.name()
        );

        body.putByteArray(
                RAW_MSDU_KEY,
                msdu
        );

        body.putString(
                "src_mac",
                logical.sourceMac
        );

        body.putString(
                "dst_mac",
                logical.targetMac
        );

        return body;
    }

    public static OSINetworkPacket decode(
            CompoundTag body
    ) {
        if (!isRawDnsCarrier(
                body
        )) {
            throw new IllegalArgumentException(
                    "Not a VSIA raw DNS carrier"
            );
        }

        LlcSnapFrame frame =
                LlcSnapCodec.decodeRfc1042(
                        body.getByteArray(
                                RAW_MSDU_KEY
                        )
                );

        if (frame.etherType()
                != EtherType.IPV4.value()) {
            throw new IllegalArgumentException(
                    "DNS carrier requires IPv4 EtherType"
            );
        }

        RawIpv4Packet ipv4 =
                RawIpv4Decoder.decode(
                        frame.payload()
                );

        if (!ipv4.checksumValid()
                || ipv4.protocol() != 17) {
            throw new IllegalArgumentException(
                    "Invalid DNS IPv4 packet"
            );
        }

        RawUdpPacket udp =
                RawUdpCodec.decode(
                        ipv4.sourceAddress(),
                        ipv4.destinationAddress(),
                        ipv4.payload()
                );

        if (!udp.checksumValid()
                || (
                udp.sourcePort() != 53
                        && udp.destinationPort() != 53
        )) {
            throw new IllegalArgumentException(
                    "Invalid DNS UDP ports/checksum"
            );
        }

        DnsMessage dns =
                DnsCodec.decode(
                        udp.payload()
                );

        if (dns.questions()
                .isEmpty()) {
            throw new IllegalArgumentException(
                    "DNS question section is empty"
            );
        }

        var question =
                dns.questions()
                        .get(0);

        OSINetworkPacket logical =
                new OSINetworkPacket();

        String carrierSource =
                body.getString(
                        "src_mac"
                );

        String carrierTarget =
                body.getString(
                        "dst_mac"
                );

        logical.sourceMac =
                carrierSource == null
                        ? ""
                        : carrierSource;

        logical.targetMac =
                carrierTarget == null
                        ? ""
                        : carrierTarget;

        logical.sourceIp =
                ipv4.sourceAddress();

        logical.targetIp =
                ipv4.destinationAddress();

        logical.sourcePort =
                udp.sourcePort();

        logical.targetPort =
                udp.destinationPort();

        logical.ipProtocol =
                17;

        logical.ipv4HeaderChecksum =
                ipv4.headerChecksum();

        logical.ipPacketLength =
                ipv4.totalLength();

        logical.transportChecksum =
                udp.checksum();

        logical.applicationProtocol =
                "DNS";

        logical.isResponse =
                dns.response();

        logical.payload.putInt(
                "dns_id",
                dns.id()
        );

        logical.payload.putString(
                "domain",
                question.name()
        );

        logical.payload.putString(
                "query_type",
                question.type()
                        .name()
        );

        logical.payload.putInt(
                "rcode",
                dns.responseCode()
        );

        if (!dns.answers()
                .isEmpty()) {
            DnsResourceRecord answer =
                    dns.answers()
                            .get(0);

            logical.payload.putString(
                    "record_type",
                    answer.type()
                            .name()
            );

            logical.payload.putString(
                    "answer",
                    answer.text()
            );

            logical.payload.putString(
                    "resolved_ip",
                    answer.type()
                            == DnsType.A
                            ? answer.text()
                            : ""
            );

            logical.payload.putInt(
                    "ttl",
                    (
                            int
                    ) Math.min(
                            Integer.MAX_VALUE,
                            answer.ttl()
                    )
            );
        } else {
            logical.payload.putString(
                    "record_type",
                    question.type()
                            .name()
            );

            logical.payload.putString(
                    "answer",
                    ""
            );

            logical.payload.putString(
                    "resolved_ip",
                    "0.0.0.0"
            );
        }

        return logical;
    }
}
