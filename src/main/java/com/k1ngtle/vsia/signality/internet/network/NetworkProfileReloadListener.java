package com.k1ngtle.vsia.signality.internet.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.k1ngtle.vsia.signality.Signality;
import com.k1ngtle.vsia.signality.engineering.phy.Modulation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Locale;
import java.util.Map;

public final class NetworkProfileReloadListener
        extends SimpleJsonResourceReloadListener {

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    public NetworkProfileReloadListener() {
        super(
                GSON,
                "signality/network_profiles"
        );
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> entries,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        NetworkProfileRegistry.resetToBuiltIns();

        int loaded = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation id = entry.getKey();

            try {
                NetworkProfile profile =
                        parse(
                                id,
                                entry.getValue().getAsJsonObject()
                        );

                NetworkProfileRegistry.register(profile);
                loaded++;
            } catch (Exception exception) {
                Signality.LOGGER.error(
                        "Failed to load Signality network profile {}",
                        id,
                        exception
                );
            }
        }

        Signality.LOGGER.info(
                "Loaded {} datapack Signality network profile(s); {} total profile(s) available.",
                loaded,
                NetworkProfileRegistry.values().size()
        );
    }

    private static NetworkProfile parse(
            ResourceLocation id,
            JsonObject json
    ) {
        NetworkKind kind =
                NetworkKind.valueOf(
                        requiredString(
                                json,
                                "kind"
                        ).toUpperCase(Locale.ROOT)
                );

        JsonArray frequencyArray =
                json.getAsJsonArray(
                        "frequencies_hz"
                );

        if (frequencyArray == null
                || frequencyArray.size() == 0) {
            throw new IllegalArgumentException(
                    "frequencies_hz must contain at least one frequency"
            );
        }

        double[] frequencies =
                new double[
                        frequencyArray.size()
                        ];

        for (int i = 0; i < frequencyArray.size(); i++) {
            frequencies[i] =
                    frequencyArray
                            .get(i)
                            .getAsDouble();
        }

        JsonObject phyJson =
                requiredObject(
                        json,
                        "phy"
                );

        JsonObject codingJson =
                requiredObject(
                        phyJson,
                        "coding"
                );

        JsonObject ofdmJson =
                optionalObject(
                        phyJson,
                        "ofdm"
                );

        PhyDefinition phy =
                new PhyDefinition(
                        Modulation.valueOf(
                                requiredString(
                                        phyJson,
                                        "modulation"
                                ).toUpperCase(Locale.ROOT)
                        ),
                        requiredString(
                                codingJson,
                                "id"
                        ),
                        requiredDouble(
                                codingJson,
                                "rate"
                        ),
                        optionalDouble(
                                codingJson,
                                "gain_db",
                                0.0
                        ),
                        optionalInt(
                                phyJson,
                                "spatial_streams",
                                1
                        ),
                        optionalDouble(
                                phyJson,
                                "guard_efficiency",
                                1.0
                        ),
                        optionalDouble(
                                phyJson,
                                "mac_efficiency",
                                1.0
                        ),
                        optionalDouble(
                                phyJson,
                                "receiver_noise_figure_db",
                                7.0
                        ),
                        ofdmJson == null
                                ? 0
                                : optionalInt(
                                ofdmJson,
                                "fft_size",
                                0
                        ),
                        ofdmJson == null
                                ? 0
                                : optionalInt(
                                ofdmJson,
                                "data_subcarriers",
                                0
                        ),
                        ofdmJson == null
                                ? 0.0
                                : optionalDouble(
                                ofdmJson,
                                "subcarrier_spacing_hz",
                                0.0
                        ),
                        ofdmJson == null
                                ? 0
                                : optionalInt(
                                ofdmJson,
                                "cyclic_prefix_samples",
                                0
                        )
                );

        return new NetworkProfile(
                id,
                kind,
                optionalString(
                        json,
                        "display_name",
                        id.toString()
                ),
                optionalString(
                        json,
                        "compatibility_group",
                        id.toString()
                ),
                frequencies,
                optionalDouble(
                        json,
                        "default_frequency_hz",
                        frequencies[0]
                ),
                requiredDouble(
                        json,
                        "bandwidth_hz"
                ),
                requiredDouble(
                        json,
                        "transmit_power_watts"
                ),
                optionalDouble(
                        json,
                        "antenna_gain",
                        1.0
                ),
                requiredDouble(
                        json,
                        "sensitivity_watts"
                ),
                optionalDouble(
                        json,
                        "maximum_range_blocks",
                        Double.MAX_VALUE
                ),
                optionalString(
                        json,
                        "protocol",
                        ""
                ),
                optionalString(
                        json,
                        "security",
                        ""
                ),
                phy
        );
    }

    private static JsonObject requiredObject(
            JsonObject json,
            String key
    ) {
        if (!json.has(key)
                || !json.get(key).isJsonObject()) {
            throw new IllegalArgumentException(
                    "Missing required object: " + key
            );
        }

        return json.getAsJsonObject(key);
    }

    private static JsonObject optionalObject(
            JsonObject json,
            String key
    ) {
        if (!json.has(key)
                || json.get(key).isJsonNull()) {
            return null;
        }

        if (!json.get(key).isJsonObject()) {
            throw new IllegalArgumentException(
                    key + " must be an object"
            );
        }

        return json.getAsJsonObject(key);
    }

    private static String requiredString(
            JsonObject json,
            String key
    ) {
        if (!json.has(key)) {
            throw new IllegalArgumentException(
                    "Missing required field: " + key
            );
        }

        return json.get(key).getAsString();
    }

    private static double requiredDouble(
            JsonObject json,
            String key
    ) {
        if (!json.has(key)) {
            throw new IllegalArgumentException(
                    "Missing required field: " + key
            );
        }

        return json.get(key).getAsDouble();
    }

    private static String optionalString(
            JsonObject json,
            String key,
            String fallback
    ) {
        return json.has(key)
                ? json.get(key).getAsString()
                : fallback;
    }

    private static double optionalDouble(
            JsonObject json,
            String key,
            double fallback
    ) {
        return json.has(key)
                ? json.get(key).getAsDouble()
                : fallback;
    }

    private static int optionalInt(
            JsonObject json,
            String key,
            int fallback
    ) {
        return json.has(key)
                ? json.get(key).getAsInt()
                : fallback;
    }
}
