package com.k1ngtle.vsia.signality.internet.radio.voice.client;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RadioAudioDevices {
    private static final float[] OUTPUT_RATES =
            new float[]{
                    48_000.0F,
                    44_100.0F,
                    16_000.0F,
                    8_000.0F
            };

    private static final float[] INPUT_RATES =
            new float[]{
                    48_000.0F,
                    44_100.0F,
                    16_000.0F
            };

    private RadioAudioDevices() {
    }

    public static List<DeviceOption> outputOptions() {
        List<DeviceOption> result =
                new ArrayList<>();

        result.add(
                new DeviceOption(
                        RadioAudioSettings
                                .OUTPUT_FOLLOW_MINECRAFT,
                        "Follow Minecraft sound device",
                        "Preferred"
                )
        );

        result.add(
                new DeviceOption(
                        RadioAudioSettings
                                .OUTPUT_SYSTEM_DEFAULT,
                        "System default output",
                        "Fallback"
                )
        );

        Set<String> names =
                new LinkedHashSet<>();

        for (Mixer.Info info
                : AudioSystem.getMixerInfo()) {
            Mixer mixer =
                    AudioSystem.getMixer(
                            info
                    );

            if (!supportsOutput(
                    mixer
            )) {
                continue;
            }

            if (names.add(
                    info.getName()
            )) {
                result.add(
                        new DeviceOption(
                                info.getName(),
                                info.getName(),
                                detail(
                                        info
                                )
                        )
                );
            }
        }

        return List.copyOf(
                result
        );
    }

    public static List<DeviceOption> inputOptions() {
        List<DeviceOption> result =
                new ArrayList<>();

        result.add(
                new DeviceOption(
                        RadioAudioSettings
                                .INPUT_SYSTEM_DEFAULT,
                        "System default microphone",
                        "Default"
                )
        );

        Set<String> names =
                new LinkedHashSet<>();

        for (Mixer.Info info
                : AudioSystem.getMixerInfo()) {
            Mixer mixer =
                    AudioSystem.getMixer(
                            info
                    );

            if (!supportsInput(
                    mixer
            )) {
                continue;
            }

            if (names.add(
                    info.getName()
            )) {
                result.add(
                        new DeviceOption(
                                info.getName(),
                                info.getName(),
                                detail(
                                        info
                                )
                        )
                );
            }
        }

        return List.copyOf(
                result
        );
    }

    public static Mixer findMixerByName(
            String name
    ) {
        if (name == null
                || name.isBlank()) {
            return null;
        }

        for (Mixer.Info info
                : AudioSystem.getMixerInfo()) {
            if (info.getName()
                    .equals(
                            name
                    )) {
                return AudioSystem.getMixer(
                        info
                );
            }
        }

        return null;
    }

    public static boolean supportsOutput(
            Mixer mixer
    ) {
        if (mixer == null) {
            return false;
        }

        for (float rate
                : OUTPUT_RATES) {
            if (mixer.isLineSupported(
                    new DataLine.Info(
                            SourceDataLine.class,
                            outputFormat(
                                    rate
                            )
                    )
            )) {
                return true;
            }
        }

        return false;
    }

    public static boolean supportsInput(
            Mixer mixer
    ) {
        if (mixer == null) {
            return false;
        }

        for (float rate
                : INPUT_RATES) {
            if (mixer.isLineSupported(
                    new DataLine.Info(
                            TargetDataLine.class,
                            inputFormat(
                                    rate
                            )
                    )
            )) {
                return true;
            }
        }

        return false;
    }

    public static AudioFormat outputFormat(
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

    public static AudioFormat inputFormat(
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

    private static String detail(
            Mixer.Info info
    ) {
        String vendor =
                info.getVendor() == null
                        ? ""
                        : info.getVendor()
                        .trim();

        String description =
                info.getDescription() == null
                        ? ""
                        : info.getDescription()
                        .trim();

        if (!vendor.isBlank()
                && !description.isBlank()) {
            return vendor
                    + " · "
                    + description;
        }

        if (!description.isBlank()) {
            return description;
        }

        return vendor;
    }

    public record DeviceOption(
            String id,
            String label,
            String detail
    ) {
        public DeviceOption {
            id =
                    id == null
                            ? ""
                            : id;

            label =
                    label == null
                            ? id
                            : label;

            detail =
                    detail == null
                            ? ""
                            : detail;
        }
    }
}
