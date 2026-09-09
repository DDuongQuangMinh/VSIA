package com.k1ngtle.vsia.signality.internet.dns.physical;

import com.k1ngtle.vsia.signality.internet.provider.InternetDnsRecord;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistrySavedData;
import com.k1ngtle.vsia.signality.internet.provider.RegisteredDomain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class PhysicalDnsStateSavedData extends SavedData {
    public static final String DATA_NAME = "vsia_physical_dns";

    private static final int HISTORY_LIMIT = 16;

    private final Map<String, String> tldServers = new LinkedHashMap<>();
    private final Map<String, DnsAuthorityDelegation> delegations = new LinkedHashMap<>();
    private final Map<String, DnssecZoneKey> zoneKeys = new LinkedHashMap<>();
    private final Map<String, List<DnsZoneSnapshot>> histories = new LinkedHashMap<>();
    private final Map<String, DnsZoneSnapshot> replicas = new LinkedHashMap<>();

    public PhysicalDnsStateSavedData() {
    }

    public PhysicalDnsStateSavedData(CompoundTag tag) {
        loadFromTag(tag);
    }

    public static PhysicalDnsStateSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        PhysicalDnsStateSavedData::new,
                        PhysicalDnsStateSavedData::new,
                        DATA_NAME
                );
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        ListTag tlds = new ListTag();

        tldServers.forEach((zone, ip) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Zone", zone);
            entry.putString("Ip", ip);
            tlds.add(entry);
        });

        tag.put("TldServers", tlds);

        ListTag delegationList = new ListTag();

        for (DnsAuthorityDelegation delegation : delegations.values()) {
            delegationList.add(delegation.save());
        }

        tag.put("Delegations", delegationList);

        ListTag keyList = new ListTag();

        for (DnssecZoneKey key : zoneKeys.values()) {
            keyList.add(key.save());
        }

        tag.put("ZoneKeys", keyList);

        ListTag historyList = new ListTag();

        histories.forEach((zone, snapshots) -> {
            for (DnsZoneSnapshot snapshot : snapshots) {
                CompoundTag entry = snapshot.save();
                entry.putString("HistoryZone", zone);
                historyList.add(entry);
            }
        });

        tag.put("Histories", historyList);

        ListTag replicaList = new ListTag();

        replicas.forEach((replicaKey, snapshot) -> {
            CompoundTag entry = snapshot.save();
            entry.putString("ReplicaKey", replicaKey);
            replicaList.add(entry);
        });

        tag.put("Replicas", replicaList);

        return tag;
    }

    public synchronized void setTldServer(
            String tld,
            String ip
    ) {
        tldServers.put(
                normalizeTld(tld),
                ip
        );
        setDirty();
    }

    public synchronized Optional<String> tldServer(String tld) {
        return Optional.ofNullable(
                tldServers.get(
                        normalizeTld(tld)
                )
        );
    }

    public synchronized Map<String, String> tldServers() {
        return Map.copyOf(tldServers);
    }

    public synchronized void setDelegation(
            DnsAuthorityDelegation delegation
    ) {
        delegations.put(
                normalizeZone(delegation.zone()),
                delegation
        );
        setDirty();
    }

    public synchronized Optional<DnsAuthorityDelegation> delegation(String zone) {
        return Optional.ofNullable(
                delegations.get(
                        normalizeZone(zone)
                )
        );
    }

    public synchronized Collection<DnsAuthorityDelegation> delegations() {
        return List.copyOf(delegations.values());
    }

    public synchronized DnssecZoneKey ensureKey(String zone) {
        String normalized = normalizeZone(zone);
        DnssecZoneKey existing = zoneKeys.get(normalized);

        if (existing != null) {
            return existing;
        }

        DnssecZoneKey generated = DnssecEngine.generate(normalized);
        zoneKeys.put(normalized, generated);
        setDirty();

        return generated;
    }

    public synchronized Optional<DnssecZoneKey> key(String zone) {
        return Optional.ofNullable(
                zoneKeys.get(
                        normalizeZone(zone)
                )
        );
    }

    public synchronized DnsZoneSnapshot observePrimary(
            ServerLevel level,
            String requestedZone
    ) {
        String zone = normalizeZone(requestedZone);
        InternetRegistrySavedData registry = InternetRegistrySavedData.get(level);

        List<InternetDnsRecord> records = new ArrayList<>(
                registry.records(zone)
        );

        RegisteredDomain domain = registry.domain(zone)
                .orElse(null);

        if (domain != null) {
            for (String nameServer : domain.nameServers()) {
                boolean exists = records.stream()
                        .anyMatch(record ->
                                record.type().equalsIgnoreCase("NS")
                                        && record.name().equalsIgnoreCase(zone)
                                        && record.value().equalsIgnoreCase(nameServer)
                        );

                if (!exists) {
                    records.add(
                            new InternetDnsRecord(
                                    zone,
                                    zone,
                                    "NS",
                                    nameServer,
                                    300,
                                    0,
                                    0,
                                    0
                            )
                    );
                }
            }
        }

        records.sort(
                Comparator.comparing(InternetDnsRecord::key)
        );

        String fingerprint = fingerprint(records);
        List<DnsZoneSnapshot> history = histories.computeIfAbsent(
                zone,
                ignored -> new ArrayList<>()
        );

        if (!history.isEmpty()) {
            DnsZoneSnapshot latest = history.get(history.size() - 1);

            if (latest.fingerprint().equals(fingerprint)
                    && System.currentTimeMillis() - latest.generatedAtMillis()
                    < 43_200_000L) {
                return latest;
            }
        }

        long serial = nextSerial(
                history.isEmpty()
                        ? 0L
                        : history.get(history.size() - 1).serial()
        );

        DnssecZoneKey key = ensureKey(zone);
        long generatedAt = System.currentTimeMillis();
        long inception = generatedAt / 1000L - 60L;
        long expiration = generatedAt / 1000L + 86400L;

        Map<String, String> signatures = new LinkedHashMap<>();

        for (InternetDnsRecord record : records) {
            String canonical = DnssecEngine.canonicalAnswer(
                    zone,
                    serial,
                    record.name(),
                    record.type(),
                    record.ttlSeconds(),
                    record.answerValue(),
                    inception,
                    expiration,
                    key.keyTag()
            );

            signatures.put(
                    record.key(),
                    DnssecEngine.sign(
                            canonical,
                            key
                    )
            );
        }

        DnsZoneSnapshot snapshot = new DnsZoneSnapshot(
                zone,
                serial,
                fingerprint,
                generatedAt,
                records,
                signatures
        );

        history.add(snapshot);

        while (history.size() > HISTORY_LIMIT) {
            history.remove(0);
        }

        setDirty();

        return snapshot;
    }

    public synchronized Optional<DnsZoneSnapshot> latestPrimary(String zone) {
        List<DnsZoneSnapshot> history = histories.get(
                normalizeZone(zone)
        );

        if (history == null || history.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                history.get(history.size() - 1)
        );
    }

    public synchronized Optional<DnsZoneSnapshot> snapshot(
            String zone,
            long serial
    ) {
        List<DnsZoneSnapshot> history = histories.get(
                normalizeZone(zone)
        );

        if (history == null) {
            return Optional.empty();
        }

        return history.stream()
                .filter(snapshot ->
                        snapshot.serial() == serial
                )
                .findFirst();
    }

    public synchronized void saveReplica(
            String serverIp,
            DnsZoneSnapshot snapshot
    ) {
        replicas.put(
                replicaKey(
                        serverIp,
                        snapshot.zone()
                ),
                snapshot
        );
        setDirty();
    }

    public synchronized Optional<DnsZoneSnapshot> replica(
            String serverIp,
            String zone
    ) {
        return Optional.ofNullable(
                replicas.get(
                        replicaKey(
                                serverIp,
                                zone
                        )
                )
        );
    }

    public synchronized Delta delta(
            String zone,
            long oldSerial,
            DnsZoneSnapshot current
    ) {
        Optional<DnsZoneSnapshot> old = snapshot(
                zone,
                oldSerial
        );

        if (old.isEmpty()) {
            return new Delta(
                    false,
                    List.of(),
                    List.of()
            );
        }

        Map<String, InternetDnsRecord> before = old.get().recordsByKey();
        Map<String, InternetDnsRecord> after = current.recordsByKey();

        List<InternetDnsRecord> deleted = before.entrySet()
                .stream()
                .filter(entry ->
                        !after.containsKey(
                                entry.getKey()
                        )
                )
                .map(Map.Entry::getValue)
                .toList();

        List<InternetDnsRecord> added = after.entrySet()
                .stream()
                .filter(entry ->
                        !before.containsKey(
                                entry.getKey()
                        )
                )
                .map(Map.Entry::getValue)
                .toList();

        return new Delta(
                true,
                deleted,
                added
        );
    }

    private void loadFromTag(CompoundTag tag) {
        if (tag.contains("TldServers", Tag.TAG_LIST)) {
            ListTag list = tag.getList("TldServers", Tag.TAG_COMPOUND);

            for (int index = 0; index < list.size(); index++) {
                CompoundTag entry = list.getCompound(index);

                tldServers.put(
                        normalizeTld(entry.getString("Zone")),
                        entry.getString("Ip")
                );
            }
        }

        if (tag.contains("Delegations", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Delegations", Tag.TAG_COMPOUND);

            for (int index = 0; index < list.size(); index++) {
                DnsAuthorityDelegation delegation =
                        DnsAuthorityDelegation.load(
                                list.getCompound(index)
                        );

                delegations.put(
                        normalizeZone(delegation.zone()),
                        delegation
                );
            }
        }

        if (tag.contains("ZoneKeys", Tag.TAG_LIST)) {
            ListTag list = tag.getList("ZoneKeys", Tag.TAG_COMPOUND);

            for (int index = 0; index < list.size(); index++) {
                DnssecZoneKey key =
                        DnssecZoneKey.load(
                                list.getCompound(index)
                        );

                zoneKeys.put(
                        normalizeZone(key.zone()),
                        key
                );
            }
        }

        if (tag.contains("Histories", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Histories", Tag.TAG_COMPOUND);

            for (int index = 0; index < list.size(); index++) {
                CompoundTag entry = list.getCompound(index);
                String zone = normalizeZone(
                        entry.getString("HistoryZone")
                );

                histories.computeIfAbsent(
                        zone,
                        ignored -> new ArrayList<>()
                ).add(
                        DnsZoneSnapshot.load(entry)
                );
            }

            histories.values()
                    .forEach(snapshots ->
                            snapshots.sort(
                                    Comparator.comparingLong(
                                            DnsZoneSnapshot::serial
                                    )
                            )
                    );
        }

        if (tag.contains("Replicas", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Replicas", Tag.TAG_COMPOUND);

            for (int index = 0; index < list.size(); index++) {
                CompoundTag entry = list.getCompound(index);

                replicas.put(
                        entry.getString("ReplicaKey"),
                        DnsZoneSnapshot.load(entry)
                );
            }
        }
    }

    private static String fingerprint(List<InternetDnsRecord> records) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            for (InternetDnsRecord record : records) {
                digest.update(
                        record.key()
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                );
                digest.update((byte) '\n');
            }

            return HexFormat.of()
                    .withUpperCase()
                    .formatHex(
                            digest.digest()
                    );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to fingerprint DNS zone.",
                    exception
            );
        }
    }

    private static long nextSerial(long previous) {
        long dateBase = Long.parseLong(
                LocalDate.now(ZoneOffset.UTC)
                        .toString()
                        .replace("-", "")
                        + "00"
        );

        if (previous < dateBase) {
            return dateBase;
        }

        long next = (previous + 1L) & 0xFFFF_FFFFL;

        return next == 0L
                ? 1L
                : next;
    }

    public static String tldOf(String name) {
        String normalized = normalizeZone(name);

        if (".".equals(normalized)) {
            return "";
        }

        int dot = normalized.lastIndexOf('.');

        return dot < 0
                ? normalized
                : normalized.substring(dot + 1);
    }

    public static String normalizeTld(String value) {
        String normalized = normalizeZone(value);

        return normalized.startsWith(".")
                ? normalized.substring(1)
                : normalized;
    }

    public static String normalizeZone(String value) {
        return DnssecEngine.normalizeZone(value);
    }

    private static String replicaKey(
            String ip,
            String zone
    ) {
        return ip
                + "|"
                + normalizeZone(zone);
    }

    public record Delta(
            boolean ixfrAvailable,
            List<InternetDnsRecord> deleted,
            List<InternetDnsRecord> added
    ) {
    }
}
