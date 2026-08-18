package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.FirewallBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.StorageServerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class WifiEngineeringTargetResolver {
    public static final int MAX_HOPS =
            12;

    private WifiEngineeringTargetResolver() {
    }

    public static WifiEngineeringResolution resolve(
            Level level,
            BlockPos requestedPos
    ) {
        if (level == null
                || requestedPos == null) {
            return WifiEngineeringResolution.failure(
                    "Missing level or target position"
            );
        }

        BlockPos start =
                requestedPos.immutable();

        BlockEntity startEntity =
                level.getBlockEntity(
                        start
                );

        if (startEntity == null) {
            return WifiEngineeringResolution.failure(
                    describeUnsupported(
                            level,
                            start,
                            null
                    )
            );
        }

        if (startEntity
                instanceof NetworkDeviceBlockEntity direct
                && WifiEngineeringProbe.supports(
                direct
        )) {
            return WifiEngineeringResolution.success(
                    new WifiEngineeringResolvedTarget(
                            start,
                            start,
                            direct,
                            0,
                            "direct"
                    )
            );
        }

        Queue<BlockPos> queue =
                new ArrayDeque<>();

        Map<BlockPos, Integer> depth =
                new HashMap<>();

        Map<BlockPos, BlockPos> previous =
                new HashMap<>();

        Set<BlockPos> visited =
                new HashSet<>();

        queue.add(
                start
        );

        depth.put(
                start,
                0
        );

        visited.add(
                start
        );

        while (!queue.isEmpty()) {
            BlockPos current =
                    queue.remove();

            int currentDepth =
                    depth.getOrDefault(
                            current,
                            0
                    );

            if (currentDepth > 0) {
                BlockEntity blockEntity =
                        level.getBlockEntity(
                                current
                        );

                if (blockEntity
                        instanceof NetworkDeviceBlockEntity device
                        && WifiEngineeringProbe.supports(
                        device
                )) {
                    return WifiEngineeringResolution.success(
                            new WifiEngineeringResolvedTarget(
                                    start,
                                    current,
                                    device,
                                    currentDepth,
                                    route(
                                            start,
                                            current,
                                            previous
                                    )
                            )
                    );
                }
            }

            if (currentDepth >= MAX_HOPS) {
                continue;
            }

            BlockEntity currentEntity =
                    level.getBlockEntity(
                            current
                    );

            for (BlockPos neighbor
                    : neighbors(
                    currentEntity
            )) {
                if (neighbor == null) {
                    continue;
                }

                BlockPos immutable =
                        neighbor.immutable();

                if (visited.add(
                        immutable
                )) {
                    previous.put(
                            immutable,
                            current
                    );

                    depth.put(
                            immutable,
                            currentDepth + 1
                    );

                    queue.add(
                            immutable
                    );
                }
            }
        }

        return WifiEngineeringResolution.failure(
                describeUnsupported(
                        level,
                        start,
                        startEntity
                )
                        + " | No reachable Wi-Fi NetworkDeviceBlockEntity found within "
                        + MAX_HOPS
                        + " infrastructure hops"
        );
    }

    private static List<BlockPos> neighbors(
            BlockEntity blockEntity
    ) {
        if (blockEntity
                instanceof NetworkSwitchBlockEntity networkSwitch) {
            return List.copyOf(
                    networkSwitch.getConnectedDevices()
            );
        }

        if (blockEntity
                instanceof FirewallBlockEntity firewall) {
            return List.copyOf(
                    firewall.getConnectedDevices()
            );
        }

        if (blockEntity
                instanceof StorageServerBlockEntity storage) {
            BlockPos rack =
                    storage.getConnectedRackPos();

            return rack == null
                    ? List.of()
                    : List.of(
                            rack
                    );
        }

        return List.of();
    }

    private static String describeUnsupported(
            Level level,
            BlockPos pos,
            BlockEntity blockEntity
    ) {
        String blockId =
                BuiltInRegistries.BLOCK
                        .getKey(
                                level.getBlockState(
                                        pos
                                )
                                        .getBlock()
                        )
                        .toString();

        String entity =
                blockEntity == null
                        ? "none"
                        : blockEntity.getClass()
                        .getName();

        return "Target "
                + pos.toShortString()
                + " | block="
                + blockId
                + " | blockEntity="
                + entity;
    }

    private static String route(
            BlockPos start,
            BlockPos target,
            Map<BlockPos, BlockPos> previous
    ) {
        ArrayDeque<BlockPos> route =
                new ArrayDeque<>();

        BlockPos cursor =
                target;

        route.addFirst(
                cursor
        );

        while (!cursor.equals(
                start
        )) {
            cursor =
                    previous.get(
                            cursor
                    );

            if (cursor == null) {
                break;
            }

            route.addFirst(
                    cursor
            );
        }

        StringBuilder builder =
                new StringBuilder();

        boolean first =
                true;

        for (BlockPos pos : route) {
            if (!first) {
                builder.append(
                        " -> "
                );
            }

            builder.append(
                    pos.toShortString()
            );

            first =
                    false;
        }

        return builder.toString();
    }
}
