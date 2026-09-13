package com.k1ngtle.vsia.signality.internet.radio.voice.client;

import com.k1ngtle.vsia.signality.internet.radio.voice.MuLawCodec;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RadioAudioPlayback {
    private static final int FRAME_MILLIS =
            20;

    private static final int START_BUFFER_FRAMES =
            3;

    private static final int MAX_BUFFERED_FRAMES =
            50;

    private static final int MAX_CONCEAL_FRAMES =
            2;

    private final Map<UUID, StreamBuffer> streams =
            new ConcurrentHashMap<>();

    private final AtomicBoolean running =
            new AtomicBoolean();

    private final Random random =
            new Random();

    private volatile SourceDataLine line;
    private volatile int outputRepeat =
            1;

    private volatile String lastError =
            "";

    public void enqueue(
            UUID sourceRadioId,
            int sequenceNumber,
            byte[] muLaw,
            boolean endOfTransmission,
            double snrDb,
            double intelligibility,
            String emission
    ) {
        if (sourceRadioId == null) {
            return;
        }

        ensureStarted();

        streams
                .computeIfAbsent(
                        sourceRadioId,
                        ignored ->
                                new StreamBuffer()
                )
                .offer(
                        new Frame(
                                sequenceNumber,
                                muLaw == null
                                        ? new byte[0]
                                        : muLaw.clone(),
                                endOfTransmission,
                                snrDb,
                                clamp01(
                                        intelligibility
                                ),
                                emission == null
                                        ? ""
                                        : emission
                        )
                );
    }

    public String lastError() {
        return lastError;
    }

    public void stop() {
        running.set(
                false
        );

        streams.clear();

        SourceDataLine current =
                line;

        line = null;

        if (current != null) {
            try {
                current.flush();
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
                UUID selectedId =
                        selectReadyStream();

                if (selectedId == null) {
                    Thread.sleep(
                            4L
                    );

                    continue;
                }

                StreamBuffer buffer =
                        streams.get(
                                selectedId
                        );

                if (buffer == null) {
                    continue;
                }

                PlayResult result =
                        buffer.next();

                if (result == null) {
                    Thread.sleep(
                            2L
                    );

                    continue;
                }

                if (result.removeStream()) {
                    streams.remove(
                            selectedId,
                            buffer
                    );

                    continue;
                }

                SourceDataLine current =
                        line;

                if (current == null) {
                    break;
                }

                Frame frame =
                        result.frame();

                if (frame == null) {
                    continue;
                }

                byte[] audio =
                        frame.audio();

                if (result.concealed()) {
                    audio =
                            attenuateMuLaw(
                                    audio,
                                    0.62
                            );
                }

                if (audio.length > 0) {
                    byte[] pcm =
                            MuLawCodec.decodePcm16Le(
                                    audio,
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
                } else if (!frame.endOfTransmission()) {
                    writeSilence(
                            current
                    );
                }

                if (frame.endOfTransmission()) {
                    if (!"DIGITAL"
                            .equalsIgnoreCase(
                                    frame.emission()
                            )) {
                        playSquelchTail(
                                current,
                                frame.intelligibility()
                        );
                    }

                    streams.remove(
                            selectedId,
                            buffer
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

    private UUID selectReadyStream() {
        UUID best =
                null;

        double bestSnr =
                Double.NEGATIVE_INFINITY;

        for (Map.Entry<UUID, StreamBuffer> entry
                : streams.entrySet()) {
            StreamBuffer buffer =
                    entry.getValue();

            if (!buffer.ready()) {
                continue;
            }

            double snr =
                    buffer.peekSnr();

            if (best == null
                    || snr > bestSnr) {
                best =
                        entry.getKey();

                bestSnr =
                        snr;
            }
        }

        return best;
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
                                * 0.18
                )
        );

        output.start();

        line =
                output;

        outputRepeat =
                repeat;

        lastError =
                "";
    }

    private static AudioFormat format(
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

    private void writeSilence(
            SourceDataLine output
    ) {
        byte[] silence =
                new byte[
                        8_000
                                * FRAME_MILLIS
                                / 1_000
                                * 2
                                * outputRepeat
                ];

        output.write(
                silence,
                0,
                silence.length
        );
    }

    private void playSquelchTail(
            SourceDataLine output,
            double intelligibility
    ) {
        int samples =
                160
                        * outputRepeat;

        byte[] noise =
                new byte[
                        samples
                                * 2
                ];

        double amplitude =
                700.0
                        + 1_600.0
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

    private static byte[] attenuateMuLaw(
            byte[] muLaw,
            double gain
    ) {
        if (muLaw == null
                || muLaw.length == 0) {
            return new byte[0];
        }

        byte[] pcm =
                MuLawCodec.decodePcm16Le(
                        muLaw,
                        1
                );

        applyGain(
                pcm,
                gain
        );

        byte[] result =
                new byte[
                        pcm.length / 2
                ];

        for (int i = 0;
             i < result.length;
             i++) {
            short sample =
                    (short) (
                            (
                                    pcm[i * 2]
                                            & 0xFF
                            )
                                    | (
                                    pcm[i * 2 + 1]
                                            << 8
                            )
                    );

            result[i] =
                    MuLawCodec.encode(
                            sample
                    );
        }

        return result;
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

    private static final class StreamBuffer {
        private final TreeMap<Integer, Frame> frames =
                new TreeMap<>();

        private int expectedSequence =
                Integer.MIN_VALUE;

        private boolean started;
        private int endSequence =
                Integer.MIN_VALUE;

        private Frame lastGood;
        private int concealCount;

        synchronized void offer(
                Frame frame
        ) {
            if (frame == null) {
                return;
            }

            if (frames.size()
                    >= MAX_BUFFERED_FRAMES) {
                frames.pollFirstEntry();
            }

            frames.putIfAbsent(
                    frame.sequenceNumber(),
                    frame
            );

            if (frame.endOfTransmission()) {
                endSequence =
                        frame.sequenceNumber();
            }

            if (expectedSequence
                    == Integer.MIN_VALUE) {
                expectedSequence =
                        frame.sequenceNumber();
            }
        }

        synchronized boolean ready() {
            if (frames.isEmpty()) {
                return false;
            }

            if (started) {
                return true;
            }

            return frames.size()
                    >= START_BUFFER_FRAMES
                    || endSequence
                    != Integer.MIN_VALUE;
        }

        synchronized double peekSnr() {
            Frame frame =
                    frames.isEmpty()
                            ? lastGood
                            : frames.firstEntry()
                            .getValue();

            return frame == null
                    ? Double.NEGATIVE_INFINITY
                    : frame.snrDb();
        }

        synchronized PlayResult next() {
            if (!ready()) {
                return null;
            }

            if (!started) {
                started =
                        true;

                if (!frames.isEmpty()) {
                    expectedSequence =
                            frames.firstKey();
                }
            }

            Frame frame =
                    frames.remove(
                            expectedSequence
                    );

            if (frame != null) {
                expectedSequence++;
                concealCount =
                        0;

                if (!frame.endOfTransmission()
                        && frame.audio().length > 0) {
                    lastGood =
                            frame;
                }

                return new PlayResult(
                        frame,
                        false,
                        false
                );
            }

            if (endSequence
                    != Integer.MIN_VALUE
                    && expectedSequence
                    > endSequence) {
                return new PlayResult(
                        null,
                        false,
                        true
                );
            }

            if (!frames.isEmpty()
                    && frames.firstKey()
                    > expectedSequence) {
                expectedSequence++;

                if (lastGood != null
                        && concealCount
                        < MAX_CONCEAL_FRAMES) {
                    concealCount++;

                    return new PlayResult(
                            new Frame(
                                    expectedSequence - 1,
                                    lastGood.audio(),
                                    false,
                                    lastGood.snrDb(),
                                    lastGood.intelligibility(),
                                    lastGood.emission()
                            ),
                            true,
                            false
                    );
                }

                return new PlayResult(
                        new Frame(
                                expectedSequence - 1,
                                new byte[0],
                                false,
                                Double.NEGATIVE_INFINITY,
                                0.0,
                                ""
                        ),
                        false,
                        false
                );
            }

            return null;
        }
    }

    private record PlayResult(
            Frame frame,
            boolean concealed,
            boolean removeStream
    ) {
    }

    private record Frame(
            int sequenceNumber,
            byte[] audio,
            boolean endOfTransmission,
            double snrDb,
            double intelligibility,
            String emission
    ) {
    }
}
