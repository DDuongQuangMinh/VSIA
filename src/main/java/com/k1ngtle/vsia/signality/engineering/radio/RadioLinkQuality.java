package com.k1ngtle.vsia.signality.engineering.radio;

public record RadioLinkQuality(
        double receivedPowerDbm,
        double snrDb,
        double intelligibility,
        double staticLevel,
        double packetSuccessProbability,
        boolean squelchOpen
) {
}
