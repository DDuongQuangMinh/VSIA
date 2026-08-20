package com.k1ngtle.vsia.signality.engineering.firewall.w117;

import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class W117HostEndpoint {
    private static final int MAX_PENDING_PER_NEIGHBOR = 8;
    private static final int MAX_PENDING_TOTAL = 64;
    private static final int MAX_ARP_ATTEMPTS = 3;
    private static final long ARP_RETRY_MILLIS = 1_000L;

    private final String name;
    private final String ipv4;
    private final String subnetMask;
    private final String defaultGateway;
    private final String macAddress;

    private final W117NeighborCache neighbors =
            new W117NeighborCache(60_000L);

    private final Map<String, PendingResolution> pending =
            new LinkedHashMap<>();

    private long deliveredIpv4 = 0L;
    private long arpRequestsTx = 0L;
    private long arpRepliesTx = 0L;
    private long arpLearned = 0L;
    private long arpFailures = 0L;
    private long pendingDrops = 0L;
    private boolean duplicateIpv4 = false;
    private String lastEvent = "READY";

    public W117HostEndpoint(
            String name,
            String ipv4,
            String subnetMask,
            String defaultGateway,
            String macAddress
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name");
        }

        if (!W117Ipv4.valid(ipv4)) {
            throw new IllegalArgumentException("ipv4");
        }

        if (!W117Ipv4.contiguousMask(subnetMask)) {
            throw new IllegalArgumentException("subnetMask");
        }

        if (!W117Ipv4.valid(defaultGateway)) {
            throw new IllegalArgumentException("defaultGateway");
        }

        if (macAddress == null || macAddress.isBlank()) {
            throw new IllegalArgumentException("macAddress");
        }

        this.name = name;
        this.ipv4 = ipv4;
        this.subnetMask = subnetMask;
        this.defaultGateway = defaultGateway;
        this.macAddress = macAddress;
    }

    public String name() {
        return name;
    }

    public String ipv4() {
        return ipv4;
    }

    public String subnetMask() {
        return subnetMask;
    }

    public String defaultGateway() {
        return defaultGateway;
    }

    public String macAddress() {
        return macAddress;
    }

    public long deliveredIpv4() {
        return deliveredIpv4;
    }

    public boolean duplicateIpv4() {
        return duplicateIpv4;
    }

    public int neighborCount(long nowMillis) {
        return neighbors.size(nowMillis);
    }

    public int pendingCount() {
        int total = 0;

        for (PendingResolution resolution : pending.values()) {
            total += resolution.frames.size();
        }

        return total;
    }

    public String nextHop(String destinationIp) {
        return W117Ipv4.sameSubnet(
                ipv4,
                destinationIp,
                subnetMask
        )
                ? destinationIp
                : defaultGateway;
    }

    public List<OSINetworkPacket> sendIpv4(
            OSINetworkPacket packet,
            long nowMillis
    ) {
        if (packet == null) {
            return List.of();
        }

        packet.sourceIp = ipv4;
        packet.sourceMac = macAddress;

        String nextHop =
                nextHop(packet.targetIp);

        var entry =
                neighbors.lookup(
                        nextHop,
                        nowMillis
                );

        if (entry.isPresent()) {
            OSINetworkPacket ready =
                    clonePacket(packet);

            ready.sourceMac = macAddress;
            ready.targetMac =
                    entry.get().mac();

            lastEvent =
                    "IPV4_TX nextHop="
                            + nextHop
                            + " mac="
                            + entry.get().mac();

            return List.of(ready);
        }

        if (pendingCount() >= MAX_PENDING_TOTAL) {
            pendingDrops++;
            lastEvent =
                    "DROP_PENDING_GLOBAL_LIMIT";
            return List.of();
        }

        PendingResolution resolution =
                pending.computeIfAbsent(
                        nextHop,
                        key -> new PendingResolution(
                                nextHop,
                                0,
                                nowMillis
                        )
                );

        if (resolution.frames.size()
                >= MAX_PENDING_PER_NEIGHBOR) {
            pendingDrops++;
            lastEvent =
                    "DROP_PENDING_NEIGHBOR_LIMIT nextHop="
                            + nextHop;
            return List.of();
        }

        resolution.frames.add(
                clonePacket(packet)
        );

        if (resolution.attempts == 0) {
            resolution.attempts = 1;
            resolution.nextRetryMillis =
                    nowMillis + ARP_RETRY_MILLIS;
            arpRequestsTx++;

            lastEvent =
                    "ARP_REQUEST nextHop="
                            + nextHop
                            + " attempt=1";

            return List.of(
                    W117ArpFrame.request(
                            macAddress,
                            ipv4,
                            nextHop,
                            "W1.17-ARP-" + name
                    )
            );
        }

        lastEvent =
                "ARP_PENDING nextHop="
                        + nextHop
                        + " depth="
                        + resolution.frames.size();

        return List.of();
    }

    public List<OSINetworkPacket> receive(
            OSINetworkPacket packet,
            long nowMillis
    ) {
        if (packet == null) {
            return List.of();
        }

        if (W117ArpFrame.isArp(packet)) {
            return receiveArp(
                    packet,
                    nowMillis
            );
        }

        if (ipv4.equals(packet.targetIp)) {
            deliveredIpv4++;
            lastEvent =
                    "IPV4_DELIVERED "
                            + packet.sourceIp
                            + " -> "
                            + packet.targetIp;

            if (packet.payload != null
                    && packet.payload.getBoolean(
                    "w117_echo_request"
            )) {
                OSINetworkPacket reply =
                        new OSINetworkPacket();

                reply.sourceIp = ipv4;
                reply.targetIp = packet.sourceIp;
                reply.sourcePort = packet.targetPort;
                reply.targetPort = packet.sourcePort;
                reply.ipProtocol = packet.ipProtocol;
                reply.applicationProtocol =
                        packet.applicationProtocol;
                reply.ipPacketLength =
                        Math.max(
                                64,
                                packet.ipPacketLength
                        );
                reply.ttl = 64;
                reply.isResponse = true;
                reply.sessionId =
                        packet.sessionId
                                + "-REPLY";

                CompoundTag payload =
                        packet.payload.copy();

                payload.putBoolean(
                        "w117_echo_request",
                        false
                );

                payload.putBoolean(
                        "w117_echo_reply",
                        true
                );

                if ("TCP".equalsIgnoreCase(
                        packet.applicationProtocol
                )
                        && packet.payload.getBoolean(
                        "tcp_syn"
                )) {
                    payload.putBoolean(
                            "tcp_syn",
                            true
                    );
                    payload.putBoolean(
                            "tcp_ack",
                            true
                    );
                }

                reply.payload = payload;

                return sendIpv4(
                        reply,
                        nowMillis
                );
            }
        }

        return List.of();
    }

    private List<OSINetworkPacket> receiveArp(
            OSINetworkPacket packet,
            long nowMillis
    ) {
        String senderIp =
                W117ArpFrame.senderIp(packet);

        String senderMac =
                W117ArpFrame.senderMac(packet);

        if (ipv4.equals(senderIp)
                && !macAddress.equalsIgnoreCase(senderMac)) {
            duplicateIpv4 = true;
            lastEvent =
                    "DUPLICATE_IPV4 senderMac="
                            + senderMac;
        }

        if (W117Ipv4.valid(senderIp)
                && senderMac != null
                && !senderMac.isBlank()) {
            neighbors.learn(
                    senderIp,
                    senderMac,
                    nowMillis
            );
            arpLearned++;
        }

        if (W117ArpFrame.isRequest(packet)
                && ipv4.equals(
                W117ArpFrame.targetIp(packet)
        )
                && !macAddress.equalsIgnoreCase(
                senderMac
        )) {
            arpRepliesTx++;

            lastEvent =
                    "ARP_REPLY target="
                            + senderIp;

            return List.of(
                    W117ArpFrame.reply(
                            macAddress,
                            ipv4,
                            senderMac,
                            senderIp,
                            packet.sessionId
                    )
            );
        }

        if (W117ArpFrame.isReply(packet)) {
            PendingResolution resolution =
                    pending.remove(senderIp);

            if (resolution == null) {
                lastEvent =
                        "ARP_LEARNED "
                                + senderIp
                                + " -> "
                                + senderMac;
                return List.of();
            }

            List<OSINetworkPacket> ready =
                    new ArrayList<>();

            for (OSINetworkPacket queued :
                    resolution.frames) {
                queued.sourceMac = macAddress;
                queued.targetMac = senderMac;
                ready.add(queued);
            }

            lastEvent =
                    "ARP_RESOLVED "
                            + senderIp
                            + " flushed="
                            + ready.size();

            return ready;
        }

        return List.of();
    }

    public List<OSINetworkPacket> tick(
            long nowMillis
    ) {
        neighbors.expire(nowMillis);

        List<OSINetworkPacket> output =
                new ArrayList<>();

        List<String> failed =
                new ArrayList<>();

        for (PendingResolution resolution :
                pending.values()) {
            if (nowMillis < resolution.nextRetryMillis) {
                continue;
            }

            if (resolution.attempts
                    >= MAX_ARP_ATTEMPTS) {
                arpFailures++;
                pendingDrops +=
                        resolution.frames.size();
                failed.add(
                        resolution.nextHop
                );

                lastEvent =
                        "ARP_FAILED nextHop="
                                + resolution.nextHop;

                continue;
            }

            resolution.attempts++;
            resolution.nextRetryMillis =
                    nowMillis + ARP_RETRY_MILLIS;

            arpRequestsTx++;

            output.add(
                    W117ArpFrame.request(
                            macAddress,
                            ipv4,
                            resolution.nextHop,
                            "W1.17-ARP-RETRY-"
                                    + resolution.attempts
                    )
            );

            lastEvent =
                    "ARP_RETRY nextHop="
                            + resolution.nextHop
                            + " attempt="
                            + resolution.attempts;
        }

        for (String nextHop : failed) {
            pending.remove(nextHop);
        }

        return output;
    }

    public OSINetworkPacket gratuitousArp() {
        arpRequestsTx++;
        lastEvent = "GARP_TX";

        return W117ArpFrame.gratuitous(
                macAddress,
                ipv4
        );
    }

    public void clearDynamicState() {
        neighbors.clear();
        pending.clear();
        deliveredIpv4 = 0L;
        arpRequestsTx = 0L;
        arpRepliesTx = 0L;
        arpLearned = 0L;
        arpFailures = 0L;
        pendingDrops = 0L;
        duplicateIpv4 = false;
        lastEvent = "READY";
    }

    public String status(long nowMillis) {
        return "HOST "
                + name
                + " "
                + ipv4
                + "/"
                + W117Ipv4.prefixLength(subnetMask)
                + " gw="
                + defaultGateway
                + " mac="
                + macAddress
                + " neighbors="
                + neighborCount(nowMillis)
                + " pending="
                + pendingCount()
                + " delivered="
                + deliveredIpv4
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
                + " duplicateIp="
                + duplicateIpv4
                + " last="
                + lastEvent;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putString("Name", name);
        tag.putString("Ipv4", ipv4);
        tag.putString("Mask", subnetMask);
        tag.putString("Gateway", defaultGateway);
        tag.putString("Mac", macAddress);

        return tag;
    }

    public static W117HostEndpoint load(
            CompoundTag tag
    ) {
        return new W117HostEndpoint(
                tag.getString("Name"),
                tag.getString("Ipv4"),
                tag.getString("Mask"),
                tag.getString("Gateway"),
                tag.getString("Mac")
        );
    }

    private static OSINetworkPacket clonePacket(
            OSINetworkPacket packet
    ) {
        return OSINetworkPacket.deserializeNBT(
                packet.serializeNBT().copy()
        );
    }

    private static final class PendingResolution {
        private final String nextHop;
        private final List<OSINetworkPacket> frames =
                new ArrayList<>();
        private int attempts;
        private long nextRetryMillis;

        private PendingResolution(
                String nextHop,
                int attempts,
                long nextRetryMillis
        ) {
            this.nextHop = nextHop;
            this.attempts = attempts;
            this.nextRetryMillis =
                    nextRetryMillis;
        }
    }
}
