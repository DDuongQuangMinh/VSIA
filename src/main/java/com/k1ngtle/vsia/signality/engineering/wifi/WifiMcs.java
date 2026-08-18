package com.k1ngtle.vsia.signality.engineering.wifi;

import com.k1ngtle.vsia.signality.engineering.phy.CodingProfile;
import com.k1ngtle.vsia.signality.engineering.phy.Modulation;
import com.k1ngtle.vsia.signality.engineering.phy.PhyProfile;

public record WifiMcs(
        int index,
        Modulation modulation,
        double codingRate,
        double minimumSnrDb
) {
    public PhyProfile applyTo(PhyProfile base) {
        return new PhyProfile(
                base.centerFrequencyHz(),
                base.bandwidthHz(),
                base.txPowerDbm(),
                base.txGainDbi(),
                base.rxGainDbi(),
                base.receiverNoiseFigureDb(),
                modulation,
                new CodingProfile(
                        "wifi_mcs_" + index,
                        codingRate,
                        base.coding().codingGainDb()
                ),
                base.spatialStreams(),
                base.guardEfficiency(),
                base.macEfficiency()
        );
    }
}
