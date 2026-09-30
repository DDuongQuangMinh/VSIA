package com.k1ngtle.vsia.cockpit.detection;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.F35VsShipHelper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class F35ShipSilhouetteScanner {
    private static final int GRID_WIDTH =
            42;

    private static final int GRID_HEIGHT =
            54;

    private static final int MAX_VERTICAL_SAMPLES =
            32;

    private static final long CACHE_TICKS =
            40L;

    private static final Map<Long, Cached> CACHE =
            new ConcurrentHashMap<>();

    private F35ShipSilhouetteScanner() {
    }

    public static F35ShipSilhouette scan(
            F35CockpitSeatBlockEntity cockpit
    ) {
        if (!(cockpit.getLevel()
                instanceof ServerLevel level)) {
            return F35ShipSilhouette.empty();
        }

        F35VsShipHelper.ShipSnapshot ship =
                F35VsShipHelper.shipSnapshot(
                        cockpit
                );

        if (!ship.detected()) {
            return F35ShipSilhouette.empty();
        }

        long now =
                level.getGameTime();

        Cached cached =
                CACHE.get(
                        ship.shipId()
                );

        if (cached != null
                && now - cached.tick()
                < CACHE_TICKS) {
            return cached.silhouette();
        }

        F35VsShipHelper.ShipBounds bounds =
                F35VsShipHelper.shipLocalBounds(
                        cockpit
                );

        if (!bounds.available()) {
            return F35ShipSilhouette.empty();
        }

        boolean[] occupied =
                new boolean[
                        GRID_WIDTH
                                * GRID_HEIGHT
                        ];

        int sizeX =
                Math.max(
                        1,
                        bounds.sizeX()
                );

        int sizeY =
                Math.max(
                        1,
                        bounds.sizeY()
                );

        int sizeZ =
                Math.max(
                        1,
                        bounds.sizeZ()
                );

        int stepX =
                Math.max(
                        1,
                        (int) Math.ceil(
                                sizeX
                                        / (double) GRID_WIDTH
                        )
                );

        int stepZ =
                Math.max(
                        1,
                        (int) Math.ceil(
                                sizeZ
                                        / (double) GRID_HEIGHT
                        )
                );

        int stepY =
                Math.max(
                        1,
                        (int) Math.ceil(
                                sizeY
                                        / (double) MAX_VERTICAL_SAMPLES
                        )
                );

        BlockPos.MutableBlockPos cursor =
                new BlockPos.MutableBlockPos();

        for (int x = bounds.minX();
             x <= bounds.maxX();
             x += stepX) {
            for (int z = bounds.minZ();
                 z <= bounds.maxZ();
                 z += stepZ) {
                boolean columnOccupied =
                        false;

                for (int y = bounds.minY();
                     y <= bounds.maxY();
                     y += stepY) {
                    cursor.set(
                            x,
                            y,
                            z
                    );

                    if (!level.getBlockState(
                            cursor
                    ).isAir()) {
                        columnOccupied =
                                true;
                        break;
                    }
                }

                if (!columnOccupied) {
                    continue;
                }

                int gridX =
                        Math.min(
                                GRID_WIDTH - 1,
                                Math.max(
                                        0,
                                        (int) (
                                                (
                                                        x
                                                                - bounds.minX()
                                                )
                                                        / (double) sizeX
                                                        * GRID_WIDTH
                                        )
                                )
                        );

                int gridY =
                        Math.min(
                                GRID_HEIGHT - 1,
                                Math.max(
                                        0,
                                        (int) (
                                                (
                                                        z
                                                                - bounds.minZ()
                                                )
                                                        / (double) sizeZ
                                                        * GRID_HEIGHT
                                        )
                                )
                        );

                occupied[
                        gridY
                                * GRID_WIDTH
                                + gridX
                        ] =
                        true;
            }
        }

        int anchorX =
                gridCoordinate(
                        cockpit.getBlockPos()
                                .getX(),
                        bounds.minX(),
                        sizeX,
                        GRID_WIDTH
                );

        int anchorY =
                gridCoordinate(
                        cockpit.getBlockPos()
                                .getZ(),
                        bounds.minZ(),
                        sizeZ,
                        GRID_HEIGHT
                );

        F35ShipSilhouette silhouette =
                new F35ShipSilhouette(
                        GRID_WIDTH,
                        GRID_HEIGHT,
                        anchorX,
                        anchorY,
                        sizeX,
                        sizeZ,
                        occupied
                );

        CACHE.put(
                ship.shipId(),
                new Cached(
                        now,
                        silhouette
                )
        );

        return silhouette;
    }

    private static int gridCoordinate(
            int value,
            int min,
            int size,
            int gridSize
    ) {
        return Math.min(
                gridSize - 1,
                Math.max(
                        0,
                        (int) (
                                (
                                        value
                                                - min
                                )
                                        / (double) Math.max(
                                        1,
                                        size
                                )
                                        * gridSize
                        )
                )
        );
    }

    private record Cached(
            long tick,
            F35ShipSilhouette silhouette
    ) {
    }
}
