package com.k1ngtle.vsia.signality.engineering.channel;

import com.k1ngtle.vsia.signality.integration.vs.VsRuntimeCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4dc;
import org.joml.Vector3d;

import java.lang.reflect.Method;

public final class VsWorldPoseResolver {
    private static volatile boolean initialized;
    private static volatile Method toWorldCoordinates;

    private VsWorldPoseResolver() {
    }

    public static Vec3 toWorld(Level level, Vec3 position) {
        if (level == null || position == null) {
            return position;
        }

        if (level instanceof ServerLevel serverLevel) {
            Object ship = VsRuntimeCompat.findShipManagingPos(
                    serverLevel,
                    BlockPos.containing(position.x, position.y, position.z)
            );

            if (ship != null) {
                Matrix4dc shipToWorld = VsRuntimeCompat.shipToWorld(ship);

                if (shipToWorld != null) {
                    Vector3d value = new Vector3d(
                            position.x,
                            position.y,
                            position.z
                    );

                    shipToWorld.transformPosition(value);

                    if (finite(value.x, value.y, value.z)) {
                        return new Vec3(value.x, value.y, value.z);
                    }
                }
            }
        }

        ensureInitialized();

        Method method = toWorldCoordinates;

        if (method != null) {
            try {
                Object result = method.invoke(null, level, position);

                if (result instanceof Vec3 vec3
                        && finite(vec3.x, vec3.y, vec3.z)) {
                    return vec3;
                }
            } catch (Throwable ignored) {
            }
        }

        return position;
    }

    public static Vec3 directionToWorld(
            Level level,
            Vec3 localOrigin,
            Vec3 localDirection
    ) {
        if (localDirection == null
                || localDirection.lengthSqr() < 1.0E-18D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }

        Vec3 direction = localDirection.normalize();
        Vec3 worldOrigin = toWorld(level, localOrigin);
        Vec3 worldEnd = toWorld(level, localOrigin.add(direction));
        Vec3 transformed = worldEnd.subtract(worldOrigin);

        if (!finite(transformed.x, transformed.y, transformed.z)
                || transformed.lengthSqr() < 1.0E-18D) {
            return direction;
        }

        return transformed.normalize();
    }

    public static boolean available() {
        ensureInitialized();
        return toWorldCoordinates != null;
    }

    private static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }

        initialized = true;

        try {
            Class<?> utilityClass = Class.forName(
                    "org.valkyrienskies.mod.common.VSGameUtilsKt"
            );

            for (Method method : utilityClass.getMethods()) {
                if (!method.getName().equals("toWorldCoordinates")) {
                    continue;
                }

                Class<?>[] parameters = method.getParameterTypes();

                if (parameters.length == 2
                        && Level.class.isAssignableFrom(parameters[0])
                        && Vec3.class.isAssignableFrom(parameters[1])
                        && Vec3.class.isAssignableFrom(method.getReturnType())) {
                    toWorldCoordinates = method;
                    break;
                }
            }
        } catch (Throwable ignored) {
            toWorldCoordinates = null;
        }
    }

    private static boolean finite(double x, double y, double z) {
        return Double.isFinite(x)
                && Double.isFinite(y)
                && Double.isFinite(z);
    }
}
