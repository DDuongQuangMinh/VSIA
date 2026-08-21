package com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119;

public record W119BridgeDecision(
        W119BridgeAction action,
        String reason,
        String sourceMac,
        String destinationMac,
        boolean broadcastOrMulticast
) {
    public static W119BridgeDecision of(
            W119BridgeAction action,
            String reason,
            String sourceMac,
            String destinationMac,
            boolean broadcastOrMulticast
    ) {
        return new W119BridgeDecision(
                action,
                reason,
                sourceMac,
                destinationMac,
                broadcastOrMulticast
        );
    }
}
