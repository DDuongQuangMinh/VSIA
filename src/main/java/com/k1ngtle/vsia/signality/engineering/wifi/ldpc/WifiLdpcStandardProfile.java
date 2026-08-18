package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

public record WifiLdpcStandardProfile(
        WifiLdpcStandardProfileMetadata metadata,
        QcLdpcBaseMatrix matrix
) {
    public WifiLdpcStandardProfile {
        if (metadata == null
                || matrix == null) {
            throw new IllegalArgumentException(
                    "metadata/matrix"
            );
        }

        if (!matrix.standardized()) {
            throw new IllegalArgumentException(
                    "standard profile matrix must be marked standardized"
            );
        }

        if (matrix.expansionFactor()
                != metadata.expansionFactor()) {
            throw new IllegalArgumentException(
                    "matrix/metadata expansion-factor mismatch"
            );
        }

        if (matrix.codewordBits()
                != metadata.codewordLength()
                .bits()) {
            throw new IllegalArgumentException(
                    "matrix/metadata codeword-length mismatch"
            );
        }
    }
}
