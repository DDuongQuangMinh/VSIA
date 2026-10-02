package com.k1ngtle.vsia.cockpit.display;

import com.k1ngtle.vsia.cockpit.client.F35DisplayClientConfig;
import com.k1ngtle.vsia.cockpit.client.F35TargetLockClient;
import com.k1ngtle.vsia.cockpit.detection.F35DetectionContact;
import com.k1ngtle.vsia.cockpit.detection.F35DetectionType;
import com.k1ngtle.vsia.cockpit.detection.F35ShipSilhouette;
import com.k1ngtle.vsia.cockpit.display.stores.F35StoresSnapshot;
import com.k1ngtle.vsia.cockpit.display.telemetry.AircraftTelemetry;
import com.mojang.blaze3d.vertex.PoseStack;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

public final class F35PanoramicDisplayRenderer {
    public static final float VIRTUAL_WIDTH =
            860.0F;

    public static final float VIRTUAL_HEIGHT =
            343.0F;

    public static final float TOP_BAR_HEIGHT =
            43.0F;

    public static final float DIVIDER_1 =
            214.0F;

    public static final float DIVIDER_2 =
            430.0F;

    public static final float DIVIDER_3 =
            645.0F;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern(
                    "HH:mm:ss"
            );

    public void render(
            PoseStack poseStack,
            MultiBufferSource buffers,
            F35DisplayState state,
            float monitorWidth,
            float monitorHeight
    ) {
        float scale =
                Math.min(
                        monitorWidth
                                / VIRTUAL_WIDTH,
                        monitorHeight
                                / VIRTUAL_HEIGHT
                );

        float configuredScale =
                F35DisplayClientConfig.displayScale();

        float drawWidth =
                VIRTUAL_WIDTH
                        * scale
                        * configuredScale;

        float drawHeight =
                VIRTUAL_HEIGHT
                        * scale
                        * configuredScale;

        scale *=
                configuredScale;

        poseStack.pushPose();

        poseStack.translate(
                -drawWidth
                        / 2.0F,
                drawHeight
                        / 2.0F,
                -0.0008F
        );

        poseStack.scale(
                scale,
                -scale,
                scale
        );

        F35DisplayCanvas canvas =
                new F35DisplayCanvas(
                        poseStack,
                        buffers
                );

        renderFrame(
                canvas,
                state
        );

        poseStack.popPose();
    }

    public void renderStatus(
            PoseStack poseStack,
            MultiBufferSource buffers,
            String headline,
            String detail,
            float monitorWidth,
            float monitorHeight
    ) {
        float scale =
                Math.min(
                        monitorWidth
                                / VIRTUAL_WIDTH,
                        monitorHeight
                                / VIRTUAL_HEIGHT
                );

        float drawWidth =
                VIRTUAL_WIDTH
                        * scale;

        float drawHeight =
                VIRTUAL_HEIGHT
                        * scale;

        poseStack.pushPose();

        poseStack.translate(
                -drawWidth
                        / 2.0F,
                drawHeight
                        / 2.0F,
                -0.0008F
        );

        poseStack.scale(
                scale,
                -scale,
                scale
        );

        F35DisplayCanvas canvas =
                new F35DisplayCanvas(
                        poseStack,
                        buffers
                );

        canvas.clearClip();

        canvas.rect(
                20.0F,
                20.0F,
                VIRTUAL_WIDTH - 40.0F,
                VIRTUAL_HEIGHT - 40.0F,
                F35DisplayPalette.GRID
        );

        canvas.text(
                "DISPLAY BUS",
                36.0F,
                42.0F,
                1.0F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                headline,
                245.0F,
                153.0F,
                1.35F,
                F35DisplayPalette.GREEN
        );

        canvas.text(
                detail,
                286.0F,
                180.0F,
                0.85F,
                F35DisplayPalette.DIM
        );

        canvas.text(
                "VSIA COCKPIT DISPLAY",
                36.0F,
                292.0F,
                0.85F,
                F35DisplayPalette.WHITE
        );

        canvas.clearClip();
        poseStack.popPose();
    }

    private void renderFrame(
            F35DisplayCanvas canvas,
            F35DisplayState state
    ) {
        canvas.clearClip();

        if (state.ownship()
                .shipDetected()) {
            F35TrackTrailCache.update(
                    state
            );
        } else {
            F35TrackTrailCache.clear();
        }

        canvas.line(
                0.0F,
                TOP_BAR_HEIGHT,
                VIRTUAL_WIDTH,
                TOP_BAR_HEIGHT,
                F35DisplayPalette.GRID
        );

        canvas.line(
                DIVIDER_1,
                TOP_BAR_HEIGHT,
                DIVIDER_1,
                VIRTUAL_HEIGHT,
                F35DisplayPalette.GRID
        );

        canvas.line(
                DIVIDER_2,
                TOP_BAR_HEIGHT,
                DIVIDER_2,
                VIRTUAL_HEIGHT,
                F35DisplayPalette.GRID
        );

        canvas.line(
                DIVIDER_3,
                TOP_BAR_HEIGHT,
                DIVIDER_3,
                VIRTUAL_HEIGHT,
                F35DisplayPalette.GRID
        );

        renderTopBar(
                canvas,
                state
        );

        if (!state.ownship()
                .shipDetected()) {
            renderNoShipDetected(
                    canvas,
                    state
            );
            canvas.clearClip();
            return;
        }

        renderStores(
                canvas,
                state
        );

        renderSensor(
                canvas,
                state
        );

        renderTsd(
                canvas,
                state
        );

        renderHsi(
                canvas,
                state
        );

        canvas.clearClip();
    }

