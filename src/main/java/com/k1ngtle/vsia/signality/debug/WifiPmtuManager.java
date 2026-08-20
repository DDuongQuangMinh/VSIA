package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.mtu.PathMtuCache;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.mtu.PathMtuDiscoverySession;
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
public final class WifiPmtuManager {
    public static final long PROBE_TIMEOUT_MICROS =
            8_000_000L;

    private static final Map<DeviceKey, Job> JOBS =
            new LinkedHashMap<>();

    private static final Map<DeviceKey, String> LAST_STATUS =
            new LinkedHashMap<>();

    private static final Map<DeviceKey, PathMtuCache> CACHES =
            new LinkedHashMap<>();

    private WifiPmtuManager() {
    }

    public static boolean start(
            ServerPlayer owner,
            NetworkDeviceBlockEntity device,
            String destinationIp,
            int initialBytes
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

        PathMtuDiscoverySession session;

        try {
            session =
                    new PathMtuDiscoverySession(
                            (
                                    System.nanoTime()
                                            ^ device.getBlockPos()
                                            .asLong()
                            )
                                    & Long.MAX_VALUE,
                            destinationIp,
                            initialBytes
                    );
        } catch (IllegalArgumentException exception) {
            return false;
        }

        JOBS.put(
                key,
                new Job(
                        owner.getUUID(),
                        session,
                        false,
                        -1L
                )
        );

        LAST_STATUS.put(
                key,
                "PMTUD started "
                        + destinationIp
                        + " bytes="
                        + initialBytes
        );

        return true;
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

        PathMtuDiscoverySession session =
                job.session();

        String type =
                packet.payload.getString(
                        "type"
                );

        if ("DESTINATION_UNREACHABLE".equalsIgnoreCase(
                type
        )
                && packet.payload.getInt(
                "icmp_code"
        ) == 4
                && session.destinationIp()
                .equals(
                        packet.payload.getString(
                                "quoted_target_ip"
                        )
                )) {
            int nextHopMtu =
                    packet.payload.getInt(
                            "next_hop_mtu"
                    );

            if (session.onFragmentationNeeded(
                    nextHopMtu
            )) {
                cacheFor(
                        key
                ).learn(
                        session.destinationIp(),
                        nextHopMtu,
                        nowMicros
                );

                LAST_STATUS.put(
                        key,
                        "PMTU learned "
                                + session.destinationIp()
                                + " = "
                                + nextHopMtu
                                + " bytes; retrying"
                );

                JOBS.put(
                        key,
                        new Job(
                                job.owner(),
                                session,
                                false,
                                -1L
                        )
                );

                send(
                        job.owner(),
                        "PMTU fragmentation needed: next-hop MTU="
                                + nextHopMtu
                                + " | retrying DF probe"
                );
            }

            return;
        }

        if (!"ECHO_REPLY".equalsIgnoreCase(
                type
        )) {
            return;
        }

        long responseSessionId =
                packet.payload.getLong(
                        "pmtu_session_id"
                );

        int responseBytes =
                packet.payload.getInt(
                        "pmtu_probe_bytes"
                );

        if (!session.onEchoReply(
                responseSessionId,
                responseBytes,
                packet.sourceIp
        )) {
            return;
        }

        cacheFor(
                key
        ).learn(
                session.destinationIp(),
                session.learnedMtu(),
                nowMicros
        );

        LAST_STATUS.put(
                key,
                "PMTU confirmed "
                        + session.destinationIp()
                        + " = "
                        + session.learnedMtu()
                        + " bytes"
        );

        JOBS.remove(
                key
        );

        send(
                job.owner(),
                "PMTU confirmed: "
                        + session.destinationIp()
                        + " = "
                        + session.learnedMtu()
                        + " bytes"
        );
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
                JOBS.remove(
                        key
                );
                continue;
            }

            BlockEntity blockEntity =
                    level.getBlockEntity(
                            key.pos()
                    );

            if (!(blockEntity
                    instanceof NetworkDeviceBlockEntity device)) {
                JOBS.remove(
                        key
                );
                continue;
            }

            long nowMicros =
                    device.wifiNetworkNowMicros();

            if (job.waiting()) {
                if (nowMicros
                        - job.sentMicros()
                        < PROBE_TIMEOUT_MICROS) {
                    continue;
                }

                LAST_STATUS.put(
                        key,
                        "PMTUD timeout at "
                                + job.session()
                                .currentProbeBytes()
                                + " bytes"
                );

                JOBS.remove(
                        key
                );

                send(
                        job.owner(),
                        "PMTUD timed out at "
                                + job.session()
                                .currentProbeBytes()
                                + " bytes"
                );

                continue;
            }

            boolean sent =
                    device.sendWifiPmtuProbe(
                            job.session()
                                    .destinationIp(),
                            job.session()
                                    .currentProbeBytes(),
                            job.session()
                                    .sessionId()
                    );

            if (!sent) {
                LAST_STATUS.put(
                        key,
                        "PMTUD probe could not be sent"
                );

                JOBS.remove(
                        key
                );

                continue;
            }

            JOBS.put(
                    key,
                    new Job(
                            job.owner(),
                            job.session(),
                            true,
                            nowMicros
                    )
            );
        }
    }

    public static String status(
            NetworkDeviceBlockEntity device
    ) {
        if (device == null
                || device.getLevel() == null) {
            return "PMTUD unavailable";
        }

        DeviceKey key =
                DeviceKey.of(
                        device
                );

        String status =
                LAST_STATUS.getOrDefault(
                        key,
                        "No PMTUD session"
                );

        int cached =
                cacheFor(
                        key
                ).mtuFor(
                        "192.168.2.20"
                );

        return status
                + " | default/cache="
                + cached;
    }

    public static int cachedMtu(
            NetworkDeviceBlockEntity device,
            String destinationIp
    ) {
        if (device == null
                || device.getLevel() == null) {
            return PathMtuCache.DEFAULT_IPV4_MTU;
        }

        return cacheFor(
                DeviceKey.of(
                        device
                )
        ).mtuFor(
                destinationIp
        );
    }

    private static PathMtuCache cacheFor(
            DeviceKey key
    ) {
        return CACHES.computeIfAbsent(
                key,
                ignored ->
                        new PathMtuCache()
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
            PathMtuDiscoverySession session,
            boolean waiting,
            long sentMicros
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
