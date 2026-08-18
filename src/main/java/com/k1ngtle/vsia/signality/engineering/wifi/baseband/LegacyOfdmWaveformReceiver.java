package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;
import com.k1ngtle.vsia.signality.engineering.math.Fft;

public final class LegacyOfdmWaveformReceiver {
    public static final double SAMPLE_RATE_HZ =
            20_000_000.0;

    private LegacyOfdmWaveformReceiver() {
    }

    public static LegacyOfdmReceiveResult receive(
            Complex[] received,
            int scramblerSeed
    ) {
        LegacyPacketDetection detection =
                LegacyPacketDetector.detect(
                        received
                );

        int packetStart =
                detection.sampleIndex();

        double cfoHz =
                LegacyCfoEstimator.estimateHz(
                        received,
                        packetStart,
                        SAMPLE_RATE_HZ
                );

        Complex[] corrected =
                LegacyCfoEstimator.correct(
                        received,
                        cfoHz,
                        SAMPLE_RATE_HZ,
                        packetStart
                );

        packetStart =
                LegacyLtfSynchronizer.refinePacketStart(
                        corrected,
                        packetStart
                );

        int ltfStart =
                packetStart
                        + LegacyTrainingFields.STF_SAMPLES;

        LegacyChannelEstimate channel =
                LegacyChannelEstimator.estimateFromLtf(
                        corrected,
                        ltfStart
                );

        int signalStart =
                ltfStart
                        + LegacyTrainingFields.LTF_SAMPLES;

        Complex[] signalTime =
                slice(
                        corrected,
                        signalStart,
                        LegacySignalCodec.CP_SAMPLES
                                + LegacySignalCodec.SYMBOL_SAMPLES
                );

        Complex[] signalBins =
                Fft.fft(
                        LegacyOfdmTimeDomain.removeCyclicPrefix(
                                signalTime
                        )
                );

        signalBins =
                LegacyEqualizer.equalize(
                        signalBins,
                        channel
                );

        signalBins =
                LegacyPilotPhaseCorrector.correct(
                        signalBins,
                        0
                );

        LegacySignalField signal =
                LegacySignalCodec.decodeFrequency(
                        signalBins
                );

        LegacyOfdmRateProfile rate =
                signal.rate();

        int uncodedWithoutPad =
                LegacyOfdmPpduEncoder.SERVICE_BITS
                        + signal.lengthBytes()
                        * 8
                        + LegacyOfdmPpduEncoder.TAIL_BITS;

        int nDbps =
                rate.dataBitsPerSymbol();

        int symbols =
                Math.max(
                        1,
                        (
                                uncodedWithoutPad
                                        + nDbps - 1
                        )
                                / nDbps
                );

        int dataStart =
                signalStart
                        + LegacyOfdmTimeDomain.SYMBOL_SAMPLES;

        int nCbps =
                rate.codedBitsPerSymbol();

        double[] softCoded =
                new double[
                        symbols
                                * nCbps
                        ];

        for (int symbol = 0;
             symbol < symbols;
             symbol++) {
            int start =
                    dataStart
                            + symbol
                            * LegacyOfdmTimeDomain.SYMBOL_SAMPLES;

            Complex[] time =
                    slice(
                            corrected,
                            start,
                            LegacyOfdmTimeDomain.SYMBOL_SAMPLES
                    );

            Complex[] bins =
                    LegacyOfdmTimeDomain.fftAfterCp(
                            time
                    );

            bins =
                    LegacyEqualizer.equalize(
                            bins,
                            channel
                    );

            bins =
                    LegacyPilotPhaseCorrector.correct(
                            bins,
                            symbol + 1
                    );

            Complex[] data =
                    LegacyOfdmSubcarrierMapper.extractData(
                            bins
                    );

            double[] interleavedLlr =
                    WifiSoftDemapper.llr(
                            data,
                            rate.modulation(),
                            channel.noiseVariance()
                    );

            double[] codedLlr =
                    deinterleaveSoft(
                            interleavedLlr,
                            rate.bitsPerSubcarrier()
                    );

            System.arraycopy(
                    codedLlr,
                    0,
                    softCoded,
                    symbol * nCbps,
                    nCbps
            );
        }

        int inputBitCount =
                symbols
                        * nDbps;

        int[] decodedScrambled =
                WifiSoftViterbiDecoder.decode(
                        softCoded,
                        inputBitCount,
                        rate.codeRate()
                );

        int[] descrambled =
                WifiScrambler.apply(
                        decodedScrambled,
                        scramblerSeed
                );

        int payloadBits =
                signal.lengthBytes()
                        * 8;

        int[] psduBits =
                new int[
                        payloadBits
                        ];

        System.arraycopy(
                descrambled,
                LegacyOfdmPpduEncoder.SERVICE_BITS,
                psduBits,
                0,
                payloadBits
        );

        return new LegacyOfdmReceiveResult(
                WifiBitOrder.lsbFirstBitsToBytes(
                        psduBits
                ),
                detection,
                packetStart,
                cfoHz,
                signal,
                channel
        );
    }

    private static double[] deinterleaveSoft(
            double[] input,
            int bitsPerSubcarrier
    ) {
        int nCbps =
                input.length;

        if (nCbps <= 0
                || nCbps % 16 != 0) {
            throw new IllegalArgumentException(
                    "N_CBPS"
            );
        }

        int s =
                Math.max(
                        bitsPerSubcarrier / 2,
                        1
                );

        double[] output =
                new double[
                        nCbps
                        ];

        for (int k = 0;
             k < nCbps;
             k++) {
            int i =
                    (
                            nCbps / 16
                    )
                            * (k % 16)
                            + k / 16;

            int j =
                    s
                            * (i / s)
                            + (
                            i
                                    + nCbps
                                    - (
                                    16 * i
                            )
                                    / nCbps
                    )
                            % s;

            output[k] =
                    input[j];
        }

        return output;
    }

    private static Complex[] slice(
            Complex[] source,
            int start,
            int length
    ) {
        if (start < 0
                || length < 0
                || start + length
                > source.length) {
            throw new IllegalArgumentException(
                    "Sample slice out of bounds"
            );
        }

        Complex[] result =
                new Complex[
                        length
                        ];

        System.arraycopy(
                source,
                start,
                result,
                0,
                length
        );

        return result;
    }
}
