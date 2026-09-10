package com.k1ngtle.vsia.signality.engineering.wifi.integration.w127;

import com.k1ngtle.vsia.signality.internet.dns.physical.DnsZoneSnapshot;
import com.k1ngtle.vsia.signality.internet.dns.physical.PhysicalDnsBootstrap;
import com.k1ngtle.vsia.signality.internet.dns.physical.PhysicalDnsService;
import com.k1ngtle.vsia.signality.internet.dns.physical.PhysicalDnsStateSavedData;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Locale;

public final class W127InternetStressSession {
    private final ServerLevel level;
    private final BlockPos resolverPos;
    private final BlockPos rootPos;
    private final BlockPos tldPos;
    private final BlockPos primaryPos;
    private final BlockPos secondaryPos;
    private final String zone;
    private final String hostname;

    private W127Stage stage = W127Stage.SETUP;
    private W127Failure failure = W127Failure.NONE;
    private String detail = "Waiting for first tick";
    private long startTick = -1L;
    private long stageStartTick = -1L;
    private boolean started;

    private String resolverIp = "";
    private String rootIp = "";
    private String tldIp = "";
    private String primaryIp = "";
    private String secondaryIp = "";

    private long protectedReplicaSerial;
    private int protectedReplicaRecords;

    public W127InternetStressSession(
            ServerLevel level,
            BlockPos resolverPos,
            BlockPos rootPos,
            BlockPos tldPos,
            BlockPos primaryPos,
            BlockPos secondaryPos,
            String zone,
            String hostname
    ) {
        this.level = level;
        this.resolverPos = resolverPos.immutable();
        this.rootPos = rootPos.immutable();
        this.tldPos = tldPos.immutable();
        this.primaryPos = primaryPos.immutable();
        this.secondaryPos = secondaryPos.immutable();
        this.zone = normalize(zone);
        this.hostname = normalize(hostname);
    }

    public void tick() {
        if (finished()) {
            return;
        }

        try {
            if (startTick < 0L) {
                startTick = level.getGameTime();
                stageStartTick = startTick;
            }

            ServerRackBlockEntity resolver = rack(resolverPos);
            ServerRackBlockEntity root = rack(rootPos);
            ServerRackBlockEntity tld = rack(tldPos);
            ServerRackBlockEntity primary = rack(primaryPos);
            ServerRackBlockEntity secondary = rack(secondaryPos);

            if (resolver == null
                    || root == null
                    || tld == null
                    || primary == null
                    || secondary == null) {
                fail(
                        W127Failure.INVALID_TOPOLOGY,
                        "Expected five loaded ServerRacks: resolver, root, TLD, primary, secondary"
                );
                return;
            }

            if (resolverIp.isBlank()) {
                resolverIp = resolver.ipAddress();
                rootIp = root.ipAddress();
                tldIp = tld.ipAddress();
                primaryIp = primary.ipAddress();
                secondaryIp = secondary.ipAddress();
            }

            switch (stage) {
                case SETUP ->
                        setup(
                                resolver,
                                root,
                                tld,
                                primary,
                                secondary
                        );

                case BASELINE_AXFR ->
                        baselineAxfr(
                                secondary
                        );

                case BASELINE_DNS ->
                        baselineDns(
                                resolver
                        );

                case ROOT_SINGLE_DROP_RECOVERY ->
                        rootSingleDropRecovery(
                                resolver
                        );

                case ROOT_OUTAGE_EXPECTED_FAIL ->
                        rootOutageExpectedFail(
                                resolver
                        );

                case ROOT_POST_OUTAGE_RECOVERY ->
                        rootPostOutageRecovery(
                                resolver
                        );

                case TLD_OUTAGE_EXPECTED_FAIL ->
                        tldOutageExpectedFail(
                                resolver
                        );

                case TLD_POST_OUTAGE_RECOVERY ->
                        tldPostOutageRecovery(
                                resolver
                        );

                case PRIMARY_OUTAGE_SECONDARY_FAILOVER ->
                        primaryOutageSecondaryFailover(
                                resolver
                        );

                case ROOT_RRSIG_TAMPER_REJECT ->
                        rootRrsigTamperReject(
                                resolver
                        );

                case TLD_EXPIRED_SIGNATURE_REJECT ->
                        tldExpiredSignatureReject(
                                resolver
                        );

                case AUTH_DNSKEY_DS_MISMATCH_REJECT ->
                        authDnskeyMismatchReject(
                                resolver
                        );

                case AUTH_RRSIG_TAMPER_REJECT ->
                        authRrsigTamperReject(
                                resolver
                        );

                case DUPLICATE_REFERRAL_IDEMPOTENCE ->
                        duplicateReferralIdempotence(
                                resolver
                        );

                case SERVFAIL_REJECT ->
                        servfailReject(
                                resolver
                        );

                case AXFR_REORDER_TOLERANCE ->
                        axfrReorderTolerance(
                                secondary
                        );

                case AXFR_DROP_INCOMPLETE_SAFE ->
                        axfrDropIncompleteSafe(
                                secondary
                        );

                case AXFR_RETRY_RECOVERY ->
                        axfrRetryRecovery(
                                secondary
                        );

                case IXFR_STALE_SERIAL_FALLBACK ->
                        ixfrStaleSerialFallback(
                                secondary
                        );

                case FINAL_BASELINE ->
                        finalBaseline(
                                resolver
                        );

                case CLEAN_STABILITY ->
                        cleanStability();

                case COMPLETE,
                     FAILED -> {
                }
            }
        } catch (Exception exception) {
            fail(
                    W127Failure.INTERNAL_ERROR,
                    exception.getClass().getSimpleName()
                            + ": "
                            + safe(exception.getMessage())
            );
        }
    }

