package com.k1ngtle.vsia.signality.engineering.firewall.w117;

import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class W117InterfaceNeighborEngine {
    private static final int MAX_PENDING_PER_NEIGHBOR = 8;
    private static final int MAX_PENDING_TOTAL = 64;
    private static final int MAX_ARP_ATTEMPTS = 3;
    private static final long ARP_RETRY_MILLIS = 1_000L;

    private final W117NeighborCache cache =
            new W117NeighborCache(60_000L);

    private final Map<String, PendingResolution> pending =
            new LinkedHashMap<>();

    private long arpRequestsTx = 0L;
    private long arpRepliesTx = 0L;
    private long arpLearned = 0L;
    private long arpFailures = 0L;
    private long pendingDrops = 0L;
    private String lastEvent = "READY";

    public Optional<W117NeighborCache.Entry> lookup(
            String ipv4,
            long nowMillis
    ) {
        return cache.lookup(
                ipv4,
                nowMillis
        );
    }

    public void learn(
            String ipv4,
            String mac,
            long nowMillis
    ) {
        cache.learn(
                ipv4,
                mac,
                nowMillis
        );
        arpLearned++;

        PendingResolution resolution =
                pending.remove(ipv4);

        if (resolution != null) {
            resolution.resolvedMac = mac;
            resolution.resolved = true;
            pending.put(
                    "__resolved__" + ipv4,
                    resolution
            );
        }

        lastEvent =
                "ARP_LEARNED "
                        + ipv4
                        + " -> "
                        + mac;
    }

    public boolean queue(
            String nextHop,
            OSINetworkPacket packet,
            String egressPort,
            long nowMillis
    ) {
        if (pendingCount() >= MAX_PENDING_TOTAL) {
            pendingDrops++;
            lastEvent =
                    "DROP_PENDING_GLOBAL_LIMIT";
            return false;
        }

        PendingResolution resolution =
                pending.computeIfAbsent(
                        nextHop,
                        key -> new PendingResolution(
                                nextHop,
                                egressPort,
                                nowMillis
                        )
                );

        if (resolution.frames.size()
                >= MAX_PENDING_PER_NEIGHBOR) {
            pendingDrops++;
            lastEvent =
                    "DROP_PENDING_NEIGHBOR_LIMIT "
                            + nextHop;
            return false;
        }

        resolution.frames.add(
                OSINetworkPacket.deserializeNBT(
                        packet.serializeNBT().copy()
                )
        );

        lastEvent =
                "QUEUED nextHop="
                        + nextHop
                        + " depth="
                        + resolution.frames.size();

        return true;
    }

    public boolean needsInitialRequest(
            String nextHop
    ) {
        PendingResolution resolution =
                pending.get(nextHop);

        return resolution != null
                && resolution.attempts == 0;
    }

    public void markRequestSent(
            String nextHop,
            long nowMillis
    ) {
        PendingResolution resolution =
                pending.get(nextHop);

        if (resolution == null) {
            return;
        }

        resolution.attempts++;
        resolution.nextRetryMillis =
                nowMillis + ARP_RETRY_MILLIS;
        arpRequestsTx++;

        lastEvent =
                "ARP_REQUEST nextHop="
                        + nextHop
                        + " attempt="
                        + resolution.attempts;
    }

    public List<ResolvedPacket> drainResolved() {
        List<ResolvedPacket> output =
                new ArrayList<>();

        List<String> remove =
                new ArrayList<>();

        for (Map.Entry<String, PendingResolution> entry :
                pending.entrySet()) {
            PendingResolution resolution =
                    entry.getValue();

            if (!resolution.resolved) {
                continue;
            }

            for (OSINetworkPacket packet :
                    resolution.frames) {
                output.add(
                        new ResolvedPacket(
                                packet,
                                resolution.egressPort,
                                resolution.nextHop,
                                resolution.resolvedMac
                        )
                );
            }

            remove.add(entry.getKey());
        }

        for (String key : remove) {
            pending.remove(key);
        }

        return output;
    }

    public List<Retry> tick(
            long nowMillis
    ) {
        cache.expire(nowMillis);

        List<Retry> retries =
                new ArrayList<>();

        List<String> failed =
                new ArrayList<>();

        for (Map.Entry<String, PendingResolution> entry :
                pending.entrySet()) {
            if (entry.getKey().startsWith("__resolved__")) {
                continue;
            }

            PendingResolution resolution =
                    entry.getValue();

            if (nowMillis
                    < resolution.nextRetryMillis) {
                continue;
            }

            if (resolution.attempts
                    >= MAX_ARP_ATTEMPTS) {
                arpFailures++;
                pendingDrops +=
                        resolution.frames.size();

                failed.add(entry.getKey());

                lastEvent =
                        "ARP_FAILED nextHop="
                                + resolution.nextHop;

                continue;
            }

            resolution.attempts++;
            resolution.nextRetryMillis =
                    nowMillis + ARP_RETRY_MILLIS;
            arpRequestsTx++;

            retries.add(
                    new Retry(
                            resolution.nextHop,
                            resolution.egressPort,
                            resolution.attempts
                    )
            );

            lastEvent =
                    "ARP_RETRY nextHop="
                            + resolution.nextHop
                            + " attempt="
                            + resolution.attempts;
        }

        for (String key : failed) {
            pending.remove(key);
        }

        return retries;
    }

    public int neighborCount(long nowMillis) {
        return cache.size(nowMillis);
    }

    public int pendingCount() {
        int total = 0;

        for (Map.Entry<String, PendingResolution> entry :
                pending.entrySet()) {
            if (!entry.getKey().startsWith("__resolved__")) {
                total += entry.getValue().frames.size();
            }
        }

        return total;
    }

    public void noteReplyTx() {
        arpRepliesTx++;
    }

    public void clear() {
        cache.clear();
        pending.clear();
        arpRequestsTx = 0L;
        arpRepliesTx = 0L;
        arpLearned = 0L;
        arpFailures = 0L;
        pendingDrops = 0L;
        lastEvent = "READY";
    }

    public String status(long nowMillis) {
        return "neighbors="
                + neighborCount(nowMillis)
                + " pending="
                + pendingCount()
                + " arpReqTx="
                + arpRequestsTx
                + " arpRepTx="
                + arpRepliesTx
                + " arpLearned="
                + arpLearned
                + " arpFailures="
                + arpFailures
                + " pendingDrops="
                + pendingDrops
                + " last="
                + lastEvent;
    }

    public record ResolvedPacket(
            OSINetworkPacket packet,
            String egressPort,
            String nextHopIp,
            String nextHopMac
    ) {
    }

    public record Retry(
            String nextHopIp,
            String egressPort,
            int attempt
    ) {
    }

    private static final class PendingResolution {
        private final String nextHop;
        private final String egressPort;
        private final List<OSINetworkPacket> frames =
                new ArrayList<>();
        private int attempts = 0;
        private long nextRetryMillis;
        private boolean resolved = false;
        private String resolvedMac = "";

        private PendingResolution(
                String nextHop,
                String egressPort,
                long nowMillis
        ) {
            this.nextHop = nextHop;
            this.egressPort = egressPort;
            this.nextRetryMillis = nowMillis;
        }
    }
}
