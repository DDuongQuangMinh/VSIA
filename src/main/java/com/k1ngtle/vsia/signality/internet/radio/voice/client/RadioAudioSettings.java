package com.k1ngtle.vsia.signality.internet.radio.voice.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RadioAudioSettings {
    public static final String OUTPUT_FOLLOW_MINECRAFT =
            "__minecraft__";

    public static final String OUTPUT_SYSTEM_DEFAULT =
            "__default__";

    public static final String INPUT_SYSTEM_DEFAULT =
            "__default__";

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private static final Path CONFIG_PATH =
            FMLPaths.CONFIGDIR
                    .get()
                    .resolve(
                            "vsia-radio-audio.json"
                    );

    private static String outputDevice =
            OUTPUT_FOLLOW_MINECRAFT;

    private static String inputDevice =
            INPUT_SYSTEM_DEFAULT;

    private static double outputVolume =
            1.0;

    private static boolean loaded;

    private RadioAudioSettings() {
    }

    public static synchronized String outputDevice() {
        ensureLoaded();
        return outputDevice;
    }

    public static synchronized String inputDevice() {
        ensureLoaded();
        return inputDevice;
    }

    public static synchronized double outputVolume() {
        ensureLoaded();
        return outputVolume;
    }

    public static synchronized void setOutputDevice(
            String value
    ) {
        ensureLoaded();

        outputDevice =
                normalize(
                        value,
                        OUTPUT_FOLLOW_MINECRAFT
                );
    }

    public static synchronized void setInputDevice(
            String value
    ) {
        ensureLoaded();

        inputDevice =
                normalize(
                        value,
                        INPUT_SYSTEM_DEFAULT
                );
    }

    public static synchronized void setOutputVolume(
            double value
    ) {
        ensureLoaded();

        outputVolume =
                Math.max(
                        0.0,
                        Math.min(
                                2.0,
                                value
                        )
                );
    }

    public static synchronized void save() {
        ensureLoaded();

        try {
            Files.createDirectories(
                    CONFIG_PATH.getParent()
            );

            try (Writer writer =
                         Files.newBufferedWriter(
                                 CONFIG_PATH
                         )) {
                GSON.toJson(
                        new Data(
                                outputDevice,
                                inputDevice,
                                outputVolume
                        ),
                        writer
                );
            }
        } catch (Exception ignored) {
        }
    }

    public static synchronized void reset() {
        outputDevice =
                OUTPUT_FOLLOW_MINECRAFT;

        inputDevice =
                INPUT_SYSTEM_DEFAULT;

        outputVolume =
                1.0;

        loaded =
                true;

        save();
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        loaded =
                true;

        if (!Files.isRegularFile(
                CONFIG_PATH
        )) {
            return;
        }

        try (Reader reader =
                     Files.newBufferedReader(
                             CONFIG_PATH
                     )) {
            Data data =
                    GSON.fromJson(
                            reader,
                            Data.class
                    );

            if (data == null) {
                return;
            }

            outputDevice =
                    normalize(
                            data.outputDevice(),
                            OUTPUT_FOLLOW_MINECRAFT
                    );

            inputDevice =
                    normalize(
                            data.inputDevice(),
                            INPUT_SYSTEM_DEFAULT
                    );

            outputVolume =
                    Math.max(
                            0.0,
                            Math.min(
                                    2.0,
                                    data.outputVolume()
                            )
                    );
        } catch (Exception ignored) {
            outputDevice =
                    OUTPUT_FOLLOW_MINECRAFT;

            inputDevice =
                    INPUT_SYSTEM_DEFAULT;

            outputVolume =
                    1.0;
        }
    }

    private static String normalize(
            String value,
            String fallback
    ) {
        if (value == null
                || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

    private record Data(
            String outputDevice,
            String inputDevice,
            double outputVolume
    ) {
    }
}
