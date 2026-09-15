package com.k1ngtle.vsia.signality.radar.network;

import com.k1ngtle.vsia.signality.api.radar.IRadarEmitter;
import com.k1ngtle.vsia.signality.api.radar.RadarRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class RadarNetworkCommand {
    private RadarNetworkCommand() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal(
                                "radarnet"
                        )
                        .then(
                                Commands.literal(
                                                "status"
                                        )
                                        .executes(
                                                ctx ->
                                                        status(
                                                                ctx,
                                                                RadarNetworkService.DEFAULT_NETWORK
                                                        )
                                        )
                                        .then(
                                                Commands.argument(
                                                                "network",
                                                                StringArgumentType.word()
                                                        )
                                                        .executes(
                                                                ctx ->
                                                                        status(
                                                                                ctx,
                                                                                StringArgumentType.getString(
                                                                                        ctx,
                                                                                        "network"
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "tracks"
                                        )
                                        .executes(
                                                ctx ->
                                                        tracks(
                                                                ctx,
                                                                RadarNetworkService.DEFAULT_NETWORK
                                                        )
                                        )
                                        .then(
                                                Commands.argument(
                                                                "network",
                                                                StringArgumentType.word()
                                                        )
                                                        .executes(
                                                                ctx ->
                                                                        tracks(
                                                                                ctx,
                                                                                StringArgumentType.getString(
                                                                                        ctx,
                                                                                        "network"
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "networks"
                                        )
                                        .executes(
                                                RadarNetworkCommand::networks
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "emitter"
                                        )
                                        .executes(
                                                RadarNetworkCommand::emitter
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "bind"
                                        )
                                        .requires(
                                                source ->
                                                        source.hasPermission(
                                                                2
                                                        )
                                        )
                                        .then(
                                                Commands.argument(
                                                                "network",
                                                                StringArgumentType.word()
                                                        )
                                                        .executes(
                                                                RadarNetworkCommand::bind
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "unbind"
                                        )
                                        .requires(
                                                source ->
                                                        source.hasPermission(
                                                                2
                                                        )
                                        )
                                        .executes(
                                                RadarNetworkCommand::unbind
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "clear"
                                        )
                                        .requires(
                                                source ->
                                                        source.hasPermission(
                                                                2
                                                        )
                                        )
                                        .executes(
                                                ctx ->
                                                        clear(
                                                                ctx,
                                                                RadarNetworkService.DEFAULT_NETWORK
                                                        )
                                        )
                                        .then(
                                                Commands.argument(
                                                                "network",
                                                                StringArgumentType.word()
                                                        )
                                                        .executes(
                                                                ctx ->
                                                                        clear(
                                                                                ctx,
                                                                                StringArgumentType.getString(
                                                                                        ctx,
                                                                                        "network"
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int status(
            CommandContext<CommandSourceStack> context,
            String networkId
    ) {
        CommandSourceStack source =
                context.getSource();

        ServerLevel level =
                source.getLevel();

        String normalized =
                RadarNetworkService.normalizeNetworkId(
                        networkId
                );

        RadarNetworkStats stats =
                RadarNetworkService.stats(
                        level,
                        normalized
                );

        source.sendSuccess(
                () ->
                        Component.literal(
                                        String.format(
                                                Locale.ROOT,
                                                "Radar network '%s': tracks=%d (tentative=%d confirmed=%d coasting=%d), queue=%d, accepted=%d, dropped=%d, delivered=%d",
                                                stats.networkId(),
                                                stats.trackCount(),
                                                stats.tentativeTracks(),
                                                stats.confirmedTracks(),
                                                stats.coastingTracks(),
                                                stats.queuedReports(),
                                                stats.reportsAccepted(),
                                                stats.reportsDropped(),
                                                stats.reportsDelivered()
                                        )
                                )
                                .withStyle(
                                        ChatFormatting.AQUA
                                ),
                false
        );

        return stats.trackCount();
    }

    private static int tracks(
            CommandContext<CommandSourceStack> context,
            String networkId
    ) {
        CommandSourceStack source =
                context.getSource();

        ServerLevel level =
                source.getLevel();

        String normalized =
                RadarNetworkService.normalizeNetworkId(
                        networkId
                );

        List<RadarNetworkTrack> tracks =
                RadarNetworkService.tracks(
                        level,
                        normalized
                );

        if (tracks.isEmpty()) {
            source.sendSuccess(
                    () ->
                            Component.literal(
                                    "No tracks in radar network '"
                                            + normalized
                                            + "'."
                            ),
                    false
            );

            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                        "Radar tracks in '"
                                                + normalized
                                                + "' ("
                                                + tracks.size()
                                                + "):"
                                )
                                .withStyle(
                                        ChatFormatting.AQUA
                                ),
                false
        );

        long now =
                level.getGameTime();

        for (RadarNetworkTrack track :
                tracks) {
            String iffLabel =
                    track.iff().authenticated()
                            ? track.iff().affiliation().name()
                            + "["
                            + track.iff().callsign()
                            + "/"
                            + String.format(
                                    Locale.ROOT,
                                    "%04d",
                                    track.iff().squawkCode()
                            )
                            + "]"
                            : track.iff().replyStatus().name();

            String line =
                    String.format(
                            Locale.ROOT,
                            "  %s  %-9s  pos=(%.1f, %.1f, %.1f)  v=%.1fm/s  sigma=%.1fm  q=%.0f%%  sensors=%d  hits=%d  SNRbest=%.1fdB  stale=%.2fs  IFF=%s",
                            shortId(
                                    track.trackId()
                            ),
                            track.state()
                                    .name(),
                            track.position().x,
                            track.position().y,
                            track.position().z,
                            track.speedMps(),
                            track.positionUncertaintyMeters(),
                            track.quality()
                                    * 100.0,
                            track.sensorCount(),
                            track.hits(),
                            10.0
                                    * Math.log10(
                                    Math.max(
                                            1.0E-12,
                                            track.bestSnrLinear()
                                    )
                            ),
                            track.staleSeconds(
                                    now
                            ),
                            iffLabel
                    );

            source.sendSuccess(
                    () ->
                            Component.literal(
                                    line
                            ),
                    false
            );
        }

        return tracks.size();
    }

    private static int networks(
            CommandContext<CommandSourceStack> context
    ) {
        CommandSourceStack source =
                context.getSource();

        List<String> ids =
                new ArrayList<>(
                        RadarNetworkService.networkIds(
                                source.getLevel()
                        )
                );

        ids.sort(
                String::compareTo
        );

        source.sendSuccess(
                () ->
                        Component.literal(
                                        "Radar networks: "
                                                + String.join(
                                                ", ",
                                                ids
                                        )
                                )
                                .withStyle(
                                        ChatFormatting.AQUA
                                ),
                false
        );

        return ids.size();
    }

    private static int emitter(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        CommandSourceStack source =
                context.getSource();

        IRadarEmitter emitter =
                nearestEmitter(
                        source
                );

        String network =
                RadarNetworkService.networkFor(
                        emitter.id()
                );

        source.sendSuccess(
                () ->
                        Component.literal(
                                        String.format(
                                                Locale.ROOT,
                                                "Nearest radar %s is bound to network '%s'.",
                                                shortId(
                                                        emitter.id()
                                                ),
                                                network
                                        )
                                )
                                .withStyle(
                                        ChatFormatting.AQUA
                                ),
                false
        );

        return 1;
    }

    private static int bind(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        CommandSourceStack source =
                context.getSource();

        IRadarEmitter emitter =
                nearestEmitter(
                        source
                );

        String requested =
                StringArgumentType.getString(
                        context,
                        "network"
                );

        String network =
                RadarNetworkService.bindEmitter(
                        source.getLevel(),
                        emitter.id(),
                        requested
                );

        source.sendSuccess(
                () ->
                        Component.literal(
                                        "Bound nearest radar "
                                                + shortId(
                                                emitter.id()
                                        )
                                                + " to network '"
                                                + network
                                                + "'."
                                )
                                .withStyle(
                                        ChatFormatting.GREEN
                                ),
                true
        );

        return 1;
    }

    private static int unbind(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        CommandSourceStack source =
                context.getSource();

        IRadarEmitter emitter =
                nearestEmitter(
                        source
                );

        RadarNetworkService.unbindEmitter(
                emitter.id()
        );

        source.sendSuccess(
                () ->
                        Component.literal(
                                        "Unbound nearest radar "
                                                + shortId(
                                                emitter.id()
                                        )
                                                + "; it now feeds network '"
                                                + RadarNetworkService.DEFAULT_NETWORK
                                                + "'."
                                )
                                .withStyle(
                                        ChatFormatting.GREEN
                                ),
                true
        );

        return 1;
    }

    private static int clear(
            CommandContext<CommandSourceStack> context,
            String networkId
    ) {
        CommandSourceStack source =
                context.getSource();

        String normalized =
                RadarNetworkService.normalizeNetworkId(
                        networkId
                );

        RadarNetworkService.clearNetwork(
                source.getLevel(),
                normalized
        );

        source.sendSuccess(
                () ->
                        Component.literal(
                                        "Cleared radar network '"
                                                + normalized
                                                + "'."
                                )
                                .withStyle(
                                        ChatFormatting.YELLOW
                                ),
                true
        );

        return 1;
    }

    private static IRadarEmitter nearestEmitter(
            CommandSourceStack source
    ) throws CommandSyntaxException {
        ServerLevel level =
                source.getLevel();

        Vec3 origin =
                source.getPosition();

        IRadarEmitter best =
                null;

        double bestDistance =
                Double.POSITIVE_INFINITY;

        for (IRadarEmitter emitter :
                RadarRegistry.emittersIn(
                        level
                )) {
            double distance =
                    emitter.originWorld()
                            .distanceToSqr(
                                    origin
                            );

            if (distance
                    < bestDistance) {
                bestDistance = distance;
                best = emitter;
            }
        }

        if (best == null) {
            throw new SimpleCommandExceptionType(
                    Component.literal(
                            "No active radars in this level."
                    )
            )
                    .create();
        }

        return best;
    }

    private static String shortId(
            UUID id
    ) {
        String value =
                id.toString();

        return value.substring(
                0,
                Math.min(
                        8,
                        value.length()
                )
        );
    }
}
