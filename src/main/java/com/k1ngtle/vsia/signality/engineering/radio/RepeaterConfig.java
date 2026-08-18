package com.k1ngtle.vsia.signality.engineering.radio;

public record RepeaterConfig(
        double inputFrequencyHz,
        double outputFrequencyHz,
        String accessCode
) {
    public RepeaterConfig {
        if (inputFrequencyHz <= 0.0
                || outputFrequencyHz <= 0.0) {
            throw new IllegalArgumentException(
                    "Repeater frequencies must be positive"
            );
        }

        accessCode =
                accessCode == null
                        ? ""
                        : accessCode;
    }
}