    private void renderTopBar(
            F35DisplayCanvas canvas,
            F35DisplayState state
    ) {
        AircraftTelemetry ownship =
                state.ownship();

        canvas.setClip(
                0.0F,
                0.0F,
                VIRTUAL_WIDTH,
                TOP_BAR_HEIGHT
        );

        canvas.circle(
                25.0F,
                18.0F,
                14.0F,
                F35DisplayPalette.GREEN
        );

        canvas.aircraft(
                25.0F,
                18.0F,
                8.0F,
                F35DisplayPalette.GREEN
        );

        canvas.text(
                threeDigits(
                        ownship.headingDeg()
                ),
                12.0F,
                34.0F,
                1.0F,
                F35DisplayPalette.WHITE
        );

        canvas.rect(
                71.0F,
                3.0F,
                40.0F,
                35.0F,
                F35DisplayPalette.GRID
        );

        canvas.text(
                format1(
                        ownship.speedMps()
                ),
                76.0F,
                6.0F,
                1.0F,
                F35DisplayPalette.WHITE
        );

        canvas.text(
                "M/S",
                76.0F,
                16.0F,
                0.8F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                format0(
                        ownship.altitudeMeters()
                ),
                76.0F,
                27.0F,
                0.9F,
                F35DisplayPalette.WHITE
        );

        canvas.aircraft(
                132.0F,
                21.0F,
                12.0F,
                F35DisplayPalette.GREEN
        );

        canvas.text(
                "ICAWS",
                270.0F,
                12.0F,
                1.4F,
                F35DisplayPalette.GREEN
        );

        canvas.text(
                "PHM",
                326.0F,
                12.0F,
                1.4F,
                F35DisplayPalette.GREEN
        );

        canvas.text(
                "AP",
                379.0F,
                12.0F,
                1.4F,
                F35DisplayPalette.CYAN
        );

        canvas.diamond(
                430.0F,
                19.0F,
                10.0F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                threeDigits(
                        ownship.headingDeg()
                ),
                463.0F,
                3.0F,
                1.0F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                format0(
                        ownship.speedMps()
                                * 1.94384
                ),
                507.0F,
                3.0F,
                1.0F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                "RECORDER",
                567.0F,
                4.0F,
                0.9F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                shipLine(
                        state
                ),
                567.0F,
                16.0F,
                0.8F,
                F35DisplayPalette.GREEN
        );

        canvas.text(
                "IFF",
                690.0F,
                4.0F,
                1.0F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                iffSummary(
                        state
                ),
                690.0F,
                15.0F,
                0.9F,
                F35DisplayPalette.WHITE
        );


        canvas.text(
                "TGT "
                        + F35DisplayClientConfig.targetSelectionSummary(),
                690.0F,
                26.0F,
                0.72F,
                F35DisplayPalette.GREEN
        );

        LocalTime localTime =
                LocalTime.ofInstant(
                        Instant.ofEpochMilli(
                                state.localTimeMillis()
                        ),
                        ZoneId.systemDefault()
                );

        canvas.text(
                TIME_FORMAT.format(
                        localTime
                ),
                772.0F,
                4.0F,
                0.9F,
                F35DisplayPalette.WHITE
        );

        canvas.text(
                state.ownship()
                        .shipDetected()
                        ? "RNG "
                        + rangeLabel(
                        state.radarRangeMeters()
                )
                        : "RNG N/A",
                772.0F,
                15.0F,
                0.9F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                "TRK "
                        + visibleContactCount(
                        state
                ),
                772.0F,
                26.0F,
                0.9F,
                F35DisplayPalette.GREEN
        );

        canvas.clearClip();
    }

    private void renderNoShipDetected(
            F35DisplayCanvas canvas,
            F35DisplayState state
    ) {
        canvas.setClip(
                0.0F,
                TOP_BAR_HEIGHT,
                VIRTUAL_WIDTH,
                VIRTUAL_HEIGHT
        );

        float cx =
                VIRTUAL_WIDTH / 2.0F;

        float cy =
                (TOP_BAR_HEIGHT + VIRTUAL_HEIGHT) / 2.0F;

        canvas.rect(
                cx - 160.0F,
                cy - 32.0F,
                320.0F,
                68.0F,
                F35DisplayPalette.GRID
        );

        canvas.text(
                "---NO SHIP DETECTED---",
                cx - 117.0F,
                cy - 10.0F,
                1.45F,
                F35DisplayPalette.GREEN
        );

        canvas.text(
                "PLACE COCKPIT ON A VS SHIP",
                cx - 118.0F,
                cy + 10.0F,
                0.90F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                "CURRENT: "
                        + shortLabel(
                        state.ownship()
                                .shipLabel(),
                        24
                ),
                cx - 110.0F,
                cy + 24.0F,
                0.75F,
                F35DisplayPalette.DIM
        );

        canvas.clearClip();
    }

    private void renderStores(
            F35DisplayCanvas canvas,
            F35DisplayState state
    ) {
        canvas.setClip(
                0.0F,
                TOP_BAR_HEIGHT,
                DIVIDER_1,
                VIRTUAL_HEIGHT
        );

        F35StoresSnapshot stores =
                state.stores();

        canvas.text(
                "FUEL",
                6.0F,
                49.0F,
                1.2F,
                F35DisplayPalette.MAGENTA
        );

        canvas.text(
                "REFUEL",
                43.0F,
                49.0F,
                1.0F,
                F35DisplayPalette.GREEN
        );

        canvas.text(
                "GAL",
                23.0F,
                80.0F,
                0.9F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                format0(
                        stores.fuelKg()
                ),
                55.0F,
                80.0F,
                1.1F,
                F35DisplayPalette.WHITE
        );

        canvas.text(
                "FUEL "
                        + format0(
                        stores.fuelPercent()
                )
                        + "%",
                6.0F,
                97.0F,
                0.9F,
                F35DisplayPalette.GREEN
        );

        canvas.text(
                "GUN "
                        + stores.gunLabel(),
                6.0F,
                114.0F,
                0.9F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                String.valueOf(
                        stores.gunRounds()
                ),
                63.0F,
                114.0F,
                1.0F,
                F35DisplayPalette.WHITE
        );

        renderStoresShipPlan(
                canvas,
                state,
                stores
        );

        canvas.text(
                "SMS",
                177.0F,
                328.0F,
                1.0F,
                F35DisplayPalette.GREEN
        );

        canvas.clearClip();
    }

    private void renderStoresShipPlan(
            F35DisplayCanvas canvas,
            F35DisplayState state,
            F35StoresSnapshot stores
    ) {
        F35ShipSilhouette silhouette =
                state.shipSilhouette();

        if (silhouette.available()) {
            renderScannedShipSilhouette(
                    canvas,
                    silhouette
            );
        } else {
            renderReferenceAircraft(
                    canvas
            );
        }

        renderStoreStations(
                canvas,
                stores
        );

        canvas.text(
                silhouette.available()
                        ? "SHIP SCAN "
                        + silhouette.sourceWidthBlocks()
                        + "X"
                        + silhouette.sourceLengthBlocks()
                        : "SHIP SCAN WAIT",
                54.0F,
                306.0F,
                0.72F,
                silhouette.available()
                        ? F35DisplayPalette.GREEN
                        : F35DisplayPalette.DIM
        );
    }

