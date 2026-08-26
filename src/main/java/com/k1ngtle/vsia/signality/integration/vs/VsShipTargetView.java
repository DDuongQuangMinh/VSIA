package com.k1ngtle.vsia.signality.integration.vs;

import com.k1ngtle.vsia.signality.api.radar.IRadarTarget;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class VsShipTargetView implements IRadarTarget {
    private static final double SHIP_HULL_RCS_GAIN =
            2.0D;

    private final ServerLevel level;
    private final Object ship;
    private final VsHookImpl hook;
    private final UUID id;

    public VsShipTargetView(
            ServerLevel level,
            Object ship,
            VsHookImpl hook
    ) {
        this.level =
                level;

        this.ship =
                ship;

        this.hook =
                hook;

        this.id =
                new UUID(
                        5864145284199612416L,
                        VsRuntimeCompat.shipId(
                                ship
                        )
                );
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public ServerLevel level() {
        return level;
    }

    @Override
    public Vec3 positionWorld() {
        return hook.shipObb(
                ship
        ).centerWorld();
    }

    @Override
    public Vec3 velocityWorld() {
        return hook.shipVelocity(
                ship
        );
    }

    @Override
    public double boundingRadius() {
        return hook.shipObb(
                ship
        ).enclosingRadius();
    }

    @Override
    public double radarCrossSection(
            double aspectAngleRad,
            double wavelengthMeters
    ) {
        double radius =
                boundingRadius();

        double baseRcs =
                Math.PI
                        * radius
                        * radius;

        double aspectMod =
                0.3D
                        + 0.7D
                        * Math.abs(
                        Math.sin(
                                aspectAngleRad
                        )
                );

        return baseRcs
                * aspectMod
                * SHIP_HULL_RCS_GAIN;
    }

    @Override
    public Object vsShip() {
        return ship;
    }
}
