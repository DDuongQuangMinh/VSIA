package com.k1ngtle.vsia.signality.engineering.wifi.phy;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiMcs;
import com.k1ngtle.vsia.signality.engineering.channel.RfChannelAssessment;

public final class WifiPhyController {
    private WifiPhyConfiguration configuration;
    private WifiPuncturingPattern puncturing;
    private WifiPhyLinkAssessment lastAssessment;

    public void configure(
            WifiPhyConfiguration configuration
    ) {
        this.configuration =
                configuration;

        this.puncturing =
                WifiPuncturingPattern.none(
                        configuration.channelWidth()
                );
    }

    public WifiPhyConfiguration configuration() {
        return configuration;
    }

    public WifiPuncturingPattern puncturing() {
        return puncturing;
    }

    public void setPuncturing(
            WifiPuncturingPattern puncturing
    ) {
        this.puncturing =
                puncturing;
    }

    public WifiPhyLinkAssessment lastAssessment() {
        return lastAssessment;
    }

    public WifiPhyLinkAssessment assess(
            WifiMcs mcs,
            RfChannelAssessment channel
    ) {
        if (configuration == null) {
            throw new IllegalStateException(
                    "Wi-Fi PHY is not configured"
            );
        }

        lastAssessment =
                WifiPhyLinkModel.assess(
                        configuration,
                        mcs,
                        channel,
                        puncturing
                );

        return lastAssessment;
    }
}
