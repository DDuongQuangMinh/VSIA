package com.k1ngtle.vsia.signality.engineering.wifi.phy;

import com.k1ngtle.vsia.signality.engineering.channel.OfdmDopplerAssessment;
import com.k1ngtle.vsia.signality.engineering.channel.OfdmDopplerModel;
import com.k1ngtle.vsia.signality.engineering.channel.RfChannelAssessment;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiMcs;

public final class WifiPhyLinkModel {
    private WifiPhyLinkModel() {
    }

    public static WifiPhyLinkAssessment assess(
            WifiPhyConfiguration configuration,
            WifiMcs mcs,
            RfChannelAssessment channel,
            WifiPuncturingPattern puncturing
    ) {
        WifiOfdmNumerology numerology =
                WifiOfdmNumerologyTable.resolve(
                        configuration.generation(),
                        configuration.channelWidth(),
                        configuration.guardInterval()
                );

        double dopplerHz =
                channel == null
                        ? 0.0
                        : channel.dopplerHz();

        OfdmDopplerAssessment doppler =
                OfdmDopplerModel.assess(
                        dopplerHz,
                        numerology.subcarrierSpacingHz()
                );

        double inputSinrDb =
                channel == null
                        ? 0.0
                        : channel.sinrDb();

        double effectiveSinrDb =
                inputSinrDb;

        if (channel != null) {
            double signal =
                    channel.effectiveDesiredPowerWatts();

            double baseInterference =
                    channel.interferencePowerWatts()
                            + channel.noisePowerWatts();

            double desired =
                    signal
                            * doppler.desiredSubcarrierPowerFraction();

            double ici =
                    signal
                            * doppler.interCarrierInterferencePowerFraction();

            effectiveSinrDb =
                    ratioDb(
                            desired,
                            baseInterference
                                    + ici
                    );
        }

        WifiMimoAssessment mimo =
                WifiMimoModel.assess(
                        configuration,
                        effectiveSinrDb
                );

        WifiPhyRateResult rate =
                WifiPhyRateCalculator.calculate(
                        configuration,
                        mcs,
                        null,
                        puncturing,
                        effectiveSinrDb
                );

        return new WifiPhyLinkAssessment(
                configuration,
                numerology,
                mimo,
                rate,
                inputSinrDb,
                dopplerHz,
                doppler.normalizedFrequencyOffset(),
                doppler.desiredSubcarrierPowerFraction(),
                doppler.interCarrierInterferencePowerFraction(),
                effectiveSinrDb
        );
    }

    public static double equivalentSignalPowerForNoiseFloor(
            WifiPhyLinkAssessment assessment,
            double noisePowerWatts
    ) {
        if (assessment == null) {
            return 0.0;
        }

        double sinrDb =
                assessment.effectiveSinrDb();

        if (!Double.isFinite(
                sinrDb
        )) {
            return sinrDb > 0.0
                    ? Double.MAX_VALUE
                    : 0.0;
        }

        return noisePowerWatts
                * Math.pow(
                10.0,
                sinrDb / 10.0
        );
    }

    private static double ratioDb(
            double numerator,
            double denominator
    ) {
        if (numerator <= 0.0) {
            return Double.NEGATIVE_INFINITY;
        }

        if (denominator <= 0.0) {
            return Double.POSITIVE_INFINITY;
        }

        return 10.0
                * Math.log10(
                numerator / denominator
        );
    }
}
