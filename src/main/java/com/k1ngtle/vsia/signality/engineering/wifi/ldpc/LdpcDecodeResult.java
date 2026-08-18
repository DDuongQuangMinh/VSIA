package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

public record LdpcDecodeResult(
        int[] codeword,
        int[] informationBits,
        int iterations,
        boolean converged,
        int syndromeWeight
) {
    public LdpcDecodeResult {
        codeword =
                codeword.clone();

        informationBits =
                informationBits.clone();
    }

    @Override
    public int[] codeword() {
        return codeword.clone();
    }

    @Override
    public int[] informationBits() {
        return informationBits.clone();
    }
}
