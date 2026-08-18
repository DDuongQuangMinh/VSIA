package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;

public record LegacyOfdmWaveform(
        LegacySignalField signal,
        LegacyOfdmPpdu data,
        Complex[] samples,
        int stfStart,
        int ltfStart,
        int signalStart,
        int dataStart
) {
    public LegacyOfdmWaveform {
        samples =
                samples.clone();
    }

    @Override
    public Complex[] samples() {
        return samples.clone();
    }
}
