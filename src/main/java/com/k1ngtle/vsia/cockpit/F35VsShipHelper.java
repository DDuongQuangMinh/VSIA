package com.k1ngtle.vsia.cockpit;

import java.lang.reflect.Method;
import java.util.Optional;
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
            -2.72;

    private F35VsShipHelper() {
    }

    public static ShipSnapshot shipSnapshot(
            BlockEntity blockEntity
    ) {
        if (blockEntity.getLevel() == null) {
            return ShipSnapshot.none(
                    Vec3.atCenterOf(blockEntity.getBlockPos())
            );
        }

        Level level =
                blockEntity.getLevel();

        BlockPos pos =
                blockEntity.getBlockPos();

        Object ship =
                findManagingShip(level, pos);

        if (ship == null) {
            return ShipSnapshot.none(
                    Vec3.atCenterOf(pos)
            );
        }

        Vec3 centerLocal =
                Vec3.atCenterOf(pos);

        Vec3 worldCenter =
                transformShipToWorld(
                        ship,
                        centerLocal
                );

        Vec3 velocity =
                readVelocity(ship);

        Orientation orientation =
                readOrientation(ship);

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
                                        0.18
                                )
                        )
                        .add(
                                right.scale(
                                        0.0
                                )
                        );

        ShipSnapshot snapshot =
                shipSnapshot(blockEntity);

        if (!snapshot.detected()) {
            return local;
        }

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
        return facing.getOpposite().toYRot();
    }

    @Nullable
    private static Object findManagingShip(
            Level level,
            BlockPos pos
    ) {
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

    private static Optional<Object> tryInvokeManagingShip(
            Class<?> utilsClass,
            String methodName,
            Level level,
            BlockPos pos
    ) {
        for (Method method :
                utilsClass.getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }

            Class<?>[] params =
                    method.getParameterTypes();

            try {
                Object result;

                if (params.length == 2
                        && Level.class.isAssignableFrom(params[0])
                        && BlockPos.class.isAssignableFrom(params[1])) {
                    result =
                            method.invoke(
                                    null,
                                    level,
                                    pos
                            );
                } else if (params.length == 4
                        && Level.class.isAssignableFrom(params[0])) {
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
                    return Optional.of(result);
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
                : String.valueOf(value);
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
