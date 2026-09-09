package com.k1ngtle.vsia.signality.engineering.wifi.security.protocol;

public record WifiSecurityCapabilities(
        boolean authenticationRequired,
        boolean fourWayHandshake,
        boolean pairwiseKey,
        boolean dataProtection,
        boolean preSharedKey
) {
}
