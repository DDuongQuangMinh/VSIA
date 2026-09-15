package com.k1ngtle.vsia.signality.radar.network;

import com.k1ngtle.vsia.signality.radar.iff.IffReplyStatus;
import com.k1ngtle.vsia.signality.radar.iff.IffResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public final class RadarNetwork {
    private final String id;
    private final RadarNetworkConfig config;

    private final PriorityQueue<RadarSensorReport> reports =
            new PriorityQueue<>();

    private final Map<UUID, MutableTrack> tracks =
            new HashMap<>();

    private long reportsAccepted;
    private long reportsDropped;
    private long reportsDelivered;

    public RadarNetwork(
            String id,
            RadarNetworkConfig config
    ) {
        this.id =
                id;

        this.config =
                config == null
                        ? RadarNetworkConfig.realisticDefault()
                        : config;
    }

    public synchronized String id() {
        return id;
    }

    public synchronized RadarNetworkConfig config() {
        return config;
    }

    public synchronized boolean accept(
            RadarSensorReport report
    ) {
        if (report == null
                || report.measurement() == null) {
            reportsDropped++;
            return false;
        }

        if (reports.size()
                >= config.maxQueuedReports()) {
            reportsDropped++;
            return false;
        }

        reports.offer(
                report
        );

        reportsAccepted++;
        return true;
    }

    public synchronized void recordDroppedReport() {
        reportsDropped++;
    }

    public synchronized void tick(
            long nowTick
    ) {
        int delivered = 0;

        while (delivered
                < config.maxReportsPerTick()
                && !reports.isEmpty()) {
            RadarSensorReport next =
                    reports.peek();

            if (next.deliveryTick()
                    > nowTick) {
                break;
            }

            reports.poll();
            reportsDelivered++;
            delivered++;

            ingest(
                    next.measurement()
            );
        }

        updateTrackLifecycle(
                nowTick
        );
    }

    public synchronized List<RadarNetworkTrack> tracks(
            long nowTick
    ) {
        List<RadarNetworkTrack> result =
                new ArrayList<>(
                        tracks.size()
                );

        for (MutableTrack track :
                tracks.values()) {
            result.add(
                    track.snapshot(
                            nowTick
                    )
            );
        }

        result.sort(
                Comparator
                        .comparing(
                                RadarNetworkTrack::state
                        )
                        .thenComparingDouble(
                                RadarNetworkTrack::positionUncertaintyMeters
                        )
                        .thenComparing(
                                RadarNetworkTrack::trackId
                        )
        );

        return List.copyOf(
                result
        );
    }

    public synchronized RadarNetworkStats stats() {
        int tentative = 0;
        int confirmed = 0;
        int coasting = 0;

        for (MutableTrack track :
                tracks.values()) {
            switch (track.state) {
                case TENTATIVE ->
                        tentative++;

                case CONFIRMED ->
                        confirmed++;

                case COASTING ->
                        coasting++;
            }
        }

        return new RadarNetworkStats(
                id,
                reports.size(),
                tracks.size(),
                tentative,
                confirmed,
                coasting,
                reportsAccepted,
                reportsDropped,
                reportsDelivered
        );
    }

    public synchronized void clear() {
        reports.clear();
        tracks.clear();

        reportsAccepted = 0L;
        reportsDropped = 0L;
        reportsDelivered = 0L;
    }

    private void ingest(
            RadarMeasurement measurement
    ) {
        MutableTrack match =
                associate(
                        measurement
                );

        if (match == null) {
            createTrack(
                    measurement
            );

            return;
        }

        if (measurement.measurementTick()
                < match.lastMeasurementTick) {
            return;
        }

        match.update(
                measurement,
                config
        );
    }

    private MutableTrack associate(
            RadarMeasurement measurement
    ) {
        MutableTrack best = null;
        double bestScore =
                Double.POSITIVE_INFINITY;

        for (MutableTrack track :
                tracks.values()) {
            double dt =
                    Math.max(
                            0.0,
                            (
                                    measurement.measurementTick()
                                            - track.lastMeasurementTick
                            )
                                    / 20.0
                    );

            Vec3 predicted =
                    track.position.add(
                            track.velocity.scale(
                                    dt
                            )
                    );

            double residual =
                    predicted.distanceTo(
                            measurement.position()
                    );

            double sigma =
                    Math.sqrt(
                            Math.max(
                                    0.25,
                                    track.positionVarianceMeters2
                                            + measurement.positionVarianceMeters2()
                            )
                    );

            double dynamicGate =
                    config.baseGateMeters()
                            + 3.0
                            * sigma
                            + Math.min(
                            100.0,
                            track.velocity.length()
                                    * dt
                                    * 0.35
                    );

            dynamicGate =
                    Math.min(
                            600.0,
                            dynamicGate
                    );

            if (residual
                    > dynamicGate) {
                continue;
            }

            double velocityDelta =
                    track.velocity
                            .subtract(
                                    measurement.velocity()
                            )
                            .length();

            double velocityPenalty =
                    Math.min(
                            1.0,
                            velocityDelta
                                    / 150.0
                    )
                            * 0.20;

            double score =
                    residual
                            / Math.max(
                            1.0,
                            dynamicGate
                    )
                            + velocityPenalty;

            if (score
                    < bestScore) {
                bestScore = score;
                best = track;
            }
        }

        return best;
    }

    private void createTrack(
            RadarMeasurement measurement
    ) {
        MutableTrack track =
                new MutableTrack(
                        UUID.randomUUID(),
                        measurement
                );

        tracks.put(
                track.trackId,
                track
        );
    }

    private void updateTrackLifecycle(
            long nowTick
    ) {
        Iterator<MutableTrack> iterator =
                tracks.values()
                        .iterator();

        while (iterator.hasNext()) {
            MutableTrack track =
                    iterator.next();

            long staleTicks =
                    Math.max(
                            0L,
                            nowTick
                                    - track.lastMeasurementTick
                    );

            if (staleTicks
                    > config.dropAfterTicks()) {
                iterator.remove();
                continue;
            }

            if (track.state
                    == RadarTrackState.TENTATIVE
                    && staleTicks
                    > config.coastAfterTicks()) {
                iterator.remove();
                continue;
            }

            if (track.state
                    == RadarTrackState.CONFIRMED
                    && staleTicks
                    > config.coastAfterTicks()) {
                track.state =
                        RadarTrackState.COASTING;
            }
        }
    }

    private static double clamp(
            double value,
            double min,
            double max
    ) {
        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }

    private static final class MutableTrack {
        private final UUID trackId;
        private final long createdTick;
        private final Set<UUID> contributingSensors =
                new HashSet<>();

        private RadarTrackState state =
                RadarTrackState.TENTATIVE;

        private Vec3 position;
        private Vec3 velocity;

        private long lastMeasurementTick;
        private int hits;
        private double bestSnrLinear;
        private double positionVarianceMeters2;
        private double quality;
        private IffResult latestIff =
                IffResult.noTransponder();
        private long lastAuthenticatedIffTick =
                Long.MIN_VALUE;

        private MutableTrack(
                UUID trackId,
                RadarMeasurement first
        ) {
            this.trackId =
                    trackId;

            createdTick =
                    first.measurementTick();

            position =
                    first.position();

            velocity =
                    first.velocity();

            lastMeasurementTick =
                    first.measurementTick();

            hits = 1;

            contributingSensors.add(
                    first.emitterId()
            );

            bestSnrLinear =
                    first.snrLinear();

            positionVarianceMeters2 =
                    Math.max(
                            0.25,
                            first.positionVarianceMeters2()
                    );

            quality =
                    initialQuality(
                            first
                    );

            applyIff(
                    first.iff(),
                    first.measurementTick()
            );
        }

        private void update(
                RadarMeasurement measurement,
                RadarNetworkConfig config
        ) {
            double dt =
                    Math.max(
                            0.05,
                            (
                                    measurement.measurementTick()
                                            - lastMeasurementTick
                            )
                                    / 20.0
                    );

            Vec3 predicted =
                    position.add(
                            velocity.scale(
                                    dt
                            )
                    );

            Vec3 residual =
                    measurement.position()
                            .subtract(
                                    predicted
                            );

            double measurementVariance =
                    Math.max(
                            0.25,
                            measurement.positionVarianceMeters2()
                    );

            double trackVariance =
                    Math.max(
                            0.25,
                            positionVarianceMeters2
                    );

            double trustMeasurement =
                    trackVariance
                            / (
                            trackVariance
                                    + measurementVariance
                    );

            double snrWeight =
                    clamp(
                            Math.log10(
                                    1.0
                                            + measurement.snrLinear()
                            )
                                    / 2.5,
                            0.0,
                            1.0
                    );

            double alpha =
                    clamp(
                            0.45
                                    + 0.35
                                    * trustMeasurement
                                    + 0.08
                                    * snrWeight
                                    + (
                                    measurement.trackQuality()
                                            ? 0.04
                                            : 0.0
                            ),
                            0.45,
                            0.90
                    );

            double beta =
                    clamp(
                            0.08
                                    + 0.20
                                    * trustMeasurement
                                    + 0.04
                                    * snrWeight,
                            0.08,
                            0.32
                    );

            position =
                    predicted.add(
                            residual.scale(
                                    alpha
                            )
                    );

            Vec3 kinematicVelocity =
                    velocity.add(
                            residual.scale(
                                    beta
                                            / dt
                            )
                    );

            double radialBlend =
                    measurement.trackQuality()
                            ? 0.12
                            : 0.06;

            velocity =
                    kinematicVelocity
                            .scale(
                                    1.0
                                            - radialBlend
                            )
                            .add(
                                    measurement.velocity()
                                            .scale(
                                                    radialBlend
                                            )
                            );

            double processNoise =
                    1.5
                            + 0.08
                            * velocity.length();

            positionVarianceMeters2 =
                    Math.max(
                            0.25,
                            (
                                    1.0
                                            - alpha
                            )
                                    * (
                                    1.0
                                            - alpha
                            )
                                    * trackVariance
                                    + alpha
                                    * alpha
                                    * measurementVariance
                                    + processNoise
                                    * processNoise
                                    * dt
                                    * dt
                    );

            lastMeasurementTick =
                    measurement.measurementTick();

            hits++;

            contributingSensors.add(
                    measurement.emitterId()
            );

            bestSnrLinear =
                    Math.max(
                            bestSnrLinear,
                            measurement.snrLinear()
                    );

            if (state
                    == RadarTrackState.COASTING) {
                state =
                        RadarTrackState.CONFIRMED;
            }

            if (hits
                    >= config.confirmHits()) {
                state =
                        RadarTrackState.CONFIRMED;
            }

            quality =
                    trackQuality(
                            measurement,
                            hits,
                            contributingSensors.size(),
                            positionVarianceMeters2
                    );

            applyIff(
                    measurement.iff(),
                    measurement.measurementTick()
            );
        }

        private void applyIff(
                IffResult result,
                long measurementTick
        ) {
            if (result == null) {
                return;
            }

            if (result.authenticated()) {
                latestIff = result;
                lastAuthenticatedIffTick = measurementTick;
            } else if (result.replyStatus() == IffReplyStatus.AUTH_FAILED) {
                latestIff = result;
                lastAuthenticatedIffTick = Long.MIN_VALUE;
            } else if (latestIff == null
                    || latestIff.replyStatus() == IffReplyStatus.NO_TRANSPONDER) {
                latestIff = result;
            }
        }

        private RadarNetworkTrack snapshot(
                long nowTick
        ) {
            double coastSeconds =
                    Math.max(
                            0L,
                            nowTick
                                    - lastMeasurementTick
                    )
                            / 20.0;

            Vec3 predicted =
                    position.add(
                            velocity.scale(
                                    coastSeconds
                            )
                    );

            double processSigma =
                    1.5
                            + 0.08
                            * velocity.length();

            double variance =
                    positionVarianceMeters2
                            + processSigma
                            * processSigma
                            * coastSeconds
                            * coastSeconds;

            double coastPenalty =
                    1.0
                            / (
                            1.0
                                    + coastSeconds
                                    * 0.20
                    );

            IffResult snapshotIff = latestIff;

            if (lastAuthenticatedIffTick != Long.MIN_VALUE
                    && nowTick - lastAuthenticatedIffTick > 100L) {
                snapshotIff = IffResult.unknown(IffReplyStatus.NO_REPLY);
            }

            if (snapshotIff == null) {
                snapshotIff = IffResult.noTransponder();
            }

            return new RadarNetworkTrack(
                    trackId,
                    state,
                    predicted,
                    velocity,
                    createdTick,
                    lastMeasurementTick,
                    hits,
                    contributingSensors.size(),
                    contributingSensors,
                    bestSnrLinear,
                    Math.sqrt(
                            Math.max(
                                    0.25,
                                    variance
                            )
                    ),
                    clamp(
                            quality
                                    * coastPenalty,
                            0.0,
                            1.0
                    ),
                    snapshotIff
            );
        }

        private static double initialQuality(
                RadarMeasurement measurement
        ) {
            return trackQuality(
                    measurement,
                    1,
                    1,
                    measurement.positionVarianceMeters2()
            );
        }

        private static double trackQuality(
                RadarMeasurement measurement,
                int hits,
                int sensors,
                double variance
        ) {
            double snr =
                    clamp(
                            Math.log10(
                                    1.0
                                            + measurement.snrLinear()
                            )
                                    / 2.5,
                            0.0,
                            1.0
                    );

            double history =
                    clamp(
                            hits
                                    / 8.0,
                            0.0,
                            1.0
                    );

            double multiSensor =
                    clamp(
                            (
                                    sensors - 1
                            )
                                    / 3.0,
                            0.0,
                            1.0
                    );

            double precision =
                    1.0
                            / (
                            1.0
                                    + Math.sqrt(
                                    Math.max(
                                            0.0,
                                            variance
                                    )
                            )
                                    / 35.0
                    );

            double quality =
                    0.15
                            + 0.30
                            * snr
                            + 0.25
                            * history
                            + 0.15
                            * multiSensor
                            + 0.15
                            * precision;

            if (measurement.trackQuality()) {
                quality += 0.05;
            }

            return clamp(
                    quality,
                    0.0,
                    1.0
            );
        }
    }
}
