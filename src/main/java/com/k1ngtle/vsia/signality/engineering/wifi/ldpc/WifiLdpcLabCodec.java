package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

public final class WifiLdpcLabCodec {
    private final QcLdpcBaseMatrix profile;
    private final QcLdpcEncoder encoder;
    private final LayeredMinSumLdpcDecoder decoder;

    public WifiLdpcLabCodec(
            QcLdpcBaseMatrix profile
    ) {
        this.profile =
                profile;

        this.encoder =
                new QcLdpcEncoder(
                        profile
                );

        this.decoder =
                new LayeredMinSumLdpcDecoder(
                        profile
                );
    }

    public QcLdpcBaseMatrix profile() {
        return profile;
    }

    public int[] encode(
            int[] informationBits
    ) {
        return encoder.encode(
                informationBits
        );
    }

    public LdpcDecodeResult decode(
            double[] llrs,
            int maxIterations
    ) {
        return decoder.decode(
                llrs,
                maxIterations,
                0.80
        );
    }

    public int syndromeWeight(
            int[] codeword
    ) {
        return encoder.syndromeWeight(
                codeword
        );
    }
}
