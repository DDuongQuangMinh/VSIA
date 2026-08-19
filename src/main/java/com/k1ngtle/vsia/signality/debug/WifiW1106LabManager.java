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

    private static final int MAX_ASSOCIATION_RETRIES =
            5;

    private static final long CONNECT_SETTLE_TICKS =
            80L;

    private static final long RETRY_WAIT_TICKS =
            20L;

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

                case CLIENT_CONNECT ->
                        connectClient(
                                minecraftServer,
                                level,
                                job,
                                client,
                                router
                        );

                case CLIENT_VERIFY ->
                        verifyClient(
                                minecraftServer,
                                level,
                                job,
                                client,
                                router
                        );

                case SERVER_CONNECT ->
                        connectServer(
                                minecraftServer,
                                level,
                                job,
                                server,
                                router
                        );

                case SERVER_VERIFY ->
                        verifyServer(
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
        if (!router.configureWifiAccessPoint(
                SSID
        )) {
            fail(
                    minecraftServer,
                    job,
                    "router could not enter AP mode"
            );
            return;
        }

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

        if (!client.configureWifiStation()
                || !server.configureWifiStation()) {
            fail(
                    minecraftServer,
                    job,
                    "client/server could not enter STATION mode"
            );
            return;
        }

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
                    "Wi-Fi channel/profile mismatch at "
                            + formatFrequency(
                            frequency
                    )
            );
            return;
        }

        update(
                job.next(
                        Phase.CLIENT_CONNECT,
                        0,
                        level.getGameTime()
                                + RETRY_WAIT_TICKS,
                        "CONFIGURED"
                                + " | channel="
                                + formatFrequency(
                                frequency
                        )
                                + " | discovery=PROVISIONED"
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
                        + " | deterministic AP provisioning enabled"
        );
    }

    private static void connectClient(
            MinecraftServer minecraftServer,
            ServerLevel level,
            LabJob job,
            NetworkDeviceBlockEntity client,
            NetworkDeviceBlockEntity router
    ) {
        if (!provisionAndConnect(
                client,
                router
        )) {
            retryOrFail(
                    minecraftServer,
                    level,
                    job,
                    Phase.CLIENT_CONNECT,
                    "client provisioning/connect failed"
            );
            return;
        }

        update(
                job.next(
                        Phase.CLIENT_VERIFY,
                        job.retries(),
                        level.getGameTime()
                                + CONNECT_SETTLE_TICKS,
                        "CLIENT_CONNECTING"
                )
        );
    }

    private static void verifyClient(
            MinecraftServer minecraftServer,
            ServerLevel level,
            LabJob job,
            NetworkDeviceBlockEntity client,
            NetworkDeviceBlockEntity router
    ) {
        if (!stationReady(
                client
        )) {
            retryOrFail(
                    minecraftServer,
                    level,
                    job,
                    Phase.CLIENT_CONNECT,
                    "client association incomplete"
                            + " | state="
                            + client.wifiStationState()
                            + "/"
                            + client.wifiSecurityState()
                            + " | diag="
                            + client.wifiSecurityDiagnostic()
            );
            return;
        }

        update(
                job.next(
                        Phase.SERVER_CONNECT,
                        0,
                        level.getGameTime()
                                + RETRY_WAIT_TICKS,
                        "CLIENT_READY"
                )
        );
    }

    private static void connectServer(
            MinecraftServer minecraftServer,
            ServerLevel level,
            LabJob job,
            NetworkDeviceBlockEntity server,
            NetworkDeviceBlockEntity router
    ) {
        if (!provisionAndConnect(
                server,
                router
        )) {
            retryOrFail(
                    minecraftServer,
                    level,
                    job,
                    Phase.SERVER_CONNECT,
                    "server provisioning/connect failed"
            );
            return;
        }

        update(
                job.next(
                        Phase.SERVER_VERIFY,
                        job.retries(),
                        level.getGameTime()
                                + CONNECT_SETTLE_TICKS,
                        "SERVER_CONNECTING"
                )
        );
    }

    private static void verifyServer(
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
            retryOrFail(
                    minecraftServer,
                    level,
                    job,
                    Phase.SERVER_CONNECT,
                    "server association incomplete"
                            + " | state="
                            + server.wifiStationState()
                            + "/"
                            + server.wifiSecurityState()
                            + " | diag="
                            + server.wifiSecurityDiagnostic()
            );
            return;
        }

        if (!stationReady(
                client
        )) {
            retryOrFail(
                    minecraftServer,
                    level,
                    job,
                    Phase.CLIENT_CONNECT,
                    "client lost association while server joined"
            );
            return;
        }

        String result =
                "W1.10.6 lab READY"
                        + " | client 192.168.1.100"
                        + " | router 192.168.1.1/192.168.2.1"
                        + " | server 192.168.2.20"
                        + " | channel="
                        + formatFrequency(
                        router.activeFrequencyHz()
                )
                        + " | discovery=PROVISIONED"
                        + " | association=LIVE";

        LAST_STATUS.put(
                job.owner(),
                result
        );

        JOBS.remove(
                job.owner()
        );

        send(
                minecraftServer,
                job.owner(),
                result
        );
    }

    private static boolean provisionAndConnect(
            NetworkDeviceBlockEntity station,
            NetworkDeviceBlockEntity router
    ) {
        double frequency =
                router.activeFrequencyHz();

        boolean provisioned =
                station.provisionWifiKnownAccessPoint(
                        SSID,
                        router.wifiMacAddress(),
                        router.wifiNetworkSecurityProfile(),
                        router.wifiNetworkProfileName(),
                        frequency
                );

        if (!provisioned) {
            return false;
        }

        router.sendWifiBeacon();

        return station.connectWifi(
                SSID
        );
    }

    private static void retryOrFail(
            MinecraftServer minecraftServer,
            ServerLevel level,
            LabJob job,
            Phase retryPhase,
            String reason
    ) {
        int retry =
                job.retries() + 1;

        if (retry
                > MAX_ASSOCIATION_RETRIES) {
            fail(
                    minecraftServer,
                    job,
                    reason
                            + " | retries="
                            + job.retries()
            );
            return;
        }

        update(
                job.next(
                        retryPhase,
                        retry,
                        level.getGameTime()
                                + RETRY_WAIT_TICKS,
                        "RETRY "
                                + retry
                                + "/"
                                + MAX_ASSOCIATION_RETRIES
                                + " | "
                                + reason
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
        CLIENT_CONNECT,
        CLIENT_VERIFY,
        SERVER_CONNECT,
        SERVER_VERIFY
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
                long nextActionTick,
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
                    nextActionTick,
                    nextStatus
            );
        }
    }
}
