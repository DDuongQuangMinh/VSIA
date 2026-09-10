package com.k1ngtle.vsia.signality.engineering.wifi.integration.w127;

import com.k1ngtle.vsia.signality.engineering.firewall.w117.W117ArpFrame;
import com.k1ngtle.vsia.signality.engineering.firewall.w117.W117HostEndpoint;
import com.k1ngtle.vsia.signality.engineering.firewall.w118.W118DhcpMessage;
import com.k1ngtle.vsia.signality.engineering.firewall.w118.W118DhcpServer;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;

import java.util.List;

public final class W127UnitTestSuite {
    public record Result(
            String name,
            boolean passed,
            String detail
    ) {
    }

    private W127UnitTestSuite() {
    }

    public static List<Result> runAll() {
        return List.of(
                stageLabel(),
                wildcardRule(),
                exactRule(),
                wrongKindRejected(),
                oneShotConsumption(),
                persistentRule(),
                exponentialTimeout(),
                delayBound(),
                deepCopyIsolation(),
                rrsigMutation(),
                expiryMutation(),
                dnskeyMutation(),
                forcedSerialMutation(),
                servfailMutation(),
                arpFailureRecovery(),
                dhcpPoolExhaustionRecovery()
        );
    }

    private static Result stageLabel() {
        return new Result(
                "w127-stage-label",
                true,
                "W1.27 — Stress / Failure / Edge Cases"
        );
    }

    private static Result wildcardRule() {
        W127FaultRule rule =
                rule(
                        W127FaultAction.DROP,
                        "",
                        "",
                        "",
                        "",
                        "",
                        null,
                        null,
                        1
                );

        return result(
                "w127-fault-wildcard-match",
                rule.matches(rootReferral()),
                "Blank/* selectors match any packet field"
        );
    }

    private static Result exactRule() {
        W127FaultRule rule =
                rule(
                        W127FaultAction.DROP,
                        "192.168.1.10",
                        "192.168.1.2",
                        "DNS",
                        "ROOT_REFERRAL",
                        "",
                        null,
                        true,
                        1
                );

        return result(
                "w127-fault-exact-match",
                rule.matches(rootReferral()),
                "Root referral source/destination/protocol/kind/response matched"
        );
    }

    private static Result wrongKindRejected() {
        W127FaultRule rule =
                rule(
                        W127FaultAction.DROP,
                        "192.168.1.10",
                        "192.168.1.2",
                        "DNS",
                        "AUTH_ANSWER",
                        "",
                        null,
                        true,
                        1
                );

        return result(
                "w127-fault-wrong-kind-rejected",
                !rule.matches(rootReferral()),
                "AUTH_ANSWER rule does not capture ROOT_REFERRAL"
        );
    }

    private static Result oneShotConsumption() {
        W127FaultRule rule =
                rule(
                        W127FaultAction.DROP,
                        "",
                        "",
                        "",
                        "",
                        "",
                        null,
                        null,
                        1
                );

        boolean first =
                rule.consume();

        boolean exhausted =
                rule.exhausted();

        boolean second =
                rule.consume();

        return result(
                "w127-one-shot-fault",
                first
                        && exhausted
                        && !second,
                "One-shot fault is consumed exactly once"
        );
    }

    private static Result persistentRule() {
        W127FaultRule rule =
                rule(
                        W127FaultAction.DROP,
                        "",
                        "",
                        "",
                        "",
                        "",
                        null,
                        null,
                        -1
                );

        boolean passed =
                rule.consume()
                        && rule.consume()
                        && !rule.exhausted()
                        && rule.remainingMatches() < 0;

        return result(
                "w127-persistent-outage-rule",
                passed,
                "Negative match count models an outage until explicitly cleared"
        );
    }

    private static Result exponentialTimeout() {
        boolean passed =
                W127ResiliencePolicy.dnsTimeoutTicks(0) == 60L
                        && W127ResiliencePolicy.dnsTimeoutTicks(1) == 120L
                        && W127ResiliencePolicy.dnsTimeoutTicks(2) == 240L
                        && W127ResiliencePolicy.dnsTimeoutTicks(9) == 240L;

        return result(
                "w127-bounded-backoff-policy",
                passed,
                "DNS resilience timing model: 60 -> 120 -> 240 tick cap"
        );
    }

