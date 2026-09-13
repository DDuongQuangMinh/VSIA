package com.k1ngtle.vsia.signality.internet.radio.voice.client;

import com.k1ngtle.vsia.signality.internet.radio.voice.MuLawCodec;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RadioAudioPlayback {
    private static final int RADIO_SAMPLE_RATE =
            8_000;

    private static final int MAX_QUEUE =
            12;

    private final LinkedBlockingDeque<Frame> queue =
            new LinkedBlockingDeque<>(
                    MAX_QUEUE
            );

    private final AtomicBoolean running =
            new AtomicBoolean();

    private final Random random =
            new Random();

    private volatile SourceDataLine line;
    private volatile int outputRepeat = 1;
    private volatile String lastError = "";

    public void enqueue(
            byte[] muLaw,
            boolean endOfTransmission,
            double intelligibility,
            String emission
    ) {
        ensureStarted();

        Frame frame =
                new Frame(
                        muLaw == null
                                ? new byte[0]
                                : muLaw.clone(),
                        endOfTransmission,
                        clamp01(
                                intelligibility
                        ),
                        emission == null
                                ? ""
                                : emission
                );

        if (!queue.offerLast(
                frame
        )) {
            queue.pollFirst();
            queue.offerLast(
                    frame
            );
        }
    }

    public String lastError() {
        return lastError;
    }

    public void stop() {
        running.set(
                false
        );

        queue.clear();

        SourceDataLine current =
                line;

        line = null;

        if (current != null) {
            try {
                current.drain();
            } catch (Exception ignored) {
            }

            try {
                current.close();
            } catch (Exception ignored) {
            }
        }
    }

    private synchronized void ensureStarted() {
        if (running.get()) {
            return;
        }

        try {
            openLine();
        } catch (LineUnavailableException exception) {
            lastError =
                    exception.getMessage() == null
                            ? "No compatible output device"
                            : exception.getMessage();

            return;
        }

        running.set(
                true
        );

        Thread thread =
                new Thread(
                        this::playbackLoop,
                        "VSIA-Radio-Playback"
                );

        thread.setDaemon(
                true
        );

        thread.start();
    }

    private void playbackLoop() {
        try {
            while (running.get()) {
                Frame frame =
                        queue.takeFirst();

                SourceDataLine current =
                        line;

                if (current == null) {
                    break;
                }

                if (frame.audio().length > 0) {
                    byte[] pcm =
                            MuLawCodec.decodePcm16Le(
                                    frame.audio(),
                                    outputRepeat
                            );

                    applyGain(
                            pcm,
                            0.55
                                    + 0.45
                                    * frame.intelligibility()
                    );

                    current.write(
                            pcm,
                            0,
                            pcm.length
                    );
                }

                if (frame.endOfTransmission()
                        && !"DIGITAL"
                        .equalsIgnoreCase(
                                frame.emission()
                        )) {
                    playSquelchTail(
                            current,
                            frame.intelligibility()
                    );
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread()
                    .interrupt();
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

            SourceDataLine current =
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

    private void openLine()
            throws LineUnavailableException {
        AudioFormat eightKhz =
                format(
                        8_000.0F
                );

        if (AudioSystem.isLineSupported(
                new DataLine.Info(
                        SourceDataLine.class,
                        eightKhz
                )
        )) {
            open(
                    eightKhz,
                    1
            );

            return;
        }

        AudioFormat sixteenKhz =
                format(
                        16_000.0F
                );

        open(
                sixteenKhz,
                2
        );
    }

    private void open(
            AudioFormat format,
            int repeat
    ) throws LineUnavailableException {
        DataLine.Info info =
                new DataLine.Info(
                        SourceDataLine.class,
                        format
                );

        SourceDataLine output =
                (SourceDataLine) AudioSystem
                        .getLine(
                                info
                        );

        output.open(
                format,
                (int) (
                        format.getSampleRate()
                                * format.getFrameSize()
                                * 0.25
                )
        );

        output.start();

        line =
                output;

        outputRepeat =
                repeat;

        lastError = "";
    }

    private AudioFormat format(
            float rate
    ) {
        return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                rate,
                16,
                1,
                2,
                rate,
                false
        );
    }

    private void playSquelchTail(
            SourceDataLine output,
            double intelligibility
    ) {
        int samples =
                240
                        * outputRepeat;

        byte[] noise =
                new byte[
                        samples
                                * 2
                ];

        double amplitude =
                900.0
                        + 1_800.0
                        * (
                        1.0
                                - intelligibility
                );

        for (int i = 0;
             i < samples;
             i++) {
            short sample =
                    (short) (
                            (
                                    random.nextDouble()
                                            * 2.0
                                            - 1.0
                            )
                                    * amplitude
                    );

            noise[i * 2] =
                    (byte) (
                            sample
                                    & 0xFF
                    );

            noise[i * 2 + 1] =
                    (byte) (
                            (
                                    sample
                                            >>> 8
                            )
                                    & 0xFF
                    );
        }

        output.write(
                noise,
                0,
                noise.length
        );
    }

    private static void applyGain(
            byte[] pcm16Le,
            double gain
    ) {
        for (int i = 0;
             i + 1 < pcm16Le.length;
             i += 2) {
            short sample =
                    (short) (
                            (
                                    pcm16Le[i]
                                            & 0xFF
                            )
                                    | (
                                    pcm16Le[i + 1]
                                            << 8
                            )
                    );

            int scaled =
                    (int) Math.round(
                            sample
                                    * gain
                    );

            scaled =
                    Math.max(
                            Short.MIN_VALUE,
                            Math.min(
                                    Short.MAX_VALUE,
                                    scaled
                            )
                    );

            pcm16Le[i] =
                    (byte) (
                            scaled
                                    & 0xFF
                    );

            pcm16Le[i + 1] =
                    (byte) (
                            (
                                    scaled
                                            >>> 8
                            )
                                    & 0xFF
                    );
        }
    }

    private static double clamp01(
            double value
    ) {
        if (!Double.isFinite(
                value
        )) {
            return 0.0;
        }

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }

    private record Frame(
            byte[] audio,
            boolean endOfTransmission,
            double intelligibility,
            String emission
    ) {
    }
}
