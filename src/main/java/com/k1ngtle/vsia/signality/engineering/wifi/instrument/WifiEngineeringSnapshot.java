package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

import com.k1ngtle.vsia.signality.engineering.wifi.live.WifiLivePhyMode;
import com.k1ngtle.vsia.signality.engineering.wifi.live.WifiLivePhyPath;
import com.k1ngtle.vsia.signality.engineering.wifi.phy.WifiPhyGeneration;

import java.util.UUID;

public record WifiEngineeringSnapshot(
        UUID deviceId,
        String networkProfile,
        double frequencyHz,
        String wifiMode,
        String stationState,
        String securityState,
        int mcsIndex,
        WifiPhyGeneration generation,
        int channelWidthMhz,
        double guardIntervalUs,
        int spatialStreams,
        double estimatedPhyRateBps,
        double dopplerIciFraction,
        double receivedPowerDbm,
        double snrDb,
        double bitErrorRate,
        double frameErrorRate,
        double correctedSinrDb,
        double propagationDelayMicros,
        long airtimeMicros,
        double temporalInterferenceFactor,
        double captureMarginDb,
        boolean captured,
        boolean mediumBusy,
        double mediumEnergyDbm,
        int overlappingTransmitters,
        WifiLivePhyMode liveMode,
        WifiLivePhyPath livePath,
        boolean liveEvaluated,
        boolean liveDelivered,
        int liveCodewords,
        int liveDecoderIterations,
        String liveDetail
) {
}