    private static Result delayBound() {
        boolean passed =
                W127ResiliencePolicy.boundedDelay(-5L) == 1L
                        && W127ResiliencePolicy.boundedDelay(20L) == 20L
                        && W127ResiliencePolicy.boundedDelay(999L) == 200L;

        return result(
                "w127-delay-bounds",
                passed,
                "Fault delay remains bounded from 1 to 200 ticks"
        );
    }

    private static Result deepCopyIsolation() {
        OSINetworkPacket original =
                rootReferral();

        OSINetworkPacket copy =
                W127PacketMutator.copy(
                        original
                );

        copy.payload.putString(
                "pdns_kind",
                "CHANGED"
        );

        return result(
                "w127-packet-copy-isolation",
                "ROOT_REFERRAL".equals(
                        original.payload.getString(
                                "pdns_kind"
                        )
                ),
                "Mutation of a copied fault packet does not alter the original"
        );
    }

    private static Result rrsigMutation() {
        OSINetworkPacket original =
                rootReferral();

        original.payload.putString(
                "dnssec_rrsig",
                "AAAA"
        );

        OSINetworkPacket changed =
                W127PacketMutator.corruptRrsig(
                        original
                );

        return result(
                "w127-rrsig-corruption",
                !"AAAA".equals(
                        changed.payload.getString(
                                "dnssec_rrsig"
                        )
                )
                        && "AAAA".equals(
                        original.payload.getString(
                                "dnssec_rrsig"
                        )
                ),
                "RRSIG corruption is isolated to the injected copy"
        );
    }

    private static Result expiryMutation() {
        long now = 1_800_000_000L;

        OSINetworkPacket changed =
                W127PacketMutator.expireRrsig(
                        rootReferral(),
                        now
                );

        return result(
                "w127-expired-signature",
                changed.payload.getLong(
                        "dnssec_expiration"
                ) < now,
                "Injected signature expiration is in the past"
        );
    }

    private static Result dnskeyMutation() {
        OSINetworkPacket packet =
                rootReferral();

        packet.payload.putString(
                "dnssec_public_raw",
                "AAAA"
        );

        OSINetworkPacket changed =
                W127PacketMutator.corruptDnskey(
                        packet
                );

        return result(
                "w127-dnskey-ds-mismatch",
                !"AAAA".equals(
                        changed.payload.getString(
                                "dnssec_public_raw"
                        )
                ),
                "DNSKEY material can be deterministically altered to force DS mismatch"
        );
    }

    private static Result forcedSerialMutation() {
        OSINetworkPacket packet =
                rootReferral();

        packet.payload.putLong(
                "from_serial",
                2026091001L
        );

        OSINetworkPacket changed =
                W127PacketMutator.forceTransferSerial(
                        packet,
                        1L
                );

        return result(
                "w127-stale-ixfr-serial",
                changed.payload.getLong(
                        "from_serial"
                ) == 1L,
                "IXFR request serial can be forced outside retained history"
        );
    }

    private static Result servfailMutation() {
        OSINetworkPacket changed =
                W127PacketMutator.servfail(
                        rootReferral()
                );

        return result(
                "w127-upstream-servfail",
                changed.payload.getInt(
                        "rcode"
                ) == 2,
                "Injected DNS response uses RCODE=2 SERVFAIL"
        );
    }

    private static Result arpFailureRecovery() {
        W117HostEndpoint host =
                new W117HostEndpoint(
                        "w127-host",
                        "192.168.1.2",
                        "255.255.255.0",
                        "192.168.1.1",
                        "02:00:00:00:00:02"
                );

        OSINetworkPacket payload =
                new OSINetworkPacket();

        payload.targetIp =
                "192.168.1.10";

        payload.applicationProtocol =
                "DNS";

        payload.targetPort =
                53;

        List<OSINetworkPacket> first =
                host.sendIpv4(
                        payload,
                        0L
                );

        List<OSINetworkPacket> retryTwo =
                host.tick(
                        1001L
                );

        List<OSINetworkPacket> retryThree =
                host.tick(
                        2002L
                );

        host.tick(
                3003L
        );

        boolean failedClosed =
                first.size() == 1
                        && W117ArpFrame.isArp(
                        first.get(0)
                )
                        && retryTwo.size() == 1
                        && retryThree.size() == 1
                        && host.pendingCount() == 0;

        OSINetworkPacket retryPayload =
                new OSINetworkPacket();

        retryPayload.targetIp =
                "192.168.1.10";

        retryPayload.applicationProtocol =
                "DNS";

        retryPayload.targetPort =
                53;

        List<OSINetworkPacket> restarted =
                host.sendIpv4(
                        retryPayload,
                        4000L
                );

        OSINetworkPacket reply =
                W117ArpFrame.reply(
                        "02:00:00:00:00:10",
                        "192.168.1.10",
                        "02:00:00:00:00:02",
                        "192.168.1.2",
                        "W127-ARP-RECOVERY"
                );

        List<OSINetworkPacket> flushed =
                host.receive(
                        reply,
                        4001L
                );

        boolean recovered =
                restarted.size() == 1
                        && W117ArpFrame.isArp(
                        restarted.get(0)
                )
                        && flushed.size() == 1
                        && "192.168.1.10".equals(
                        flushed.get(0).targetIp
                )
                        && host.pendingCount() == 0;

        return result(
                "w127-arp-timeout-recovery",
                failedClosed && recovered,
                "Three-attempt ARP failure clears pending state; a later ARP reply flushes a fresh queued IPv4 packet"
        );
    }

