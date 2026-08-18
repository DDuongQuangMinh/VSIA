package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;

public record LegacyOfdmPpdu(
        LegacyOfdmRateProfile rate,
        int scramblerSeed,
        int psduLengthBytes,
        int ofdmSymbols,
        int padBits,
        int[] scrambledDataBits,
        int[] puncturedCodedBits,
        int[] interleavedCodedBits,
        Complex[][] frequencyDomainSymbols
) {
    public LegacyOfdmPpdu {
        scrambledDataBits =
                scrambledDataBits.clone();

        puncturedCodedBits =
                puncturedCodedBits.clone();

        interleavedCodedBits =
                interleavedCodedBits.clone();

        frequencyDomainSymbols =
                frequencyDomainSymbols.clone();
    }

    @Override
    public int[] scrambledDataBits() {
        return scrambledDataBits.clone();
    }

    @Override
    public int[] puncturedCodedBits() {
        return puncturedCodedBits.clone();
    }

    @Override
    public int[] interleavedCodedBits() {
        return interleavedCodedBits.clone();
    }

    @Override
    public Complex[][] frequencyDomainSymbols() {
        return frequencyDomainSymbols.clone();
    }
}
