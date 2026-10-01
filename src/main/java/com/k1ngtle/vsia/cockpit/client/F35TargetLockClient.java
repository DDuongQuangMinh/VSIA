package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.cockpit.detection.F35DetectionContact;
import com.k1ngtle.vsia.cockpit.display.F35RadarTrackView;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class F35TargetLockClient {
    public enum LockKind {
        NONE,
        RADAR,
        DETECTION
    }

    private static LockKind kind = LockKind.NONE;
    private static UUID targetId;

    private F35TargetLockClient() {
    }

    public static LockKind kind() {
        return kind;
    }

    @Nullable
    public static UUID targetId() {
        return targetId;
    }

    public static boolean hasLock() {
        return kind != LockKind.NONE && targetId != null;
    }

    public static void lockRadar(UUID trackId) {
        if (trackId == null) {
            clear();
            return;
        }

        kind = LockKind.RADAR;
        targetId = trackId;
    }

    public static void lockDetection(UUID contactId) {
        if (contactId == null) {
            clear();
            return;
        }

        kind = LockKind.DETECTION;
        targetId = contactId;
    }

    public static void toggleRadar(UUID trackId) {
        if (isRadarLocked(trackId)) {
            clear();
        } else {
            lockRadar(trackId);
        }
    }

    public static void toggleDetection(UUID contactId) {
        if (isDetectionLocked(contactId)) {
            clear();
        } else {
            lockDetection(contactId);
        }
    }

    public static void clear() {
        kind = LockKind.NONE;
        targetId = null;
    }

    public static boolean isRadarLocked(UUID trackId) {
        return kind == LockKind.RADAR
                && targetId != null
                && targetId.equals(trackId);
    }

    public static boolean isDetectionLocked(UUID contactId) {
        return kind == LockKind.DETECTION
                && targetId != null
                && targetId.equals(contactId);
    }

    @Nullable
    public static F35RadarTrackView lockedRadarTrack(
            List<F35RadarTrackView> tracks
    ) {
        if (kind != LockKind.RADAR || targetId == null) {
            return null;
        }

        for (F35RadarTrackView track : tracks) {
            if (targetId.equals(track.trackId())) {
                return track;
            }
        }

        return null;
    }

    @Nullable
    public static F35DetectionContact lockedDetection(
            List<F35DetectionContact> contacts
    ) {
        if (kind != LockKind.DETECTION || targetId == null) {
            return null;
        }

        for (F35DetectionContact contact : contacts) {
            if (targetId.equals(contact.contactId())) {
                return contact;
            }
        }

        return null;
    }

    public static void validate(
            List<F35RadarTrackView> tracks,
            List<F35DetectionContact> contacts
    ) {
        if (!hasLock()) {
            return;
        }

        if (kind == LockKind.RADAR) {
            if (lockedRadarTrack(tracks) == null) {
                clear();
            }
        } else if (kind == LockKind.DETECTION) {
            if (lockedDetection(contacts) == null) {
                clear();
            }
        }
    }

    public static String summary(
            List<F35RadarTrackView> tracks,
            List<F35DetectionContact> contacts
    ) {
        F35RadarTrackView radar = lockedRadarTrack(tracks);

        if (radar != null) {
            String affiliation = radar.iffAffiliation();
            if (affiliation == null || affiliation.isBlank()) {
                affiliation = "UNKNOWN";
            }
            return "RADAR " + radar.shortId() + " " + affiliation;
        }

        F35DetectionContact detection = lockedDetection(contacts);

        if (detection != null) {
            return detection.type().name() + " " + detection.shortId();
        }

        return "NONE";
    }
}
