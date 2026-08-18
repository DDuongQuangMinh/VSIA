package com.k1ngtle.vsia.signality.engineering.channel;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RfKinematicTracker {
    private static final Map<UUID, Sample> SAMPLES =
            new HashMap<>();

    private RfKinematicTracker() {
    }

    public static synchronized Vec3 updateAndGetVelocityMetersPerSecond(
            UUID deviceId,
            Vec3 worldPosition,
            long gameTick
    ) {
        Sample previous =
                SAMPLES.get(
                        deviceId
                );

        if (previous == null) {
            SAMPLES.put(
                    deviceId,
                    new Sample(
                            worldPosition,
                            gameTick,
                            Vec3.ZERO
                    )
            );

            return Vec3.ZERO;
        }

        if (gameTick <= previous.tick()) {
            return previous.velocityMetersPerSecond();
        }

        long deltaTicks =
                gameTick
                        - previous.tick();

        double seconds =
                deltaTicks
                        / 20.0;

        if (seconds <= 0.0) {
            return Vec3.ZERO;
        }

        Vec3 delta =
                worldPosition.subtract(
                        previous.position()
                );

        Vec3 velocity =
                delta.scale(
                        1.0 / seconds
                );

        if (!finite(
                velocity
        )) {
            velocity =
                    Vec3.ZERO;
        }

        SAMPLES.put(
                deviceId,
                new Sample(
                        worldPosition,
                        gameTick,
                        velocity
                )
        );

        return velocity;
    }

    public static synchronized void remove(
            UUID deviceId
    ) {
        SAMPLES.remove(
                deviceId
        );
    }

    public static synchronized void clear() {
        SAMPLES.clear();
    }

    private static boolean finite(
            Vec3 value
    ) {
        return Double.isFinite(
                value.x
        )
                && Double.isFinite(
                value.y
        )
                && Double.isFinite(
                value.z
        );
    }

    private record Sample(
            Vec3 position,
            long tick,
            Vec3 velocityMetersPerSecond
    ) {
    }
}
