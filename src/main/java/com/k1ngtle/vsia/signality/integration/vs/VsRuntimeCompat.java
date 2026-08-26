package com.k1ngtle.vsia.signality.integration.vs;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBdc;
import org.joml.primitives.AABBic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class VsRuntimeCompat {
    private static final String VS_GAME_UTILS =
            "org.valkyrienskies.mod.common.VSGameUtilsKt";

    private static final String VS_MOD =
            "org.valkyrienskies.mod.common.ValkyrienSkiesMod";

    private static final String LEGACY_FORCE_APPLIER =
            "org.valkyrienskies.mod.common.util.GameTickForceApplier";

    private static final Object MISSING =
            new Object();

    private VsRuntimeCompat() {
    }

    public static Object findShipManagingPos(
            ServerLevel level,
            BlockPos pos
    ) {
        Object loaded =
                invokeStaticIfPresent(
                        VS_GAME_UTILS,
                        "getLoadedShipManagingPos",
                        level,
                        pos
                );

        if (loaded != MISSING) {
            return loaded;
        }

        Object legacy =
                invokeStaticIfPresent(
                        VS_GAME_UTILS,
                        "getShipManagingPos",
                        level,
                        pos
                );

        return legacy == MISSING
                ? null
                : legacy;
    }

    public static Object findLoadedShipById(
            ServerLevel level,
            long shipId
    ) {
        Object world =
                invokeStaticIfPresent(
                        VS_GAME_UTILS,
                        "getShipObjectWorld",
                        level
                );

        if (world == MISSING
                || world == null) {
            return null;
        }

        Object loadedShips =
                invokeInstanceIfPresent(
                        world,
                        "getLoadedShips"
                );

        if (loadedShips == MISSING
                || loadedShips == null) {
            return null;
        }

        Object ship =
                invokeInstanceIfPresent(
                        loadedShips,
                        "getById",
                        shipId
                );

        return ship == MISSING
                ? null
                : ship;
    }

    public static Stream<Object> loadedShips(
            ServerLevel level
    ) {
        Object world =
                invokeStaticIfPresent(
                        VS_GAME_UTILS,
                        "getShipObjectWorld",
                        level
                );

        if (world == MISSING
                || world == null) {
            return Stream.empty();
        }

        Object loadedShips =
                invokeInstanceIfPresent(
                        world,
                        "getLoadedShips"
                );

        if (loadedShips == MISSING
                || loadedShips == null) {
            return Stream.empty();
        }

        if (loadedShips instanceof Iterable<?> iterable) {
            return StreamSupport.stream(
                            iterable.spliterator(),
                            false
                    )
                    .map(value -> value);
        }

        return Stream.empty();
    }

    public static long shipId(
            Object ship
    ) {
        Object value =
                invokeInstanceIfPresent(
                        ship,
                        "getId"
                );

        return value instanceof Number number
                ? number.longValue()
                : -1L;
    }

    public static Vector3dc shipWorldPosition(
            Object ship
    ) {
        Object transform =
                invokeInstanceIfPresent(
                        ship,
                        "getTransform"
                );

        if (transform != MISSING
                && transform != null) {
            Object position =
                    invokeInstanceIfPresent(
                            transform,
                            "getPositionInWorld"
                    );

            if (position instanceof Vector3dc vector) {
                return vector;
            }
        }

        Matrix4dc matrix =
                shipToWorld(
                        ship
                );

        if (matrix == null) {
            return null;
        }

        Vector3d origin =
                new Vector3d();

        matrix.transformPosition(
                origin
        );

        return origin;
    }

    public static Vector3dc shipVelocity(
            Object ship
    ) {
        Object velocity =
                invokeInstanceIfPresent(
                        ship,
                        "getVelocity"
                );

        return velocity instanceof Vector3dc vector
                ? vector
                : null;
    }

    public static double shipMass(
            Object ship
    ) {
        Object inertia =
                invokeInstanceIfPresent(
                        ship,
                        "getInertiaData"
                );

        if (inertia == MISSING
                || inertia == null) {
            return Double.NaN;
        }

        Object mass =
                invokeInstanceIfPresent(
                        inertia,
                        "getMass"
                );

        return mass instanceof Number number
                ? number.doubleValue()
                : Double.NaN;
    }

    public static Matrix4dc shipToWorld(
            Object ship
    ) {
        Object transform =
                invokeInstanceIfPresent(
                        ship,
                        "getTransform"
                );

        if (transform == MISSING
                || transform == null) {
            return null;
        }

        Object matrix =
                invokeInstanceIfPresent(
                        transform,
                        "getShipToWorld"
                );

        return matrix instanceof Matrix4dc value
                ? value
                : null;
    }

    public static Matrix4dc worldToShip(
            Object ship
    ) {
        Object transform =
                invokeInstanceIfPresent(
                        ship,
                        "getTransform"
                );

        if (transform == MISSING
                || transform == null) {
            return null;
        }

        Object matrix =
                invokeInstanceIfPresent(
                        transform,
                        "getWorldToShip"
                );

        return matrix instanceof Matrix4dc value
                ? value
                : null;
    }

    public static AABBdc worldAabb(
            Object ship
    ) {
        Object value =
                invokeInstanceIfPresent(
                        ship,
                        "getWorldAABB"
                );

        return value instanceof AABBdc aabb
                ? aabb
                : null;
    }

    public static AABBic shipAabb(
            Object ship
    ) {
        Object value =
                invokeInstanceIfPresent(
                        ship,
                        "getShipAABB"
                );

        return value instanceof AABBic aabb
                ? aabb
                : null;
    }

    public static boolean applyWorldForce(
            Object ship,
            Vector3dc force
    ) {
        if (ship == null
                || force == null) {
            return false;
        }

        if (tryModernForceAdapter(
                ship,
                force
        )) {
            return true;
        }

        return tryLegacyForceAttachment(
                ship,
                force
        );
    }

    private static boolean tryModernForceAdapter(
            Object ship,
            Vector3dc force
    ) {
        Object dimension =
                invokeInstanceIfPresent(
                        ship,
                        "getChunkClaimDimension"
                );

        if (dimension == MISSING
                || dimension == null) {
            return false;
        }

        Object adapter =
                invokeClassMethodIfPresent(
                        VS_MOD,
                        "getOrCreateGTPA",
                        dimension
                );

        if (adapter == MISSING
                || adapter == null) {
            return false;
        }

        long id =
                shipId(
                        ship
                );

        if (id < 0L) {
            return false;
        }

        Object applied =
                invokeInstanceIfPresent(
                        adapter,
                        "applyWorldForce",
                        id,
                        force,
                        null
                );

        if (applied != MISSING) {
            return true;
        }

        applied =
                invokeInstanceIfPresent(
                        adapter,
                        "applyInvariantForce",
                        id,
                        force
                );

        if (applied != MISSING) {
            return true;
        }

        applied =
                invokeInstanceIfPresent(
                        adapter,
                        "applyWorldForceToBodyPos",
                        id,
                        force,
                        null
                );

        return applied != MISSING;
    }

    private static boolean tryLegacyForceAttachment(
            Object ship,
            Vector3dc force
    ) {
        try {
            Class<?> applierClass =
                    Class.forName(
                            LEGACY_FORCE_APPLIER
                    );

            Object applier =
                    invokeInstanceIfPresent(
                            ship,
                            "getAttachment",
                            applierClass
                    );

            if (applier == MISSING
                    || applier == null) {
                return false;
            }

            Object applied =
                    invokeInstanceIfPresent(
                            applier,
                            "applyInvariantForce",
                            force
                    );

            return applied != MISSING;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object invokeStaticIfPresent(
            String className,
            String methodName,
            Object... args
    ) {
        try {
            Class<?> type =
                    Class.forName(
                            className
                    );

            Method method =
                    findCompatibleMethod(
                            type,
                            methodName,
                            args
                    );

            if (method == null
                    || !Modifier.isStatic(
                    method.getModifiers()
            )) {
                return MISSING;
            }

            return method.invoke(
                    null,
                    args
            );
        } catch (Throwable ignored) {
            return MISSING;
        }
    }

    private static Object invokeClassMethodIfPresent(
            String className,
            String methodName,
            Object... args
    ) {
        try {
            Class<?> type =
                    Class.forName(
                            className
                    );

            Method method =
                    findCompatibleMethod(
                            type,
                            methodName,
                            args
                    );

            if (method == null) {
                return MISSING;
            }

            Object receiver =
                    null;

            if (!Modifier.isStatic(
                    method.getModifiers()
            )) {
                Field instance =
                        type.getField(
                                "INSTANCE"
                        );

                receiver =
                        instance.get(
                                null
                        );
            }

            return method.invoke(
                    receiver,
                    args
            );
        } catch (Throwable ignored) {
            return MISSING;
        }
    }

    private static Object invokeInstanceIfPresent(
            Object target,
            String methodName,
            Object... args
    ) {
        if (target == null) {
            return MISSING;
        }

        try {
            Method method =
                    findCompatibleMethod(
                            target.getClass(),
                            methodName,
                            args
                    );

            if (method == null) {
                return MISSING;
            }

            return method.invoke(
                    target,
                    args
            );
        } catch (Throwable ignored) {
            return MISSING;
        }
    }

    private static Method findCompatibleMethod(
            Class<?> type,
            String name,
            Object[] args
    ) {
        return Arrays.stream(
                        type.getMethods()
                )
                .filter(method ->
                        method.getName()
                                .equals(
                                        name
                                )
                )
                .filter(method ->
                        method.getParameterCount()
                                == args.length
                )
                .filter(method ->
                        parametersAccept(
                                method.getParameterTypes(),
                                args
                        )
                )
                .findFirst()
                .orElse(
                        null
                );
    }

    private static boolean parametersAccept(
            Class<?>[] parameterTypes,
            Object[] args
    ) {
        for (int i = 0;
             i < parameterTypes.length;
             i++) {
            if (!parameterAccepts(
                    parameterTypes[i],
                    args[i]
            )) {
                return false;
            }
        }

        return true;
    }

    private static boolean parameterAccepts(
            Class<?> parameterType,
            Object arg
    ) {
        if (arg == null) {
            return !parameterType.isPrimitive();
        }

        Class<?> actual =
                arg.getClass();

        if (!parameterType.isPrimitive()) {
            return parameterType.isAssignableFrom(
                    actual
            );
        }

        return primitiveWrapper(
                parameterType
        ).isAssignableFrom(
                actual
        );
    }

    private static Class<?> primitiveWrapper(
            Class<?> primitive
    ) {
        if (primitive == boolean.class) {
            return Boolean.class;
        }

        if (primitive == byte.class) {
            return Byte.class;
        }

        if (primitive == short.class) {
            return Short.class;
        }

        if (primitive == int.class) {
            return Integer.class;
        }

        if (primitive == long.class) {
            return Long.class;
        }

        if (primitive == float.class) {
            return Float.class;
        }

        if (primitive == double.class) {
            return Double.class;
        }

        if (primitive == char.class) {
            return Character.class;
        }

        return primitive;
    }
}
