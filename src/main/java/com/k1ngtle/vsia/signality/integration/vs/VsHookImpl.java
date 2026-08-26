package com.k1ngtle.vsia.signality.integration.vs;

import com.k1ngtle.vsia.signality.api.geom.Obb;
import com.k1ngtle.vsia.signality.api.radar.IRadarTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4dc;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBdc;
import org.joml.primitives.AABBic;

import java.util.stream.Stream;

public final class VsHookImpl implements VsCompat.VsHook {
    @Override
    public Object shipManagingPos(
            ServerLevel level,
            BlockPos pos
    ) {
        return VsRuntimeCompat.findShipManagingPos(
                level,
                pos
        );
    }

    @Override
    public Vec3 transformShipToWorld(
            Object ship,
            Vec3 shipPos
    ) {
        Matrix4dc matrix =
                VsRuntimeCompat.shipToWorld(
                        ship
                );

        if (matrix == null) {
            return shipPos;
        }

        Vector3d value =
                new Vector3d(
                        shipPos.x,
                        shipPos.y,
                        shipPos.z
                );

        matrix.transformPosition(
                value
        );

        return new Vec3(
                value.x,
                value.y,
                value.z
        );
    }

    @Override
    public Vec3 transformDirShipToWorld(
            Object ship,
            Vec3 shipDir
    ) {
        Matrix4dc matrix =
                VsRuntimeCompat.shipToWorld(
                        ship
                );

        if (matrix == null) {
            return shipDir;
        }

        Vector3d value =
                new Vector3d(
                        shipDir.x,
                        shipDir.y,
                        shipDir.z
                );

        matrix.transformDirection(
                value
        );

        return new Vec3(
                value.x,
                value.y,
                value.z
        );
    }

    @Override
    public Vec3 transformWorldToShip(
            Object ship,
            Vec3 worldPos
    ) {
        Matrix4dc matrix =
                VsRuntimeCompat.worldToShip(
                        ship
                );

        if (matrix == null) {
            return worldPos;
        }

        Vector3d value =
                new Vector3d(
                        worldPos.x,
                        worldPos.y,
                        worldPos.z
                );

        matrix.transformPosition(
                value
        );

        return new Vec3(
                value.x,
                value.y,
                value.z
        );
    }

    @Override
    public Vec3 transformDirWorldToShip(
            Object ship,
            Vec3 worldDir
    ) {
        Matrix4dc matrix =
                VsRuntimeCompat.worldToShip(
                        ship
                );

        if (matrix == null) {
            return worldDir;
        }

        Vector3d value =
                new Vector3d(
                        worldDir.x,
                        worldDir.y,
                        worldDir.z
                );

        matrix.transformDirection(
                value
        );

        return new Vec3(
                value.x,
                value.y,
                value.z
        );
    }

    @Override
    public Vec3 shipCenter(
            Object ship
    ) {
        AABBdc aabb =
                VsRuntimeCompat.worldAabb(
                        ship
                );

        if (aabb != null) {
            return new Vec3(
                    (aabb.minX()
                            + aabb.maxX()) * 0.5D,
                    (aabb.minY()
                            + aabb.maxY()) * 0.5D,
                    (aabb.minZ()
                            + aabb.maxZ()) * 0.5D
            );
        }

        Vector3dc position =
                VsRuntimeCompat.shipWorldPosition(
                        ship
                );

        if (position == null) {
            return Vec3.ZERO;
        }

        return new Vec3(
                position.x(),
                position.y(),
                position.z()
        );
    }

    @Override
    public Vec3 shipVelocity(
            Object ship
    ) {
        Vector3dc velocity =
                VsRuntimeCompat.shipVelocity(
                        ship
                );

        if (velocity == null) {
            return Vec3.ZERO;
        }

        return new Vec3(
                velocity.x(),
                velocity.y(),
                velocity.z()
        );
    }

    @Override
    public Obb shipObb(
            Object ship
    ) {
        AABBic shipAabb =
                VsRuntimeCompat.shipAabb(
                        ship
                );

        Matrix4dc matrix =
                VsRuntimeCompat.shipToWorld(
                        ship
                );

        if (shipAabb == null
                || matrix == null) {
            return new Obb(
                    shipCenter(
                            ship
                    ),
                    Vec3.ZERO,
                    new Quaterniond()
            );
        }

        double cx =
                (shipAabb.minX()
                        + shipAabb.maxX()) * 0.5D;

        double cy =
                (shipAabb.minY()
                        + shipAabb.maxY()) * 0.5D;

        double cz =
                (shipAabb.minZ()
                        + shipAabb.maxZ()) * 0.5D;

        double hx =
                (shipAabb.maxX()
                        - shipAabb.minX()) * 0.5D;

        double hy =
                (shipAabb.maxY()
                        - shipAabb.minY()) * 0.5D;

        double hz =
                (shipAabb.maxZ()
                        - shipAabb.minZ()) * 0.5D;

        Vector3d centerShip =
                new Vector3d(
                        cx,
                        cy,
                        cz
                );

        matrix.transformPosition(
                centerShip
        );

        Quaterniond rotation =
                new Quaterniond();

        matrix.getNormalizedRotation(
                rotation
        );

        return new Obb(
                new Vec3(
                        centerShip.x,
                        centerShip.y,
                        centerShip.z
                ),
                new Vec3(
                        hx,
                        hy,
                        hz
                ),
                rotation
        );
    }

    @Override
    public Stream<IRadarTarget> shipTargetsIn(
            ServerLevel level
    ) {
        return VsRuntimeCompat.loadedShips(
                        level
                )
                .map(ship ->
                        new VsShipTargetView(
                                level,
                                ship,
                                this
                        )
                );
    }
}
