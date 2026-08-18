package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

import com.k1ngtle.vsia.signality.engineering.wifi.live.WifiLivePhyMode;
import com.k1ngtle.vsia.signality.engineering.wifi.live.WifiLivePhyPath;
import com.k1ngtle.vsia.signality.engineering.wifi.phy.WifiPhyGeneration;

import java.util.List;
import java.util.UUID;

public final class WifiEngineeringGuiTestSuite {
    private WifiEngineeringGuiTestSuite() {
    }

    public static List<WifiEngineeringGuiTestResult> runAll() {
        return List.of(
                historyCapacity(),
                historyOrder(),
                historyClear(),
                sampleProjection()
        );
    }

    private static WifiEngineeringGuiTestResult historyCapacity() {
        WifiEngineeringHistory history =
                new WifiEngineeringHistory(
                        3
                );

        history.add(
                snapshot(
                        1.0,
                        1
                )
        );
        history.add(
                snapshot(
                        2.0,
                        2
                )
        );
        history.add(
                snapshot(
                        3.0,
                        3
                )
        );
        history.add(
                snapshot(
                        4.0,
                        4
                )
        );

        boolean passed =
                history.size() == 3
                        && history.samples()
                        .get(0)
                        .snrDb() == 2.0
                        && history.samples()
                        .get(2)
                        .snrDb() == 4.0;

        return result(
                "wifi-w16-history-capacity",
                passed,
                "Analyzer history must evict the oldest sample when capacity is exceeded"
        );
    }

    private static WifiEngineeringGuiTestResult historyOrder() {
        WifiEngineeringHistory history =
                new WifiEngineeringHistory(
                        5
                );

        history.add(
                snapshot(
                        10.0,
                        5
                )
        );
        history.add(
                snapshot(
                        20.0,
                        6
                )
        );
        history.add(
                snapshot(
                        30.0,
                        7
                )
        );

        List<WifiEngineeringSample> samples =
                history.samples();

        boolean passed =
                samples.get(0)
                        .sequence()
                        < samples.get(1)
                        .sequence()
                        && samples.get(1)
                        .sequence()
                        < samples.get(2)
                        .sequence()
                        && samples.get(0)
                        .snrDb() == 10.0
                        && samples.get(2)
                        .snrDb() == 30.0;

        return result(
                "wifi-w16-history-order",
                passed,
                "Analyzer samples must remain in chronological order"
        );
    }

    private static WifiEngineeringGuiTestResult historyClear() {
        WifiEngineeringHistory history =
                new WifiEngineeringHistory(
                        4
                );

        history.add(
                snapshot(
                        12.0,
                        3
                )
        );

        history.clear();

        return result(
                "wifi-w16-history-clear",
                history.size() == 0,
                "Clear-history control must remove all stored analyzer samples"
        );
    }

    private static WifiEngineeringGuiTestResult sampleProjection() {
        WifiEngineeringHistory history =
                new WifiEngineeringHistory(
                        4
                );

        history.add(
                snapshot(
                        22.5,
                        17
                )
        );

        WifiEngineeringSample sample =
                history.samples()
                        .get(0);

        boolean passed =
                sample.snrDb() == 22.5
                        && sample.sinrDb() == 21.25
                        && sample.ber() == 1.0E-7
                        && sample.fer() == 2.0E-4
                        && sample.mediumEnergyDbm() == -67.0
                        && sample.phyRateBps() == 866_700_000.0
                        && sample.decoderIterations() == 17
                        && sample.delivered();

        return result(
                "wifi-w16-sample-projection",
                passed,
                "History sample must preserve the engineering metrics plotted by the GUI"
        );
    }

    private static WifiEngineeringSnapshot snapshot(
            double snrDb,
            int iterations
    ) {
        return new WifiEngineeringSnapshot(
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000016"
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
                866_700_000.0,
                0.001,
                -58.0,
                snrDb,
                1.0E-7,
                2.0E-4,
                21.25,
                0.31,
                220L,
                0.20,
                8.5,
                true,
                false,
                -67.0,
                1,
                WifiLivePhyMode.BIT_LEVEL_AUTO,
                WifiLivePhyPath.STANDARD_LDPC_FEC,
                true,
                true,
                2,
                iterations,
                "Recovered"
        );
    }

    private static WifiEngineeringGuiTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new WifiEngineeringGuiTestResult(
                id,
                passed,
                detail
        );
    }
}
