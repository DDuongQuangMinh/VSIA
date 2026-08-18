package com.k1ngtle.vsia.signality.engineering.wifi.dhcp;

import com.k1ngtle.vsia.signality.engineering.wifi.arp.MacAddressBytes;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.Ipv4Address;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DhcpCodec {
    public static final int BOOTP_FIXED_BYTES = 236;
    public static final int MAGIC_COOKIE_BYTES = 4;
    public static final int MIN_DHCP_BYTES = 300;

    private static final byte[] MAGIC_COOKIE =
            new byte[] {
                    0x63,
                    (byte) 0x82,
                    0x53,
                    0x63
            };

    private DhcpCodec() {
    }

    public static byte[] encode(DhcpPacket packet) {
        if (packet == null) {
            throw new IllegalArgumentException("packet");
        }

        if (packet.hardwareType() != DhcpPacket.HTYPE_ETHERNET
                || packet.hardwareLength() != DhcpPacket.HLEN_ETHERNET) {
            throw new IllegalArgumentException(
                    "Only Ethernet DHCP is supported"
            );
        }

        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        out.write(packet.op());
        out.write(packet.hardwareType());
        out.write(packet.hardwareLength());
        out.write(packet.hops());

        put32(out, packet.transactionId());
        put16(out, packet.seconds());
        put16(out, packet.flags());

        out.writeBytes(Ipv4Address.parse(packet.clientIp()));
        out.writeBytes(Ipv4Address.parse(packet.yourIp()));
        out.writeBytes(Ipv4Address.parse(packet.serverIp()));
        out.writeBytes(Ipv4Address.parse(packet.relayIp()));

        byte[] chaddr = new byte[16];
        byte[] mac = MacAddressBytes.parse(packet.clientMac());
        System.arraycopy(mac, 0, chaddr, 0, 6);
        out.writeBytes(chaddr);

        out.writeBytes(new byte[64]);
        out.writeBytes(new byte[128]);

        out.writeBytes(MAGIC_COOKIE);

        for (Map.Entry<Integer, byte[]> entry
                : packet.options().entrySet()) {
            int code = entry.getKey();

            if (code == DhcpOption.PAD
                    || code == DhcpOption.END) {
                continue;
            }

            byte[] value = entry.getValue();

            if (value == null
                    || value.length > 255) {
                throw new IllegalArgumentException(
                        "Invalid DHCP option length"
                );
            }

            out.write(code);
            out.write(value.length);
            out.writeBytes(value);
        }

        out.write(DhcpOption.END);

        while (out.size() < MIN_DHCP_BYTES) {
            out.write(DhcpOption.PAD);
        }

        return out.toByteArray();
    }

    public static DhcpPacket decode(byte[] bytes) {
        if (bytes == null
                || bytes.length < BOOTP_FIXED_BYTES + MAGIC_COOKIE_BYTES) {
            throw new IllegalArgumentException(
                    "DHCP packet too short"
            );
        }

        if (bytes[236] != MAGIC_COOKIE[0]
                || bytes[237] != MAGIC_COOKIE[1]
                || bytes[238] != MAGIC_COOKIE[2]
                || bytes[239] != MAGIC_COOKIE[3]) {
            throw new IllegalArgumentException(
                    "DHCP magic cookie missing"
            );
        }

        int hlen = bytes[2] & 0xFF;

        if (hlen != 6) {
            throw new IllegalArgumentException(
                    "Only Ethernet DHCP hlen=6 is supported"
            );
        }

        Map<Integer, byte[]> options =
                new LinkedHashMap<>();

        int index = 240;

        while (index < bytes.length) {
            int code = bytes[index] & 0xFF;
            index++;

            if (code == DhcpOption.PAD) {
                continue;
            }

            if (code == DhcpOption.END) {
                break;
            }

            if (index >= bytes.length) {
                throw new IllegalArgumentException(
                        "Truncated DHCP option"
                );
            }

            int length = bytes[index] & 0xFF;
            index++;

            if (index + length > bytes.length) {
                throw new IllegalArgumentException(
                        "Truncated DHCP option value"
                );
            }

            byte[] value = new byte[length];
            System.arraycopy(bytes, index, value, 0, length);
            index += length;

            options.put(code, value);
        }

        return new DhcpPacket(
                bytes[0] & 0xFF,
                bytes[1] & 0xFF,
                hlen,
                bytes[3] & 0xFF,
                read32(bytes, 4),
                read16(bytes, 8),
                read16(bytes, 10),
                formatIpv4(bytes, 12),
                formatIpv4(bytes, 16),
                formatIpv4(bytes, 20),
                formatIpv4(bytes, 24),
                MacAddressBytes.format(bytes, 28),
                options
        );
    }

    public static Map<Integer, byte[]> discoverOptions(
            String clientMac
    ) {
        Map<Integer, byte[]> options =
                new LinkedHashMap<>();

        options.put(
                DhcpOption.MESSAGE_TYPE,
                new byte[] {
                        (byte) DhcpMessageType.DISCOVER.code()
                }
        );

        options.put(
                DhcpOption.PARAMETER_REQUEST_LIST,
                new byte[] {
                        1,
                        3,
                        6,
                        51,
                        54
                }
        );

        byte[] clientId = new byte[7];
        clientId[0] = 1;
        byte[] mac = MacAddressBytes.parse(clientMac);
        System.arraycopy(mac, 0, clientId, 1, 6);

        options.put(
                DhcpOption.CLIENT_IDENTIFIER,
                clientId
        );

        return options;
    }

    public static Map<Integer, byte[]> requestOptions(
            String clientMac,
            String requestedIp,
            String serverIdentifier
    ) {
        Map<Integer, byte[]> options =
                discoverOptions(clientMac);

        options.put(
                DhcpOption.MESSAGE_TYPE,
                new byte[] {
                        (byte) DhcpMessageType.REQUEST.code()
                }
        );

        options.put(
                DhcpOption.REQUESTED_IP,
                Ipv4Address.parse(requestedIp)
        );

        options.put(
                DhcpOption.SERVER_IDENTIFIER,
                Ipv4Address.parse(serverIdentifier)
        );

        return options;
    }

    public static Map<Integer, byte[]> replyOptions(
            DhcpMessageType type,
            String subnetMask,
            String router,
            String dns,
            int leaseSeconds,
            String serverIdentifier
    ) {
        Map<Integer, byte[]> options =
                new LinkedHashMap<>();

        options.put(
                DhcpOption.MESSAGE_TYPE,
                new byte[] {
                        (byte) type.code()
                }
        );

        if (serverIdentifier != null
                && !serverIdentifier.isBlank()) {
            options.put(
                    DhcpOption.SERVER_IDENTIFIER,
                    Ipv4Address.parse(serverIdentifier)
            );
        }

        if (subnetMask != null
                && !subnetMask.isBlank()) {
            options.put(
                    DhcpOption.SUBNET_MASK,
                    Ipv4Address.parse(subnetMask)
            );
        }

        if (router != null
                && !router.isBlank()) {
            options.put(
                    DhcpOption.ROUTER,
                    Ipv4Address.parse(router)
            );
        }

        if (dns != null
                && !dns.isBlank()) {
            options.put(
                    DhcpOption.DNS,
                    Ipv4Address.parse(dns)
            );
        }

        if (leaseSeconds > 0) {
            options.put(
                    DhcpOption.LEASE_TIME,
                    unsigned32Bytes(leaseSeconds)
            );
        }

        return options;
    }

    public static Map<Integer, byte[]> copyOptions(
            Map<Integer, byte[]> source
    ) {
        Map<Integer, byte[]> copy =
                new LinkedHashMap<>();

        if (source != null) {
            source.forEach(
                    (key, value) ->
                            copy.put(
                                    key,
                                    value == null
                                            ? new byte[0]
                                            : value.clone()
                            )
            );
        }

        return copy;
    }

    public static String formatIpv4(
            byte[] bytes,
            int offset
    ) {
        return (bytes[offset] & 0xFF)
                + "."
                + (bytes[offset + 1] & 0xFF)
                + "."
                + (bytes[offset + 2] & 0xFF)
                + "."
                + (bytes[offset + 3] & 0xFF);
    }

    public static long read32(
            byte[] bytes,
            int offset
    ) {
        return ((long) (bytes[offset] & 0xFF) << 24)
                | ((long) (bytes[offset + 1] & 0xFF) << 16)
                | ((long) (bytes[offset + 2] & 0xFF) << 8)
                | (long) (bytes[offset + 3] & 0xFF);
    }

    public static byte[] unsigned32Bytes(
            long value
    ) {
        byte[] bytes = new byte[4];

        bytes[0] = (byte) (value >>> 24);
        bytes[1] = (byte) (value >>> 16);
        bytes[2] = (byte) (value >>> 8);
        bytes[3] = (byte) value;

        return bytes;
    }

    private static int read16(
            byte[] bytes,
            int offset
    ) {
        return ((bytes[offset] & 0xFF) << 8)
                | (bytes[offset + 1] & 0xFF);
    }

    private static void put16(
            ByteArrayOutputStream out,
            int value
    ) {
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    private static void put32(
            ByteArrayOutputStream out,
            long value
    ) {
        out.write((int) (value >>> 24) & 0xFF);
        out.write((int) (value >>> 16) & 0xFF);
        out.write((int) (value >>> 8) & 0xFF);
        out.write((int) value & 0xFF);
    }
}
