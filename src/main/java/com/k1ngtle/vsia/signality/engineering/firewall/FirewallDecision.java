package com.k1ngtle.vsia.signality.engineering.firewall;

public record FirewallDecision(
        FirewallAction action,
        ConntrackState state,
        String ruleName,
        String reason,
        Nat44Mapping natMapping,
        boolean reverseNat
) {
    public boolean allowed() {
        return action == FirewallAction.ACCEPT
                || action == FirewallAction.LOG;
    }
}
