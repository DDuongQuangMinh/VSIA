package com.k1ngtle.vsia.signality.debug;

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
import net.minecraftforge.server.ServerLifecycleHooks;

public final class WifiW1106LabManager {
    public static final String SSID =
            "VSIA-W1106-LAB";

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
        MinecraftServer minecraftServer =
                ServerLifecycleHooks.getCurrentServer();

        if (minecraftServer == null) {
            LAST_STATUS.put(
                    owner,
                    "W1.10.6 lab FAILED | server unavailable"
            );
            return;
        }

        ServerLevel level =
                minecraftServer.getLevel(
                        dimension
                );

        if (level == null) {
            fail(
                    minecraftServer,
                    owner,
                    "dimension unavailable"
            );
            return;
        }

        NetworkDeviceBlockEntity client =
                deviceAt(
                        level,
                        clientPos
                );

        NetworkDeviceBlockEntity router =
                deviceAt(
                        level,
                        routerPos
                );

        NetworkDeviceBlockEntity server =
                deviceAt(
                        level,
                        serverPos
                );

        if (client == null
                || router == null
                || server == null) {
            fail(
                    minecraftServer,
                    owner,
                    "client/router/server not loaded"
            );
            return;
        }

        if (!router.provisionWifiLabAccessPoint(
                SSID
        )) {
            fail(
                    minecraftServer,
                    owner,
                    "router AP provisioning failed"
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

        double frequency =
                router.activeFrequencyHz();

        if (!client.provisionWifiLabStationLink(
                SSID,
                router.wifiMacAddress(),
                frequency
        )) {
            fail(
                    minecraftServer,
                    owner,
                    "client lab link provisioning failed"
            );
            return;
        }

        if (!server.provisionWifiLabStationLink(
                SSID,
                router.wifiMacAddress(),
                frequency
        )) {
            fail(
                    minecraftServer,
                    owner,
                    "server lab link provisioning failed"
            );
            return;
        }

        if (!router.provisionWifiLabAssociatedStation(
                client.wifiMacAddress()
        )
                || !router.provisionWifiLabAssociatedStation(
                server.wifiMacAddress()
        )) {
            fail(
                    minecraftServer,
                    owner,
                    "router station association provisioning failed"
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

        String result =
                "W1.10.6 lab READY"
                        + " | client 192.168.1.100"
                        + " | router 192.168.1.1/192.168.2.1"
                        + " | server 192.168.2.20"
                        + " | channel="
                        + formatFrequency(
                        frequency
                )
                        + " | discovery=PROVISIONED"
                        + " | association=PROVISIONED"
                        + " | security=OPEN_LAB"
                        + " | routing-data-path=LIVE";

        LAST_STATUS.put(
                owner,
                result
        );

        send(
                minecraftServer,
                owner,
                result
        );
    }

    public static String status(
            UUID owner
    ) {
        return LAST_STATUS.getOrDefault(
                owner,
                "No W1.10.6 lab setup has been run in this session."
        );
    }

    private static void fail(
            MinecraftServer minecraftServer,
            UUID owner,
            String reason
    ) {
        String result =
                "W1.10.6 lab FAILED | "
                        + reason;

        LAST_STATUS.put(
                owner,
                result
        );

        send(
                minecraftServer,
                owner,
                result
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

    private static String formatFrequency(
            double frequencyHz
    ) {
        return String.format(
                java.util.Locale.ROOT,
                "%.3f GHz",
                frequencyHz / 1.0e9
        );
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
}
