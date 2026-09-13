package com.k1ngtle.vsia.signality.internet.radio.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.field.FieldDeviceNetwork;
import com.k1ngtle.vsia.signality.internet.network.NetworkProfile;
import com.k1ngtle.vsia.signality.internet.radio.portable.PortableRadioEndpoint;
import com.k1ngtle.vsia.signality.internet.radio.portable.PortableRadioState;
import com.k1ngtle.vsia.signality.internet.radio.voice.network.S2CRadioVoiceFramePacket;
import com.k1ngtle.vsia.signality.internet.routing.LongHaulRoutePolicy;
import com.k1ngtle.vsia.signality.internet.satellite.internet.SatelliteBackhaulService;
import com.k1ngtle.vsia.signality.internet.satellite.internet.SatelliteInternetPath;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class RadioDevSelfTestService {
    private static final double LIGHT_SPEED_METERS_PER_SECOND =
            299_792_458.0;

    private static final double RECEIVER_NOISE_FIGURE_DB =
            6.0;

    private static final double DEFAULT_REMOTE_DISTANCE_BLOCKS =
            5_100.0;

    private static final Map<UUID, DevConfig> CONFIGS =
            new HashMap<>();

    private static final Map<UUID, DevSession> SESSIONS =
            new HashMap<>();

    private RadioDevSelfTestService() {
    }

    public static void enableDefault(
            ServerPlayer player
    ) {
        if (player == null) {
            return;
        }

        DevConfig existing =
                CONFIGS.get(
                        player.getUUID()
                );

        Vec3 remote =
                existing != null
                        && existing.remotePosition() != null
                        ? existing.remotePosition()
                        : player.position()
                        .add(
                                DEFAULT_REMOTE_DISTANCE_BLOCKS,
                                0.0,
                                0.0
                        );

        CONFIGS.put(
                player.getUUID(),
                new DevConfig(
                        true,
                        remote
                )
        );

        sendEnabledMessage(
                player,
                remote
        );
    }

    public static void enableAtDistance(
            ServerPlayer player,
            double distanceBlocks
    ) {
        if (player == null) {
            return;
        }

        double distance =
                Math.max(
                        1.0,
                        distanceBlocks
                );

        Vec3 remote =
                player.position()
                        .add(
                                distance,
                                0.0,
                                0.0
                        );

        CONFIGS.put(
                player.getUUID(),
                new DevConfig(
                        true,
                        remote
                )
        );

        sendEnabledMessage(
                player,
                remote
        );
    }

    public static void setRemote(
            ServerPlayer player,
            Vec3 remotePosition
    ) {
        if (player == null
                || remotePosition == null) {
            return;
        }

        DevConfig existing =
                CONFIGS.get(
                        player.getUUID()
                );

        boolean enabled =
                existing != null
                        && existing.enabled();

        CONFIGS.put(
                player.getUUID(),
                new DevConfig(
                        enabled,
                        remotePosition
                )
        );

        player.sendSystemMessage(
                Component.literal(
                                String.format(
                                        Locale.ROOT,
                                        "[VS:IA DEV] Virtual radio peer set to %.1f %.1f %.1f",
                                        remotePosition.x,
                                        remotePosition.y,
                                        remotePosition.z
                                )
                        )
                        .withStyle(
                                ChatFormatting.AQUA
                        )
        );
    }

    public static void disable(
            ServerPlayer player
    ) {
        if (player == null) {
            return;
        }

        DevConfig existing =
                CONFIGS.get(
                        player.getUUID()
                );

        if (existing != null) {
            CONFIGS.put(
                    player.getUUID(),
                    new DevConfig(
                            false,
                            existing.remotePosition()
                    )
            );
        }

        SESSIONS.remove(
                player.getUUID()
        );

        player.sendSystemMessage(
                Component.literal(
                                "[VS:IA DEV] Radio self-echo disabled"
                        )
                        .withStyle(
                                ChatFormatting.YELLOW
                        )
        );
    }

    public static boolean enabled(
            ServerPlayer player
    ) {
        if (player == null) {
            return false;
        }

        DevConfig config =
                CONFIGS.get(
                        player.getUUID()
                );

        return config != null
                && config.enabled()
                && config.remotePosition() != null;
    }

    public static void begin(
            PortableRadioEndpoint endpoint,
            int sessionId
    ) {
        if (endpoint == null
                || !endpoint.valid()) {
            return;
        }

        ServerPlayer player =
                endpoint.player();

        DevConfig config =
                CONFIGS.get(
                        player.getUUID()
                );

        if (config == null
                || !config.enabled()
                || config.remotePosition() == null) {
            return;
        }

        RoutePlan plan =
                resolvePlan(
                        endpoint,
                        config.remotePosition()
                );

        DevSession session =
                new DevSession(
                        sessionId,
                        virtualPeerId(
                                player
                        ),
                        config.remotePosition(),
                        plan,
                        new ArrayList<>()
                );

        SESSIONS.put(
                player.getUUID(),
                session
        );

        if (!plan.available()) {
            player.sendSystemMessage(
                    Component.literal(
                                    "[VS:IA DEV] Self-test route unavailable: "
                                            + plan.summary()
                            )
                            .withStyle(
                                    ChatFormatting.RED
                            )
            );

            PortableRadioState.status(
                    endpoint.stack(),
                    "DEV self-test route unavailable"
            );

            return;
        }

        player.sendSystemMessage(
                Component.literal(
                                "[VS:IA DEV] Self-test armed: "
                                        + plan.summary()
                        )
                        .withStyle(
                                ChatFormatting.GREEN
                        )
        );

        PortableRadioState.status(
                endpoint.stack(),
                "DEV self-test armed: "
                        + plan.mode()
        );
    }

    public static void capture(
            PortableRadioEndpoint endpoint,
            int sessionId,
            CompoundTag radioMessage,
            double frequencyHz
    ) {
        if (endpoint == null
                || radioMessage == null
                || !endpoint.valid()) {
            return;
        }

        ServerPlayer player =
                endpoint.player();

        DevSession session =
                SESSIONS.get(
                        player.getUUID()
                );

        if (session == null
                || session.sessionId()
                != sessionId
                || !session.plan()
                .available()) {
            return;
        }

        if (!"VOICE".equalsIgnoreCase(
                radioMessage.getString(
                        "radio_message_type"
                )
        )
                || !"G711_MULAW_8K".equalsIgnoreCase(
                radioMessage.getString(
                        "voice_stream_codec"
                )
        )) {
            return;
        }

        int sequence =
                radioMessage.getInt(
                        "voice_stream_seq"
                );

        boolean end =
                radioMessage.getBoolean(
                        "end_of_transmission"
                );

        byte[] audio =
                radioMessage.getByteArray(
                        "voice_data"
                );

        if (!end
                && audio.length == 0) {
            return;
        }

        double probability =
                session.plan()
                        .packetSuccessProbability();

        if (!end
                && !survives(
                player,
                session,
                sequence,
                probability
        )) {
            return;
        }

        session.frames()
                .add(
                        new EchoFrame(
                                sequence,
                                audio.clone(),
                                end,
                                session.plan()
                                        .snrDb(),
                                session.plan()
                                        .intelligibility(),
                                PortableRadioState
                                        .emission(
                                                endpoint.stack()
                                        )
                                        .name(),
                                frequencyHz
                        )
                );
    }

    public static void finish(
            PortableRadioEndpoint endpoint,
            int sessionId
    ) {
        if (endpoint == null) {
            return;
        }

        ServerPlayer player =
                endpoint.player();

        DevSession session =
                SESSIONS.remove(
                        player.getUUID()
                );

        if (session == null
                || session.sessionId()
                != sessionId) {
            return;
        }

        if (!session.plan()
                .available()) {
            PortableRadioState.status(
                    endpoint.stack(),
                    "DEV echo blocked: "
                            + session.plan()
                            .summary()
            );

            return;
        }

        List<EchoFrame> frames =
                session.frames();

        if (frames.isEmpty()) {
            player.sendSystemMessage(
                    Component.literal(
                                    "[VS:IA DEV] No voice frames survived the virtual link."
                            )
                            .withStyle(
                                    ChatFormatting.RED
                            )
            );

            PortableRadioState.status(
                    endpoint.stack(),
                    "DEV echo: no surviving voice frames"
            );

            return;
        }

        boolean hasEnd =
                frames.stream()
                        .anyMatch(
                                EchoFrame::endOfTransmission
                        );

        for (EchoFrame frame
                : frames) {
            FieldDeviceNetwork.sendToPlayer(
                    player,
                    new S2CRadioVoiceFramePacket(
                            session.virtualPeerId(),
                            frame.sequenceNumber(),
                            frame.encodedAudio(),
                            frame.endOfTransmission(),
                            frame.snrDb(),
                            frame.intelligibility(),
                            frame.emission()
                    )
            );
        }

        if (!hasEnd) {
            int endSequence =
                    frames.get(
                            frames.size() - 1
                    )
                            .sequenceNumber()
                            + 1;

            FieldDeviceNetwork.sendToPlayer(
                    player,
                    new S2CRadioVoiceFramePacket(
                            session.virtualPeerId(),
                            endSequence,
                            new byte[0],
                            true,
                            session.plan()
                                    .snrDb(),
                            session.plan()
                                    .intelligibility(),
                            PortableRadioState
                                    .emission(
                                            endpoint.stack()
                                    )
                                    .name()
                    )
            );
        }

        player.sendSystemMessage(
                Component.literal(
                                String.format(
                                        Locale.ROOT,
                                        "[VS:IA DEV] Echo replay: %s | %d frames | SNR %.1f dB | P %.3f",
                                        session.plan()
                                                .mode(),
                                        frames.size(),
                                        session.plan()
                                                .snrDb(),
                                        session.plan()
                                                .packetSuccessProbability()
                                )
                        )
                        .withStyle(
                                ChatFormatting.AQUA
                        )
        );

        PortableRadioState.status(
                endpoint.stack(),
                "DEV echo replayed via "
                        + session.plan()
                        .mode()
        );
    }

    public static String status(
            ServerPlayer player
    ) {
        if (player == null) {
            return "No player";
        }

        DevConfig config =
                CONFIGS.get(
                        player.getUUID()
                );

        if (config == null
                || config.remotePosition() == null) {
            return "DEV radio echo is not configured";
        }

        double distance =
                player.position()
                        .distanceTo(
                                config.remotePosition()
                        );

        String state =
                config.enabled()
                        ? "ON"
                        : "OFF";

        return String.format(
                Locale.ROOT,
                "DEV echo %s | remote %.1f %.1f %.1f | distance %.3f km (%d blocks)",
                state,
                config.remotePosition().x,
                config.remotePosition().y,
                config.remotePosition().z,
                distance
                        / LongHaulRoutePolicy.BLOCKS_PER_KILOMETER,
                Math.round(
                        distance
                )
        );
    }

    public static String routeStatus(
            PortableRadioEndpoint endpoint
    ) {
        if (endpoint == null
                || !endpoint.valid()) {
            return "Hold a Temporary Field Radio first";
        }

        DevConfig config =
                CONFIGS.get(
                        endpoint.player()
                                .getUUID()
                );

        if (config == null
                || config.remotePosition() == null) {
            return "DEV radio echo is not configured";
        }

        RoutePlan plan =
                resolvePlan(
                        endpoint,
                        config.remotePosition()
                );

        return plan.summary();
    }

    public static void cancel(
            PortableRadioEndpoint endpoint
    ) {
        if (endpoint == null) {
            return;
        }

        SESSIONS.remove(
                endpoint.player()
                        .getUUID()
        );
    }

    private static RoutePlan resolvePlan(
            PortableRadioEndpoint endpoint,
            Vec3 remotePosition
    ) {
        Vec3 source =
                endpoint.positionWorld();

        double distance =
                source.distanceTo(
                        remotePosition
                );

        if (distance
                >= LongHaulRoutePolicy
                .SATELLITE_REQUIRED_DISTANCE_BLOCKS) {

            SatelliteBackhaulService.PathResult result =
                    SatelliteBackhaulService.resolve(
                            endpoint.level(),
                            source,
                            remotePosition
                    );

            if (!result.success()
                    || !result.satelliteRequired()
                    || result.path() == null) {
                return RoutePlan.unavailable(
                        "SATELLITE",
                        result.error()
                );
            }

            SatelliteInternetPath path =
                    result.path();

            double snr =
                    path.link()
                            .bottleneckSnrDb();

            double probability =
                    clamp01(
                            path.link()
                                    .packetSuccessProbability()
                    );

            double intelligibility =
                    intelligibilityFromSnr(
                            snr
                    );

            return new RoutePlan(
                    true,
                    "SATELLITE",
                    snr,
                    probability,
                    intelligibility,
                    SatelliteBackhaulService
                            .routeSummary(
                                    path
                            )
            );
        }

        ItemStackView view =
                ItemStackView.of(
                        endpoint
                );

        if (distance
                > view.maximumRangeBlocks()) {
            return RoutePlan.unavailable(
                    "DIRECT RF",
                    String.format(
                            Locale.ROOT,
                            "Virtual peer is %.0f blocks away, beyond the %.0f-block radio profile range",
                            distance,
                            view.maximumRangeBlocks()
                    )
            );
        }

        double receivedPowerDbm =
                directReceivedPowerDbm(
                        view,
                        distance
                );

        double noiseFloorDbm =
                -174.0
                        + 10.0
                        * Math.log10(
                        Math.max(
                                1.0,
                                view.bandwidthHz()
                        )
                )
                        + RECEIVER_NOISE_FIGURE_DB;

        double snr =
                receivedPowerDbm
                        - noiseFloorDbm;

        if (snr
                < PortableRadioState
                .squelchDb(
                        endpoint.stack()
                )) {
            return RoutePlan.unavailable(
                    "DIRECT RF",
                    String.format(
                            Locale.ROOT,
                            "Virtual direct link is below squelch: SNR %.1f dB",
                            snr
                    )
            );
        }

        double probability =
                packetProbabilityFromSnr(
                        snr
                );

        double intelligibility =
                intelligibilityFromSnr(
                        snr
                );

        return new RoutePlan(
                true,
                "DIRECT RF",
                snr,
                probability,
                intelligibility,
                String.format(
                        Locale.ROOT,
                        "DEV direct virtual peer | %.3f km | RX %.1f dBm | SNR %.1f dB | P %.3f",
                        distance
                                / LongHaulRoutePolicy.BLOCKS_PER_KILOMETER,
                        receivedPowerDbm,
                        snr,
                        probability
                )
        );
    }

    private static double directReceivedPowerDbm(
            ItemStackView view,
            double distanceBlocks
    ) {
        double distanceMeters =
                Math.max(
                        1.0,
                        distanceBlocks
                );

        double frequencyHz =
                Math.max(
                        1.0,
                        view.frequencyHz()
                );

        double wavelength =
                LIGHT_SPEED_METERS_PER_SECOND
                        / frequencyHz;

        double fsplDb =
                20.0
                        * Math.log10(
                        4.0
                                * Math.PI
                                * distanceMeters
                                / wavelength
                );

        double txPowerDbm =
                10.0
                        * Math.log10(
                        Math.max(
                                1.0E-15,
                                view.transmitPowerWatts()
                        )
                                * 1000.0
                );

        return txPowerDbm
                + view.antennaGainDbi()
                + view.antennaGainDbi()
                - fsplDb;
    }

    private static double packetProbabilityFromSnr(
            double snrDb
    ) {
        return clamp01(
                1.0
                        / (
                        1.0
                                + Math.exp(
                                -(snrDb - 2.0)
                                        / 1.75
                        )
                )
        );
    }

    private static double intelligibilityFromSnr(
            double snrDb
    ) {
        return clamp01(
                1.0
                        / (
                        1.0
                                + Math.exp(
                                -(snrDb - 3.0)
                                        / 2.5
                        )
                )
        );
    }

    private static boolean survives(
            ServerPlayer player,
            DevSession session,
            int sequence,
            double probability
    ) {
        if (probability >= 0.999999) {
            return true;
        }

        long seed =
                player.getUUID()
                        .getMostSignificantBits()
                        ^ Long.rotateLeft(
                        session.virtualPeerId()
                                .getLeastSignificantBits(),
                        13
                )
                        ^ Long.rotateLeft(
                        sequence,
                        7
                )
                        ^ player.serverLevel()
                        .getGameTime();

        return new Random(
                seed
        )
                .nextDouble()
                <= probability;
    }

    private static UUID virtualPeerId(
            ServerPlayer player
    ) {
        return UUID.nameUUIDFromBytes(
                (
                        "vsia-dev-radio-peer:"
                                + player.getUUID()
                )
                        .getBytes(
                                StandardCharsets.UTF_8
                        )
        );
    }

    private static double clamp01(
            double value
    ) {
        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }

    private static void sendEnabledMessage(
            ServerPlayer player,
            Vec3 remote
    ) {
        double distance =
                player.position()
                        .distanceTo(
                                remote
                        );

        player.sendSystemMessage(
                Component.literal(
                                String.format(
                                        Locale.ROOT,
                                        "[VS:IA DEV] Radio self-echo enabled | virtual peer %.3f km away | %.1f %.1f %.1f",
                                        distance
                                                / LongHaulRoutePolicy
                                                .BLOCKS_PER_KILOMETER,
                                        remote.x,
                                        remote.y,
                                        remote.z
                                )
                        )
                        .withStyle(
                                ChatFormatting.GREEN
                        )
        );
    }

    @SubscribeEvent
    public static void onLogout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        CONFIGS.remove(
                event.getEntity()
                        .getUUID()
        );

        SESSIONS.remove(
                event.getEntity()
                        .getUUID()
        );
    }

    @SubscribeEvent
    public static void onServerStopped(
            ServerStoppedEvent event
    ) {
        CONFIGS.clear();
        SESSIONS.clear();
    }

    private record DevConfig(
            boolean enabled,
            Vec3 remotePosition
    ) {
    }

    private record DevSession(
            int sessionId,
            UUID virtualPeerId,
            Vec3 remotePosition,
            RoutePlan plan,
            List<EchoFrame> frames
    ) {
    }

    private record EchoFrame(
            int sequenceNumber,
            byte[] encodedAudio,
            boolean endOfTransmission,
            double snrDb,
            double intelligibility,
            String emission,
            double frequencyHz
    ) {
        private EchoFrame {
            encodedAudio =
                    encodedAudio == null
                            ? new byte[0]
                            : encodedAudio.clone();

            emission =
                    emission == null
                            ? ""
                            : emission;
        }
    }

    private record RoutePlan(
            boolean available,
            String mode,
            double snrDb,
            double packetSuccessProbability,
            double intelligibility,
            String summary
    ) {
        private static RoutePlan unavailable(
                String mode,
                String summary
        ) {
            return new RoutePlan(
                    false,
                    mode,
                    Double.NEGATIVE_INFINITY,
                    0.0,
                    0.0,
                    summary == null
                            ? "Route unavailable"
                            : summary
            );
        }
    }

    private record ItemStackView(
            double frequencyHz,
            double bandwidthHz,
            double transmitPowerWatts,
            double antennaGainDbi,
            double maximumRangeBlocks
    ) {
        private static ItemStackView of(
                PortableRadioEndpoint endpoint
        ) {
            NetworkProfile profile =
                    PortableRadioState.profile(
                            endpoint.stack()
                    );

            return new ItemStackView(
                    PortableRadioState.frequencyHz(
                            endpoint.stack()
                    ),
                    PortableRadioState.bandwidthHz(
                            endpoint.stack()
                    ),
                    profile.transmitPowerWatts(),
                    profile.antennaGain(),
                    profile.maximumRangeBlocks()
            );
        }
    }
}
