package com.k1ngtle.vsia.signality.internet.satellite.internet;

import com.k1ngtle.vsia.signality.internet.satellite.SatelliteLinkAssessment;
import com.k1ngtle.vsia.signality.internet.satellite.device.TemporarySatelliteTerminalBlockEntity;

public record SatelliteInternetPath(
        TemporarySatelliteTerminalBlockEntity sourceTerminal,
        TemporarySatelliteTerminalBlockEntity destinationTerminal,
        SatelliteLinkAssessment link,
        double endpointDistanceBlocks,
        double sourceAccessDistanceBlocks,
        double destinationAccessDistanceBlocks
) {
    public boolean valid() {
        return sourceTerminal != null
                && destinationTerminal != null
                && link != null
                && link.visible();
    }
}
