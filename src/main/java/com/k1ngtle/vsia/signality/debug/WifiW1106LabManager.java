package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
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
public final class WifiW1106LabManager {
    public static final String SSID =
            "VSIA-W1106-LAB";

    private static final int MAX_STAGE_RETRIES =
            5;

    private static final long SHORT_WAIT =
            10L;

    private static final long DISCOVERY_WAIT =
            40L;

    private static final long ASSOCIATION_WAIT =
            80L;

    private static final Map<UUID, LabJob> JOBS =
            new LinkedHashMap<>();

    private static final Map<UUID, String> LAST_STATUS =
            new LinkedHashMap<>();

    private WifiW1106LabManager() {
    }

    public static void start(
            UUID owner,
            ResourceKey<Level> dimension,
            BlockPos clientPos,
            BlockPos routerPos,
            BlockPos serverPos,
            long startTick
    ) {
        LabJob job =
                new LabJob(
                        owner,
                        dimension,
                        clientPos.immutable(),
                        routerPos.immutable(),
                        serverPos.immutable(),
                        Phase.CONFIGURE,
                        0,
                        startTick,
                        "STARTING"
                );

        JOBS.put(
                owner,
                job
        );

        LAST_STATUS.put(
                owner,
                describe(
                        job
                )
        );
    }

    public static String status(
            UUID owner
    ) {
        LabJob active =
                JOBS.get(
                        owner
                );

        return active != null
                ? describe(
                active
        )
                : LAST_STATUS.getOrDefault(
                owner,
                "No W1.10.6 lab setup has been run in this session."
        );
    }

    @SubscribeEvent
    public static void onServerTick(
            TickEvent.ServerTickEvent event
    ) {
        if (event.phase
                != TickEvent.Phase.END
                || JOBS.isEmpty()) {
            return;
        }

        MinecraftServer minecraftServer =
                ServerLifecycleHooks.getCurrentServer();

        if (minecraftServer == null) {
            return;
        }

        for (UUID owner :
                java.util.List.copyOf(
                        JOBS.keySet()
                )) {
            LabJob job =
                    JOBS.get(
                            owner
                    );

            if (job == null) {
                continue;
            }

            ServerLevel level =
                    minecraftServer.getLevel(
                            job.dimension()
                    );

            if (level == null) {
                fail(
                        minecraftServer,
                        job,
                        "dimension unavailable"
                );
                continue;
            }

            NetworkDeviceBlockEntity client =
                    deviceAt(
                            level,
                            job.clientPos()
                    );

            NetworkDeviceBlockEntity router =
                    deviceAt(
                            level,
                            job.routerPos()
                    );

            NetworkDeviceBlockEntity server =
                    deviceAt(
                            level,
                            job.serverPos()
                    );

            if (client == null
                    || router == null
                    || server == null) {
                fail(
                        minecraftServer,
                        job,
                        "client/router/server not loaded"
                );
                continue;
            }

            if (level.getGameTime()
                    < job.nextActionTick()) {
                continue;
            }

            switch (job.phase()) {
                case CONFIGURE ->
                        configure(
                                minecraftServer,
                                level,
                                job,
                                client,
                                router,
                                server
                        );

                case CLIENT_SCAN ->
                        clientScan(
                                level,
                                job,
                                client,
                                router
                        );

                case CLIENT_DISCOVERY_CHECK ->
                        clientDiscoveryCheck(
                                level,
                                job,
                                client,
                                router
                        );

                case CLIENT_ASSOCIATION_CHECK ->
                        clientAssociationCheck(
                                level,
                                job,
                                client,
                                router
                        );

                case SERVER_SCAN ->
                        serverScan(
                                level,
                                job,
                                server,
                                router
                        );

                case SERVER_DISCOVERY_CHECK ->
                        serverDiscoveryCheck(
                                level,
                                job,
                                server,
                                router
                        );

                case SERVER_ASSOCIATION_CHECK ->
                        serverAssociationCheck(
                                minecraftServer,
                                level,
                                job,
                                client,
                                server,
                                router
                        );
            }
        }
    }

    private static void configure(
            MinecraftServer minecraftServer,
            ServerLevel level,
            LabJob job,
            NetworkDeviceBlockEntity client,
            NetworkDeviceBlockEntity router,
            NetworkDeviceBlockEntity server
    ) {
        router.configureWifiAccessPoint(
                SSID
        );

        router.configureWifiStaticIpv4(
                "192.168.1.1",
                "255.255.255.0",
                ""
        );

        router.configureWifiLiveRouterInterface(
                "lan0",
                "192.168.1.1",
                24
        );

        router.configureWifiLiveRouterInterface(
                "lan1",
                "192.168.2.1",
                24
        );

        router.setWifiLiveRouterEnabled(
                true
        );

        client.configureWifiStation();
        server.configureWifiStation();

        client.configureWifiStaticIpv4(
                "192.168.1.100",
                "255.255.255.0",
                "192.168.1.1"
        );

        server.configureWifiStaticIpv4(
                "192.168.2.20",
                "255.255.255.0",
                "192.168.2.1"
        );

        double frequency =
                router.activeFrequencyHz();

        if (!client.configureWifiActiveFrequency(
                frequency
        )
                || !server.configureWifiActiveFrequency(
                frequency
        )) {
            fail(
                    minecraftServer,
                    job,
                    "channel/profile mismatch at "
                            + formatFrequency(
                            frequency
                    )
            );
            return;
        }

        router.sendWifiBeacon();

        update(
                job.next(
                        Phase.CLIENT_SCAN,
                        0,
                        level.getGameTime()
                                + SHORT_WAIT,
                        "CONFIGURED"
                                + " | channel="
                                + formatFrequency(
                                frequency
                        )
                                + " | next=CLIENT_SCAN"
                )
        );

        send(
                minecraftServer,
                job.owner(),
                "W1.10.6 lab configured"
                        + " | "
                        + formatFrequency(
                        frequency
                )
                        + " | starting staggered client discovery"
        );
    }

