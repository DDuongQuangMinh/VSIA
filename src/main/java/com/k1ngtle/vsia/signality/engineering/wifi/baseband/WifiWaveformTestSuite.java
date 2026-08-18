package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public final class WifiWaveformTestSuite {
    private WifiWaveformTestSuite() {
    }

    public static List<WifiWaveformTestResult> runAll() {
        return List.of(
                preambleLengths(),
                pilotPolarityPeriod(),
                signalFieldRoundTrip(),
                waveformLayout(),
                packetDetectionLeadingZeros(),
                cfoEstimate(),
                flatChannelEstimate(),
                softViterbiRoundTrip(),
                cleanWaveformRoundTrip(),
                impairedWaveformRoundTrip(),
                multipathWaveformRoundTrip()
        );
    }

    private static WifiWaveformTestResult preambleLengths() {
        boolean passed =
                LegacyTrainingFields.stfTimeDomain().length == 160
                        && LegacyTrainingFields.ltfTimeDomain().length == 160;

        return result(
                "wifi-w12-preamble-lengths",
                passed,
                "Legacy STF and LTF lab structures must each occupy 160 samples at 20 Msps"
        );
    }

    private static WifiWaveformTestResult pilotPolarityPeriod() {
        boolean passed =
                true;

        for (int i = 0;
             i < 127;
             i++) {
            if (LegacyPilotPolarity.forSymbol(i)
                    != LegacyPilotPolarity.forSymbol(i + 127)) {
                passed =
                        false;
                break;
            }
        }

        return result(
                "wifi-w12-pilot-polarity-period",
                passed,
                "Pilot polarity generator must repeat with period 127"
        );
    }

    private static WifiWaveformTestResult signalFieldRoundTrip() {
        LegacySignalField input =
                new LegacySignalField(
                        LegacyOfdmRateProfile.ofMbps(
                                54
                        ),
                        1500
                );

        Complex[] bins =
                LegacySignalCodec.encodeFrequency(
                        input
                );

        LegacySignalField output =
                LegacySignalCodec.decodeFrequency(
                        bins
                );

        boolean passed =
                output.rate().rateMbps() == 54
                        && output.lengthBytes() == 1500;

        return result(
                "wifi-w12-lsig-roundtrip",
                passed,
                "L-SIG RATE/LENGTH/parity/tail encoding must round-trip in a noiseless frequency-domain test"
        );
    }

    private static WifiWaveformTestResult waveformLayout() {
        byte[] payload =
                new byte[
                        100
                        ];

        LegacyOfdmWaveform waveform =
                LegacyOfdmWaveformEncoder.encode(
                        payload,
                        LegacyOfdmRateProfile.ofMbps(
                                54
                        ),
                        0x5D
                );

        int expected =
                160
                        + 160
                        + 80
                        + 4 * 80;

        boolean passed =
                waveform.samples().length == expected
                        && waveform.stfStart() == 0
                        && waveform.ltfStart() == 160
                        && waveform.signalStart() == 320
                        && waveform.dataStart() == 400;

        return result(
                "wifi-w12-waveform-layout",
                passed,
                "100-byte 54-Mbit/s waveform must be STF160 + LTF160 + L-SIG80 + four DATA symbols"
        );
    }

    private static WifiWaveformTestResult packetDetectionLeadingZeros() {
        LegacyOfdmWaveform waveform =
                shortWaveform();

        Complex[] samples =
                LegacyWaveformChannel.apply(
                        waveform.samples(),
                        new LegacyWaveformImpairment(
                                20_000_000.0,
                                0.0,
                                45.0,
                                7L,
                                37
                        )
                );

        LegacyPacketDetection detection =
                LegacyPacketDetector.detect(
                        samples
                );

        return result(
                "wifi-w12-packet-detection",
                Math.abs(
                        detection.sampleIndex() - 37
                ) <= 8
                        && detection.metric() > 0.90,
                "STF detector should place its coarse estimate within eight samples of a 37-sample leading offset"
        );
    }

    private static WifiWaveformTestResult cfoEstimate() {
        LegacyOfdmWaveform waveform =
                shortWaveform();

        double expected =
                125_000.0;

        Complex[] samples =
                LegacyWaveformChannel.apply(
                        waveform.samples(),
                        new LegacyWaveformImpairment(
                                20_000_000.0,
                                expected,
                                45.0,
                                11L,
                                21
                        )
                );

        LegacyPacketDetection detection =
                LegacyPacketDetector.detect(
                        samples
                );

        double estimated =
                LegacyCfoEstimator.estimateHz(
                        samples,
                        detection.sampleIndex(),
                        20_000_000.0
                );

        return result(
                "wifi-w12-cfo-estimate",
                Math.abs(
                        estimated - expected
                ) < 500.0,
                "STF lag-16 phase estimator should recover 125 kHz CFO within 500 Hz at high SNR"
        );
    }

    private static WifiWaveformTestResult flatChannelEstimate() {
        LegacyOfdmWaveform waveform =
                shortWaveform();

        LegacyChannelEstimate estimate =
                LegacyChannelEstimator.estimateFromLtf(
                        waveform.samples(),
                        waveform.ltfStart()
                );

        Complex[] response =
                estimate.frequencyResponse();

        Complex[] reference =
                LegacyTrainingFields.ltfFrequency();

        double error =
                0.0;

        int count =
                0;

        for (int i = 0;
             i < response.length;
             i++) {
            if (reference[i].magnitudeSquared()
                    < 1.0E-18) {
                continue;
            }

            error +=
                    response[i]
                            .subtract(
                                    new Complex(
                                            1.0,
                                            0.0
                                    )
                            )
                            .magnitudeSquared();

            count++;
        }

        double mse =
                error
                        / Math.max(
                        1,
                        count
                );

        return result(
                "wifi-w12-flat-channel-estimate",
                mse < 1.0E-20,
                "Noiseless L-LTF channel estimate should be unity on every occupied training carrier"
        );
    }

    private static WifiWaveformTestResult softViterbiRoundTrip() {
        int[] input =
                deterministicBits(
                        216
                );

        int[] coded =
                WifiBccEncoder.encode(
                        input,
                        BccCodeRate.RATE_3_4
                );

        double[] llr =
                new double[
                        coded.length
                        ];

        for (int i = 0;
             i < coded.length;
             i++) {
            llr[i] =
                    coded[i] == 1
                            ? 8.0
                            : -8.0;
        }

        int[] output =
                WifiSoftViterbiDecoder.decode(
                        llr,
                        input.length,
                        BccCodeRate.RATE_3_4
                );

        return result(
                "wifi-w12-soft-viterbi-roundtrip",
                Arrays.equals(
                        input,
                        output
                ),
                "Soft-decision Viterbi must decode a high-confidence rate-3/4 punctured stream"
        );
    }

    private static WifiWaveformTestResult cleanWaveformRoundTrip() {
        byte[] payload =
                "VSIA-W1.2-CLEAN"
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        LegacyOfdmWaveform waveform =
                LegacyOfdmWaveformEncoder.encode(
                        payload,
                        LegacyOfdmRateProfile.ofMbps(
                                54
                        ),
                        0x5D
                );

        LegacyOfdmReceiveResult result =
                LegacyOfdmWaveformReceiver.receive(
                        waveform.samples(),
                        0x5D
                );

        return result(
                "wifi-w12-clean-waveform-roundtrip",
                Arrays.equals(
                        payload,
                        result.psdu()
                ),
                "Complete time-domain legacy waveform must decode noiselessly"
        );
    }

    private static WifiWaveformTestResult impairedWaveformRoundTrip() {
        byte[] payload =
                "VSIA-W1.2-CFO-AWGN"
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        LegacyOfdmWaveform waveform =
                LegacyOfdmWaveformEncoder.encode(
                        payload,
                        LegacyOfdmRateProfile.ofMbps(
                                54
                        ),
                        0x5D
                );

        Complex[] impaired =
                LegacyWaveformChannel.apply(
                        waveform.samples(),
                        new LegacyWaveformImpairment(
                                20_000_000.0,
                                125_000.0,
                                35.0,
                                1234L,
                                37,
                                0.0005
                        )
                );

        LegacyOfdmReceiveResult received =
                LegacyOfdmWaveformReceiver.receive(
                        impaired,
                        0x5D
                );

        boolean passed =
                Arrays.equals(
                        payload,
                        received.psdu()
                )
                        && received.synchronizedPacketStart() == 37
                        && Math.abs(
                        received.estimatedCfoHz()
                                - 125_000.0
                ) < 1_000.0;

        return result(
                "wifi-w12-cfo-awgn-phase-noise-roundtrip",
                passed,
                "Receiver must survive 125 kHz CFO, 35 dB AWGN, small phase noise and a 37-sample leading offset"
        );
    }

    private static WifiWaveformTestResult multipathWaveformRoundTrip() {
        byte[] payload =
                "VSIA-W1.2-MULTIPATH"
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        LegacyOfdmWaveform waveform =
                LegacyOfdmWaveformEncoder.encode(
                        payload,
                        LegacyOfdmRateProfile.ofMbps(
                                36
                        ),
                        0x5D
                );

        Complex[] multipath =
                LegacyMultipathChannel.apply(
                        waveform.samples(),
                        new Complex[] {
                                new Complex(
                                        1.0,
                                        0.0
                                ),
                                new Complex(
                                        0.0,
                                        0.0
                                ),
                                new Complex(
                                        0.22,
                                        0.08
                                ),
                                new Complex(
                                        -0.10,
                                        0.05
                                )
                        }
                );

        Complex[] impaired =
                LegacyWaveformChannel.apply(
                        multipath,
                        new LegacyWaveformImpairment(
                                20_000_000.0,
                                35_000.0,
                                40.0,
                                99L,
                                16
                        )
                );

        LegacyOfdmReceiveResult received =
                LegacyOfdmWaveformReceiver.receive(
                        impaired,
                        0x5D
                );

        return result(
                "wifi-w12-multipath-equalization-roundtrip",
                Arrays.equals(
                        payload,
                        received.psdu()
                ),
                "L-LTF channel estimation and one-tap frequency equalization must recover a short in-CP multipath channel"
        );
    }

    private static LegacyOfdmWaveform shortWaveform() {
        return LegacyOfdmWaveformEncoder.encode(
                "VSIA"
                        .getBytes(
                                StandardCharsets.UTF_8
                        ),
                LegacyOfdmRateProfile.ofMbps(
                        24
                ),
                0x5D
        );
    }

    private static int[] deterministicBits(
            int count
    ) {
        int[] bits =
                new int[
                        count
                        ];

        int state =
                0x1234567;

        for (int i = 0;
             i < count;
             i++) {
            state =
                    state * 1103515245
                            + 12345;

            bits[i] =
                    (state >>> 30)
                            & 1;
        }

        return bits;
    }

    private static WifiWaveformTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new WifiWaveformTestResult(
                id,
                passed,
                detail
        );
    }
}
