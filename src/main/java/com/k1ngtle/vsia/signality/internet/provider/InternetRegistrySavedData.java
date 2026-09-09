package com.k1ngtle.vsia.signality.internet.provider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InternetRegistrySavedData extends SavedData {
    public static final String DATA_NAME = "vsia_internet_registry";
    public static final UUID SYSTEM_OWNER = new UUID(0L, 0L);
    public static final String SYSTEM_PROVIDER = "vsia-net";
    public static final String SYSTEM_DOMAIN = "vsia-net.com";

    private static final long ONE_YEAR_MILLIS = Duration.ofDays(365).toMillis();
    private static final long TRANSFER_LOCK_MILLIS = Duration.ofDays(60).toMillis();
    private static final int MAX_PROVIDERS_PER_PLAYER = 8;
    private static final int MAX_DOMAINS_PER_PLAYER = 64;
    private static final int MAX_RECORDS_PER_ZONE = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, OwnedInternetProvider> providers = new LinkedHashMap<>();
    private final Map<String, RegisteredDomain> domains = new LinkedHashMap<>();
    private final Map<String, List<InternetDnsRecord>> zoneRecords = new LinkedHashMap<>();

    private long nextPrivateAsn = 64513L;

    public InternetRegistrySavedData() {
        ensureBootstrap();
    }

    public InternetRegistrySavedData(CompoundTag tag) {
        loadFromTag(tag);
        ensureBootstrap();
    }

    public static InternetRegistrySavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        InternetRegistrySavedData::new,
                        InternetRegistrySavedData::new,
                        DATA_NAME
                );
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        tag.putLong("NextPrivateAsn", nextPrivateAsn);

        ListTag providerList = new ListTag();
        providers.values().forEach(provider -> providerList.add(provider.save()));
        tag.put("Providers", providerList);

        ListTag domainList = new ListTag();
        domains.values().forEach(domain -> domainList.add(domain.save()));
        tag.put("Domains", domainList);

        ListTag recordList = new ListTag();
        zoneRecords.values().forEach(records ->
                records.forEach(record -> recordList.add(record.save()))
        );
        tag.put("DnsRecords", recordList);

        return tag;
    }

    public synchronized Collection<OwnedInternetProvider> providers() {
        return List.copyOf(providers.values());
    }

    public synchronized Collection<RegisteredDomain> domains() {
        return List.copyOf(domains.values());
    }

    public synchronized Optional<OwnedInternetProvider> provider(String id) {
        return Optional.ofNullable(providers.get(normalizeId(id)));
    }

    public synchronized Optional<RegisteredDomain> domain(String value) {
        return Optional.ofNullable(domains.get(InternetRegistryValidators.normalizeDomain(value)));
    }

    public synchronized List<OwnedInternetProvider> providersOwnedBy(UUID playerUuid) {
        return providers.values()
                .stream()
                .filter(provider -> provider.ownedBy(playerUuid))
                .toList();
    }

    public synchronized List<RegisteredDomain> domainsOwnedBy(UUID playerUuid) {
        return domains.values()
                .stream()
                .filter(domain -> domain.ownedBy(playerUuid))
                .toList();
    }

    public synchronized InternetRegistryResult registerProvider(
            UUID ownerUuid,
            String ownerName,
            String requestedId,
            String displayName,
            long nowMillis
    ) {
        String id = normalizeId(requestedId);

        if (!InternetRegistryValidators.validProviderId(id)) {
            return InternetRegistryResult.fail(
                    "Provider ID must be 3-32 lowercase letters, numbers, or dashes."
            );
        }

        if (displayName == null || displayName.isBlank() || displayName.length() > 64) {
            return InternetRegistryResult.fail("Provider display name must be 1-64 characters.");
        }

        if (providers.containsKey(id)) {
            return InternetRegistryResult.fail("Provider ID is already registered.");
        }

        if (providersOwnedBy(ownerUuid).size() >= MAX_PROVIDERS_PER_PLAYER) {
            return InternetRegistryResult.fail("Provider ownership limit reached.");
        }

        long asn;

        try {
            asn = allocatePrivateAsn();
        } catch (IllegalStateException exception) {
            return InternetRegistryResult.fail(exception.getMessage());
        }

        OwnedInternetProvider provider = new OwnedInternetProvider(
                id,
                displayName.trim(),
                ownerUuid,
                safeOwnerName(ownerName),
                asn,
                nowMillis,
                true,
                true,
                false
        );

        providers.put(id, provider);
        setDirty();

        return InternetRegistryResult.ok(
                "Provider registered: " + provider.displayName()
                        + " | id=" + provider.id()
                        + " | AS" + provider.autonomousSystemNumber()
        );
    }

    public synchronized InternetRegistryResult setRegistrarEnabled(
            UUID actor,
            String providerId,
            boolean enabled
    ) {
        String id = normalizeId(providerId);
        OwnedInternetProvider provider = providers.get(id);

        if (provider == null) {
            return InternetRegistryResult.fail("Provider not found.");
        }

        if (!provider.ownedBy(actor) && !provider.systemReserved()) {
            return InternetRegistryResult.fail("You do not own this provider.");
        }

        providers.put(
                id,
                new OwnedInternetProvider(
                        provider.id(),
                        provider.displayName(),
                        provider.ownerUuid(),
                        provider.ownerName(),
                        provider.autonomousSystemNumber(),
                        provider.createdAtMillis(),
                        enabled,
                        provider.active(),
                        provider.systemReserved()
                )
        );

        setDirty();

        return InternetRegistryResult.ok(
                "Registrar service " + (enabled ? "enabled." : "disabled.")
        );
    }

    public synchronized InternetRegistryResult registerDomain(
            UUID ownerUuid,
            String ownerName,
            String requestedDomain,
            String registrarProviderId,
            long nowMillis
    ) {
        String normalized = InternetRegistryValidators.normalizeDomain(requestedDomain);

        if (!InternetRegistryValidators.isRegistrableRoot(normalized)) {
            return InternetRegistryResult.fail(
                    "Register a supported root domain, e.g. example.com or example.co.uk."
            );
        }

        RegisteredDomain existing = domains.get(normalized);

        if (existing != null && !existing.expired(nowMillis)) {
            return InternetRegistryResult.fail(
                    "Domain is already registered to " + existing.ownerName() + "."
            );
        }

        long owned = domainsOwnedBy(ownerUuid)
                .stream()
                .filter(domain -> !domain.expired(nowMillis))
                .count();

        if (owned >= MAX_DOMAINS_PER_PLAYER) {
            return InternetRegistryResult.fail("Domain ownership limit reached.");
        }

        String registrar = normalizeId(registrarProviderId);

        if (registrar.isBlank()) {
            registrar = SYSTEM_PROVIDER;
        }

        OwnedInternetProvider provider = providers.get(registrar);

        if (provider == null || !provider.active() || !provider.registrarEnabled()) {
            return InternetRegistryResult.fail(
                    "Registrar provider is unavailable or does not offer registrar service."
            );
        }

        RegisteredDomain domain = new RegisteredDomain(
                normalized,
                ownerUuid,
                safeOwnerName(ownerName),
                registrar,
                nowMillis,
                nowMillis + ONE_YEAR_MILLIS,
                nowMillis + TRANSFER_LOCK_MILLIS,
                newAuthInfo(),
                List.of(
                        "ns1." + SYSTEM_DOMAIN,
                        "ns2." + SYSTEM_DOMAIN
                ),
                true,
                false
        );

        domains.put(normalized, domain);
        zoneRecords.put(normalized, new ArrayList<>());
        setDirty();

        return InternetRegistryResult.ok(
                "Domain registered: " + normalized
                        + " | registrant=" + domain.ownerName()
                        + " | registrar=" + registrar
        );
    }

    public synchronized InternetRegistryResult renewDomain(
            UUID actor,
            String requestedDomain,
            long nowMillis
    ) {
        String normalized = InternetRegistryValidators.normalizeDomain(requestedDomain);
        RegisteredDomain domain = domains.get(normalized);

        if (domain == null) {
            return InternetRegistryResult.fail("Domain not found.");
        }

        if (!domain.ownedBy(actor) && !domain.systemReserved()) {
            return InternetRegistryResult.fail("You do not own this domain.");
        }

        long expires = Math.max(nowMillis, domain.expiresAtMillis()) + ONE_YEAR_MILLIS;

        domains.put(
                normalized,
                copyDomain(
                        domain,
                        domain.ownerUuid(),
                        domain.ownerName(),
                        domain.registrarProviderId(),
                        expires,
                        domain.transferLockedUntilMillis(),
                        domain.authInfo(),
                        domain.nameServers()
                )
        );

        setDirty();

        return InternetRegistryResult.ok("Domain renewed for one year.");
    }

    public synchronized InternetRegistryResult setNameServers(
            UUID actor,
            String requestedDomain,
            List<String> requestedNameServers
    ) {
        String normalized = InternetRegistryValidators.normalizeDomain(requestedDomain);
        RegisteredDomain domain = domains.get(normalized);

        if (domain == null) {
            return InternetRegistryResult.fail("Domain not found.");
        }

        if (!domain.ownedBy(actor) && !domain.systemReserved()) {
            return InternetRegistryResult.fail("You do not own this domain.");
        }

        if (requestedNameServers == null
                || requestedNameServers.size() < 2
                || requestedNameServers.size() > 8) {
            return InternetRegistryResult.fail("A delegation requires 2-8 name servers.");
        }

        List<String> normalizedServers = new ArrayList<>();

        for (String value : requestedNameServers) {
            String server = InternetRegistryValidators.normalizeDomain(value);

            if (!InternetRegistryValidators.validHostname(server)) {
                return InternetRegistryResult.fail("Invalid name server: " + value);
            }

            if (!normalizedServers.contains(server)) {
                normalizedServers.add(server);
            }
        }

        if (normalizedServers.size() < 2) {
            return InternetRegistryResult.fail("At least two distinct name servers are required.");
        }

        domains.put(
                normalized,
                copyDomain(
                        domain,
                        domain.ownerUuid(),
                        domain.ownerName(),
                        domain.registrarProviderId(),
                        domain.expiresAtMillis(),
                        domain.transferLockedUntilMillis(),
                        domain.authInfo(),
                        normalizedServers
                )
        );

        setDirty();

        return InternetRegistryResult.ok(
                "Delegation updated: " + String.join(", ", normalizedServers)
        );
    }

    public synchronized InternetRegistryResult changeRegistrar(
            UUID actor,
            String requestedDomain,
            String providerId
    ) {
        String normalized = InternetRegistryValidators.normalizeDomain(requestedDomain);
        RegisteredDomain domain = domains.get(normalized);

        if (domain == null) {
            return InternetRegistryResult.fail("Domain not found.");
        }

        if (!domain.ownedBy(actor)) {
            return InternetRegistryResult.fail("You do not own this domain.");
        }

        String registrar = normalizeId(providerId);
        OwnedInternetProvider provider = providers.get(registrar);

        if (provider == null || !provider.active() || !provider.registrarEnabled()) {
            return InternetRegistryResult.fail("Target registrar is unavailable.");
        }

        domains.put(
                normalized,
                copyDomain(
                        domain,
                        domain.ownerUuid(),
                        domain.ownerName(),
                        registrar,
                        domain.expiresAtMillis(),
                        domain.transferLockedUntilMillis(),
                        domain.authInfo(),
                        domain.nameServers()
                )
        );

        setDirty();

        return InternetRegistryResult.ok("Registrar changed to " + registrar + ".");
    }

    public synchronized InternetRegistryResult rotateAuthInfo(
            UUID actor,
            String requestedDomain
    ) {
        String normalized = InternetRegistryValidators.normalizeDomain(requestedDomain);
        RegisteredDomain domain = domains.get(normalized);

        if (domain == null) {
            return InternetRegistryResult.fail("Domain not found.");
        }

        if (!domain.ownedBy(actor) || domain.systemReserved()) {
            return InternetRegistryResult.fail("You do not own a transferable registration.");
        }

        String auth = newAuthInfo();

        domains.put(
                normalized,
                copyDomain(
                        domain,
                        domain.ownerUuid(),
                        domain.ownerName(),
                        domain.registrarProviderId(),
                        domain.expiresAtMillis(),
                        domain.transferLockedUntilMillis(),
                        auth,
                        domain.nameServers()
                )
        );

        setDirty();

        return InternetRegistryResult.ok("AUTHINFO=" + auth);
    }

    public synchronized InternetRegistryResult claimRegistrantTransfer(
            UUID newOwnerUuid,
            String newOwnerName,
            String requestedDomain,
            String authInfo,
            long nowMillis
    ) {
        String normalized = InternetRegistryValidators.normalizeDomain(requestedDomain);
        RegisteredDomain domain = domains.get(normalized);

        if (domain == null) {
            return InternetRegistryResult.fail("Domain not found.");
        }

        if (domain.systemReserved()) {
            return InternetRegistryResult.fail("System-reserved domain cannot be transferred.");
        }

        if (nowMillis < domain.transferLockedUntilMillis()) {
            return InternetRegistryResult.fail("Domain is under a 60-day transfer lock.");
        }

        if (authInfo == null || !domain.authInfo().equals(authInfo.trim())) {
            return InternetRegistryResult.fail("Invalid AUTHINFO transfer code.");
        }

        domains.put(
                normalized,
                copyDomain(
                        domain,
                        newOwnerUuid,
                        safeOwnerName(newOwnerName),
                        domain.registrarProviderId(),
                        domain.expiresAtMillis(),
                        nowMillis + TRANSFER_LOCK_MILLIS,
                        newAuthInfo(),
                        domain.nameServers()
                )
        );

        setDirty();

        return InternetRegistryResult.ok(
                "Registrant ownership transferred to " + safeOwnerName(newOwnerName) + "."
        );
    }

    public synchronized InternetRegistryResult addDnsRecord(
            UUID actor,
            String requestedZone,
            String owner,
            String requestedType,
            String rawValue,
            int ttlSeconds
    ) {
        String zone = InternetRegistryValidators.normalizeDomain(requestedZone);
        RegisteredDomain domain = domains.get(zone);

        if (domain == null) {
            return InternetRegistryResult.fail("Registered zone not found.");
        }

        if (!domain.ownedBy(actor) && !domain.systemReserved()) {
            return InternetRegistryResult.fail("You do not own this DNS zone.");
        }

        String type = requestedType == null
                ? ""
                : requestedType.trim().toUpperCase(Locale.ROOT);

        if (!InternetRegistryValidators.supportedDnsType(type)) {
            return InternetRegistryResult.fail(
                    "Supported types: A AAAA CNAME MX TXT NS SRV CAA."
            );
        }

        if (ttlSeconds < 30 || ttlSeconds > 86400) {
            return InternetRegistryResult.fail("TTL must be 30-86400 seconds.");
        }

        String fqdn = InternetRegistryValidators.fqdn(zone, owner);

        if (!InternetRegistryValidators.validDnsOwnerName(fqdn)
                || !fqdn.equals(zone) && !fqdn.endsWith("." + zone)) {
            return InternetRegistryResult.fail(
                    "DNS owner name must remain inside the registered zone."
            );
        }

        InternetDnsRecord record;

        try {
            record = parseRecord(zone, fqdn, type, rawValue, ttlSeconds);
        } catch (IllegalArgumentException exception) {
            return InternetRegistryResult.fail(exception.getMessage());
        }

        List<InternetDnsRecord> records =
                zoneRecords.computeIfAbsent(zone, ignored -> new ArrayList<>());

        if (records.size() >= MAX_RECORDS_PER_ZONE) {
            return InternetRegistryResult.fail("DNS zone record limit reached.");
        }

        boolean hasCname = records.stream()
                .anyMatch(existing ->
                        existing.name().equals(fqdn) && existing.type().equals("CNAME")
                );

        boolean hasOther = records.stream()
                .anyMatch(existing ->
                        existing.name().equals(fqdn) && !existing.type().equals("CNAME")
                );

        if ("CNAME".equals(type) && fqdn.equals(zone)) {
            return InternetRegistryResult.fail(
                    "A zone apex cannot be CNAME because SOA/NS authority exists there."
            );
        }

        if ("CNAME".equals(type) && hasOther || !"CNAME".equals(type) && hasCname) {
            return InternetRegistryResult.fail(
                    "CNAME cannot coexist with other record types at the same owner name."
            );
        }

        if (records.stream().anyMatch(existing -> existing.key().equals(record.key()))) {
            return InternetRegistryResult.fail("Identical DNS record already exists.");
        }

        records.add(record);
        setDirty();

        return InternetRegistryResult.ok(
                "DNS record added: " + record.name()
                        + " " + record.ttlSeconds()
                        + " IN " + record.type()
                        + " " + record.answerValue()
        );
    }

    public synchronized InternetRegistryResult removeDnsRecord(
            UUID actor,
            String requestedZone,
            String owner,
            String requestedType
    ) {
        String zone = InternetRegistryValidators.normalizeDomain(requestedZone);
        RegisteredDomain domain = domains.get(zone);

        if (domain == null) {
            return InternetRegistryResult.fail("Registered zone not found.");
        }

        if (!domain.ownedBy(actor) && !domain.systemReserved()) {
            return InternetRegistryResult.fail("You do not own this DNS zone.");
        }

        String fqdn = InternetRegistryValidators.fqdn(zone, owner);
        String type = requestedType == null
                ? ""
                : requestedType.trim().toUpperCase(Locale.ROOT);

        List<InternetDnsRecord> records =
                zoneRecords.computeIfAbsent(zone, ignored -> new ArrayList<>());

        int before = records.size();

        records.removeIf(record ->
                record.name().equals(fqdn) && record.type().equals(type)
        );

        if (records.size() == before) {
            return InternetRegistryResult.fail("No matching RRset found.");
        }

        setDirty();

        return InternetRegistryResult.ok("DNS RRset removed.");
    }

    public synchronized List<InternetDnsRecord> records(String requestedZone) {
        String zone = InternetRegistryValidators.normalizeDomain(requestedZone);
        return List.copyOf(zoneRecords.getOrDefault(zone, List.of()));
    }

    public synchronized Optional<InternetDnsAnswer> resolveFirst(
            String requestedName,
            String requestedType,
            long nowMillis
    ) {
        return resolveFirst(
                InternetRegistryValidators.normalizeDomain(requestedName),
                requestedType == null ? "A" : requestedType.trim().toUpperCase(Locale.ROOT),
                nowMillis,
                0,
                new ArrayList<>()
        );
    }

    public synchronized String rdap(String requestedDomain, long nowMillis) {
        String normalized = InternetRegistryValidators.normalizeDomain(requestedDomain);
        String root = InternetRegistryValidators.registrableRoot(normalized);
        RegisteredDomain domain = domains.get(root);

        if (domain == null) {
            return "RDAP: NOT FOUND | " + normalized;
        }

        OwnedInternetProvider registrar = providers.get(domain.registrarProviderId());

        return "RDAP DOMAIN " + domain.domain()
                + "\nRegistrant: " + domain.ownerName()
                + "\nRegistrar: "
                + (registrar == null
                ? domain.registrarProviderId()
                : registrar.displayName() + " (" + registrar.id() + ")")
                + "\nStatus: "
                + (domain.expired(nowMillis)
                ? "EXPIRED"
                : domain.active() ? "ACTIVE" : "INACTIVE")
                + "\nCreated: " + domain.createdAtMillis()
                + "\nExpires: " + domain.expiresAtMillis()
                + "\nTransfer lock until: " + domain.transferLockedUntilMillis()
                + "\nName servers: " + String.join(", ", domain.nameServers());
    }

    public synchronized String delegationTrace(
            String requestedName,
            String requestedType,
            long nowMillis
    ) {
        return resolveFirst(requestedName, requestedType, nowMillis)
                .map(InternetDnsAnswer::trace)
                .orElse(
                        "ROOT -> ." + InternetRegistryValidators.publicSuffix(requestedName)
                                + " REGISTRY -> NXDOMAIN/NODATA"
                );
    }

    private Optional<InternetDnsAnswer> resolveFirst(
            String name,
            String type,
            long nowMillis,
            int depth,
            List<String> trace
    ) {
        if (depth > 8 || name.isBlank()) {
            return Optional.empty();
        }

        String zone = findZone(name, nowMillis);

        if (zone.isBlank()) {
            return Optional.empty();
        }

        RegisteredDomain registration = domains.get(zone);
        String suffix = InternetRegistryValidators.publicSuffix(zone);

        if (trace.isEmpty()) {
            trace.add("ROOT");
            trace.add("." + suffix + " REGISTRY");
            trace.add(
                    "DELEGATION " + zone + " -> " + String.join(",", registration.nameServers())
            );
        }

        if ("NS".equals(type) && name.equals(zone) && !registration.nameServers().isEmpty()) {
            String server = registration.nameServers().get(0);
            trace.add("AUTH NS " + server);

            return Optional.of(
                    new InternetDnsAnswer(
                            name,
                            type,
                            server,
                            300,
                            zone,
                            true,
                            String.join(" -> ", trace)
                    )
            );
        }

        if ("SOA".equals(type) && name.equals(zone)) {
            String primary = registration.nameServers().isEmpty()
                    ? "ns1." + SYSTEM_DOMAIN
                    : registration.nameServers().get(0);

            String soa = primary
                    + " hostmaster." + zone
                    + " " + Math.max(1L, registration.createdAtMillis() / 1000L)
                    + " 3600 600 1209600 300";

            trace.add("AUTH SOA");

            return Optional.of(
                    new InternetDnsAnswer(
                            name,
                            type,
                            soa,
                            300,
                            zone,
                            true,
                            String.join(" -> ", trace)
                    )
            );
        }

        List<InternetDnsRecord> records = zoneRecords.getOrDefault(zone, List.of());

        InternetDnsRecord direct = records.stream()
                .filter(record ->
                        record.name().equals(name) && record.type().equals(type)
                )
                .findFirst()
                .orElse(null);

        if (direct == null && !name.equals(zone)) {
            String wildcard = "*." + zone;

            direct = records.stream()
                    .filter(record ->
                            record.name().equals(wildcard) && record.type().equals(type)
                    )
                    .findFirst()
                    .orElse(null);
        }

        if (direct != null) {
            trace.add(
                    "AUTH " + direct.name()
                            + " " + direct.type()
                            + " " + direct.answerValue()
            );

            return Optional.of(
                    new InternetDnsAnswer(
                            name,
                            type,
                            direct.answerValue(),
                            direct.ttlSeconds(),
                            zone,
                            true,
                            String.join(" -> ", trace)
                    )
            );
        }

        if (!"CNAME".equals(type)) {
            InternetDnsRecord cname = records.stream()
                    .filter(record ->
                            record.name().equals(name) && record.type().equals("CNAME")
                    )
                    .findFirst()
                    .orElse(null);

            if (cname != null) {
                trace.add("CNAME " + name + " -> " + cname.value());

                return resolveFirst(
                        InternetRegistryValidators.normalizeDomain(cname.value()),
                        type,
                        nowMillis,
                        depth + 1,
                        trace
                );
            }
        }

        return Optional.empty();
    }

    private void loadFromTag(CompoundTag tag) {
        nextPrivateAsn = tag.contains("NextPrivateAsn")
                ? tag.getLong("NextPrivateAsn")
                : 64513L;

        if (tag.contains("Providers", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Providers", Tag.TAG_COMPOUND);

            for (int i = 0; i < list.size(); i++) {
                OwnedInternetProvider provider =
                        OwnedInternetProvider.load(list.getCompound(i));

                providers.put(provider.id(), provider);
            }
        }

        if (tag.contains("Domains", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Domains", Tag.TAG_COMPOUND);

            for (int i = 0; i < list.size(); i++) {
                RegisteredDomain domain =
                        RegisteredDomain.load(list.getCompound(i));

                domains.put(domain.domain(), domain);
            }
        }

        if (tag.contains("DnsRecords", Tag.TAG_LIST)) {
            ListTag list = tag.getList("DnsRecords", Tag.TAG_COMPOUND);

            for (int i = 0; i < list.size(); i++) {
                InternetDnsRecord record =
                        InternetDnsRecord.load(list.getCompound(i));

                zoneRecords
                        .computeIfAbsent(record.zone(), ignored -> new ArrayList<>())
                        .add(record);
            }
        }
    }

    private void ensureBootstrap() {
        long now = System.currentTimeMillis();

        providers.putIfAbsent(
                SYSTEM_PROVIDER,
                new OwnedInternetProvider(
                        SYSTEM_PROVIDER,
                        "VSIA Network Registry",
                        SYSTEM_OWNER,
                        "SYSTEM",
                        64512L,
                        now,
                        true,
                        true,
                        true
                )
        );

        domains.putIfAbsent(
                SYSTEM_DOMAIN,
                new RegisteredDomain(
                        SYSTEM_DOMAIN,
                        SYSTEM_OWNER,
                        "SYSTEM",
                        SYSTEM_PROVIDER,
                        now,
                        Long.MAX_VALUE,
                        0L,
                        "",
                        List.of(
                                "ns1." + SYSTEM_DOMAIN,
                                "ns2." + SYSTEM_DOMAIN
                        ),
                        true,
                        true
                )
        );

        List<InternetDnsRecord> records =
                zoneRecords.computeIfAbsent(SYSTEM_DOMAIN, ignored -> new ArrayList<>());

        bootstrapRecord(
                records,
                new InternetDnsRecord(
                        SYSTEM_DOMAIN,
                        "www." + SYSTEM_DOMAIN,
                        "A",
                        "192.168.1.2",
                        300,
                        0,
                        0,
                        0
                )
        );

        bootstrapRecord(
                records,
                new InternetDnsRecord(
                        SYSTEM_DOMAIN,
                        "mail." + SYSTEM_DOMAIN,
                        "A",
                        "192.168.1.2",
                        300,
                        0,
                        0,
                        0
                )
        );

        bootstrapRecord(
                records,
                new InternetDnsRecord(
                        SYSTEM_DOMAIN,
                        "ns1." + SYSTEM_DOMAIN,
                        "A",
                        "192.168.1.2",
                        300,
                        0,
                        0,
                        0
                )
        );

        bootstrapRecord(
                records,
                new InternetDnsRecord(
                        SYSTEM_DOMAIN,
                        "ns2." + SYSTEM_DOMAIN,
                        "A",
                        "192.168.1.2",
                        300,
                        0,
                        0,
                        0
                )
        );
    }

    private void bootstrapRecord(
            List<InternetDnsRecord> records,
            InternetDnsRecord desired
    ) {
        boolean exists = records.stream()
                .anyMatch(record -> record.key().equals(desired.key()));

        if (!exists) {
            records.add(desired);
        }
    }

    private String findZone(String name, long nowMillis) {
        return domains.values()
                .stream()
                .filter(domain ->
                        domain.active()
                                && !domain.expired(nowMillis)
                                && (name.equals(domain.domain())
                                || name.endsWith("." + domain.domain()))
                )
                .map(RegisteredDomain::domain)
                .max(Comparator.comparingInt(String::length))
                .orElse("");
    }

    private long allocatePrivateAsn() {
        long candidate = nextPrivateAsn;

        while (true) {
            if (candidate > 65534L && candidate < 4200000000L) {
                candidate = 4200000000L;
            }

            if (candidate > 4294967294L) {
                throw new IllegalStateException("No private-use ASNs remain.");
            }

            long current = candidate;

            boolean inUse = providers.values()
                    .stream()
                    .anyMatch(provider ->
                            provider.autonomousSystemNumber() == current
                    );

            candidate++;

            if (!inUse && InternetRegistryValidators.isPrivateAsn(current)) {
                nextPrivateAsn = candidate;
                return current;
            }
        }
    }

    private InternetDnsRecord parseRecord(
            String zone,
            String name,
            String type,
            String rawValue,
            int ttl
    ) {
        String value = rawValue == null ? "" : rawValue.trim();

        if (value.isBlank()) {
            throw new IllegalArgumentException("DNS record value cannot be empty.");
        }

        int priority = 0;
        int weight = 0;
        int port = 0;

        switch (type) {
            case "A" -> {
                if (!InternetRegistryValidators.validIpv4(value)) {
                    throw new IllegalArgumentException("A records require a valid IPv4 address.");
                }
            }

            case "AAAA" -> {
                if (!InternetRegistryValidators.validIpv6(value)) {
                    throw new IllegalArgumentException("AAAA records require a valid IPv6 address.");
                }
            }

            case "CNAME", "NS" -> {
                value = InternetRegistryValidators.normalizeDomain(value);

                if (!InternetRegistryValidators.validHostname(value)) {
                    throw new IllegalArgumentException(type + " requires a valid target hostname.");
                }
            }

            case "MX" -> {
                String[] parts = value.split("\\s+", 2);

                if (parts.length != 2) {
                    throw new IllegalArgumentException("MX format: <priority> <mail-host>.");
                }

                try {
                    priority = Integer.parseInt(parts[0]);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("MX priority must be an integer.");
                }

                value = InternetRegistryValidators.normalizeDomain(parts[1]);

                if (priority < 0
                        || priority > 65535
                        || !InternetRegistryValidators.validHostname(value)) {
                    throw new IllegalArgumentException("Invalid MX priority or target.");
                }
            }

            case "SRV" -> {
                String[] parts = value.split("\\s+", 4);

                if (parts.length != 4) {
                    throw new IllegalArgumentException(
                            "SRV format: <priority> <weight> <port> <target>."
                    );
                }

                try {
                    priority = Integer.parseInt(parts[0]);
                    weight = Integer.parseInt(parts[1]);
                    port = Integer.parseInt(parts[2]);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException(
                            "SRV priority/weight/port must be integers."
                    );
                }

                value = InternetRegistryValidators.normalizeDomain(parts[3]);

                if (priority < 0
                        || priority > 65535
                        || weight < 0
                        || weight > 65535
                        || port < 0
                        || port > 65535
                        || !InternetRegistryValidators.validHostname(value)) {
                    throw new IllegalArgumentException("Invalid SRV fields.");
                }
            }

            case "TXT" -> {
                if (value.length() > 255) {
                    throw new IllegalArgumentException(
                            "This TXT character-string is limited to 255 characters."
                    );
                }
            }

            case "CAA" -> {
                String[] parts = value.split("\\s+", 3);

                if (parts.length < 3) {
                    throw new IllegalArgumentException(
                            "CAA format: <flags> <tag> <value>."
                    );
                }

                try {
                    int flags = Integer.parseInt(parts[0]);

                    if (flags < 0 || flags > 255) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("CAA flags must be 0-255.");
                }
            }

            default -> throw new IllegalArgumentException("Unsupported DNS record type.");
        }

        return new InternetDnsRecord(
                zone,
                name,
                type,
                value,
                ttl,
                priority,
                weight,
                port
        );
    }

    private RegisteredDomain copyDomain(
            RegisteredDomain source,
            UUID ownerUuid,
            String ownerName,
            String registrar,
            long expires,
            long transferLock,
            String authInfo,
            List<String> nameServers
    ) {
        return new RegisteredDomain(
                source.domain(),
                ownerUuid,
                ownerName,
                registrar,
                source.createdAtMillis(),
                expires,
                transferLock,
                authInfo,
                nameServers,
                source.active(),
                source.systemReserved()
        );
    }

    private String normalizeId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safeOwnerName(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value.trim();
    }

    private String newAuthInfo() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);

        StringBuilder builder = new StringBuilder();

        for (byte value : bytes) {
            builder.append(String.format(Locale.ROOT, "%02x", value & 0xFF));
        }

        return builder.toString();
    }
}
