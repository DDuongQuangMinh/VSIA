package com.k1ngtle.vsia.signality.engineering.reality;

public final class GeneralRfAirtimeModel {
    private GeneralRfAirtimeModel() {
    }

    public static long estimateMicros(
            long payloadBits,
            double bandwidthHz
    ) {
        double conservativeBitRate =
                Math.max(
                        1.0,
                        bandwidthHz
                                * 0.50
                );

        double seconds =
                Math.max(
                        0.0,
                        payloadBits
                                / conservativeBitRate
                );

        return Math.max(
                1L,
                (long) Math.ceil(
                        seconds
                                * 1_000_000.0
                )
        );
    }
}
