package com.k1ngtle.vsia.signality.engineering.wifi.ip.router;

public final class IcmpErrorModel {
    public static final int DESTINATION_UNREACHABLE =
            3;

    public static final int TIME_EXCEEDED =
            11;

    public static final int PARAMETER_PROBLEM =
            12;

    public static final int FRAGMENTATION_NEEDED_CODE =
            4;

    public static final int PARAMETER_PROBLEM_POINTER_CODE =
            0;

    public static final int PARAMETER_PROBLEM_MISSING_OPTION_CODE =
            1;

    public static final int PARAMETER_PROBLEM_BAD_LENGTH_CODE =
            2;

    private IcmpErrorModel() {
    }

    public static int internetChecksum(
            byte[] data
    ) {
        long sum =
                0;

        for (int i = 0;
             i < data.length;
             i += 2) {
            int word =
                    (data[i] & 0xFF)
                            << 8;

            if (i + 1 < data.length) {
                word |=
                        data[i + 1]
                                & 0xFF;
            }

            sum +=
                    word;

            while ((sum >>> 16)
                    != 0) {
                sum =
                        (sum & 0xFFFF)
                                + (sum >>> 16);
            }
        }

        return (int) (~sum)
                & 0xFFFF;
    }

    public static byte[] encode(
            int type,
            int code,
            byte[] quoted
    ) {
        return encodeWithRest(
                type,
                code,
                0,
                quoted
        );
    }

    public static byte[] encodeFragmentationNeeded(
            int nextHopMtu,
            byte[] quoted
    ) {
        if (nextHopMtu < 68
                || nextHopMtu > 65535) {
            throw new IllegalArgumentException(
                    "Invalid next-hop MTU"
            );
        }

        return encodeWithRest(
                DESTINATION_UNREACHABLE,
                FRAGMENTATION_NEEDED_CODE,
                nextHopMtu & 0xFFFF,
                quoted
        );
    }

    public static byte[] encodeParameterProblem(
            int code,
            int pointer,
            byte[] quoted
    ) {
        if (code < 0 || code > 2) {
            throw new IllegalArgumentException(
                    "Invalid Parameter Problem code"
            );
        }

        if (pointer < 0 || pointer > 255) {
            throw new IllegalArgumentException(
                    "Invalid Parameter Problem pointer"
            );
        }

        int rest =
                (pointer & 0xFF)
                        << 24;

        return encodeWithRest(
                PARAMETER_PROBLEM,
                code,
                rest,
                quoted
        );
    }

    public static byte[] encodeWithRest(
            int type,
            int code,
            int restOfHeader,
            byte[] quoted
    ) {
        byte[] quote =
                quoted == null
                        ? new byte[0]
                        : quoted;

        byte[] out =
                new byte[
                        8 + quote.length
                ];

        out[0] =
                (byte) type;

        out[1] =
                (byte) code;

        out[4] =
                (byte) (
                        restOfHeader >>> 24
                );

        out[5] =
                (byte) (
                        restOfHeader >>> 16
                );

        out[6] =
                (byte) (
                        restOfHeader >>> 8
                );

        out[7] =
                (byte) restOfHeader;

        System.arraycopy(
                quote,
                0,
                out,
                8,
                quote.length
        );

        int checksum =
                internetChecksum(
                        out
                );

        out[2] =
                (byte) (
                        checksum >>> 8
                );

        out[3] =
                (byte) checksum;

        return out;
    }
}
