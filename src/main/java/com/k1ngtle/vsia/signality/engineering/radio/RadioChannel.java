package com.k1ngtle.vsia.signality.engineering.radio;

public record RadioChannel(
        String id,
        double frequencyHz,
        double bandwidthHz,
        RadioEmission emission,
        String accessCode
) {
    public RadioChannel {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id");
        }

        if (frequencyHz <= 0.0) {
            throw new IllegalArgumentException("frequencyHz");
        }

        if (bandwidthHz <= 0.0) {
            throw new IllegalArgumentException("bandwidthHz");
        }

        if (emission == null) {
            throw new IllegalArgumentException("emission");
        }

        accessCode = accessCode == null ? "" : accessCode;
    }
}
