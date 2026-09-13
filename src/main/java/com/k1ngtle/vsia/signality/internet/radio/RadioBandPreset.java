package com.k1ngtle.vsia.signality.internet.radio;

import com.k1ngtle.vsia.signality.Signality;
import com.k1ngtle.vsia.signality.engineering.radio.RadioEmission;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public enum RadioBandPreset {
    HF("radio_hf", 10_000_000.0, 12_500.0, RadioEmission.AM, "HF-OPS"),
    VHF("radio_vhf", 150_000_000.0, 25_000.0, RadioEmission.FM, "VHF-OPS"),
    UHF("radio_uhf", 450_000_000.0, 25_000.0, RadioEmission.FM, "UHF-OPS");

    private final String profilePath;
    private final double frequencyHz;
    private final double bandwidthHz;
    private final RadioEmission emission;
    private final String channelId;

    RadioBandPreset(
            String profilePath,
            double frequencyHz,
            double bandwidthHz,
            RadioEmission emission,
            String channelId
    ) {
        this.profilePath = profilePath;
        this.frequencyHz = frequencyHz;
        this.bandwidthHz = bandwidthHz;
        this.emission = emission;
        this.channelId = channelId;
    }

    public ResourceLocation profileId() {
        return new ResourceLocation(Signality.MODID, profilePath);
    }

    public double frequencyHz() {
        return frequencyHz;
    }

    public double bandwidthHz() {
        return bandwidthHz;
    }

    public RadioEmission emission() {
        return emission;
    }

    public String channelId() {
        return channelId;
    }

    public boolean apply(NetworkDeviceBlockEntity device) {
        if (device == null) {
            return false;
        }

        if (!device.setNetworkProfile(profileId())) {
            return false;
        }

        device.setRadioSquelchSnrThresholdDb(3.0);

        return device.configureRadioTransceiver(
                channelId,
                frequencyHz,
                bandwidthHz,
                emission,
                "100.0"
        );
    }

    public static RadioBandPreset fromCommand(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
