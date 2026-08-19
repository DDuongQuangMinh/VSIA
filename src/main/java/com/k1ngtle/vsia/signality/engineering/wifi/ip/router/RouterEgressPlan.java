package com.k1ngtle.vsia.signality.engineering.wifi.ip.router;

public record RouterEgressPlan(
        String sourceMac,
        String destinationMac,
        String sourceIp,
        String destinationIp,
        int ttl,
        String egressInterface,
        String nextHopIp
) {
    public static RouterEgressPlan from(
            RouterForwardDecision decision,
            String routerMac
    ) {
        if (decision == null
                || decision.action()
                != RouterForwardAction.FORWARD) {
            throw new IllegalArgumentException(
                    "FORWARD decision required"
            );
        }

        return new RouterEgressPlan(
                routerMac,
                decision.nextHopMac(),
                decision.sourceIp(),
                decision.destinationIp(),
                decision.outgoingTtl(),
                decision.egressInterface(),
                decision.nextHopIp()
        );
    }
}
