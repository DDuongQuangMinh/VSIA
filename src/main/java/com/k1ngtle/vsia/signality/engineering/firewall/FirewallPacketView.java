package com.k1ngtle.vsia.signality.engineering.firewall;

import java.util.Locale;

public record FirewallPacketView(
        IpFamily family,
        String protocol,
        String sourceIp,
        int sourcePort,
        String destinationIp,
        int destinationPort,
        String ingressInterface,
        String egressInterface,
        boolean tcpSyn,
        boolean tcpAck,
        boolean tcpFin,
        boolean tcpRst,
        boolean icmpError,
        FirewallFlowKey relatedFlow,
        int fragmentIdentification,
        int fragmentOffset,
        boolean moreFragments
) {
    public FirewallPacketView {
        if (family == null) throw new IllegalArgumentException("family");
        protocol = protocol == null ? "IP" : protocol.trim().toUpperCase(Locale.ROOT);
        sourceIp = require(sourceIp, "sourceIp");
        destinationIp = require(destinationIp, "destinationIp");
        ingressInterface = ingressInterface == null ? "" : ingressInterface;
        egressInterface = egressInterface == null ? "" : egressInterface;
        if (sourcePort < 0 || sourcePort > 65535) throw new IllegalArgumentException("sourcePort");
        if (destinationPort < 0 || destinationPort > 65535) throw new IllegalArgumentException("destinationPort");
        if (fragmentIdentification < 0) throw new IllegalArgumentException("fragmentIdentification");
        if (fragmentOffset < 0) throw new IllegalArgumentException("fragmentOffset");
    }

    public FirewallFlowKey flowKey() {
        return new FirewallFlowKey(
                family,
                protocol,
                sourceIp,
                sourcePort,
                destinationIp,
                destinationPort
        );
    }

    public boolean fragmented() {
        return fragmentOffset > 0 || moreFragments;
    }

    public boolean nonInitialFragment() {
        return fragmentOffset > 0;
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name);
        return value.trim();
    }
}
