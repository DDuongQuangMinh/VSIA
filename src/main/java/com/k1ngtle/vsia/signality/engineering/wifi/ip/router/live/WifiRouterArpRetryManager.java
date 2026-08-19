package com.k1ngtle.vsia.signality.engineering.wifi.ip.router.live;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
public final class WifiRouterArpRetryManager {
    private static final long RETRY_TICKS = 20L;
    private static final int MAX_ATTEMPTS = 3;

    private static final Map<String, RetryJob> JOBS =
            new LinkedHashMap<>();

    private WifiRouterArpRetryManager() {
    }

    public static void schedule(
            ServerLevel level,
            BlockPos routerPos,
            String interfaceName,
            String nextHopIp
    ) {
        if (level == null
                || routerPos == null
                || interfaceName == null
                || interfaceName.isBlank()
                || nextHopIp == null
                || nextHopIp.isBlank()) {
            return;
        }

        String key =
                level.dimension().location()
                        + "|"
                        + routerPos.asLong()
                        + "|"
                        + interfaceName
                        + "|"
                        + nextHopIp;

        JOBS.computeIfAbsent(
                key,
                ignored ->
                        new RetryJob(
                                level.dimension(),
                                routerPos.immutable(),
                                interfaceName,
                                nextHopIp,
                                level.getGameTime() + RETRY_TICKS,
                                1
                        )
        );
    }

    public static void cancel(
            ServerLevel level,
            BlockPos routerPos,
            String interfaceName,
            String nextHopIp
    ) {
        if (level == null
                || routerPos == null) {
            return;
        }

        String key =
                level.dimension().location()
                        + "|"
                        + routerPos.asLong()
                        + "|"
                        + interfaceName
                        + "|"
                        + nextHopIp;

        JOBS.remove(key);
    }

    @SubscribeEvent
    public static void onServerTick(
            TickEvent.ServerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END
                || JOBS.isEmpty()) {
            return;
        }

        MinecraftServer server =
                ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            return;
        }

        Iterator<Map.Entry<String, RetryJob>> iterator =
                JOBS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, RetryJob> entry =
                    iterator.next();

            RetryJob job =
                    entry.getValue();

            ServerLevel level =
                    server.getLevel(
                            job.dimension()
                    );

            if (level == null) {
                iterator.remove();
                continue;
            }

            long now =
                    level.getGameTime();

            if (now < job.nextAttemptTick()) {
                continue;
            }

            BlockEntity blockEntity =
                    level.getBlockEntity(
                            job.routerPos()
                    );

            if (!(blockEntity
                    instanceof NetworkDeviceBlockEntity router)) {
                iterator.remove();
                continue;
            }

            if (router.wifiRouterHasNeighbor(
                    job.interfaceName(),
                    job.nextHopIp()
            )) {
                iterator.remove();
                continue;
            }

            if (!router.wifiRouterHasPending(
                    job.interfaceName(),
                    job.nextHopIp()
            )) {
                iterator.remove();
                continue;
            }

            if (job.attemptsSent()
                    >= MAX_ATTEMPTS) {
                router.wifiRouterArpRetryExhausted(
                        job.interfaceName(),
                        job.nextHopIp(),
                        job.attemptsSent()
                );

                iterator.remove();
                continue;
            }

            int nextAttempt =
                    job.attemptsSent() + 1;

            router.retryWifiRouterArpRequest(
                    job.interfaceName(),
                    job.nextHopIp(),
                    nextAttempt
            );

            entry.setValue(
                    new RetryJob(
                            job.dimension(),
                            job.routerPos(),
                            job.interfaceName(),
                            job.nextHopIp(),
                            now + RETRY_TICKS,
                            nextAttempt
                    )
            );
        }
    }

    private record RetryJob(
            ResourceKey<Level> dimension,
            BlockPos routerPos,
            String interfaceName,
            String nextHopIp,
            long nextAttemptTick,
            int attemptsSent
    ) {
    }
}
