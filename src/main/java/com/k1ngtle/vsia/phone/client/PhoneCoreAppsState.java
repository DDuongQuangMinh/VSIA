package com.k1ngtle.vsia.phone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

public final class PhoneCoreAppsState {
    public record MailMessage(
            long id,
            String from,
            String to,
            String subject,
            String body,
            long time,
            boolean sent
    ) {
    }

    private static final List<MailMessage> MAIL =
            new ArrayList<>();

    private static boolean loaded;
    private static long nextMailId = 1L;

    private static String faceTimeTarget = "";
    private static long faceTimeStartedAt;
    private static boolean faceTimeMuted;
    private static boolean faceTimeCameraEnabled = true;

    private static int tvChannel;
    private static boolean tvPlaying = true;
    private static int tvVolume = 65;

    private static int podcastEpisode;
    private static boolean podcastPlaying;
    private static long podcastStartedAt;
    private static long podcastAccumulated;
    private static int podcastSpeedIndex;

    private static int musicTrack;
    private static boolean musicPlaying;
    private static long musicStartedAt;
    private static long musicAccumulated;
    private static boolean musicRepeat;

    private static boolean fitnessInstalled = true;
    private static boolean fitnessWorkoutActive;
    private static long fitnessStartedAt;
    private static double fitnessDistance;
    private static Vec3 fitnessLastPosition;

    private static double walletBalance = 1250.00D;

    private PhoneCoreAppsState() {
    }

