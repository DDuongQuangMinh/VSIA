package com.k1ngtle.vsia.signality.internet.dns.physical;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.provider.InternetDnsRecord;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistryResult;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistrySavedData;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.ServerRackDirectory;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class PhysicalDnsClosureTestManager {
    private static Session active;

    private PhysicalDnsClosureTestManager() {
    }

    public static synchronized String start(
            ServerLevel level,
            UUID ownerUuid,
            String ownerName,
            ServerRackBlockEntity resolver,
            ServerRackBlockEntity root,
            ServerRackBlockEntity tld,
            ServerRackBlockEntity primary,
            ServerRackBlockEntity secondary,
            String zone,
            String hostname
    ) {
        if (active != null && !active.finished) {
            return "A physical DNS closure test is already active.";
        }

        PhysicalDnsBootstrap.Result bootstrap =
                PhysicalDnsBootstrap.configure(
                        level,
                        resolver,
                        root,
                        tld,
                        primary,
                        secondary,
                        zone
                );

        if (!bootstrap.success()) {
            return "FAIL | " + bootstrap.detail();
        }

        String normalizedZone =
                PhysicalDnsStateSavedData.normalizeZone(
                        zone
                );

        String normalizedHostname =
                hostname == null
                        || hostname.isBlank()
                        ? "www." + normalizedZone
                        : hostname.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        InternetRegistrySavedData registry =
                InternetRegistrySavedData.get(
                        level
                );

        registry.removeDnsRecord(
                ownerUuid,
                normalizedZone,
                "_isp1-xfr-test",
                "TXT"
        );

        active =
                new Session(
                        level,
                        ownerUuid,
                        ownerName,
                        resolver.ipAddress(),
                        primary.ipAddress(),
                        secondary.ipAddress(),
                        normalizedZone,
                        normalizedHostname,
                        level.getGameTime()
                );

        String transfer =
                PhysicalDnsService.startTransfer(
                        secondary,
                        "AXFR"
                );

        active.stage =
                Stage.WAIT_AXFR;

        active.detail =
                "Initial AXFR started | "
                        + transfer;

        return "Physical DNS closure test started"
                + " | zone="
                + normalizedZone
                + " | host="
                + normalizedHostname
                + " | "
                + bootstrap.detail();
    }

    public static synchronized Snapshot snapshot() {
        if (active == null) {
            return null;
        }

        return active.snapshot();
    }

    public static synchronized void reset() {
        active = null;
    }

    @SubscribeEvent
    public static void onServerTick(
            TickEvent.ServerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Session session;

        synchronized (PhysicalDnsClosureTestManager.class) {
            session = active;
        }

        if (session == null || session.finished) {
            return;
        }

        if (session.level.getGameTime()
                - session.lastCheckTick < 5L) {
            return;
        }

        session.lastCheckTick =
                session.level.getGameTime();

        session.tick();
    }

    private enum Stage {
        IDLE,
        WAIT_AXFR,
        WAIT_RESOLUTION,
        WAIT_IXFR_ADD,
        WAIT_IXFR_DELETE,
        COMPLETE,
        FAILED
    }

    private static final class Session {
        private final ServerLevel level;
        private final UUID ownerUuid;
        private final String ownerName;
        private final String resolverIp;
        private final String primaryIp;
        private final String secondaryIp;
        private final String zone;
        private final String hostname;
        private final long startedTick;

        private Stage stage =
                Stage.IDLE;

        private boolean finished;
        private boolean passed;
        private String detail =
                "";

        private long lastCheckTick;

        private Session(
                ServerLevel level,
                UUID ownerUuid,
                String ownerName,
                String resolverIp,
                String primaryIp,
                String secondaryIp,
                String zone,
                String hostname,
                long startedTick
        ) {
            this.level = level;
            this.ownerUuid = ownerUuid;
            this.ownerName = ownerName;
            this.resolverIp = resolverIp;
            this.primaryIp = primaryIp;
            this.secondaryIp = secondaryIp;
            this.zone = zone;
            this.hostname = hostname;
            this.startedTick = startedTick;
        }

        private void tick() {
            if (level.getGameTime()
                    - startedTick > 3600L) {
                fail(
                        "Closure test timeout."
                );
                return;
            }

            switch (stage) {
                case WAIT_AXFR ->
                        waitAxfr();

                case WAIT_RESOLUTION ->
                        waitResolution();

                case WAIT_IXFR_ADD ->
                        waitIxfrAdd();

                case WAIT_IXFR_DELETE ->
                        waitIxfrDelete();

                default -> {
                }
            }
        }

        private void waitAxfr() {
            String status =
                    PhysicalDnsService.transferStatus(
                            secondaryIp
                    );

            detail =
                    "AXFR | "
                            + status;

            if (status.startsWith(
                    "FAIL"
            )) {
                fail(
                        status
                );
                return;
            }

            if (!status.startsWith(
                    "PASS"
            )) {
                return;
            }

            ServerRackBlockEntity resolver =
                    ServerRackDirectory.byIp(
                            level,
                            resolverIp
                    );

            if (resolver == null) {
                fail(
                        "Recursive resolver is not loaded."
                );
                return;
            }

            String started =
                    PhysicalDnsService.startDiagnostic(
                            resolver,
                            hostname,
                            "A"
                    );

            stage =
                    Stage.WAIT_RESOLUTION;

            detail =
                    "DNSSEC iterative resolution | "
                            + started;
        }

        private void waitResolution() {
            String status =
                    PhysicalDnsService.resolutionStatus(
                            resolverIp
                    );

            detail =
                    "RESOLUTION | "
                            + status;

            if (status.startsWith(
                    "FAIL"
            )) {
                fail(
                        status
                );
                return;
            }

            if (!status.startsWith(
                    "PASS"
            )) {
                return;
            }

            InternetRegistryResult add =
                    InternetRegistrySavedData.get(
                            level
                    ).addDnsRecord(
                            ownerUuid,
                            zone,
                            "_isp1-xfr-test",
                            "TXT",
                            "physical-ixfr-proof",
                            300
                    );

            if (!add.success()) {
                fail(
                        "Could not add temporary IXFR proof record: "
                                + add.message()
                );
                return;
            }

            ServerRackBlockEntity secondary =
                    ServerRackDirectory.byIp(
                            level,
                            secondaryIp
                    );

            if (secondary == null) {
                fail(
                        "Secondary authoritative server is not loaded."
                );
                return;
            }

            String transfer =
                    PhysicalDnsService.startTransfer(
                            secondary,
                            "IXFR"
                    );

            stage =
                    Stage.WAIT_IXFR_ADD;

            detail =
                    "IXFR add | "
                            + transfer;
        }

        private void waitIxfrAdd() {
            String status =
                    PhysicalDnsService.transferStatus(
                            secondaryIp
                    );

            detail =
                    "IXFR ADD | "
                            + status;

            if (status.startsWith(
                    "FAIL"
            )) {
                cleanupTemporaryRecord();
                fail(
                        status
                );
                return;
            }

            if (!status.startsWith(
                    "PASS"
            )) {
                return;
            }

            DnsZoneSnapshot replica =
                    PhysicalDnsStateSavedData.get(
                            level
                    ).replica(
                            secondaryIp,
                            zone
                    ).orElse(
                            null
                    );

            if (replica == null
                    || replica.records()
                    .stream()
                    .noneMatch(
                            record ->
                                    record.name()
                                            .equalsIgnoreCase(
                                                    "_isp1-xfr-test."
                                                            + zone
                                            )
                                            && record.type()
                                            .equalsIgnoreCase(
                                                    "TXT"
                                            )
                                            && record.value()
                                            .equals(
                                                    "physical-ixfr-proof"
                                            )
                    )) {
                cleanupTemporaryRecord();
                fail(
                        "IXFR reported PASS but the secondary replica did not receive the ADD delta."
                );
                return;
            }

            InternetRegistryResult remove =
                    InternetRegistrySavedData.get(
                            level
                    ).removeDnsRecord(
                            ownerUuid,
                            zone,
                            "_isp1-xfr-test",
                            "TXT"
                    );

            if (!remove.success()) {
                fail(
                        "Could not remove temporary IXFR proof record: "
                                + remove.message()
                );
                return;
            }

            ServerRackBlockEntity secondary =
                    ServerRackDirectory.byIp(
                            level,
                            secondaryIp
                    );

            if (secondary == null) {
                fail(
                        "Secondary authoritative server disappeared."
                );
                return;
            }

            String transfer =
                    PhysicalDnsService.startTransfer(
                            secondary,
                            "IXFR"
                    );

            stage =
                    Stage.WAIT_IXFR_DELETE;

            detail =
                    "IXFR delete | "
                            + transfer;
        }

        private void waitIxfrDelete() {
            String status =
                    PhysicalDnsService.transferStatus(
                            secondaryIp
                    );

            detail =
                    "IXFR DELETE | "
                            + status;

            if (status.startsWith(
                    "FAIL"
            )) {
                fail(
                        status
                );
                return;
            }

            if (!status.startsWith(
                    "PASS"
            )) {
                return;
            }

            DnsZoneSnapshot replica =
                    PhysicalDnsStateSavedData.get(
                            level
                    ).replica(
                            secondaryIp,
                            zone
                    ).orElse(
                            null
                    );

            if (replica == null) {
                fail(
                        "Secondary replica vanished after IXFR delete."
                );
                return;
            }

            boolean temporaryStillPresent =
                    replica.records()
                            .stream()
                            .anyMatch(
                                    record ->
                                            record.name()
                                                    .equalsIgnoreCase(
                                                            "_isp1-xfr-test."
                                                                    + zone
                                                    )
                                                    && record.type()
                                                    .equalsIgnoreCase(
                                                            "TXT"
                                                    )
                            );

            if (temporaryStillPresent) {
                fail(
                        "IXFR DELETE delta did not remove the temporary record."
                );
                return;
            }

            stage =
                    Stage.COMPLETE;

            finished =
                    true;

            passed =
                    true;

            detail =
                    "PASS | physical ROOT/TLD referrals"
                            + " | DNSSEC chain validated"
                            + " | AXFR complete"
                            + " | IXFR ADD complete"
                            + " | IXFR DELETE complete"
                            + " | primary="
                            + primaryIp
                            + " | secondary="
                            + secondaryIp
                            + " | zone="
                            + zone;
        }

        private void cleanupTemporaryRecord() {
            InternetRegistrySavedData.get(
                    level
            ).removeDnsRecord(
                    ownerUuid,
                    zone,
                    "_isp1-xfr-test",
                    "TXT"
            );
        }

        private void fail(String reason) {
            finished =
                    true;

            passed =
                    false;

            stage =
                    Stage.FAILED;

            detail =
                    "FAIL | "
                            + reason;
        }

        private Snapshot snapshot() {
            return new Snapshot(
                    stage.name(),
                    finished,
                    passed,
                    detail,
                    resolverIp,
                    primaryIp,
                    secondaryIp,
                    zone,
                    hostname,
                    PhysicalDnsService.transferStatus(
                            secondaryIp
                    ),
                    PhysicalDnsService.resolutionStatus(
                            resolverIp
                    ),
                    PhysicalDnsService.resolutionTrace(
                            resolverIp
                    ),
                    Math.max(
                            0L,
                            level.getGameTime()
                                    - startedTick
                    )
            );
        }
    }

    public record Snapshot(
            String stage,
            boolean finished,
            boolean passed,
            String detail,
            String resolverIp,
            String primaryIp,
            String secondaryIp,
            String zone,
            String hostname,
            String transferStatus,
            String resolutionStatus,
            String trace,
            long elapsedTicks
    ) {
    }
}
