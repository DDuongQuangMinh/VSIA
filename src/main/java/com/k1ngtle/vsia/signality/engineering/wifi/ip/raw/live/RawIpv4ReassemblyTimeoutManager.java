package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw.live;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.raw.RawIpv4ReassemblyKey;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class RawIpv4ReassemblyTimeoutManager {
    private static final long MICROS_PER_TICK = 50_000L;

    private static final Map<String, TimeoutJob> JOBS =
            new LinkedHashMap<>();

    private RawIpv4ReassemblyTimeoutManager() {
    }

    public static void schedule(
            ServerLevel level,
            BlockPos devicePos,
            RawIpv4ReassemblyKey key,
            long timeoutMicros
    ) {
        if (level == null
                || devicePos == null
                || key == null
                || timeoutMicros <= 0) {
            return;
        }

        long delayTicks =
                Math.max(
                        1L,
                        (timeoutMicros + MICROS_PER_TICK - 1L)
                                / MICROS_PER_TICK
                );

        String jobKey =
                jobKey(
                        level.dimension(),
                        devicePos,
                        key
                );

        JOBS.put(
                jobKey,
                new TimeoutJob(
                        level.dimension(),
                        devicePos.immutable(),
                        key,
                        level.getGameTime() + delayTicks
                )
        );
    }

    public static void cancel(
            ServerLevel level,
            BlockPos devicePos,
            RawIpv4ReassemblyKey key
    ) {
        if (level == null
                || devicePos == null
                || key == null) {
            return;
        }

        JOBS.remove(
                jobKey(
                        level.dimension(),
                        devicePos,
                        key
                )
        );
    }

    public static void cancelDevice(
            ServerLevel level,
            BlockPos devicePos
    ) {
        if (level == null
                || devicePos == null) {
            return;
        }

        String prefix =
                level.dimension()
                        .location()
                        + "|"
                        + devicePos.asLong()
                        + "|";

        JOBS.keySet()
                .removeIf(
                        key ->
                                key.startsWith(
                                        prefix
                                )
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

        MinecraftServer server =
                ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            return;
        }

        Iterator<Map.Entry<String, TimeoutJob>> iterator =
                JOBS.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, TimeoutJob> entry =
                    iterator.next();

            TimeoutJob job =
                    entry.getValue();

            ServerLevel level =
                    server.getLevel(
                            job.dimension()
                    );

            if (level == null) {
                iterator.remove();
                continue;
            }

            if (level.getGameTime()
                    < job.deadlineTick()) {
                continue;
            }

            BlockEntity blockEntity =
                    level.getBlockEntity(
                            job.devicePos()
                    );

            if (!(blockEntity
                    instanceof NetworkDeviceBlockEntity device)) {
                iterator.remove();
                continue;
            }

            device.wifiRawIpv4ReassemblyTimeout(
                    job.key()
            );

            iterator.remove();
        }
    }

    private static String jobKey(
            ResourceKey<Level> dimension,
            BlockPos pos,
            RawIpv4ReassemblyKey key
    ) {
        return dimension.location()
                + "|"
                + pos.asLong()
                + "|"
                + key.sourceIp()
                + "|"
                + key.destinationIp()
                + "|"
                + key.protocol()
                + "|"
                + key.identification();
    }

    private record TimeoutJob(
            ResourceKey<Level> dimension,
            BlockPos devicePos,
            RawIpv4ReassemblyKey key,
            long deadlineTick
    ) {
    }
}