    private void renderScannedShipSilhouette(
            F35DisplayCanvas canvas,
            F35ShipSilhouette silhouette
    ) {
        float areaX =
                64.0F;

        float areaY =
                132.0F;

        float areaWidth =
                88.0F;

        float areaHeight =
                152.0F;

        float cellScale =
                Math.min(
                        areaWidth
                                / Math.max(
                                1,
                                silhouette.width()
                        ),
                        areaHeight
                                / Math.max(
                                1,
                                silhouette.height()
                        )
                );

        float drawWidth =
                silhouette.width()
                        * cellScale;

        float drawHeight =
                silhouette.height()
                        * cellScale;

        float originX =
                areaX
                        + (
                        areaWidth
                                - drawWidth
                ) / 2.0F;

        float originY =
                areaY
                        + (
                        areaHeight
                                - drawHeight
                ) / 2.0F;

        int outline =
                F35DisplayPalette.MAGENTA;

        for (int y = 0;
             y < silhouette.height();
             y++) {
            for (int x = 0;
                 x < silhouette.width();
                 x++) {
                if (!silhouette.occupied(
                        x,
                        y
                )) {
                    continue;
                }

                float left =
                        originX
                                + x
                                * cellScale;

                float top =
                        originY
                                + y
                                * cellScale;

                float right =
                        left
                                + cellScale;

                float bottom =
                        top
                                + cellScale;

                if (!silhouette.occupied(
                        x,
                        y - 1
                )) {
                    canvas.line(
                            left,
                            top,
                            right,
                            top,
                            outline
                    );
                }

                if (!silhouette.occupied(
                        x + 1,
                        y
                )) {
                    canvas.line(
                            right,
                            top,
                            right,
                            bottom,
                            outline
                    );
                }

                if (!silhouette.occupied(
                        x,
                        y + 1
                )) {
                    canvas.line(
                            right,
                            bottom,
                            left,
                            bottom,
                            outline
                    );
                }

                if (!silhouette.occupied(
                        x - 1,
                        y
                )) {
                    canvas.line(
                            left,
                            bottom,
                            left,
                            top,
                            outline
                    );
                }
            }
        }

        if (silhouette.anchorX() >= 0
                && silhouette.anchorY() >= 0) {
            float anchorX =
                    originX
                            + (
                            silhouette.anchorX()
                                    + 0.5F
                    ) * cellScale;

            float anchorY =
                    originY
                            + (
                            silhouette.anchorY()
                                    + 0.5F
                    ) * cellScale;

            canvas.cross(
                    anchorX,
                    anchorY,
                    4.0F,
                    F35DisplayPalette.GREEN
            );
        }
    }

    private void renderReferenceAircraft(
            F35DisplayCanvas canvas
    ) {
        float cx =
                108.0F;

        float cy =
                205.0F;

        int magenta =
                F35DisplayPalette.MAGENTA;

        canvas.line(
                cx,
                cy - 60.0F,
                cx,
                cy + 62.0F,
                magenta
        );

        canvas.line(
                cx,
                cy - 35.0F,
                cx - 35.0F,
                cy + 4.0F,
                magenta
        );

        canvas.line(
                cx,
                cy - 35.0F,
                cx + 35.0F,
                cy + 4.0F,
                magenta
        );

        canvas.line(
                cx - 35.0F,
                cy + 4.0F,
                cx - 12.0F,
                cy + 12.0F,
                magenta
        );

        canvas.line(
                cx + 35.0F,
                cy + 4.0F,
                cx + 12.0F,
                cy + 12.0F,
                magenta
        );

        canvas.line(
                cx,
                cy + 30.0F,
                cx - 18.0F,
                cy + 52.0F,
                magenta
        );

        canvas.line(
                cx,
                cy + 30.0F,
                cx + 18.0F,
                cy + 52.0F,
                magenta
        );
    }

    private void renderStoreStations(
            F35DisplayCanvas canvas,
            F35StoresSnapshot stores
    ) {
        float cx =
                108.0F;

        float cy =
                205.0F;

        int magenta =
                F35DisplayPalette.MAGENTA;

        float[][] stations =
                new float[][]{
                        {cx - 76.0F, cy - 55.0F},
                        {cx + 48.0F, cy - 55.0F},
                        {cx - 82.0F, cy - 13.0F},
                        {cx + 54.0F, cy - 13.0F},
                        {cx - 76.0F, cy + 35.0F},
                        {cx + 48.0F, cy + 35.0F}
                };

        List<F35StoresSnapshot.Station> stationData =
                stores.stations();

        for (int i = 0;
             i < stations.length;
             i++) {
            float x =
                    stations[i][0];

            float y =
                    stations[i][1];

            F35StoresSnapshot.Station station =
                    i < stationData.size()
                            ? stationData.get(
                            i
                    )
                            : null;

            int color =
                    station != null
                            && station.selected()
                            ? F35DisplayPalette.GREEN
                            : magenta;

            canvas.rect(
                    x,
                    y,
                    24.0F,
                    24.0F,
                    color
            );

            if (station != null) {
                canvas.text(
                        station.label(),
                        x + 3.0F,
                        y + 4.0F,
                        0.62F,
                        color
                );

                canvas.text(
                        String.valueOf(
                                station.count()
                        ),
                        x + 9.0F,
                        y + 14.0F,
                        0.72F,
                        F35DisplayPalette.WHITE
                );
            }
        }
    }

    private void renderSensor(
            F35DisplayCanvas canvas,
            F35DisplayState state
    ) {
        canvas.setClip(
                DIVIDER_1,
                TOP_BAR_HEIGHT,
                DIVIDER_2,
                VIRTUAL_HEIGHT
        );

        canvas.text(
                "TFLIR",
                225.0F,
                49.0F,
                1.0F,
                F35DisplayPalette.GREEN
        );

        canvas.text(
                "A-S",
                225.0F,
                59.0F,
                0.9F,
                F35DisplayPalette.GREEN
        );

        canvas.text(
                "CNTL>",
                386.0F,
                49.0F,
                0.9F,
                F35DisplayPalette.CYAN
        );

        float centerX =
                323.0F;

        float centerY =
                151.0F;

        float radius =
                64.0F;

        canvas.circle(
                centerX,
                centerY,
                radius,
                F35DisplayPalette.MAGENTA
        );

        canvas.cross(
                centerX,
                centerY,
                24.0F,
                F35DisplayPalette.WHITE
        );


        double sensorRangeMeters =
                sensorDetectionRangeMeters(
                        state
                );

        renderSensorDetectionContacts(
                canvas,
                state,
                centerX,
                centerY,
                radius - 6.0F,
                sensorRangeMeters
        );

        canvas.line(
                centerX - 34.0F,
                centerY,
                centerX - 22.0F,
                centerY,
                F35DisplayPalette.WHITE
        );

        canvas.line(
                centerX + 22.0F,
                centerY,
                centerX + 34.0F,
                centerY,
                F35DisplayPalette.WHITE
        );

        canvas.text(
                "EXP2",
                392.0F,
                85.0F,
                0.9F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                "SEND>",
                392.0F,
                123.0F,
                0.9F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                "STOR>",
                392.0F,
                160.0F,
                0.9F,
                F35DisplayPalette.GREEN
        );

        canvas.text(
                "RECL>",
                392.0F,
                199.0F,
                0.9F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                "SLAVE",
                302.0F,
                224.0F,
                0.9F,
                F35DisplayPalette.CYAN
        );

        F35RadarTrackView selected =
                state.selectedTrack();

        if (selected != null
                && radarTrackVisible(
                selected
        )) {
            float[] projected =
                    projectRelative(
                            state.ownship(),
                            selected,
                            radius
                                    * 0.72F,
                            centerX,
                            centerY
                    );

            drawTrackSymbol(
                    canvas,
                    selected,
                    projected[0],
                    projected[1],
                    true
            );

            canvas.text(
                    selected.shortId(),
                    238.0F,
                    246.0F,
                    0.9F,
                    F35DisplayPalette.WHITE
            );

            canvas.text(
                    "R "
                            + format0(
                            selected.position()
                                    .distanceTo(
                                            state.ownship()
                                                    .position()
                                    )
                    ),
                    238.0F,
                    258.0F,
                    0.9F,
                    F35DisplayPalette.WHITE
            );
        }

        canvas.rect(
                221.0F,
                291.0F,
                100.0F,
                33.0F,
                F35DisplayPalette.GRID
        );

        canvas.text(
                "SENS / RADAR",
                230.0F,
                299.0F,
                0.9F,
                F35DisplayPalette.WHITE
        );

        canvas.text(
                visibleContactCount(
                        state
                )
                        + " CONTACTS",
                230.0F,
                311.0F,
                0.8F,
                F35DisplayPalette.GREEN
        );


        canvas.text(
                "DET "
                        + rangeLabel(
                        sensorRangeMeters
                ),
                337.0F,
                311.0F,
                0.72F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                "IDS",
                373.0F,
                328.0F,
                1.0F,
                F35DisplayPalette.GREEN
        );

        canvas.clearClip();
    }