    private static void clientScan(
            ServerLevel level,
            LabJob job,
            NetworkDeviceBlockEntity client,
            NetworkDeviceBlockEntity router
    ) {
        double frequency =
                router.activeFrequencyHz();

        client.configureWifiActiveFrequency(
                frequency
        );

        router.sendWifiBeacon();

        client.scanWifi();

        client.configureWifiActiveFrequency(
                frequency
        );

        router.sendWifiBeacon();

        update(
                job.next(
                        Phase.CLIENT_DISCOVERY_CHECK,
                        job.retries(),
                        level.getGameTime()
                                + DISCOVERY_WAIT,
                        "CLIENT_SCANNING"
                                + " | channel="
                                + formatFrequency(
                                frequency
                        )
                )
        );
    }

    private static void clientDiscoveryCheck(
            ServerLevel level,
            LabJob job,
            NetworkDeviceBlockEntity client,
            NetworkDeviceBlockEntity router
    ) {
        boolean saw =
                sawLabAp(
                        client
                );

        if (!saw) {
            retryStage(
                    level,
                    job,
                    router,
                    Phase.CLIENT_SCAN,
                    "client did not discover AP"
            );
            return;
        }

        boolean started =
                client.connectWifi(
                        SSID
                );

        if (!started) {
            retryStage(
                    level,
                    job,
                    router,
                    Phase.CLIENT_SCAN,
                    "client discovered AP but connectWifi returned false"
            );
            return;
        }

        update(
                job.next(
                        Phase.CLIENT_ASSOCIATION_CHECK,
                        0,
                        level.getGameTime()
                                + ASSOCIATION_WAIT,
                        "CLIENT_CONNECTING"
                )
        );
    }

    private static void clientAssociationCheck(
            ServerLevel level,
            LabJob job,
            NetworkDeviceBlockEntity client,
            NetworkDeviceBlockEntity router
    ) {
        if (!stationReady(
                client
        )) {
            retryStage(
                    level,
                    job,
                    router,
                    Phase.CLIENT_SCAN,
                    "client association incomplete"
                            + " | state="
                            + client.wifiStationState()
                            + "/"
                            + client.wifiSecurityState()
            );
            return;
        }

        router.sendWifiBeacon();

        update(
                job.next(
                        Phase.SERVER_SCAN,
                        0,
                        level.getGameTime()
                                + SHORT_WAIT,
                        "CLIENT_READY"
                                + " | next=SERVER_SCAN"
                )
        );
    }

    private static void serverScan(
            ServerLevel level,
            LabJob job,
            NetworkDeviceBlockEntity server,
            NetworkDeviceBlockEntity router
    ) {
        double frequency =
                router.activeFrequencyHz();

        server.configureWifiActiveFrequency(
                frequency
        );

        router.sendWifiBeacon();

        server.scanWifi();

        server.configureWifiActiveFrequency(
                frequency
        );

        router.sendWifiBeacon();

        update(
                job.next(
                        Phase.SERVER_DISCOVERY_CHECK,
                        job.retries(),
                        level.getGameTime()
                                + DISCOVERY_WAIT,
                        "SERVER_SCANNING"
                                + " | channel="
                                + formatFrequency(
                                frequency
                        )
                )
        );
    }

    private static void serverDiscoveryCheck(
            ServerLevel level,
            LabJob job,
            NetworkDeviceBlockEntity server,
            NetworkDeviceBlockEntity router
    ) {
        boolean saw =
                sawLabAp(
                        server
                );

        if (!saw) {
            retryStage(
                    level,
                    job,
                    router,
                    Phase.SERVER_SCAN,
                    "server did not discover AP"
            );
            return;
        }

        boolean started =
                server.connectWifi(
                        SSID
                );

        if (!started) {
            retryStage(
                    level,
                    job,
                    router,
                    Phase.SERVER_SCAN,
                    "server discovered AP but connectWifi returned false"
            );
            return;
        }

        update(
                job.next(
                        Phase.SERVER_ASSOCIATION_CHECK,
                        0,
                        level.getGameTime()
                                + ASSOCIATION_WAIT,
                        "SERVER_CONNECTING"
                )
        );
    }

