package com.k1ngtle.vsia.signality.engineering.channel;

public record RfMediumState(
        double totalEnergyWatts,
        double totalEnergyDbm,
        int overlappingTransmitters,
        boolean busy
) {
}
