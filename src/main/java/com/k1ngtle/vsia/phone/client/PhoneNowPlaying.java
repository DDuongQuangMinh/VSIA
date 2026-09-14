package com.k1ngtle.vsia.phone.client;

public final class PhoneNowPlaying {
    public enum Kind {
        NONE,
        MUSIC,
        PODCAST,
        TV
    }

    private static final String[] MUSIC_TITLES = {
            "Overworld Signal",
            "Night Operations",
            "Digital Horizon",
            "Return to Base"
    };

    private static final String[] MUSIC_ARTISTS = {
            "VS:IA Audio",
            "Signality",
            "Network Lab",
            "VS:IA Audio"
    };

    private static final String[] PODCAST_TITLES = {
            "Building VS:IA",
            "Signals & Networks",
            "Ships, Physics & Systems"
    };

    private static final String[] TV_TITLES = {
            "VS:IA Live",
            "World Weather",
            "Network Ops"
    };

    private static Kind active = Kind.NONE;

    private PhoneNowPlaying() {
    }

    public static synchronized void mark(Kind kind) {
        if (kind != null) {
            active = kind;
        }
    }

    public static synchronized Kind kind() {
        if (active != Kind.NONE) {
            return active;
        }

        if (PhoneCoreAppsState.musicPlaying()
                || PhoneCoreAppsState.musicElapsedMillis() > 0L) {
            return Kind.MUSIC;
        }

        if (PhoneCoreAppsState.podcastPlaying()
                || PhoneCoreAppsState.podcastElapsedMillis() > 0L) {
            return Kind.PODCAST;
        }

        return Kind.NONE;
    }

    public static synchronized boolean hasActive() {
        return kind() != Kind.NONE;
    }

    public static synchronized String title() {
        return switch (kind()) {
            case MUSIC ->
                    MUSIC_TITLES[
                            Math.max(
                                    0,
                                    Math.min(
                                            MUSIC_TITLES.length - 1,
                                            PhoneCoreAppsState.musicTrack()
                                    )
                            )
                    ];

            case PODCAST ->
                    PODCAST_TITLES[
                            Math.max(
                                    0,
                                    Math.min(
                                            PODCAST_TITLES.length - 1,
                                            PhoneCoreAppsState.podcastEpisode()
                                    )
                            )
                    ];

            case TV ->
                    TV_TITLES[
                            Math.max(
                                    0,
                                    Math.min(
                                            TV_TITLES.length - 1,
                                            PhoneCoreAppsState.tvChannel()
                                    )
                            )
                    ];

            default ->
                    "";
        };
    }

    public static synchronized String subtitle() {
        return switch (kind()) {
            case MUSIC ->
                    MUSIC_ARTISTS[
                            Math.max(
                                    0,
                                    Math.min(
                                            MUSIC_ARTISTS.length - 1,
                                            PhoneCoreAppsState.musicTrack()
                                    )
                            )
                    ];

            case PODCAST ->
                    "Podcasts";

            case TV ->
                    "TV";

            default ->
                    "";
        };
    }

    public static synchronized long elapsedMillis() {
        return switch (kind()) {
            case MUSIC ->
                    PhoneCoreAppsState.musicElapsedMillis();

            case PODCAST ->
                    PhoneCoreAppsState.podcastElapsedMillis();

            default ->
                    0L;
        };
    }

    public static synchronized boolean playing() {
        return switch (kind()) {
            case MUSIC ->
                    PhoneCoreAppsState.musicPlaying();

            case PODCAST ->
                    PhoneCoreAppsState.podcastPlaying();

            case TV ->
                    PhoneCoreAppsState.tvPlaying();

            default ->
                    false;
        };
    }

    public static synchronized void toggle() {
        switch (kind()) {
            case MUSIC ->
                    PhoneCoreAppsState.toggleMusic();

            case PODCAST ->
                    PhoneCoreAppsState.togglePodcast();

            case TV ->
                    PhoneCoreAppsState.toggleTv();

            default -> {
            }
        }
    }

    public static synchronized void previous() {
        switch (kind()) {
            case MUSIC ->
                    PhoneCoreAppsState.previousMusicTrack();

            case PODCAST -> {
                int current =
                        PhoneCoreAppsState.podcastEpisode();

                PhoneCoreAppsState.setPodcastEpisode(
                        Math.floorMod(
                                current - 1,
                                3
                        )
                );
            }

            case TV ->
                    PhoneCoreAppsState.nextTvChannel();

            default -> {
            }
        }
    }

    public static synchronized void next() {
        switch (kind()) {
            case MUSIC ->
                    PhoneCoreAppsState.nextMusicTrack();

            case PODCAST -> {
                int current =
                        PhoneCoreAppsState.podcastEpisode();

                PhoneCoreAppsState.setPodcastEpisode(
                        (current + 1)
                                % 3
                );
            }

            case TV ->
                    PhoneCoreAppsState.nextTvChannel();

            default -> {
            }
        }
    }
}
