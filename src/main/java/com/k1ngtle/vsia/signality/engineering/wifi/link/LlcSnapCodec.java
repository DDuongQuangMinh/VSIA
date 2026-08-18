package com.k1ngtle.vsia.signality.engineering.wifi.link;

import java.util.Arrays;

public final class LlcSnapCodec {
    public static final int HEADER_BYTES =
            8;

    public static final int DSAP_SNAP =
            0xAA;

    public static final int SSAP_SNAP =
            0xAA;

    public static final int CONTROL_UI =
            0x03;

    public static final int RFC1042_OUI =
            0x000000;

    private LlcSnapCodec() {
    }

    public static byte[] encodeRfc1042(
            EtherType etherType,
            byte[] payload
    ) {
        if (etherType == null
                || etherType == EtherType.UNKNOWN
                || etherType.value() < 0) {
            throw new IllegalArgumentException(
                    "A concrete EtherType is required"
            );
        }

        byte[] body =
                payload == null
                        ? new byte[0]
                        : payload.clone();

        byte[] frame =
                new byte[
                        HEADER_BYTES
                                + body.length
                ];

        frame[0] =
                (
                        byte
                ) DSAP_SNAP;

        frame[1] =
                (
                        byte
                ) SSAP_SNAP;

        frame[2] =
                (
                        byte
                ) CONTROL_UI;

        frame[3] =
                0x00;

        frame[4] =
                0x00;

        frame[5] =
                0x00;

        frame[6] =
                (
                        byte
                ) (
                etherType.value()
                        >>> 8
        );

        frame[7] =
                (
                        byte
                ) etherType.value();

        System.arraycopy(
                body,
                0,
                frame,
                HEADER_BYTES,
                body.length
        );

        return frame;
    }

    public static LlcSnapFrame decode(
            byte[] msdu
    ) {
        if (msdu == null
                || msdu.length < HEADER_BYTES) {
            throw new IllegalArgumentException(
                    "LLC/SNAP MSDU must contain at least 8 bytes"
            );
        }

        int oui =
                (
                        (
                                msdu[3]
                                        & 0xFF
                        )
                                << 16
                )
                        | (
                        (
                                msdu[4]
                                        & 0xFF
                        )
                                << 8
                )
                        | (
                        msdu[5]
                                & 0xFF
                );

        int etherType =
                (
                        (
                                msdu[6]
                                        & 0xFF
                        )
                                << 8
                )
                        | (
                        msdu[7]
                                & 0xFF
                );

        return new LlcSnapFrame(
                msdu[0]
                        & 0xFF,
                msdu[1]
                        & 0xFF,
                msdu[2]
                        & 0xFF,
                oui,
                etherType,
                Arrays.copyOfRange(
                        msdu,
                        HEADER_BYTES,
                        msdu.length
                )
        );
    }

    public static LlcSnapFrame decodeRfc1042(
            byte[] msdu
    ) {
        LlcSnapFrame frame =
                decode(
                        msdu
                );

        if (!frame.isRfc1042Snap()) {
            throw new IllegalArgumentException(
                    "Unsupported LLC/SNAP header"
            );
        }

        return frame;
    }
}
