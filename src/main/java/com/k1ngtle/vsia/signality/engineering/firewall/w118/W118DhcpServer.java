package com.k1ngtle.vsia.signality.engineering.firewall.w118;

import com.k1ngtle.vsia.signality.engineering.firewall.w117.W117Ipv4;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;

import java.util.LinkedHashMap;
import java.util.Map;

public final class W118DhcpServer {
    private final Map<String, W118DhcpLease> leasesByMac =
            new LinkedHashMap<>();

    private final Map<String, String> offeredByMac =
            new LinkedHashMap<>();

    private String serverIp = "192.168.10.1";
    private String subnetMask = "255.255.255.0";
    private String gateway = "192.168.10.1";
    private String dnsServer = "192.168.10.1";
    private String poolStart = "192.168.10.100";
    private String poolEnd = "192.168.10.199";
    private long leaseSeconds = 600L;

    public void configure(
            String serverIp,
            String subnetMask,
            String gateway,
            String dnsServer,
            String poolStart,
            String poolEnd,
            long leaseSeconds
    ) {
        if (!W117Ipv4.valid(serverIp)
                || !W117Ipv4.contiguousMask(subnetMask)
                || !W117Ipv4.valid(gateway)
                || !W117Ipv4.valid(dnsServer)
                || !W117Ipv4.valid(poolStart)
                || !W117Ipv4.valid(poolEnd)) {
            throw new IllegalArgumentException("Invalid DHCP configuration");
        }

        long start =
                W117Ipv4.parse(poolStart);

        long end =
                W117Ipv4.parse(poolEnd);

        if (start > end) {
            throw new IllegalArgumentException("poolStart > poolEnd");
        }

        this.serverIp = serverIp;
        this.subnetMask = subnetMask;
        this.gateway = gateway;
        this.dnsServer = dnsServer;
        this.poolStart = poolStart;
        this.poolEnd = poolEnd;
        this.leaseSeconds =
                Math.max(
                        60L,
                        leaseSeconds
                );
    }

    public OSINetworkPacket handle(
            OSINetworkPacket packet,
            String serverMac,
            long nowMillis
    ) {
        if (!W118DhcpMessage.isDhcp(packet)) {
            return null;
        }

        expire(nowMillis);

        W118DhcpMessage.Type type =
                W118DhcpMessage.type(packet);

        if (type == W118DhcpMessage.Type.DISCOVER) {
            String mac =
                    W118DhcpMessage.clientMac(packet);

            String ip =
                    existingOrAvailable(
                            mac,
                            nowMillis
                    );

            if (ip == null) {
                return W118DhcpMessage.nak(
                        packet,
                        serverMac,
                        serverIp
                );
            }

            offeredByMac.put(
                    mac,
                    ip
            );

            return W118DhcpMessage.offer(
                    packet,
                    serverMac,
                    serverIp,
                    ip,
                    subnetMask,
                    gateway,
                    dnsServer,
                    leaseSeconds
            );
        }

        if (type == W118DhcpMessage.Type.REQUEST) {
            String mac =
                    W118DhcpMessage.clientMac(packet);

            String requested =
                    W118DhcpMessage.requestedIp(packet);

            W118DhcpLease existing =
                    leasesByMac.get(mac);

            if (existing != null
                    && existing.ipv4().equals(requested)
                    && !existing.expired(nowMillis)) {
                W118DhcpLease renewed =
                        createLease(
                                mac,
                                requested,
                                nowMillis
                        );

                leasesByMac.put(
                        mac,
                        renewed
                );

                return ack(
                        packet,
                        serverMac,
                        renewed
                );
            }

            String offered =
                    offeredByMac.get(mac);

            if (offered == null
                    || !offered.equals(requested)
                    || ipOwnedByOther(
                    requested,
                    mac,
                    nowMillis
            )) {
                return W118DhcpMessage.nak(
                        packet,
                        serverMac,
                        serverIp
                );
            }

            W118DhcpLease lease =
                    createLease(
                            mac,
                            requested,
                            nowMillis
                    );

            leasesByMac.put(
                    mac,
                    lease
            );

            offeredByMac.remove(mac);

            return ack(
                    packet,
                    serverMac,
                    lease
            );
        }

        if (type == W118DhcpMessage.Type.RELEASE) {
            leasesByMac.remove(
                    W118DhcpMessage.clientMac(packet)
            );
            offeredByMac.remove(
                    W118DhcpMessage.clientMac(packet)
            );
        }

        return null;
    }