    public boolean finished() {
        return stage == W127Stage.COMPLETE
                || stage == W127Stage.FAILED;
    }

    public boolean passed() {
        return stage == W127Stage.COMPLETE
                && failure == W127Failure.NONE;
    }

    public W127Snapshot snapshot(
            boolean internetSuitePassed,
            boolean wifiRegressionPassed,
            String wifiRegressionDetail
    ) {
        return new W127Snapshot(
                stage,
                failure,
                finished(),
                passed(),
                detail,
                startTick < 0L
                        ? 0L
                        : Math.max(
                        0L,
                        level.getGameTime() - startTick
                ),
                resolverIp.isBlank()
                        ? ""
                        : PhysicalDnsService.resolutionStatus(
                        resolverIp
                ),
                secondaryIp.isBlank()
                        ? ""
                        : PhysicalDnsService.transferStatus(
                        secondaryIp
                ),
                resolverIp.isBlank()
                        ? ""
                        : PhysicalDnsService.resolutionTrace(
                        resolverIp
                ),
                W127FaultController.snapshot(),
                internetSuitePassed,
                wifiRegressionPassed,
                wifiRegressionDetail
        );
    }

    private void setup(
            ServerRackBlockEntity resolver,
            ServerRackBlockEntity root,
            ServerRackBlockEntity tld,
            ServerRackBlockEntity primary,
            ServerRackBlockEntity secondary
    ) {
        if (!started) {
            W127FaultController.clearAll();
            W127FaultController.resetMetrics();

            PhysicalDnsBootstrap.Result result =
                    PhysicalDnsBootstrap.configure(
                            level,
                            resolver,
                            root,
                            tld,
                            primary,
                            secondary,
                            zone
                    );

            if (!result.success()) {
                fail(
                        W127Failure.BOOTSTRAP_FAILED,
                        result.detail()
                );
                return;
            }

            detail =
                    "Physical DNS hierarchy verified; starting baseline AXFR";

            transition(
                    W127Stage.BASELINE_AXFR
            );
        }
    }