    public static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }

        loaded = true;

        Properties properties =
                new Properties();

        Path path = storagePath();

        if (Files.isRegularFile(path)) {
            try (Reader reader =
                         Files.newBufferedReader(
                                 path,
                                 StandardCharsets.UTF_8
                         )) {
                properties.load(reader);
            } catch (IOException ignored) {
            }
        }

        fitnessInstalled =
                Boolean.parseBoolean(
                        properties.getProperty(
                                "fitnessInstalled",
                                "true"
                        )
                );

        walletBalance =
                parseDouble(
                        properties.getProperty(
                                "walletBalance",
                                "1250.0"
                        ),
                        1250.0D
                );

        faceTimeMuted =
                Boolean.parseBoolean(
                        properties.getProperty(
                                "faceTimeMuted",
                                "false"
                        )
                );

        faceTimeCameraEnabled =
                Boolean.parseBoolean(
                        properties.getProperty(
                                "faceTimeCameraEnabled",
                                "true"
                        )
                );

        tvVolume =
                Math.max(
                        0,
                        Math.min(
                                100,
                                parseInt(
                                        properties.getProperty(
                                                "tvVolume",
                                                "65"
                                        ),
                                        65
                                )
                        )
                );

        podcastSpeedIndex =
                Math.max(
                        0,
                        Math.min(
                                2,
                                parseInt(
                                        properties.getProperty(
                                                "podcastSpeedIndex",
                                                "0"
                                        ),
                                        0
                                )
                        )
                );

        musicRepeat =
                Boolean.parseBoolean(
                        properties.getProperty(
                                "musicRepeat",
                                "false"
                        )
                );

        int count =
                parseInt(
                        properties.getProperty(
                                "mail.count",
                                "0"
                        ),
                        0
                );

        for (int i = 0; i < count; i++) {
            String prefix =
                    "mail."
                            + i
                            + ".";

            long id =
                    parseLong(
                            properties.getProperty(
                                    prefix + "id",
                                    "0"
                            ),
                            0L
                    );

            MAIL.add(
                    new MailMessage(
                            id,
                            decode(
                                    properties.getProperty(
                                            prefix + "from",
                                            ""
                                    )
                            ),
                            decode(
                                    properties.getProperty(
                                            prefix + "to",
                                            ""
                                    )
                            ),
                            decode(
                                    properties.getProperty(
                                            prefix + "subject",
                                            ""
                                    )
                            ),
                            decode(
                                    properties.getProperty(
                                            prefix + "body",
                                            ""
                                    )
                            ),
                            parseLong(
                                    properties.getProperty(
                                            prefix + "time",
                                            "0"
                                    ),
                                    0L
                            ),
                            Boolean.parseBoolean(
                                    properties.getProperty(
                                            prefix + "sent",
                                            "false"
                                    )
                            )
                    )
            );

            nextMailId =
                    Math.max(
                            nextMailId,
                            id + 1L
                    );
        }

        if (MAIL.isEmpty()) {
            MAIL.add(
                    new MailMessage(
                            nextMailId++,
                            "VS:IA System",
                            "You",
                            "Welcome to Mail",
                            "Your VS:IA phone mail client is ready.",
                            System.currentTimeMillis(),
                            false
                    )
            );

            save();
        }
    }

    public static synchronized List<MailMessage> inbox() {
        ensureLoaded();

        return MAIL.stream()
                .filter(
                        mail -> !mail.sent()
                )
                .sorted(
                        (a, b) ->
                                Long.compare(
                                        b.time(),
                                        a.time()
                                )
                )
                .toList();
    }

    public static synchronized List<MailMessage> sent() {
        ensureLoaded();

        return MAIL.stream()
                .filter(
                        MailMessage::sent
                )
                .sorted(
                        (a, b) ->
                                Long.compare(
                                        b.time(),
                                        a.time()
                                )
                )
                .toList();
    }

    public static synchronized void sendMail(
            String to,
            String subject,
            String body
    ) {
        ensureLoaded();

        MAIL.add(
                new MailMessage(
                        nextMailId++,
                        "You",
                        safe(
                                to,
                                "Unknown"
                        ),
                        safe(
                                subject,
                                "(No Subject)"
                        ),
                        body == null
                                ? ""
                                : body,
                        System.currentTimeMillis(),
                        true
                )
        );

        save();
    }

    public static synchronized boolean faceTimeActive() {
        return !faceTimeTarget.isBlank();
    }

    public static synchronized String faceTimeTarget() {
        return faceTimeTarget;
    }

    public static synchronized void startFaceTime(
            String target
    ) {
        faceTimeTarget =
                target == null
                        ? ""
                        : target;

        faceTimeStartedAt =
                System.currentTimeMillis();
        save();
    }

    public static synchronized void endFaceTime() {
        faceTimeTarget = "";
        faceTimeStartedAt = 0L;
        save();
    }

    public static synchronized long faceTimeSeconds() {
        if (!faceTimeActive()) {
            return 0L;
        }

        return Math.max(
                0L,
                (System.currentTimeMillis()
                        - faceTimeStartedAt)
                        / 1000L
        );
    }

    public static synchronized boolean faceTimeMuted() {
        return faceTimeMuted;
    }

    public static synchronized void toggleFaceTimeMuted() {
        faceTimeMuted = !faceTimeMuted;
        save();
    }

    public static synchronized boolean faceTimeCameraEnabled() {
        return faceTimeCameraEnabled;
    }

    public static synchronized void toggleFaceTimeCameraEnabled() {
        faceTimeCameraEnabled = !faceTimeCameraEnabled;
        save();
    }

    public static synchronized int tvChannel() {
        return tvChannel;
    }

    public static synchronized void nextTvChannel() {
        tvChannel =
                (tvChannel + 1)
                        % 3;
        save();
    }

    public static synchronized boolean tvPlaying() {
        return tvPlaying;
    }

    public static synchronized void toggleTv() {
        tvPlaying = !tvPlaying;
        save();
    }

    public static synchronized int tvVolume() {
        return tvVolume;
    }

    public static synchronized void adjustTvVolume(
            int delta
    ) {
        tvVolume =
                Math.max(
                        0,
                        Math.min(
                                100,
                                tvVolume + delta
                        )
                );

        save();
    }

    public static synchronized int podcastEpisode() {
        return podcastEpisode;
    }

    public static synchronized void setPodcastEpisode(
            int index
    ) {
        podcastEpisode =
                Math.max(
                        0,
                        Math.min(
                                2,
                                index
                        )
                );

        podcastAccumulated = 0L;
        podcastStartedAt =
                System.currentTimeMillis();
        save();
    }

    public static synchronized boolean podcastPlaying() {
        return podcastPlaying;
    }

    public static synchronized void togglePodcast() {
        if (podcastPlaying) {
            podcastAccumulated =
                    podcastElapsedMillis();
            podcastPlaying = false;
        } else {
            podcastStartedAt =
                    System.currentTimeMillis();
            podcastPlaying = true;
        }

        save();
    }

    public static synchronized long podcastElapsedMillis() {
        if (!podcastPlaying) {
            return podcastAccumulated;
        }

        return podcastAccumulated
                + System.currentTimeMillis()
                - podcastStartedAt;
    }

    public static synchronized void skipPodcastMillis(
            long delta
    ) {
        long updated =
                Math.max(
                        0L,
                        podcastElapsedMillis()
                                + delta
                );

        podcastAccumulated = updated;

        if (podcastPlaying) {
            podcastStartedAt =
                    System.currentTimeMillis();
        }

        save();
    }

    public static synchronized String podcastSpeedLabel() {
        return switch (podcastSpeedIndex) {
            case 1 -> "1.5x";
            case 2 -> "2x";
            default -> "1x";
        };
    }

    public static synchronized void cyclePodcastSpeed() {
        podcastSpeedIndex =
                (podcastSpeedIndex + 1)
                        % 3;

        save();
    }

    public static synchronized float podcastPitch() {
        return switch (podcastSpeedIndex) {
            case 1 -> 1.5F;
            case 2 -> 2.0F;
            default -> 1.0F;
        };
    }

    public static synchronized int musicTrack() {
        return musicTrack;
    }

    public static synchronized void nextMusicTrack() {
        musicTrack =
                (musicTrack + 1)
                        % 4;

        musicAccumulated = 0L;
        musicStartedAt =
                System.currentTimeMillis();
        save();
    }

    public static synchronized void previousMusicTrack() {
        musicTrack =
                Math.floorMod(
                        musicTrack - 1,
                        4
                );

        musicAccumulated = 0L;
        musicStartedAt =
                System.currentTimeMillis();
        save();
    }

    public static synchronized boolean musicPlaying() {
        return musicPlaying;
    }

    public static synchronized void toggleMusic() {
        if (musicPlaying) {
            musicAccumulated =
                    musicElapsedMillis();
            musicPlaying = false;
        } else {
            musicStartedAt =
                    System.currentTimeMillis();
            musicPlaying = true;
        }

        save();
    }

    public static synchronized long musicElapsedMillis() {
        if (!musicPlaying) {
            return musicAccumulated;
        }

        return musicAccumulated
                + System.currentTimeMillis()
                - musicStartedAt;
    }

    public static synchronized boolean musicRepeat() {
        return musicRepeat;
    }

    public static synchronized void toggleMusicRepeat() {
        musicRepeat = !musicRepeat;
        save();
    }

    public static synchronized boolean fitnessInstalled() {
        ensureLoaded();
        return fitnessInstalled;
    }

    public static synchronized void setFitnessInstalled(
            boolean installed
    ) {
        ensureLoaded();
        fitnessInstalled = installed;
        save();
    }

    public static synchronized boolean fitnessWorkoutActive() {
        return fitnessWorkoutActive;
    }

    public static synchronized void startFitnessWorkout() {
        fitnessWorkoutActive = true;
        fitnessStartedAt = System.currentTimeMillis();
        fitnessDistance = 0.0D;

        Minecraft minecraft =
                Minecraft.getInstance();

        fitnessLastPosition =
                minecraft.player == null
                        ? null
                        : minecraft.player.position();
    }

    public static synchronized void stopFitnessWorkout() {
        fitnessWorkoutActive = false;
        fitnessLastPosition = null;
    }

    public static synchronized long fitnessSeconds() {
        if (!fitnessWorkoutActive) {
            return 0L;
        }

        return Math.max(
                0L,
                (System.currentTimeMillis()
                        - fitnessStartedAt)
                        / 1000L
        );
    }

    public static synchronized double fitnessDistance() {
        return fitnessDistance;
    }

    public static synchronized int fitnessSteps() {
        return (int) Math.round(
                fitnessDistance
                        * 1.35D
        );
    }

    public static synchronized double walletBalance() {
        ensureLoaded();
        return walletBalance;
    }

    public static synchronized boolean walletPurchase(
            double amount
    ) {
        ensureLoaded();

        if (amount <= 0.0D
                || walletBalance < amount) {
            return false;
        }

        walletBalance -= amount;
        save();
        return true;
    }

    public static synchronized void tick() {
        ensureLoaded();

        if (!fitnessWorkoutActive) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            fitnessLastPosition = null;
            return;
        }

        Vec3 current =
                minecraft.player.position();

        if (fitnessLastPosition != null) {
            double dx =
                    current.x
                            - fitnessLastPosition.x;

            double dz =
                    current.z
                            - fitnessLastPosition.z;

            double horizontal =
                    Math.sqrt(
                            dx * dx
                                    + dz * dz
                    );

            if (horizontal >= 0.0D
                    && horizontal < 16.0D) {
                fitnessDistance += horizontal;
            }
        }

        fitnessLastPosition = current;
    }

    public static synchronized void reset() {
        MAIL.clear();
        nextMailId = 1L;
        endFaceTime();

        tvChannel = 0;
        tvPlaying = true;
        tvVolume = 65;

        faceTimeMuted = false;
        faceTimeCameraEnabled = true;

        podcastEpisode = 0;
        podcastPlaying = false;
        podcastStartedAt = 0L;
        podcastAccumulated = 0L;
        podcastSpeedIndex = 0;

        musicTrack = 0;
        musicPlaying = false;
        musicStartedAt = 0L;
        musicAccumulated = 0L;
        musicRepeat = false;

        fitnessInstalled = true;
        fitnessWorkoutActive = false;
        fitnessDistance = 0.0D;
        fitnessLastPosition = null;

        walletBalance = 1250.0D;

        save();
    }

    private static void save() {
        Properties properties =
                new Properties();

        properties.setProperty(
                "fitnessInstalled",
                Boolean.toString(
                        fitnessInstalled
                )
        );

        properties.setProperty(
                "walletBalance",
                Double.toString(
                        walletBalance
                )
        );

        properties.setProperty(
                "faceTimeMuted",
                Boolean.toString(
                        faceTimeMuted
                )
        );

        properties.setProperty(
                "faceTimeCameraEnabled",
                Boolean.toString(
                        faceTimeCameraEnabled
                )
        );

        properties.setProperty(
                "tvVolume",
                Integer.toString(
                        tvVolume
                )
        );

        properties.setProperty(
                "podcastSpeedIndex",
                Integer.toString(
                        podcastSpeedIndex
                )
        );

        properties.setProperty(
                "musicRepeat",
                Boolean.toString(
                        musicRepeat
                )
        );

        properties.setProperty(
                "mail.count",
                Integer.toString(
                        MAIL.size()
                )
        );

        for (int i = 0; i < MAIL.size(); i++) {
            MailMessage mail =
                    MAIL.get(i);

            String prefix =
                    "mail."
                            + i
                            + ".";

            properties.setProperty(
                    prefix + "id",
                    Long.toString(
                            mail.id()
                    )
            );

            properties.setProperty(
                    prefix + "from",
                    encode(
                            mail.from()
                    )
            );

            properties.setProperty(
                    prefix + "to",
                    encode(
                            mail.to()
                    )
            );

            properties.setProperty(
                    prefix + "subject",
                    encode(
                            mail.subject()
                    )
            );

            properties.setProperty(
                    prefix + "body",
                    encode(
                            mail.body()
                    )
            );

            properties.setProperty(
                    prefix + "time",
                    Long.toString(
                            mail.time()
                    )
            );

            properties.setProperty(
                    prefix + "sent",
                    Boolean.toString(
                            mail.sent()
                    )
            );
        }

        Path path =
                storagePath();

        try {
            Files.createDirectories(
                    path.getParent()
            );

            try (Writer writer =
                         Files.newBufferedWriter(
                                 path,
                                 StandardCharsets.UTF_8
                         )) {
                properties.store(
                        writer,
                        "VSIA phone core apps"
                );
            }
        } catch (IOException ignored) {
        }
    }

    private static Path storagePath() {
        return Minecraft.getInstance()
                .gameDirectory
                .toPath()
                .resolve("config")
                .resolve("vsia")
                .resolve("phone_core_apps.properties");
    }

    private static String encode(
            String value
    ) {
        return Base64.getEncoder()
                .encodeToString(
                        (value == null
                                ? ""
                                : value)
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                );
    }

    private static String decode(
            String value
    ) {
        try {
            return new String(
                    Base64.getDecoder()
                            .decode(
                                    value == null
                                            ? ""
                                            : value
                            ),
                    StandardCharsets.UTF_8
            );
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private static String safe(
            String value,
            String fallback
    ) {
        if (value == null
                || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

    private static int parseInt(
            String value,
            int fallback
    ) {
        try {
            return Integer.parseInt(
                    value
            );
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static long parseLong(
            String value,
            long fallback
    ) {
        try {
            return Long.parseLong(
                    value
            );
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double parseDouble(
            String value,
            double fallback
    ) {
        try {
            return Double.parseDouble(
                    value
            );
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