    public int expire(long nowMillis) {
        int before =
                leasesByMac.size();

        leasesByMac.entrySet().removeIf(
                e -> e.getValue().expired(nowMillis)
        );

        offeredByMac.entrySet().removeIf(
                e -> !leasesByMac.containsKey(e.getKey())
                        && ipOwnedByOther(
                        e.getValue(),
                        e.getKey(),
                        nowMillis
                )
        );

        return before - leasesByMac.size();
    }

    public W118DhcpLease leaseForMac(
            String mac,
            long nowMillis
    ) {
        expire(nowMillis);
        return leasesByMac.get(mac);
    }

    public int leaseCount(long nowMillis) {
        expire(nowMillis);
        return leasesByMac.size();
    }

    public Map<String, W118DhcpLease> leases(
            long nowMillis
    ) {
        expire(nowMillis);
        return Map.copyOf(
                leasesByMac
        );
    }

    public void clearDynamic() {
        leasesByMac.clear();
        offeredByMac.clear();
    }

    public String status(long nowMillis) {
        return "DHCP server="
                + serverIp
                + " pool="
                + poolStart
                + "-"
                + poolEnd
                + " leases="
                + leaseCount(nowMillis)
                + " leaseSeconds="
                + leaseSeconds;
    }

    private OSINetworkPacket ack(
            OSINetworkPacket request,
            String serverMac,
            W118DhcpLease lease
    ) {
        return W118DhcpMessage.ack(
                request,
                serverMac,
                serverIp,
                lease.clientMac(),
                lease.ipv4(),
                subnetMask,
                gateway,
                dnsServer,
                leaseSeconds
        );
    }

    private W118DhcpLease createLease(
            String mac,
            String ip,
            long nowMillis
    ) {
        long leaseMillis =
                leaseSeconds * 1000L;

        return new W118DhcpLease(
                mac,
                ip,
                nowMillis,
                nowMillis + leaseMillis,
                nowMillis + leaseMillis / 2L,
                nowMillis + (leaseMillis * 7L) / 8L
        );
    }

    private String existingOrAvailable(
            String mac,
            long nowMillis
    ) {
        W118DhcpLease existing =
                leasesByMac.get(mac);

        if (existing != null
                && !existing.expired(nowMillis)) {
            return existing.ipv4();
        }

        String offered =
                offeredByMac.get(mac);

        if (offered != null
                && !ipOwnedByOther(
                offered,
                mac,
                nowMillis
        )) {
            return offered;
        }

        long start =
                W117Ipv4.parse(poolStart);

        long end =
                W117Ipv4.parse(poolEnd);

        for (long candidate = start;
             candidate <= end;
             candidate++) {
            String ip =
                    formatIpv4(candidate);

            if (!ipOwnedByOther(
                    ip,
                    mac,
                    nowMillis
            )
                    && !offeredByMac.containsValue(ip)) {
                return ip;
            }
        }

        return null;
    }

    private boolean ipOwnedByOther(
            String ip,
            String mac,
            long nowMillis
    ) {
        expireLeasesOnly(nowMillis);

        for (W118DhcpLease lease :
                leasesByMac.values()) {
            if (lease.ipv4().equals(ip)
                    && !lease.clientMac().equalsIgnoreCase(mac)) {
                return true;
            }
        }

        return false;
    }

    private void expireLeasesOnly(long nowMillis) {
        leasesByMac.entrySet().removeIf(
                e -> e.getValue().expired(nowMillis)
        );
    }

    private static String formatIpv4(long value) {
        long v =
                value & 0xFFFFFFFFL;

        return ((v >> 24) & 0xFF)
                + "."
                + ((v >> 16) & 0xFF)
                + "."
                + ((v >> 8) & 0xFF)
                + "."
                + (v & 0xFF);
    }
}
