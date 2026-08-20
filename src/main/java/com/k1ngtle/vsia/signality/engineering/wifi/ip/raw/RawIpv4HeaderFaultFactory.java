package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.InternetChecksum;

import java.util.Arrays;
import java.util.Locale;

public final class RawIpv4HeaderFaultFactory {
    private RawIpv4HeaderFaultFactory() {
    }

    public static byte[] apply(
            byte[] validRawIpv4,
            String faultName
    ) {
        if (validRawIpv4 == null
                || validRawIpv4.length < 20) {
            throw new IllegalArgumentException(
                    "validRawIpv4"
            );
        }

        String fault =
                faultName == null
                        ? ""
                        : faultName.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        byte[] out =
                validRawIpv4.clone();

        switch (fault) {
            case "reserved_flag" -> {
                int flags =
                        read16(
                                out,
                                6
                        );

                flags |=
                        0x8000;

                put16(
                        out,
                        6,
                        flags
                );

                recomputeHeaderChecksum(
                        out
                );
            }

            case "df_mf_conflict" -> {
                int flags =
                        read16(
                                out,
                                6
                        );

                flags |=
                        0x6000;

                put16(
                        out,
                        6,
                        flags
                );

                recomputeHeaderChecksum(
                        out
                );
            }

            case "bad_total_length" -> {
                put16(
                        out,
                        2,
                        10
                );

                recomputeHeaderChecksum(
                        out
                );
            }

            case "bad_checksum" -> {
                out[8] =
                        (byte) (
                                (out[8] & 0xFF)
                                        - 1
                        );
            }

            case "bad_ihl" -> {
                out[0] =
                        (byte) (
                                (out[0] & 0xF0)
                                        | 0x04
                        );
            }

            case "malformed_option" -> {
                byte[] expanded =
                        insertFourOptionBytes(
                                out
                        );

                expanded[20] =
                        0x07;

                expanded[21] =
                        0x01;

                expanded[22] =
                        0x00;

                expanded[23] =
                        0x00;

                recomputeHeaderChecksum(
                        expanded
                );

                return expanded;
            }

            default ->
                    throw new IllegalArgumentException(
                            "Unknown W1.14 fault: "
                                    + faultName
                    );
        }

        return out;
    }

    private static byte[] insertFourOptionBytes(
            byte[] raw
    ) {
        int oldIhl =
                raw[0]
                        & 0x0F;

        int oldHeaderBytes =
                oldIhl * 4;

        if (oldHeaderBytes + 4 > 60) {
            throw new IllegalArgumentException(
                    "Cannot expand IPv4 header beyond IHL=15"
            );
        }

        int totalLength =
                read16(
                        raw,
                        2
                );

        byte[] out =
                new byte[
                        totalLength + 4
                ];

        System.arraycopy(
                raw,
                0,
                out,
                0,
                oldHeaderBytes
        );

        System.arraycopy(
                raw,
                oldHeaderBytes,
                out,
                oldHeaderBytes + 4,
                totalLength
                        - oldHeaderBytes
        );

        out[0] =
                (byte) (
                        (out[0] & 0xF0)
                                | (oldIhl + 1)
                );

        put16(
                out,
                2,
                totalLength + 4
        );

        return out;
    }

    private static void recomputeHeaderChecksum(
            byte[] raw
    ) {
        int ihlWords =
                raw[0]
                        & 0x0F;

        int headerBytes =
                ihlWords >= 5
                        ? ihlWords * 4
                        : Math.min(
                        20,
                        raw.length
                );

        if (headerBytes > raw.length) {
            headerBytes =
                    raw.length;
        }

        raw[10] =
                0;

        raw[11] =
                0;

        int checksum =
                InternetChecksum.compute(
                        Arrays.copyOfRange(
                                raw,
                                0,
                                headerBytes
                        )
                );

        put16(
                raw,
                10,
                checksum
        );
    }

    private static int read16(
            byte[] data,
            int offset
    ) {
        return ((data[offset] & 0xFF) << 8)
                | (data[offset + 1] & 0xFF);
    }

    private static void put16(
            byte[] data,
            int offset,
            int value
    ) {
        data[offset] =
                (byte) (
                        value >>> 8
                );

        data[offset + 1] =
                (byte) value;
    }
}
