package com.k1ngtle.vsia.signality.internet.network;

import com.k1ngtle.vsia.signality.Signality;
import com.k1ngtle.vsia.signality.engineering.phy.Modulation;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class NetworkProfileRegistry {
    public static final ResourceLocation DEFAULT_PROFILE_ID =
            new ResourceLocation(
                    Signality.MODID,
                    "wifi_7"
            );

    private static final Map<ResourceLocation, NetworkProfile>
            BUILT_INS =
            new LinkedHashMap<>();

    private static final Map<ResourceLocation, NetworkProfile>
            PROFILES =
            new LinkedHashMap<>();

    static {
        registerBuiltIns();
        resetToBuiltIns();
    }

    private NetworkProfileRegistry() {
    }

    private static void registerBuiltIns() {
        addWifi(
                "wifi_1",
                "Wi-Fi 1",
                new double[]{2_437_000_000.0},
                2_437_000_000.0,
                22_000_000.0,
                "signality:80211_legacy",
                "signality:open",
                Modulation.QPSK,
                "legacy_fec",
                1.0 / 2.0,
                1.5,
                1,
                0,
                0,
                0.0,
                0
        );

        addWifi(
                "wifi_2",
                "Wi-Fi 2",
                new double[]{5_200_000_000.0},
                5_200_000_000.0,
                20_000_000.0,
                "signality:80211_legacy",
                "signality:open",
                Modulation.QAM64,
                "convolutional",
                3.0 / 4.0,
                2.0,
                1,
                64,
                48,
                312_500.0,
                16
        );

        addWifi(
                "wifi_3",
                "Wi-Fi 3",
                new double[]{2_437_000_000.0},
                2_437_000_000.0,
                20_000_000.0,
                "signality:80211_legacy",
                "signality:wpa",
                Modulation.QAM64,
                "convolutional",
                3.0 / 4.0,
                2.0,
                1,
                64,
                48,
                312_500.0,
                16
        );

        addWifi(
                "wifi_4",
                "Wi-Fi 4",
                new double[]{
                        2_437_000_000.0,
                        5_200_000_000.0
                },
                5_200_000_000.0,
                40_000_000.0,
                "signality:80211n",
                "signality:wpa2",
                Modulation.QAM64,
                "convolutional",
                5.0 / 6.0,
                2.5,
                2,
                128,
                108,
                312_500.0,
                32
        );

        addWifi(
                "wifi_5",
                "Wi-Fi 5",
                new double[]{5_200_000_000.0},
                5_200_000_000.0,
                80_000_000.0,
                "signality:80211ac",
                "signality:wpa2",
                Modulation.QAM256,
                "ldpc",
                5.0 / 6.0,
                3.0,
                4,
                256,
                234,
                312_500.0,
                64
        );

        addWifi(
                "wifi_6",
                "Wi-Fi 6",
                new double[]{
                        2_437_000_000.0,
                        5_200_000_000.0
                },
                5_200_000_000.0,
                160_000_000.0,
                "signality:80211ax",
                "signality:wpa3",
                Modulation.QAM1024,
                "ldpc",
                5.0 / 6.0,
                3.5,
                4,
                2048,
                1960,
                78_125.0,
                256
        );

        addWifi(
                "wifi_6e",
                "Wi-Fi 6E",
                new double[]{6_100_000_000.0},
                6_100_000_000.0,
                160_000_000.0,
                "signality:80211ax",
                "signality:wpa3",
                Modulation.QAM1024,
                "ldpc",
                5.0 / 6.0,
                3.5,
                4,
                2048,
                1960,
                78_125.0,
                256
        );

        addWifi(
                "wifi_7",
                "Wi-Fi 7",
                new double[]{
                        2_437_000_000.0,
                        5_200_000_000.0,
                        6_100_000_000.0
                },
                6_100_000_000.0,
                320_000_000.0,
                "signality:80211be",
                "signality:wpa3",
                Modulation.QAM4096,
                "ldpc",
                5.0 / 6.0,
                4.0,
                4,
                4096,
                3920,
                78_125.0,
                512
        );

        addCellular(
                "cellular_1g",
                "1G Cellular",
                "signality:cellular_1g",
                900_000_000.0,
                25_000.0,
                0.6,
                2_500.0,
                "signality:1g_analog",
                Modulation.BPSK,
                "uncoded",
                1.0,
                0.0,
                1,
                0,
                0,
                0.0,
                0
        );

        addCellular(
                "cellular_2g",
                "2G Cellular",
                "signality:cellular_2g",
                900_000_000.0,
                200_000.0,
                0.25,
                3_000.0,
                "signality:gsm",
                Modulation.QPSK,
                "convolutional",
                1.0 / 2.0,
                2.0,
                1,
                0,
                0,
                0.0,
                0
        );

        addCellular(
                "cellular_3g",
                "3G Cellular",
                "signality:cellular_3g",
                2_100_000_000.0,
                5_000_000.0,
                0.25,
                2_500.0,
                "signality:umts",
                Modulation.QPSK,
                "turbo",
                1.0 / 2.0,
                2.5,
                1,
                0,
                0,
                0.0,
                0
        );

        addCellular(
                "cellular_4g",
                "4G LTE",
                "signality:cellular_lte",
                1_800_000_000.0,
                20_000_000.0,
                0.2,
                3_500.0,
                "signality:lte",
                Modulation.QAM64,
                "turbo",
                2.0 / 3.0,
                3.0,
                2,
                2048,
                1200,
                15_000.0,
                144
        );

        addCellular(
                "cellular_5g",
                "5G",
                "signality:cellular_5g",
                3_500_000_000.0,
                100_000_000.0,
                0.2,
                2_000.0,
                "signality:5g_nr",
                Modulation.QAM256,
                "nr_ldpc",
                3.0 / 4.0,
                4.0,
                4,
                4096,
                3276,
                30_000.0,
                288
        );

        addRadio(
                "radio_hf",
                "HF Radio",
                "signality:radio_hf",
                10_000_000.0,
                12_500.0,
                20.0,
                10_000.0
        );

        addRadio(
                "radio_vhf",
                "VHF Radio",
                "signality:radio_vhf",
                150_000_000.0,
                25_000.0,
                5.0,
                5_000.0
        );

        addRadio(
                "radio_uhf",
                "UHF Radio",
                "signality:radio_uhf",
                450_000_000.0,
                25_000.0,
                5.0,
                3_500.0
        );
    }

    private static void addWifi(
            String path,
            String displayName,
            double[] frequencies,
            double defaultFrequency,
            double bandwidth,
            String protocol,
            String security,
            Modulation modulation,
            String codingId,
            double codingRate,
            double codingGain,
            int streams,
            int fftSize,
            int dataSubcarriers,
            double subcarrierSpacing,
            int cyclicPrefixSamples
    ) {
        add(
                path,
                NetworkKind.WIFI,
                displayName,
                "signality:80211",
                frequencies,
                defaultFrequency,
                bandwidth,
                0.1,
                1.0,
                1e-12,
                260.0,
                protocol,
                security,
                new PhyDefinition(
                        modulation,
                        codingId,
                        codingRate,
                        codingGain,
                        streams,
                        0.90,
                        0.80,
                        7.0,
                        fftSize,
                        dataSubcarriers,
                        subcarrierSpacing,
                        cyclicPrefixSamples
                )
        );
    }

    private static void addCellular(
            String path,
            String displayName,
            String compatibilityGroup,
            double frequency,
            double bandwidth,
            double transmitPower,
            double maximumRange,
            String protocol,
            Modulation modulation,
            String codingId,
            double codingRate,
            double codingGain,
            int streams,
            int fftSize,
            int dataSubcarriers,
            double subcarrierSpacing,
            int cyclicPrefixSamples
    ) {
        add(
                path,
                NetworkKind.CELLULAR,
                displayName,
                compatibilityGroup,
                new double[]{frequency},
                frequency,
                bandwidth,
                transmitPower,
                1.0,
                1e-13,
                maximumRange,
                protocol,
                "signality:cellular_auth",
                new PhyDefinition(
                        modulation,
                        codingId,
                        codingRate,
                        codingGain,
                        streams,
                        0.90,
                        0.85,
                        5.0,
                        fftSize,
                        dataSubcarriers,
                        subcarrierSpacing,
                        cyclicPrefixSamples
                )
        );
    }

    private static void addRadio(
            String path,
            String displayName,
            String compatibilityGroup,
            double frequency,
            double bandwidth,
            double transmitPower,
            double maximumRange
    ) {
        add(
                path,
                NetworkKind.RADIO,
                displayName,
                compatibilityGroup,
                new double[]{frequency},
                frequency,
                bandwidth,
                transmitPower,
                1.0,
                1e-14,
                maximumRange,
                "signality:radio",
                "signality:none",
                new PhyDefinition(
                        Modulation.QPSK,
                        "radio_fec",
                        1.0 / 2.0,
                        2.0,
                        1,
                        0.85,
                        0.75,
                        6.0,
                        0,
                        0,
                        0.0,
                        0
                )
        );
    }

    private static void add(
            String path,
            NetworkKind kind,
            String displayName,
            String compatibilityGroup,
            double[] frequencies,
            double defaultFrequency,
            double bandwidth,
            double transmitPower,
            double antennaGain,
            double sensitivity,
            double maximumRange,
            String protocol,
            String security,
            PhyDefinition phy
    ) {
        ResourceLocation id =
                new ResourceLocation(
                        Signality.MODID,
                        path
                );

        BUILT_INS.put(
                id,
                new NetworkProfile(
                        id,
                        kind,
                        displayName,
                        compatibilityGroup,
                        frequencies,
                        defaultFrequency,
                        bandwidth,
                        transmitPower,
                        antennaGain,
                        sensitivity,
                        maximumRange,
                        protocol,
                        security,
                        phy
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
        return PROFILES.getOrDefault(
                id,
                PROFILES.get(DEFAULT_PROFILE_ID)
        );
    }

    public static synchronized NetworkProfile defaultProfile() {
        return PROFILES.get(DEFAULT_PROFILE_ID);
    }

    public static synchronized Collection<NetworkProfile> values() {
        return Collections.unmodifiableList(
                PROFILES.values()
                        .stream()
                        .toList()
        );
    }

    public static synchronized Map<ResourceLocation, NetworkProfile> snapshot() {
        return Collections.unmodifiableMap(
                new LinkedHashMap<>(
                        PROFILES
                )
        );
    }
}