    private void baselineAxfr(
            ServerRackBlockEntity secondary
    ) {
        if (!started) {
            started = true;
            String start =
                    PhysicalDnsService.startTransfer(
                            secondary,
                            "AXFR"
                    );

            detail =
                    "Baseline AXFR | "
                            + start;
        }

        String status =
                PhysicalDnsService.transferStatus(
                        secondaryIp
                );

        if (status.startsWith("PASS")) {
            DnsZoneSnapshot replica =
                    replica();

            if (replica == null) {
                fail(
                        W127Failure.BASELINE_AXFR_FAILED,
                        "AXFR says PASS but no secondary replica exists"
                );
                return;
            }

            protectedReplicaSerial =
                    replica.serial();

            protectedReplicaRecords =
                    replica.records().size();

            transition(
                    W127Stage.BASELINE_DNS,
                    "Baseline AXFR PASS | serial="
                            + Long.toUnsignedString(
                            protectedReplicaSerial
                    )
                            + " records="
                            + protectedReplicaRecords
            );
            return;
        }

        if (status.startsWith("FAIL")
                || stageTimedOut()) {
            fail(
                    W127Failure.BASELINE_AXFR_FAILED,
                    status
            );
        }
    }

    private void baselineDns(
            ServerRackBlockEntity resolver
    ) {
        if (!beginResolution(
                resolver,
                "Baseline physical DNS"
        )) {
            return;
        }

        String status = resolution();

        if (isDnsPass(status)) {
            transition(
                    W127Stage.ROOT_SINGLE_DROP_RECOVERY,
                    "Baseline DNSSEC iterative resolution PASS"
            );
            return;
        }

        if (status.startsWith("FAIL")
                || stageTimedOut()) {
            fail(
                    W127Failure.BASELINE_DNS_FAILED,
                    status
            );
        }
    }

    private void rootSingleDropRecovery(
            ServerRackBlockEntity resolver
    ) {
        if (!started) {
            W127FaultController.clearAll();

            W127FaultController.arm(
                    W127FaultAction.DROP,
                    resolverIp,
                    rootIp,
                    "DNS",
                    "ROOT_REFERRAL_QUERY",
                    "",
                    null,
                    false,
                    1,
                    1L,
                    0L
            );

            if (!startResolution(
                    resolver,
                    "Drop first resolver->Root query"
            )) {
                return;
            }
        }

        String status = resolution();

        if (isDnsPass(status)) {
            String trace = trace();

            if (!trace.contains(
                    "RETRY WAIT_ROOT_REFERRAL #1"
            )) {
                fail(
                        W127Failure.ROOT_RETRY_FAILED,
                        "Resolution passed but Root retry was not observed | "
                                + trace
                );
                return;
            }

            W127FaultController.clearAll();

            transition(
                    W127Stage.ROOT_OUTAGE_EXPECTED_FAIL,
                    "Single Root-query loss recovered through bounded DNS retry"
            );
            return;
        }

        if (status.startsWith("FAIL")
                || stageTimedOut()) {
            fail(
                    W127Failure.ROOT_RETRY_FAILED,
                    status
            );
        }
    }

    private void rootOutageExpectedFail(
            ServerRackBlockEntity resolver
    ) {
        if (!started) {
            W127FaultController.clearAll();

            W127FaultController.arm(
                    W127FaultAction.DROP,
                    resolverIp,
                    rootIp,
                    "DNS",
                    "ROOT_REFERRAL_QUERY",
                    "",
                    null,
                    false,
                    -1,
                    1L,
                    0L
            );

            if (!startResolution(
                    resolver,
                    "Persistent Root outage"
            )) {
                return;
            }
        }

        String status = resolution();

        if (status.startsWith("FAIL")) {
            if (!status.contains(
                    "WAIT_ROOT_REFERRAL"
            )) {
                fail(
                        W127Failure.ROOT_OUTAGE_NOT_DETECTED,
                        "Root outage failed at wrong state | "
                                + status
                );
                return;
            }

            W127FaultController.clearAll();

            transition(
                    W127Stage.ROOT_POST_OUTAGE_RECOVERY,
                    "Persistent Root outage failed closed at WAIT_ROOT_REFERRAL"
            );
            return;
        }

        if (stageTimedOut(
                420L
        )) {
            fail(
                    W127Failure.ROOT_OUTAGE_NOT_DETECTED,
                    "Persistent Root outage did not produce bounded failure"
            );
        }
    }

