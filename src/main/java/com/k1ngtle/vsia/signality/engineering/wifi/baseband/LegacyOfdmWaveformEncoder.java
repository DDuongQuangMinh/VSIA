package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;

public final class LegacyOfdmWaveformEncoder {
    private LegacyOfdmWaveformEncoder() {
    }

    public static LegacyOfdmWaveform encode(
            byte[] psdu,
            LegacyOfdmRateProfile rate,
            int scramblerSeed
    ) {
        LegacyOfdmPpdu data =
                LegacyOfdmPpduEncoder.encode(
                        psdu,
                        rate,
                        scramblerSeed
                );

        LegacySignalField signal =
                new LegacySignalField(
                        rate,
                        psdu == null
                                ? 0
                                : psdu.length
                );

        Complex[] stf =
                LegacyTrainingFields.stfTimeDomain();

        Complex[] ltf =
                LegacyTrainingFields.ltfTimeDomain();

        Complex[] sig =
                LegacySignalCodec.encodeTimeDomain(
                        signal
                );

        Complex[][] frequency =
                data.frequencyDomainSymbols();

        Complex[][] dataTime =
                new Complex[
                        frequency.length
                        ][];

        int dataSamples =
                0;

        for (int i = 0;
             i < frequency.length;
             i++) {
            dataTime[i] =
                    LegacyOfdmTimeDomain
                            .withCyclicPrefix(
                                    frequency[i]
                            );

            dataSamples +=
                    dataTime[i].length;
        }

        int stfStart =
                0;

        int ltfStart =
                stfStart
                        + stf.length;

        int signalStart =
                ltfStart
                        + ltf.length;

        int dataStart =
                signalStart
                        + sig.length;

        Complex[] samples =
                new Complex[
                        dataStart
                                + dataSamples
                        ];

        int cursor =
                0;

        System.arraycopy(
                stf,
                0,
                samples,
                cursor,
                stf.length
        );

        cursor +=
                stf.length;

        System.arraycopy(
                ltf,
                0,
                samples,
                cursor,
                ltf.length
        );

        cursor +=
                ltf.length;

        System.arraycopy(
                sig,
                0,
                samples,
                cursor,
                sig.length
        );

        cursor +=
                sig.length;

        for (Complex[] symbol : dataTime) {
            System.arraycopy(
                    symbol,
                    0,
                    samples,
                    cursor,
                    symbol.length
            );

            cursor +=
                    symbol.length;
        }

        return new LegacyOfdmWaveform(
                signal,
                data,
                samples,
                stfStart,
                ltfStart,
                signalStart,
                dataStart
        );
    }
}
