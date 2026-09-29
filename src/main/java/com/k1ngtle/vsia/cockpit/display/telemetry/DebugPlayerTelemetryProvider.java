package com.k1ngtle.vsia.cockpit.display.telemetry;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class DebugPlayerTelemetryProvider
        implements AircraftTelemetryProvider {
    @Override
    public AircraftTelemetry capture(
            F35CockpitSeatBlockEntity cockpit,
            float partialTick
    ) {
        LocalPlayer player =
                Minecraft.getInstance()
                        .player;

        if (player == null) {
            Vec3 center =
                    Vec3.atCenterOf(
                            cockpit.getBlockPos()
                    );

            return new AircraftTelemetry(
                    center,
                    Vec3.ZERO,
                    0.0,
                    0.0,
                    0.0,
                    center.y,
                    0.0,
                    0.0
            );
        }

        Vec3 velocity =
                player.getDeltaMovement()
                        .scale(
                                20.0
                        );

        double heading =
                normalizeDegrees(
                        player.getYRot()
                );

        return new AircraftTelemetry(
                player.position(),
                velocity,
                heading,
                player.getXRot(),
                0.0,
                player.getY(),
                velocity.length(),
                velocity.y
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
}
