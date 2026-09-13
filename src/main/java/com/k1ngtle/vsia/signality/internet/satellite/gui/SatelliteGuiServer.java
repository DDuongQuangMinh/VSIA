package com.k1ngtle.vsia.signality.internet.satellite.gui;

import com.k1ngtle.vsia.signality.internet.satellite.SatelliteLinkAssessment;
import com.k1ngtle.vsia.signality.internet.satellite.SatelliteNetworkManager;
import com.k1ngtle.vsia.signality.internet.satellite.VsAwareSatelliteLinkService;
import com.k1ngtle.vsia.signality.internet.satellite.device.TemporarySatelliteTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public final class SatelliteGuiServer {
    private static final double MAX_DISTANCE_SQR =
            64.0D * 64.0D;

    private SatelliteGuiServer() {
    }

    public static SatelliteGuiSnapshot snapshot(
            ServerPlayer player,
            BlockPos pos
    ) {
        TemporarySatelliteTerminalBlockEntity terminal =
                terminal(
                        player,
                        pos
                );

        if (terminal == null) {
            return SatelliteGuiSnapshot.invalid(
                    pos,
                    "Satellite terminal is missing, unloaded, or too far away"
            );
        }

        SatelliteLinkAssessment assessment =
                terminal.lastAssessment();

        if (!assessment.visible()) {
            assessment =
                    VsAwareSatelliteLinkService
                            .assessSelf(
                                    terminal
                            );

            terminal.setLastAssessment(
                    assessment
            );
        }

        return new SatelliteGuiSnapshot(
                pos,
                true,
                terminal.band()
                        .name(),
                terminal.band()
                        .uplinkHz(),
                terminal.band()
                        .downlinkHz(),
                terminal.band()
                        .bandwidthHz(),
                terminal.minimumElevationDeg(),
                assessment,
                terminal.lastPacket(),
                terminal.status()
        );
    }

    public static SatelliteGuiSnapshot apply(
            ServerPlayer player,
            BlockPos pos,
            SatelliteGuiAction action
    ) {
        TemporarySatelliteTerminalBlockEntity terminal =
                terminal(
                        player,
                        pos
                );

        if (terminal == null) {
            return SatelliteGuiSnapshot.invalid(
                    pos,
                    "Satellite terminal is missing, unloaded, or too far away"
            );
        }

        switch (action) {
            case REFRESH -> {
                terminal.setLastAssessment(
                        VsAwareSatelliteLinkService
                                .assessSelf(
                                        terminal
                                )
                );
            }

            case BAND_CYCLE -> {
                terminal.cycleBand();
                terminal.setLastAssessment(
                        VsAwareSatelliteLinkService
                                .assessSelf(
                                        terminal
                                )
                );
            }

            case ELEVATION_DOWN ->
                    terminal.adjustMinimumElevation(
                            -1.0
                    );

            case ELEVATION_UP ->
                    terminal.adjustMinimumElevation(
                            1.0
                    );

            case LINK_TEST -> {
                TemporarySatelliteTerminalBlockEntity target =
                        VsAwareSatelliteLinkService
                                .nearestOther(
                                        terminal
                                );

                SatelliteLinkAssessment assessment =
                        target == null
                                ? VsAwareSatelliteLinkService
                                .assessSelf(
                                        terminal
                                )
                                : VsAwareSatelliteLinkService
                                .assess(
                                        terminal,
                                        target
                                );

                terminal.setLastAssessment(
                        assessment
                );

                terminal.setStatus(
                        assessment.visible()
                                ? "VS-aware satellite link "
                                + assessment.satelliteName()
                                : "No VS-aware common satellite"
                );
            }

            case PACKET_TEST -> {
                TemporarySatelliteTerminalBlockEntity target =
                        VsAwareSatelliteLinkService
                                .nearestOther(
                                        terminal
                                );

                if (target == null) {
                    terminal.setStatus(
                            "Place a second satellite terminal for packet test"
                    );
                } else {
                    VsAwareSatelliteLinkService
                            .sendPacket(
                                    terminal,
                                    target,
                                    "Satellite terminal packet test"
                            );
                }
            }
        }

        return snapshot(
                player,
                pos
        );
    }

    private static TemporarySatelliteTerminalBlockEntity terminal(
            ServerPlayer player,
            BlockPos pos
    ) {
        if (player == null
                || pos == null
                || !player.serverLevel()
                .hasChunkAt(pos)) {
            return null;
        }

        if (!(player.serverLevel()
                .getBlockEntity(pos)
                instanceof TemporarySatelliteTerminalBlockEntity terminal)) {
            return null;
        }

        if (player.position()
                .distanceToSqr(
                        VsAwareSatelliteLinkService
                                .terminalWorldPosition(
                                        terminal
                                )
                ) > MAX_DISTANCE_SQR) {
            return null;
        }

        return terminal;
    }
}
