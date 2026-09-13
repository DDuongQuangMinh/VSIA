package com.k1ngtle.vsia.signality.internet.satellite.internet;

import com.k1ngtle.vsia.signality.internet.satellite.SatelliteLinkAssessment;
import com.k1ngtle.vsia.signality.internet.satellite.device.TemporarySatelliteTerminalBlockEntity;

public record SatelliteInternetPath(
        TemporarySatelliteTerminalBlockEntity userTerminal,
        TemporarySatelliteTerminalBlockEntity gatewayTerminal,
        SatelliteLinkAssessment link
) {
    public boolean valid() {
        return userTerminal != null
                && gatewayTerminal != null
                && link != null
                && link.visible();
    }
}
