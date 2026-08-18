package com.k1ngtle.vsia.signality.engineering.wifi.dns;

import com.k1ngtle.vsia.signality.engineering.wifi.dns.live.DnsRawLiveCarrierCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.link.LlcSnapCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.link.LlcSnapFrame;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Decoder;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public final class DnsRawTestSuite {
    private DnsRawTestSuite() {
    }

    public static List<DnsRawTestResult> runAll() {
        return List.of(
                queryVector(),
                queryDecode(),
                responseCompression(),
                nxdomain(),
                liveQueryCarrier(),
                liveResponseCarrier(),
                wrongUdpPortRejected(),
                malformedPointerRejected()
        );
    }

    private static DnsRawTestResult queryVector() {
        byte[] query =
                DnsCodec.encodeQuery(
                        0x1234,
                        "www.vsia-net.com",
                        DnsType.A
                );

        String expected =
                "123401000001000000000000"
                        + "03777777"
                        + "08767369612D6E6574"
                        + "03636F6D"
                        + "00"
                        + "0001"
                        + "0001";

        return result(
                "wifi-w1103-query-kat",
                expected.equals(
                        com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawPacketHex.encode(
                                query
                        )
                ),
                "DNS A query must reproduce ID/header/QNAME/QTYPE/QCLASS bytes exactly"
        );
    }

    private static DnsRawTestResult queryDecode() {
        DnsMessage message =
                DnsCodec.decode(
                        DnsCodec.encodeQuery(
                                0xBEEF,
                                "www.vsia-net.com",
                                DnsType.A
                        )
                );

        return result(
                "wifi-w1103-query-decode",
                message.id() == 0xBEEF
                        && !message.response()
                        && message.recursionDesired()
                        && message.questions().size() == 1
                        && message.questions().get(0).name()
                        .equals("www.vsia-net.com")
                        && message.questions().get(0).type()
                        == DnsType.A
                        && message.questions().get(0).dnsClass() == 1,
                "DNS query decoder must recover header flags and QNAME/QTYPE/QCLASS"
        );
    }

    private static DnsRawTestResult responseCompression() {
        byte[] response =
                DnsCodec.encodeResponse(
                        0x1234,
                        "www.vsia-net.com",
                        DnsType.A,
                        "192.168.1.2",
                        300,
                        true
                );

        DnsMessage decoded =
                DnsCodec.decode(
                        response
                );

        boolean pointer =
                false;

        for (int i = 12; i + 1 < response.length; i++) {
            if ((response[i] & 0xFF) == 0xC0
                    && (response[i + 1] & 0xFF) == 0x0C) {
                pointer = true;
                break;
            }
        }

        return result(
                "wifi-w1103-response-compression",
                pointer
                        && decoded.response()
                        && decoded.authoritative()
                        && decoded.noError()
                        && decoded.answers().size() == 1
                        && decoded.answers().get(0).text()
                        .equals("192.168.1.2")
                        && decoded.answers().get(0).ttl() == 300,
                "DNS response must support RFC-style compressed answer NAME pointer C0 0C"
        );
    }

    private static DnsRawTestResult nxdomain() {
        DnsMessage decoded =
                DnsCodec.decode(
                        DnsCodec.encodeResponse(
                                7,
                                "missing.vsia-net.com",
                                DnsType.A,
                                null,
                                0,
                                true
                        )
                );

        return result(
                "wifi-w1103-nxdomain",
                decoded.response()
                        && decoded.nameError()
                        && decoded.answers().isEmpty(),
                "Unknown host response must carry RCODE 3 / NXDOMAIN with zero answers"
        );
    }

    private static DnsRawTestResult liveQueryCarrier() {
        OSINetworkPacket query =
                queryLogical();

        CompoundTag body =
                DnsRawLiveCarrierCodec.encode(
                        query
                );

        OSINetworkPacket decoded =
                DnsRawLiveCarrierCodec.decode(
                        body
                );

        LlcSnapFrame frame =
                LlcSnapCodec.decodeRfc1042(
                        body.getByteArray(
                                DnsRawLiveCarrierCodec.RAW_MSDU_KEY
                        )
                );

        var ipv4 =
                RawIpv4Decoder.decode(
                        frame.payload()
                );

        return result(
                "wifi-w1103-live-query",
                DnsRawLiveCarrierCodec.isRawDnsCarrier(body)
                        && frame.etherType() == 0x0800
                        && ipv4.protocol() == 17
                        && ipv4.checksumValid()
                        && decoded.targetPort == 53
                        && decoded.payload.getString("domain")
                        .equals("www.vsia-net.com")
                        && decoded.sourceMac.equals("3ade93b46caf"),
                "Live query must preserve VSIA link MAC while carrying DNS through LLC/SNAP IPv4 UDP"
        );
    }

    private static DnsRawTestResult liveResponseCarrier() {
        OSINetworkPacket response =
                new OSINetworkPacket();

        response.sourceMac =
                "c72ec34fc58c";
        response.targetMac =
                "3ade93b46caf";
        response.sourceIp =
                "192.168.1.2";
        response.targetIp =
                "192.168.1.101";
        response.sourcePort =
                53;
        response.targetPort =
                53052;
        response.applicationProtocol =
                "DNS";
        response.isResponse =
                true;

        response.payload.putInt(
                "dns_id",
                52
        );

        response.payload.putString(
                "domain",
                "www.vsia-net.com"
        );

        response.payload.putString(
                "query_type",
                "A"
        );

        response.payload.putString(
                "answer",
                "192.168.1.2"
        );

        response.payload.putString(
                "resolved_ip",
                "192.168.1.2"
        );

        response.payload.putInt(
                "ttl",
                300
        );

        response.payload.putInt(
                "rcode",
                0
        );

        OSINetworkPacket decoded =
                DnsRawLiveCarrierCodec.decode(
                        DnsRawLiveCarrierCodec.encode(
                                response
                        )
                );

        return result(
                "wifi-w1103-live-response",
                decoded.isResponse
                        && decoded.sourceMac.equals("c72ec34fc58c")
                        && decoded.targetMac.equals("3ade93b46caf")
                        && decoded.sourcePort == 53
                        && decoded.payload.getString("answer")
                        .equals("192.168.1.2")
                        && decoded.payload.getInt("ttl") == 300,
                "Live A response must preserve routing MACs, answer bytes, and TTL"
        );
    }

    private static DnsRawTestResult wrongUdpPortRejected() {
        OSINetworkPacket query =
                queryLogical();

        CompoundTag body =
                DnsRawLiveCarrierCodec.encode(
                        query
                );

        byte[] msdu =
                body.getByteArray(
                        DnsRawLiveCarrierCodec.RAW_MSDU_KEY
                );

        int udpOffset =
                LlcSnapCodec.HEADER_BYTES + 20;

        msdu[udpOffset + 2] =
                0x00;

        msdu[udpOffset + 3] =
                0x50;

        body.putByteArray(
                DnsRawLiveCarrierCodec.RAW_MSDU_KEY,
                msdu
        );

        boolean rejected =
                false;

        try {
            DnsRawLiveCarrierCodec.decode(
                    body
            );
        } catch (IllegalArgumentException expected) {
            rejected =
                    true;
        }

        return result(
                "wifi-w1103-port-reject",
                rejected,
                "DNS live decoder must reject traffic where neither UDP port is 53"
        );
    }

    private static DnsRawTestResult malformedPointerRejected() {
        byte[] response =
                DnsCodec.encodeResponse(
                        1,
                        "www.vsia-net.com",
                        DnsType.A,
                        "192.168.1.2",
                        300,
                        true
                );

        int pointer =
                -1;

        for (int i = 12; i + 1 < response.length; i++) {
            if ((response[i] & 0xFF) == 0xC0
                    && (response[i + 1] & 0xFF) == 0x0C) {
                pointer =
                        i;
                break;
            }
        }

        if (pointer < 0) {
            return result(
                    "wifi-w1103-pointer-reject",
                    false,
                    "No compression pointer found"
            );
        }

        response[pointer] =
                (byte) 0xFF;
        response[pointer + 1] =
                (byte) 0xFF;

        boolean rejected =
                false;

        try {
            DnsCodec.decode(
                    response
            );
        } catch (IllegalArgumentException expected) {
            rejected =
                    true;
        }

        return result(
                "wifi-w1103-pointer-reject",
                rejected,
                "DNS decoder must reject out-of-range compression pointers"
        );
    }

    private static OSINetworkPacket queryLogical() {
        OSINetworkPacket query =
                new OSINetworkPacket();

        query.sourceMac =
                "3ade93b46caf";

        query.targetMac =
                "c72ec34fc58c";

        query.sourceIp =
                "192.168.1.101";

        query.targetIp =
                "192.168.1.2";

        query.sourcePort =
                53052;

        query.targetPort =
                53;

        query.applicationProtocol =
                "DNS";

        query.payload.putInt(
                "dns_id",
                52
        );

        query.payload.putString(
                "domain",
                "www.vsia-net.com"
        );

        query.payload.putString(
                "query_type",
                "A"
        );

        return query;
    }

    private static DnsRawTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new DnsRawTestResult(
                id,
                passed,
                detail
        );
    }
}
