package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class W1191LocalTargetAuthorization {
    private static final String RESOLVER_CLASS =
            "com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTargetResolver";

    private static final int MODEL_HORIZONTAL_RADIUS = 4;
    private static final int MODEL_VERTICAL_RADIUS = 3;

    private W1191LocalTargetAuthorization() {
    }

    public static boolean isLocalWifiEndpoint(
            ServerLevel level,
            BlockPos requestedAnchor
    ) {
        if (level == null || requestedAnchor == null) {
            return false;
        }

        Object resolution =
                invokeResolver(
                        level,
                        requestedAnchor
                );

        if (resolution == null) {
            return false;
        }

        BlockPos resolvedWifiPos =
                extractResolvedPosition(
                        resolution,
                        0
                );

        if (resolvedWifiPos == null) {
            return false;
        }

        /*
         * IMPORTANT:
         *
         * The GUI coordinate is an engineering/model anchor, not necessarily
         * the logical BlockEntity coordinate.
         *
         * W1.6.2 already normalizes a visible GeckoLib/Blockbench point such
         * as:
         *
         *   3 -60 -23
         *
         * to the actual logical NetworkDeviceBlockEntity:
         *
         *   5 -60 -23
         *
         * before topology traversal.
         *
         * Authorization therefore has to compare the resolver's final Wi-Fi
         * endpoint against the LOCAL MODEL ANCHOR, not against the raw GUI
         * coordinate.
         */
        BlockPos localModelAnchor =
                findLocalModelAnchor(
                        level,
                        requestedAnchor
                );

        if (localModelAnchor == null) {
            return false;
        }

        BlockEntity localBlockEntity =
                level.getBlockEntity(
                        localModelAnchor
                );

        /*
         * Configuration is allowed only when the local normalized physical
         * model itself is a NetworkDeviceBlockEntity AND that same device is
         * the Wi-Fi endpoint selected by the engineering resolver.
         *
         * This keeps switch/firewall/storage infrastructure views read-only:
         *
         * switch model
         *   -> localModelAnchor = switch
         *   -> NOT NetworkDeviceBlockEntity
         *   -> BLOCK
         *
         * router/server-rack Wi-Fi model
         *   -> localModelAnchor = that NetworkDeviceBlockEntity
         *   -> resolvedWifiPos = same logical position
         *   -> ALLOW
         */
        return localBlockEntity instanceof NetworkDeviceBlockEntity
                && localModelAnchor.equals(
                        resolvedWifiPos
                );
    }

    private static BlockPos findLocalModelAnchor(
            ServerLevel level,
            BlockPos requested
    ) {
        BlockEntity exact =
                level.getBlockEntity(
                        requested
                );

        if (isSupportedInfrastructure(
                exact
        )) {
            return requested;
        }

        BlockPos best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        int bestPriority = Integer.MAX_VALUE;

        for (int dy = -MODEL_VERTICAL_RADIUS;
             dy <= MODEL_VERTICAL_RADIUS;
             dy++) {
            for (int dx = -MODEL_HORIZONTAL_RADIUS;
                 dx <= MODEL_HORIZONTAL_RADIUS;
                 dx++) {
                for (int dz = -MODEL_HORIZONTAL_RADIUS;
                     dz <= MODEL_HORIZONTAL_RADIUS;
                     dz++) {
                    BlockPos candidate =
                            requested.offset(
                                    dx,
                                    dy,
                                    dz
                            );

                    BlockEntity blockEntity =
                            level.getBlockEntity(
                                    candidate
                            );

                    if (!isSupportedInfrastructure(
                            blockEntity
                    )) {
                        continue;
                    }

                    double distance =
                            requested.distSqr(
                                    candidate
                            );

                    int priority =
                            infrastructurePriority(
                                    blockEntity
                            );

                    if (distance < bestDistance
                            || (Double.compare(
                                    distance,
                                    bestDistance
                            ) == 0
                            && priority < bestPriority)) {
                        best = candidate;
                        bestDistance = distance;
                        bestPriority = priority;
                    }
                }
            }
        }

        return best;
    }

    private static boolean isSupportedInfrastructure(
            BlockEntity blockEntity
    ) {
        if (blockEntity == null) {
            return false;
        }

        if (blockEntity
                instanceof NetworkDeviceBlockEntity) {
            return true;
        }

        String simpleName =
                blockEntity.getClass()
                        .getSimpleName();

        return simpleName.equals(
                "NetworkSwitchBlockEntity"
        )
                || simpleName.equals(
                        "FirewallBlockEntity"
                )
                || simpleName.equals(
                        "StorageServerBlockEntity"
                );
    }

    private static int infrastructurePriority(
            BlockEntity blockEntity
    ) {
        if (blockEntity
                instanceof NetworkDeviceBlockEntity) {
            return 0;
        }

        String simpleName =
                blockEntity.getClass()
                        .getSimpleName();

        if (simpleName.equals(
                "NetworkSwitchBlockEntity"
        )) {
            return 1;
        }

        if (simpleName.equals(
                "FirewallBlockEntity"
        )) {
            return 2;
        }

        if (simpleName.equals(
                "StorageServerBlockEntity"
        )) {
            return 3;
        }

        return 100;
    }

    private static Object invokeResolver(
            ServerLevel level,
            BlockPos requestedAnchor
    ) {
        try {
            Class<?> resolverClass =
                    Class.forName(
                            RESOLVER_CLASS
                    );

            for (Method method
                    : resolverClass.getDeclaredMethods()) {
                if (!Modifier.isStatic(
                        method.getModifiers()
                )) {
                    continue;
                }

                if (!method.getName()
                        .equals("resolve")) {
                    continue;
                }

                Class<?>[] parameterTypes =
                        method.getParameterTypes();

                if (parameterTypes.length != 2) {
                    continue;
                }

                boolean normalOrder =
                        parameterTypes[0]
                                .isAssignableFrom(
                                        level.getClass()
                                )
                                && parameterTypes[1]
                                .isAssignableFrom(
                                        BlockPos.class
                                );

                boolean reverseOrder =
                        parameterTypes[1]
                                .isAssignableFrom(
                                        level.getClass()
                                )
                                && parameterTypes[0]
                                .isAssignableFrom(
                                        BlockPos.class
                                );

                if (!normalOrder
                        && !reverseOrder) {
                    continue;
                }

                method.setAccessible(true);

                if (normalOrder) {
                    return method.invoke(
                            null,
                            level,
                            requestedAnchor
                    );
                }

                return method.invoke(
                        null,
                        requestedAnchor,
                        level
                );
            }
        } catch (ReflectiveOperationException
                 | LinkageError ignored) {
        }

        return null;
    }

    private static BlockPos extractResolvedPosition(
            Object value,
            int depth
    ) {
        if (value == null
                || depth > 8) {
            return null;
        }

        if (value instanceof BlockPos pos) {
            return pos;
        }

        if (value instanceof BlockEntity blockEntity) {
            return blockEntity.getBlockPos();
        }

        if (value instanceof Optional<?> optional) {
            if (optional.isEmpty()) {
                return null;
            }

            return extractResolvedPosition(
                    optional.get(),
                    depth + 1
            );
        }

        if (value instanceof List<?> list) {
            for (int i = list.size() - 1;
                 i >= 0;
                 i--) {
                BlockPos pos =
                        extractResolvedPosition(
                                list.get(i),
                                depth + 1
                        );

                if (pos != null) {
                    return pos;
                }
            }

            return null;
        }

        if (value instanceof Collection<?> collection) {
            BlockPos last = null;

            for (Object item : collection) {
                BlockPos pos =
                        extractResolvedPosition(
                                item,
                                depth + 1
                        );

                if (pos != null) {
                    last = pos;
                }
            }

            return last;
        }

        String[] preferredMethods = {
                "resolvedPosition",
                "resolvedPos",
                "position",
                "blockPos",
                "pos",
                "targetPosition",
                "targetPos",
                "resolvedTarget",
                "target",
                "endpoint",
                "device",
                "resolved",
                "result",
                "path"
        };

        for (String methodName
                : preferredMethods) {
            Object nested =
                    invokeZeroArg(
                            value,
                            methodName
                    );

            if (nested == null
                    || nested == value) {
                continue;
            }

            BlockPos pos =
                    extractResolvedPosition(
                            nested,
                            depth + 1
                    );

            if (pos != null) {
                return pos;
            }
        }

        return null;
    }

    private static Object invokeZeroArg(
            Object target,
            String methodName
    ) {
        if (target == null) {
            return null;
        }

        try {
            Method method =
                    target.getClass()
                            .getMethod(
                                    methodName
                            );

            if (method.getParameterCount()
                    != 0) {
                return null;
            }

            method.setAccessible(true);

            return method.invoke(
                    target
            );
        } catch (ReflectiveOperationException
                 | RuntimeException ignored) {
            return null;
        }
    }
}