    private void rootPostOutageRecovery(
            ServerRackBlockEntity resolver
    ) {
        if (!beginResolution(
                resolver,
                "Root post-outage recovery"
        )) {
            return;
        }

        String status = resolution();

        if (isDnsPass(status)) {
            transition(
                    W127Stage.TLD_OUTAGE_EXPECTED_FAIL,
                    "Root service recovered without world restart"
            );
            return;
        }

        if (status.startsWith("FAIL")
                || stageTimedOut()) {
            fail(
                    W127Failure.ROOT_RECOVERY_FAILED,
                    status
            );
        }
    }

    private void tldOutageExpectedFail(
            ServerRackBlockEntity resolver
    ) {
        if (!started) {
            W127FaultController.clearAll();

            W127FaultController.arm(
                    W127FaultAction.DROP,
                    resolverIp,
                    tldIp,
                    "DNS",
                    "DNSKEY_QUERY",
                    "",
                    null,
                    false,
                    -1,
                    1L,
                    0L
            );

            if (!startResolution(
                    resolver,
                    "Persistent TLD DNSKEY outage"
            )) {
                return;
            }
        }

        String status = resolution();

        if (status.startsWith("FAIL")) {
            if (!status.contains(
                    "WAIT_TLD_DNSKEY"
            )) {
                fail(
                        W127Failure.TLD_OUTAGE_NOT_DETECTED,
                        "TLD outage failed at wrong state | "
                                + status
                );
                return;
            }

            W127FaultController.clearAll();

            transition(
                    W127Stage.TLD_POST_OUTAGE_RECOVERY,
                    "Persistent TLD outage failed closed at WAIT_TLD_DNSKEY"
            );
            return;
        }

        if (stageTimedOut(
                420L
        )) {
            fail(
                    W127Failure.TLD_OUTAGE_NOT_DETECTED,
                    "Persistent TLD outage did not produce bounded failure"
            );
        }
    }

    private void tldPostOutageRecovery(
            ServerRackBlockEntity resolver
    ) {
        if (!beginResolution(
                resolver,
                "TLD post-outage recovery"
        )) {
            return;
        }

        String status = resolution();

        if (isDnsPass(status)) {
            transition(
                    W127Stage.PRIMARY_OUTAGE_SECONDARY_FAILOVER,
                    "TLD service recovered without world restart"
            );
            return;
        }

        if (status.startsWith("FAIL")
                || stageTimedOut()) {
            fail(
                    W127Failure.TLD_RECOVERY_FAILED,
                    status
            );
        }
    }

    private void primaryOutageSecondaryFailover(
            ServerRackBlockEntity resolver
    ) {
        if (!started) {
            W127FaultController.clearAll();

            W127FaultController.arm(
                    W127FaultAction.DROP,
                    resolverIp,
                    primaryIp,
                    "DNS",
                    "DNSKEY_QUERY",
                    "",
                    null,
                    false,
                    -1,
                    1L,
                    0L
            );

            if (!startResolution(
                    resolver,
                    "Primary authoritative outage"
            )) {
                return;
            }
        }

        String status = resolution();

        if (isDnsPass(status)) {
            String trace = trace();

            if (!trace.contains(
                    "failover secondary "
                            + secondaryIp
            )) {
                fail(
                        W127Failure.SECONDARY_FAILOVER_FAILED,
                        "Resolution passed but secondary failover was not observed | "
                                + trace
                );
                return;
            }

            W127FaultController.clearAll();

            transition(
                    W127Stage.ROOT_RRSIG_TAMPER_REJECT,
                    "Authoritative primary outage failed over to transferred secondary copy"
            );
            return;
        }

        if (status.startsWith("FAIL")
                || stageTimedOut(
                420L
        )) {
            fail(
                    W127Failure.SECONDARY_FAILOVER_FAILED,
                    status
            );
        }
    }

    private void rootRrsigTamperReject(
            ServerRackBlockEntity resolver
    ) {
        if (!started) {
            W127FaultController.clearAll();

            W127FaultController.arm(
                    W127FaultAction.CORRUPT_RRSIG,
                    rootIp,
                    resolverIp,
                    "DNS",
                    "ROOT_REFERRAL",
                    "",
                    null,
                    true,
                    1,
                    1L,
                    0L
            );

            if (!startResolution(
                    resolver,
                    "Tampered Root RRSIG"
            )) {
                return;
            }
        }

        expectedDnssecFailure(
                W127Failure.DNSSEC_TAMPER_ACCEPTED,
                "DNSSEC root trust-anchor/referral validation failed.",
                W127Stage.TLD_EXPIRED_SIGNATURE_REJECT,
                "Tampered signed Root referral rejected"
        );
    }

