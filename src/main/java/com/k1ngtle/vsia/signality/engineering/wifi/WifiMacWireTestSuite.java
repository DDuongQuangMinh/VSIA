package com.k1ngtle.vsia.signality.engineering.wifi;

import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

public final class WifiMacWireTestSuite {
    private WifiMacWireTestSuite() {
    }

    public static List<WifiMacWireTestResult> runAll() {
        return List.of(
                ackKnownAnswer(),
                ackRoundTrip(),
                fcsCorruptionRejected(),
                informationElementRoundTrip(),
                managementSsidRoundTrip(),
                blockAckRoundTrip(),
                navTimer(),
                timingRelationships()
        );
    }

    private static WifiMacWireTestResult ackKnownAnswer() {
        WifiMacFrame frame =
                new WifiMacFrame(
                        WifiMacController.FC_ACK,
                        0,
                        mac(
                                "00:11:22:33:44:55"
                        ),
                        new byte[6],
                        new byte[6],
                        0,
                        new byte[0]
                );

        String actual =
                HexFormat.of()
                        .formatHex(
                                frame.encode()
                        );

        String expected =
                "d400000000112233445571eaf24b";

        return result(
                "wifi-ack-wire-known-answer",
                expected.equals(
                        actual
                ),
                "expected="
                        + expected
                        + " actual="
                        + actual
        );
    }

    private static WifiMacWireTestResult ackRoundTrip() {
        WifiMacFrame original =
                new WifiMacFrame(
                        WifiMacController.FC_ACK,
                        314,
                        mac(
                                "AA:BB:CC:DD:EE:FF"
                        ),
                        new byte[6],
                        new byte[6],
                        0,
                        new byte[0]
                );

        WifiMacFrame decoded =
                WifiMacFrame.decode(
                        original.encode()
                );

        boolean passed =
                decoded.frameControl()
                        == original.frameControl()
                        && decoded.durationId()
                        == original.durationId()
                        && Arrays.equals(
                        decoded.address1(),
                        original.address1()
                )
                        && decoded.payload().length
                        == 0;

        return result(
                "wifi-ack-wire-roundtrip",
                passed,
                "ACK must use a receiver-address-only control header and preserve FCS"
        );
    }

    private static WifiMacWireTestResult fcsCorruptionRejected() {
        WifiMacFrame frame =
                new WifiMacFrame(
                        WifiMacController.FC_DATA,
                        0,
                        mac(
                                "00:11:22:33:44:55"
                        ),
                        mac(
                                "66:77:88:99:AA:BB"
                        ),
                        mac(
                                "CC:DD:EE:FF:00:11"
                        ),
                        0x1230,
                        new byte[] {
                                1,
                                2,
                                3,
                                4,
                                5
                        }
                );

        byte[] encoded =
                frame.encode();

        encoded[10] ^=
                0x01;

        boolean rejected =
                false;

        try {
            WifiMacFrame.decode(
                    encoded
            );
        } catch (IllegalArgumentException expected) {
            rejected =
                    true;
        }

        return result(
                "wifi-fcs-corruption-rejected",
                rejected,
                "A one-bit protected-frame mutation must fail FCS validation"
        );
    }

    private static WifiMacWireTestResult informationElementRoundTrip() {
        List<WifiInformationElement> input =
                List.of(
                        new WifiInformationElement(
                                0,
                                "VSIA".getBytes(
                                        java.nio.charset.StandardCharsets.UTF_8
                                )
                        ),
                        new WifiInformationElement(
                                1,
                                new byte[] {
                                        (byte) 0x82,
                                        (byte) 0x84
                                }
                        )
                );

        List<WifiInformationElement> output =
                WifiInformationElementCodec.decode(
                        WifiInformationElementCodec.encode(
                                input
                        )
                );

        boolean passed =
                output.size()
                        == 2
                        && output.get(0).id()
                        == 0
                        && Arrays.equals(
                        output.get(0).data(),
                        input.get(0).data()
                )
                        && output.get(1).id()
                        == 1;

        return result(
                "wifi-information-element-roundtrip",
                passed,
                "IE ID/Length/Payload serialization must round-trip"
        );
    }

    private static WifiMacWireTestResult managementSsidRoundTrip() {
        CompoundTag body =
                new CompoundTag();

        body.putString(
                "ssid",
                "VSIA-LAB"
        );

        byte[] encoded =
                WifiManagementCodec.encodeBody(
                        WifiMacController.FC_PROBE_REQ,
                        body
                );

        CompoundTag decoded =
                WifiManagementCodec.decodeBody(
                        WifiMacController.FC_PROBE_REQ,
                        encoded
                );

        return result(
                "wifi-probe-ssid-ie-roundtrip",
                "VSIA-LAB".equals(
                        decoded.getString(
                                "ssid"
                        )
                ),
                "Probe Request SSID must be carried by an Information Element"
        );
    }

    private static WifiMacWireTestResult blockAckRoundTrip() {
        WifiBlockAckBitmap bitmap =
                new WifiBlockAckBitmap(
                        100
                );

        bitmap.acknowledge(
                100
        );

        bitmap.acknowledge(
                101
        );

        bitmap.acknowledge(
                103
        );

        WifiBlockAckBitmap decoded =
                WifiCompressedBlockAckCodec.decode(
                        WifiCompressedBlockAckCodec.encode(
                                bitmap,
                                3
                        )
                );

        boolean passed =
                decoded.startingSequence()
                        == 100
                        && decoded.acknowledged(
                        100
                )
                        && decoded.acknowledged(
                        101
                )
                        && !decoded.acknowledged(
                        102
                )
                        && decoded.acknowledged(
                        103
                );

        return result(
                "wifi-compressed-block-ack-roundtrip",
                passed,
                "Compressed Block Ack starting sequence and 64-bit bitmap must round-trip"
        );
    }

    private static WifiMacWireTestResult navTimer() {
        WifiNavState nav =
                new WifiNavState();

        nav.observe(
                1_000L,
                500
        );

        boolean passed =
                nav.active(
                        1_499L
                )
                        && !nav.active(
                        1_500L
                )
                        && nav.remainingMicros(
                        1_250L
                )
                        == 250L;

        return result(
                "wifi-nav-duration",
                passed,
                "NAV must expire at the advertised Duration/ID deadline"
        );
    }

    private static WifiMacWireTestResult timingRelationships() {
        WifiMacTimingProfile timing =
                WifiMacTimingProfile.ofdmDefault();

        boolean passed =
                timing.difsUs()
                        == timing.sifsUs()
                        + 2
                        * timing.slotTimeUs()
                        && timing.aifsUs(
                        WifiAccessCategory.BEST_EFFORT
                )
                        == timing.sifsUs()
                        + WifiAccessCategory.BEST_EFFORT.aifsn()
                        * timing.slotTimeUs();

        return result(
                "wifi-interframe-timing-relationships",
                passed,
                "DIFS/AIFS must be derived from SIFS, slot time and AIFSN"
        );
    }

    private static WifiMacWireTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new WifiMacWireTestResult(
                id,
                passed,
                detail
        );
    }

    private static byte[] mac(
            String value
    ) {
        String normalized =
                value.replace(
                                ":",
                                ""
                        )
                        .replace(
                                "-",
                                ""
                        );

        return HexFormat.of()
                .parseHex(
                        normalized
                );
    }
}