    private void renderTsd(
            F35DisplayCanvas canvas,
            F35DisplayState state
    ) {
        canvas.setClip(
                DIVIDER_2,
                TOP_BAR_HEIGHT,
                DIVIDER_3,
                VIRTUAL_HEIGHT
        );

        canvas.text(
                "TSD1",
                440.0F,
                49.0F,
                1.0F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                "A-S",
                440.0F,
                59.0F,
                0.9F,
                F35DisplayPalette.GREEN
        );

        canvas.text(
                "MODE",
                483.0F,
                49.0F,
                0.9F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                "TWS",
                484.0F,
                59.0F,
                0.9F,
                F35DisplayPalette.WHITE
        );

        canvas.text(
                "RESET",
                574.0F,
                49.0F,
                0.9F,
                F35DisplayPalette.CYAN
        );

        float cx =
                537.5F;

        float cy =
                329.0F;

        int ring =
                F35DisplayPalette.WHITE;

        canvas.arc(
                cx,
                cy,
                62.0F,
                205.0F,
                335.0F,
                24,
                ring
        );

        canvas.arc(
                cx,
                cy,
                115.0F,
                205.0F,
                335.0F,
                32,
                ring
        );

        canvas.arc(
                cx,
                cy,
                168.0F,
                205.0F,
                335.0F,
                40,
                F35DisplayPalette.GRID
        );


        renderTsdReferenceDecorations(
                canvas,
                state,
                cx,
                cy,
                168.0F
        );

        canvas.text(
                rangeLabel(
                        state.radarRangeMeters()
                ),
                522.0F,
                91.0F,
                0.8F,
                F35DisplayPalette.WHITE
        );

        canvas.cross(
                cx,
                167.0F,
                8.0F,
                F35DisplayPalette.GREEN
        );

        double localDetectionRangeMeters =
                sensorDetectionRangeMeters(
                        state
                );

        canvas.text(
                "DET "
                        + rangeLabel(
                        localDetectionRangeMeters
                ),
                442.0F,
                76.0F,
                0.72F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                detectionSummary(
                        state
                ),
                553.0F,
                76.0F,
                0.72F,
                F35DisplayPalette.GREEN
        );

        renderSelectedBearingLine(
                canvas,
                state,
                cx,
                cy - 15.0F,
                cx,
                cy,
                160.0F,
                false
        );

        renderTracks(
                canvas,
                state,
                cx,
                cy,
                160.0F,
                false
        );

        renderDetectionContacts(
                canvas,
                state,
                cx,
                cy,
                160.0F,
                false
        );

        canvas.aircraft(
                cx,
                cy - 15.0F,
                9.0F,
                F35DisplayPalette.CYAN
        );

        renderSelectedTrackBox(
                canvas,
                state,
                452.0F,
                279.0F
        );

        canvas.text(
                "HSD",
                615.0F,
                328.0F,
                1.0F,
                F35DisplayPalette.GREEN
        );

        canvas.clearClip();
    }

    private void renderHsi(
            F35DisplayCanvas canvas,
            F35DisplayState state
    ) {
        canvas.setClip(
                DIVIDER_3,
                TOP_BAR_HEIGHT,
                VIRTUAL_WIDTH,
                VIRTUAL_HEIGHT
        );

        canvas.text(
                "TSD2",
                656.0F,
                49.0F,
                1.0F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                "A-S",
                656.0F,
                59.0F,
                0.9F,
                F35DisplayPalette.GREEN
        );

        canvas.text(
                "MODE",
                701.0F,
                49.0F,
                0.9F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                "TWS",
                701.0F,
                59.0F,
                0.9F,
                F35DisplayPalette.WHITE
        );

        canvas.text(
                "RESET",
                792.0F,
                49.0F,
                0.9F,
                F35DisplayPalette.CYAN
        );

        float cx =
                753.0F;

        float cy =
                224.0F;

        canvas.circle(
                cx,
                cy,
                39.0F,
                F35DisplayPalette.GRID
        );

        canvas.circle(
                cx,
                cy,
                75.0F,
                F35DisplayPalette.GRID
        );

        canvas.circle(
                cx,
                cy,
                112.0F,
                F35DisplayPalette.WHITE
        );


        renderHsiReferenceDecorations(
                canvas,
                state,
                cx,
                cy,
                112.0F
        );

        canvas.aircraft(
                cx,
                cy,
                11.0F,
                F35DisplayPalette.CYAN
        );

        canvas.line(
                cx,
                cy - 122.0F,
                cx,
                cy - 112.0F,
                F35DisplayPalette.WHITE
        );

        canvas.text(
                threeDigits(
                        state.ownship()
                                .headingDeg()
                ),
                cx - 11.0F,
                86.0F,
                0.9F,
                F35DisplayPalette.WHITE
        );

        double localDetectionRangeMeters =
                sensorDetectionRangeMeters(
                        state
                );

        canvas.text(
                "DET "
                        + rangeLabel(
                        localDetectionRangeMeters
                ),
                656.0F,
                76.0F,
                0.72F,
                F35DisplayPalette.CYAN
        );

        canvas.text(
                detectionSummary(
                        state
                ),
                782.0F,
                76.0F,
                0.72F,
                F35DisplayPalette.GREEN
        );

        renderSelectedBearingLine(
                canvas,
                state,
                cx,
                cy,
                cx,
                cy,
                108.0F,
                true
        );

        renderTracks(
                canvas,
                state,
                cx,
                cy,
                108.0F,
                true
        );

        renderDetectionContacts(
                canvas,
                state,
                cx,
                cy,
                108.0F,
                true
        );

        canvas.text(
                "HSI",
                732.0F,
                328.0F,
                1.0F,
                F35DisplayPalette.GREEN
        );

        canvas.text(
                "VAR",
                811.0F,
                328.0F,
                1.0F,
                F35DisplayPalette.GREEN
        );

        canvas.clearClip();
    }

