package com.k1ngtle.vsia.signality.engineering.firewall;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public final class StatefulFirewallEngine {
    private final ConntrackTable conntrack;
    private final Nat44Table nat44;
    private final FragmentAssociationTable fragments;
    private final List<FirewallRule> rules = new ArrayList<>();

    private FirewallAction defaultIpv4Policy = FirewallAction.DROP;
    private FirewallAction defaultIpv6Policy = FirewallAction.DROP;
    private boolean nat44Enabled;
    private String nat44PublicIp = "";

    public StatefulFirewallEngine() {
        this(
                new ConntrackTable(),
                new Nat44Table(),
                new FragmentAssociationTable()
        );
    }

    public StatefulFirewallEngine(
            ConntrackTable conntrack,
            Nat44Table nat44,
            FragmentAssociationTable fragments
    ) {
        this.conntrack = conntrack;
        this.nat44 = nat44;
        this.fragments = fragments;
    }

    public static StatefulFirewallEngine permissiveCompatibilityEngine() {
        StatefulFirewallEngine engine = new StatefulFirewallEngine();
        engine.defaultIpv4Policy = FirewallAction.ACCEPT;
        engine.defaultIpv6Policy = FirewallAction.ACCEPT;

        engine.addRule(
                new FirewallRule(
                        10,
                        "ALLOW-ESTABLISHED-RELATED",
                        FirewallAction.ACCEPT,
                        null,
                        "ANY",
                        "ANY",
                        "ANY",
                        -1,
                        -1,
                        "ANY",
                        "ANY",
                        EnumSet.of(
                                ConntrackState.ESTABLISHED,
                                ConntrackState.RELATED
                        )
                )
        );

        return engine;
    }

    public void addRule(FirewallRule rule) {
        rules.add(rule);
        rules.sort(Comparator.comparingInt(FirewallRule::sequence));
    }

    public void clearRules() {
        rules.clear();
    }

    public void setDefaultPolicy(
            IpFamily family,
            FirewallAction action
    ) {
        if (family == IpFamily.IPV4) {
            defaultIpv4Policy = action;
        } else {
            defaultIpv6Policy = action;
        }
    }

    public void enableNat44(String publicIpv4) {
        if (IpPrefixMatcher.family(publicIpv4) != IpFamily.IPV4) {
            throw new IllegalArgumentException("NAT44 public address must be IPv4");
        }

        nat44Enabled = true;
        nat44PublicIp = publicIpv4;
    }

    public void disableNat44() {
        nat44Enabled = false;
        nat44PublicIp = "";
    }

    public FirewallDecision inspect(
            FirewallPacketView packet,
            int packetBytes,
            long nowMillis
    ) {
        FragmentAssociationTable.Association association = null;

        if (packet.nonInitialFragment()) {
            association = fragments.lookup(packet, nowMillis);

            if (association == null) {
                return new FirewallDecision(
                        FirewallAction.DROP,
                        ConntrackState.INVALID,
                        "",
                        "UNASSOCIATED_NONINITIAL_FRAGMENT",
                        null,
                        false
                );
            }
        }

        Nat44Mapping inboundNat = null;

        if (nat44Enabled
                && packet.family() == IpFamily.IPV4
                && packet.ingressInterface().equalsIgnoreCase("WAN")
                && !packet.nonInitialFragment()) {
            inboundNat = nat44.findInbound(packet, nowMillis);
        }

        ConntrackTable.Lookup lookup =
                association != null
                        ? new ConntrackTable.Lookup(
                        association.state(),
                        null,
                        false
                )
                        : conntrack.classify(packet, nowMillis);

        ConntrackState state = lookup.state();

        FirewallRule matchedRule = null;

        for (FirewallRule rule : rules) {
            if (rule.matches(packet, state)) {
                matchedRule = rule;
                break;
            }
        }

        FirewallAction action =
                matchedRule != null
                        ? matchedRule.action()
                        : defaultPolicy(packet.family());

        if (matchedRule != null) {
            matchedRule.hit(packetBytes);
        }

        if (action == FirewallAction.DROP
                || action == FirewallAction.REJECT) {
            return new FirewallDecision(
                    action,
                    state,
                    matchedRule == null ? "" : matchedRule.name(),
                    matchedRule == null
                            ? "DEFAULT_POLICY"
                            : "RULE",
                    inboundNat,
                    inboundNat != null
            );
        }

        if (state == ConntrackState.NEW
                && !packet.nonInitialFragment()) {
            conntrack.create(packet, nowMillis);
        }

        Nat44Mapping outboundNat = null;

        if (nat44Enabled
                && packet.family() == IpFamily.IPV4
                && packet.ingressInterface().equalsIgnoreCase("LAN")
                && !packet.nonInitialFragment()) {
            outboundNat =
                    nat44.allocatePat(
                            packet,
                            nat44PublicIp,
                            nowMillis
                    );
        }

        Nat44Mapping activeNat =
                inboundNat != null
                        ? inboundNat
                        : outboundNat;

        if (packet.fragmented()
                && packet.fragmentOffset() == 0) {
            fragments.remember(
                    packet,
                    state == ConntrackState.NEW
                            ? ConntrackState.ESTABLISHED
                            : state,
                    activeNat,
                    nowMillis
            );
        }

        return new FirewallDecision(
                action,
                state,
                matchedRule == null ? "" : matchedRule.name(),
                matchedRule == null
                        ? "DEFAULT_POLICY"
                        : "RULE",
                activeNat,
                inboundNat != null
        );
    }

    public int expire(long nowMillis) {
        return conntrack.expire(nowMillis)
                + nat44.expire(nowMillis)
                + fragments.expire(nowMillis);
    }

    private FirewallAction defaultPolicy(IpFamily family) {
        return family == IpFamily.IPV4
                ? defaultIpv4Policy
                : defaultIpv6Policy;
    }

    public ConntrackTable conntrack() {
        return conntrack;
    }

    public Nat44Table nat44() {
        return nat44;
    }

    public List<FirewallRule> rules() {
        return List.copyOf(rules);
    }
}
