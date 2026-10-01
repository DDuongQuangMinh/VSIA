package com.k1ngtle.vsia.cockpit;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4dc;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class F35VsShipHelper {
    private static final double SEAT_OFFSET_Y =
            -2.22;

    private static final double SEAT_OFFSET_FORWARD =
            0.43;

    private F35VsShipHelper() {
    }

    public static ShipSnapshot shipSnapshot(
            BlockEntity blockEntity
    ) {
        if (blockEntity.getLevel() == null) {
            return ShipSnapshot.none(
                    Vec3.atCenterOf(
                            blockEntity.getBlockPos()
                    )
            );
        }

        Object ship =
                findManagingShip(
                        blockEntity.getLevel(),
                        blockEntity.getBlockPos()
                );

        if (ship == null) {
            return ShipSnapshot.none(
                    Vec3.atCenterOf(
                            blockEntity.getBlockPos()
                    )
            );
        }

        return snapshotFromShip(
                ship,
                Vec3.atCenterOf(
                        blockEntity.getBlockPos()
                )
        );
    }

    public static ShipBounds shipLocalBounds(
            BlockEntity blockEntity
    ) {
        if (blockEntity.getLevel() == null) {
            return ShipBounds.empty();
        }

        Object ship =
                findManagingShip(
                        blockEntity.getLevel(),
                        blockEntity.getBlockPos()
                );

        return ship == null
                ? ShipBounds.empty()
                : readShipBounds(
                ship
        );
    }

    public static List<ShipSnapshot> loadedShips(
            Level level
    ) {
        Object shipWorld =
                getShipObjectWorld(
                        level
                );

        if (shipWorld == null) {
            return List.of();
        }

        Object loadedShips =
                invokeNoArg(
                        shipWorld,
                        "getLoadedShips"
                );

        if (loadedShips == null) {
            return List.of();
        }

        List<Object> rawShips =
                extractObjects(
                        loadedShips
                );

        if (rawShips.isEmpty()) {
            return List.of();
        }

        List<ShipSnapshot> snapshots =
                new ArrayList<>(
                        rawShips.size()
                );

        for (Object rawShip :
                rawShips) {
            if (rawShip == null) {
                continue;
            }

            snapshots.add(
                    snapshotFromShip(
                            rawShip,
                            null
                    )
            );
        }

        return List.copyOf(
                snapshots
        );
    }

    public static UUID shipContactId(
            long shipId
    ) {
        return UUID.nameUUIDFromBytes(
                (
                        "vsia:vs-ship:"
                                + shipId
                ).getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }

    public static Vec3 seatWorldPosition(
            BlockEntity blockEntity,
            Direction facing
    ) {
        Vec3 center =
                Vec3.atCenterOf(
                        blockEntity.getBlockPos()
                );

        Vec3 forward =
                new Vec3(
                        facing.getStepX(),
                        0.0,
                        facing.getStepZ()
                );

        Vec3 right =
                new Vec3(
                        -facing.getStepZ(),
                        0.0,
                        facing.getStepX()
                );

        Vec3 local =
                center
                        .add(
                                0.0,
                                SEAT_OFFSET_Y,
                                0.0
                        )
                        .add(
                                forward.scale(
                                        SEAT_OFFSET_FORWARD
                                )
                        )
                        .add(
                                right.scale(
                                        0.0
                                )
                        );

        Object ship =
                findManagingShip(
                        blockEntity.getLevel(),
                        blockEntity.getBlockPos()
                );

        if (ship == null) {
            return local;
        }

        return transformShipToWorld(
                ship,
                local
        );
    }

    public static float seatViewYaw(
            Direction facing
    ) {
        return facing.getOpposite()
                .toYRot();
    }

    public static double cockpitHeadingDeg(
            F35CockpitSeatBlockEntity cockpit
    ) {
        Direction forwardFacing =
                cockpit.getBlockState()
                        .getValue(
                                F35CockpitSeatBlock.FACING
                        )
                        .getOpposite();

        Vec3 localCenter =
                Vec3.atCenterOf(
                        cockpit.getBlockPos()
                );

        Vec3 localForward =
                localCenter.add(
                        forwardFacing.getStepX(),
                        0.0,
                        forwardFacing.getStepZ()
                );

        Object ship =
                findManagingShip(
                        cockpit.getLevel(),
                        cockpit.getBlockPos()
                );

        Vec3 worldCenter =
                localCenter;

        Vec3 worldForward =
                localForward;

        if (ship != null) {
            worldCenter =
                    transformShipToWorld(
                            ship,
                            localCenter
                    );

            worldForward =
                    transformShipToWorld(
                            ship,
                            localForward
                    );
        }

        double dx =
                worldForward.x
                        - worldCenter.x;

        double dz =
                worldForward.z
                        - worldCenter.z;

        if (dx * dx + dz * dz < 1.0E-12) {
            return normalizeDegrees(
                    forwardFacing.toYRot()
            );
        }

        return normalizeDegrees(
                Math.toDegrees(
                        Math.atan2(
                                -dx,
                                dz
                        )
                )
        );
    }

    private static ShipSnapshot snapshotFromShip(
            Object ship,
            @Nullable Vec3 knownLocalPoint
    ) {
        ShipBounds bounds =
                readShipBounds(
                        ship
                );

        Vec3 localCenter;

        if (bounds.available()) {
            localCenter =
                    new Vec3(
                            (
                                    bounds.minX()
                                            + bounds.maxX()
                                            + 1.0
                            ) / 2.0,
                            (
                                    bounds.minY()
                                            + bounds.maxY()
                                            + 1.0
                            ) / 2.0,
                            (
                                    bounds.minZ()
                                            + bounds.maxZ()
                                            + 1.0
                            ) / 2.0
                    );
        } else if (knownLocalPoint != null) {
            localCenter =
                    knownLocalPoint;
        } else {
            localCenter =
                    Vec3.ZERO;
        }

        Vec3 worldCenter =
                transformShipToWorld(
                        ship,
                        localCenter
                );

        if (!bounds.available()
                && knownLocalPoint == null) {
            Vec3 transformPosition =
                    readTransformWorldPosition(
                            ship
                    );

            if (transformPosition != null) {
                worldCenter =
                        transformPosition;
            }
        }

        Vec3 velocity =
                readVelocity(
                        ship
                );

        Orientation orientation =
                readOrientation(
                        ship
                );

        long shipId =
                readLong(
                        ship,
                        "getId",
                        0L
                );

        String slug =
                readString(
                        ship,
                        "getSlug",
                        "VS-SHIP"
                );

        return new ShipSnapshot(
                true,
                shipId,
                slug,
                worldCenter,
                velocity,
                orientation.headingDeg(),
                orientation.pitchDeg(),
                orientation.rollDeg()
        );
    }

    @Nullable
    private static Object findManagingShip(
            @Nullable Level level,
            BlockPos pos
    ) {
        if (level == null) {
            return null;
        }

        try {
            Class<?> utilsClass =
                    Class.forName(
                            "org.valkyrienskies.mod.common.VSGameUtilsKt"
                    );

            for (String methodName :
                    new String[]{
                            "getShipManagingPos",
                            "getShipObjectManagingPos"
                    }) {
                Optional<Object> result =
                        tryInvokeManagingShip(
                                utilsClass,
                                methodName,
                                level,
                                pos
                        );

                if (result.isPresent()) {
                    return result.get();
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    @Nullable
    private static Object getShipObjectWorld(
            Level level
    ) {
        try {
            Class<?> utilsClass =
                    Class.forName(
                            "org.valkyrienskies.mod.common.VSGameUtilsKt"
                    );

            for (Method method :
                    utilsClass.getMethods()) {
                if (!method.getName()
                        .equals(
                                "getShipObjectWorld"
                        )) {
                    continue;
                }

                Class<?>[] params =
                        method.getParameterTypes();

                if (params.length == 1
                        && params[0]
                        .isAssignableFrom(
                                level.getClass()
                        )) {
                    Object value =
                            method.invoke(
                                    null,
                                    level
                            );

                    if (value != null) {
                        return value;
                    }
                }

                if (params.length == 1
                        && Level.class
                        .isAssignableFrom(
                                params[0]
                        )) {
                    Object value =
                            method.invoke(
                                    null,
                                    level
                            );

                    if (value != null) {
                        return value;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Optional<Object> tryInvokeManagingShip(
            Class<?> utilsClass,
            String methodName,
            Level level,
            BlockPos pos
    ) {
        for (Method method :
                utilsClass.getMethods()) {
            if (!method.getName()
                    .equals(
                            methodName
                    )) {
                continue;
            }

            Class<?>[] params =
                    method.getParameterTypes();

            try {
                Object result;

                if (params.length == 2
                        && Level.class
                        .isAssignableFrom(
                                params[0]
                        )
                        && BlockPos.class
                        .isAssignableFrom(
                                params[1]
                        )) {
                    result =
                            method.invoke(
                                    null,
                                    level,
                                    pos
                            );
                } else if (params.length == 4
                        && Level.class
                        .isAssignableFrom(
                                params[0]
                        )) {
                    result =
                            method.invoke(
                                    null,
                                    level,
                                    pos.getX(),
                                    pos.getY(),
                                    pos.getZ()
                            );
                } else {
                    continue;
                }

                if (result != null) {
                    return Optional.of(
                            result
                    );
                }
            } catch (Throwable ignored) {
            }
        }

        return Optional.empty();
    }

    private static Vec3 transformShipToWorld(
            Object ship,
            Vec3 localPos
    ) {
        try {
            Object transform =
                    invokeNoArg(
                            ship,
                            "getTransform"
                    );

            if (transform == null) {
                return localPos;
            }

            Object matrixObject =
                    invokeNoArg(
                            transform,
                            "getShipToWorld"
                    );

            if (matrixObject instanceof Matrix4dc matrix) {
                Vector3d transformed =
                        matrix.transformPosition(
                                new Vector3d(
                                        localPos.x,
                                        localPos.y,
                                        localPos.z
                                )
                        );

                return new Vec3(
                        transformed.x,
                        transformed.y,
                        transformed.z
                );
            }
        } catch (Throwable ignored) {
        }

        return localPos;
    }

    @Nullable
    private static Vec3 readTransformWorldPosition(
            Object ship
    ) {
        try {
            Object transform =
                    invokeNoArg(
                            ship,
                            "getTransform"
                    );

            if (transform == null) {
                return null;
            }

            for (String methodName :
                    new String[]{
                            "getPositionInWorld",
                            "getWorldPosition"
                    }) {
                Object value =
                        invokeNoArg(
                                transform,
                                methodName
                        );

                if (value instanceof Vector3dc vector) {
                    return new Vec3(
                            vector.x(),
                            vector.y(),
                            vector.z()
                    );
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Vec3 readVelocity(
            Object ship
    ) {
        try {
            Object velocity =
                    invokeNoArg(
                            ship,
                            "getVelocity"
                    );

            if (velocity instanceof Vector3dc vector) {
                return new Vec3(
                        vector.x(),
                        vector.y(),
                        vector.z()
                );
            }
        } catch (Throwable ignored) {
        }

        return Vec3.ZERO;
    }

    private static Orientation readOrientation(
            Object ship
    ) {
        try {
            Object transform =
                    invokeNoArg(
                            ship,
                            "getTransform"
                    );

            if (transform == null) {
                return Orientation.ZERO;
            }

            Object matrixObject =
                    invokeNoArg(
                            transform,
                            "getShipToWorld"
                    );

            if (matrixObject instanceof Matrix4dc matrix) {
                Quaterniond rotation =
                        matrix.getNormalizedRotation(
                                new Quaterniond()
                        );

                Vector3d euler =
                        rotation.getEulerAnglesYXZ(
                                new Vector3d()
                        );

                return new Orientation(
                        normalizeDegrees(
                                Math.toDegrees(
                                        euler.y
                                )
                        ),
                        Math.toDegrees(
                                euler.x
                        ),
                        Math.toDegrees(
                                euler.z
                        )
                );
            }
        } catch (Throwable ignored) {
        }

        return Orientation.ZERO;
    }

    private static ShipBounds readShipBounds(
            Object ship
    ) {
        Object aabb =
                invokeNoArg(
                        ship,
                        "getShipAABB"
                );

        if (aabb == null) {
            return ShipBounds.empty();
        }

        Integer minX =
                readIntObject(
                        aabb,
                        "minX"
                );

        Integer minY =
                readIntObject(
                        aabb,
                        "minY"
                );

        Integer minZ =
                readIntObject(
                        aabb,
                        "minZ"
                );

        Integer maxX =
                readIntObject(
                        aabb,
                        "maxX"
                );

        Integer maxY =
                readIntObject(
                        aabb,
                        "maxY"
                );

        Integer maxZ =
                readIntObject(
                        aabb,
                        "maxZ"
                );

        if (minX == null
                || minY == null
                || minZ == null
                || maxX == null
                || maxY == null
                || maxZ == null) {
            return ShipBounds.empty();
        }

        return new ShipBounds(
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ
        );
    }

    private static List<Object> extractObjects(
            Object container
    ) {
        List<Object> result =
                new ArrayList<>();

        if (container instanceof Map<?, ?> map) {
            result.addAll(
                    map.values()
            );
            return result;
        }

        if (container instanceof Collection<?> collection) {
            result.addAll(
                    collection
            );
            return result;
        }

        if (container instanceof Iterable<?> iterable) {
            for (Object value :
                    iterable) {
                result.add(
                        value
                );
            }
            return result;
        }

        for (String methodName :
                new String[]{
                        "values",
                        "getValues",
                        "getShips",
                        "getAll"
                }) {
            Object nested =
                    invokeNoArg(
                            container,
                            methodName
                    );

            if (nested == null
                    || nested == container) {
                continue;
            }

            List<Object> nestedValues =
                    extractObjects(
                            nested
                    );

            if (!nestedValues.isEmpty()) {
                return nestedValues;
            }
        }

        try {
            Method iteratorMethod =
                    container.getClass()
                            .getMethod(
                                    "iterator"
                            );

            Object iteratorObject =
                    iteratorMethod.invoke(
                            container
                    );

            if (iteratorObject instanceof Iterator<?> iterator) {
                while (iterator.hasNext()) {
                    result.add(
                            iterator.next()
                    );
                }
            }
        } catch (Throwable ignored) {
        }

        return result;
    }

    @Nullable
    private static Object invokeNoArg(
            Object target,
            String methodName
    ) {
        try {
            Method method =
                    target.getClass()
                            .getMethod(
                                    methodName
                            );

            return method.invoke(
                    target
            );
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static long readLong(
            Object target,
            String methodName,
            long fallback
    ) {
        Object value =
                invokeNoArg(
                        target,
                        methodName
                );

        if (value instanceof Number number) {
            return number.longValue();
        }

        return fallback;
    }

    @Nullable
    private static Integer readIntObject(
            Object target,
            String methodName
    ) {
        Object value =
                invokeNoArg(
                        target,
                        methodName
                );

        if (value instanceof Number number) {
            return number.intValue();
        }

        return null;
    }

    private static String readString(
            Object target,
            String methodName,
            String fallback
    ) {
        Object value =
                invokeNoArg(
                        target,
                        methodName
                );

        return value == null
                ? fallback
                : String.valueOf(
                        value
                );
    }

    private static double normalizeDegrees(
            double value
    ) {
        double normalized =
                value % 360.0;

        if (normalized < 0.0) {
            normalized += 360.0;
        }

        return normalized;
    }

    public record ShipSnapshot(
            boolean detected,
            long shipId,
            String shipSlug,
            Vec3 worldCenter,
            Vec3 velocity,
            double headingDeg,
            double pitchDeg,
            double rollDeg
    ) {
        public static ShipSnapshot none(
                Vec3 worldCenter
        ) {
            return new ShipSnapshot(
                    false,
                    0L,
                    "NO-SHIP",
                    worldCenter,
                    Vec3.ZERO,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    public record ShipBounds(
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ
    ) {
        public static ShipBounds empty() {
            return new ShipBounds(
                    0,
                    0,
                    0,
                    -1,
                    -1,
                    -1
            );
        }

        public boolean available() {
            return maxX >= minX
                    && maxY >= minY
                    && maxZ >= minZ;
        }

        public int sizeX() {
            return available()
                    ? maxX - minX + 1
                    : 0;
        }

        public int sizeY() {
            return available()
                    ? maxY - minY + 1
                    : 0;
        }

        public int sizeZ() {
            return available()
                    ? maxZ - minZ + 1
                    : 0;
        }
    }

    private record Orientation(
            double headingDeg,
            double pitchDeg,
            double rollDeg
    ) {
        private static final Orientation ZERO =
                new Orientation(
                        0.0,
                        0.0,
                        0.0
                );
    }
}
