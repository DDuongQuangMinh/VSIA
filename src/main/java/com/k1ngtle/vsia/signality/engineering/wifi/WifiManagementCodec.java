package com.k1ngtle.vsia.signality.engineering.wifi;

import net.minecraft.nbt.CompoundTag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class WifiManagementCodec {
    private static final int IE_SSID = 0;
    private static final int IE_SUPPORTED_RATES = 1;
    private static final int IE_VENDOR_SPECIFIC = 221;

    private static final byte[] DEFAULT_RATES =
            new byte[] {
                    (byte) 0x82,
                    (byte) 0x84,
                    (byte) 0x8B,
                    (byte) 0x96,
                    0x0C,
                    0x12,
                    0x18,
                    0x24
            };

    private static final byte[] SIGNALLITY_OUI =
            new byte[] {
                    0x56,
                    0x53,
                    0x49
            };

    private WifiManagementCodec() {
    }

    public static byte[] encodeBody(
            int frameControl,
            CompoundTag body
    ) {
        int subtype =
                frameControl
                        & 0x00FC;

        try {
            if (subtype
                    == (
                    WifiMacController.FC_AUTH
                            & 0x00FC
            )) {
                return encodeAuthentication(
                        body
                );
            }

            if (subtype
                    == (
                    WifiMacController.FC_ASSOC_REQ
                            & 0x00FC
            )) {
                return encodeAssociationRequest(
                        body
                );
            }

            if (subtype
                    == (
                    WifiMacController.FC_ASSOC_RESP
                            & 0x00FC
            )) {
                return encodeAssociationResponse(
                        body
                );
            }

            if (subtype
                    == (
                    WifiMacController.FC_PROBE_REQ
                            & 0x00FC
            )) {
                return encodeProbeRequest(
                        body
                );
            }

            if (subtype
                    == (
                    WifiMacController.FC_PROBE_RESP
                            & 0x00FC
            )
                    || subtype
                    == (
                    WifiMacController.FC_BEACON
                            & 0x00FC
            )) {
                return encodeAdvertisement(
                        body
                );
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Unable to encode management body",
                    exception
            );
        }

        throw new IllegalArgumentException(
                "Unsupported management subtype: "
                        + Integer.toHexString(
                        subtype
                )
        );
    }

    public static CompoundTag decodeBody(
            int frameControl,
            byte[] payload
    ) {
        int subtype =
                frameControl
                        & 0x00FC;

        try {
            if (subtype
                    == (
                    WifiMacController.FC_AUTH
                            & 0x00FC
            )) {
                return decodeAuthentication(
                        payload
                );
            }

            if (subtype
                    == (
                    WifiMacController.FC_ASSOC_REQ
                            & 0x00FC
            )) {
                return decodeAssociationRequest(
                        payload
                );
            }

            if (subtype
                    == (
                    WifiMacController.FC_ASSOC_RESP
                            & 0x00FC
            )) {
                return decodeAssociationResponse(
                        payload
                );
            }

            if (subtype
                    == (
                    WifiMacController.FC_PROBE_REQ
                            & 0x00FC
            )) {
                return decodeProbeRequest(
                        payload
                );
            }

            if (subtype
                    == (
                    WifiMacController.FC_PROBE_RESP
                            & 0x00FC
            )
                    || subtype
                    == (
                    WifiMacController.FC_BEACON
                            & 0x00FC
            )) {
                return decodeAdvertisement(
                        payload
                );
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Unable to decode management body",
                    exception
            );
        }

        throw new IllegalArgumentException(
                "Unsupported management subtype"
        );
    }

    public static boolean isManagementSubtype(
            int frameControl
    ) {
        return WifiFrameType.fromFrameControl(
                frameControl
        )
                == WifiFrameType.MANAGEMENT;
    }

    private static byte[] encodeAuthentication(
            CompoundTag body
    ) throws Exception {
        ByteArrayOutputStream bytes =
                new ByteArrayOutputStream();

        DataOutputStream out =
                new DataOutputStream(
                        bytes
                );

        writeU16Le(
                out,
                body.getInt(
                        "algorithm"
                )
        );

        writeU16Le(
                out,
                body.getInt(
                        "transaction_sequence"
                )
        );

        writeU16Le(
                out,
                body.getInt(
                        "status_code"
                )
        );

        return bytes.toByteArray();
    }

    private static CompoundTag decodeAuthentication(
            byte[] payload
    ) throws Exception {
        requireLength(
                payload,
                6
        );

        DataInputStream in =
                new DataInputStream(
                        new ByteArrayInputStream(
                                payload
                        )
                );

        CompoundTag body =
                new CompoundTag();

        body.putInt(
                "algorithm",
                readU16Le(
                        in
                )
        );

        body.putInt(
                "transaction_sequence",
                readU16Le(
                        in
                )
        );

        body.putInt(
                "status_code",
                readU16Le(
                        in
                )
        );

        return body;
    }

    private static byte[] encodeAssociationRequest(
            CompoundTag body
    ) throws Exception {
        ByteArrayOutputStream bytes =
                new ByteArrayOutputStream();

        DataOutputStream out =
                new DataOutputStream(
                        bytes
                );

        writeU16Le(
                out,
                0x0001
        );

        writeU16Le(
                out,
                10
        );

        out.write(
                WifiInformationElementCodec.encode(
                        List.of(
                                ssid(
                                        body.getString(
                                                "ssid"
                                        )
                                ),
                                supportedRates()
                        )
                )
        );

        return bytes.toByteArray();
    }

    private static CompoundTag decodeAssociationRequest(
            byte[] payload
    ) throws Exception {
        requireLength(
                payload,
                4
        );

        CompoundTag body =
                new CompoundTag();

        List<WifiInformationElement> elements =
                WifiInformationElementCodec.decode(
                        slice(
                                payload,
                                4
                        )
                );

        body.putString(
                "ssid",
                ssidFrom(
                        elements
                )
        );

        return body;
    }

    private static byte[] encodeAssociationResponse(
            CompoundTag body
    ) throws Exception {
        ByteArrayOutputStream bytes =
                new ByteArrayOutputStream();

        DataOutputStream out =
                new DataOutputStream(
                        bytes
                );

        writeU16Le(
                out,
                0x0001
        );

        writeU16Le(
                out,
                body.getInt(
                        "status_code"
                )
        );

        int aid =
                body.getInt(
                        "association_id"
                )
                        & 0x3FFF;

        writeU16Le(
                out,
                aid | 0xC000
        );

        return bytes.toByteArray();
    }

    private static CompoundTag decodeAssociationResponse(
            byte[] payload
    ) throws Exception {
        requireLength(
                payload,
                6
        );

        DataInputStream in =
                new DataInputStream(
                        new ByteArrayInputStream(
                                payload
                        )
                );

        readU16Le(
                in
        );

        CompoundTag body =
                new CompoundTag();

        body.putInt(
                "status_code",
                readU16Le(
                        in
                )
        );

        body.putInt(
                "association_id",
                readU16Le(
                        in
                )
                        & 0x3FFF
        );

        return body;
    }

    private static byte[] encodeProbeRequest(
            CompoundTag body
    ) {
        return WifiInformationElementCodec.encode(
                List.of(
                        ssid(
                                body.getString(
                                        "ssid"
                                )
                        ),
                        supportedRates()
                )
        );
    }

    private static CompoundTag decodeProbeRequest(
            byte[] payload
    ) {
        CompoundTag body =
                new CompoundTag();

        body.putString(
                "ssid",
                ssidFrom(
                        WifiInformationElementCodec.decode(
                                payload
                        )
                )
        );

        return body;
    }

    private static byte[] encodeAdvertisement(
            CompoundTag body
    ) throws Exception {
        ByteArrayOutputStream bytes =
                new ByteArrayOutputStream();

        DataOutputStream out =
                new DataOutputStream(
                        bytes
                );

        writeU64Le(
                out,
                0L
        );

        writeU16Le(
                out,
                100
        );

        int capabilities =
                0x0001;

        String security =
                body.getString(
                        "security"
                );

        if (security != null
                && !security.isBlank()
                && !security.endsWith(
                ":open"
        )) {
            capabilities |=
                    0x0010;
        }

        writeU16Le(
                out,
                capabilities
        );

        List<WifiInformationElement> elements =
                new ArrayList<>();

        elements.add(
                ssid(
                        body.getString(
                                "ssid"
                        )
                )
        );

        elements.add(
                supportedRates()
        );

        elements.add(
                vendorMetadata(
                        body
                )
        );

        out.write(
                WifiInformationElementCodec.encode(
                        elements
                )
        );

        return bytes.toByteArray();
    }

    private static CompoundTag decodeAdvertisement(
            byte[] payload
    ) throws Exception {
        requireLength(
                payload,
                12
        );

        CompoundTag body =
                new CompoundTag();

        List<WifiInformationElement> elements =
                WifiInformationElementCodec.decode(
                        slice(
                                payload,
                                12
                        )
                );

        body.putString(
                "ssid",
                ssidFrom(
                        elements
                )
        );

        for (WifiInformationElement element : elements) {
            if (element.id()
                    == IE_VENDOR_SPECIFIC) {
                decodeVendorMetadata(
                        element.data(),
                        body
                );
            }
        }

        return body;
    }

    private static WifiInformationElement ssid(
            String value
    ) {
        byte[] bytes =
                value == null
                        ? new byte[0]
                        : value.getBytes(
                        StandardCharsets.UTF_8
                );

        if (bytes.length > 32) {
            byte[] trimmed =
                    new byte[32];

            System.arraycopy(
                    bytes,
                    0,
                    trimmed,
                    0,
                    32
            );

            bytes =
                    trimmed;
        }

        return new WifiInformationElement(
                IE_SSID,
                bytes
        );
    }

    private static WifiInformationElement supportedRates() {
        return new WifiInformationElement(
                IE_SUPPORTED_RATES,
                DEFAULT_RATES
        );
    }

    private static String ssidFrom(
            List<WifiInformationElement> elements
    ) {
        for (WifiInformationElement element : elements) {
            if (element.id()
                    == IE_SSID) {
                return new String(
                        element.data(),
                        StandardCharsets.UTF_8
                );
            }
        }

        return "";
    }

    private static WifiInformationElement vendorMetadata(
            CompoundTag body
    ) throws Exception {
        ByteArrayOutputStream bytes =
                new ByteArrayOutputStream();

        DataOutputStream out =
                new DataOutputStream(
                        bytes
                );

        out.write(
                SIGNALLITY_OUI
        );

        out.writeByte(
                1
        );

        out.writeUTF(
                body.getString(
                        "security"
                )
        );

        out.writeUTF(
                body.getString(
                        "network_profile"
                )
        );

        out.writeDouble(
                body.getDouble(
                        "frequency_hz"
                )
        );

        return new WifiInformationElement(
                IE_VENDOR_SPECIFIC,
                bytes.toByteArray()
        );
    }

    private static void decodeVendorMetadata(
            byte[] bytes,
            CompoundTag body
    ) throws Exception {
        if (bytes.length < 4
                || bytes[0] != SIGNALLITY_OUI[0]
                || bytes[1] != SIGNALLITY_OUI[1]
                || bytes[2] != SIGNALLITY_OUI[2]
                || bytes[3] != 1) {
            return;
        }

        DataInputStream in =
                new DataInputStream(
                        new ByteArrayInputStream(
                                bytes,
                                4,
                                bytes.length - 4
                        )
                );

        body.putString(
                "security",
                in.readUTF()
        );

        body.putString(
                "network_profile",
                in.readUTF()
        );

        body.putDouble(
                "frequency_hz",
                in.readDouble()
        );
    }

    private static byte[] slice(
            byte[] source,
            int from
    ) {
        byte[] result =
                new byte[
                        source.length - from
                        ];

        System.arraycopy(
                source,
                from,
                result,
                0,
                result.length
        );

        return result;
    }

    private static void requireLength(
            byte[] payload,
            int minimum
    ) {
        if (payload == null
                || payload.length < minimum) {
            throw new IllegalArgumentException(
                    "Management body too short"
            );
        }
    }

    private static void writeU16Le(
            DataOutputStream out,
            int value
    ) throws Exception {
        out.writeByte(
                value
                        & 0xFF
        );

        out.writeByte(
                (
                        value >>> 8
                )
                        & 0xFF
        );
    }

    private static int readU16Le(
            DataInputStream in
    ) throws Exception {
        int lo =
                in.readUnsignedByte();

        int hi =
                in.readUnsignedByte();

        return lo
                | (
                hi << 8
        );
    }

    private static void writeU64Le(
            DataOutputStream out,
            long value
    ) throws Exception {
        for (int i = 0;
             i < 8;
             i++) {
            out.writeByte(
                    (int) (
                            value
                                    >>> (
                                    i * 8
                            )
                    )
                            & 0xFF
            );
        }
    }
}
