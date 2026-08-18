package com.k1ngtle.vsia.signality.engineering.wifi.phy;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiMcs;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiMcsTable;

import java.util.List;

public final class WifiPhyTestSuite {
    private WifiPhyTestSuite() {
    }

    public static List<WifiPhyTestResult> runAll() {
        return List.of(
                he20Numerology(),
                vht80Numerology(),
                he20Mcs11Rate(),
                eht320Mcs13Rate(),
                puncturingRateReduction(),
                mimoRankBound(),
                ofdmaFitsChannel()
        );
    }

    private static WifiPhyTestResult he20Numerology() {
        WifiOfdmNumerology value =
                WifiOfdmNumerologyTable.resolve(
                        WifiPhyGeneration.HE,
                        WifiChannelWidth.MHZ_20,
                        WifiGuardInterval.GI_0_8_US
                );

        boolean passed =
                value.fftSize() == 256
                        && Math.abs(
                        value.subcarrierSpacingHz()
                                - 78_125.0
                ) < 1.0E-9
                        && Math.abs(
                        value.usefulSymbolTimeUs()
                                - 12.8
                ) < 1.0E-12
                        && value.dataTones() == 234;

        return result(
                "wifi-he-20mhz-numerology",
                passed,
                "HE 20 MHz uses 256-point numerology, 78.125 kHz spacing and 234 full-band data tones"
        );
    }

    private static WifiPhyTestResult vht80Numerology() {
        WifiOfdmNumerology value =
                WifiOfdmNumerologyTable.resolve(
                        WifiPhyGeneration.VHT,
                        WifiChannelWidth.MHZ_80,
                        WifiGuardInterval.GI_0_8_US
                );

        boolean passed =
                value.fftSize() == 256
                        && Math.abs(
                        value.subcarrierSpacingHz()
                                - 312_500.0
                ) < 1.0E-9
                        && value.dataTones() == 234
                        && value.pilotTones() == 8;

        return result(
                "wifi-vht-80mhz-numerology",
                passed,
                "VHT 80 MHz uses 312.5 kHz spacing with 234 data and 8 pilot subcarriers"
        );
    }

    private static WifiPhyTestResult he20Mcs11Rate() {
        WifiPhyConfiguration configuration =
                new WifiPhyConfiguration(
                        WifiPhyGeneration.HE,
                        WifiChannelWidth.MHZ_20,
                        WifiGuardInterval.GI_0_8_US,
                        1,
                        1,
                        1,
                        0.0,
                        1.0
                );

        WifiMcs mcs =
                WifiMcsTable.byIndex(
                        11
                );

        WifiPhyRateResult rate =
                WifiPhyRateCalculator.calculate(
                        configuration,
                        mcs,
                        null,
                        WifiPuncturingPattern.none(
                                WifiChannelWidth.MHZ_20
                        ),
                        40.0
                );

        double expected =
                143_382_352.94117647;

        boolean passed =
                Math.abs(
                        rate.grossPhyRateBps()
                                - expected
                )
                        < 2.0;

        return result(
                "wifi-he20-mcs11-1ss-rate",
                passed,
                "Expected approximately 143.382353 Mbit/s from 234*10*(5/6)/(12.8+0.8 us)"
        );
    }

    private static WifiPhyTestResult eht320Mcs13Rate() {
        WifiPhyConfiguration configuration =
                new WifiPhyConfiguration(
                        WifiPhyGeneration.EHT,
                        WifiChannelWidth.MHZ_320,
                        WifiGuardInterval.GI_0_8_US,
                        1,
                        1,
                        1,
                        0.0,
                        1.0
                );

        WifiPhyRateResult rate =
                WifiPhyRateCalculator.calculate(
                        configuration,
                        WifiMcsTable.byIndex(
                                13
                        ),
                        null,
                        WifiPuncturingPattern.none(
                                WifiChannelWidth.MHZ_320
                        ),
                        50.0
                );

        double expected =
                2_882_352_941.1764708;

        boolean passed =
                Math.abs(
                        rate.grossPhyRateBps()
                                - expected
                )
                        < 10.0;

        return result(
                "wifi-eht320-mcs13-1ss-rate",
                passed,
                "Expected approximately 2.882353 Gbit/s from 3920*12*(5/6)/(12.8+0.8 us)"
        );
    }

    private static WifiPhyTestResult puncturingRateReduction() {
        WifiPhyConfiguration configuration =
                new WifiPhyConfiguration(
                        WifiPhyGeneration.EHT,
                        WifiChannelWidth.MHZ_80,
                        WifiGuardInterval.GI_0_8_US,
                        1,
                        1,
                        1,
                        0.0,
                        1.0
                );

        WifiMcs mcs =
                WifiMcsTable.byIndex(
                        11
                );

        WifiPhyRateResult full =
                WifiPhyRateCalculator.calculate(
                        configuration,
                        mcs,
                        null,
                        WifiPuncturingPattern.none(
                                WifiChannelWidth.MHZ_80
                        ),
                        40.0
                );

        WifiPhyRateResult half =
                WifiPhyRateCalculator.calculate(
                        configuration,
                        mcs,
                        null,
                        new WifiPuncturingPattern(
                                4,
                                0b0101
                        ),
                        40.0
                );

        boolean passed =
                Math.abs(
                        half.effectivePhyRateBps()
                                / full.effectivePhyRateBps()
                                - 0.5
                ) < 1.0E-12;

        return result(
                "wifi-eht-puncturing-rate-reduction",
                passed,
                "Two inactive 20 MHz segments out of four must reduce modeled usable bandwidth to 50%"
        );
    }

    private static WifiPhyTestResult mimoRankBound() {
        WifiPhyConfiguration configuration =
                new WifiPhyConfiguration(
                        WifiPhyGeneration.EHT,
                        WifiChannelWidth.MHZ_160,
                        WifiGuardInterval.GI_0_8_US,
                        4,
                        4,
                        2,
                        0.1,
                        1.0
                );

        WifiMimoAssessment assessment =
                WifiMimoModel.assess(
                        configuration,
                        40.0
                );

        boolean passed =
                assessment.channelRank() == 2
                        && assessment.usableSpatialStreams() <= 2;

        return result(
                "wifi-mimo-rank-bound",
                passed,
                "Usable spatial streams cannot exceed min(Ntx,Nrx)"
        );
    }

    private static WifiPhyTestResult ofdmaFitsChannel() {
        List<WifiOfdmaAllocation> allocations =
                WifiOfdmaScheduler.allocate(
                        WifiChannelWidth.MHZ_20,
                        List.of(
                                new WifiOfdmaUserDemand("a", 4.0, 30.0),
                                new WifiOfdmaUserDemand("b", 3.0, 25.0),
                                new WifiOfdmaUserDemand("c", 2.0, 20.0),
                                new WifiOfdmaUserDemand("d", 1.0, 15.0)
                        )
                );

        int end =
                allocations.stream()
                        .mapToInt(
                                value ->
                                        value.toneStart()
                                                + value.toneCount()
                        )
                        .max()
                        .orElse(
                                0
                        );

        boolean passed =
                allocations.size() == 4
                        && end
                        <= WifiResourceUnit.RU_242.tones();

        return result(
                "wifi-ofdma-allocation-fits-channel",
                passed,
                "Allocated RUs must fit inside the full-band tone budget"
        );
    }

    private static WifiPhyTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new WifiPhyTestResult(
                id,
                passed,
                detail
        );
    }
}
