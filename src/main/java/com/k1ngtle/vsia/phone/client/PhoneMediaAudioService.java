package com.k1ngtle.vsia.phone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class PhoneMediaAudioService {
    private static LoopingPhoneSound musicSound;
    private static LoopingPhoneSound podcastSound;
    private static LoopingPhoneSound tvSound;

    private PhoneMediaAudioService() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.player == null) {
            stopAll();
            return;
        }

        syncMusic(minecraft);
        syncPodcast(minecraft);
        syncTv(minecraft);
    }

    private static void syncMusic(
            Minecraft minecraft
    ) {
        if (!PhoneCoreAppsState.musicPlaying()) {
            stopMusic();
            return;
        }

        ResourceLocation id =
                musicTrackId(
                        PhoneCoreAppsState.musicTrack()
                );

        if (musicSound == null
                || !musicSound.matches(id)
                || musicSound.hasStopped()) {
            stopMusic();
            musicSound =
                    playLoop(
                            minecraft,
                            id,
                            0.55F,
                            1.0F
                    );
        } else {
            musicSound.setTarget(
                    0.55F,
                    1.0F
            );
        }
    }

    private static void syncPodcast(
            Minecraft minecraft
    ) {
        if (!PhoneCoreAppsState.podcastPlaying()) {
            stopPodcast();
            return;
        }

        ResourceLocation id =
                podcastTrackId(
                        PhoneCoreAppsState.podcastEpisode()
                );

        float pitch =
                PhoneCoreAppsState.podcastPitch();

        if (podcastSound == null
                || !podcastSound.matches(id)
                || podcastSound.hasStopped()) {
            stopPodcast();
            podcastSound =
                    playLoop(
                            minecraft,
                            id,
                            0.60F,
                            pitch
                    );
        } else {
            podcastSound.setTarget(
                    0.60F,
                    pitch
            );
        }
    }

    private static void syncTv(
            Minecraft minecraft
    ) {
        if (!PhoneCoreAppsState.tvPlaying()) {
            stopTv();
            return;
        }

        ResourceLocation id =
                tvChannelId(
                        PhoneCoreAppsState.tvChannel()
                );

        float volume =
                Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                PhoneCoreAppsState.tvVolume()
                                        / 100.0F
                        )
                );

        if (tvSound == null
                || !tvSound.matches(id)
                || tvSound.hasStopped()) {
            stopTv();
            tvSound =
                    playLoop(
                            minecraft,
                            id,
                            volume,
                            1.0F
                    );
        } else {
            tvSound.setTarget(
                    volume,
                    1.0F
            );
        }
    }

    private static LoopingPhoneSound playLoop(
            Minecraft minecraft,
            ResourceLocation id,
            float volume,
            float pitch
    ) {
        LoopingPhoneSound sound =
                new LoopingPhoneSound(
                        id,
                        volume,
                        pitch
                );

        minecraft.getSoundManager()
                .play(sound);

        return sound;
    }

    public static void stopAll() {
        stopMusic();
        stopPodcast();
        stopTv();
    }

    private static void stopMusic() {
        if (musicSound != null) {
            musicSound.stopNow();
            musicSound = null;
        }
    }

    private static void stopPodcast() {
        if (podcastSound != null) {
            podcastSound.stopNow();
            podcastSound = null;
        }
    }

    private static void stopTv() {
        if (tvSound != null) {
            tvSound.stopNow();
            tvSound = null;
        }
    }

    private static ResourceLocation musicTrackId(
            int track
    ) {
        return switch (track) {
            case 1 -> new ResourceLocation(
                    "vsia",
                    "phone.music.night_operations"
            );
            case 2 -> new ResourceLocation(
                    "vsia",
                    "phone.music.digital_horizon"
            );
            case 3 -> new ResourceLocation(
                    "vsia",
                    "phone.music.return_to_base"
            );
            default -> new ResourceLocation(
                    "vsia",
                    "phone.music.overworld_signal"
            );
        };
    }

    private static ResourceLocation podcastTrackId(
            int episode
    ) {
        return switch (episode) {
            case 1 -> new ResourceLocation(
                    "vsia",
                    "phone.podcast.signals_networks"
            );
            case 2 -> new ResourceLocation(
                    "vsia",
                    "phone.podcast.ships_physics"
            );
            default -> new ResourceLocation(
                    "vsia",
                    "phone.podcast.building_vsia"
            );
        };
    }

    private static ResourceLocation tvChannelId(
            int channel
    ) {
        return switch (channel) {
            case 1 -> new ResourceLocation(
                    "vsia",
                    "phone.tv.world_weather"
            );
            case 2 -> new ResourceLocation(
                    "vsia",
                    "phone.tv.network_ops"
            );
            default -> new ResourceLocation(
                    "vsia",
                    "phone.tv.vsia_live"
            );
        };
    }

    private static final class LoopingPhoneSound extends AbstractTickableSoundInstance {
        private final ResourceLocation id;
        private float targetVolume;
        private float targetPitch;

        private LoopingPhoneSound(
                ResourceLocation id,
                float volume,
                float pitch
        ) {
            super(
                    SoundEvent.createVariableRangeEvent(id),
                    SoundSource.RECORDS,
                    RandomSource.create()
            );
            this.id = id;
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.volume = volume;
            this.pitch = pitch;
            this.targetVolume = volume;
            this.targetPitch = pitch;
            this.x = 0.0D;
            this.y = 0.0D;
            this.z = 0.0D;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            this.volume = targetVolume;
            this.pitch = targetPitch;
        }

        private boolean matches(
                ResourceLocation other
        ) {
            return id.equals(other);
        }

        private boolean hasStopped() {
            return isStopped();
        }

        private void setTarget(
                float volume,
                float pitch
        ) {
            this.targetVolume = volume;
            this.targetPitch = pitch;
        }

        private void stopNow() {
            stop();
        }
    }
}
