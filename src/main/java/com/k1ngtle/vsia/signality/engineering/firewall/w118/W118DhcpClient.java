package com.k1ngtle.vsia.signality.engineering.firewall.w118;

import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;

import java.util.List;
import java.util.Random;

public final class W118DhcpClient {
    public enum State {
        INIT,
        SELECTING,
        REQUESTING,
        BOUND,
        RENEWING,
        REBINDING
    }

    public record Configuration(
            String ipv4,
            String subnetMask,
            String gateway,
            String dnsServer,
            String serverId,
            long leaseStartMillis,
            long leaseEndMillis,
            long t1Millis,
            long t2Millis
    ) {
        public boolean expired(long nowMillis) {
            return nowMillis >= leaseEndMillis;
        }
    }

    private State state = State.INIT;
    private Configuration configuration;
    private int xid = 0;
    private int attempts = 0;
    private long nextRetryMillis = 0L;
    private String offeredIp = "";
    private String serverId = "";
    private String lastEvent = "INIT";

    public State state() {
        return state;
    }

    public Configuration configuration() {
        return configuration;
    }

    public boolean bound() {
        return state == State.BOUND
                || state == State.RENEWING
                || state == State.REBINDING;
    }

    public OSINetworkPacket start(
            String mac,
            long nowMillis
    ) {
        xid =
                (int) (
                        (nowMillis
                                ^ mac.hashCode()
                                ^ 0x118D4C50L)
                                & 0x7FFFFFFF
                );

        if (xid == 0) {
            xid = 1;
        }

        state = State.SELECTING;
        configuration = null;
        attempts = 1;
        nextRetryMillis = nowMillis + 4_000L;
        offeredIp = "";
        serverId = "";
        lastEvent = "DISCOVER_TX";

        return W118DhcpMessage.discover(
                mac,
                xid
        );
    }

    public List<OSINetworkPacket> receive(
            OSINetworkPacket packet,
            String mac,
            long nowMillis
    ) {
        if (!W118DhcpMessage.isDhcp(packet)
                || W118DhcpMessage.xid(packet) != xid
                || !mac.equalsIgnoreCase(
                W118DhcpMessage.clientMac(packet)
        )) {
            return List.of();
        }

        W118DhcpMessage.Type type =
                W118DhcpMessage.type(packet);

        if (type == W118DhcpMessage.Type.OFFER
                && state == State.SELECTING) {
            offeredIp =
                    W118DhcpMessage.yourIp(packet);
            serverId =
                    W118DhcpMessage.serverId(packet);

            state = State.REQUESTING;
            attempts = 1;
            nextRetryMillis = nowMillis + 4_000L;
            lastEvent = "OFFER_RX_REQUEST_TX";

            return List.of(
                    W118DhcpMessage.request(
                            mac,
                            xid,
                            offeredIp,
                            serverId,
                            true
                    )
            );
        }

        if (type == W118DhcpMessage.Type.ACK
                && (state == State.REQUESTING
                || state == State.RENEWING
                || state == State.REBINDING)) {
            long leaseMillis =
                    W118DhcpMessage.leaseSeconds(packet)
                            * 1000L;

            long t1 =
                    W118DhcpMessage.t1Seconds(packet)
                            * 1000L;

            long t2 =
                    W118DhcpMessage.t2Seconds(packet)
                            * 1000L;

            configuration =
                    new Configuration(
                            W118DhcpMessage.yourIp(packet),
                            W118DhcpMessage.subnetMask(packet),
                            W118DhcpMessage.gateway(packet),
                            W118DhcpMessage.dnsServer(packet),
                            W118DhcpMessage.serverId(packet),
                            nowMillis,
                            nowMillis + leaseMillis,
                            nowMillis + t1,
                            nowMillis + t2
                    );

            state = State.BOUND;
            attempts = 0;
            nextRetryMillis = 0L;
            lastEvent = "ACK_RX_BOUND";

            return List.of();
        }

        if (type == W118DhcpMessage.Type.NAK) {
            state = State.INIT;
            configuration = null;
            attempts = 0;
            nextRetryMillis = 0L;
            lastEvent = "NAK_RX_INIT";
        }

        return List.of();
    }

    public List<OSINetworkPacket> tick(
            String mac,
            long nowMillis
    ) {
        if (configuration != null
                && configuration.expired(nowMillis)) {
            state = State.INIT;
            configuration = null;
            attempts = 0;
            nextRetryMillis = 0L;
            lastEvent = "LEASE_EXPIRED";
            return List.of();
        }

        if (state == State.BOUND
                && configuration != null
                && nowMillis >= configuration.t1Millis()) {
            state = State.RENEWING;
            xid = nextXid();
            attempts = 1;
            nextRetryMillis = nowMillis + 4_000L;
            lastEvent = "T1_RENEW";

            OSINetworkPacket renew =
                    W118DhcpMessage.request(
                            mac,
                            xid,
                            configuration.ipv4(),
                            configuration.serverId(),
                            false
                    );

            renew.sourceIp =
                    configuration.ipv4();
            renew.targetIp =
                    configuration.serverId();

            return List.of(renew);
        }

        if (state == State.RENEWING
                && configuration != null
                && nowMillis >= configuration.t2Millis()) {
            state = State.REBINDING;
            xid = nextXid();
            attempts = 1;
            nextRetryMillis = nowMillis + 4_000L;
            lastEvent = "T2_REBIND";

            OSINetworkPacket rebind =
                    W118DhcpMessage.request(
                            mac,
                            xid,
                            configuration.ipv4(),
                            configuration.serverId(),
                            true
                    );

            rebind.sourceIp =
                    configuration.ipv4();

            return List.of(rebind);
        }

        if ((state == State.SELECTING
                || state == State.REQUESTING
                || state == State.RENEWING
                || state == State.REBINDING)
                && nowMillis >= nextRetryMillis) {
            if (attempts >= 4) {
                state = State.INIT;
                attempts = 0;
                lastEvent = "RETRY_EXHAUSTED";
                return List.of();
            }

            attempts++;
            nextRetryMillis =
                    nowMillis + (4_000L << Math.min(3, attempts - 1));

            if (state == State.SELECTING) {
                lastEvent = "DISCOVER_RETRY_" + attempts;
                return List.of(
                        W118DhcpMessage.discover(
                                mac,
                                xid
                        )
                );
            }

            String requested =
                    configuration != null
                            ? configuration.ipv4()
                            : offeredIp;

            String server =
                    configuration != null
                            ? configuration.serverId()
                            : serverId;

            boolean broadcast =
                    state == State.REQUESTING
                            || state == State.REBINDING;

            lastEvent =
                    "REQUEST_RETRY_" + attempts;

            return List.of(
                    W118DhcpMessage.request(
                            mac,
                            xid,
                            requested,
                            server,
                            broadcast
                    )
            );
        }

        return List.of();
    }

    public void clear() {
        state = State.INIT;
        configuration = null;
        xid = 0;
        attempts = 0;
        nextRetryMillis = 0L;
        offeredIp = "";
        serverId = "";
        lastEvent = "INIT";
    }

    public String status() {
        String address =
                configuration == null
                        ? "unassigned"
                        : configuration.ipv4();

        return "dhcpState="
                + state
                + " ip="
                + address
                + " xid="
                + xid
                + " attempts="
                + attempts
                + " last="
                + lastEvent;
    }

    private int nextXid() {
        xid = (xid + 0x118 + 1) & 0x7FFFFFFF;
        if (xid == 0) {
            xid = 1;
        }
        return xid;
    }
}
