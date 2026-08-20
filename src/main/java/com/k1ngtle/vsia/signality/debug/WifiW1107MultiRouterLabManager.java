package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class WifiW1107MultiRouterLabManager {
    public static final String SSID =
            "VSIA-W1107-MULTIHOP";

    private static final Map<UUID, String> LAST_STATUS =
            new LinkedHashMap<>();

    private WifiW1107MultiRouterLabManager() {
    }

    public static String setup(
            UUID owner,
            ResourceKey<Level> dimension,
            BlockPos clientPos,
            BlockPos routerAPos,
            BlockPos routerBPos,
            BlockPos serverPos
    ) {
        MinecraftServer minecraftServer =
                ServerLifecycleHooks.getCurrentServer();

        if (minecraftServer == null) {
            return fail(
                    owner,
                    "server unavailable"
            );
        }

        ServerLevel level =
                minecraftServer.getLevel(
                        dimension
                );

        if (level == null) {
            return fail(
                    owner,
                    "dimension unavailable"
            );
        }

        NetworkDeviceBlockEntity client =
                deviceAt(
                        level,
                        clientPos
                );

        NetworkDeviceBlockEntity routerA =
                deviceAt(
                        level,
                        routerAPos
                );

        NetworkDeviceBlockEntity routerB =
                deviceAt(
                        level,
                        routerBPos
                );

        NetworkDeviceBlockEntity server =
                deviceAt(
                        level,
                        serverPos
                );

        if (client == null
                || routerA == null
                || routerB == null
                || server == null) {
            return fail(
                    owner,
                    "client/routerA/routerB/server not loaded"
            );
        }

        routerA.resetWifiLiveRouterConfiguration();
        routerB.resetWifiLiveRouterConfiguration();

        if (!routerA.provisionWifiLabAccessPoint(
                SSID
        )) {
            return fail(
                    owner,
                    "Router A AP provisioning failed"
            );
        }

        double frequency =
                routerA.activeFrequencyHz();

        if (!client.provisionWifiLabStationLink(
                SSID,
                routerA.wifiMacAddress(),
                frequency
        )) {
            return fail(
                    owner,
                    "client link provisioning failed"
            );
        }

        if (!routerB.provisionWifiLabStationLink(
                SSID,
                routerA.wifiMacAddress(),
                frequency
        )) {
            return fail(
                    owner,
                    "Router B link provisioning failed"
            );
        }

        if (!server.provisionWifiLabStationLink(
                SSID,
                routerA.wifiMacAddress(),
                frequency
        )) {
            return fail(
                    owner,
                    "server link provisioning failed"
            );
        }

        if (!routerA.provisionWifiLabAssociatedStation(
                client.wifiMacAddress()
        )
                || !routerA.provisionWifiLabAssociatedStation(
                routerB.wifiMacAddress()
        )
                || !routerA.provisionWifiLabAssociatedStation(
                server.wifiMacAddress()
        )) {
            return fail(
                    owner,
                    "Router A associated-station provisioning failed"
            );
        }

        client.configureWifiStaticIpv4(
                "192.168.1.100",
                "255.255.255.0",
                "192.168.1.1"
        );

        routerA.configureWifiStaticIpv4(
                "192.168.1.1",
                "255.255.255.0",
                ""
        );

        routerB.configureWifiStaticIpv4(
                "10.0.0.2",
                "255.255.255.252",
                ""
        );

        server.configureWifiStaticIpv4(
                "192.168.2.20",
                "255.255.255.0",
                "192.168.2.1"
        );

        boolean aLan =
                routerA.configureWifiLiveRouterInterface(
                        "lan0",
                        "192.168.1.1",
                        24
                );

        boolean aTransit =
                routerA.configureWifiLiveRouterInterface(
                        "transit0",
                        "10.0.0.1",
                        30
                );

        boolean bTransit =
                routerB.configureWifiLiveRouterInterface(
                        "transit0",
                        "10.0.0.2",
                        30
                );

        boolean bLan =
                routerB.configureWifiLiveRouterInterface(
                        "lan1",
                        "192.168.2.1",
                        24
                );

        if (!aLan
                || !aTransit
                || !bTransit
                || !bLan) {
            return fail(
                    owner,
                    "router interface configuration failed"
            );
        }

        boolean aRoute =
                routerA.addWifiLiveRouterRoute(
                        "192.168.2.0",
                        24,
                        "10.0.0.2",
                        "transit0",
                        10
                );

        boolean bRoute =
                routerB.addWifiLiveRouterRoute(
                        "192.168.1.0",
                        24,
                        "10.0.0.1",
                        "transit0",
                        10
                );

        if (!aRoute
                || !bRoute) {
            return fail(
                    owner,
                    "static route configuration failed"
            );
        }

        routerA.setWifiLiveRouterEnabled(
                true
        );

        routerB.setWifiLiveRouterEnabled(
                true
        );

        String result =
                "W1.10.7 lab READY"
                        + " | client=192.168.1.100"
                        + " | A=192.168.1.1/10.0.0.1"
                        + " | B=10.0.0.2/192.168.2.1"
                        + " | server=192.168.2.20"
                        + " | transit=10.0.0.0/30"
                        + " | discovery=PROVISIONED"
                        + " | association=PROVISIONED"
                        + " | routing-data-path=LIVE";

        LAST_STATUS.put(
                owner,
                result
        );

        return result;
    }

    public static String status(
            UUID owner
    ) {
        return LAST_STATUS.getOrDefault(
                owner,
                "No W1.10.7 multi-router lab has been configured in this session."
        );
    }

    private static String fail(
            UUID owner,
            String reason
    ) {
        String result =
                "W1.10.7 lab FAILED | "
                        + reason;

        LAST_STATUS.put(
                owner,
                result
        );

        return result;
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
}
