package com.k1ngtle.vsia.signality.engineering.vm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.k1ngtle.vsia.signality.Signality;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ProtocolProgramReloadListener
        extends SimpleJsonResourceReloadListener {

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    public ProtocolProgramReloadListener() {
        super(
                GSON,
                "signality/protocol_programs"
        );
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> entries,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        ProtocolProgramRegistry.clear();

        int loaded = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry
                : entries.entrySet()) {
            try {
                ProtocolProgram program =
                        parse(
                                entry.getKey(),
                                entry.getValue()
                                        .getAsJsonObject()
                        );

                ProtocolProgramRegistry.register(
                        program
                );

                loaded++;
            } catch (Exception exception) {
                Signality.LOGGER.error(
                        "Failed to load Signality protocol program {}",
                        entry.getKey(),
                        exception
                );
            }
        }

        Signality.LOGGER.info(
                "Loaded {} Signality protocol VM program(s).",
                loaded
        );
    }

    private static ProtocolProgram parse(
            ResourceLocation id,
            JsonObject json
    ) {
        int version =
                optionalInt(
                        json,
                        "version",
                        1
                );

        JsonObject limitsJson =
                optionalObject(
                        json,
                        "limits"
                );

        ProtocolVmLimits limits =
                limitsJson == null
                        ? ProtocolVmLimits.DEFAULT
                        : new ProtocolVmLimits(
                        optionalInt(
                                limitsJson,
                                "max_instructions_per_run",
                                ProtocolVmLimits.DEFAULT
                                        .maxInstructionsPerRun()
                        ),
                        optionalInt(
                                limitsJson,
                                "max_buffer_bytes",
                                ProtocolVmLimits.DEFAULT
                                        .maxBufferBytes()
                        ),
                        optionalInt(
                                limitsJson,
                                "max_frame_bytes",
                                ProtocolVmLimits.DEFAULT
                                        .maxFrameBytes()
                        ),
                        optionalInt(
                                limitsJson,
                                "max_timers",
                                ProtocolVmLimits.DEFAULT
                                        .maxTimers()
                        ),
                        optionalInt(
                                limitsJson,
                                "max_timer_delay_ticks",
                                ProtocolVmLimits.DEFAULT
                                        .maxTimerDelayTicks()
                        )
                );

        JsonArray instructionJson =
                requiredArray(
                        json,
                        "instructions"
                );

        Map<String, Integer> labels =
                new HashMap<>();

        ProtocolInstruction[] instructions =
                new ProtocolInstruction[
                        instructionJson.size()
                        ];

        for (int i = 0;
             i < instructionJson.size();
             i++) {
            JsonObject object =
                    instructionJson
                            .get(i)
                            .getAsJsonObject();

            if (object.has("label")) {
                String label =
                        object.get("label")
                                .getAsString();

                if (label.isBlank()) {
                    throw new IllegalArgumentException(
                            "Blank label at instruction "
                                    + i
                    );
                }

                if (labels.put(
                        label,
                        i
                ) != null) {
                    throw new IllegalArgumentException(
                            "Duplicate label: "
                                    + label
                    );
                }
            }
        }

        for (int i = 0;
             i < instructionJson.size();
             i++) {
            JsonObject object =
                    instructionJson
                            .get(i)
                            .getAsJsonObject();

            ProtocolOpcode opcode =
                    ProtocolOpcode.valueOf(
                            requiredString(
                                    object,
                                    "op"
                            ).toUpperCase(
                                    Locale.ROOT
                            )
                    );

            String targetLabel =
                    optionalString(
                            object,
                            "target",
                            ""
                    );

            int target =
                    targetLabel.isBlank()
                            ? -1
                            : labels.getOrDefault(
                            targetLabel,
                            -1
                    );

            if (!targetLabel.isBlank()
                    && target < 0) {
                throw new IllegalArgumentException(
                        "Unknown target label "
                                + targetLabel
                                + " at instruction "
                                + i
                );
            }

            instructions[i] =
                    new ProtocolInstruction(
                            opcode,
                            optionalInt(
                                    object,
                                    "a",
                                    0
                            ),
                            optionalInt(
                                    object,
                                    "b",
                                    0
                            ),
                            optionalInt(
                                    object,
                                    "c",
                                    0
                            ),
                            optionalDouble(
                                    object,
                                    "value",
                                    0.0
                            ),
                            optionalString(
                                    object,
                                    "text",
                                    ""
                            ),
                            target
                    );
        }

        Map<String, Integer> entrypoints =
                new HashMap<>();

        JsonObject entries =
                requiredObject(
                        json,
                        "entrypoints"
                );

        for (Map.Entry<String, JsonElement> entry
                : entries.entrySet()) {
            String label =
                    entry.getValue()
                            .getAsString();

            Integer pc =
                    labels.get(
                            label
                    );

            if (pc == null) {
                throw new IllegalArgumentException(
                        "Entrypoint "
                                + entry.getKey()
                                + " references unknown label "
                                + label
                );
            }

            entrypoints.put(
                    entry.getKey(),
                    pc
            );
        }

        return new ProtocolProgram(
                id,
                version,
                limits,
                entrypoints,
                java.util.Arrays.asList(
                        instructions
                )
        );
    }

    private static JsonObject requiredObject(
            JsonObject json,
            String key
    ) {
        if (!json.has(key)
                || !json.get(key)
                .isJsonObject()) {
            throw new IllegalArgumentException(
                    "Missing required object: "
                            + key
            );
        }

        return json.getAsJsonObject(
                key
        );
    }

    private static JsonObject optionalObject(
            JsonObject json,
            String key
    ) {
        if (!json.has(key)
                || json.get(key)
                .isJsonNull()) {
            return null;
        }

        if (!json.get(key)
                .isJsonObject()) {
            throw new IllegalArgumentException(
                    key + " must be an object"
            );
        }

        return json.getAsJsonObject(
                key
        );
    }

    private static JsonArray requiredArray(
            JsonObject json,
            String key
    ) {
        if (!json.has(key)
                || !json.get(key)
                .isJsonArray()) {
            throw new IllegalArgumentException(
                    "Missing required array: "
                            + key
            );
        }

        return json.getAsJsonArray(
                key
        );
    }

    private static String requiredString(
            JsonObject json,
            String key
    ) {
        if (!json.has(key)) {
            throw new IllegalArgumentException(
                    "Missing required field: "
                            + key
            );
        }

        return json.get(key)
                .getAsString();
    }

    private static String optionalString(
            JsonObject json,
            String key,
            String fallback
    ) {
        return json.has(key)
                ? json.get(key)
                .getAsString()
                : fallback;
    }

    private static int optionalInt(
            JsonObject json,
            String key,
            int fallback
    ) {
        return json.has(key)
                ? json.get(key)
                .getAsInt()
                : fallback;
    }

    private static double optionalDouble(
            JsonObject json,
            String key,
            double fallback
    ) {
        return json.has(key)
                ? json.get(key)
                .getAsDouble()
                : fallback;
    }
}
