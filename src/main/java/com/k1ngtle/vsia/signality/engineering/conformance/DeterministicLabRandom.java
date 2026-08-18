package com.k1ngtle.vsia.signality.engineering.conformance;

import java.util.Random;

public final class DeterministicLabRandom {
    private final long seed;
    private final Random random;

    public DeterministicLabRandom(
            long seed
    ) {
        this.seed = seed;
        this.random =
                new Random(
                        seed
                );
    }

    public long seed() {
        return seed;
    }

    public double nextDouble() {
        return random.nextDouble();
    }

    public int nextInt(
            int bound
    ) {
        return random.nextInt(
                bound
        );
    }

    public long nextLong() {
        return random.nextLong();
    }
}
