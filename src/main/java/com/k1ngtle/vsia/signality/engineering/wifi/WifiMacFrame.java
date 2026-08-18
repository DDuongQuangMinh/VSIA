package com.k1ngtle.vsia.signality.engineering.wifi;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.zip.CRC32;

public record WifiMacFrame(
        int frameControl,
        int durationId,
        byte[] address1,
        byte[] address2,
        byte[] address3,
        int sequenceControl,
        byte[] payload
) {
    public static final int TYPE_MASK =
            0x000C;

    public static final int SUBTYPE_MASK =
            0x00F0;

    public static final int TYPE_CONTROL =
            0x0004;

    public static final int SUBTYPE_RTS =
            0x00B0;

    public static final int SUBTYPE_CTS =
            0x00C0;

    public static final int SUBTYPE_ACK =
            0x00D0;

    public WifiMacFrame {
        address1 =
                normalizeMac(
                        address1
                );

        address2 =
                normalizeMac(
                        address2
                );

        address3 =
                normalizeMac(
                        address3
                );

        payload =
                payload == null
                        ? new byte[0]
                        : payload.clone();

        frameControl &=
                0xFFFF;

        durationId &=
                0xFFFF;

        sequenceControl &=
                0xFFFF;
    }

    @Override
    public byte[] address1() {
        return address1.clone();
    }

    @Override
    public byte[] address2() {
        return address2.clone();
    }

    @Override
    public byte[] address3() {
        return address3.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    public WifiFrameType type() {
        return WifiFrameType.fromFrameControl(
                frameControl
        );
    }

    public int subtype() {
        return (
                frameControl >>> 4
        )
                & 0x0F;
    }

    public boolean retry() {
        return (
                frameControl
                        & 0x0800
        )
                != 0;
    }

    public int headerLengthBytes() {
        if (isAck()
                || isCts()) {
            return 10;
        }

        if (type() == WifiFrameType.CONTROL) {
            return 16;
        }

        return 24;
    }

    public boolean isRts() {
        return isControlSubtype(
                SUBTYPE_RTS
        );
    }

    public boolean isCts() {
        return isControlSubtype(
                SUBTYPE_CTS
        );
    }

    public boolean isAck() {
        return isControlSubtype(
                SUBTYPE_ACK
        );
    }

    public byte[] encodeWithoutFcs() {
        validateSupportedHeaderShape();

        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        writeU16Le(
                out,
                frameControl
        );

        writeU16Le(
                out,
                durationId
        );

        out.writeBytes(
                address1
        );

        if (isAck()
                || isCts()) {
            return out.toByteArray();
        }

        out.writeBytes(
                address2
        );

        if (type() == WifiFrameType.CONTROL) {
            out.writeBytes(
                    payload
            );

            return out.toByteArray();
        }

        out.writeBytes(
                address3
        );

        writeU16Le(
                out,
                sequenceControl
        );

        out.writeBytes(
                payload
        );

        return out.toByteArray();
    }

    public byte[] encode() {
        byte[] body =
                encodeWithoutFcs();

        long fcs =
                calculateFcs(
                        body
                );

        ByteArrayOutputStream out =
                new ByteArrayOutputStream(
                        body.length + 4
                );

        out.writeBytes(
                body
        );

        writeU32Le(
                out,
                fcs
        );

        return out.toByteArray();
    }

    public long fcs() {
        return calculateFcs(
                encodeWithoutFcs()
        );
    }

    public static WifiMacFrame decode(
            byte[] bytes
    ) {
        if (bytes == null
                || bytes.length < 14) {
            throw new IllegalArgumentException(
                    "802.11 frame is too short"
            );
        }

        long expectedFcs =
                readU32Le(
                        bytes,
                        bytes.length - 4
                );

        byte[] protectedBytes =
                Arrays.copyOf(
                        bytes,
                        bytes.length - 4
                );

        long actualFcs =
                calculateFcs(
                        protectedBytes
                );

        if (actualFcs
                != expectedFcs) {
            throw new IllegalArgumentException(
                    String.format(
                            "802.11 FCS mismatch: expected %08X actual %08X",
                            expectedFcs,
                            actualFcs
                    )
            );
        }

        int frameControl =
                readU16Le(
                        bytes,
                        0
                );

        int duration =
                readU16Le(
                        bytes,
                        2
                );

        byte[] address1 =
                Arrays.copyOfRange(
                        bytes,
                        4,
                        10
                );

        int subtypeField =
                frameControl
                        & (
                        TYPE_MASK
                                | SUBTYPE_MASK
                );

        if (subtypeField
                == (
                TYPE_CONTROL
                        | SUBTYPE_ACK
        )
                || subtypeField
                == (
                TYPE_CONTROL
                        | SUBTYPE_CTS
        )) {
            if (bytes.length != 14) {
                throw new IllegalArgumentException(
                        "ACK/CTS frame length must be 14 bytes including FCS"
                );
            }

            return new WifiMacFrame(
                    frameControl,
                    duration,
                    address1,
                    new byte[6],
                    new byte[6],
                    0,
                    new byte[0]
            );
        }

        if ((
                frameControl
                        & TYPE_MASK
        )
                == TYPE_CONTROL) {
            if (bytes.length < 20) {
                throw new IllegalArgumentException(
                        "Control frame is too short"
                );
            }

            byte[] address2 =
                    Arrays.copyOfRange(
                            bytes,
                            10,
                            16
                    );

            byte[] payload =
                    Arrays.copyOfRange(
                            bytes,
                            16,
                            bytes.length - 4
                    );

            if (subtypeField
                    == (
                    TYPE_CONTROL
                            | SUBTYPE_RTS
            )
                    && payload.length != 0) {
                throw new IllegalArgumentException(
                        "RTS frame must not carry a MAC body"
                );
            }

            return new WifiMacFrame(
                    frameControl,
                    duration,
                    address1,
                    address2,
                    new byte[6],
                    0,
                    payload
            );
        }

        validateSupportedHeaderShape(
                frameControl
        );

        if (bytes.length < 28) {
            throw new IllegalArgumentException(
                    "Management/data frame is too short"
            );
        }

        byte[] address2 =
                Arrays.copyOfRange(
                        bytes,
                        10,
                        16
                );

        byte[] address3 =
                Arrays.copyOfRange(
                        bytes,
                        16,
                        22
                );

        int sequenceControl =
                readU16Le(
                        bytes,
                        22
                );

        byte[] payload =
                Arrays.copyOfRange(
                        bytes,
                        24,
                        bytes.length - 4
                );

        return new WifiMacFrame(
                frameControl,
                duration,
                address1,
                address2,
                address3,
                sequenceControl,
                payload
        );
    }

    public static long calculateFcs(
            byte[] bytes
    ) {
        CRC32 crc =
                new CRC32();

        crc.update(
                bytes
        );

        return crc.getValue()
                & 0xFFFF_FFFFL;
    }

    private void validateSupportedHeaderShape() {
        validateSupportedHeaderShape(
                frameControl
        );
    }

    private static void validateSupportedHeaderShape(
            int frameControl
    ) {
        WifiFrameType type =
                WifiFrameType.fromFrameControl(
                        frameControl
                );

        if (type == WifiFrameType.DATA) {
            boolean toDs =
                    (
                            frameControl
                                    & 0x0100
                    )
                            != 0;

            boolean fromDs =
                    (
                            frameControl
                                    & 0x0200
                    )
                            != 0;

            if (toDs
                    && fromDs) {
                throw new IllegalArgumentException(
                        "Four-address WDS data headers are not implemented in Phase 10B"
                );
            }

            int subtype =
                    (
                            frameControl >>> 4
                    )
                            & 0x0F;

            if (subtype >= 8) {
                throw new IllegalArgumentException(
                        "QoS data header serialization is not implemented in Phase 10B"
                );
            }
        }
    }

    private boolean isControlSubtype(
            int subtype
    ) {
        return (
                frameControl
                        & (
                        TYPE_MASK
                                | SUBTYPE_MASK
                )
        )
                == (
                TYPE_CONTROL
                        | subtype
        );
    }

    private static byte[] normalizeMac(
            byte[] value
    ) {
        if (value == null) {
            return new byte[6];
        }

        if (value.length != 6) {
            throw new IllegalArgumentException(
                    "MAC address must be 6 bytes"
            );
        }

        return value.clone();
    }

    private static void writeU16Le(
            ByteArrayOutputStream out,
            int value
    ) {
        out.write(
                value
                        & 0xFF
        );

        out.write(
                (
                        value >>> 8
                )
                        & 0xFF
        );
    }

    private static void writeU32Le(
            ByteArrayOutputStream out,
            long value
    ) {
        out.write(
                (int) value
                        & 0xFF
        );

        out.write(
                (int) (
                        value >>> 8
                )
                        & 0xFF
        );

        out.write(
                (int) (
                        value >>> 16
                )
                        & 0xFF
        );

        out.write(
                (int) (
                        value >>> 24
                )
                        & 0xFF
        );
    }

    private static int readU16Le(
            byte[] bytes,
            int offset
    ) {
        return (
                bytes[offset]
                        & 0xFF
        )
                | (
                (
                        bytes[offset + 1]
                                & 0xFF
                )
                        << 8
        );
    }

    private static long readU32Le(
            byte[] bytes,
            int offset
    ) {
        return (
                bytes[offset]
                        & 0xFFL
        )
                | (
                (
                        bytes[offset + 1]
                                & 0xFFL
                )
                        << 8
        )
                | (
                (
                        bytes[offset + 2]
                                & 0xFFL
                )
                        << 16
        )
                | (
                (
                        bytes[offset + 3]
                                & 0xFFL
                )
                        << 24
        );
    }
}
