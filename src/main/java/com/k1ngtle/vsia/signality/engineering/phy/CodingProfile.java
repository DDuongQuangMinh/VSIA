package com.k1ngtle.vsia.signality.engineering.phy;

public record CodingProfile(
        String id,
        double rate,
        double codingGainDb
) {
    public CodingProfile {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id");
        }

        if (rate <= 0.0 || rate > 1.0) {
            throw new IllegalArgumentException("rate must be in (0, 1]");
        }
    }

    public static CodingProfile uncoded() {
        return new CodingProfile(
                "uncoded",
                1.0,
                0.0
        );
    }
}