    private void renderTsdReferenceDecorations(
            F35DisplayCanvas canvas,
            F35DisplayState state,
            float cx,
            float cy,
            float outerRadius
    ) {
        float[] left =
                polarPoint(
                        cx,
                        cy,
                        outerRadius,
                        205.0
                );

        float[] right =
                polarPoint(
                        cx,
                        cy,
                        outerRadius,
                        335.0
                );

        canvas.line(
                cx,
                cy - 15.0F,
                left[0],
                left[1],
                F35DisplayPalette.GRID
        );

        canvas.line(
                cx,
                cy - 15.0F,
                right[0],
                right[1],
                F35DisplayPalette.GRID
        );

        canvas.text(
                rangeLabel(
                        state.radarRangeMeters()
                                / 3.0
                ),
                470.0F,
                148.0F,
                0.58F,
                F35DisplayPalette.WHITE
        );

        canvas.text(
                rangeLabel(
                        state.radarRangeMeters()
                                * 2.0
                                / 3.0
                ),
                594.0F,
                148.0F,
                0.58F,
                F35DisplayPalette.WHITE
        );

        canvas.text(
                "TGT "
                        + F35DisplayClientConfig.targetSelectionSummary(),
                570.0F,
                91.0F,
                0.62F,
                F35DisplayPalette.GREEN
        );

        canvas.text(
                F35DisplayClientConfig.trackTrails()
                        ? "TRAIL ON"
                        : "TRAIL OFF",
                442.0F,
                91.0F,
                0.58F,
                F35DisplayPalette.CYAN
        );
    }

    private void renderHsiReferenceDecorations(
            F35DisplayCanvas canvas,
            F35DisplayState state,
            float cx,
            float cy,
            float outerRadius
    ) {
        double ownHeading =
                state.ownship()
                        .headingDeg();

        for (int heading = 0;
             heading < 360;
             heading += 30) {
            double relative =
                    Math.toRadians(
                            heading
                                    - ownHeading
                    );

            float sin =
                    (float) Math.sin(
                            relative
                    );

            float cos =
                    (float) Math.cos(
                            relative
                    );

            float x1 =
                    cx
                            + sin
                            * (outerRadius - 8.0F);

            float y1 =
                    cy
                            - cos
                            * (outerRadius - 8.0F);

            float x2 =
                    cx
                            + sin
                            * outerRadius;

            float y2 =
                    cy
                            - cos
                            * outerRadius;

            canvas.line(
                    x1,
                    y1,
                    x2,
                    y2,
                    F35DisplayPalette.WHITE
            );

            float tx =
                    cx
                            + sin
                            * (outerRadius - 18.0F)
                            - 5.0F;

            float ty =
                    cy
                            - cos
                            * (outerRadius - 18.0F)
                            - 3.0F;

            canvas.text(
                    String.format(
                            "%02d",
                            heading / 10
                    ),
                    tx,
                    ty,
                    0.48F,
                    F35DisplayPalette.DIM
            );
        }

        canvas.text(
                rangeLabel(
                        state.radarRangeMeters()
                                / 3.0
                ),
                cx + 7.0F,
                cy - 39.0F,
                0.48F,
                F35DisplayPalette.DIM
        );

        canvas.text(
                rangeLabel(
                        state.radarRangeMeters()
                                * 2.0
                                / 3.0
                ),
                cx + 7.0F,
                cy - 75.0F,
                0.48F,
                F35DisplayPalette.DIM
        );
    }

    private void renderSelectedBearingLine(
            F35DisplayCanvas canvas,
            F35DisplayState state,
            float startX,
            float startY,
            float centerX,
            float centerY,
            float radius,
            boolean fullCircle
    ) {
        Vec3 targetPosition =
                null;

        F35RadarTrackView selected =
                state.selectedTrack();

        if (selected != null
                && radarTrackVisible(
                selected
        )) {
            targetPosition =
                    selected.position();
        } else {
            F35DetectionContact detection =
                    F35TargetLockClient.lockedDetection(
                            state.detections()
                    );

            if (detection != null
                    && detectionContactVisible(
                    detection
            )) {
                targetPosition =
                        detection.position();
            }
        }

        if (targetPosition == null) {
            return;
        }

        float[] target =
                scopePoint(
                        state.ownship(),
                        targetPosition,
                        state.radarRangeMeters(),
                        centerX,
                        centerY,
                        radius,
                        fullCircle
                );

        if (target == null) {
            return;
        }

        renderDashedLine(
                canvas,
                startX,
                startY,
                target[0],
                target[1],
                F35DisplayPalette.WHITE
        );
    }

    private void renderDashedLine(
            F35DisplayCanvas canvas,
            float x1,
            float y1,
            float x2,
            float y2,
            int color
    ) {
        int segments =
                12;

        for (int i = 0;
             i < segments;
             i += 2) {
            float a =
                    (float) i
                            / segments;

            float b =
                    (float) (i + 1)
                            / segments;

            canvas.line(
                    x1 + (x2 - x1) * a,
                    y1 + (y2 - y1) * a,
                    x1 + (x2 - x1) * b,
                    y1 + (y2 - y1) * b,
                    dim(
                            color,
                            0.72F
                    )
            );
        }
    }

    private static float[] polarPoint(
            float cx,
            float cy,
            float radius,
            double degrees
    ) {
        double radians =
                Math.toRadians(
                        degrees
                );

        return new float[]{
                cx + (float) Math.cos(
                        radians
                ) * radius,
                cy + (float) Math.sin(
                        radians
                ) * radius
        };
    }

    private void renderTracks(
            F35DisplayCanvas canvas,
            F35DisplayState state,
            float centerX,
            float centerY,
            float radius,
            boolean fullCircle
    ) {
        AircraftTelemetry ownship =
                state.ownship();

        for (F35RadarTrackView track :
                state.tracks()) {
            if (!radarTrackVisible(
                    track
            )) {
                continue;
            }

            float[] point =
                    scopePoint(
                            ownship,
                            track.position(),
                            state.radarRangeMeters(),
                            centerX,
                            centerY,
                            radius,
                            fullCircle
                    );

            if (point == null) {
                continue;
            }

            int color =
                    trackColor(
                            track
                    );

            if (F35DisplayClientConfig.trackTrails()) {
                renderRadarTrail(
                        canvas,
                        state,
                        track,
                        centerX,
                        centerY,
                        radius,
                        fullCircle,
                        color
                );
            }

            if (F35DisplayClientConfig.velocityVectors()) {
                renderVelocityVector(
                        canvas,
                        ownship,
                        track.position(),
                        track.velocity(),
                        6.0,
                        state.radarRangeMeters(),
                        centerX,
                        centerY,
                        radius,
                        fullCircle,
                        point[0],
                        point[1],
                        color
                );
            }

            drawTrackSymbol(
                    canvas,
                    track,
                    point[0],
                    point[1],
                    state.selectedTrack() != null
                            && state.selectedTrack()
                            .trackId()
                            .equals(
                                    track.trackId()
                            )
            );
        }
    }

