package com.k1ngtle.vsia.signality.internet.radio.voice.client;

import com.k1ngtle.vsia.signality.internet.radio.voice.MuLawCodec;
import net.minecraft.client.Minecraft;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RadioAudioPlayback {
    private static final int RADIO_SAMPLE_RATE =
            8_000;

    private static final int FRAME_MILLIS =
            20;

    private static final int START_BUFFER_FRAMES =
            3;

    private static final int MAX_BUFFERED_FRAMES =
            100;

    private static final int MAX_CONCEAL_FRAMES =
            2;

    private static final float[] OUTPUT_RATES =
            new float[]{
                    48_000.0F,
                    44_100.0F,
                    16_000.0F,
                    8_000.0F
            };

    private final Map<UUID, StreamBuffer> streams =
            new ConcurrentHashMap<>();

    private final AtomicBoolean running =
            new AtomicBoolean();

    private final Random random =
            new Random();

    private volatile SourceDataLine line;

    private volatile float outputSampleRate =
            48_000.0F;

    private volatile String lastError =
            "";

    private volatile String outputDeviceDescription =
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

        if (!running.get()
                || line == null) {
            return;
        }

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

    public String outputDeviceDescription() {
        return outputDeviceDescription;
    }

    public boolean available() {
        return running.get()
                && line != null;
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
                current.stop();
            } catch (Exception ignored) {
            }

            try {
                current.close();
            } catch (Exception ignored) {
            }
        }
    }

    private synchronized void ensureStarted() {
        if (running.get()
                && line != null) {
            return;
        }

        try {
            openLine();
        } catch (LineUnavailableException exception) {
            lastError =
                    exception.getMessage() == null
                            ? "No compatible radio audio output device"
                            : exception.getMessage();

            outputDeviceDescription =
                    "";

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
                            2L
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
                            1L
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
                            decodeAndResample(
                                    audio,
                                    outputSampleRate
                            );

                    applyGain(
                            pcm,
                            0.70
                                    + 0.30
                                    * frame.intelligibility()
                    );

                    writeFully(
                            current,
                            pcm
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
        String preferredMinecraftDevice =
                minecraftSoundDevice();

        List<Mixer.Info> mixers =
                sortedMixers(
                        preferredMinecraftDevice
                );

        List<String> failures =
                new ArrayList<>();

        if (!preferredMinecraftDevice.isBlank()) {
            for (Mixer.Info info
                    : mixers) {
                if (deviceMatchScore(
                        preferredMinecraftDevice,
                        info.getName()
                ) <= 0) {
                    continue;
                }

                if (tryOpenMixer(
                        info,
                        failures
                )) {
                    return;
                }
            }
        }

        if (tryOpenSystemDefault(
                failures
        )) {
            return;
        }

        for (Mixer.Info info
                : mixers) {
            if (tryOpenMixer(
                    info,
                    failures
            )) {
                return;
            }
        }

        throw new LineUnavailableException(
                failures.isEmpty()
                        ? "No Java Sound output line supports PCM mono audio"
                        : "No usable output device. "
                        + String.join(
                        " | ",
                        failures
                )
        );
    }

    private boolean tryOpenSystemDefault(
            List<String> failures
    ) {
        for (float rate
                : OUTPUT_RATES) {
            AudioFormat format =
                    format(
                            rate
                    );

            try {
                SourceDataLine output =
                        AudioSystem.getSourceDataLine(
                                format
                        );

                openAndStart(
                        output,
                        format,
                        "System default"
                );

                return true;
            } catch (Exception exception) {
                failures.add(
                        "default "
                                + Math.round(
                                rate
                        )
                                + " Hz: "
                                + shortMessage(
                                exception
                        )
                );
            }
        }

        return false;
    }

    private boolean tryOpenMixer(
            Mixer.Info info,
            List<String> failures
    ) {
        Mixer mixer =
                AudioSystem.getMixer(
                        info
                );

        for (float rate
                : OUTPUT_RATES) {
            AudioFormat format =
                    format(
                            rate
                    );

            DataLine.Info lineInfo =
                    new DataLine.Info(
                            SourceDataLine.class,
                            format
                    );

            if (!mixer.isLineSupported(
                    lineInfo
            )) {
                continue;
            }

            try {
                SourceDataLine output =
                        (SourceDataLine) mixer
                                .getLine(
                                        lineInfo
                                );

                openAndStart(
                        output,
                        format,
                        info.getName()
                );

                return true;
            } catch (Exception exception) {
                failures.add(
                        info.getName()
                                + " "
                                + Math.round(
                                rate
                        )
                                + " Hz: "
                                + shortMessage(
                                exception
                        )
                );
            }
        }

        return false;
    }

    private void openAndStart(
            SourceDataLine output,
            AudioFormat format,
            String deviceName
    ) throws LineUnavailableException {
        int bufferBytes =
                Math.max(
                        4_096,
                        (int) (
                                format.getSampleRate()
                                        * format.getFrameSize()
                                        * 0.25
                        )
                );

        output.open(
                format,
                bufferBytes
        );

        output.start();

        line =
                output;

        outputSampleRate =
                format.getSampleRate();

        outputDeviceDescription =
                deviceName
                        + " @ "
                        + Math.round(
                        outputSampleRate
                )
                        + " Hz";

        lastError =
                "";
    }

    private static List<Mixer.Info> sortedMixers(
            String preferred
    ) {
        List<Mixer.Info> result =
                new ArrayList<>(
                        List.of(
                                AudioSystem.getMixerInfo()
                        )
                );

        result.sort(
                Comparator
                        .comparingInt(
                                (Mixer.Info info) ->
                                        deviceMatchScore(
                                                preferred,
                                                info.getName()
                                        )
                        )
                        .reversed()
                        .thenComparing(
                                Mixer.Info::getName,
                                String.CASE_INSENSITIVE_ORDER
                        )
        );

        return result;
    }

    private static int deviceMatchScore(
            String preferred,
            String candidate
    ) {
        if (preferred == null
                || preferred.isBlank()
                || candidate == null
                || candidate.isBlank()) {
            return 0;
        }

        String a =
                normalizeDeviceName(
                        preferred
                );

        String b =
                normalizeDeviceName(
                        candidate
                );

        if (a.equals(
                b
        )) {
            return 100;
        }

        if (a.contains(
                b
        )
                || b.contains(
                a
        )) {
            return 80;
        }

        int score =
                0;

        for (String token
                : a.split(
                " "
        )) {
            if (token.length() < 3) {
                continue;
            }

            if (b.contains(
                    token
            )) {
                score +=
                        10;
            }
        }

        return score;
    }

    private static String normalizeDeviceName(
            String value
    ) {
        return value
                .toLowerCase(
                        Locale.ROOT
                )
                .replace(
                        "openal soft on ",
                        ""
                )
                .replaceAll(
                        "[^a-z0-9]+",
                        " "
                )
                .trim();
    }

    private static String minecraftSoundDevice() {
        try {
            Minecraft minecraft =
                    Minecraft.getInstance();

            if (minecraft == null
                    || minecraft.options == null) {
                return "";
            }

            String selected =
                    minecraft.options
                            .soundDevice()
                            .get();

            return selected == null
                    ? ""
                    : selected.trim();
        } catch (Throwable ignored) {
            return "";
        }
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

    private static byte[] decodeAndResample(
            byte[] muLaw,
            float targetRate
    ) {
        if (muLaw == null
                || muLaw.length == 0) {
            return new byte[0];
        }

        short[] source =
                new short[
                        muLaw.length
                ];

        for (int i = 0;
             i < muLaw.length;
             i++) {
            source[i] =
                    MuLawCodec.decode(
                            muLaw[i]
                    );
        }

        if (Math.abs(
                targetRate
                        - RADIO_SAMPLE_RATE
        ) < 1.0F) {
            byte[] pcm =
                    new byte[
                            source.length
                                    * 2
                    ];

            for (int i = 0;
                 i < source.length;
                 i++) {
                writeSample(
                        pcm,
                        i,
                        source[i]
                );
            }

            return pcm;
        }

        int targetSamples =
                Math.max(
                        1,
                        Math.round(
                                source.length
                                        * targetRate
                                        / RADIO_SAMPLE_RATE
                        )
                );

        byte[] pcm =
                new byte[
                        targetSamples
                                * 2
                ];

        if (source.length == 1) {
            for (int i = 0;
                 i < targetSamples;
                 i++) {
                writeSample(
                        pcm,
                        i,
                        source[0]
                );
            }

            return pcm;
        }

        for (int i = 0;
             i < targetSamples;
             i++) {
            double sourcePosition =
                    (
                            (double) i
                                    * (
                                    source.length - 1
                            )
                    )
                            / Math.max(
                            1,
                            targetSamples - 1
                    );

            int left =
                    Math.max(
                            0,
                            Math.min(
                                    source.length - 1,
                                    (int) Math.floor(
                                            sourcePosition
                                    )
                            )
                    );

            int right =
                    Math.min(
                            source.length - 1,
                            left + 1
                    );

            double fraction =
                    sourcePosition
                            - left;

            double value =
                    source[left]
                            + (
                            source[right]
                                    - source[left]
                    )
                            * fraction;

            writeSample(
                    pcm,
                    i,
                    (short) Math.round(
                            value
                    )
            );
        }

        return pcm;
    }

    private static void writeSample(
            byte[] pcm,
            int sampleIndex,
            short sample
    ) {
        int index =
                sampleIndex
                        * 2;

        pcm[index] =
                (byte) (
                        sample
                                & 0xFF
                );

        pcm[index + 1] =
                (byte) (
                        (
                                sample
                                        >>> 8
                        )
                                & 0xFF
                );
    }

    private void writeSilence(
            SourceDataLine output
    ) {
        int samples =
                Math.max(
                        1,
                        Math.round(
                                outputSampleRate
                                        * FRAME_MILLIS
                                        / 1_000.0F
                        )
                );

        writeFully(
                output,
                new byte[
                        samples
                                * 2
                ]
        );
    }

    private void playSquelchTail(
            SourceDataLine output,
            double intelligibility
    ) {
        int samples =
                Math.max(
                        1,
                        Math.round(
                                outputSampleRate
                                        * FRAME_MILLIS
                                        / 1_000.0F
                        )
                );

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

            writeSample(
                    noise,
                    i,
                    sample
            );
        }

        writeFully(
                output,
                noise
        );
    }

    private static void writeFully(
            SourceDataLine output,
            byte[] data
    ) {
        int offset =
                0;

        while (offset < data.length) {
            int written =
                    output.write(
                            data,
                            offset,
                            data.length
                                    - offset
                    );

            if (written <= 0) {
                break;
            }

            offset +=
                    written;
        }
    }

    private static byte[] attenuateMuLaw(
            byte[] muLaw,
            double gain
    ) {
        if (muLaw == null
                || muLaw.length == 0) {
            return new byte[0];
        }

        byte[] result =
                new byte[
                        muLaw.length
                ];

        for (int i = 0;
             i < muLaw.length;
             i++) {
            short sample =
                    MuLawCodec.decode(
                            muLaw[i]
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

            result[i] =
                    MuLawCodec.encode(
                            (short) scaled
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

    private static String shortMessage(
            Exception exception
    ) {
        String message =
                exception.getMessage();

        if (message == null
                || message.isBlank()) {
            return exception.getClass()
                    .getSimpleName();
        }

        return message;
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

            while (frames.size()
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
