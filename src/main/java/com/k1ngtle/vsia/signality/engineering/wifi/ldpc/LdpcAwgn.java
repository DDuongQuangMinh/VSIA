package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

import java.util.Random;

public final class LdpcAwgn {
    private LdpcAwgn() {
    }

    public static double[] bpskLlrs(
            int[] bits,
            double snrDb,
            long seed
    ) {
        double noiseVariance =
                Math.pow(
                        10.0,
                        -snrDb / 10.0
                );

        double sigma =
                Math.sqrt(
                        noiseVariance / 2.0
                );

        Random random =
                new Random(
                        seed
                );

        double[] llrs =
                new double[
                        bits.length
                        ];

        for (int i = 0;
             i < bits.length;
             i++) {
            double transmitted =
                    bits[i] == 1
                            ? 1.0
                            : -1.0;

            double received =
                    transmitted
                            + random.nextGaussian()
                            * sigma;

            llrs[i] =
                    2.0
                            * received
                            / Math.max(
                            1.0E-12,
                            noiseVariance
                    );
        }

        return llrs;
    }
}