    private void renderRadarTrail(
            F35DisplayCanvas canvas,
            F35DisplayState state,
            F35RadarTrackView track,
            float centerX,
            float centerY,
            float radius,
            boolean fullCircle,
            int color
    ) {
        renderTrail(
                canvas,
                state.ownship(),
                F35TrackTrailCache.radarTrail(
                        track.trackId()
                ),
                state.radarRangeMeters(),
                centerX,
                centerY,
                radius,
                fullCircle,
                color
        );
    }

    private void renderDetectionTrail(
            F35DisplayCanvas canvas,
            F35DisplayState state,
            F35DetectionContact contact,
            double rangeMeters,
            float centerX,
            float centerY,
            float radius,
            boolean fullCircle,
            int color
    ) {
        renderTrail(
                canvas,
                state.ownship(),
                F35TrackTrailCache.detectionTrail(
                        contact.contactId()
                ),
                rangeMeters,
                centerX,
                centerY,
                radius,
                fullCircle,
                color
        );
    }

    private void renderTrail(
            F35DisplayCanvas canvas,
            AircraftTelemetry ownship,
            List<F35TrackTrailCache.TrailPoint> trail,
            double rangeMeters,
            float centerX,
            float centerY,
            float radius,
            boolean fullCircle,
            int color
    ) {
        if (trail.size() < 2) {
            return;
        }

        float[] previous =
                null;

        for (int i = 0;
             i < trail.size();
             i++) {
            F35TrackTrailCache.TrailPoint sample =
                    trail.get(
                            i
                    );

            float[] point =
                    scopePoint(
                            ownship,
                            sample.position(),
                            rangeMeters,
                            centerX,
                            centerY,
                            radius,
                            fullCircle
                    );

            if (point == null) {
                previous =
                        null;
                continue;
            }

            if (previous != null) {
                float fraction =
                        trail.size() <= 1
                                ? 1.0F
                                : (float) i
                                / (float) (trail.size() - 1);

                int trailColor =
                        dim(
                                color,
                                0.24F
                                        + fraction
                                        * 0.66F
                        );

                canvas.line(
                        previous[0],
                        previous[1],
                        point[0],
                        point[1],
                        trailColor
                );
            }

            previous =
                    point;
        }
    }

    private void renderVelocityVector(
            F35DisplayCanvas canvas,
            AircraftTelemetry ownship,
            Vec3 position,
            Vec3 velocity,
            double seconds,
            double rangeMeters,
            float centerX,
            float centerY,
            float radius,
            boolean fullCircle,
            float startX,
            float startY,
            int color
    ) {
        if (velocity.lengthSqr()
                < 1.0E-6) {
            return;
        }

        Vec3 future =
                position.add(
                        velocity.scale(
                                seconds
                        )
                );

        float[] end =
                scopePoint(
                        ownship,
                        future,
                        rangeMeters,
                        centerX,
                        centerY,
                        radius,
                        fullCircle
                );

        if (end == null) {
            return;
        }

        canvas.line(
                startX,
                startY,
                end[0],
                end[1],
                dim(
                        color,
                        0.78F
                )
        );
    }

    private static float[] scopePoint(
            AircraftTelemetry ownship,
            Vec3 target,
            double rangeMeters,
            float centerX,
            float centerY,
            float radius,
            boolean fullCircle
    ) {
        if (rangeMeters <= 0.0) {
            return null;
        }

        Relative relative =
                relativeToOwnship(
                        ownship,
                        target
                );

        double normalizedRight =
                relative.right()
                        / rangeMeters;

        double normalizedForward =
                relative.forward()
                        / rangeMeters;

        if (!com.k1ngtle.vsia.cockpit.network.F35RadarContactProjection.forwardVisible(fullCircle,normalizedForward)) {
            return null;
        }

        double radial =
                Math.sqrt(
                        normalizedRight
                                * normalizedRight
                                + normalizedForward
                                * normalizedForward
                );

        if (radial > 1.0) {
            return null;
        }

        return new float[]{
                centerX
                        + (float) normalizedRight
                        * radius,
                centerY
                        - (float) normalizedForward
                        * radius
        };
    }

    private void renderSensorDetectionContacts(
            F35DisplayCanvas canvas,
            F35DisplayState state,
            float centerX,
            float centerY,
            float radius,
            double rangeMeters
    ) {
        if (rangeMeters <= 0.0) {
            return;
        }

        AircraftTelemetry ownship =
                state.ownship();

        for (F35DetectionContact contact :
                state.detections()) {
            if (!detectionContactVisible(
                    contact
            )) {
                continue;
            }

            float[] point =
                    scopePoint(
                            ownship,
                            contact.position(),
                            rangeMeters,
                            centerX,
                            centerY,
                            radius,
                            true
                    );

            if (point == null) {
                continue;
            }

            drawDetectionSymbol(
                    canvas,
                    contact,
                    point[0],
                    point[1]
            );
        }
    }

    private static double sensorDetectionRangeMeters(
            F35DisplayState state
    ) {
        double farthest =
                0.0;

        for (F35DetectionContact contact :
                state.detections()) {
            if (!detectionContactVisible(
                    contact
            )) {
                continue;
            }

            farthest =
                    Math.max(
                            farthest,
                            contact.position()
                                    .distanceTo(
                                            state.ownship()
                                                    .position()
                                    )
                    );
        }

        if (farthest <= 0.0) {
            return 50.0;
        }

        double requested =
                farthest
                        * 1.15;

        double[] steps =
                new double[]{
                        50.0,
                        100.0,
                        250.0,
                        500.0,
                        1000.0,
                        5000.0,
                        10_000.0,
                        20_000.0,
                        40_000.0,
                        80_000.0
                };

        for (double step :
                steps) {
            if (requested <= step) {
                return step;
            }
        }

        return 80_000.0;
    }

