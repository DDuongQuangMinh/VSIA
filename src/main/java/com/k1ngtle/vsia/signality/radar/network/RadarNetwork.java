package com.k1ngtle.vsia.signality.radar.network;

import com.k1ngtle.vsia.signality.radar.iff.IffReplyStatus;
import com.k1ngtle.vsia.signality.radar.iff.IffResult;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
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
    private static final double SPATIAL_NIS_GATE =
            20.0;

    private static final double RADIAL_NIS_GATE =
            25.0;

    private static final double MAX_TRACK_SPEED_MPS =
            350.0;

    private static final double MAX_TRACK_ACCELERATION_MPS2 =
            25.0;

    private static final double MIN_POSITION_VARIANCE_M2 =
            0.25;

    private static final double MIN_VELOCITY_VARIANCE_MPS2 =
            0.25;

    private static final double INITIAL_VELOCITY_VARIANCE_MPS2 =
            225.0;

    private static final double PROCESS_ACCELERATION_SIGMA_MPS2 =
            4.0;

    private static final long CONFIRM_WINDOW_TICKS =
            80L;

    private static final long IFF_MEMORY_TICKS =
            100L;

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

        mergeDuplicateTracks(
                nowTick
        );

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
            if (measurement.measurementTick()
                    + 2L
                    < track.lastMeasurementTick) {
                continue;
            }

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
                    track.predictPosition(
                            dt
                    );

            Vec3 innovation =
                    measurement.position()
                            .subtract(
                                    predicted
                            );

            double innovationDistance =
                    innovation.length();

            double predictedVariance =
                    track.predictedPositionVariance(
                            dt
                    );

            double innovationVariance =
                    Math.max(
                            MIN_POSITION_VARIANCE_M2,
                            predictedVariance
                                    + measurement.positionVarianceMeters2()
                    );

            double sigma =
                    Math.sqrt(
                            innovationVariance
                    );

            double hardGate =
                    config.baseGateMeters()
                            + (
                            track.state
                                    == RadarTrackState.COASTING
                                    ? 5.0
                                    : 4.0
                    )
                            * sigma;

            hardGate =
                    Math.min(
                            track.state
                                    == RadarTrackState.COASTING
                                    ? 140.0
                                    : 90.0,
                            hardGate
                    );

            if (innovationDistance
                    > hardGate) {
                continue;
            }

            double spatialNis =
                    innovation.lengthSqr()
                            / innovationVariance;

            if (spatialNis
                    > SPATIAL_NIS_GATE) {
                continue;
            }

            Vec3 los =
                    lineOfSight(
                            measurement.sensorPosition(),
                            measurement.position()
                    );

            double predictedRadial =
                    track.velocity.dot(
                            los
                    );

            double measuredRadial =
                    measurement.velocity()
                            .dot(
                                    los
                            );

            double radialResidual =
                    measuredRadial
                            - predictedRadial;

            double radialVariance =
                    Math.max(
                            0.25,
                            track.velocityVarianceMps2
                                    + measurement.radialVelocityVarianceMps2()
                    );

            double radialNis =
                    radialResidual
                            * radialResidual
                            / radialVariance;

            if (track.hits
                    >= 3
                    && radialNis
                    > RADIAL_NIS_GATE) {
                continue;
            }

            double stateBias =
                    switch (track.state) {
                        case CONFIRMED ->
                                -0.55;

                        case COASTING ->
                                -0.30;

                        case TENTATIVE ->
                                0.10;
                    };

            double iffBias =
                    iffAssociationBias(
                            track.latestIff,
                            measurement.iff()
                    );

            if (Double.isInfinite(
                    iffBias
            )) {
                continue;
            }

            double score =
                    spatialNis
                            + 0.25
                            * Math.min(
                            RADIAL_NIS_GATE,
                            radialNis
                    )
                            + stateBias
                            + iffBias;

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

    private void mergeDuplicateTracks(
            long nowTick
    ) {
        boolean merged;

        do {
            merged = false;

            List<MutableTrack> values =
                    new ArrayList<>(
                            tracks.values()
                    );

            outer:
            for (int i = 0;
                 i < values.size();
                 i++) {
                MutableTrack a =
                        values.get(
                                i
                        );

                if (!tracks.containsKey(
                        a.trackId
                )) {
                    continue;
                }

                for (int j = i + 1;
                     j < values.size();
                     j++) {
                    MutableTrack b =
                            values.get(
                                    j
                            );

                    if (!tracks.containsKey(
                            b.trackId
                    )) {
                        continue;
                    }

                    if (!shouldMerge(
                            a,
                            b,
                            nowTick
                    )) {
                        continue;
                    }

                    MutableTrack winner =
                            stronger(
                                    a,
                                    b
                            );

                    MutableTrack loser =
                            winner == a
                                    ? b
                                    : a;

                    winner.absorb(
                            loser
                    );

                    tracks.remove(
                            loser.trackId
                    );

                    merged = true;
                    break outer;
                }
            }
        } while (merged);
    }

    private boolean shouldMerge(
            MutableTrack a,
            MutableTrack b,
            long nowTick
    ) {
        if (iffConflict(
                a.latestIff,
                b.latestIff
        )) {
            return false;
        }

        Vec3 positionA =
                a.predictToTick(
                        nowTick
                );

        Vec3 positionB =
                b.predictToTick(
                        nowTick
                );

        double distance =
                positionA.distanceTo(
                        positionB
                );

        double varianceA =
                a.predictedPositionVariance(
                        Math.max(
                                0.0,
                                (
                                        nowTick
                                                - a.lastMeasurementTick
                                )
                                        / 20.0
                        )
                );

        double varianceB =
                b.predictedPositionVariance(
                        Math.max(
                                0.0,
                                (
                                        nowTick
                                                - b.lastMeasurementTick
                                )
                                        / 20.0
                        )
                );

        double mergeSigma =
                Math.sqrt(
                        Math.max(
                                MIN_POSITION_VARIANCE_M2,
                                varianceA
                                        + varianceB
                        )
                );

        boolean sameAuthenticatedIff =
                sameAuthenticatedIff(
                        a.latestIff,
                        b.latestIff
                );

        double mergeGate =
                Math.min(
                        sameAuthenticatedIff
                                ? 30.0
                                : 16.0,
                        3.0
                                + (
                                sameAuthenticatedIff
                                        ? 3.0
                                        : 2.0
                        )
                                * mergeSigma
                );

        if (distance
                > mergeGate) {
            return false;
        }

        double velocityDifference =
                a.velocity
                        .subtract(
                                b.velocity
                        )
                        .length();

        double velocitySigma =
                Math.sqrt(
                        Math.max(
                                MIN_VELOCITY_VARIANCE_MPS2,
                                a.velocityVarianceMps2
                                        + b.velocityVarianceMps2
                        )
                );

        double velocityGate =
                Math.min(
                        sameAuthenticatedIff
                                ? 45.0
                                : 25.0,
                        6.0
                                + 2.5
                                * velocitySigma
                );

        return velocityDifference
                <= velocityGate;
    }

    private MutableTrack stronger(
            MutableTrack a,
            MutableTrack b
    ) {
        int stateA =
                stateRank(
                        a.state
                );

        int stateB =
                stateRank(
                        b.state
                );

        if (stateA
                != stateB) {
            return stateA
                    > stateB
                    ? a
                    : b;
        }

        if (a.lastMeasurementTick
                != b.lastMeasurementTick) {
            return a.lastMeasurementTick
                    > b.lastMeasurementTick
                    ? a
                    : b;
        }

        if (a.hits
                != b.hits) {
            return a.hits
                    > b.hits
                    ? a
                    : b;
        }

        return a.createdTick
                <= b.createdTick
                ? a
                : b;
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

    private static int stateRank(
            RadarTrackState state
    ) {
        return switch (state) {
            case CONFIRMED ->
                    3;

            case COASTING ->
                    2;

            case TENTATIVE ->
                    1;
        };
    }

    private static double iffAssociationBias(
            IffResult track,
            IffResult measurement
    ) {
        if (track == null
                || measurement == null) {
            return 0.0;
        }

        if (track.authenticated()
                && measurement.authenticated()) {
            if (track.modeSAddress()
                    != measurement.modeSAddress()) {
                return Double.POSITIVE_INFINITY;
            }

            return -2.0;
        }

        return 0.0;
    }

    private static boolean iffConflict(
            IffResult a,
            IffResult b
    ) {
        return a != null
                && b != null
                && a.authenticated()
                && b.authenticated()
                && a.modeSAddress()
                != b.modeSAddress();
    }

    private static boolean sameAuthenticatedIff(
            IffResult a,
            IffResult b
    ) {
        return a != null
                && b != null
                && a.authenticated()
                && b.authenticated()
                && a.modeSAddress()
                == b.modeSAddress();
    }

    private static Vec3 lineOfSight(
            Vec3 sensor,
            Vec3 target
    ) {
        Vec3 delta =
                target.subtract(
                        sensor
                );

        double length =
                delta.length();

        if (length
                < 1.0E-9) {
            return new Vec3(
                    1.0,
                    0.0,
                    0.0
            );
        }

        return delta.scale(
                1.0 / length
        );
    }

    private static Vec3 clampMagnitude(
            Vec3 value,
            double maximum
    ) {
        double length =
                value.length();

        if (length
                <= maximum
                || length
                < 1.0E-12) {
            return value;
        }

        return value.scale(
                maximum / length
        );
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

        private final Deque<Long> recentHitTicks =
                new ArrayDeque<>();

        private RadarTrackState state =
                RadarTrackState.TENTATIVE;

        private Vec3 position;
        private Vec3 velocity;

        private Vec3 velocityBaselinePosition;
        private long velocityBaselineTick;

        private long lastMeasurementTick;
        private long lastUniqueHitTick;

        private int hits;

        private double bestSnrLinear;
        private double positionVarianceMeters2;
        private double velocityVarianceMps2;

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
                    clampMagnitude(
                            first.velocity(),
                            MAX_TRACK_SPEED_MPS
                    );

            velocityBaselinePosition =
                    first.position();

            velocityBaselineTick =
                    first.measurementTick();

            lastMeasurementTick =
                    first.measurementTick();

            lastUniqueHitTick =
                    first.measurementTick();

            hits =
                    1;

            recentHitTicks.addLast(
                    first.measurementTick()
            );

            contributingSensors.add(
                    first.emitterId()
            );

            bestSnrLinear =
                    first.snrLinear();

            positionVarianceMeters2 =
                    Math.max(
                            MIN_POSITION_VARIANCE_M2,
                            first.positionVarianceMeters2()
                    );

            velocityVarianceMps2 =
                    Math.max(
                            INITIAL_VELOCITY_VARIANCE_MPS2,
                            first.radialVelocityVarianceMps2()
                                    * 9.0
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

        private Vec3 predictPosition(
                double dt
        ) {
            double boundedDt =
                    clamp(
                            dt,
                            0.0,
                            5.0
                    );

            return position.add(
                    velocity.scale(
                            boundedDt
                    )
            );
        }

        private Vec3 predictToTick(
                long tick
        ) {
            double dt =
                    Math.max(
                            0.0,
                            (
                                    tick
                                            - lastMeasurementTick
                            )
                                    / 20.0
                    );

            return predictPosition(
                    dt
            );
        }

        private double predictedPositionVariance(
                double dt
        ) {
            double boundedDt =
                    clamp(
                            dt,
                            0.0,
                            5.0
                    );

            double accelerationVariance =
                    PROCESS_ACCELERATION_SIGMA_MPS2
                            * PROCESS_ACCELERATION_SIGMA_MPS2;

            return Math.max(
                    MIN_POSITION_VARIANCE_M2,
                    positionVarianceMeters2
                            + velocityVarianceMps2
                            * boundedDt
                            * boundedDt
                            + 0.25
                            * accelerationVariance
                            * boundedDt
                            * boundedDt
                            * boundedDt
                            * boundedDt
            );
        }

        private void update(
                RadarMeasurement measurement,
                RadarNetworkConfig config
        ) {
            double rawDt =
                    (
                            measurement.measurementTick()
                                    - lastMeasurementTick
                    )
                            / 20.0;

            double dt =
                    clamp(
                            rawDt,
                            0.05,
                            2.0
                    );

            Vec3 predicted =
                    predictPosition(
                            dt
                    );

            Vec3 innovation =
                    measurement.position()
                            .subtract(
                                    predicted
                            );

            double measurementVariance =
                    Math.max(
                            MIN_POSITION_VARIANCE_M2,
                            measurement.positionVarianceMeters2()
                    );

            double predictedVariance =
                    predictedPositionVariance(
                            dt
                    );

            double kalmanLikeGain =
                    predictedVariance
                            / (
                            predictedVariance
                                    + measurementVariance
                    );

            double snrWeight =
                    clamp(
                            Math.log10(
                                    1.0
                                            + measurement.snrLinear()
                            )
                                    / 3.0,
                            0.0,
                            1.0
                    );

            double alpha =
                    clamp(
                            0.20
                                    + 0.45
                                    * kalmanLikeGain
                                    + 0.08
                                    * snrWeight,
                            0.20,
                            0.72
                    );

            position =
                    predicted.add(
                            innovation.scale(
                                    alpha
                            )
                    );

            Vec3 candidateVelocity =
                    velocity;

            double baselineDt =
                    (
                            measurement.measurementTick()
                                    - velocityBaselineTick
                    )
                            / 20.0;

            if (baselineDt
                    >= 0.25) {
                Vec3 sampledVelocity =
                        measurement.position()
                                .subtract(
                                        velocityBaselinePosition
                                )
                                .scale(
                                        1.0 / baselineDt
                                );

                sampledVelocity =
                        clampMagnitude(
                                sampledVelocity,
                                MAX_TRACK_SPEED_MPS
                        );

                double sampledBlend =
                        clamp(
                                0.08
                                        + 0.05
                                        * Math.min(
                                        1.0,
                                        baselineDt
                                )
                                        + 0.05
                                        * snrWeight,
                                0.08,
                                0.18
                        );

                candidateVelocity =
                        candidateVelocity
                                .scale(
                                        1.0
                                                - sampledBlend
                                )
                                .add(
                                        sampledVelocity.scale(
                                                sampledBlend
                                        )
                                );

                velocityBaselinePosition =
                        measurement.position();

                velocityBaselineTick =
                        measurement.measurementTick();
            }

            double innovationVelocityGain =
                    clamp(
                            0.06
                                    + 0.04
                                    * snrWeight
                                    + 0.02
                                    * Math.min(
                                    1.0,
                                    dt
                            ),
                            0.06,
                            0.12
                    );

            candidateVelocity =
                    candidateVelocity.add(
                            innovation.scale(
                                    innovationVelocityGain
                            )
                    );

            Vec3 los =
                    lineOfSight(
                            measurement.sensorPosition(),
                            measurement.position()
                    );

            double measuredRadial =
                    measurement.velocity()
                            .dot(
                                    los
                            );

            double candidateRadial =
                    candidateVelocity.dot(
                            los
                    );

            double radialGain =
                    measurement.trackQuality()
                            ? 0.45
                            : 0.30;

            candidateVelocity =
                    candidateVelocity.add(
                            los.scale(
                                    (
                                            measuredRadial
                                                    - candidateRadial
                                    )
                                            * radialGain
                            )
                    );

            Vec3 deltaVelocity =
                    candidateVelocity.subtract(
                            velocity
                    );

            double accelerationLimit =
                    MAX_TRACK_ACCELERATION_MPS2
                            * dt;

            deltaVelocity =
                    clampMagnitude(
                            deltaVelocity,
                            accelerationLimit
                    );

            velocity =
                    clampMagnitude(
                            velocity.add(
                                    deltaVelocity
                            ),
                            MAX_TRACK_SPEED_MPS
                    );

            double positionResidualVariance =
                    (
                            1.0
                                    - alpha
                    )
                            * (
                            1.0
                                    - alpha
                    )
                            * predictedVariance
                            + alpha
                            * alpha
                            * measurementVariance;

            positionVarianceMeters2 =
                    Math.max(
                            MIN_POSITION_VARIANCE_M2,
                            positionResidualVariance
                                    + 0.5
                                    * PROCESS_ACCELERATION_SIGMA_MPS2
                                    * PROCESS_ACCELERATION_SIGMA_MPS2
                                    * dt
                                    * dt
            );

            double radialMeasurementVariance =
                    Math.max(
                            MIN_VELOCITY_VARIANCE_MPS2,
                            measurement.radialVelocityVarianceMps2()
                    );

            velocityVarianceMps2 =
                    Math.max(
                            MIN_VELOCITY_VARIANCE_MPS2,
                            velocityVarianceMps2
                                    * 0.86
                                    + radialMeasurementVariance
                                    * 0.14
                                    + 0.20
                                    * PROCESS_ACCELERATION_SIGMA_MPS2
                                    * PROCESS_ACCELERATION_SIGMA_MPS2
                                    * dt
            );

            lastMeasurementTick =
                    measurement.measurementTick();

            recordHit(
                    measurement.measurementTick(),
                    config.confirmHits()
            );

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

            quality =
                    trackQuality(
                            measurement,
                            hits,
                            contributingSensors.size(),
                            positionVarianceMeters2,
                            velocityVarianceMps2
                    );

            applyIff(
                    measurement.iff(),
                    measurement.measurementTick()
            );
        }

        private void recordHit(
                long measurementTick,
                int confirmHits
        ) {
            if (measurementTick
                    == lastUniqueHitTick) {
                return;
            }

            lastUniqueHitTick =
                    measurementTick;

            hits++;

            recentHitTicks.addLast(
                    measurementTick
            );

            while (!recentHitTicks.isEmpty()
                    && measurementTick
                    - recentHitTicks.peekFirst()
                    > CONFIRM_WINDOW_TICKS) {
                recentHitTicks.removeFirst();
            }

            if (recentHitTicks.size()
                    >= confirmHits) {
                state =
                        RadarTrackState.CONFIRMED;
            }
        }

        private void absorb(
                MutableTrack other
        ) {
            contributingSensors.addAll(
                    other.contributingSensors
            );

            bestSnrLinear =
                    Math.max(
                            bestSnrLinear,
                            other.bestSnrLinear
                    );

            hits =
                    Math.max(
                            hits,
                            other.hits
                    );

            recentHitTicks.addAll(
                    other.recentHitTicks
            );

            List<Long> orderedHits =
                    new ArrayList<>(
                            new HashSet<>(
                                    recentHitTicks
                            )
                    );

            orderedHits.sort(
                    Long::compareTo
            );

            recentHitTicks.clear();

            long newest =
                    Math.max(
                            lastMeasurementTick,
                            other.lastMeasurementTick
                    );

            for (Long tick :
                    orderedHits) {
                if (newest
                        - tick
                        <= CONFIRM_WINDOW_TICKS) {
                    recentHitTicks.addLast(
                            tick
                    );
                }
            }

            if (stateRank(
                    other.state
            )
                    > stateRank(
                    state
            )) {
                state =
                        other.state;
            }

            positionVarianceMeters2 =
                    Math.min(
                            positionVarianceMeters2,
                            other.positionVarianceMeters2
                    );

            velocityVarianceMps2 =
                    Math.min(
                            velocityVarianceMps2,
                            other.velocityVarianceMps2
                    );

            quality =
                    Math.max(
                            quality,
                            other.quality
                    );

            if (other.lastMeasurementTick
                    > lastMeasurementTick) {
                position =
                        other.position;

                velocity =
                        other.velocity;

                lastMeasurementTick =
                        other.lastMeasurementTick;

                velocityBaselinePosition =
                        other.velocityBaselinePosition;

                velocityBaselineTick =
                        other.velocityBaselineTick;
            }

            mergeIff(
                    other
            );
        }

        private void mergeIff(
                MutableTrack other
        ) {
            if (other.latestIff == null) {
                return;
            }

            if (other.latestIff.authenticated()) {
                if (latestIff == null
                        || !latestIff.authenticated()
                        || other.lastAuthenticatedIffTick
                        > lastAuthenticatedIffTick) {
                    latestIff =
                            other.latestIff;

                    lastAuthenticatedIffTick =
                            other.lastAuthenticatedIffTick;
                }

                return;
            }

            if (latestIff == null
                    || latestIff.replyStatus()
                    == IffReplyStatus.NO_TRANSPONDER) {
                latestIff =
                        other.latestIff;
            }
        }

        private void applyIff(
                IffResult result,
                long measurementTick
        ) {
            if (result == null) {
                return;
            }

            if (result.authenticated()) {
                latestIff =
                        result;

                lastAuthenticatedIffTick =
                        measurementTick;
            } else if (result.replyStatus()
                    == IffReplyStatus.AUTH_FAILED) {
                latestIff =
                        result;

                lastAuthenticatedIffTick =
                        Long.MIN_VALUE;
            } else if (latestIff == null
                    || latestIff.replyStatus()
                    == IffReplyStatus.NO_TRANSPONDER) {
                latestIff =
                        result;
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
                    predictPosition(
                            coastSeconds
                    );

            double variance =
                    predictedPositionVariance(
                            coastSeconds
                    );

            double coastPenalty =
                    1.0
                            / (
                            1.0
                                    + coastSeconds
                                    * 0.18
                    );

            IffResult snapshotIff =
                    latestIff;

            if (lastAuthenticatedIffTick
                    != Long.MIN_VALUE
                    && nowTick
                    - lastAuthenticatedIffTick
                    > IFF_MEMORY_TICKS) {
                snapshotIff =
                        IffResult.unknown(
                                IffReplyStatus.NO_REPLY
                        );
            }

            if (snapshotIff == null) {
                snapshotIff =
                        IffResult.noTransponder();
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
                                    MIN_POSITION_VARIANCE_M2,
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
                    measurement.positionVarianceMeters2(),
                    INITIAL_VELOCITY_VARIANCE_MPS2
            );
        }

        private static double trackQuality(
                RadarMeasurement measurement,
                int hits,
                int sensors,
                double positionVariance,
                double velocityVariance
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
                                    / 10.0,
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

            double positionPrecision =
                    1.0
                            / (
                            1.0
                                    + Math.sqrt(
                                    Math.max(
                                            0.0,
                                            positionVariance
                                    )
                            )
                                    / 25.0
                    );

            double velocityPrecision =
                    1.0
                            / (
                            1.0
                                    + Math.sqrt(
                                    Math.max(
                                            0.0,
                                            velocityVariance
                                    )
                            )
                                    / 15.0
                    );

            double quality =
                    0.10
                            + 0.28
                            * snr
                            + 0.24
                            * history
                            + 0.12
                            * multiSensor
                            + 0.14
                            * positionPrecision
                            + 0.12
                            * velocityPrecision;

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
