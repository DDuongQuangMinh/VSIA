package com.k1ngtle.vsia.cockpit.display;

import com.k1ngtle.vsia.cockpit.detection.F35DetectionContact;
import com.k1ngtle.vsia.cockpit.detection.F35ShipSilhouette;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public final class F35ClientDetectionCache {
    private static final Map<UUID, Snapshot> SNAPSHOTS = new HashMap<>();
    private static final Snapshot EMPTY =
            new Snapshot(
                    0L,
                    List.of(),
                    F35ShipSilhouette.empty()
            );

    private F35ClientDetectionCache() {
    }

    public static Snapshot snapshot(UUID cockpitId) {
        Snapshot value = SNAPSHOTS.getOrDefault(cockpitId, EMPTY);
        return System.currentTimeMillis() - value.receivedMillis() > 1500L ? EMPTY : value;
    }

    public static void clearAll() { SNAPSHOTS.clear(); }
    public static void invalidate(UUID cockpitId,long tick,List<UUID> ids) {
        Snapshot old=SNAPSHOTS.get(cockpitId);
        if(old==null||tick<old.serverTick())return;
        SNAPSHOTS.put(cockpitId,new Snapshot(tick,old.contacts().stream().filter(c->!ids.contains(c.contactId())).toList(),old.silhouette(),old.receivedMillis()));
    }

    public static void accept(
            UUID cockpitId,
            long serverTick,
            List<F35DetectionContact> contacts,
            F35ShipSilhouette silhouette
    ) {
        if (cockpitId == null || cockpitId.equals(new UUID(0L, 0L))) return;
        Snapshot old=SNAPSHOTS.get(cockpitId);if(old!=null&&serverTick<old.serverTick())return;
        SNAPSHOTS.put(cockpitId,
                new Snapshot(
                        serverTick,
                        contacts,
                        silhouette
                ));
    }

    public record Snapshot(
            long serverTick,
            List<F35DetectionContact> contacts,
            F35ShipSilhouette silhouette,
            long receivedMillis
    ) {
        public Snapshot(long serverTick, List<F35DetectionContact> contacts, F35ShipSilhouette silhouette) {
            this(serverTick, contacts, silhouette, System.currentTimeMillis());
        }
        public Snapshot {
            contacts =
                    List.copyOf(
                            contacts
                    );

            silhouette =
                    silhouette == null
                            ? F35ShipSilhouette.empty()
                            : silhouette;
        }
    }
}
