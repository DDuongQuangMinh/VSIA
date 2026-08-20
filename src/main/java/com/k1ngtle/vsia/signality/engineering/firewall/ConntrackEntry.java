package com.k1ngtle.vsia.signality.engineering.firewall;

public final class ConntrackEntry {
    private final FirewallFlowKey original;
    private final FirewallFlowKey reply;
    private ConntrackState state;
    private long lastSeenMillis;
    private long expiresAtMillis;
    private TcpState tcpState;

    public ConntrackEntry(
            FirewallFlowKey original,
            long nowMillis,
            long timeoutMillis
    ) {
        this.original = original;
        this.reply = original.reverse();
        this.state = ConntrackState.NEW;
        this.lastSeenMillis = nowMillis;
        this.expiresAtMillis = nowMillis + timeoutMillis;
        this.tcpState = TcpState.NONE;
    }

    public boolean matches(FirewallFlowKey key) {
        return original.equals(key) || reply.equals(key);
    }

    public boolean replyDirection(FirewallFlowKey key) {
        return reply.equals(key);
    }

    public void observe(
            FirewallPacketView packet,
            boolean replyDirection,
            long nowMillis
    ) {
        lastSeenMillis = nowMillis;

        if (packet.protocol().equals("TCP")) {
            observeTcp(packet, replyDirection);
            expiresAtMillis = nowMillis + tcpTimeout(tcpState);
        } else if (packet.protocol().equals("UDP")) {
            if (replyDirection) state = ConntrackState.ESTABLISHED;
            expiresAtMillis = nowMillis + (state == ConntrackState.ESTABLISHED ? 180_000L : 30_000L);
        } else {
            if (replyDirection) state = ConntrackState.ESTABLISHED;
            expiresAtMillis = nowMillis + 30_000L;
        }
    }

    private void observeTcp(
            FirewallPacketView packet,
            boolean replyDirection
    ) {
        if (packet.tcpRst()) {
            tcpState = TcpState.CLOSED;
            state = ConntrackState.ESTABLISHED;
            return;
        }

        if (tcpState == TcpState.NONE
                && packet.tcpSyn()
                && !packet.tcpAck()
                && !replyDirection) {
            tcpState = TcpState.SYN_SENT;
            state = ConntrackState.NEW;
            return;
        }

        if (tcpState == TcpState.SYN_SENT
                && packet.tcpSyn()
                && packet.tcpAck()
                && replyDirection) {
            tcpState = TcpState.SYN_RECV;
            state = ConntrackState.ESTABLISHED;
            return;
        }

        if ((tcpState == TcpState.SYN_RECV || tcpState == TcpState.SYN_SENT)
                && packet.tcpAck()
                && !packet.tcpSyn()) {
            tcpState = TcpState.ESTABLISHED;
            state = ConntrackState.ESTABLISHED;
            return;
        }

        if (packet.tcpFin()) {
            tcpState = TcpState.FIN_WAIT;
            state = ConntrackState.ESTABLISHED;
            return;
        }

        if (tcpState == TcpState.ESTABLISHED
                || replyDirection) {
            state = ConntrackState.ESTABLISHED;
        }
    }

    private static long tcpTimeout(TcpState state) {
        return switch (state) {
            case SYN_SENT, SYN_RECV -> 60_000L;
            case ESTABLISHED -> 300_000L;
            case FIN_WAIT -> 30_000L;
            case CLOSED -> 5_000L;
            default -> 30_000L;
        };
    }

    public boolean expired(long nowMillis) {
        return nowMillis >= expiresAtMillis;
    }

    public FirewallFlowKey original() { return original; }
    public FirewallFlowKey reply() { return reply; }
    public ConntrackState state() { return state; }
    public long lastSeenMillis() { return lastSeenMillis; }
    public TcpState tcpState() { return tcpState; }

    public enum TcpState {
        NONE,
        SYN_SENT,
        SYN_RECV,
        ESTABLISHED,
        FIN_WAIT,
        CLOSED
    }
}
