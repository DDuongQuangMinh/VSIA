package com.k1ngtle.vsia.signality.internet.dns.physical;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.signality.internet.provider.InternetDnsRecord;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistryValidators;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.ServerRackDirectory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class PhysicalDnsService {
    private static final long RESPONSE_TIMEOUT_TICKS = 60L;
    private static final int MAX_RETRIES = 1;

    private static final Map<String, ResolveTransaction> RESOLVES =
            new ConcurrentHashMap<>();

    private static final Map<String, TransferReceive> TRANSFERS =
            new ConcurrentHashMap<>();

    private static final Map<String, String> LAST_RESOLUTION =
            new ConcurrentHashMap<>();

    private static final Map<String, String> LAST_TRACE =
            new ConcurrentHashMap<>();

    private static final Map<String, String> LAST_TRANSFER =
            new ConcurrentHashMap<>();

    private PhysicalDnsService() {
    }

    public static boolean handle(
            ServerRackBlockEntity rack,
            OSINetworkPacket packet
    ) {
        PhysicalDnsRackConfig config =
                PhysicalDnsRackConfig.read(rack);

        if (!config.physicalEnabled()
                || !rack.dnsEnabled()
                || packet.targetPort != 53) {
            return false;
        }

        if ("DNS_XFR".equalsIgnoreCase(packet.applicationProtocol)) {
            return handleTransferPacket(
                    rack,
                    config,
                    packet
            );
        }

        if (!"DNS".equalsIgnoreCase(packet.applicationProtocol)) {
            return false;
        }

        if (packet.isResponse) {
            if (config.role() == PhysicalDnsRole.RECURSIVE
                    && packet.payload.getBoolean("physical_dns")) {
                handleResolverResponse(
                        rack,
                        config,
                        packet
                );
                return true;
            }

            return false;
        }

        if (packet.payload.getBoolean("physical_dns")) {
            handleInternalQuery(
                    rack,
                    config,
                    packet
            );
            return true;
        }

        if (config.role() == PhysicalDnsRole.RECURSIVE) {
            startClientResolution(
                    rack,
                    config,
                    packet
            );
            return true;
        }

        if (config.role() == PhysicalDnsRole.AUTHORITATIVE_PRIMARY
                || config.role() == PhysicalDnsRole.AUTHORITATIVE_SECONDARY) {
            answerAuthoritativeQuery(
                    rack,
                    config,
                    packet
            );
            return true;
        }

        return false;
    }

    public static String startDiagnostic(
            ServerRackBlockEntity resolver,
            String name,
            String type
    ) {
        PhysicalDnsRackConfig config =
                PhysicalDnsRackConfig.read(resolver);

        if (config.role() != PhysicalDnsRole.RECURSIVE) {
            return "Selected ServerRack is not configured as a recursive resolver.";
        }

        if (!(resolver.getLevel() instanceof ServerLevel level)) {
            return "Resolver is not loaded on a server level.";
        }

        String normalizedName =
                normalizeName(name);

        String normalizedType =
                normalizeType(type);

        if (normalizedName.isBlank()) {
            return "DNS name cannot be blank.";
        }

        ResolveTransaction transaction =
                new ResolveTransaction(
                        UUID.randomUUID().toString(),
                        level,
                        resolver.ipAddress(),
                        null,
                        normalizedName,
                        normalizedType,
                        0,
                        config.dnssecValidation()
                );

        RESOLVES.put(
                transaction.id,
                transaction
        );

        LAST_RESOLUTION.put(
                resolver.ipAddress(),
                "RUNNING | "
                        + normalizedName
                        + " "
                        + normalizedType
        );

        LAST_TRACE.put(
                resolver.ipAddress(),
                ""
        );

        transaction.trace.add(
                "CLIENT -> RECURSIVE "
                        + resolver.ipAddress()
                        + " UDP/53 "
                        + normalizedName
                        + " "
                        + normalizedType
        );

        sendRootReferralQuery(
                resolver,
                config,
                transaction
        );

        return "Physical iterative DNS query started: "
                + normalizedName
                + " "
                + normalizedType;
    }

    public static String resolutionStatus(
            String resolverIp
    ) {
        return LAST_RESOLUTION.getOrDefault(
                resolverIp,
                "No completed physical DNS resolution."
        );
    }

    public static String resolutionTrace(
            String resolverIp
    ) {
        return LAST_TRACE.getOrDefault(
                resolverIp,
                "No physical DNS trace recorded."
        );
    }

    public static String startTransfer(
            ServerRackBlockEntity secondary,
            String requestedMode
    ) {
        PhysicalDnsRackConfig config =
                PhysicalDnsRackConfig.read(secondary);

        if (config.role() != PhysicalDnsRole.AUTHORITATIVE_SECONDARY) {
            return "Selected ServerRack is not an authoritative secondary.";
        }

        if (config.masterIp().isBlank()) {
            return "Secondary has no primary/master IP configured.";
        }

        if (!(secondary.getLevel() instanceof ServerLevel level)) {
            return "Secondary is not loaded on a server level.";
        }

        String mode =
                "IXFR".equalsIgnoreCase(requestedMode)
                        ? "IXFR"
                        : "AXFR";

        PhysicalDnsStateSavedData state =
                PhysicalDnsStateSavedData.get(level);

        long fromSerial =
                state.replica(
                        secondary.ipAddress(),
                        config.zone()
                ).map(
                        DnsZoneSnapshot::serial
                ).orElse(
                        0L
                );

        String session =
                UUID.randomUUID().toString();

        OSINetworkPacket request =
                internalPacket(
                        secondary,
                        config.masterIp(),
                        "DNS_XFR",
                        session
                );

        request.ipProtocol = 6;
        request.payload.putBoolean(
                "physical_dns",
                true
        );
        request.payload.putString(
                "xfr_kind",
                "REQUEST"
        );
        request.payload.putString(
                "zone",
                config.zone()
        );
        request.payload.putString(
                "mode",
                mode
        );
        request.payload.putLong(
                "from_serial",
                fromSerial
        );

        TRANSFERS.put(
                session,
                new TransferReceive(
                        session,
                        level,
                        secondary.ipAddress(),
                        config.zone()
                )
        );

        LAST_TRANSFER.put(
                secondary.ipAddress(),
                "RUNNING | "
                        + mode
                        + " requested from "
                        + config.masterIp()
                        + " | fromSerial="
                        + Long.toUnsignedString(
                        fromSerial
                )
        );

        secondary.physicalDnsTransmit(
                request
        );

        return mode
                + " request started over DNS/TCP semantics to "
                + config.masterIp();
    }

    public static String transferStatus(
            String secondaryIp
    ) {
        return LAST_TRANSFER.getOrDefault(
                secondaryIp,
                "No AXFR/IXFR transfer recorded."
        );
    }

    @SubscribeEvent
    public static void onServerTick(
            TickEvent.ServerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        for (ResolveTransaction transaction :
                new ArrayList<>(
                        RESOLVES.values()
                )) {
            if (transaction.finished) {
                continue;
            }

            long now =
                    transaction.level.getGameTime();

            if (now - transaction.lastSentTick
                    <= RESPONSE_TIMEOUT_TICKS) {
                continue;
            }

            ServerRackBlockEntity resolver =
                    ServerRackDirectory.byIp(
                            transaction.level,
                            transaction.resolverIp
                    );

            if (resolver == null) {
                fail(
                        transaction,
                        null,
                        "Resolver disappeared while query was active."
                );
                continue;
            }

            PhysicalDnsRackConfig config =
                    PhysicalDnsRackConfig.read(
                            resolver
                    );

            if ((transaction.stage == ResolveStage.WAIT_AUTH_DNSKEY
                    || transaction.stage == ResolveStage.WAIT_AUTH_ANSWER)
                    && !transaction.usingSecondary
                    && !transaction.authSecondaryIp.isBlank()) {
                transaction.usingSecondary = true;
                transaction.currentAuthIp =
                        transaction.authSecondaryIp;
                transaction.retries = 0;
                transaction.trace.add(
                        "TIMEOUT primary -> failover secondary "
                                + transaction.currentAuthIp
                );

                if (transaction.stage == ResolveStage.WAIT_AUTH_DNSKEY) {
                    sendAuthDnskeyQuery(
                            resolver,
                            transaction
                    );
                } else {
                    sendAuthAnswerQuery(
                            resolver,
                            transaction
                    );
                }

                continue;
            }

            if (transaction.retries < MAX_RETRIES) {
                transaction.retries++;
                transaction.trace.add(
                        "RETRY "
                                + transaction.stage
                                + " #"
                                + transaction.retries
                );

                resend(
                        resolver,
                        config,
                        transaction
                );
            } else {
                fail(
                        transaction,
                        resolver,
                        "DNS upstream timeout at "
                                + transaction.stage
                );
            }
        }
    }

    private static void startClientResolution(
            ServerRackBlockEntity resolver,
            PhysicalDnsRackConfig config,
            OSINetworkPacket clientQuery
    ) {
        if (!(resolver.getLevel() instanceof ServerLevel level)) {
            return;
        }

        String name =
                normalizeName(
                        clientQuery.payload.getString(
                                "domain"
                        )
                );

        String type =
                normalizeType(
                        clientQuery.payload.contains(
                                "query_type"
                        )
                                ? clientQuery.payload.getString(
                                "query_type"
                        )
                                : "A"
                );

        ClientEndpoint client =
                new ClientEndpoint(
                        clientQuery.sourceMac,
                        clientQuery.sourceIp,
                        clientQuery.sourcePort,
                        clientQuery.sessionId,
                        clientQuery.payload.getInt(
                                "dns_id"
                        )
                );

        ResolveTransaction transaction =
                new ResolveTransaction(
                        UUID.randomUUID().toString(),
                        level,
                        resolver.ipAddress(),
                        client,
                        name,
                        type,
                        client.dnsId,
                        config.dnssecValidation()
                );

        RESOLVES.put(
                transaction.id,
                transaction
        );

        transaction.trace.add(
                "CLIENT "
                        + client.sourceIp
                        + ":"
                        + client.sourcePort
                        + " -> RECURSIVE "
                        + resolver.ipAddress()
                        + ":53 "
                        + name
                        + " "
                        + type
        );

        sendRootReferralQuery(
                resolver,
                config,
                transaction
        );
    }

    private static void handleInternalQuery(
            ServerRackBlockEntity rack,
            PhysicalDnsRackConfig config,
            OSINetworkPacket query
    ) {
        String kind =
                query.payload.getString(
                        "pdns_kind"
                );

        switch (config.role()) {
            case ROOT -> {
                if ("ROOT_REFERRAL_QUERY".equals(kind)) {
                    answerRootReferral(
                            rack,
                            query
                    );
                }
            }

            case TLD -> {
                if ("DNSKEY_QUERY".equals(kind)) {
                    answerDnskey(
                            rack,
                            config,
                            query
                    );
                } else if ("TLD_REFERRAL_QUERY".equals(kind)) {
                    answerTldReferral(
                            rack,
                            config,
                            query
                    );
                }
            }

            case AUTHORITATIVE_PRIMARY,
                 AUTHORITATIVE_SECONDARY -> {
                if ("DNSKEY_QUERY".equals(kind)) {
                    answerDnskey(
                            rack,
                            config,
                            query
                    );
                } else if ("AUTH_QUERY".equals(kind)) {
                    answerAuthoritativeQuery(
                            rack,
                            config,
                            query
                    );
                }
            }

            default -> {
            }
        }
    }

    private static void answerRootReferral(
            ServerRackBlockEntity root,
            OSINetworkPacket query
    ) {
        if (!(root.getLevel() instanceof ServerLevel level)) {
            return;
        }

        String name =
                normalizeName(
                        query.payload.getString(
                                "domain"
                        )
                );

        String tld =
                PhysicalDnsStateSavedData.tldOf(
                        name
                );

        PhysicalDnsStateSavedData state =
                PhysicalDnsStateSavedData.get(
                        level
                );

        String tldIp =
                state.tldServer(
                        tld
                ).orElse(
                        ""
                );

        if (tldIp.isBlank()) {
            sendError(
                    root,
                    query,
                    2,
                    "No physical TLD server configured for ."
                            + tld
            );
            return;
        }

        DnssecZoneKey rootKey =
                state.ensureKey(
                        "."
                );

        DnssecZoneKey tldKey =
                state.ensureKey(
                        tld
                );

        String ds =
                DnssecEngine.ds(
                        tld,
                        tldKey
                );

        long now =
                System.currentTimeMillis()
                        / 1000L;

        long inception =
                now - 60L;

        long expiration =
                now + 86400L;

        String canonical =
                DnssecEngine.canonicalReferral(
                        ".",
                        name,
                        tld,
                        tldIp,
                        "",
                        ds,
                        inception,
                        expiration,
                        rootKey.keyTag()
                );

        OSINetworkPacket response =
                root.physicalDnsResponse(
                        query
                );

        response.payload.putBoolean(
                "physical_dns",
                true
        );
        response.payload.putString(
                "pdns_kind",
                "ROOT_REFERRAL"
        );
        response.payload.putString(
                "domain",
                name
        );
        response.payload.putString(
                "delegated_zone",
                tld
        );
        response.payload.putString(
                "primary_ip",
                tldIp
        );
        response.payload.putString(
                "secondary_ip",
                ""
        );
        response.payload.putString(
                "ds",
                ds
        );

        putSignature(
                response.payload,
                rootKey,
                canonical,
                inception,
                expiration
        );

        root.physicalDnsTransmit(
                response
        );
    }

    private static void answerDnskey(
            ServerRackBlockEntity rack,
            PhysicalDnsRackConfig config,
            OSINetworkPacket query
    ) {
        if (!(rack.getLevel() instanceof ServerLevel level)) {
            return;
        }

        String zone =
                normalizeZone(
                        query.payload.getString(
                                "zone"
                        )
                );

        if (!normalizeZone(config.zone())
                .equals(
                        zone
                )) {
            sendError(
                    rack,
                    query,
                    5,
                    "DNSKEY zone not authoritative on this server."
            );
            return;
        }

        PhysicalDnsStateSavedData state =
                PhysicalDnsStateSavedData.get(
                        level
                );

        DnssecZoneKey key =
                state.ensureKey(
                        zone
                );

        long now =
                System.currentTimeMillis()
                        / 1000L;

        long inception =
                now - 60L;

        long expiration =
                now + 86400L;

        String canonical =
                DnssecEngine.canonicalDnskey(
                        zone,
                        key,
                        inception,
                        expiration
                );

        OSINetworkPacket response =
                rack.physicalDnsResponse(
                        query
                );

        response.payload.putBoolean(
                "physical_dns",
                true
        );
        response.payload.putString(
                "pdns_kind",
                "DNSKEY_ANSWER"
        );
        response.payload.putString(
                "zone",
                zone
        );

        putSignature(
                response.payload,
                key,
                canonical,
                inception,
                expiration
        );

        rack.physicalDnsTransmit(
                response
        );
    }

    private static void answerTldReferral(
            ServerRackBlockEntity tldRack,
            PhysicalDnsRackConfig config,
            OSINetworkPacket query
    ) {
        if (!(tldRack.getLevel() instanceof ServerLevel level)) {
            return;
        }

        String name =
                normalizeName(
                        query.payload.getString(
                                "domain"
                        )
                );

        String zone =
                InternetRegistryValidators.registrableRoot(
                        name
                );

        if (zone.isBlank()) {
            sendError(
                    tldRack,
                    query,
                    3,
                    "NXDOMAIN"
            );
            return;
        }

        PhysicalDnsStateSavedData state =
                PhysicalDnsStateSavedData.get(
                        level
                );

        DnsAuthorityDelegation delegation =
                state.delegation(
                        zone
                ).orElse(
                        null
                );

        if (delegation == null) {
            sendError(
                    tldRack,
                    query,
                    3,
                    "No delegation for "
                            + zone
            );
            return;
        }

        String expectedTld =
                normalizeTld(
                        config.zone()
                );

        if (!expectedTld.equals(
                delegation.tld()
        )) {
            sendError(
                    tldRack,
                    query,
                    5,
                    "Delegation belongs to a different TLD."
            );
            return;
        }

        DnssecZoneKey tldKey =
                state.ensureKey(
                        expectedTld
                );

        DnssecZoneKey childKey =
                state.ensureKey(
                        zone
                );

        String ds =
                DnssecEngine.ds(
                        zone,
                        childKey
                );

        long now =
                System.currentTimeMillis()
                        / 1000L;

        long inception =
                now - 60L;

        long expiration =
                now + 86400L;

        String canonical =
                DnssecEngine.canonicalReferral(
                        expectedTld,
                        name,
                        zone,
                        delegation.primaryIp(),
                        delegation.secondaryIp(),
                        ds,
                        inception,
                        expiration,
                        tldKey.keyTag()
                );

        OSINetworkPacket response =
                tldRack.physicalDnsResponse(
                        query
                );

        response.payload.putBoolean(
                "physical_dns",
                true
        );
        response.payload.putString(
                "pdns_kind",
                "TLD_REFERRAL"
        );
        response.payload.putString(
                "domain",
                name
        );
        response.payload.putString(
                "delegated_zone",
                zone
        );
        response.payload.putString(
                "primary_ip",
                delegation.primaryIp()
        );
        response.payload.putString(
                "secondary_ip",
                delegation.secondaryIp()
        );
        response.payload.putString(
                "primary_ns",
                delegation.primaryNameServer()
        );
        response.payload.putString(
                "secondary_ns",
                delegation.secondaryNameServer()
        );
        response.payload.putString(
                "ds",
                ds
        );

        putSignature(
                response.payload,
                tldKey,
                canonical,
                inception,
                expiration
        );

        tldRack.physicalDnsTransmit(
                response
        );
    }

    private static void answerAuthoritativeQuery(
            ServerRackBlockEntity rack,
            PhysicalDnsRackConfig config,
            OSINetworkPacket query
    ) {
        if (!(rack.getLevel() instanceof ServerLevel level)) {
            return;
        }

        String name =
                normalizeName(
                        query.payload.contains(
                                "domain"
                        )
                                ? query.payload.getString(
                                "domain"
                        )
                                : query.payload.getString(
                                "query_name"
                        )
                );

        String type =
                normalizeType(
                        query.payload.contains(
                                "query_type"
                        )
                                ? query.payload.getString(
                                "query_type"
                        )
                                : "A"
                );

        String zone =
                normalizeZone(
                        config.zone()
                );

        if (!name.equals(zone)
                && !name.endsWith(
                "."
                        + zone
        )) {
            sendError(
                    rack,
                    query,
                    5,
                    "NOTAUTH"
            );
            return;
        }

        PhysicalDnsStateSavedData state =
                PhysicalDnsStateSavedData.get(
                        level
                );

        DnsZoneSnapshot snapshot;

        if (config.role()
                == PhysicalDnsRole.AUTHORITATIVE_PRIMARY) {
            snapshot =
                    state.observePrimary(
                            level,
                            zone
                    );
        } else {
            snapshot =
                    state.replica(
                            rack.ipAddress(),
                            zone
                    ).orElse(
                            null
                    );

            if (snapshot == null) {
                sendError(
                        rack,
                        query,
                        2,
                        "Secondary has no transferred copy of "
                                + zone
                );
                return;
            }
        }

        DnsZoneSnapshot.Resolution resolution =
                snapshot.resolve(
                        name,
                        type
                );

        InternetDnsRecord answer =
                resolution.answer();

        InternetDnsRecord cname =
                resolution.cname();

        boolean nameExists =
                snapshot.records()
                        .stream()
                        .anyMatch(
                                record ->
                                        record.name()
                                                .equalsIgnoreCase(
                                                        name
                                                )
                        );

        int rcode =
                answer == null
                        ? nameExists
                        ? 0
                        : 3
                        : 0;

        OSINetworkPacket response =
                rack.physicalDnsResponse(
                        query
                );

        response.payload.putBoolean(
                "physical_dns",
                query.payload.getBoolean(
                        "physical_dns"
                )
        );
        response.payload.putString(
                "pdns_kind",
                "AUTH_ANSWER"
        );
        response.payload.putString(
                "domain",
                name
        );
        response.payload.putString(
                "query_type",
                type
        );
        response.payload.putString(
                "zone",
                zone
        );
        response.payload.putLong(
                "zone_serial",
                snapshot.serial()
        );
        response.payload.putInt(
                "rcode",
                rcode
        );
        response.payload.putBoolean(
                "aa",
                true
        );

        if (answer != null) {
            response.payload.putString(
                    "record_name",
                    answer.name()
            );
            response.payload.putString(
                    "record_type",
                    answer.type()
            );
            response.payload.putString(
                    "answer",
                    answer.answerValue()
            );
            response.payload.putInt(
                    "ttl",
                    answer.ttlSeconds()
            );
            response.payload.putString(
                    "record_signature",
                    snapshot.signatureFor(
                            answer
                    )
            );
        } else {
            response.payload.putString(
                    "record_name",
                    name
            );
            response.payload.putString(
                    "record_type",
                    type
            );
            response.payload.putString(
                    "answer",
                    ""
            );
            response.payload.putInt(
                    "ttl",
                    0
            );
        }

        if (cname != null) {
            response.payload.putString(
                    "cname_name",
                    cname.name()
            );
            response.payload.putString(
                    "cname_value",
                    cname.value()
            );
            response.payload.putInt(
                    "cname_ttl",
                    cname.ttlSeconds()
            );
            response.payload.putString(
                    "cname_signature",
                    snapshot.signatureFor(
                            cname
                    )
            );
        }

        DnssecZoneKey key =
                state.ensureKey(
                        zone
                );

        long inception =
                snapshot.generatedAtMillis()
                        / 1000L
                        - 60L;

        long expiration =
                snapshot.generatedAtMillis()
                        / 1000L
                        + 86400L;

        response.payload.putString(
                "dnssec_dnskey",
                key.presentation()
        );
        response.payload.putString(
                "dnssec_public_x509",
                key.publicKeyX509Base64()
        );
        response.payload.putString(
                "dnssec_public_raw",
                key.dnskeyPublicBase64()
        );
        response.payload.putInt(
                "dnssec_key_tag",
                key.keyTag()
        );
        response.payload.putInt(
                "dnssec_flags",
                key.flags()
        );
        response.payload.putInt(
                "dnssec_protocol",
                key.protocol()
        );
        response.payload.putInt(
                "dnssec_algorithm",
                key.algorithm()
        );
        response.payload.putLong(
                "dnssec_inception",
                inception
        );
        response.payload.putLong(
                "dnssec_expiration",
                expiration
        );

        if (!query.payload.getBoolean(
                "physical_dns"
        )) {
            response.payload.putInt(
                    "dns_id",
                    query.payload.getInt(
                            "dns_id"
                    )
            );
            response.payload.putString(
                    "resolved_ip",
                    answer != null
                            && "A".equalsIgnoreCase(
                            answer.type()
                    )
                            ? answer.value()
                            : "0.0.0.0"
            );
        }

        rack.physicalDnsTransmit(
                response
        );
    }

    private static void handleResolverResponse(
            ServerRackBlockEntity resolver,
            PhysicalDnsRackConfig config,
            OSINetworkPacket response
    ) {
        ResolveTransaction transaction =
                RESOLVES.get(
                        response.sessionId
                );

        if (transaction == null
                || transaction.finished) {
            return;
        }

        int rcode =
                response.payload.getInt(
                        "rcode"
                );

        if (rcode != 0
                && response.payload.contains(
                "rcode"
        )) {
            fail(
                    transaction,
                    resolver,
                    "Upstream DNS error rcode="
                            + rcode
                            + " detail="
                            + response.payload.getString(
                            "detail"
                    )
            );
            return;
        }

        String kind =
                response.payload.getString(
                        "pdns_kind"
                );

        switch (transaction.stage) {
            case WAIT_ROOT_REFERRAL -> {
                if (!"ROOT_REFERRAL".equals(kind)) {
                    return;
                }

                if (transaction.dnssec
                        && !verifyRootReferral(
                        config,
                        transaction,
                        response
                )) {
                    fail(
                            transaction,
                            resolver,
                            "DNSSEC root trust-anchor/referral validation failed."
                    );
                    return;
                }

                transaction.tldZone =
                        response.payload.getString(
                                "delegated_zone"
                        );

                transaction.tldIp =
                        response.payload.getString(
                                "primary_ip"
                        );

                transaction.expectedTldDs =
                        response.payload.getString(
                                "ds"
                        );

                transaction.trace.add(
                        "ROOT "
                                + response.sourceIp
                                + " -> REFERRAL ."
                                + transaction.tldZone
                                + " glue="
                                + transaction.tldIp
                                + " DS="
                                + transaction.expectedTldDs
                );

                sendTldDnskeyQuery(
                        resolver,
                        transaction
                );
            }

            case WAIT_TLD_DNSKEY -> {
                if (!"DNSKEY_ANSWER".equals(kind)) {
                    return;
                }

                if (transaction.dnssec
                        && !verifyChildDnskey(
                        transaction.tldZone,
                        transaction.expectedTldDs,
                        response
                )) {
                    fail(
                            transaction,
                            resolver,
                            "DNSSEC TLD DNSKEY/DS validation failed."
                    );
                    return;
                }

                transaction.tldPublicX509 =
                        response.payload.getString(
                                "dnssec_public_x509"
                        );

                transaction.tldKeyTag =
                        response.payload.getInt(
                                "dnssec_key_tag"
                        );

                transaction.trace.add(
                        "TLD ."
                                + transaction.tldZone
                                + " DNSKEY validated against parent DS"
                );

                sendTldReferralQuery(
                        resolver,
                        transaction
                );
            }

            case WAIT_TLD_REFERRAL -> {
                if (!"TLD_REFERRAL".equals(kind)) {
                    return;
                }

                if (transaction.dnssec
                        && !verifyTldReferral(
                        transaction,
                        response
                )) {
                    fail(
                            transaction,
                            resolver,
                            "DNSSEC signed TLD referral validation failed."
                    );
                    return;
                }

                transaction.authZone =
                        response.payload.getString(
                                "delegated_zone"
                        );

                transaction.authPrimaryIp =
                        response.payload.getString(
                                "primary_ip"
                        );

                transaction.authSecondaryIp =
                        response.payload.getString(
                                "secondary_ip"
                        );

                transaction.currentAuthIp =
                        transaction.authPrimaryIp;

                transaction.expectedAuthDs =
                        response.payload.getString(
                                "ds"
                        );

                transaction.trace.add(
                        "TLD ."
                                + transaction.tldZone
                                + " -> REFERRAL "
                                + transaction.authZone
                                + " primary="
                                + transaction.authPrimaryIp
                                + " secondary="
                                + transaction.authSecondaryIp
                                + " DS="
                                + transaction.expectedAuthDs
                );

                sendAuthDnskeyQuery(
                        resolver,
                        transaction
                );
            }

            case WAIT_AUTH_DNSKEY -> {
                if (!"DNSKEY_ANSWER".equals(kind)) {
                    return;
                }

                if (transaction.dnssec
                        && !verifyChildDnskey(
                        transaction.authZone,
                        transaction.expectedAuthDs,
                        response
                )) {
                    fail(
                            transaction,
                            resolver,
                            "DNSSEC authoritative DNSKEY/DS validation failed."
                    );
                    return;
                }

                transaction.authPublicX509 =
                        response.payload.getString(
                                "dnssec_public_x509"
                        );

                transaction.authKeyTag =
                        response.payload.getInt(
                                "dnssec_key_tag"
                        );

                transaction.trace.add(
                        "AUTH "
                                + transaction.currentAuthIp
                                + " DNSKEY validated against TLD DS"
                );

                sendAuthAnswerQuery(
                        resolver,
                        transaction
                );
            }

            case WAIT_AUTH_ANSWER -> {
                if (!"AUTH_ANSWER".equals(kind)) {
                    return;
                }

                if (transaction.dnssec
                        && !verifyAuthoritativeAnswer(
                        transaction,
                        response
                )) {
                    fail(
                            transaction,
                            resolver,
                            "DNSSEC authoritative RRSIG validation failed."
                    );
                    return;
                }

                transaction.trace.add(
                        "AUTH "
                                + response.sourceIp
                                + " -> "
                                + transaction.name
                                + " "
                                + transaction.type
                                + " answer="
                                + response.payload.getString(
                                "answer"
                        )
                                + " rcode="
                                + response.payload.getInt(
                                "rcode"
                        )
                                + (
                                transaction.dnssec
                                        ? " AD=1"
                                        : ""
                        )
                );

                finish(
                        transaction,
                        resolver,
                        response
                );
            }

            default -> {
            }
        }
    }

    private static void sendRootReferralQuery(
            ServerRackBlockEntity resolver,
            PhysicalDnsRackConfig config,
            ResolveTransaction transaction
    ) {
        if (config.rootHintIp().isBlank()) {
            fail(
                    transaction,
                    resolver,
                    "Resolver has no root hint."
            );
            return;
        }

        OSINetworkPacket packet =
                internalPacket(
                        resolver,
                        config.rootHintIp(),
                        "DNS",
                        transaction.id
                );

        packet.ipProtocol = 17;
        packet.payload.putBoolean(
                "physical_dns",
                true
        );
        packet.payload.putString(
                "pdns_kind",
                "ROOT_REFERRAL_QUERY"
        );
        packet.payload.putString(
                "domain",
                transaction.name
        );
        packet.payload.putString(
                "query_type",
                transaction.type
        );
        packet.payload.putBoolean(
                "rd",
                false
        );
        packet.payload.putBoolean(
                "do",
                transaction.dnssec
        );

        transaction.stage =
                ResolveStage.WAIT_ROOT_REFERRAL;

        markSent(
                transaction
        );

        transaction.trace.add(
                "RECURSIVE "
                        + resolver.ipAddress()
                        + " -> ROOT "
                        + config.rootHintIp()
                        + " UDP/53 iterative"
        );

        resolver.physicalDnsTransmit(
                packet
        );
    }

    private static void sendTldDnskeyQuery(
            ServerRackBlockEntity resolver,
            ResolveTransaction transaction
    ) {
        OSINetworkPacket packet =
                internalPacket(
                        resolver,
                        transaction.tldIp,
                        "DNS",
                        transaction.id
                );

        packet.ipProtocol = 17;
        packet.payload.putBoolean(
                "physical_dns",
                true
        );
        packet.payload.putString(
                "pdns_kind",
                "DNSKEY_QUERY"
        );
        packet.payload.putString(
                "zone",
                transaction.tldZone
        );
        packet.payload.putString(
                "query_type",
                "DNSKEY"
        );
        packet.payload.putBoolean(
                "do",
                true
        );

        transaction.stage =
                ResolveStage.WAIT_TLD_DNSKEY;

        markSent(
                transaction
        );

        transaction.trace.add(
                "RECURSIVE -> TLD "
                        + transaction.tldIp
                        + " DNSKEY "
                        + transaction.tldZone
        );

        resolver.physicalDnsTransmit(
                packet
        );
    }

    private static void sendTldReferralQuery(
            ServerRackBlockEntity resolver,
            ResolveTransaction transaction
    ) {
        OSINetworkPacket packet =
                internalPacket(
                        resolver,
                        transaction.tldIp,
                        "DNS",
                        transaction.id
                );

        packet.ipProtocol = 17;
        packet.payload.putBoolean(
                "physical_dns",
                true
        );
        packet.payload.putString(
                "pdns_kind",
                "TLD_REFERRAL_QUERY"
        );
        packet.payload.putString(
                "domain",
                transaction.name
        );
        packet.payload.putString(
                "query_type",
                transaction.type
        );
        packet.payload.putBoolean(
                "rd",
                false
        );
        packet.payload.putBoolean(
                "do",
                transaction.dnssec
        );

        transaction.stage =
                ResolveStage.WAIT_TLD_REFERRAL;

        markSent(
                transaction
        );

        transaction.trace.add(
                "RECURSIVE -> TLD "
                        + transaction.tldIp
                        + " referral query "
                        + transaction.name
        );

        resolver.physicalDnsTransmit(
                packet
        );
    }

    private static void sendAuthDnskeyQuery(
            ServerRackBlockEntity resolver,
            ResolveTransaction transaction
    ) {
        OSINetworkPacket packet =
                internalPacket(
                        resolver,
                        transaction.currentAuthIp,
                        "DNS",
                        transaction.id
                );

        packet.ipProtocol = 17;
        packet.payload.putBoolean(
                "physical_dns",
                true
        );
        packet.payload.putString(
                "pdns_kind",
                "DNSKEY_QUERY"
        );
        packet.payload.putString(
                "zone",
                transaction.authZone
        );
        packet.payload.putString(
                "query_type",
                "DNSKEY"
        );
        packet.payload.putBoolean(
                "do",
                true
        );

        transaction.stage =
                ResolveStage.WAIT_AUTH_DNSKEY;

        markSent(
                transaction
        );

        transaction.trace.add(
                "RECURSIVE -> AUTH "
                        + transaction.currentAuthIp
                        + " DNSKEY "
                        + transaction.authZone
                + (
                transaction.usingSecondary
                        ? " [SECONDARY]"
                        : " [PRIMARY]"
        )
        );

        resolver.physicalDnsTransmit(
                packet
        );
    }

    private static void sendAuthAnswerQuery(
            ServerRackBlockEntity resolver,
            ResolveTransaction transaction
    ) {
        OSINetworkPacket packet =
                internalPacket(
                        resolver,
                        transaction.currentAuthIp,
                        "DNS",
                        transaction.id
                );

        packet.ipProtocol = 17;
        packet.payload.putBoolean(
                "physical_dns",
                true
        );
        packet.payload.putString(
                "pdns_kind",
                "AUTH_QUERY"
        );
        packet.payload.putString(
                "domain",
                transaction.name
        );
        packet.payload.putString(
                "query_type",
                transaction.type
        );
        packet.payload.putBoolean(
                "rd",
                false
        );
        packet.payload.putBoolean(
                "do",
                transaction.dnssec
        );

        transaction.stage =
                ResolveStage.WAIT_AUTH_ANSWER;

        markSent(
                transaction
        );

        transaction.trace.add(
                "RECURSIVE -> AUTH "
                        + transaction.currentAuthIp
                        + " "
                        + transaction.name
                        + " "
                        + transaction.type
        );

        resolver.physicalDnsTransmit(
                packet
        );
    }

    private static void resend(
            ServerRackBlockEntity resolver,
            PhysicalDnsRackConfig config,
            ResolveTransaction transaction
    ) {
        switch (transaction.stage) {
            case WAIT_ROOT_REFERRAL ->
                    sendRootReferralQuery(
                            resolver,
                            config,
                            transaction
                    );

            case WAIT_TLD_DNSKEY ->
                    sendTldDnskeyQuery(
                            resolver,
                            transaction
                    );

            case WAIT_TLD_REFERRAL ->
                    sendTldReferralQuery(
                            resolver,
                            transaction
                    );

            case WAIT_AUTH_DNSKEY ->
                    sendAuthDnskeyQuery(
                            resolver,
                            transaction
                    );

            case WAIT_AUTH_ANSWER ->
                    sendAuthAnswerQuery(
                            resolver,
                            transaction
                    );

            default -> {
            }
        }
    }

    private static boolean verifyRootReferral(
            PhysicalDnsRackConfig config,
            ResolveTransaction transaction,
            OSINetworkPacket response
    ) {
        String actualRootDs =
                dsFromPacket(
                        ".",
                        response.payload
                );

        if (config.rootTrustAnchor().isBlank()
                || !config.rootTrustAnchor()
                .equalsIgnoreCase(
                        actualRootDs
                )) {
            return false;
        }

        return verifyReferralPacket(
                ".",
                transaction.name,
                response
        );
    }

    private static boolean verifyTldReferral(
            ResolveTransaction transaction,
            OSINetworkPacket response
    ) {
        if (transaction.tldPublicX509.isBlank()) {
            return false;
        }

        return verifyReferralPacketWithPublicKey(
                transaction.tldZone,
                transaction.name,
                response,
                transaction.tldPublicX509
        );
    }

    private static boolean verifyChildDnskey(
            String zone,
            String expectedDs,
            OSINetworkPacket response
    ) {
        String actualDs =
                dsFromPacket(
                        zone,
                        response.payload
                );

        if (!expectedDs.equalsIgnoreCase(
                actualDs
        )) {
            return false;
        }

        long inception =
                response.payload.getLong(
                        "dnssec_inception"
                );

        long expiration =
                response.payload.getLong(
                        "dnssec_expiration"
                );

        long now =
                System.currentTimeMillis()
                        / 1000L;

        if (!DnssecEngine.signatureTimeValid(
                inception,
                expiration,
                now
        )) {
            return false;
        }

        DnssecZoneKey key =
                keyFromPacket(
                        zone,
                        response.payload
                );

        String canonical =
                DnssecEngine.canonicalDnskey(
                        zone,
                        key,
                        inception,
                        expiration
                );

        return DnssecEngine.verify(
                canonical,
                response.payload.getString(
                        "dnssec_rrsig"
                ),
                response.payload.getString(
                        "dnssec_public_x509"
                )
        );
    }

    private static boolean verifyAuthoritativeAnswer(
            ResolveTransaction transaction,
            OSINetworkPacket response
    ) {
        int rcode =
                response.payload.getInt(
                        "rcode"
                );

        if (rcode == 3) {
            return true;
        }

        String answer =
                response.payload.getString(
                        "answer"
                );

        if (answer.isBlank()) {
            return true;
        }

        long inception =
                response.payload.getLong(
                        "dnssec_inception"
                );

        long expiration =
                response.payload.getLong(
                        "dnssec_expiration"
                );

        long now =
                System.currentTimeMillis()
                        / 1000L;

        if (!DnssecEngine.signatureTimeValid(
                inception,
                expiration,
                now
        )) {
            return false;
        }

        String canonical =
                DnssecEngine.canonicalAnswer(
                        response.payload.getString(
                                "zone"
                        ),
                        response.payload.getLong(
                                "zone_serial"
                        ),
                        response.payload.getString(
                                "record_name"
                        ),
                        response.payload.getString(
                                "record_type"
                        ),
                        response.payload.getInt(
                                "ttl"
                        ),
                        answer,
                        inception,
                        expiration,
                        transaction.authKeyTag
                );

        boolean answerValid =
                DnssecEngine.verify(
                        canonical,
                        response.payload.getString(
                                "record_signature"
                        ),
                        transaction.authPublicX509
                );

        if (!answerValid) {
            return false;
        }

        String cname =
                response.payload.getString(
                        "cname_value"
                );

        if (!cname.isBlank()) {
            String cnameCanonical =
                    DnssecEngine.canonicalAnswer(
                            response.payload.getString(
                                    "zone"
                            ),
                            response.payload.getLong(
                                    "zone_serial"
                            ),
                            response.payload.getString(
                                    "cname_name"
                            ),
                            "CNAME",
                            response.payload.getInt(
                                    "cname_ttl"
                            ),
                            cname,
                            inception,
                            expiration,
                            transaction.authKeyTag
                    );

            return DnssecEngine.verify(
                    cnameCanonical,
                    response.payload.getString(
                            "cname_signature"
                    ),
                    transaction.authPublicX509
            );
        }

        return true;
    }

    private static boolean verifyReferralPacket(
            String signerZone,
            String queryName,
            OSINetworkPacket response
    ) {
        return verifyReferralPacketWithPublicKey(
                signerZone,
                queryName,
                response,
                response.payload.getString(
                        "dnssec_public_x509"
                )
        );
    }

    private static boolean verifyReferralPacketWithPublicKey(
            String signerZone,
            String queryName,
            OSINetworkPacket response,
            String publicKey
    ) {
        long inception =
                response.payload.getLong(
                        "dnssec_inception"
                );

        long expiration =
                response.payload.getLong(
                        "dnssec_expiration"
                );

        long now =
                System.currentTimeMillis()
                        / 1000L;

        if (!DnssecEngine.signatureTimeValid(
                inception,
                expiration,
                now
        )) {
            return false;
        }

        String canonical =
                DnssecEngine.canonicalReferral(
                        signerZone,
                        queryName,
                        response.payload.getString(
                                "delegated_zone"
                        ),
                        response.payload.getString(
                                "primary_ip"
                        ),
                        response.payload.getString(
                                "secondary_ip"
                        ),
                        response.payload.getString(
                                "ds"
                        ),
                        inception,
                        expiration,
                        response.payload.getInt(
                                "dnssec_key_tag"
                        )
                );

        return DnssecEngine.verify(
                canonical,
                response.payload.getString(
                        "dnssec_rrsig"
                ),
                publicKey
        );
    }

    private static String dsFromPacket(
            String zone,
            CompoundTag payload
    ) {
        try {
            return DnssecEngine.dsFromPresentation(
                    zone,
                    payload.getInt(
                            "dnssec_flags"
                    ),
                    payload.getInt(
                            "dnssec_protocol"
                    ),
                    payload.getInt(
                            "dnssec_algorithm"
                    ),
                    payload.getInt(
                            "dnssec_key_tag"
                    ),
                    payload.getString(
                            "dnssec_public_raw"
                    )
            );
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static DnssecZoneKey keyFromPacket(
            String zone,
            CompoundTag payload
    ) {
        return new DnssecZoneKey(
                normalizeZone(zone),
                payload.getInt(
                        "dnssec_flags"
                ),
                payload.getInt(
                        "dnssec_protocol"
                ),
                payload.getInt(
                        "dnssec_algorithm"
                ),
                payload.getInt(
                        "dnssec_key_tag"
                ),
                payload.getString(
                        "dnssec_public_x509"
                ),
                "",
                payload.getString(
                        "dnssec_public_raw"
                ),
                0L
        );
    }

    private static void putSignature(
            CompoundTag payload,
            DnssecZoneKey key,
            String canonical,
            long inception,
            long expiration
    ) {
        payload.putString(
                "dnssec_dnskey",
                key.presentation()
        );
        payload.putString(
                "dnssec_public_x509",
                key.publicKeyX509Base64()
        );
        payload.putString(
                "dnssec_public_raw",
                key.dnskeyPublicBase64()
        );
        payload.putInt(
                "dnssec_key_tag",
                key.keyTag()
        );
        payload.putInt(
                "dnssec_flags",
                key.flags()
        );
        payload.putInt(
                "dnssec_protocol",
                key.protocol()
        );
        payload.putInt(
                "dnssec_algorithm",
                key.algorithm()
        );
        payload.putLong(
                "dnssec_inception",
                inception
        );
        payload.putLong(
                "dnssec_expiration",
                expiration
        );
        payload.putString(
                "dnssec_rrsig",
                DnssecEngine.sign(
                        canonical,
                        key
                )
        );
    }

    private static void finish(
            ResolveTransaction transaction,
            ServerRackBlockEntity resolver,
            OSINetworkPacket upstream
    ) {
        transaction.finished = true;
        transaction.stage =
                ResolveStage.COMPLETE;

        String answer =
                upstream.payload.getString(
                        "answer"
                );

        int rcode =
                upstream.payload.getInt(
                        "rcode"
                );

        String result =
                "PASS | "
                        + transaction.name
                        + " "
                        + transaction.type
                        + " -> "
                        + (
                        answer.isBlank()
                                ? "<no-data>"
                                : answer
                )
                        + " | rcode="
                        + rcode
                        + " | hops="
                        + transaction.trace.size()
                        + " | DNSSEC="
                        + (
                        transaction.dnssec
                                ? "VALIDATED"
                                : "OFF"
                )
                        + " | authority="
                        + transaction.currentAuthIp;

        LAST_RESOLUTION.put(
                transaction.resolverIp,
                result
        );

        LAST_TRACE.put(
                transaction.resolverIp,
                String.join(
                        "\n",
                        transaction.trace
                )
        );

        if (transaction.client != null
                && resolver != null) {
            OSINetworkPacket response =
                    new OSINetworkPacket();

            response.sourceMac =
                    resolver.physicalDnsMacAddress();

            response.targetMac =
                    transaction.client.sourceMac;

            response.sourceIp =
                    resolver.ipAddress();

            response.targetIp =
                    transaction.client.sourceIp;

            response.sourcePort =
                    53;

            response.targetPort =
                    transaction.client.sourcePort;

            response.ipProtocol =
                    17;

            response.applicationProtocol =
                    "DNS";

            response.isResponse =
                    true;

            response.sessionId =
                    transaction.client.sessionId;

            response.payload.putInt(
                    "dns_id",
                    transaction.client.dnsId
            );
            response.payload.putString(
                    "domain",
                    transaction.name
            );
            response.payload.putString(
                    "query_type",
                    transaction.type
            );
            response.payload.putString(
                    "record_type",
                    upstream.payload.getString(
                            "record_type"
                    )
            );
            response.payload.putString(
                    "answer",
                    answer
            );
            response.payload.putString(
                    "resolved_ip",
                    "A".equals(
                            transaction.type
                    )
                            && !answer.isBlank()
                            ? answer
                            : "0.0.0.0"
            );
            response.payload.putInt(
                    "ttl",
                    upstream.payload.getInt(
                            "ttl"
                    )
            );
            response.payload.putInt(
                    "rcode",
                    rcode
            );
            response.payload.putBoolean(
                    "ad",
                    transaction.dnssec
            );
            response.payload.putBoolean(
                    "dnssec_valid",
                    transaction.dnssec
            );
            response.payload.putString(
                    "authority_ip",
                    transaction.currentAuthIp
            );
            response.payload.putInt(
                    "referral_hops",
                    transaction.trace.size()
            );

            resolver.physicalDnsTransmit(
                    response
            );
        }

        RESOLVES.remove(
                transaction.id
        );
    }

    private static void fail(
            ResolveTransaction transaction,
            ServerRackBlockEntity resolver,
            String detail
    ) {
        transaction.finished = true;
        transaction.stage =
                ResolveStage.FAILED;

        transaction.trace.add(
                "FAIL "
                        + detail
        );

        LAST_RESOLUTION.put(
                transaction.resolverIp,
                "FAIL | "
                        + detail
        );

        LAST_TRACE.put(
                transaction.resolverIp,
                String.join(
                        "\n",
                        transaction.trace
                )
        );

        if (transaction.client != null
                && resolver != null) {
            OSINetworkPacket response =
                    new OSINetworkPacket();

            response.sourceMac =
                    resolver.physicalDnsMacAddress();

            response.targetMac =
                    transaction.client.sourceMac;

            response.sourceIp =
                    resolver.ipAddress();

            response.targetIp =
                    transaction.client.sourceIp;

            response.sourcePort =
                    53;

            response.targetPort =
                    transaction.client.sourcePort;

            response.ipProtocol =
                    17;

            response.applicationProtocol =
                    "DNS";

            response.isResponse =
                    true;

            response.sessionId =
                    transaction.client.sessionId;

            response.payload.putInt(
                    "dns_id",
                    transaction.client.dnsId
            );
            response.payload.putString(
                    "domain",
                    transaction.name
            );
            response.payload.putString(
                    "query_type",
                    transaction.type
            );
            response.payload.putString(
                    "record_type",
                    transaction.type
            );
            response.payload.putString(
                    "answer",
                    ""
            );
            response.payload.putString(
                    "resolved_ip",
                    "0.0.0.0"
            );
            response.payload.putInt(
                    "ttl",
                    0
            );
            response.payload.putInt(
                    "rcode",
                    2
            );
            response.payload.putBoolean(
                    "ad",
                    false
            );
            response.payload.putBoolean(
                    "dnssec_valid",
                    false
            );
            response.payload.putString(
                    "detail",
                    detail
            );

            resolver.physicalDnsTransmit(
                    response
            );
        }

        RESOLVES.remove(
                transaction.id
        );
    }

    private static boolean handleTransferPacket(
            ServerRackBlockEntity rack,
            PhysicalDnsRackConfig config,
            OSINetworkPacket packet
    ) {
        if (!packet.payload.getBoolean(
                "physical_dns"
        )) {
            return false;
        }

        if (!packet.isResponse
                && "REQUEST".equals(
                packet.payload.getString(
                        "xfr_kind"
                )
        )
                && config.role()
                == PhysicalDnsRole.AUTHORITATIVE_PRIMARY) {
            answerTransferRequest(
                    rack,
                    config,
                    packet
            );
            return true;
        }

        if (packet.isResponse
                && config.role()
                == PhysicalDnsRole.AUTHORITATIVE_SECONDARY) {
            receiveTransferChunk(
                    rack,
                    config,
                    packet
            );
            return true;
        }

        return true;
    }

    private static void answerTransferRequest(
            ServerRackBlockEntity primary,
            PhysicalDnsRackConfig config,
            OSINetworkPacket request
    ) {
        if (!(primary.getLevel() instanceof ServerLevel level)) {
            return;
        }

        String zone =
                normalizeZone(
                        request.payload.getString(
                                "zone"
                        )
                );

        if (!zone.equals(
                normalizeZone(
                        config.zone()
                )
        )) {
            sendTransferError(
                    primary,
                    request,
                    "NOTAUTH"
            );
            return;
        }

        PhysicalDnsStateSavedData state =
                PhysicalDnsStateSavedData.get(
                        level
                );

        DnsZoneSnapshot current =
                state.observePrimary(
                        level,
                        zone
                );

        String requestedMode =
                request.payload.getString(
                        "mode"
                ).toUpperCase(
                        Locale.ROOT
                );

        long fromSerial =
                request.payload.getLong(
                        "from_serial"
                );

        PhysicalDnsStateSavedData.Delta delta =
                "IXFR".equals(
                        requestedMode
                )
                        ? state.delta(
                        zone,
                        fromSerial,
                        current
                )
                        : new PhysicalDnsStateSavedData.Delta(
                        false,
                        List.of(),
                        List.of()
                );

        String mode =
                "IXFR".equals(
                        requestedMode
                )
                        && delta.ixfrAvailable()
                        ? "IXFR"
                        : "AXFR";

        List<TransferItem> items =
                new ArrayList<>();

        if ("IXFR".equals(
                mode
        )) {
            for (InternetDnsRecord record :
                    delta.deleted()) {
                items.add(
                        new TransferItem(
                                "DELETE",
                                record,
                                ""
                        )
                );
            }

            for (InternetDnsRecord record :
                    delta.added()) {
                items.add(
                        new TransferItem(
                                "ADD",
                                record,
                                current.signatureFor(
                                        record
                                )
                        )
                );
            }
        } else {
            for (InternetDnsRecord record :
                    current.records()) {
                items.add(
                        new TransferItem(
                                "ADD",
                                record,
                                current.signatureFor(
                                        record
                                )
                        )
                );
            }
        }

        int total =
                items.size()
                        + 2;

        sendTransferBegin(
                primary,
                request,
                mode,
                fromSerial,
                current,
                total
        );

        int sequence =
                1;

        for (TransferItem item :
                items) {
            sendTransferData(
                    primary,
                    request,
                    mode,
                    current,
                    total,
                    sequence++,
                    item
            );
        }

        sendTransferEnd(
                primary,
                request,
                mode,
                current,
                total,
                sequence
        );
    }

    private static void sendTransferBegin(
            ServerRackBlockEntity primary,
            OSINetworkPacket request,
            String mode,
            long fromSerial,
            DnsZoneSnapshot current,
            int total
    ) {
        OSINetworkPacket packet =
                primary.physicalDnsResponse(
                        request
                );

        packet.applicationProtocol =
                "DNS_XFR";

        packet.ipProtocol =
                6;

        packet.payload.putBoolean(
                "physical_dns",
                true
        );
        packet.payload.putString(
                "xfr_kind",
                "BEGIN"
        );
        packet.payload.putString(
                "mode",
                mode
        );
        packet.payload.putString(
                "zone",
                current.zone()
        );
        packet.payload.putLong(
                "from_serial",
                fromSerial
        );
        packet.payload.putLong(
                "to_serial",
                current.serial()
        );
        packet.payload.putString(
                "fingerprint",
                current.fingerprint()
        );
        packet.payload.putLong(
                "generated_at",
                current.generatedAtMillis()
        );
        packet.payload.putInt(
                "sequence",
                0
        );
        packet.payload.putInt(
                "total",
                total
        );

        primary.physicalDnsTransmit(
                packet
        );
    }

    private static void sendTransferData(
            ServerRackBlockEntity primary,
            OSINetworkPacket request,
            String mode,
            DnsZoneSnapshot current,
            int total,
            int sequence,
            TransferItem item
    ) {
        OSINetworkPacket packet =
                primary.physicalDnsResponse(
                        request
                );

        packet.applicationProtocol =
                "DNS_XFR";

        packet.ipProtocol =
                6;

        packet.payload.putBoolean(
                "physical_dns",
                true
        );
        packet.payload.putString(
                "xfr_kind",
                "DATA"
        );
        packet.payload.putString(
                "mode",
                mode
        );
        packet.payload.putString(
                "zone",
                current.zone()
        );
        packet.payload.putLong(
                "to_serial",
                current.serial()
        );
        packet.payload.putInt(
                "sequence",
                sequence
        );
        packet.payload.putInt(
                "total",
                total
        );
        packet.payload.putString(
                "operation",
                item.operation
        );
        packet.payload.put(
                "record",
                item.record.save()
        );
        packet.payload.putString(
                "record_signature",
                item.signature
        );

        primary.physicalDnsTransmit(
                packet
        );
    }

    private static void sendTransferEnd(
            ServerRackBlockEntity primary,
            OSINetworkPacket request,
            String mode,
            DnsZoneSnapshot current,
            int total,
            int sequence
    ) {
        OSINetworkPacket packet =
                primary.physicalDnsResponse(
                        request
                );

        packet.applicationProtocol =
                "DNS_XFR";

        packet.ipProtocol =
                6;

        packet.payload.putBoolean(
                "physical_dns",
                true
        );
        packet.payload.putString(
                "xfr_kind",
                "END"
        );
        packet.payload.putString(
                "mode",
                mode
        );
        packet.payload.putString(
                "zone",
                current.zone()
        );
        packet.payload.putLong(
                "to_serial",
                current.serial()
        );
        packet.payload.putInt(
                "sequence",
                sequence
        );
        packet.payload.putInt(
                "total",
                total
        );

        primary.physicalDnsTransmit(
                packet
        );
    }

    private static void sendTransferError(
            ServerRackBlockEntity primary,
            OSINetworkPacket request,
            String detail
    ) {
        OSINetworkPacket packet =
                primary.physicalDnsResponse(
                        request
                );

        packet.applicationProtocol =
                "DNS_XFR";

        packet.ipProtocol =
                6;

        packet.payload.putBoolean(
                "physical_dns",
                true
        );
        packet.payload.putString(
                "xfr_kind",
                "ERROR"
        );
        packet.payload.putString(
                "detail",
                detail
        );

        primary.physicalDnsTransmit(
                packet
        );
    }

    private static void receiveTransferChunk(
            ServerRackBlockEntity secondary,
            PhysicalDnsRackConfig config,
            OSINetworkPacket packet
    ) {
        TransferReceive receive =
                TRANSFERS.get(
                        packet.sessionId
                );

        if (receive == null) {
            return;
        }

        String kind =
                packet.payload.getString(
                        "xfr_kind"
                );

        if ("ERROR".equals(
                kind
        )) {
            LAST_TRANSFER.put(
                    secondary.ipAddress(),
                    "FAIL | "
                            + packet.payload.getString(
                            "detail"
                    )
            );

            TRANSFERS.remove(
                    packet.sessionId
            );
            return;
        }

        if ("BEGIN".equals(
                kind
        )) {
            receive.mode =
                    packet.payload.getString(
                            "mode"
                    );

            receive.toSerial =
                    packet.payload.getLong(
                            "to_serial"
                    );

            receive.fingerprint =
                    packet.payload.getString(
                            "fingerprint"
                    );

            receive.generatedAt =
                    packet.payload.getLong(
                            "generated_at"
                    );

            receive.total =
                    packet.payload.getInt(
                            "total"
                    );

            receive.chunks.put(
                    0,
                    packet.payload.copy()
            );

            return;
        }

        int sequence =
                packet.payload.getInt(
                        "sequence"
                );

        receive.chunks.put(
                sequence,
                packet.payload.copy()
        );

        boolean hasEnd =
                receive.chunks.values()
                        .stream()
                        .anyMatch(
                                chunk ->
                                        "END".equals(
                                                chunk.getString(
                                                        "xfr_kind"
                                                )
                                        )
                        );

        if (receive.total <= 0
                || receive.chunks.size() < receive.total
                || !hasEnd) {
            LAST_TRANSFER.put(
                    secondary.ipAddress(),
                    "WAITING | received="
                            + receive.chunks.size()
                            + "/"
                            + receive.total
            );
            return;
        }

        applyTransfer(
                secondary,
                config,
                receive
        );

        TRANSFERS.remove(
                packet.sessionId
        );
    }

    private static void applyTransfer(
            ServerRackBlockEntity secondary,
            PhysicalDnsRackConfig config,
            TransferReceive receive
    ) {
        PhysicalDnsStateSavedData state =
                PhysicalDnsStateSavedData.get(
                        receive.level
                );

        Map<String, InternetDnsRecord> records =
                new LinkedHashMap<>();

        Map<String, String> signatures =
                new LinkedHashMap<>();

        if ("IXFR".equals(
                receive.mode
        )) {
            DnsZoneSnapshot existing =
                    state.replica(
                            secondary.ipAddress(),
                            config.zone()
                    ).orElse(
                            null
                    );

            if (existing == null) {
                LAST_TRANSFER.put(
                        secondary.ipAddress(),
                        "FAIL | IXFR has no local base replica."
                );
                return;
            }

            records.putAll(
                    existing.recordsByKey()
            );

            for (InternetDnsRecord record :
                    existing.records()) {
                signatures.put(
                        record.key(),
                        existing.signatureFor(
                                record
                        )
                );
            }
        }

        receive.chunks.entrySet()
                .stream()
                .sorted(
                        Map.Entry.comparingByKey()
                )
                .forEach(entry -> {
                    CompoundTag payload =
                            entry.getValue();

                    if (!"DATA".equals(
                            payload.getString(
                                    "xfr_kind"
                            )
                    )) {
                        return;
                    }

                    InternetDnsRecord record =
                            InternetDnsRecord.load(
                                    payload.getCompound(
                                            "record"
                                    )
                            );

                    String operation =
                            payload.getString(
                                    "operation"
                            );

                    if ("DELETE".equals(
                            operation
                    )) {
                        records.remove(
                                record.key()
                        );
                        signatures.remove(
                                record.key()
                        );
                    } else {
                        records.put(
                                record.key(),
                                record
                        );

                        signatures.put(
                                record.key(),
                                payload.getString(
                                        "record_signature"
                                )
                        );
                    }
                });

        List<InternetDnsRecord> ordered =
                records.values()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        InternetDnsRecord::key
                                )
                        )
                        .toList();

        DnsZoneSnapshot snapshot =
                new DnsZoneSnapshot(
                        config.zone(),
                        receive.toSerial,
                        receive.fingerprint,
                        receive.generatedAt,
                        ordered,
                        signatures
                );

        state.saveReplica(
                secondary.ipAddress(),
                snapshot
        );

        LAST_TRANSFER.put(
                secondary.ipAddress(),
                "PASS | "
                        + receive.mode
                        + " "
                        + config.zone()
                        + " serial="
                        + Long.toUnsignedString(
                        receive.toSerial
                )
                        + " records="
                        + ordered.size()
                        + " packets="
                        + receive.total
                        + " transport=TCP/53"
        );
    }

    private static void sendError(
            ServerRackBlockEntity rack,
            OSINetworkPacket query,
            int rcode,
            String detail
    ) {
        OSINetworkPacket response =
                rack.physicalDnsResponse(
                        query
                );

        response.payload.putBoolean(
                "physical_dns",
                query.payload.getBoolean(
                        "physical_dns"
                )
        );
        response.payload.putString(
                "pdns_kind",
                "ERROR"
        );
        response.payload.putInt(
                "rcode",
                rcode
        );
        response.payload.putString(
                "detail",
                detail
        );
        response.payload.putString(
                "domain",
                query.payload.getString(
                        "domain"
                )
        );
        response.payload.putString(
                "query_type",
                query.payload.getString(
                        "query_type"
                )
        );
        response.payload.putString(
                "answer",
                ""
        );
        response.payload.putString(
                "resolved_ip",
                "0.0.0.0"
        );

        rack.physicalDnsTransmit(
                response
        );
    }

    private static OSINetworkPacket internalPacket(
            ServerRackBlockEntity source,
            String targetIp,
            String application,
            String session
    ) {
        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.sourceMac =
                source.physicalDnsMacAddress();

        packet.targetMac =
                "";

        packet.sourceIp =
                source.ipAddress();

        packet.targetIp =
                targetIp;

        packet.sourcePort =
                53;

        packet.targetPort =
                53;

        packet.ttl =
                64;

        packet.applicationProtocol =
                application;

        packet.sessionId =
                session;

        return packet;
    }

    private static void markSent(
            ResolveTransaction transaction
    ) {
        transaction.lastSentTick =
                transaction.level.getGameTime();

        transaction.retries =
                Math.max(
                        0,
                        transaction.retries
                );
    }

    private static String normalizeName(String value) {
        if (value == null) {
            return "";
        }

        String normalized =
                value.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        while (normalized.endsWith(
                "."
        )) {
            normalized =
                    normalized.substring(
                            0,
                            normalized.length()
                                    - 1
                    );
        }

        return normalized;
    }

    private static String normalizeType(String value) {
        return value == null
                || value.isBlank()
                ? "A"
                : value.trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }

    private static String normalizeZone(String value) {
        return PhysicalDnsStateSavedData.normalizeZone(
                value
        );
    }

    private static String normalizeTld(String value) {
        return PhysicalDnsStateSavedData.normalizeTld(
                value
        );
    }

    private enum ResolveStage {
        NEW,
        WAIT_ROOT_REFERRAL,
        WAIT_TLD_DNSKEY,
        WAIT_TLD_REFERRAL,
        WAIT_AUTH_DNSKEY,
        WAIT_AUTH_ANSWER,
        COMPLETE,
        FAILED
    }

    private static final class ResolveTransaction {
        private final String id;
        private final ServerLevel level;
        private final String resolverIp;
        private final ClientEndpoint client;
        private final String name;
        private final String type;
        private final int dnsId;
        private final boolean dnssec;
        private final List<String> trace =
                new ArrayList<>();

        private ResolveStage stage =
                ResolveStage.NEW;

        private String tldZone =
                "";

        private String tldIp =
                "";

        private String expectedTldDs =
                "";

        private String tldPublicX509 =
                "";

        private int tldKeyTag;

        private String authZone =
                "";

        private String authPrimaryIp =
                "";

        private String authSecondaryIp =
                "";

        private String currentAuthIp =
                "";

        private String expectedAuthDs =
                "";

        private String authPublicX509 =
                "";

        private int authKeyTag;

        private boolean usingSecondary;

        private boolean finished;

        private long lastSentTick;

        private int retries;

        private ResolveTransaction(
                String id,
                ServerLevel level,
                String resolverIp,
                ClientEndpoint client,
                String name,
                String type,
                int dnsId,
                boolean dnssec
        ) {
            this.id = id;
            this.level = level;
            this.resolverIp = resolverIp;
            this.client = client;
            this.name = name;
            this.type = type;
            this.dnsId = dnsId;
            this.dnssec = dnssec;
        }
    }

    private record ClientEndpoint(
            String sourceMac,
            String sourceIp,
            int sourcePort,
            String sessionId,
            int dnsId
    ) {
    }

    private static final class TransferReceive {
        private final String id;
        private final ServerLevel level;
        private final String secondaryIp;
        private final String zone;
        private final Map<Integer, CompoundTag> chunks =
                new LinkedHashMap<>();

        private String mode =
                "";

        private long toSerial;

        private String fingerprint =
                "";

        private long generatedAt;

        private int total;

        private TransferReceive(
                String id,
                ServerLevel level,
                String secondaryIp,
                String zone
        ) {
            this.id = id;
            this.level = level;
            this.secondaryIp = secondaryIp;
            this.zone = zone;
        }
    }

    private record TransferItem(
            String operation,
            InternetDnsRecord record,
            String signature
    ) {
    }
}
