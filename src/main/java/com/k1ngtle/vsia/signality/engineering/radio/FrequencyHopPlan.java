package com.k1ngtle.vsia.signality.engineering.radio;

import java.util.Arrays;

public final class FrequencyHopPlan {
    private final double[] frequenciesHz;
    private final int[] permutation;
    private int hopIndex;

    public FrequencyHopPlan(
            double[] frequenciesHz,
            long seed
    ) {
        if (frequenciesHz == null
                || frequenciesHz.length == 0) {
            throw new IllegalArgumentException(
                    "frequenciesHz"
            );
        }

        this.frequenciesHz =
                Arrays.copyOf(
                        frequenciesHz,
                        frequenciesHz.length
                );

        this.permutation =
                new int[
                        frequenciesHz.length
                        ];

        for (int i = 0;
             i < permutation.length;
             i++) {
            permutation[i] = i;
        }

        java.util.Random random =
                new java.util.Random(seed);

        for (int i = permutation.length - 1;
             i > 0;
             i--) {
            int j =
                    random.nextInt(
                            i + 1
                    );

            int temp =
                    permutation[i];

            permutation[i] =
                    permutation[j];

            permutation[j] =
                    temp;
        }
    }

    public double currentFrequencyHz() {
        return frequenciesHz[
                permutation[
                        hopIndex
                                % permutation.length
                        ]
                ];
    }

    public double advance() {
        hopIndex =
                (hopIndex + 1)
                        % permutation.length;

        return currentFrequencyHz();
    }

    public int hopIndex() {
        return hopIndex;
    }

    public void setHopIndex(int hopIndex) {
        this.hopIndex =
                Math.floorMod(
                        hopIndex,
                        permutation.length
                );
    }
}
