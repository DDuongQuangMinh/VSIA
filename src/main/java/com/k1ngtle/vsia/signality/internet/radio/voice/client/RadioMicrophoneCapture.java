package com.k1ngtle.vsia.signality.internet.radio.voice.client;

import com.k1ngtle.vsia.signality.internet.radio.voice.MuLawCodec;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class RadioMicrophoneCapture {
    public static final int RADIO_SAMPLE_RATE =
            8_000;

    public static final int FRAME_MILLIS =
            20;

    public static final int RADIO_SAMPLES_PER_FRAME =
            RADIO_SAMPLE_RATE
                    * FRAME_MILLIS
                    / 1_000;

    private static final float[] CAPTURE_RATES =
            new float[]{
                    48_000.0F,
                    44_100.0F,
                    16_000.0F
            };

    private static final double NOISE_GATE_RMS =
            90.0;

    private static final double TARGET_RMS =
            5_500.0;

    private final AtomicBoolean running =
            new AtomicBoolean();

    private volatile TargetDataLine line;
    private volatile Thread thread;
    private volatile String lastError = "";

    private double smoothedGain =
            1.0;

    public boolean start(
            Consumer<byte[]> frameConsumer
    ) {
        if (frameConsumer == null
                || running.get()) {
            return false;
        }

        CaptureLine selected;

        try {
            selected =
                    openCaptureLine();
        } catch (Exception exception) {
            lastError =
                    exception.getMessage() == null
                            ? exception.getClass()
                            .getSimpleName()
                            : exception.getMessage();

            return false;
        }

        line =
                selected.line();

        running.set(
                true
        );

        line.start();

        thread =
                new Thread(
                        () ->
                                runCapture(
                                        selected.format(),
                                        frameConsumer
                                ),
                        "VSIA-Radio-Microphone"
                );

        thread.setDaemon(
                true
        );

        thread.start();

        lastError = "";
        return true;
    }

    public void stop() {
        running.set(
                false
        );

        TargetDataLine current =
                line;

        line = null;

        if (current != null) {
            try {
                current.stop();
            } catch (Exception ignored) {
            }

            try {
                current.flush();
            } catch (Exception ignored) {
            }

            try {
                current.close();
            } catch (Exception ignored) {
            }
        }

        Thread currentThread =
                thread;

        thread = null;

        if (currentThread != null
                && currentThread
                != Thread.currentThread()) {
            try {
                currentThread.join(
                        200L
                );
            } catch (InterruptedException exception) {
                Thread.currentThread()
                        .interrupt();
            }
        }
    }

    public boolean running() {
        return running.get();
    }

    public String lastError() {
        return lastError;
    }

    private void runCapture(
            AudioFormat format,
            Consumer<byte[]> frameConsumer
    ) {
        int sourceSamples =
                Math.max(
                        1,
                        Math.round(
                                format.getSampleRate()
                                        * FRAME_MILLIS
                                        / 1_000.0F
                        )
                );

        byte[] pcmFrame =
                new byte[
                        sourceSamples
                                * 2
                ];

        try {
            while (running.get()) {
                if (!readFully(
                        pcmFrame
                )) {
                    break;
                }

                byte[] encoded =
                        resampleProcessAndEncode(
                                pcmFrame,
                                sourceSamples
                        );

                frameConsumer.accept(
                        encoded
                );
            }
        } catch (Throwable throwable) {
            lastError =
                    throwable.getMessage() == null
                            ? throwable.getClass()
                            .getSimpleName()
                            : throwable.getMessage();
        } finally {
            running.set(
                    false
            );

            TargetDataLine current =
                    line;

            line = null;

            if (current != null) {
                try {
                    current.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private boolean readFully(
            byte[] buffer
    ) {
        TargetDataLine current =
                line;

        if (current == null) {
            return false;
        }

        int offset =
                0;

        while (offset < buffer.length
                && running.get()) {
            int read =
                    current.read(
                            buffer,
                            offset,
                            buffer.length
                                    - offset
                    );

            if (read <= 0) {
                return false;
            }

            offset +=
                    read;
        }

        return offset
                == buffer.length;
    }

    private byte[] resampleProcessAndEncode(
            byte[] pcm16Le,
            int sourceSamples
    ) {
        short[] samples =
                new short[
                        RADIO_SAMPLES_PER_FRAME
                ];

        double mean =
                0.0;

        for (int i = 0;
             i < RADIO_SAMPLES_PER_FRAME;
             i++) {
            double sourcePosition =
                    (
                            (double) i
                                    * (
                                    sourceSamples - 1
                            )
                    )
                            / Math.max(
                            1,
                            RADIO_SAMPLES_PER_FRAME - 1
                    );

            int left =
                    Math.max(
                            0,
                            Math.min(
                                    sourceSamples - 1,
                                    (int) Math.floor(
                                            sourcePosition
                                    )
                            )
                    );

            int right =
                    Math.min(
                            sourceSamples - 1,
                            left + 1
                    );

            double fraction =
                    sourcePosition
                            - left;

            short a =
                    readSample(
                            pcm16Le,
                            left
                    );

            short b =
                    readSample(
                            pcm16Le,
                            right
                    );

            double interpolated =
                    a
                            + (
                            b - a
                    )
                            * fraction;

            samples[i] =
                    (short) Math.round(
                            interpolated
                    );

            mean +=
                    samples[i];
        }

        mean /=
                samples.length;

        double sumSquares =
                0.0;

        for (int i = 0;
             i < samples.length;
             i++) {
            int dcRemoved =
                    (int) Math.round(
                            samples[i]
                                    - mean
                    );

            samples[i] =
                    (short) Math.max(
                            Short.MIN_VALUE,
                            Math.min(
                                    Short.MAX_VALUE,
                                    dcRemoved
                            )
                    );

            sumSquares +=
                    (
                            double
                            ) samples[i]
                            * samples[i];
        }

        double rms =
                Math.sqrt(
                        sumSquares
                                / Math.max(
                                1,
                                samples.length
                        )
                );

        double desiredGain;

        if (rms < NOISE_GATE_RMS) {
            desiredGain =
                    0.0;
        } else {
            desiredGain =
                    Math.max(
                            0.55,
                            Math.min(
                                    5.0,
                                    TARGET_RMS
                                            / rms
                            )
                    );
        }

        smoothedGain =
                0.80
                        * smoothedGain
                        + 0.20
                        * desiredGain;

        byte[] encoded =
                new byte[
                        samples.length
                ];

        for (int i = 0;
             i < samples.length;
             i++) {
            double scaled =
                    samples[i]
                            * smoothedGain;

            double limited =
                    Math.tanh(
                            scaled
                                    / 24_000.0
                    )
                            * 28_000.0;

            encoded[i] =
                    MuLawCodec.encode(
                            (short) Math.round(
                                    limited
                            )
                    );
        }

        return encoded;
    }

    private static short readSample(
            byte[] pcm16Le,
            int sampleIndex
    ) {
        int byteIndex =
                sampleIndex
                        * 2;

        return (short) (
                (
                        pcm16Le[byteIndex]
                                & 0xFF
                )
                        | (
                        pcm16Le[
                                byteIndex + 1
                        ]
                                << 8
                )
        );
    }

    private static CaptureLine openCaptureLine()
            throws LineUnavailableException {
        LineUnavailableException last =
                null;

        for (float rate
                : CAPTURE_RATES) {
            AudioFormat format =
                    new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            rate,
                            16,
                            1,
                            2,
                            rate,
                            false
                    );

            DataLine.Info info =
                    new DataLine.Info(
                            TargetDataLine.class,
                            format
                    );

            if (!AudioSystem.isLineSupported(
                    info
            )) {
                continue;
            }

            try {
                TargetDataLine target =
                        (TargetDataLine) AudioSystem
                                .getLine(
                                        info
                                );

                int frameBytes =
                        Math.max(
                                1,
                                Math.round(
                                        rate
                                                * FRAME_MILLIS
                                                / 1_000.0F
                                )
                        )
                                * 2;

                target.open(
                        format,
                        frameBytes
                                * 12
                );

                return new CaptureLine(
                        target,
                        format
                );
            } catch (LineUnavailableException exception) {
                last =
                        exception;
            }
        }

        if (last != null) {
            throw last;
        }

        throw new LineUnavailableException(
                "No compatible default microphone was found"
        );
    }

    private record CaptureLine(
            TargetDataLine line,
            AudioFormat format
    ) {
    }
}
