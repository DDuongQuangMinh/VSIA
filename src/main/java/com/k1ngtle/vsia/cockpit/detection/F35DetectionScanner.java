package com.k1ngtle.vsia.cockpit.detection;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.F35VsShipHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class F35DetectionScanner {
    private static final int MAX_CONTACTS =
            256;

    private static final double MAX_RANGE_METERS =
            80_000.0;

    private F35DetectionScanner() {
    }

    public static ScanResult scan(
            ServerPlayer viewer,
            F35CockpitSeatBlockEntity cockpit,
            F35DetectionFilter filter
    ) {
        ServerLevel level =
                viewer.serverLevel();

        F35VsShipHelper.ShipSnapshot ownShip =
                F35VsShipHelper.shipSnapshot(
                        cockpit
                );

        if (!ownShip.detected()) {
            return new ScanResult(
                    List.of(),
                    F35ShipSilhouette.empty()
            );
        }

        Vec3 origin =
                ownShip.worldCenter();

        List<F35DetectionContact> contacts =
                new ArrayList<>();

        if (filter.mobs()
                || filter.players()) {
            scanLoadedEntities(
                    level,
                    viewer,
                    origin,
                    filter,
                    contacts
            );
        }

        if (filter.ships()) {
            scanLoadedShips(
                    level,
                    ownShip,
                    origin,
                    contacts
            );
        }

        contacts.sort(
                Comparator.comparingDouble(
                        contact ->
                                contact.position()
                                        .distanceToSqr(
                                                origin
                                        )
                )
        );

        if (contacts.size()
                > MAX_CONTACTS) {
            contacts =
                    new ArrayList<>(
                            contacts.subList(
                                    0,
                                    MAX_CONTACTS
                            )
                    );
        }

        return new ScanResult(
                List.copyOf(
                        contacts
                ),
                F35ShipSilhouetteScanner.scan(
                        cockpit
                )
        );
    }

    private static void scanLoadedEntities(
            ServerLevel level,
            ServerPlayer viewer,
            Vec3 origin,
            F35DetectionFilter filter,
            List<F35DetectionContact> output
    ) {
        double maxRangeSqr =
                MAX_RANGE_METERS
                        * MAX_RANGE_METERS;

        for (Entity entity :
                level.getAllEntities()) {
            if (!entity.isAlive()
                    || entity == viewer) {
                continue;
            }

            F35DetectionType type;

            if (entity instanceof Player) {
                if (!filter.players()) {
                    continue;
                }

                type =
                        F35DetectionType.PLAYER;
            } else if (entity instanceof Mob) {
                if (!filter.mobs()) {
                    continue;
                }

                type =
                        F35DetectionType.MOB;
            } else {
                continue;
            }

            Vec3 position =
                    entity.position();

            if (position.distanceToSqr(
                    origin
            ) > maxRangeSqr) {
                continue;
            }

            Vec3 velocity =
                    entity.getDeltaMovement()
                            .scale(
                                    20.0
                            );

            output.add(
                    new F35DetectionContact(
                            entity.getUUID(),
                            type,
                            entity.getName()
                                    .getString(),
                            position,
                            velocity
                    )
            );
        }
    }

    private static void scanLoadedShips(
            ServerLevel level,
            F35VsShipHelper.ShipSnapshot ownShip,
            Vec3 origin,
            List<F35DetectionContact> output
    ) {
        double maxRangeSqr =
                MAX_RANGE_METERS
                        * MAX_RANGE_METERS;

        for (F35VsShipHelper.ShipSnapshot ship :
                F35VsShipHelper.loadedShips(
                        level
                )) {
            if (!ship.detected()
                    || ship.shipId()
                    == ownShip.shipId()) {
                continue;
            }

            if (ship.worldCenter()
                    .distanceToSqr(
                            origin
                    ) > maxRangeSqr) {
                continue;
            }

            output.add(
                    new F35DetectionContact(
                            F35VsShipHelper.shipContactId(
                                    ship.shipId()
                            ),
                            F35DetectionType.SHIP,
                            ship.shipSlug(),
                            ship.worldCenter(),
                            ship.velocity()
                    )
            );
        }
    }

    public record ScanResult(
            List<F35DetectionContact> contacts,
            F35ShipSilhouette silhouette
    ) {
        public ScanResult {
            contacts =
                    List.copyOf(
                            contacts
                    );
        }
    }
}
