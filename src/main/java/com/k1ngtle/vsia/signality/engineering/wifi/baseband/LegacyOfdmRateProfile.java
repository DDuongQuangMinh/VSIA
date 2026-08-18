package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.phy.Modulation;

public record LegacyOfdmRateProfile(
        int rateMbps,
        Modulation modulation,
        BccCodeRate codeRate
) {
    public LegacyOfdmRateProfile {
        if (rateMbps <= 0
                || modulation == null
                || codeRate == null) {
            throw new IllegalArgumentException(
                    "rate/modulation/codeRate"
            );
        }
    }

    public int bitsPerSubcarrier() {
        return modulation.bitsPerSymbol();
    }

    public int codedBitsPerSymbol() {
        return 48
                * bitsPerSubcarrier();
    }

    public int dataBitsPerSymbol() {
        return codedBitsPerSymbol()
                * codeRate.numerator()
                / codeRate.denominator();
    }

    public static LegacyOfdmRateProfile ofMbps(
            int rateMbps
    ) {
        return switch (rateMbps) {
            case 6 ->
                    new LegacyOfdmRateProfile(
                            6,
                            Modulation.BPSK,
                            BccCodeRate.RATE_1_2
                    );

            case 9 ->
                    new LegacyOfdmRateProfile(
                            9,
                            Modulation.BPSK,
                            BccCodeRate.RATE_3_4
                    );

            case 12 ->
                    new LegacyOfdmRateProfile(
                            12,
                            Modulation.QPSK,
                            BccCodeRate.RATE_1_2
                    );

            case 18 ->
                    new LegacyOfdmRateProfile(
                            18,
                            Modulation.QPSK,
                            BccCodeRate.RATE_3_4
                    );

            case 24 ->
                    new LegacyOfdmRateProfile(
                            24,
                            Modulation.QAM16,
                            BccCodeRate.RATE_1_2
                    );

            case 36 ->
                    new LegacyOfdmRateProfile(
                            36,
                            Modulation.QAM16,
                            BccCodeRate.RATE_3_4
                    );

            case 48 ->
                    new LegacyOfdmRateProfile(
                            48,
                            Modulation.QAM64,
                            BccCodeRate.RATE_2_3
                    );

            case 54 ->
                    new LegacyOfdmRateProfile(
                            54,
                            Modulation.QAM64,
                            BccCodeRate.RATE_3_4
                    );

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported legacy OFDM rate: "
                                    + rateMbps
                    );
        };
    }
}
