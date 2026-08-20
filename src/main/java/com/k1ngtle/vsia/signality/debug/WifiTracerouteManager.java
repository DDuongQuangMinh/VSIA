package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.traceroute.WifiTracerouteHop;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.traceroute.WifiTracerouteSession;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.traceroute.WifiTracerouteSnapshot;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiTracerouteManager {
    public static final long PROBE_TIMEOUT_MICROS =
            5_000_000L;

    private static final Map<DeviceKey, Job> JOBS =
            new LinkedHashMap<>();

    private static final Map<DeviceKey, WifiTracerouteSnapshot> LAST =
            new LinkedHashMap<>();

    private WifiTracerouteManager() {
    }

    public static boolean start(
            ServerPlayer owner,
            NetworkDeviceBlockEntity device,
            String destinationIp,
            int maxHops
    ) {
        if (owner == null
                || device == null
                || device.getLevel() == null) {
            return false;
        }

        DeviceKey key =
                DeviceKey.of(
                        device
                );

        long traceId =
                (
                        System.nanoTime()
                                ^ device.getBlockPos()
                                .asLong()
                )
                        & Long.MAX_VALUE;

        WifiTracerouteSession session;

        try {
            session =
                    new WifiTracerouteSession(
                            traceId,
                            destinationIp,
                            maxHops
                    );
        } catch (IllegalArgumentException exception) {
            return false;
        }

        JOBS.put(
                key,
                new Job(
                        owner.getUUID(),
                        session
                )
        );

        LAST.put(
                key,
                session.snapshot()
        );

        return true;
    }

    public static void clear(
            NetworkDeviceBlockEntity device
    ) {
        if (device == null
                || device.getLevel() == null) {
            return;
        }

        DeviceKey key =
                DeviceKey.of(
                        device
                );

        JOBS.remove(
                key
        );

        LAST.remove(
                key
        );
    }

    public static WifiTracerouteSnapshot snapshot(
            NetworkDeviceBlockEntity device
    ) {
        if (device == null
                || device.getLevel() == null) {
            return null;
        }

        DeviceKey key =
                DeviceKey.of(
                        device
                );

        Job active =
                JOBS.get(
                        key
                );

        return active != null
                ? active.session().snapshot()
                : LAST.get(
                key
        );
    }

    public static void onIcmpResponse(
            NetworkDeviceBlockEntity device,
            OSINetworkPacket packet,
            long nowMicros
    ) {
        if (device == null
                || packet == null
                || device.getLevel() == null
                || !"ICMP".equalsIgnoreCase(
                packet.applicationProtocol
        )
                || !packet.isResponse) {
            return;
        }

        DeviceKey key =
                DeviceKey.of(
                        device
                );

        Job job =
                JOBS.get(
                        key
                );

        if (job == null) {
            return;
        }

        WifiTracerouteSession session =
                job.session();

        String type =
                packet.payload.getString(
                        "type"
                );

        boolean accepted =
                false;

        if ("TIME_EXCEEDED".equalsIgnoreCase(
                type
        )
                || "DESTINATION_UNREACHABLE".equalsIgnoreCase(
                type
        )) {
            if (!session.matchesQuotedProbe(
                    packet.payload.getString(
                            "quoted_source_ip"
                    ),
                    packet.payload.getString(
                            "quoted_target_ip"
                    ),
                    packet.payload.getInt(
                            "quoted_protocol"
                    )
            )) {
                return;
            }

            if ("TIME_EXCEEDED".equalsIgnoreCase(
                    type
            )) {
                accepted =
                        session.onTimeExceeded(
                                packet.sourceIp,
                                nowMicros
                        );
            } else {
                accepted =
                        session.onDestinationUnreachable(
                                packet.sourceIp,
                                packet.payload.getInt(
                                        "icmp_code"
                                ),
                                nowMicros
                        );
            }
        } else if ("ECHO_REPLY".equalsIgnoreCase(
                type
        )) {
            long responseTraceId =
                    packet.payload.getLong(
                            "traceroute_id"
                    );

            int responseTtl =
                    packet.payload.getInt(
                            "traceroute_ttl"
                    );

            int responseAttempt =
                    packet.payload.getInt(
                            "traceroute_attempt"
                    );

            if (!session.matchesEchoReply(
                    responseTraceId,
                    responseTtl,
                    responseAttempt,
                    packet.sourceIp
            )) {
                return;
            }

            accepted =
                    session.onEchoReply(
                            packet.sourceIp,
                            nowMicros
                    );
        }

        if (!accepted) {
            return;
        }

        WifiTracerouteSnapshot snapshot =
                session.snapshot();

        LAST.put(
                key,
                snapshot
        );

        sendNewestHop(
                job.owner(),
                snapshot
        );

        if (!snapshot.running()) {
            JOBS.remove(
                    key
            );

            send(
                    job.owner(),
                    "Traceroute finished: "
                            + snapshot.finalStatus()
                            + " | hops="
                            + snapshot.hops().size()
            );
        }
    }

    @SubscribeEvent
    public static void onServerTick(
            TickEvent.ServerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END
                || JOBS.isEmpty()) {
            return;
        }

        MinecraftServer minecraftServer =
                ServerLifecycleHooks.getCurrentServer();

        if (minecraftServer == null) {
            return;
        }

        for (Map.Entry<DeviceKey, Job> entry
                : java.util.List.copyOf(
                JOBS.entrySet()
        )) {
            DeviceKey key =
                    entry.getKey();

            Job job =
                    entry.getValue();

            ServerLevel level =
                    minecraftServer.getLevel(
                            key.dimension()
                    );

            if (level == null) {
                finishUnavailable(
                        key,
                        job
                );
                continue;
            }

            BlockEntity blockEntity =
                    level.getBlockEntity(
                            key.pos()
                    );

            if (!(blockEntity
                    instanceof NetworkDeviceBlockEntity device)) {
                finishUnavailable(
                        key,
                        job
                );
                continue;
            }

            WifiTracerouteSession session =
                    job.session();

            long nowMicros =
                    device.wifiNetworkNowMicros();

            if (session.waiting()) {
                if (nowMicros
                        - session.sentMicros()
                        < PROBE_TIMEOUT_MICROS) {
                    continue;
                }

                WifiTracerouteSession.TimeoutAction action =
                        session.onTimeout();

                LAST.put(
                        key,
                        session.snapshot()
                );

                if (action
                        == WifiTracerouteSession.TimeoutAction.NEXT_HOP
                        || action
                        == WifiTracerouteSession.TimeoutAction.FINISHED) {
                    sendNewestHop(
                            job.owner(),
                            session.snapshot()
                    );
                }

                if (!session.running()) {
                    JOBS.remove(
                            key
                    );

                    send(
                            job.owner(),
                            "Traceroute finished: "
                                    + session.snapshot()
                                    .finalStatus()
                    );

                    continue;
                }
            }

            if (!session.needsProbe()) {
                continue;
            }

            WifiTracerouteSession.ProbeRequest probe =
                    session.beginProbe(
                            nowMicros
                    );

            boolean sent =
                    device.sendWifiTracerouteProbe(
                            probe.destinationIp(),
                            probe.ttl(),
                            probe.traceId(),
                            probe.attempt()
                    );

            if (!sent) {
                JOBS.remove(
                        key
                );

                LAST.put(
                        key,
                        session.snapshot()
                );

                send(
                        job.owner(),
                        "Traceroute aborted: probe could not be sent"
                );
            }
        }
    }

    public static String format(
            WifiTracerouteSnapshot snapshot
    ) {
        if (snapshot == null) {
            return "No traceroute result.";
        }

        StringBuilder builder =
                new StringBuilder();

        builder.append(
                        "trace "
                )
                .append(
                        snapshot.destinationIp()
                )
                .append(
                        " | "
                )
                .append(
                        snapshot.finalStatus()
                );

        for (WifiTracerouteHop hop
                : snapshot.hops()) {
            builder.append(
                            "\n"
                    )
                    .append(
                            hop.ttl()
                    )
                    .append(
                            "  "
                    )
                    .append(
                            hop.responderIp()
                    )
                    .append(
                            "  "
                    );

            if (Double.isNaN(
                    hop.rttMs()
            )) {
                builder.append(
                        "*"
                );
            } else {
                builder.append(
                        String.format(
                                java.util.Locale.ROOT,
                                "%.3f ms",
                                hop.rttMs()
                        )
                );
            }

            builder.append(
                            "  "
                    )
                    .append(
                            hop.result()
                    );

            if (hop.attempts() > 1) {
                builder.append(
                                " attempt="
                        )
                        .append(
                                hop.attempts()
                        );
            }
        }

        return builder.toString();
    }

    private static void sendNewestHop(
            UUID owner,
            WifiTracerouteSnapshot snapshot
    ) {
        if (snapshot.hops()
                .isEmpty()) {
            return;
        }

        WifiTracerouteHop hop =
                snapshot.hops()
                        .get(
                                snapshot.hops()
                                        .size()
                                        - 1
                        );

        String rtt =
                Double.isNaN(
                        hop.rttMs()
                )
                        ? "*"
                        : String.format(
                        java.util.Locale.ROOT,
                        "%.3f ms",
                        hop.rttMs()
                );

        send(
                owner,
                hop.ttl()
                        + "  "
                        + hop.responderIp()
                        + "  "
                        + rtt
                        + "  "
                        + hop.result()
        );
    }

    private static void finishUnavailable(
            DeviceKey key,
            Job job
    ) {
        JOBS.remove(
                key
        );

        LAST.put(
                key,
                job.session()
                        .snapshot()
        );

        send(
                job.owner(),
                "Traceroute aborted: target device unavailable"
        );
    }

    private static void send(
            UUID owner,
            String text
    ) {
        MinecraftServer minecraftServer =
                ServerLifecycleHooks.getCurrentServer();

        if (minecraftServer == null) {
            return;
        }

        ServerPlayer player =
                minecraftServer.getPlayerList()
                        .getPlayer(
                                owner
                        );

        if (player != null) {
            player.sendSystemMessage(
                    Component.literal(
                            text
                    )
            );
        }
    }

    private record Job(
            UUID owner,
            WifiTracerouteSession session
    ) {
    }

    private record DeviceKey(
            ResourceKey<Level> dimension,
            BlockPos pos
    ) {
        private static DeviceKey of(
                NetworkDeviceBlockEntity device
        ) {
            return new DeviceKey(
                    device.getLevel()
                            .dimension(),
                    device.getBlockPos()
                            .immutable()
            );
        }
    }
}