    private void tldExpiredSignatureReject(
            ServerRackBlockEntity resolver
    ) {
        if (!started) {
            W127FaultController.clearAll();

            W127FaultController.arm(
                    W127FaultAction.EXPIRE_RRSIG,
                    tldIp,
                    resolverIp,
                    "DNS",
                    "DNSKEY_ANSWER",
                    "",
                    null,
                    true,
                    1,
                    1L,
                    0L
            );

            if (!startResolution(
                    resolver,
                    "Expired TLD DNSKEY signature"
            )) {
                return;
            }
        }

        expectedDnssecFailure(
                W127Failure.DNSSEC_EXPIRED_ACCEPTED,
                "DNSSEC TLD DNSKEY/DS validation failed.",
                W127Stage.AUTH_DNSKEY_DS_MISMATCH_REJECT,
                "Expired DNSSEC signature rejected"
        );
    }

    private void authDnskeyMismatchReject(
            ServerRackBlockEntity resolver
    ) {
        if (!started) {
            W127FaultController.clearAll();

            W127FaultController.arm(
                    W127FaultAction.CORRUPT_DNSKEY,
                    primaryIp,
                    resolverIp,
                    "DNS",
                    "DNSKEY_ANSWER",
                    "",
                    null,
                    true,
                    1,
                    1L,
                    0L
            );

            if (!startResolution(
                    resolver,
                    "Authoritative DNSKEY/DS mismatch"
            )) {
                return;
            }
        }

        expectedDnssecFailure(
                W127Failure.DNSSEC_DS_MISMATCH_ACCEPTED,
                "DNSSEC authoritative DNSKEY/DS validation failed.",
                W127Stage.AUTH_RRSIG_TAMPER_REJECT,
                "Authoritative DNSKEY that did not match parent DS was rejected"
        );
    }

    private void authRrsigTamperReject(
            ServerRackBlockEntity resolver
    ) {
        if (!started) {
            W127FaultController.clearAll();

            W127FaultController.arm(
                    W127FaultAction.CORRUPT_RRSIG,
                    primaryIp,
                    resolverIp,
                    "DNS",
                    "AUTH_ANSWER",
                    "",
                    null,
                    true,
                    1,
                    1L,
                    0L
            );

            if (!startResolution(
                    resolver,
                    "Tampered authoritative RRset signature"
            )) {
                return;
            }
        }

        expectedDnssecFailure(
                W127Failure.DNSSEC_RRSIG_ACCEPTED,
                "DNSSEC authoritative RRSIG validation failed.",
                W127Stage.DUPLICATE_REFERRAL_IDEMPOTENCE,
                "Tampered authoritative RRset rejected"
        );
    }

    private void duplicateReferralIdempotence(
            ServerRackBlockEntity resolver
    ) {
        if (!started) {
            W127FaultController.clearAll();

            W127FaultController.arm(
                    W127FaultAction.DUPLICATE,
                    rootIp,
                    resolverIp,
                    "DNS",
                    "ROOT_REFERRAL",
                    "",
                    null,
                    true,
                    1,
                    1L,
                    0L
            );

            if (!startResolution(
                    resolver,
                    "Duplicate Root referral"
            )) {
                return;
            }
        }

        String status = resolution();

        if (isDnsPass(status)) {
            if (W127FaultController.snapshot()
                    .duplicated() < 1L) {
                fail(
                        W127Failure.DUPLICATE_NOT_IDEMPOTENT,
                        "Resolution passed but duplicate injection was not recorded"
                );
                return;
            }

            W127FaultController.clearAll();

            transition(
                    W127Stage.SERVFAIL_REJECT,
                    "Duplicate Root referral was idempotent"
            );
            return;
        }

        if (status.startsWith("FAIL")
                || stageTimedOut()) {
            fail(
                    W127Failure.DUPLICATE_NOT_IDEMPOTENT,
                    status
            );
        }
    }

