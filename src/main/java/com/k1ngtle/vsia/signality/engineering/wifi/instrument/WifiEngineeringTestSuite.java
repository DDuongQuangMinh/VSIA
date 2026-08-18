package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

import com.k1ngtle.vsia.signality.engineering.wifi.live.WifiLivePhyMode;
import com.k1ngtle.vsia.signality.engineering.wifi.live.WifiLivePhyPath;
import com.k1ngtle.vsia.signality.engineering.wifi.phy.WifiPhyGeneration;

import java.util.List;
import java.util.UUID;

public final class WifiEngineeringTestSuite {
    private WifiEngineeringTestSuite() {
    }

    public static List<WifiEngineeringTestResult> runAll() {
        return List.of(
                formatterIdentity(),
                formatterPhy(),
                formatterLink(),
                formatterReality(),
                formatterLive()
        );
    }

    private static WifiEngineeringTestResult formatterIdentity() {
        List<String> lines =
                WifiEngineeringFormatter.lines(
                        snapshot()
                );

        boolean passed =
                lines.get(0)
                        .contains(
                                "00000000-0000-0000-0000-000000000001"
                        )
                        && lines.get(1)
                        .contains(
                                "5.180000 GHz"
                        );

        return result(
                "wifi-w15-formatter-identity",
                passed,
                "Engineering formatter must expose device identity and tuned frequency"
        );
    }

    private static WifiEngineeringTestResult formatterPhy() {
        List<String> lines =
                WifiEngineeringFormatter.lines(
                        snapshot()
                );

        boolean passed =
                lines.stream()
                        .anyMatch(
                                line ->
                                        line.contains(
                                                "HE"
                                        )
                                                && line.contains(
                                                "MCS 11"
                                        )
                                                && line.contains(
                                                "80 MHz"
                                        )
                                                && line.contains(
                                                "2 SS"
                                        )
                        )
                        && lines.stream()
                        .anyMatch(
                                line ->
                                        line.contains(
                                                "1.200 Gbit/s"
                                        )
                        );

        return result(
                "wifi-w15-formatter-phy",
                passed,
                "Engineering formatter must expose generation, MCS, width, streams and PHY rate"
        );
    }

    private static WifiEngineeringTestResult formatterLink() {
        List<String> lines =
                WifiEngineeringFormatter.lines(
                        snapshot()
                );

        boolean passed =
                lines.stream()
                        .anyMatch(
                                line ->
                                        line.contains(
                                                "-54.250 dBm"
                                        )
                                                && line.contains(
                                                "31.500 dB"
                                        )
                                                && line.contains(
                                                "29.750 dB"
                                        )
                        )
                        && lines.stream()
                        .anyMatch(
                                line ->
                                        line.contains(
                                                "1.000e-08"
                                        )
                                                && line.contains(
                                                "2.500e-05"
                                        )
                        );

        return result(
                "wifi-w15-formatter-link",
                passed,
                "Engineering formatter must expose RSSI, SNR, SINR, BER and FER"
        );
    }

    private static WifiEngineeringTestResult formatterReality() {
        List<String> lines =
                WifiEngineeringFormatter.lines(
                        snapshot()
                );

        boolean passed =
                lines.stream()
                        .anyMatch(
                                line ->
                                        line.contains(
                                                "245 us"
                                        )
                                                && line.contains(
                                                "0.420000 us"
                                        )
                        )
                        && lines.stream()
                        .anyMatch(
                                line ->
                                        line.contains(
                                                "37.500%"
                                        )
                                                && line.contains(
                                                "7.250 dB"
                                        )
                        )
                        && lines.stream()
                        .anyMatch(
                                line ->
                                        line.contains(
                                                "BUSY"
                                        )
                                                && line.contains(
                                                "-61.000 dBm"
                                        )
                                                && line.contains(
                                                "2"
                                        )
                        );

        return result(
                "wifi-w15-formatter-reality",
                passed,
                "Engineering formatter must expose airtime, propagation, interference, capture and medium state"
        );
    }

    private static WifiEngineeringTestResult formatterLive() {
        List<String> lines =
                WifiEngineeringFormatter.lines(
                        snapshot()
                );

        boolean passed =
                lines.stream()
                        .anyMatch(
                                line ->
                                        line.contains(
                                                "BIT_LEVEL_AUTO"
                                        )
                                                && line.contains(
                                                "STANDARD_LDPC_FEC"
                                        )
                        )
                        && lines.stream()
                        .anyMatch(
                                line ->
                                        line.contains(
                                                "3"
                                        )
                                                && line.contains(
                                                "17"
                                        )
                        )
                        && lines.stream()
                        .anyMatch(
                                line ->
                                        line.contains(
                                                "Recovered"
                                        )
                        );

        return result(
                "wifi-w15-formatter-live",
                passed,
                "Engineering formatter must expose detailed-PHY path, result, codeword count and decoder iterations"
        );
    }

    private static WifiEngineeringSnapshot snapshot() {
        return new WifiEngineeringSnapshot(
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000001"
                ),
                "vsia:wifi_6e",
                5_180_000_000.0,
                "STATION",
                "ASSOCIATED",
                "SECURED",
                11,
                WifiPhyGeneration.HE,
                80,
                0.8,
                2,
                1_200_000_000.0,
                0.0025,
                -54.25,
                31.5,
                1.0E-8,
                2.5E-5,
                29.75,
                0.42,
                245L,
                0.375,
                7.25,
                true,
                true,
                -61.0,
                2,
                WifiLivePhyMode.BIT_LEVEL_AUTO,
                WifiLivePhyPath.STANDARD_LDPC_FEC,
                true,
                true,
                3,
                17,
                "Recovered MAC frame"
        );
    }

    private static WifiEngineeringTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new WifiEngineeringTestResult(
                id,
                passed,
                detail
        );
    }
}
