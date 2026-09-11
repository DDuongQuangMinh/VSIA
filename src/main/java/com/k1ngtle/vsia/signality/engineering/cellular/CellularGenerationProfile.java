package com.k1ngtle.vsia.signality.engineering.cellular;

public record CellularGenerationProfile(
        CellularGeneration generation,
        String airInterface,
        String multipleAccess,
        double nominalSubcarrierSpacingHz,
        int maximumMimoLayers,
        int maximumCqi,
        double defaultHysteresisDb,
        long defaultTimeToTriggerMillis,
        boolean supportsHarq,
        boolean supportsPacketCore
) {
    public CellularGenerationProfile {
        if (generation == null) {
            throw new IllegalArgumentException("generation");
        }
        airInterface = airInterface == null ? "" : airInterface;
        multipleAccess = multipleAccess == null ? "" : multipleAccess;
        nominalSubcarrierSpacingHz = Math.max(0.0, nominalSubcarrierSpacingHz);
        maximumMimoLayers = Math.max(1, maximumMimoLayers);
        maximumCqi = Math.max(1, maximumCqi);
        defaultHysteresisDb = Math.max(0.0, defaultHysteresisDb);
        defaultTimeToTriggerMillis = Math.max(0L, defaultTimeToTriggerMillis);
    }
}
