package com.k1ngtle.vsia.signality.engineering.channel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public final class MaterialAttenuationModel {
    private MaterialAttenuationModel() {
    }

    public static double estimateLossDb(
            ServerLevel level,
            Vec3 transmitter,
            Vec3 receiver,
            double frequencyHz
    ) {
        if (!RfChannelSettings.ENABLE_MATERIAL_ATTENUATION) {
            return 0.0;
        }

        double distance =
                transmitter.distanceTo(
                        receiver
                );

        if (distance <= 0.0) {
            return 0.0;
        }

        int maxSamples =
                Math.max(
                        1,
                        RfChannelSettings.MATERIAL_RAY_MAX_SAMPLES
                );

        int samples =
                Math.min(
                        maxSamples,
                        Math.max(
                                1,
                                (int) Math.ceil(
                                        distance
                                                / Math.max(
                                                0.1,
                                                RfChannelSettings.MATERIAL_RAY_MIN_STEP_BLOCKS
                                        )
                                )
                        )
                );

        Set<Long> visited =
                new HashSet<>();

        double totalLossDb =
                0.0;

        for (int i = 1;
             i < samples;
             i++) {
            double t =
                    i
                            / (double) samples;

            Vec3 point =
                    transmitter.lerp(
                            receiver,
                            t
                    );

            BlockPos pos =
                    BlockPos.containing(
                            point.x,
                            point.y,
                            point.z
                    );

            if (!visited.add(
                    pos.asLong()
            )) {
                continue;
            }

            BlockState state =
                    level.getBlockState(
                            pos
                    );

            if (state.isAir()) {
                continue;
            }

            ResourceLocation id =
                    BuiltInRegistries.BLOCK.getKey(
                            state.getBlock()
                    );

            totalLossDb +=
                    lossForBlock(
                            id == null
                                    ? ""
                                    : id.toString(),
                            frequencyHz
                    );
        }

        return totalLossDb;
    }

    public static double lossForBlock(
            String blockId,
            double frequencyHz
    ) {
        String id =
                blockId == null
                        ? ""
                        : blockId.toLowerCase();

        double baseDb;

        if (id.contains("glass")) {
            baseDb = 1.5;
        } else if (id.contains("leaves")
                || id.contains("wool")) {
            baseDb = 1.0;
        } else if (id.contains("planks")
                || id.contains("log")
                || id.contains("wood")) {
            baseDb = 2.0;
        } else if (id.contains("brick")) {
            baseDb = 4.0;
        } else if (id.contains("concrete")) {
            baseDb = 7.0;
        } else if (id.contains("iron")
                || id.contains("copper")
                || id.contains("gold")
                || id.contains("netherite")
                || id.contains("metal")) {
            baseDb = 12.0;
        } else if (id.contains("water")
                || id.contains("ice")) {
            baseDb = 8.0;
        } else if (id.contains("stone")
                || id.contains("deepslate")
                || id.contains("obsidian")) {
            baseDb = 5.0;
        } else if (id.contains("dirt")
                || id.contains("sand")
                || id.contains("gravel")) {
            baseDb = 3.0;
        } else {
            baseDb = 2.5;
        }

        double ghz =
                Math.max(
                        0.03,
                        frequencyHz
                                / 1_000_000_000.0
                );

        double frequencyScale =
                Math.max(
                        0.45,
                        Math.min(
                                2.5,
                                Math.sqrt(
                                        ghz
                                )
                        )
                );

        return baseDb
                * frequencyScale;
    }
}
