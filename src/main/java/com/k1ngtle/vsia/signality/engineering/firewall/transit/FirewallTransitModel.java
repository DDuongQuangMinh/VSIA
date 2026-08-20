package com.k1ngtle.vsia.signality.engineering.firewall.transit;

public final class FirewallTransitModel {
    private FirewallTransitModel() {
    }

    public enum Port {
        LAN,
        WAN,
        NONE
    }

    public static Port ingressFor(
            String ingressPos,
            String lanPos,
            String wanPos
    ) {
        if (ingressPos == null) return Port.NONE;
        if (ingressPos.equals(lanPos)) return Port.LAN;
        if (ingressPos.equals(wanPos)) return Port.WAN;
        return Port.NONE;
    }

    public static Port egressForInterface(
            String interfaceName
    ) {
        if (interfaceName == null) return Port.NONE;

        if (interfaceName.equalsIgnoreCase("GigabitEthernet1/1")
                || interfaceName.equalsIgnoreCase("Gi1/1")
                || interfaceName.equalsIgnoreCase("inside")) {
            return Port.LAN;
        }

        if (interfaceName.equalsIgnoreCase("GigabitEthernet1/2")
                || interfaceName.equalsIgnoreCase("Gi1/2")
                || interfaceName.equalsIgnoreCase("outside")) {
            return Port.WAN;
        }

        return Port.NONE;
    }

    public static boolean shouldForward(
            Port ingress,
            Port egress,
            boolean ingressUp,
            boolean egressUp,
            boolean booted
    ) {
        return booted
                && ingress != Port.NONE
                && egress != Port.NONE
                && ingress != egress
                && ingressUp
                && egressUp;
    }

    public static int forwardedTtl(
            int ttl
    ) {
        if (ttl <= 1) {
            return -1;
        }

        return ttl - 1;
    }

    public static boolean validTopology(
            String lanPos,
            String wanPos
    ) {
        return lanPos != null
                && wanPos != null
                && !lanPos.equals(wanPos);
    }
}