    private void renderDetectionContacts(
            F35DisplayCanvas canvas,
            F35DisplayState state,
            float centerX,
            float centerY,
            float radius,
            boolean fullCircle
    ) {
        AircraftTelemetry ownship =
                state.ownship();

        double detectionRangeMeters =
                sensorDetectionRangeMeters(
                        state
                );

        for (F35DetectionContact contact :
                state.detections()) {
            if (!detectionContactVisible(
                    contact
            )) {
                continue;
            }

            float[] point =
                    scopePoint(
                            ownship,
                            contact.position(),
                            detectionRangeMeters,
                            centerX,
                            centerY,
                            radius,
                            fullCircle
                    );

            if (point == null) {
                continue;
            }

            int color =
                    detectionColor(
                            contact
                    );

            boolean showTrail =
                    contact.type()
                            == F35DetectionType.MISSILE
                            ? F35DisplayClientConfig.missileTrails()
                            : F35DisplayClientConfig.trackTrails();

            if (showTrail) {
                renderDetectionTrail(
                        canvas,
                        state,
                        contact,
                        detectionRangeMeters,
                        centerX,
                        centerY,
                        radius,
                        fullCircle,
                        color
                );
            }

            if (F35DisplayClientConfig.velocityVectors()) {
                renderVelocityVector(
                        canvas,
                        ownship,
                        contact.position(),
                        contact.velocity(),
                        contact.type()
                                == F35DetectionType.MISSILE
                                ? 3.0
                                : 5.0,
                        detectionRangeMeters,
                        centerX,
                        centerY,
                        radius,
                        fullCircle,
                        point[0],
                        point[1],
                        color
                );
            }

            drawDetectionSymbol(
                    canvas,
                    contact,
                    point[0],
                    point[1]
            );
        }
    }

    private static String detectionSummary(
            F35DisplayState state
    ) {
        int mobs = 0;
        int players = 0;
        int ships = 0;
        int missiles = 0;

        for (F35DetectionContact contact :
                state.detections()) {
            if (!detectionContactVisible(
                    contact
            )) {
                continue;
            }

            if (contact.type()
                    == F35DetectionType.MOB) {
                mobs++;
            } else if (contact.type()
                    == F35DetectionType.PLAYER) {
                players++;
            } else if (contact.type()
                    == F35DetectionType.SHIP) {
                ships++;
            } else if (contact.type()
                    == F35DetectionType.MISSILE) {
                missiles++;
            }
        }

        return "M"
                + mobs
                + " P"
                + players
                + " S"
                + ships
                + " X"
                + missiles;
    }

    private static boolean radarTrackVisible(
            F35RadarTrackView track
    ) {
        return F35DisplayClientConfig.radarTrackVisible(
                track.iffAffiliation()
        );
    }

    private static boolean detectionContactVisible(
            F35DetectionContact contact
    ) {
        return switch (contact.type()) {
            case MOB ->
                    F35DisplayClientConfig.detectMobs();
            case PLAYER ->
                    F35DisplayClientConfig.detectPlayers();
            case SHIP ->
                    F35DisplayClientConfig.detectShips();
            case MISSILE ->
                    F35DisplayClientConfig.showMissiles();
        };
    }

    private static int detectionColor(
            F35DetectionContact contact
    ) {
        if(contact.iffAuthenticated()&&contact.iffStatus().startsWith("FRIENDLY"))return F35DisplayPalette.GREEN;
        return switch (contact.type()) {
            case SHIP -> F35DisplayPalette.AMBER;
            case PLAYER -> F35DisplayPalette.AMBER;
            case MISSILE -> F35DisplayPalette.RED;
            case MOB -> F35DisplayPalette.AMBER;
        };
    }

    private void drawDetectionSymbol(
            F35DisplayCanvas canvas,
            F35DetectionContact contact,
            float x,
            float y
    ) {
        int color =
                detectionColor(
                        contact
                );

        if (contact.type()
                == F35DetectionType.SHIP) {
            canvas.aircraft(
                    x,
                    y,
                    6.0F,
                    color
            );
        } else if (contact.type()
                == F35DetectionType.PLAYER) {
            canvas.rect(
                    x - 4.0F,
                    y - 4.0F,
                    8.0F,
                    8.0F,
                    color
            );
        } else if (contact.type()
                == F35DetectionType.MISSILE) {
            canvas.triangle(
                    x,
                    y,
                    5.5F,
                    color
            );

            canvas.line(
                    x,
                    y + 6.0F,
                    x,
                    y + 11.0F,
                    dim(
                            color,
                            0.72F
                    )
            );
        } else {
            canvas.diamond(
                    x,
                    y,
                    4.0F,
                    color
            );
        }

        if (F35TargetLockClient.isDetectionLocked(
                contact.contactId()
        )) {
            canvas.rect(
                    x - 8.0F,
                    y - 8.0F,
                    16.0F,
                    16.0F,
                    F35DisplayPalette.WHITE
            );
        }

        if (F35DisplayClientConfig.trackLabels()) {
            canvas.text(
                    contact.type()
                            == F35DetectionType.MISSILE
                            ? "MSL "
                            + contact.shortId()
                            : contact.type()
                            .name()
                            .substring(
                                    0,
                                    1
                            )
                            + contact.shortId(),
                    x + 7.0F,
                    y - 4.0F,
                    0.55F,
                    color
            );
        }
    }

    private void drawTrackSymbol(
            F35DisplayCanvas canvas,
            F35RadarTrackView track,
            float x,
            float y,
            boolean selected
    ) {
        int color =
                trackColor(
                        track
                );

        if ("COASTING".equals(
                track.trackState()
        )) {
            color =
                    dim(
                            color,
                            0.62F
                    );
        }

        String affiliation =
                track.iffAffiliation();

        if (track.iffAuthenticated() && affiliation.contains(
                "FRIENDLY"
        )) {
            color =
                    "COASTING".equals(
                            track.trackState()
                    )
                            ? dim(
                            F35DisplayPalette.GREEN,
                            0.62F
                    )
                            : F35DisplayPalette.GREEN;

            canvas.aircraft(
                    x,
                    y,
                    5.5F,
                    color
            );
        } else if (affiliation.contains(
                "HOSTILE"
        )) {
            canvas.triangle(
                    x,
                    y,
                    5.5F,
                    color
            );
        } else {
            canvas.diamond(
                    x,
                    y,
                    4.0F,
                    color
            );
        }

        if (selected) {
            canvas.rect(
                    x - 8.0F,
                    y - 8.0F,
                    16.0F,
                    16.0F,
                    F35DisplayPalette.WHITE
            );
        }

        if (F35DisplayClientConfig.trackLabels()) {
            canvas.text(
                    track.shortId(),
                    x + 7.0F,
                    y - 4.0F,
                    0.6F,
                    color
            );
        }
    }

