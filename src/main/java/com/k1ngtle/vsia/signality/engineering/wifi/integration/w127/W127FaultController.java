package com.k1ngtle.vsia.signality.engineering.wifi.integration.w127;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.ServerRackDirectory;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class W127FaultController {
    private static final List<W127FaultRule> RULES =
            new ArrayList<>();

    private static final List<DelayedPacket> DELAYED =
            new ArrayList<>();

    private static long matched;
    private static long dropped;
    private static long duplicated;
    private static long delayed;
    private static long delayedDelivered;
    private static long mutated;
    private static String lastEvent = "NONE";

    private W127FaultController() {
    }

    public static synchronized W127FaultRule arm(
            W127FaultAction action,
            String sourceIp,
            String targetIp,
            String applicationProtocol,
            String pdnsKind,
            String xfrKind,
            Integer sequence,
            Boolean response,
            int matches,
            long delayTicks,
            long forcedSerial
    ) {
        W127FaultRule rule =
                new W127FaultRule(
                        action,
                        sourceIp,
                        targetIp,
                        applicationProtocol,
                        pdnsKind,
                        xfrKind,
                        sequence,
                        response,
                        matches,
                        delayTicks,
                        forcedSerial
                );

        RULES.add(rule);
        lastEvent = "ARM " + rule.compact();

        return rule;
    }

    public static synchronized boolean intercept(
            ServerRackBlockEntity rack,
            OSINetworkPacket packet
    ) {
        if (rack == null
                || packet == null
                || RULES.isEmpty()) {
            return false;
        }

        W127FaultRule selected = null;

        for (W127FaultRule rule : RULES) {
            if (rule.matches(packet)
                    && rule.consume()) {
                selected = rule;
                break;
            }
        }

        RULES.removeIf(
                W127FaultRule::exhausted
        );

        if (selected == null) {
            return false;
        }

        matched++;

        switch (selected.action()) {
            case DROP -> {
                dropped++;
                lastEvent =
                        "DROP "
                                + describe(packet);
                return true;
            }

            case DUPLICATE -> {
                duplicated++;
                lastEvent =
                        "DUPLICATE "
                                + describe(packet);

                rack.w127PhysicalDnsTransmitImmediate(
                        packet
                );

                rack.w127PhysicalDnsTransmitImmediate(
                        W127PacketMutator.copy(
                                packet
                        )
                );

                return true;
            }

            case DELAY -> {
                if (!(rack.getLevel() instanceof ServerLevel level)) {
                    dropped++;
                    lastEvent =
                            "DELAY_DROP_NO_LEVEL "
                                    + describe(packet);
                    return true;
                }

                if (DELAYED.size()
                        >= W127ResiliencePolicy.MAX_DELAYED_PACKETS) {
                    dropped++;
                    lastEvent =
                            "DELAY_DROP_QUEUE_FULL "
                                    + describe(packet);
                    return true;
                }

                long due =
                        level.getGameTime()
                                + selected.delayTicks();

                DELAYED.add(
                        new DelayedPacket(
                                level,
                                rack.ipAddress(),
                                W127PacketMutator.copy(packet),
                                due
                        )
                );

                delayed++;
                lastEvent =
                        "DELAY "
                                + selected.delayTicks()
                                + "t "
                                + describe(packet);

                return true;
            }

            case CORRUPT_RRSIG -> {
                mutated++;
                lastEvent =
                        "CORRUPT_RRSIG "
                                + describe(packet);

                rack.w127PhysicalDnsTransmitImmediate(
                        W127PacketMutator.corruptRrsig(
                                packet
                        )
                );

                return true;
            }

            case EXPIRE_RRSIG -> {
                mutated++;
                long now =
                        System.currentTimeMillis()
                                / 1000L;

                lastEvent =
                        "EXPIRE_RRSIG "
                                + describe(packet);

                rack.w127PhysicalDnsTransmitImmediate(
                        W127PacketMutator.expireRrsig(
                                packet,
                                now
                        )
                );

                return true;
            }

            case CORRUPT_DNSKEY -> {
                mutated++;
                lastEvent =
                        "CORRUPT_DNSKEY "
                                + describe(packet);

                rack.w127PhysicalDnsTransmitImmediate(
                        W127PacketMutator.corruptDnskey(
                                packet
                        )
                );

                return true;
            }

            case FORCE_XFR_SERIAL -> {
                mutated++;
                lastEvent =
                        "FORCE_XFR_SERIAL="
                                + Long.toUnsignedString(
                                selected.forcedSerial()
                        )
                                + " "
                                + describe(packet);

                rack.w127PhysicalDnsTransmitImmediate(
                        W127PacketMutator.forceTransferSerial(
                                packet,
                                selected.forcedSerial()
                        )
                );

                return true;
            }

            case SERVFAIL -> {
                mutated++;
                lastEvent =
                        "SERVFAIL "
                                + describe(packet);

                rack.w127PhysicalDnsTransmitImmediate(
                        W127PacketMutator.servfail(
                                packet
                        )
                );

                return true;
            }
        }

        return false;
    }

    public static synchronized void clearRules() {
        RULES.clear();
        lastEvent = "RULES_CLEARED";
    }

    public static synchronized void clearAll() {
        RULES.clear();
        DELAYED.clear();
        lastEvent = "ALL_FAULTS_CLEARED";
    }

    public static synchronized void resetMetrics() {
        matched = 0L;
        dropped = 0L;
        duplicated = 0L;
        delayed = 0L;
        delayedDelivered = 0L;
        mutated = 0L;
        lastEvent = "METRICS_RESET";
    }

    public static synchronized W127FaultSnapshot snapshot() {
        return new W127FaultSnapshot(
                RULES.size(),
                DELAYED.size(),
                matched,
                dropped,
                duplicated,
                delayed,
                delayedDelivered,
                mutated,
                lastEvent
        );
    }

    public static synchronized List<String> rules() {
        return RULES.stream()
                .map(W127FaultRule::compact)
                .toList();
    }

    @SubscribeEvent
    public static void onServerTick(
            TickEvent.ServerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        List<DelayedPacket> due =
                new ArrayList<>();

        synchronized (W127FaultController.class) {
            Iterator<DelayedPacket> iterator =
                    DELAYED.iterator();

            while (iterator.hasNext()) {
                DelayedPacket packet =
                        iterator.next();

                if (packet.level.getGameTime()
                        >= packet.dueTick) {
                    due.add(packet);
                    iterator.remove();
                }
            }
        }

        for (DelayedPacket packet : due) {
            ServerRackBlockEntity rack =
                    ServerRackDirectory.byIp(
                            packet.level,
                            packet.sourceRackIp
                    );

            synchronized (W127FaultController.class) {
                if (rack == null) {
                    dropped++;
                    lastEvent =
                            "DELAY_DROP_SOURCE_GONE "
                                    + packet.sourceRackIp;
                    continue;
                }

                delayedDelivered++;
                lastEvent =
                        "DELAY_DELIVER "
                                + describe(packet.packet);
            }

            rack.w127PhysicalDnsTransmitImmediate(
                    packet.packet
            );
        }
    }

    private static String describe(
            OSINetworkPacket packet
    ) {
        return packet.sourceIp
                + "->"
                + packet.targetIp
                + " "
                + packet.applicationProtocol
                + " pdns="
                + packet.payload.getString(
                "pdns_kind"
        )
                + " xfr="
                + packet.payload.getString(
                "xfr_kind"
        )
                + " seq="
                + packet.payload.getInt(
                "sequence"
        );
    }

    private record DelayedPacket(
            ServerLevel level,
            String sourceRackIp,
            OSINetworkPacket packet,
            long dueTick
    ) {
    }
}
