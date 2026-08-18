package com.k1ngtle.vsia.signality.engineering.channel;

public record OfdmDopplerAssessment(
        double normalizedFrequencyOffset,
        double desiredSubcarrierPowerFraction,
        double interCarrierInterferencePowerFraction,
        double carrierLossDb
) {
}