    private static void serverAssociationCheck(
            MinecraftServer minecraftServer,
            ServerLevel level,
            LabJob job,
            NetworkDeviceBlockEntity client,
            NetworkDeviceBlockEntity server,
            NetworkDeviceBlockEntity router
    ) {
        if (!stationReady(
                server
        )) {
            retryStage(
                    level,
                    job,
                    router,
                    Phase.SERVER_SCAN,
                    "server association incomplete"
                            + " | state="
                            + server.wifiStationState()
                            + "/"
                            + server.wifiSecurityState()
            );
            return;
        }

        if (!stationReady(
                client
        )) {
            retryStage(
                    level,
                    job,
                    router,
                    Phase.CLIENT_SCAN,
                    "client lost association while server was joining"
            );
            return;
        }

        String message =
                "W1.10.6 lab READY"
                        + " | client 192.168.1.100"
                        + " | router 192.168.1.1/192.168.2.1"
                        + " | server 192.168.2.20"
                        + " | channel="
                        + formatFrequency(
                        router.activeFrequencyHz()
                );

        LAST_STATUS.put(
                job.owner(),
                message
        );

        JOBS.remove(
                job.owner()
        );

        send(
                minecraftServer,
                job.owner(),
                message
        );
    }

    private static void retryStage(
            ServerLevel level,
            LabJob job,
            NetworkDeviceBlockEntity router,
            Phase retryPhase,
            String reason
    ) {
        int nextRetry =
                job.retries() + 1;

        if (nextRetry
                > MAX_STAGE_RETRIES) {
            MinecraftServer minecraftServer =
                    ServerLifecycleHooks.getCurrentServer();

            if (minecraftServer != null) {
                fail(
                        minecraftServer,
                        job,
                        reason
                                + " | stage="
                                + retryPhase
                                + " | retries="
                                + job.retries()
                );
            }

            return;
        }

        router.sendWifiBeacon();

        update(
                job.next(
                        retryPhase,
                        nextRetry,
                        level.getGameTime()
                                + SHORT_WAIT,
                        "RETRY "
                                + nextRetry
                                + "/"
                                + MAX_STAGE_RETRIES
                                + " | "
                                + reason
                )
        );
    }

    private static boolean sawLabAp(
            NetworkDeviceBlockEntity device
    ) {
        return device.discoveredWifiNetworks()
                .stream()
                .anyMatch(
                        network ->
                                SSID.equals(
                                        network.ssid()
                                )
                );
    }

    private static boolean stationReady(
            NetworkDeviceBlockEntity device
    ) {
        return "ASSOCIATED".equals(
                device.wifiStationState()
                        .name()
        )
                && "SECURED".equals(
                device.wifiSecurityState()
                        .name()
        );
    }

    private static void fail(
            MinecraftServer minecraftServer,
            LabJob job,
            String reason
    ) {
        String message =
                "W1.10.6 lab FAILED"
                        + " | phase="
                        + job.phase()
                        + " | "
                        + reason;

        LAST_STATUS.put(
                job.owner(),
                message
        );

        JOBS.remove(
                job.owner()
        );

        send(
                minecraftServer,
                job.owner(),
                message
        );
    }

    private static void update(
            LabJob job
    ) {
        JOBS.put(
                job.owner(),
                job
        );

        LAST_STATUS.put(
                job.owner(),
                describe(
                        job
                )
        );
    }

    private static String describe(
            LabJob job
    ) {
        return "W1.10.6 lab "
                + job.status()
                + " | phase="
                + job.phase()
                + " | retries="
                + job.retries();
    }

    private static String formatFrequency(
            double frequencyHz
    ) {
        return String.format(
                java.util.Locale.ROOT,
                "%.3f GHz",
                frequencyHz / 1.0e9
        );
    }

    private static NetworkDeviceBlockEntity deviceAt(
            ServerLevel level,
            BlockPos pos
    ) {
        BlockEntity blockEntity =
                level.getBlockEntity(
                        pos
                );

        return blockEntity
                instanceof NetworkDeviceBlockEntity device
                ? device
                : null;
    }

    private static void send(
            MinecraftServer minecraftServer,
            UUID owner,
            String text
    ) {
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

    private enum Phase {
        CONFIGURE,
        CLIENT_SCAN,
        CLIENT_DISCOVERY_CHECK,
        CLIENT_ASSOCIATION_CHECK,
        SERVER_SCAN,
        SERVER_DISCOVERY_CHECK,
        SERVER_ASSOCIATION_CHECK
    }

    private record LabJob(
            UUID owner,
            ResourceKey<Level> dimension,
            BlockPos clientPos,
            BlockPos routerPos,
            BlockPos serverPos,
            Phase phase,
            int retries,
            long nextActionTick,
            String status
    ) {
        private LabJob next(
                Phase nextPhase,
                int nextRetries,
                long tick,
                String nextStatus
        ) {
            return new LabJob(
                    owner,
                    dimension,
                    clientPos,
                    routerPos,
                    serverPos,
                    nextPhase,
                    nextRetries,
                    tick,
                    nextStatus
            );
        }
    }
}
