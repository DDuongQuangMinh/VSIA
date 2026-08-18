package com.k1ngtle.vsia.signality.engineering.channel;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

public final class VsWorldPoseResolver {
    private static volatile boolean initialized;
    private static volatile Method toWorldCoordinates;

    private VsWorldPoseResolver() {
    }

    public static Vec3 toWorld(
            Level level,
            Vec3 position
    ) {
        if (level == null
                || position == null) {
            return position;
        }

        ensureInitialized();

        Method method =
                toWorldCoordinates;

        if (method == null) {
            return position;
        }

        try {
            Object result =
                    method.invoke(
                            null,
                            level,
                            position
                    );

            if (result instanceof Vec3 vec3
                    && finite(
                    vec3
            )) {
                return vec3;
            }
        } catch (Throwable ignored) {
        }

        return position;
    }

    public static Vec3 directionToWorld(
            Level level,
            Vec3 localOrigin,
            Vec3 localDirection
    ) {
        if (localDirection == null
                || localDirection.lengthSqr()
                < 1.0E-18) {
            return new Vec3(
                    0.0,
                    0.0,
                    1.0
            );
        }

        Vec3 direction =
                localDirection.normalize();

        Vec3 worldOrigin =
                toWorld(
                        level,
                        localOrigin
                );

        Vec3 worldEnd =
                toWorld(
                        level,
                        localOrigin.add(
                                direction
                        )
                );

        Vec3 transformed =
                worldEnd.subtract(
                        worldOrigin
                );

        if (!finite(
                transformed
        )
                || transformed.lengthSqr()
                < 1.0E-18) {
            return direction;
        }

        return transformed.normalize();
    }

    public static boolean available() {
        ensureInitialized();

        return toWorldCoordinates
                != null;
    }

    private static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }

        initialized =
                true;

        try {
            Class<?> utilityClass =
                    Class.forName(
                            "org.valkyrienskies.mod.common.VSGameUtilsKt"
                    );

            for (Method method
                    : utilityClass.getMethods()) {
                if (!method.getName()
                        .equals(
                                "toWorldCoordinates"
                        )) {
                    continue;
                }

                Class<?>[] parameters =
                        method.getParameterTypes();

                if (parameters.length != 2) {
                    continue;
                }

                if (!Level.class.isAssignableFrom(
                        parameters[0]
                )) {
                    continue;
                }

                if (!Vec3.class.isAssignableFrom(
                        parameters[1]
                )) {
                    continue;
                }

                if (!Vec3.class.isAssignableFrom(
                        method.getReturnType()
                )) {
                    continue;
                }

                toWorldCoordinates =
                        method;

                break;
            }
        } catch (Throwable ignored) {
            toWorldCoordinates =
                    null;
        }
    }

    private static boolean finite(
            Vec3 value
    ) {
        return value != null
                && Double.isFinite(
                value.x
        )
                && Double.isFinite(
                value.y
        )
                && Double.isFinite(
                value.z
        );
    }
}
