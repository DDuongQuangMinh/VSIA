package com.k1ngtle.vsia.cockpit.display;

import com.k1ngtle.vsia.cockpit.detection.F35DetectionContact;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public final class F35TrackTrailCache {
    private static final long SAMPLE_INTERVAL_MS =
            250L;

    private static final long HISTORY_MS =
            10_000L;

    private static final long STALE_MS =
            15_000L;

    private static final int MAX_POINTS =
            48;

    private static final double MIN_MOVE_SQR =
            0.01;

    private static final Map<UUID, Trail> RADAR_TRAILS =
            new HashMap<>();

    private static final Map<UUID, Trail> DETECTION_TRAILS =
            new HashMap<>();

    private F35TrackTrailCache() {
    }

    public static synchronized void update(
            F35DisplayState state
    ) {
        long now =
                state.localTimeMillis();

        for (F35RadarTrackView track :
                state.tracks()) {
            sample(
                    RADAR_TRAILS,
                    track.trackId(),
                    track.position(),
                    now
            );
        }

        for (F35DetectionContact contact :
                state.detections()) {
            sample(
                    DETECTION_TRAILS,
                    contact.contactId(),
                    contact.position(),
                    now
            );
        }

        prune(
                RADAR_TRAILS,
                now
        );

        prune(
                DETECTION_TRAILS,
                now
        );
    }

    public static synchronized List<TrailPoint> radarTrail(
            UUID trackId
    ) {
        return snapshot(
                RADAR_TRAILS.get(
                        trackId
                )
        );
    }

    public static synchronized List<TrailPoint> detectionTrail(
            UUID contactId
    ) {
        return snapshot(
                DETECTION_TRAILS.get(
                        contactId
                )
        );
    }

    public static synchronized void clear() {
        RADAR_TRAILS.clear();
        DETECTION_TRAILS.clear();
    }
    public static synchronized void forget(List<UUID> radar,List<UUID> detection){
        radar.forEach(RADAR_TRAILS::remove);detection.forEach(DETECTION_TRAILS::remove);
    }

    private static void sample(
            Map<UUID, Trail> trails,
            UUID id,
            Vec3 position,
            long now
    ) {
        Trail trail =
                trails.computeIfAbsent(
                        id,
                        ignored ->
                                new Trail()
                );

        trail.lastSeenMillis =
                now;

        TrailPoint last =
                trail.points.isEmpty()
                        ? null
                        : trail.points.get(
                                trail.points.size() - 1
                        );

        if (last != null
                && now - last.timeMillis()
                < SAMPLE_INTERVAL_MS) {
            return;
        }

        if (last != null
                && last.position()
                .distanceToSqr(
                        position
                ) < MIN_MOVE_SQR) {
            return;
        }

        trail.points.add(
                new TrailPoint(
                        position,
                        now
                )
        );

        while (trail.points.size()
                > MAX_POINTS) {
            trail.points.remove(
                    0
            );
        }
    }

    private static void prune(
            Map<UUID, Trail> trails,
            long now
    ) {
        Iterator<Map.Entry<UUID, Trail>> iterator =
                trails.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Trail trail =
                    iterator.next()
                            .getValue();

            trail.points.removeIf(
                    point ->
                            now - point.timeMillis()
                                    > HISTORY_MS
            );

            if (now - trail.lastSeenMillis
                    > STALE_MS) {
                iterator.remove();
            }
        }
    }

    private static List<TrailPoint> snapshot(
            Trail trail
    ) {
        if (trail == null
                || trail.points.isEmpty()) {
            return List.of();
        }

        return List.copyOf(
                trail.points
        );
    }

    private static final class Trail {
        private final List<TrailPoint> points =
                new ArrayList<>();

        private long lastSeenMillis =
                0L;
    }

    public record TrailPoint(
            Vec3 position,
            long timeMillis
    ) {
    }
}
