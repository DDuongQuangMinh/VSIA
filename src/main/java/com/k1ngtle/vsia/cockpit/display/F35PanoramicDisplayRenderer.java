package com.k1ngtle.vsia.cockpit.display;

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

        renderFrame(
                canvas,
                state
        );

        poseStack.popPose();
    }

    private void renderFrame(
            F35DisplayCanvas canvas,
            F35DisplayState state
    ) {
        canvas.clearClip();

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
                        + state.tracks()
                        .size(),
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

        renderStoresAircraft(
                canvas,
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

    private void renderStoresAircraft(
            F35DisplayCanvas canvas,
            F35StoresSnapshot stores
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

        float[][] stations =
                new float[][]{
                        {cx - 55.0F, cy - 55.0F},
                        {cx + 27.0F, cy - 55.0F},
                        {cx - 66.0F, cy - 13.0F},
                        {cx + 38.0F, cy - 13.0F},
                        {cx - 58.0F, cy + 35.0F},
                        {cx + 30.0F, cy + 35.0F}
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
                    28.0F,
                    28.0F,
                    color
            );

            if (station != null) {
                canvas.text(
                        station.label(),
                        x + 4.0F,
                        y + 5.0F,
                        0.7F,
                        color
                );

                canvas.text(
                        String.valueOf(
                                station.count()
                        ),
                        x + 11.0F,
                        y + 16.0F,
                        0.8F,
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

        if (selected != null) {
            float[] projected =
                    projectRelative(
                            state.ownship(),
                            selected,
                            radius
                                    * 0.72F,
                            centerX,
                            centerY
                    );

            canvas.diamond(
                    projected[0],
                    projected[1],
                    4.0F,
                    trackColor(
                            selected
                    )
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
                state.tracks()
                        .size()
                        + " TRACKS",
                230.0F,
                311.0F,
                0.8F,
                F35DisplayPalette.GREEN
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

        renderTracks(
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

        renderTracks(
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
            Relative relative =
                    relativeToOwnship(
                            ownship,
                            track.position()
                    );

            double normalizedRight =
                    relative.right()
                            / state.radarRangeMeters();

            double normalizedForward =
                    relative.forward()
                            / state.radarRangeMeters();

            if (!fullCircle
                    && normalizedForward
                    < -0.05) {
                continue;
            }

            double radial =
                    Math.sqrt(
                            normalizedRight
                                    * normalizedRight
                                    + normalizedForward
                                    * normalizedForward
                    );

            if (radial
                    > 1.0) {
                continue;
            }

            float x =
                    centerX
                            + (float) normalizedRight
                            * radius;

            float y =
                    centerY
                            - (float) normalizedForward
                            * radius;

            drawTrackSymbol(
                    canvas,
                    track,
                    x,
                    y,
                    state.selectedTrack() != null
                            && state.selectedTrack()
                            .trackId()
                            .equals(
                                    track.trackId()
                            )
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

        if (affiliation.contains(
                "FRIENDLY"
        )) {
            canvas.aircraft(
                    x,
                    y,
                    5.0F,
                    color
            );
        } else if (affiliation.contains(
                "HOSTILE"
        )) {
            canvas.triangle(
                    x,
                    y,
                    5.0F,
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

        canvas.text(
                track.shortId(),
                x + 7.0F,
                y - 4.0F,
                0.6F,
                color
        );
    }

    private void renderSelectedTrackBox(
            F35DisplayCanvas canvas,
            F35DisplayState state,
            float x,
            float y
    ) {
        F35RadarTrackView selected =
                state.selectedTrack();

        canvas.rect(
                x,
                y,
                77.0F,
                43.0F,
                F35DisplayPalette.CYAN
        );

        if (selected == null) {
            canvas.text(
                    "NO TRACK",
                    x + 5.0F,
                    y + 8.0F,
                    0.8F,
                    F35DisplayPalette.DIM
            );

            return;
        }

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
                Math.cos(
                        heading
                );

        double rightZ =
                Math.sin(
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
        if (track.iffAffiliation()
                .contains(
                        "FRIENDLY_EMERGENCY"
                )) {
            return F35DisplayPalette.AMBER;
        }

        if (track.iffAffiliation()
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

        if (track.iffReplyStatus()
                .contains(
                        "AUTH_FAILED"
                )
                || track.iffReplyStatus()
                .contains(
                        "NO_REPLY"
                )) {
            return F35DisplayPalette.AMBER;
        }

        return F35DisplayPalette.WHITE;
    }

    private static String iffSummary(
            F35DisplayState state
    ) {
        long friendlies =
                state.tracks()
                        .stream()
                        .filter(
                                track ->
                                        track.iffAffiliation()
                                                .contains(
                                                        "FRIENDLY"
                                                )
                        )
                        .count();

        if (friendlies > 0) {
            return "F "
                    + friendlies;
        }

        if (state.tracks()
                .isEmpty()) {
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