    private static Result dhcpPoolExhaustionRecovery() {
        W118DhcpServer server =
                new W118DhcpServer();

        server.configure(
                "192.168.10.1",
                "255.255.255.0",
                "192.168.10.1",
                "192.168.10.1",
                "192.168.10.100",
                "192.168.10.100",
                60L
        );

        String serverMac =
                "02:00:00:00:10:01";

        String macOne =
                "02:00:00:00:10:11";

        String macTwo =
                "02:00:00:00:10:12";

        OSINetworkPacket discoverOne =
                W118DhcpMessage.discover(
                        macOne,
                        1001
                );

        OSINetworkPacket offerOne =
                server.handle(
                        discoverOne,
                        serverMac,
                        0L
                );

        String firstIp =
                offerOne == null
                        ? ""
                        : W118DhcpMessage.yourIp(
                        offerOne
                );

        OSINetworkPacket requestOne =
                W118DhcpMessage.request(
                        macOne,
                        1001,
                        firstIp,
                        "192.168.10.1",
                        true
                );

        OSINetworkPacket ackOne =
                server.handle(
                        requestOne,
                        serverMac,
                        1L
                );

        OSINetworkPacket discoverTwo =
                W118DhcpMessage.discover(
                        macTwo,
                        1002
                );

        OSINetworkPacket exhausted =
                server.handle(
                        discoverTwo,
                        serverMac,
                        2L
                );

        boolean exhaustionDetected =
                ackOne != null
                        && W118DhcpMessage.type(
                        ackOne
                ) == W118DhcpMessage.Type.ACK
                        && exhausted != null
                        && W118DhcpMessage.type(
                        exhausted
                ) == W118DhcpMessage.Type.NAK
                        && server.leaseCount(
                        2L
                ) == 1;

        server.expire(
                60_002L
        );

        OSINetworkPacket recoveredOffer =
                server.handle(
                        discoverTwo,
                        serverMac,
                        60_003L
                );

        boolean recovered =
                recoveredOffer != null
                        && W118DhcpMessage.type(
                        recoveredOffer
                ) == W118DhcpMessage.Type.OFFER
                        && "192.168.10.100".equals(
                        W118DhcpMessage.yourIp(
                                recoveredOffer
                        )
                );

        return result(
                "w127-dhcp-pool-exhaustion-recovery",
                exhaustionDetected && recovered,
                "Single-address DHCP pool returns NAK while exhausted and offers the address again after lease expiry"
        );
    }

    private static W127FaultRule rule(
            W127FaultAction action,
            String sourceIp,
            String targetIp,
            String app,
            String pdnsKind,
            String xfrKind,
            Integer sequence,
            Boolean response,
            int matches
    ) {
        return new W127FaultRule(
                action,
                sourceIp,
                targetIp,
                app,
                pdnsKind,
                xfrKind,
                sequence,
                response,
                matches,
                1L,
                0L
        );
    }

    private static OSINetworkPacket rootReferral() {
        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.sourceIp =
                "192.168.1.10";

        packet.targetIp =
                "192.168.1.2";

        packet.sourcePort =
                53;

        packet.targetPort =
                53000;

        packet.applicationProtocol =
                "DNS";

        packet.isResponse =
                true;

        packet.payload.putBoolean(
                "physical_dns",
                true
        );

        packet.payload.putString(
                "pdns_kind",
                "ROOT_REFERRAL"
        );

        return packet;
    }

    private static Result result(
            String name,
            boolean passed,
            String detail
    ) {
        return new Result(
                name,
                passed,
                detail
        );
    }
}