    private void renderSelectedTrackBox(
            F35DisplayCanvas canvas,
            F35DisplayState state,
            float x,
            float y
    ) {
        F35RadarTrackView selected =
                state.selectedTrack();

        if (selected != null
                && !radarTrackVisible(
                selected
        )) {
            selected =
                    null;
        }

        F35DetectionContact detection =
                selected == null
                        ? F35TargetLockClient.lockedDetection(
                        state.detections()
                )
                        : null;

        if (detection != null
                && !detectionContactVisible(
                detection
        )) {
            detection =
                    null;
        }

        canvas.rect(
                x,
                y,
                77.0F,
                43.0F,
                F35DisplayPalette.CYAN
        );

        if (F35TargetLockClient.targetCoasting()) {
            canvas.text(
                    "COAST",
                    x + 43.0F,
                    y + 5.0F,
                    0.55F,
                    F35DisplayPalette.AMBER
            );
        }

        if (selected == null
                && detection == null) {
            canvas.text(
                    "NO LOCK",
                    x + 5.0F,
                    y + 8.0F,
                    0.8F,
                    F35DisplayPalette.DIM
            );

            return;
        }

        if (selected != null) {
            double range =
                    selected.position()
                            .distanceTo(
                                    state.ownship()
                                            .position()
                            );

            canvas.text(
                    selected.shortId(),
                    x + 5.0F,
                    y + 5.0F,
                    0.8F,
                    trackColor(
                            selected
                    )
            );
            canvas.text(selected.iffAuthenticated()?(selected.iffTelemetry().startsWith("M5")?"FRIEND M5":"FRIEND M4"):"IFF UNKNOWN",x+5.0F,y+36.0F,0.55F,trackColor(selected));

            canvas.text(
                    "R "
                            + format0(
                            range
                    ),
                    x + 5.0F,
                    y + 16.0F,
                    0.7F,
                    F35DisplayPalette.WHITE
            );

            canvas.text(
                    "V "
                            + format0(
                            selected.speedMps()
                    ),
                    x + 5.0F,
                    y + 27.0F,
                    0.7F,
                    F35DisplayPalette.WHITE
            );
            return;
        }

        double range =
                detection.position()
                        .distanceTo(
                                state.ownship()
                                        .position()
                        );

        canvas.text(
                detection.shortId(),
                x + 5.0F,
                y + 5.0F,
                0.8F,
                detectionColor(
                        detection
                )
        );
        canvas.text(detection.iffAuthenticated()?(detection.iffTelemetry().startsWith("M5")?"FRIEND M5":"FRIEND M4"):"IFF UNKNOWN",x+5.0F,y+36.0F,0.55F,detectionColor(detection));

        canvas.text(
                "R "
                        + format0(
                        range
                ),
                x + 5.0F,
                y + 16.0F,
                0.7F,
                F35DisplayPalette.WHITE
        );

        canvas.text(
                "V "
                        + format0(
                        detection.speedMps()
                ),
                x + 5.0F,
                y + 27.0F,
                0.7F,
                F35DisplayPalette.WHITE
        );
    }

    private static float[] projectRelative(
            AircraftTelemetry ownship,
            F35RadarTrackView track,
            float radius,
            float centerX,
            float centerY
    ) {
        Relative relative =
                relativeToOwnship(
                        ownship,
                        track.position()
                );

        double length =
                Math.sqrt(
                        relative.right()
                                * relative.right()
                                + relative.forward()
                                * relative.forward()
                );

        if (length
                < 1.0E-6) {
            return new float[]{
                    centerX,
                    centerY
            };
        }

        double clamped =
                Math.min(
                        radius,
                        length
                                * 0.15
                );

        return new float[]{
                centerX
                        + (float) (
                        relative.right()
                                / length
                                * clamped
                ),
                centerY
                        - (float) (
                        relative.forward()
                                / length
                                * clamped
                )
        };
    }

    private static Relative relativeToOwnship(
            AircraftTelemetry ownship,
            Vec3 target
    ) {
        double heading =
                Math.toRadians(
                        ownship.headingDeg()
                );

        double forwardX =
                -Math.sin(
                        heading
                );

        double forwardZ =
                Math.cos(
                        heading
                );

        double rightX =
                -Math.cos(
                        heading
                );

        double rightZ =
                -Math.sin(
                        heading
                );

        double dx =
                target.x
                        - ownship.position()
                        .x;

        double dz =
                target.z
                        - ownship.position()
                        .z;

        double forward =
                dx
                        * forwardX
                        + dz
                        * forwardZ;

        double right =
                dx
                        * rightX
                        + dz
                        * rightZ;

        return new Relative(
                forward,
                right
        );
    }

    private static int trackColor(
            F35RadarTrackView track
    ) {
        if (track.iffAuthenticated() && track.iffAffiliation()
                .contains(
                        "FRIENDLY"
                )) {
            return F35DisplayPalette.GREEN;
        }

        if (track.iffAffiliation()
                .contains(
                        "HOSTILE"
                )) {
            return F35DisplayPalette.RED;
        }

        return F35DisplayPalette.AMBER;
    }

    private static int visibleContactCount(
            F35DisplayState state
    ) {
        int count =
                0;

        for (F35RadarTrackView track :
                state.tracks()) {
            if (radarTrackVisible(
                    track
            )) {
                count++;
            }
        }

        for (F35DetectionContact contact :
                state.detections()) {
            if (detectionContactVisible(
                    contact
            )) {
                count++;
            }
        }

        return count;
    }

    private static String iffSummary(
            F35DisplayState state
    ) {
        long friendlies =
                state.tracks()
                        .stream()
                        .filter(
                                track ->
                                        radarTrackVisible(
                                                track
                                        )
                                                && track.iffAuthenticated()
                                                && track.iffAffiliation()
                                                .contains(
                                                        "FRIENDLY"
                                                )
                        )
                        .count();

        if (friendlies > 0) {
            return "F "
                    + friendlies;
        }

        if (visibleContactCount(
                state
        ) == 0) {
            return "STBY";
        }

        return "UNK "
                + state.tracks()
                .size();
    }

    private static String rangeLabel(
            double meters
    ) {
        if (meters
                >= 1000.0) {
            return format0(
                    meters / 1000.0
            )
                    + "KM";
        }

        return format0(
                meters
        )
                + "M";
    }

    private static String shipLine(
            F35DisplayState state
    ) {
        return shortLabel(
                state.ownship()
                        .shipDetected()
                        ? state.ownship()
                        .shipLabel()
                        : "---No Ship Detected---",
                18
        );
    }

    private static String shortLabel(
            String value,
            int maxLength
    ) {
        if (value == null) {
            return "";
        }

        if (value.length()
                <= maxLength) {
            return value;
        }

        return value.substring(
                0,
                Math.max(
                        0,
                        maxLength - 3
                )
        ) + "...";
    }

    private static String threeDigits(
            double value
    ) {
        int rounded =
                (int) Math.round(
                        value
                )
                        % 360;

        if (rounded < 0) {
            rounded += 360;
        }

        return String.format(
                "%03d",
                rounded
        );
    }

    private static String format0(
            double value
    ) {
        return String.format(
                "%.0f",
                value
        );
    }

    private static String format1(
            double value
    ) {
        return String.format(
                "%.1f",
                value
        );
    }

    private static int dim(
            int color,
            float factor
    ) {
        int alpha =
                color >>> 24
                        & 0xFF;

        int red =
                color >>> 16
                        & 0xFF;

        int green =
                color >>> 8
                        & 0xFF;

        int blue =
                color
                        & 0xFF;

        red =
                (int) (
                        red
                                * factor
                );

        green =
                (int) (
                        green
                                * factor
                );

        blue =
                (int) (
                        blue
                                * factor
                );

        return alpha
                << 24
                | red
                << 16
                | green
                << 8
                | blue;
    }

    private record Relative(
            double forward,
            double right
    ) {
    }
}
