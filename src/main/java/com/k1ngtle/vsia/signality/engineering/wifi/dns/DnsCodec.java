package com.k1ngtle.vsia.signality.engineering.wifi.dns;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.Ipv4Address;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class DnsCodec {
    public static final int HEADER_BYTES = 12;
    public static final int RCODE_NOERROR = 0;
    public static final int RCODE_NXDOMAIN = 3;

    private DnsCodec() {
    }

    public static byte[] encodeQuery(
            int id,
            String name,
            DnsType type
    ) {
        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        put16(out, id);
        put16(out, 0x0100);
        put16(out, 1);
        put16(out, 0);
        put16(out, 0);
        put16(out, 0);

        writeName(out, name);
        put16(out, type.code());
        put16(out, DnsQuestion.CLASS_IN);

        return out.toByteArray();
    }

    public static byte[] encodeResponse(
            int id,
            String name,
            DnsType type,
            String answer,
            int ttl,
            boolean authoritative
    ) {
        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        boolean found =
                answer != null
                        && !answer.isBlank()
                        && !"0.0.0.0".equals(answer);

        int flags =
                0x8000
                        | 0x0100
                        | 0x0080;

        if (authoritative) {
            flags |= 0x0400;
        }

        if (!found) {
            flags |= RCODE_NXDOMAIN;
        }

        put16(out, id);
        put16(out, flags);
        put16(out, 1);
        put16(out, found ? 1 : 0);
        put16(out, 0);
        put16(out, 0);

        writeName(out, name);
        put16(out, type.code());
        put16(out, DnsQuestion.CLASS_IN);

        if (found) {
            out.write(0xC0);
            out.write(0x0C);
            put16(out, type.code());
            put16(out, DnsQuestion.CLASS_IN);
            put32(out, Math.max(0, ttl));

            byte[] rdata =
                    encodeRdata(type, answer);

            put16(out, rdata.length);
            out.writeBytes(rdata);
        }

        return out.toByteArray();
    }

    public static DnsMessage decode(
            byte[] bytes
    ) {
        if (bytes == null
                || bytes.length < HEADER_BYTES) {
            throw new IllegalArgumentException(
                    "DNS message must contain a 12-byte header"
            );
        }

        int id = read16(bytes, 0);
        int flags = read16(bytes, 2);
        int qd = read16(bytes, 4);
        int an = read16(bytes, 6);
        int ns = read16(bytes, 8);
        int ar = read16(bytes, 10);

        if (qd > 16
                || an > 64
                || ns > 64
                || ar > 64) {
            throw new IllegalArgumentException(
                    "DNS section count exceeds lab safety limit"
            );
        }

        int[] cursor =
                new int[] {
                        HEADER_BYTES
                };

        List<DnsQuestion> questions =
                new ArrayList<>();

        for (int i = 0; i < qd; i++) {
            String name =
                    readName(
                            bytes,
                            cursor
                    );

            ensure(
                    bytes,
                    cursor[0],
                    4
            );

            DnsType type =
                    DnsType.fromCode(
                            read16(
                                    bytes,
                                    cursor[0]
                            )
                    );

            int dnsClass =
                    read16(
                            bytes,
                            cursor[0] + 2
                    );

            cursor[0] += 4;

            questions.add(
                    new DnsQuestion(
                            name,
                            type,
                            dnsClass
                    )
            );
        }

        List<DnsResourceRecord> answers =
                new ArrayList<>();

        for (int i = 0; i < an; i++) {
            String name =
                    readName(
                            bytes,
                            cursor
                    );

            ensure(
                    bytes,
                    cursor[0],
                    10
            );

            DnsType type =
                    DnsType.fromCode(
                            read16(
                                    bytes,
                                    cursor[0]
                            )
                    );

            int dnsClass =
                    read16(
                            bytes,
                            cursor[0] + 2
                    );

            long ttl =
                    read32(
                            bytes,
                            cursor[0] + 4
                    );

            int rdLength =
                    read16(
                            bytes,
                            cursor[0] + 8
                    );

            cursor[0] += 10;

            ensure(
                    bytes,
                    cursor[0],
                    rdLength
            );

            byte[] rdata =
                    java.util.Arrays.copyOfRange(
                            bytes,
                            cursor[0],
                            cursor[0] + rdLength
                    );

            String text =
                    decodeRdata(
                            bytes,
                            cursor[0],
                            rdLength,
                            type
                    );

            cursor[0] += rdLength;

            answers.add(
                    new DnsResourceRecord(
                            name,
                            type,
                            dnsClass,
                            ttl,
                            rdata,
                            text
                    )
            );
        }

        return new DnsMessage(
                id,
                (flags & 0x8000) != 0,
                (flags & 0x0400) != 0,
                (flags & 0x0200) != 0,
                (flags & 0x0100) != 0,
                (flags & 0x0080) != 0,
                flags & 0x000F,
                questions,
                answers
        );
    }

    public static void writeName(
            ByteArrayOutputStream out,
            String name
    ) {
        String normalized =
                normalizeName(name);

        if (normalized.isEmpty()) {
            out.write(0);
            return;
        }

        for (String label
                : normalized.split("\\.")) {
            byte[] bytes =
                    label.getBytes(
                            StandardCharsets.US_ASCII
                    );

            if (bytes.length == 0
                    || bytes.length > 63) {
                throw new IllegalArgumentException(
                        "DNS label must contain 1..63 octets"
                );
            }

            out.write(bytes.length);
            out.writeBytes(bytes);
        }

        out.write(0);
    }

    public static String normalizeName(
            String name
    ) {
        if (name == null) {
            return "";
        }

        String value =
                name.trim()
                        .toLowerCase(
                                java.util.Locale.ROOT
                        );

        while (value.endsWith(".")) {
            value =
                    value.substring(
                            0,
                            value.length() - 1
                    );
        }

        if (value.length() > 253) {
            throw new IllegalArgumentException(
                    "DNS name exceeds 253 characters"
            );
        }

        return value;
    }

    private static byte[] encodeRdata(
            DnsType type,
            String answer
    ) {
        if (type == DnsType.A) {
            return Ipv4Address.parse(
                    answer
            );
        }

        if (type == DnsType.AAAA) {
            try {
                return java.net.InetAddress
                        .getByName(answer)
                        .getAddress();
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Invalid AAAA address",
                        e
                );
            }
        }

        if (type == DnsType.CNAME
                || type == DnsType.PTR) {
            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            writeName(
                    out,
                    answer
            );

            return out.toByteArray();
        }

        if (type == DnsType.MX) {
            String[] parts =
                    answer.trim()
                            .split(
                                    "\\s+",
                                    2
                            );

            int preference =
                    parts.length == 2
                            ? Integer.parseInt(
                            parts[0]
                    )
                            : 10;

            String exchange =
                    parts.length == 2
                            ? parts[1]
                            : parts[0];

            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            put16(
                    out,
                    preference
            );

            writeName(
                    out,
                    exchange
            );

            return out.toByteArray();
        }

        throw new IllegalArgumentException(
                "Unsupported DNS RDATA type"
        );
    }

    private static String decodeRdata(
            byte[] message,
            int offset,
            int length,
            DnsType type
    ) {
        if (type == DnsType.A) {
            if (length != 4) {
                throw new IllegalArgumentException(
                        "A RDATA must contain 4 bytes"
                );
            }

            return (
                    message[offset]
                            & 0xFF
            )
                    + "."
                    + (
                    message[offset + 1]
                            & 0xFF
            )
                    + "."
                    + (
                    message[offset + 2]
                            & 0xFF
            )
                    + "."
                    + (
                    message[offset + 3]
                            & 0xFF
            );
        }

        if (type == DnsType.AAAA) {
            if (length != 16) {
                throw new IllegalArgumentException(
                        "AAAA RDATA must contain 16 bytes"
                );
            }

            try {
                return java.net.InetAddress
                        .getByAddress(
                                java.util.Arrays.copyOfRange(
                                        message,
                                        offset,
                                        offset + length
                                )
                        )
                        .getHostAddress();
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Invalid AAAA RDATA",
                        e
                );
            }
        }

        if (type == DnsType.CNAME
                || type == DnsType.PTR) {
            int[] cursor =
                    new int[] {
                            offset
                    };

            return readName(
                    message,
                    cursor
            );
        }

        if (type == DnsType.MX) {
            if (length < 3) {
                throw new IllegalArgumentException(
                        "MX RDATA is too short"
                );
            }

            int preference =
                    read16(
                            message,
                            offset
                    );

            int[] cursor =
                    new int[] {
                            offset + 2
                    };

            return preference
                    + " "
                    + readName(
                    message,
                    cursor
            );
        }

        return "";
    }

    private static String readName(
            byte[] bytes,
            int[] cursor
    ) {
        StringBuilder out =
                new StringBuilder();

        int position =
                cursor[0];

        int nextPosition =
                -1;

        int jumps =
                0;

        while (true) {
            ensure(
                    bytes,
                    position,
                    1
            );

            int length =
                    bytes[position]
                            & 0xFF;

            if (length == 0) {
                position++;

                if (nextPosition < 0) {
                    cursor[0] =
                            position;
                } else {
                    cursor[0] =
                            nextPosition;
                }

                break;
            }

            if (
                    (
                            length
                                    & 0xC0
                    )
                            == 0xC0
            ) {
                ensure(
                        bytes,
                        position,
                        2
                );

                int pointer =
                        (
                                (
                                        length
                                                & 0x3F
                                )
                                        << 8
                        )
                                | (
                                bytes[position + 1]
                                        & 0xFF
                        );

                if (pointer >= bytes.length
                        || ++jumps > 16) {
                    throw new IllegalArgumentException(
                            "Invalid DNS compression pointer"
                    );
                }

                if (nextPosition < 0) {
                    nextPosition =
                            position + 2;
                }

                position =
                        pointer;

                continue;
            }

            if (length > 63) {
                throw new IllegalArgumentException(
                        "Invalid DNS label length"
                );
            }

            ensure(
                    bytes,
                    position + 1,
                    length
            );

            if (!out.isEmpty()) {
                out.append('.');
            }

            out.append(
                    new String(
                            bytes,
                            position + 1,
                            length,
                            StandardCharsets.US_ASCII
                    )
            );

            position +=
                    1 + length;
        }

        return out.toString()
                .toLowerCase(
                        java.util.Locale.ROOT
                );
    }

    private static void ensure(
            byte[] bytes,
            int offset,
            int length
    ) {
        if (offset < 0
                || length < 0
                || offset + length
                > bytes.length) {
            throw new IllegalArgumentException(
                    "Truncated DNS message"
            );
        }
    }

    private static int read16(
            byte[] bytes,
            int offset
    ) {
        return (
                (
                        bytes[offset]
                                & 0xFF
                )
                        << 8
        )
                | (
                bytes[offset + 1]
                        & 0xFF
        );
    }

    private static long read32(
            byte[] bytes,
            int offset
    ) {
        return (
                (
                        long
                ) (
                        bytes[offset]
                                & 0xFF
                )
                        << 24
        )
                | (
                (
                        long
                ) (
                        bytes[offset + 1]
                                & 0xFF
                )
                        << 16
        )
                | (
                (
                        long
                ) (
                        bytes[offset + 2]
                                & 0xFF
                )
                        << 8
        )
                | (
                long
        ) (
                bytes[offset + 3]
                        & 0xFF
        );
    }

    private static void put16(
            ByteArrayOutputStream out,
            int value
    ) {
        out.write(
                (
                        value
                                >>> 8
                )
                        & 0xFF
        );

        out.write(
                value
                        & 0xFF
        );
    }

    private static void put32(
            ByteArrayOutputStream out,
            long value
    ) {
        out.write(
                (
                        int
                ) (
                        value
                                >>> 24
                )
                        & 0xFF
        );

        out.write(
                (
                        int
                ) (
                        value
                                >>> 16
                )
                        & 0xFF
        );

        out.write(
                (
                        int
                ) (
                        value
                                >>> 8
                )
                        & 0xFF
        );

        out.write(
                (
                        int
                ) value
                        & 0xFF
        );
    }
}