    private void servfailReject(
            ServerRackBlockEntity resolver
    ) {
        if (!started) {
            W127FaultController.clearAll();

            W127FaultController.arm(
                    W127FaultAction.SERVFAIL,
                    rootIp,
                    resolverIp,
                    "DNS",
                    "ROOT_REFERRAL",
                    "",
                    null,
                    true,
                    1,
                    1L,
                    0L
            );

            if (!startResolution(
                    resolver,
                    "Injected upstream SERVFAIL"
            )) {
                return;
            }
        }

        String status = resolution();

        if (status.startsWith("FAIL")
                && status.contains("rcode=2")) {
            W127FaultController.clearAll();

            transition(
                    W127Stage.AXFR_REORDER_TOLERANCE,
                    "Upstream SERVFAIL propagated as controlled resolver failure"
            );
            return;
        }

        if (isDnsPass(status)) {
            fail(
                    W127Failure.SERVFAIL_ACCEPTED,
                    "Resolver accepted an injected SERVFAIL"
            );
            return;
        }

        if (stageTimedOut()) {
            fail(
                    W127Failure.SERVFAIL_ACCEPTED,
                    status
            );
        }
    }

    private void axfrReorderTolerance(
            ServerRackBlockEntity secondary
    ) {
        if (!started) {
            W127FaultController.clearAll();

            W127FaultController.arm(
                    W127FaultAction.DELAY,
                    primaryIp,
                    secondaryIp,
                    "DNS_XFR",
                    "",
                    "DATA",
                    1,
                    true,
                    1,
                    16L,
                    0L
            );

            started = true;

            String start =
                    PhysicalDnsService.startTransfer(
                            secondary,
                            "AXFR"
                    );

            detail =
                    "AXFR reorder test | delay DATA seq=1 by 16 ticks | "
                            + start;
        }

        String status =
                PhysicalDnsService.transferStatus(
                        secondaryIp
                );

        if (status.startsWith("PASS")) {
            W127FaultSnapshot faults =
                    W127FaultController.snapshot();

            if (faults.delayed() < 1L
                    || faults.delayedDelivered() < 1L) {
                fail(
                        W127Failure.AXFR_REORDER_FAILED,
                        "AXFR passed but delayed packet was not observed | "
                                + faults.compact()
                );
                return;
            }

            protectReplica();
            W127FaultController.clearAll();

            transition(
                    W127Stage.AXFR_DROP_INCOMPLETE_SAFE,
                    "Out-of-order AXFR reassembled and committed only after all chunks arrived"
            );
            return;
        }

        if (status.startsWith("FAIL")
                || stageTimedOut()) {
            fail(
                    W127Failure.AXFR_REORDER_FAILED,
                    status
            );
        }
    }

    private void axfrDropIncompleteSafe(
            ServerRackBlockEntity secondary
    ) {
        if (!started) {
            protectReplica();
            W127FaultController.clearAll();

            W127FaultController.arm(
                    W127FaultAction.DROP,
                    primaryIp,
                    secondaryIp,
                    "DNS_XFR",
                    "",
                    "DATA",
                    1,
                    true,
                    1,
                    1L,
                    0L
            );

            started = true;

            String start =
                    PhysicalDnsService.startTransfer(
                            secondary,
                            "AXFR"
                    );

            detail =
                    "AXFR incomplete-transfer safety | drop DATA seq=1 | "
                            + start;
            return;
        }

        if (elapsedStage()
                < W127ResiliencePolicy.TRANSFER_INCOMPLETE_OBSERVE_TICKS) {
            return;
        }

        DnsZoneSnapshot replica =
                replica();

        if (replica == null
                || replica.serial() != protectedReplicaSerial
                || replica.records().size() != protectedReplicaRecords) {
            fail(
                    W127Failure.AXFR_PARTIAL_COMMIT,
                    "Incomplete AXFR changed committed secondary replica"
            );
            return;
        }

        String status =
                PhysicalDnsService.transferStatus(
                        secondaryIp
                );

        if (status.startsWith("PASS")) {
            fail(
                    W127Failure.AXFR_PARTIAL_COMMIT,
                    "AXFR incorrectly committed after one transfer DATA packet was dropped"
            );
            return;
        }

        PhysicalDnsService.w127CancelTransfers(
                secondaryIp
        );

        W127FaultController.clearAll();

        transition(
                W127Stage.AXFR_RETRY_RECOVERY,
                "Incomplete AXFR stayed uncommitted; stale receive state cancelled safely"
        );
    }

