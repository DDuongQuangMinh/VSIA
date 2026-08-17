package com.k1ngtle.vsia.signality.internet.network;

import com.k1ngtle.vsia.signality.Signality;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class NetworkProfileRegistry {
    public static final ResourceLocation DEFAULT_PROFILE_ID =
            new ResourceLocation(Signality.MODID, "wifi_7");

    private static final Map<ResourceLocation, NetworkProfile> BUILT_INS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, NetworkProfile> PROFILES = new LinkedHashMap<>();

    static {
        registerBuiltIns();
        resetToBuiltIns();
    }

    private NetworkProfileRegistry() {
    }

    private static void registerBuiltIns() {
        builtIn(
                "wifi_1",
                NetworkKind.WIFI,
                "Wi-Fi 1",
                "signality:80211",
                new double[]{2_437_000_000.0},
                2_437_000_000.0,
                22_000_000.0,
                0.1,
                1.0,
                1e-12,
                250.0,
                "signality:80211_legacy",
                "signality:open"
        );

        builtIn(
                "wifi_2",
                NetworkKind.WIFI,
                "Wi-Fi 2",
                "signality:80211",
                new double[]{5_200_000_000.0},
                5_200_000_000.0,
                20_000_000.0,
                0.1,
                1.0,
                1e-12,
                220.0,
                "signality:80211_legacy",
                "signality:open"
        );

        builtIn(
                "wifi_3",
                NetworkKind.WIFI,
                "Wi-Fi 3",
                "signality:80211",
                new double[]{2_437_000_000.0},
                2_437_000_000.0,
                20_000_000.0,
                0.1,
                1.0,
                1e-12,
                250.0,
                "signality:80211_legacy",
                "signality:wpa"
        );

        builtIn(
                "wifi_4",
                NetworkKind.WIFI,
                "Wi-Fi 4",
                "signality:80211",
                new double[]{2_437_000_000.0, 5_200_000_000.0},
                5_200_000_000.0,
                40_000_000.0,
                0.1,
                1.0,
                1e-12,
                260.0,
                "signality:80211n",
                "signality:wpa2"
        );

        builtIn(
                "wifi_5",
                NetworkKind.WIFI,
                "Wi-Fi 5",
                "signality:80211",
                new double[]{5_200_000_000.0},
                5_200_000_000.0,
                80_000_000.0,
                0.1,
                1.0,
                1e-12,
                240.0,
                "signality:80211ac",
                "signality:wpa2"
        );

        builtIn(
                "wifi_6",
                NetworkKind.WIFI,
                "Wi-Fi 6",
                "signality:80211",
                new double[]{2_437_000_000.0, 5_200_000_000.0},
                5_200_000_000.0,
                160_000_000.0,
                0.1,
                1.0,
                1e-12,
                260.0,
                "signality:80211ax",
                "signality:wpa3"
        );

        builtIn(
                "wifi_6e",
                NetworkKind.WIFI,
                "Wi-Fi 6E",
                "signality:80211",
                new double[]{6_100_000_000.0},
                6_100_000_000.0,
                160_000_000.0,
                0.1,
                1.0,
                1e-12,
                220.0,
                "signality:80211ax",
                "signality:wpa3"
        );

        builtIn(
                "wifi_7",
                NetworkKind.WIFI,
                "Wi-Fi 7",
                "signality:80211",
                new double[]{2_437_000_000.0, 5_200_000_000.0, 6_100_000_000.0},
                6_100_000_000.0,
                320_000_000.0,
                0.1,
                1.0,
                1e-12,
                220.0,
                "signality:80211be",
                "signality:wpa3"
        );

        builtIn(
                "cellular_1g",
                NetworkKind.CELLULAR,
                "1G Cellular",
                "signality:cellular_1g",
                new double[]{900_000_000.0},
                900_000_000.0,
                25_000.0,
                0.6,
                1.0,
                1e-13,
                2_500.0,
                "signality:1g_analog",
                "signality:none"
        );

        builtIn(
                "cellular_2g",
                NetworkKind.CELLULAR,
                "2G Cellular",
                "signality:cellular_2g",
                new double[]{900_000_000.0},
                900_000_000.0,
                200_000.0,
                0.25,
                1.0,
                1e-13,
                3_000.0,
                "signality:gsm",
                "signality:cellular_legacy"
        );

        builtIn(
                "cellular_3g",
                NetworkKind.CELLULAR,
                "3G Cellular",
                "signality:cellular_3g",
                new double[]{2_100_000_000.0},
                2_100_000_000.0,
                5_000_000.0,
                0.25,
                1.0,
                1e-13,
                2_500.0,
                "signality:umts",
                "signality:cellular_auth"
        );

        builtIn(
                "cellular_4g",
                NetworkKind.CELLULAR,
                "4G LTE",
                "signality:cellular_lte",
                new double[]{1_800_000_000.0},
                1_800_000_000.0,
                20_000_000.0,
                0.2,
                1.0,
                1e-13,
                3_500.0,
                "signality:lte",
                "signality:cellular_auth"
        );

        builtIn(
                "cellular_5g",
                NetworkKind.CELLULAR,
                "5G",
                "signality:cellular_5g",
                new double[]{3_500_000_000.0},
                3_500_000_000.0,
                100_000_000.0,
                0.2,
                1.0,
                1e-13,
                2_000.0,
                "signality:5g_nr",
                "signality:5g_aka"
        );

        builtIn(
                "radio_hf",
                NetworkKind.RADIO,
                "HF Radio",
                "signality:radio_hf",
                new double[]{10_000_000.0},
                10_000_000.0,
                12_500.0,
                20.0,
                1.0,
                1e-14,
                10_000.0,
                "signality:radio",
                "signality:none"
        );

        builtIn(
                "radio_vhf",
                NetworkKind.RADIO,
                "VHF Radio",
                "signality:radio_vhf",
                new double[]{150_000_000.0},
                150_000_000.0,
                25_000.0,
                5.0,
                1.0,
                1e-14,
                5_000.0,
                "signality:radio",
                "signality:none"
        );

        builtIn(
                "radio_uhf",
                NetworkKind.RADIO,
                "UHF Radio",
                "signality:radio_uhf",
                new double[]{450_000_000.0},
                450_000_000.0,
                25_000.0,
                5.0,
                1.0,
                1e-14,
                3_500.0,
                "signality:radio",
                "signality:none"
        );
    }

    private static void builtIn(
            String path,
            NetworkKind kind,
            String displayName,
            String compatibilityGroup,
            double[] frequenciesHz,
            double defaultFrequencyHz,
            double bandwidthHz,
            double transmitPowerWatts,
            double antennaGain,
            double sensitivityWatts,
            double maximumRangeBlocks,
            String protocol,
            String security
    ) {
        ResourceLocation id = new ResourceLocation(Signality.MODID, path);
        BUILT_INS.put(
                id,
                new NetworkProfile(
                        id,
                        kind,
                        displayName,
                        compatibilityGroup,
                        frequenciesHz,
                        defaultFrequencyHz,
                        bandwidthHz,
                        transmitPowerWatts,
                        antennaGain,
                        sensitivityWatts,
                        maximumRangeBlocks,
                        protocol,
                        security
                )
        );
    }

    public static synchronized void resetToBuiltIns() {
        PROFILES.clear();
        PROFILES.putAll(BUILT_INS);
    }

    public static synchronized void register(NetworkProfile profile) {
        PROFILES.put(profile.id(), profile);
    }

    public static synchronized Optional<NetworkProfile> get(ResourceLocation id) {
        return Optional.ofNullable(PROFILES.get(id));
    }

    public static synchronized NetworkProfile getOrDefault(ResourceLocation id) {
        return PROFILES.getOrDefault(id, PROFILES.get(DEFAULT_PROFILE_ID));
    }

    public static synchronized NetworkProfile defaultProfile() {
        return PROFILES.get(DEFAULT_PROFILE_ID);
    }

    public static synchronized Collection<NetworkProfile> values() {
        return Collections.unmodifiableList(PROFILES.values().stream().toList());
    }

    public static synchronized Map<ResourceLocation, NetworkProfile> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(PROFILES));
    }
}
