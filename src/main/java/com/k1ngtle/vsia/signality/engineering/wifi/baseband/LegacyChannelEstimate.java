package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;

public record LegacyChannelEstimate(
        Complex[] frequencyResponse,
        double noiseVariance
) {
    public LegacyChannelEstimate {
        frequencyResponse =
                frequencyResponse.clone();

        noiseVariance =
                Math.max(
                        1.0E-12,
                        noiseVariance
                );
    }

    @Override
    public Complex[] frequencyResponse() {
        return frequencyResponse.clone();
    }
}