    private void axfrRetryRecovery(
            ServerRackBlockEntity secondary
    ) {
        if (!started) {
            started = true;

            String start =
                    PhysicalDnsService.startTransfer(
                            secondary,
                            "AXFR"
                    );

            detail =
                    "Clean AXFR retry after incomplete transfer | "
                            + start;
        }

        String status =
                PhysicalDnsService.transferStatus(
                        secondaryIp
                );

        if (status.startsWith("PASS")) {
            protectReplica();

            transition(
                    W127Stage.IXFR_STALE_SERIAL_FALLBACK,
                    "Clean AXFR retry recovered without world restart"
            );
            return;
        }

        if (status.startsWith("FAIL")
                || stageTimedOut()) {
            fail(
                    W127Failure.AXFR_RETRY_FAILED,
                    status
            );
        }
    }

    private void ixfrStaleSerialFallback(
            ServerRackBlockEntity secondary
    ) {
        if (!started) {
            W127FaultController.clearAll();

            W127FaultController.arm(
                    W127FaultAction.FORCE_XFR_SERIAL,
                    secondaryIp,
                    primaryIp,
                    "DNS_XFR",
                    "",
                    "REQUEST",
                    null,
                    false,
                    1,
                    1L,
                    1L
            );

            started = true;

            String start =
                    PhysicalDnsService.startTransfer(
                            secondary,
                            "IXFR"
                    );

            detail =
                    "IXFR stale-serial fallback | forced from_serial=1 | "
                            + start;
        }

        String status =
                PhysicalDnsService.transferStatus(
                        secondaryIp
                );

        if (status.startsWith("PASS")) {
            if (!status.contains(
                    "AXFR "
            )) {
                fail(
                        W127Failure.IXFR_STALE_FALLBACK_FAILED,
                        "Stale IXFR serial did not fall back to AXFR | "
                                + status
                );
                return;
            }

            W127FaultController.clearAll();

            transition(
                    W127Stage.FINAL_BASELINE,
                    "Unavailable IXFR history correctly fell back to full AXFR"
            );
            return;
        }

        if (status.startsWith("FAIL")
                || stageTimedOut()) {
            fail(
                    W127Failure.IXFR_STALE_FALLBACK_FAILED,
                    status
            );
        }
    }

    private void finalBaseline(
            ServerRackBlockEntity resolver
    ) {
        if (!beginResolution(
                resolver,
                "Final clean baseline after destructive fault suite"
        )) {
            return;
        }

        String status = resolution();

        if (isDnsPass(status)) {
            transition(
                    W127Stage.CLEAN_STABILITY,
                    "Final no-fault DNSSEC resolution PASS; entering clean stability window"
            );
            return;
        }

        if (status.startsWith("FAIL")
                || stageTimedOut()) {
            fail(
                    W127Failure.FINAL_BASELINE_FAILED,
                    status
            );
        }
    }

    private void cleanStability() {
        W127FaultSnapshot faults =
                W127FaultController.snapshot();

        if (faults.activeRules() != 0
                || faults.delayedQueued() != 0) {
            fail(
                    W127Failure.FAULT_CLEANUP_FAILED,
                    "Fault state leaked into final stability window | "
                            + faults.compact()
            );
            return;
        }

        if (elapsedStage()
                >= W127ResiliencePolicy.CLEAN_STABILITY_TICKS) {
            transition(
                    W127Stage.COMPLETE,
                    "PASS: W1.27 — Stress / Failure / Edge Cases"
                            + " | baseline DNS/AXFR"
                            + " | Root loss retry"
                            + " | Root/TLD outages fail closed"
                            + " | no-restart recovery"
                            + " | authoritative secondary failover"
                            + " | DNSSEC tamper/expiry/DS mismatch/RRSIG rejection"
                            + " | duplicate idempotence"
                            + " | SERVFAIL rejection"
                            + " | AXFR reorder tolerance"
                            + " | incomplete AXFR no partial commit"
                            + " | AXFR retry recovery"
                            + " | stale IXFR -> AXFR fallback"
                            + " | final clean DNSSEC baseline"
            );
        }
    }

