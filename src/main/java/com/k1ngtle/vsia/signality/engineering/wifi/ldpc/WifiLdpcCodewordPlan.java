package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

public record WifiLdpcCodewordPlan(
        WifiLdpcCodewordLength codewordLength,
        WifiLdpcTargetRate targetRate,
        int codewordCount,
        int payloadBits,
        int nominalInformationBitsPerCodeword,
        int totalNominalInformationBits,
        int shorteningBits
) {
}
