package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

public final class SlaacEngine {
    private SlaacEngine() {
    }

    public static Ipv6Address formAddress(
            Ipv6Prefix prefix,
            String mac
    ) {
        if (prefix.length() != 64) {
            throw new IllegalArgumentException("SLAAC requires /64 prefix");
        }

        Ipv6Address interfaceId =
                Ipv6Address.linkLocalFromMac(mac);

        return interfaceId.withPrefix(prefix);
    }

    public static TentativeAddress beginDad(
            Ipv6Address address,
            long nowMicros,
            long retransTimerMicros
    ) {
        return new TentativeAddress(
                address,
                Ipv6Address.solicitedNodeMulticast(address),
                DadState.TENTATIVE,
                1,
                nowMicros + Math.max(1L, retransTimerMicros)
        );
    }

    public static TentativeAddress dadSuccess(
            TentativeAddress tentative
    ) {
        return new TentativeAddress(
                tentative.address(),
                tentative.solicitedNode(),
                DadState.PREFERRED,
                tentative.probesSent(),
                tentative.nextDeadlineMicros()
        );
    }

    public static TentativeAddress dadDuplicate(
            TentativeAddress tentative
    ) {
        return new TentativeAddress(
                tentative.address(),
                tentative.solicitedNode(),
                DadState.DUPLICATE,
                tentative.probesSent(),
                tentative.nextDeadlineMicros()
        );
    }

    public enum DadState {
        TENTATIVE,
        PREFERRED,
        DUPLICATE
    }

    public record TentativeAddress(
            Ipv6Address address,
            Ipv6Address solicitedNode,
            DadState state,
            int probesSent,
            long nextDeadlineMicros
    ) {
    }
}
