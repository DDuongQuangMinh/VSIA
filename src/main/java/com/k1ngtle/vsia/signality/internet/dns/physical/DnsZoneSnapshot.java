package com.k1ngtle.vsia.signality.internet.dns.physical;

import com.k1ngtle.vsia.signality.internet.provider.InternetDnsRecord;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record DnsZoneSnapshot(
        String zone,
        long serial,
        String fingerprint,
        long generatedAtMillis,
        List<InternetDnsRecord> records,
        Map<String, String> signatures
) {
    public DnsZoneSnapshot {
        records = records == null ? List.of() : List.copyOf(records);
        signatures = signatures == null ? Map.of() : Map.copyOf(signatures);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Zone", zone);
        tag.putLong("Serial", serial);
        tag.putString("Fingerprint", fingerprint);
        tag.putLong("GeneratedAtMillis", generatedAtMillis);

        ListTag recordList = new ListTag();

        for (InternetDnsRecord record : records) {
            CompoundTag entry = record.save();
            entry.putString(
                    "PhysicalDnsSignature",
                    signatures.getOrDefault(record.key(), "")
            );
            recordList.add(entry);
        }

        tag.put("Records", recordList);

        return tag;
    }

    public static DnsZoneSnapshot load(CompoundTag tag) {
        List<InternetDnsRecord> records = new ArrayList<>();
        Map<String, String> signatures = new LinkedHashMap<>();

        if (tag.contains("Records", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Records", Tag.TAG_COMPOUND);

            for (int index = 0; index < list.size(); index++) {
                CompoundTag entry = list.getCompound(index);
                InternetDnsRecord record = InternetDnsRecord.load(entry);
                records.add(record);

                String signature = entry.getString("PhysicalDnsSignature");

                if (!signature.isBlank()) {
                    signatures.put(record.key(), signature);
                }
            }
        }

        return new DnsZoneSnapshot(
                tag.getString("Zone"),
                tag.getLong("Serial"),
                tag.getString("Fingerprint"),
                tag.getLong("GeneratedAtMillis"),
                records,
                signatures
        );
    }

    public Optional<InternetDnsRecord> direct(
            String requestedName,
            String requestedType
    ) {
        String name = requestedName.toLowerCase(Locale.ROOT);
        String type = requestedType.toUpperCase(Locale.ROOT);

        InternetDnsRecord exact = records.stream()
                .filter(record ->
                        record.name().equalsIgnoreCase(name)
                                && record.type().equalsIgnoreCase(type)
                )
                .findFirst()
                .orElse(null);

        if (exact != null) {
            return Optional.of(exact);
        }

        String wildcard = "*." + zone;

        return records.stream()
                .filter(record ->
                        record.name().equalsIgnoreCase(wildcard)
                                && record.type().equalsIgnoreCase(type)
                )
                .findFirst();
    }

    public Resolution resolve(
            String requestedName,
            String requestedType
    ) {
        String name = requestedName.toLowerCase(Locale.ROOT);
        String type = requestedType.toUpperCase(Locale.ROOT);

        Optional<InternetDnsRecord> direct = direct(name, type);

        if (direct.isPresent()) {
            return new Resolution(
                    direct.get(),
                    null
            );
        }

        if (!"CNAME".equals(type)) {
            InternetDnsRecord cname = records.stream()
                    .filter(record ->
                            record.name().equalsIgnoreCase(name)
                                    && record.type().equalsIgnoreCase("CNAME")
                    )
                    .findFirst()
                    .orElse(null);

            if (cname != null) {
                Optional<InternetDnsRecord> target = direct(
                        cname.value(),
                        type
                );

                if (target.isPresent()) {
                    return new Resolution(
                            target.get(),
                            cname
                    );
                }
            }
        }

        return new Resolution(
                null,
                null
        );
    }

    public String signatureFor(InternetDnsRecord record) {
        if (record == null) {
            return "";
        }

        return signatures.getOrDefault(
                record.key(),
                ""
        );
    }

    public Map<String, InternetDnsRecord> recordsByKey() {
        Map<String, InternetDnsRecord> result = new LinkedHashMap<>();

        records.stream()
                .sorted(Comparator.comparing(InternetDnsRecord::key))
                .forEach(record ->
                        result.put(record.key(), record)
                );

        return result;
    }

    public record Resolution(
            InternetDnsRecord answer,
            InternetDnsRecord cname
    ) {
    }
}
