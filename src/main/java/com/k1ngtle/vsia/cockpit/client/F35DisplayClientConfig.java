package com.k1ngtle.vsia.cockpit.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.minecraftforge.fml.loading.FMLPaths;

public final class F35DisplayClientConfig {
    private static final Path CONFIG_PATH =
            FMLPaths.CONFIGDIR
                    .get()
                    .resolve(
                            "vsia"
                    )
                    .resolve(
                            "f35_display.properties"
                    );

    private static boolean loaded =
            false;

    private static int brightnessPercent =
            100;

    private static int displayScalePercent =
            100;

    private static int radarRangeIndex =
            -1;

    private static boolean trackLabels =
            true;

    private static boolean detectMobs =
            true;

    private static boolean detectPlayers =
            true;

    private static boolean detectShips =
            true;

    private F35DisplayClientConfig() {
    }

    public static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }

        loaded =
                true;

        if (!Files.exists(
                CONFIG_PATH
        )) {
            save();
            return;
        }

        Properties properties =
                new Properties();

        try (Reader reader =
                     Files.newBufferedReader(
                             CONFIG_PATH
                     )) {
            properties.load(
                    reader
            );

            brightnessPercent =
                    clamp(
                            parseInt(
                                    properties.getProperty(
                                            "brightnessPercent"
                                    ),
                                    100
                            ),
                            25,
                            100
                    );

            displayScalePercent =
                    clamp(
                            parseInt(
                                    properties.getProperty(
                                            "displayScalePercent"
                                    ),
                                    100
                            ),
                            80,
                            100
                    );

            radarRangeIndex =
                    clamp(
                            parseInt(
                                    properties.getProperty(
                                            "radarRangeIndex"
                                    ),
                                    -1
                            ),
                            -1,
                            6
                    );

            trackLabels =
                    Boolean.parseBoolean(
                            properties.getProperty(
                                    "trackLabels",
                                    "true"
                            )
                    );

            detectMobs =
                    Boolean.parseBoolean(
                            properties.getProperty(
                                    "detectMobs",
                                    "true"
                            )
                    );

            detectPlayers =
                    Boolean.parseBoolean(
                            properties.getProperty(
                                    "detectPlayers",
                                    "true"
                            )
                    );

            detectShips =
                    Boolean.parseBoolean(
                            properties.getProperty(
                                    "detectShips",
                                    "true"
                            )
                    );
        } catch (IOException ignored) {
        }
    }

    public static synchronized void save() {
        ensureParentDirectory();

        Properties properties =
                new Properties();

        properties.setProperty(
                "brightnessPercent",
                String.valueOf(
                        brightnessPercent
                )
        );

        properties.setProperty(
                "displayScalePercent",
                String.valueOf(
                        displayScalePercent
                )
        );

        properties.setProperty(
                "radarRangeIndex",
                String.valueOf(
                        radarRangeIndex
                )
        );

        properties.setProperty(
                "trackLabels",
                String.valueOf(
                        trackLabels
                )
        );

        properties.setProperty(
                "detectMobs",
                String.valueOf(
                        detectMobs
                )
        );

        properties.setProperty(
                "detectPlayers",
                String.valueOf(
                        detectPlayers
                )
        );

        properties.setProperty(
                "detectShips",
                String.valueOf(
                        detectShips
                )
        );

        try (Writer writer =
                     Files.newBufferedWriter(
                             CONFIG_PATH
                     )) {
            properties.store(
                    writer,
                    "VSIA F-35 display client configuration"
            );
        } catch (IOException ignored) {
        }
    }

    public static int brightnessPercent() {
        ensureLoaded();
        return brightnessPercent;
    }

    public static void cycleBrightness() {
        ensureLoaded();

        brightnessPercent =
                switch (brightnessPercent) {
                    case 100 -> 85;
                    case 85 -> 70;
                    case 70 -> 55;
                    case 55 -> 40;
                    default -> 100;
                };

        save();
    }

    public static int displayScalePercent() {
        ensureLoaded();
        return displayScalePercent;
    }

    public static float displayScale() {
        return displayScalePercent()
                / 100.0F;
    }

    public static void cycleDisplayScale() {
        ensureLoaded();

        displayScalePercent =
                switch (displayScalePercent) {
                    case 100 -> 95;
                    case 95 -> 90;
                    case 90 -> 85;
                    case 85 -> 80;
                    default -> 100;
                };

        save();
    }

    public static boolean trackLabels() {
        ensureLoaded();
        return trackLabels;
    }

    public static void toggleTrackLabels() {
        ensureLoaded();
        trackLabels =
                !trackLabels;
        save();
    }

    public static boolean detectMobs() {
        ensureLoaded();
        return detectMobs;
    }

    public static void toggleDetectMobs() {
        ensureLoaded();
        detectMobs =
                !detectMobs;
        save();
    }

    public static boolean detectPlayers() {
        ensureLoaded();
        return detectPlayers;
    }

    public static void toggleDetectPlayers() {
        ensureLoaded();
        detectPlayers =
                !detectPlayers;
        save();
    }

    public static boolean detectShips() {
        ensureLoaded();
        return detectShips;
    }

    public static void toggleDetectShips() {
        ensureLoaded();
        detectShips =
                !detectShips;
        save();
    }

    public static void setAllDetection(
            boolean enabled
    ) {
        ensureLoaded();
        detectMobs =
                enabled;
        detectPlayers =
                enabled;
        detectShips =
                enabled;
        save();
    }

    public static String detectionSummary() {
        ensureLoaded();

        StringBuilder builder =
                new StringBuilder();

        if (detectMobs) {
            builder.append(
                    "MOB"
            );
        }

        if (detectPlayers) {
            if (!builder.isEmpty()) {
                builder.append(
                        '+'
                );
            }
            builder.append(
                    "PLY"
            );
        }

        if (detectShips) {
            if (!builder.isEmpty()) {
                builder.append(
                        '+'
                );
            }
            builder.append(
                    "SHIP"
            );
        }

        return builder.isEmpty()
                ? "NONE"
                : builder.toString();
    }

    public static int radarRangeIndex() {
        ensureLoaded();
        return radarRangeIndex;
    }

    public static void cycleRadarRange() {
        ensureLoaded();

        radarRangeIndex++;

        if (radarRangeIndex > 6) {
            radarRangeIndex =
                    -1;
        }

        save();
    }

    public static double forcedRadarRangeMeters() {
        return switch (radarRangeIndex()) {
            case 0 -> 500.0;
            case 1 -> 1000.0;
            case 2 -> 5000.0;
            case 3 -> 10_000.0;
            case 4 -> 20_000.0;
            case 5 -> 40_000.0;
            case 6 -> 80_000.0;
            default -> -1.0;
        };
    }

    public static String radarRangeLabel() {
        return switch (radarRangeIndex()) {
            case 0 -> "500 m";
            case 1 -> "1 km";
            case 2 -> "5 km";
            case 3 -> "10 km";
            case 4 -> "20 km";
            case 5 -> "40 km";
            case 6 -> "80 km";
            default -> "AUTO";
        };
    }

    public static int applyBrightness(
            int color
    ) {
        ensureLoaded();

        float factor =
                brightnessPercent
                        / 100.0F;

        int alpha =
                color >>> 24
                        & 0xFF;

        int red =
                color >>> 16
                        & 0xFF;

        int green =
                color >>> 8
                        & 0xFF;

        int blue =
                color
                        & 0xFF;

        red =
                clamp(
                        Math.round(
                                red
                                        * factor
                        ),
                        0,
                        255
                );

        green =
                clamp(
                        Math.round(
                                green
                                        * factor
                        ),
                        0,
                        255
                );

        blue =
                clamp(
                        Math.round(
                                blue
                                        * factor
                        ),
                        0,
                        255
                );

        return alpha
                << 24
                | red
                << 16
                | green
                << 8
                | blue;
    }

    public static synchronized void resetDefaults() {
        brightnessPercent =
                100;

        displayScalePercent =
                100;

        radarRangeIndex =
                -1;

        trackLabels =
                true;

        detectMobs =
                true;

        detectPlayers =
                true;

        detectShips =
                true;

        loaded =
                true;

        save();
    }

    private static int parseInt(
            String value,
            int fallback
    ) {
        if (value == null) {
            return fallback;
        }

        try {
            return Integer.parseInt(
                    value
            );
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int clamp(
            int value,
            int min,
            int max
    ) {
        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }

    private static void ensureParentDirectory() {
        try {
            Files.createDirectories(
                    CONFIG_PATH.getParent()
            );
        } catch (IOException ignored) {
        }
    }
}
