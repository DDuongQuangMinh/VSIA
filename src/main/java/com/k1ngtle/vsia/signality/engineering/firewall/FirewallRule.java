package com.k1ngtle.vsia.signality.engineering.firewall;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public final class FirewallRule {
    private final int sequence;
    private final String name;
    private final FirewallAction action;
    private final IpFamily family;
    private final String protocol;
    private final String sourcePrefix;
    private final String destinationPrefix;
    private final int sourcePort;
    private final int destinationPort;
    private final String ingressInterface;
    private final String egressInterface;
    private final Set<ConntrackState> states;
    private long packets;
    private long bytes;

    public FirewallRule(
            int sequence,
            String name,
            FirewallAction action,
            IpFamily family,
            String protocol,
            String sourcePrefix,
            String destinationPrefix,
            int sourcePort,
            int destinationPort,
            String ingressInterface,
            String egressInterface,
            Set<ConntrackState> states
    ) {
        if (sequence < 0) throw new IllegalArgumentException("sequence");
        this.sequence = sequence;
        this.name = name == null ? "" : name;
        this.action = action == null ? FirewallAction.DROP : action;
        this.family = family;
        this.protocol = protocol == null ? "ANY" : protocol.trim().toUpperCase(Locale.ROOT);
        this.sourcePrefix = sourcePrefix == null ? "ANY" : sourcePrefix.trim();
        this.destinationPrefix = destinationPrefix == null ? "ANY" : destinationPrefix.trim();
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.ingressInterface = ingressInterface == null ? "ANY" : ingressInterface;
        this.egressInterface = egressInterface == null ? "ANY" : egressInterface;
        this.states = states == null || states.isEmpty()
                ? EnumSet.allOf(ConntrackState.class)
                : EnumSet.copyOf(states);
    }

    public boolean matches(FirewallPacketView packet, ConntrackState state) {
        if (family != null && packet.family() != family) return false;
        if (!protocol.equals("ANY") && !protocol.equals(packet.protocol())) return false;
        if (!IpPrefixMatcher.matches(packet.sourceIp(), sourcePrefix)) return false;
        if (!IpPrefixMatcher.matches(packet.destinationIp(), destinationPrefix)) return false;
        if (sourcePort >= 0 && packet.sourcePort() != sourcePort) return false;
        if (destinationPort >= 0 && packet.destinationPort() != destinationPort) return false;
        if (!ingressInterface.equalsIgnoreCase("ANY")
                && !ingressInterface.equalsIgnoreCase(packet.ingressInterface())) return false;
        if (!egressInterface.equalsIgnoreCase("ANY")
                && !egressInterface.equalsIgnoreCase(packet.egressInterface())) return false;
        return states.contains(state);
    }

    public void hit(int packetBytes) {
        packets++;
        bytes += Math.max(0, packetBytes);
    }

    public int sequence() { return sequence; }
    public String name() { return name; }
    public FirewallAction action() { return action; }
    public long packets() { return packets; }
    public long bytes() { return bytes; }
}
