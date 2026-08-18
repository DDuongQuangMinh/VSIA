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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class WifiEngineeringTargetResolver {
    public static final int MAX_HOPS =
            12;

    public static final int MODEL_SEARCH_HORIZONTAL_RADIUS =
            4;

    public static final int MODEL_SEARCH_VERTICAL_RADIUS =
            3;

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

        BlockPos requested =
                requestedPos.immutable();

        AnchorResult anchorResult =
                resolveLogicalAnchor(
                        level,
                        requested
                );

        if (anchorResult == null) {
            return WifiEngineeringResolution.failure(
                    describeUnsupported(
                            level,
                            requested,
                            level.getBlockEntity(
                                    requested
                            )
                    )
                            + " | No nearby VSIA network infrastructure block entity found within "
                            + MODEL_SEARCH_HORIZONTAL_RADIUS
                            + " horizontal and "
                            + MODEL_SEARCH_VERTICAL_RADIUS
                            + " vertical blocks"
            );
        }

        BlockPos start =
                anchorResult.pos();

        BlockEntity startEntity =
                anchorResult.blockEntity();

        if (startEntity
                instanceof NetworkDeviceBlockEntity direct
                && WifiEngineeringProbe.supports(
                direct
        )) {
            return WifiEngineeringResolution.success(
                    new WifiEngineeringResolvedTarget(
                            requested,
                            start,
                            direct,
                            0,
                            anchorResult.direct()
                                    ? "direct"
                                    : "model-anchor "
                                    + requested.toShortString()
                                    + " -> "
                                    + start.toShortString()
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
                    String topologyRoute =
                            route(
                                    start,
                                    current,
                                    previous
                            );

                    String fullRoute =
                            anchorResult.direct()
                                    ? topologyRoute
                                    : requested.toShortString()
                                    + " ~> "
                                    + topologyRoute;

                    return WifiEngineeringResolution.success(
                            new WifiEngineeringResolvedTarget(
                                    requested,
                                    current,
                                    device,
                                    currentDepth,
                                    fullRoute
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

                BlockPos normalizedNeighbor =
                        normalizeConnectedPosition(
                                level,
                                neighbor
                        );

                if (normalizedNeighbor == null) {
                    continue;
                }

                BlockPos immutable =
                        normalizedNeighbor.immutable();

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
                        requested,
                        level.getBlockEntity(
                                requested
                        )
                )
                        + " | normalizedAnchor="
                        + start.toShortString()
                        + " | normalizedBlockEntity="
                        + startEntity.getClass()
                        .getName()
                        + " | No reachable Wi-Fi NetworkDeviceBlockEntity found within "
                        + MAX_HOPS
                        + " infrastructure hops"
        );
    }

    private static AnchorResult resolveLogicalAnchor(
            Level level,
            BlockPos requested
    ) {
        BlockEntity exact =
                level.getBlockEntity(
                        requested
                );

        if (isInfrastructure(
                exact
        )) {
            return new AnchorResult(
                    requested,
                    exact,
                    true,
                    0
            );
        }

        List<AnchorResult> candidates =
                new ArrayList<>();

        for (int dy = -MODEL_SEARCH_VERTICAL_RADIUS;
             dy <= MODEL_SEARCH_VERTICAL_RADIUS;
             dy++) {
            for (int dx = -MODEL_SEARCH_HORIZONTAL_RADIUS;
                 dx <= MODEL_SEARCH_HORIZONTAL_RADIUS;
                 dx++) {
                for (int dz = -MODEL_SEARCH_HORIZONTAL_RADIUS;
                     dz <= MODEL_SEARCH_HORIZONTAL_RADIUS;
                     dz++) {
                    if (dx == 0
                            && dy == 0
                            && dz == 0) {
                        continue;
                    }

                    BlockPos candidatePos =
                            requested.offset(
                                    dx,
                                    dy,
                                    dz
                            );

                    BlockEntity candidateEntity =
                            level.getBlockEntity(
                                    candidatePos
                            );

                    if (!isInfrastructure(
                            candidateEntity
                    )) {
                        continue;
                    }

                    candidates.add(
                            new AnchorResult(
                                    candidatePos,
                                    candidateEntity,
                                    false,
                                    distanceScore(
                                            dx,
                                            dy,
                                            dz,
                                            candidateEntity
                                    )
                            )
                    );
                }
            }
        }

        return candidates.stream()
                .min(
                        Comparator
                                .comparingInt(
                                        AnchorResult::score
                                )
                                .thenComparingInt(
                                        value ->
                                                typePriority(
                                                        value.blockEntity()
                                                )
                                )
                                .thenComparingInt(
                                        value ->
                                                Math.abs(
                                                        value.pos()
                                                                .getY()
                                                                - requested.getY()
                                                )
                                )
                )
                .orElse(
                        null
                );
    }

    private static BlockPos normalizeConnectedPosition(
            Level level,
            BlockPos pos
    ) {
        BlockEntity exact =
                level.getBlockEntity(
                        pos
                );

        if (isInfrastructure(
                exact
        )) {
            return pos;
        }

        AnchorResult nearby =
                findVeryNearAnchor(
                        level,
                        pos
                );

        return nearby == null
                ? null
                : nearby.pos();
    }

    private static AnchorResult findVeryNearAnchor(
            Level level,
            BlockPos requested
    ) {
        AnchorResult best =
                null;

        for (int dy = -1;
             dy <= 1;
             dy++) {
            for (int dx = -1;
                 dx <= 1;
                 dx++) {
                for (int dz = -1;
                     dz <= 1;
                     dz++) {
                    BlockPos candidatePos =
                            requested.offset(
                                    dx,
                                    dy,
                                    dz
                            );

                    BlockEntity candidateEntity =
                            level.getBlockEntity(
                                    candidatePos
                            );

                    if (!isInfrastructure(
                            candidateEntity
                    )) {
                        continue;
                    }

                    int score =
                            distanceScore(
                                    dx,
                                    dy,
                                    dz,
                                    candidateEntity
                            );

                    AnchorResult candidate =
                            new AnchorResult(
                                    candidatePos,
                                    candidateEntity,
                                    dx == 0
                                            && dy == 0
                                            && dz == 0,
                                    score
                            );

                    if (best == null
                            || candidate.score()
                            < best.score()
                            || (
                            candidate.score()
                                    == best.score()
                                    && typePriority(
                                    candidate.blockEntity()
                            )
                                    < typePriority(
                                    best.blockEntity()
                            )
                    )) {
                        best =
                                candidate;
                    }
                }
            }
        }

        return best;
    }

    private static boolean isInfrastructure(
            BlockEntity blockEntity
    ) {
        return blockEntity
                instanceof NetworkDeviceBlockEntity
                || blockEntity
                instanceof NetworkSwitchBlockEntity
                || blockEntity
                instanceof FirewallBlockEntity
                || blockEntity
                instanceof StorageServerBlockEntity;
    }

    private static int distanceScore(
            int dx,
            int dy,
            int dz,
            BlockEntity blockEntity
    ) {
        int horizontal =
                dx * dx
                        + dz * dz;

        int vertical =
                dy * dy;

        return horizontal * 100
                + vertical * 125
                + typePriority(
                blockEntity
        );
    }

    private static int typePriority(
            BlockEntity blockEntity
    ) {
        if (blockEntity
                instanceof NetworkDeviceBlockEntity device
                && WifiEngineeringProbe.supports(
                device
        )) {
            return 0;
        }

        if (blockEntity
                instanceof NetworkSwitchBlockEntity) {
            return 1;
        }

        if (blockEntity
                instanceof FirewallBlockEntity) {
            return 2;
        }

        if (blockEntity
                instanceof StorageServerBlockEntity) {
            return 3;
        }

        if (blockEntity
                instanceof NetworkDeviceBlockEntity) {
            return 4;
        }

        return 100;
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

    private record AnchorResult(
            BlockPos pos,
            BlockEntity blockEntity,
            boolean direct,
            int score
    ) {
        private AnchorResult {
            pos =
                    pos.immutable();
        }
    }
}
