package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

public record Ipv6AnalyzerSnapshot(
        Ipv6Address linkLocal,
        Ipv6Address global,
        Ipv6Prefix prefix,
        Ipv6Address defaultRouter,
        int hopLimit,
        int interfaceMtu,
        int neighbors,
        int routes,
        String ndState,
        String dadState,
        String protocolStatus
) {
    public String render() {
        return "IPv6 "
                + (global == null ? "none" : global)
                + " | link-local "
                + (linkLocal == null ? "none" : linkLocal)
                + " | prefix "
                + (prefix == null ? "none" : prefix)
                + " | router "
                + (defaultRouter == null ? "none" : defaultRouter)
                + " | hopLimit "
                + hopLimit
                + " | MTU "
                + interfaceMtu
                + " | ND "
                + ndState
                + " | DAD "
                + dadState
                + " | neighbors "
                + neighbors
                + " | routes "
                + routes
                + " | "
                + protocolStatus;
    }
}