    private boolean beginResolution(
            ServerRackBlockEntity resolver,
            String label
    ) {
        if (started) {
            return true;
        }

        W127FaultController.clearAll();

        return startResolution(
                resolver,
                label
        );
    }

    private boolean startResolution(
            ServerRackBlockEntity resolver,
            String label
    ) {
        String result =
                PhysicalDnsService.startDiagnostic(
                        resolver,
                        hostname,
                        "A"
                );

        if (!result.startsWith(
                "Physical iterative DNS query started:"
        )) {
            fail(
                    W127Failure.INTERNAL_ERROR,
                    label
                            + " could not start | "
                            + result
            );
            return false;
        }

        started = true;
        detail =
                label
                        + " | "
                        + result;

        return true;
    }

    private void expectedDnssecFailure(
            W127Failure acceptedFailure,
            String expectedDetail,
            W127Stage next,
            String successDetail
    ) {
        String status = resolution();

        if (status.startsWith("FAIL")) {
            if (!status.contains(
                    expectedDetail
            )) {
                fail(
                        acceptedFailure,
                        "DNSSEC failure reason mismatch | expected="
                                + expectedDetail
                                + " actual="
                                + status
                );
                return;
            }

            W127FaultController.clearAll();

            transition(
                    next,
                    successDetail
            );
            return;
        }

        if (isDnsPass(status)) {
            fail(
                    acceptedFailure,
                    "Faulted DNSSEC material was accepted | "
                            + status
            );
            return;
        }

        if (stageTimedOut()) {
            fail(
                    acceptedFailure,
                    "DNSSEC negative test timed out | "
                            + status
            );
        }
    }

    private String resolution() {
        return PhysicalDnsService.resolutionStatus(
                resolverIp
        );
    }

    private String trace() {
        return PhysicalDnsService.resolutionTrace(
                resolverIp
        );
    }

    private boolean isDnsPass(String status) {
        return status.startsWith("PASS")
                && status.contains(
                "DNSSEC=VALIDATED"
        )
                && status.contains(
                hostname
        );
    }

    private void protectReplica() {
        DnsZoneSnapshot replica =
                replica();

        if (replica != null) {
            protectedReplicaSerial =
                    replica.serial();

            protectedReplicaRecords =
                    replica.records().size();
        }
    }

    private DnsZoneSnapshot replica() {
        return PhysicalDnsStateSavedData.get(
                        level
                )
                .replica(
                        secondaryIp,
                        zone
                )
                .orElse(
                        null
                );
    }

    private ServerRackBlockEntity rack(
            BlockPos pos
    ) {
        BlockEntity blockEntity =
                level.getBlockEntity(
                        pos
                );

        return blockEntity instanceof ServerRackBlockEntity rack
                ? rack
                : null;
    }

    private long elapsedStage() {
        return stageStartTick < 0L
                ? 0L
                : Math.max(
                0L,
                level.getGameTime()
                        - stageStartTick
        );
    }

    private boolean stageTimedOut() {
        return stageTimedOut(
                W127ResiliencePolicy.LIVE_STAGE_TIMEOUT_TICKS
        );
    }

    private boolean stageTimedOut(long limit) {
        return elapsedStage() > limit;
    }

    private void transition(W127Stage next) {
        transition(
                next,
                detail
        );
    }

    private void transition(
            W127Stage next,
            String newDetail
    ) {
        stage = next;
        detail = newDetail;
        stageStartTick = level.getGameTime();
        started = false;
    }

    private void fail(
            W127Failure failure,
            String detail
    ) {
        this.failure = failure;
        this.detail = detail;
        this.stage = W127Stage.FAILED;

        W127FaultController.clearAll();

        if (!secondaryIp.isBlank()) {
            PhysicalDnsService.w127CancelTransfers(
                    secondaryIp
            );
        }
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private static String safe(String value) {
        return value == null
                ? ""
                : value;
    }
}
