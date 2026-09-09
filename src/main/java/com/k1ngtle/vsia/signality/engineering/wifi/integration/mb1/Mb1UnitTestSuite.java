package com.k1ngtle.vsia.signality.engineering.wifi.integration.mb1;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiNetworkRecord;
import com.k1ngtle.vsia.signality.engineering.wifi.smartconnect.WifiBand;
import com.k1ngtle.vsia.signality.engineering.wifi.smartconnect.WifiBandUtil;
import com.k1ngtle.vsia.signality.engineering.wifi.smartconnect.WifiSmartConnectDecision;
import com.k1ngtle.vsia.signality.engineering.wifi.smartconnect.WifiSmartConnectPolicy;

import java.util.List;

public final class Mb1UnitTestSuite {
    public record Result(
            String name,
            boolean passed,
            String detail
    ) {
    }

    private Mb1UnitTestSuite() {
    }

    public static List<Result> runAll() {
        return List.of(
                bandClassification(),
                healthyFiveGhzPreferred(),
                weakFiveGhzFallsBack(),
                strongTwoFourWins(),
                stickyCurrentBand(),
                sixGhzExcludedFromMb1()
        );
    }

    private static Result bandClassification() {
        boolean passed =
                WifiBandUtil.bandForFrequency(
                        2_437_000_000.0D
                ) == WifiBand.TWO_FOUR_GHZ
                        && WifiBandUtil.bandForFrequency(
                        5_200_000_000.0D
                ) == WifiBand.FIVE_GHZ
                        && WifiBandUtil.bandForFrequency(
                        6_100_000_000.0D
                ) == WifiBand.SIX_GHZ;

        return result(
                "mb1-band-classification",
                passed,
                "2.437 / 5.2 / 6.1 GHz resolve to 2.4 / 5 / 6 GHz bands"
        );
    }

    private static Result healthyFiveGhzPreferred() {
        WifiNetworkRecord twoFour =
                network(
                        "02:00:00:00:24:01",
                        2_437_000_000.0D
                );

        WifiNetworkRecord five =
                network(
                        "02:00:00:00:50:01",
                        5_200_000_000.0D
                );

        WifiSmartConnectDecision decision =
                WifiSmartConnectPolicy.DEFAULT.select(
                        "VSIA-SMART",
                        "",
                        List.of(
                                twoFour,
                                five
                        ),
                        value ->
                                value == five
                                        ? 30.0D
                                        : 32.0D
                );

        boolean passed =
                decision.available()
                        && decision.band()
                        == WifiBand.FIVE_GHZ;

        return result(
                "mb1-healthy-5ghz-preferred",
                passed,
                decision.reason()
        );
    }

    private static Result weakFiveGhzFallsBack() {
        WifiNetworkRecord twoFour =
                network(
                        "02:00:00:00:24:02",
                        2_437_000_000.0D
                );

        WifiNetworkRecord five =
                network(
                        "02:00:00:00:50:02",
                        5_200_000_000.0D
                );

        WifiSmartConnectDecision decision =
                WifiSmartConnectPolicy.DEFAULT.select(
                        "VSIA-SMART",
                        "",
                        List.of(
                                twoFour,
                                five
                        ),
                        value ->
                                value == five
                                        ? 12.0D
                                        : 22.0D
                );

        boolean passed =
                decision.available()
                        && decision.band()
                        == WifiBand.TWO_FOUR_GHZ;

        return result(
                "mb1-weak-5ghz-fallback",
                passed,
                decision.reason()
        );
    }

    private static Result strongTwoFourWins() {
        WifiNetworkRecord twoFour =
                network(
                        "02:00:00:00:24:03",
                        2_437_000_000.0D
                );

        WifiNetworkRecord five =
                network(
                        "02:00:00:00:50:03",
                        5_200_000_000.0D
                );

        WifiSmartConnectDecision decision =
                WifiSmartConnectPolicy.DEFAULT.select(
                        "VSIA-SMART",
                        "",
                        List.of(
                                twoFour,
                                five
                        ),
                        value ->
                                value == five
                                        ? 20.0D
                                        : 31.0D
                );

        boolean passed =
                decision.available()
                        && decision.band()
                        == WifiBand.TWO_FOUR_GHZ;

        return result(
                "mb1-2ghz-range-advantage",
                passed,
                decision.reason()
        );
    }

    private static Result stickyCurrentBand() {
        WifiNetworkRecord twoFour =
                network(
                        "02:00:00:00:24:04",
                        2_437_000_000.0D
                );

        WifiNetworkRecord five =
                network(
                        "02:00:00:00:50:04",
                        5_200_000_000.0D
                );

        WifiSmartConnectPolicy policy =
                new WifiSmartConnectPolicy(
                        18.0D,
                        2.0D,
                        3.0D
                );

        WifiSmartConnectDecision decision =
                policy.select(
                        "VSIA-SMART",
                        twoFour.bssid(),
                        List.of(
                                twoFour,
                                five
                        ),
                        value ->
                                value == five
                                        ? 30.0D
                                        : 30.0D
                );

        boolean passed =
                decision.available()
                        && decision.band()
                        == WifiBand.TWO_FOUR_GHZ;

        return result(
                "mb1-band-stickiness",
                passed,
                decision.reason()
        );
    }

    private static Result sixGhzExcludedFromMb1() {
        WifiNetworkRecord twoFour =
                network(
                        "02:00:00:00:24:05",
                        2_437_000_000.0D
                );

        WifiNetworkRecord six =
                network(
                        "02:00:00:00:60:05",
                        6_100_000_000.0D
                );

        WifiSmartConnectDecision decision =
                WifiSmartConnectPolicy.DEFAULT.select(
                        "VSIA-SMART",
                        "",
                        List.of(
                                twoFour,
                                six
                        ),
                        value ->
                                value == six
                                        ? 60.0D
                                        : 20.0D
                );

        boolean passed =
                decision.available()
                        && decision.band()
                        == WifiBand.TWO_FOUR_GHZ;

        return result(
                "mb1-dual-band-boundary",
                passed,
                "MB1 deliberately steers only 2.4 and 5 GHz; 6 GHz remains outside this dual-band stage"
        );
    }

    private static WifiNetworkRecord network(
            String bssid,
            double frequencyHz
    ) {
        return new WifiNetworkRecord(
                "VSIA-SMART",
                bssid,
                "signality:open",
                "signality:wifi_7",
                frequencyHz,
                System.nanoTime()
        );
    }

    private static Result result(
            String name,
            boolean passed,
            String detail
    ) {
        return new Result(
                name,
                passed,
                detail
        );
    }
}
