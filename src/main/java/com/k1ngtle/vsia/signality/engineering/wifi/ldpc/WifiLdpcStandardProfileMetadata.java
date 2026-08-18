package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

public record WifiLdpcStandardProfileMetadata(
        String id,
        String standardFamily,
        String externalReference,
        WifiLdpcCodewordLength codewordLength,
        WifiLdpcTargetRate rate,
        int expansionFactor
) {
    public WifiLdpcStandardProfileMetadata {
        if (id == null
                || id.isBlank()
                || standardFamily == null
                || standardFamily.isBlank()
                || externalReference == null
                || externalReference.isBlank()
                || codewordLength == null
                || rate == null
                || expansionFactor < 1) {
            throw new IllegalArgumentException(
                    "invalid LDPC standard-profile metadata"
            );
        }
    }
}
