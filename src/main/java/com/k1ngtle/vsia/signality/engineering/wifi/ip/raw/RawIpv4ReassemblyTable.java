package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Decoder;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Packet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RawIpv4ReassemblyTable {
    public static final long DEFAULT_TIMEOUT_MICROS = 30_000_000L;
    public static final int DEFAULT_MAX_ASSEMBLIES = 64;
    public static final int DEFAULT_MAX_FRAGMENTS_PER_ASSEMBLY = 64;

    private long timeoutMicros;
    private final int maxAssemblies;
    private final int maxFragmentsPerAssembly;

    private final Map<RawIpv4ReassemblyKey, Assembly> assemblies =
            new LinkedHashMap<>();

    public RawIpv4ReassemblyTable() {
        this(
                DEFAULT_TIMEOUT_MICROS,
                DEFAULT_MAX_ASSEMBLIES,
                DEFAULT_MAX_FRAGMENTS_PER_ASSEMBLY
        );
    }

    public RawIpv4ReassemblyTable(long timeoutMicros) {
        this(
                timeoutMicros,
                DEFAULT_MAX_ASSEMBLIES,
                DEFAULT_MAX_FRAGMENTS_PER_ASSEMBLY
        );
    }

    public RawIpv4ReassemblyTable(
            long timeoutMicros,
            int maxAssemblies,
            int maxFragmentsPerAssembly
    ) {
        if (timeoutMicros <= 0) {
            throw new IllegalArgumentException("timeoutMicros");
        }
        if (maxAssemblies < 1) {
            throw new IllegalArgumentException("maxAssemblies");
        }
        if (maxFragmentsPerAssembly < 1) {
            throw new IllegalArgumentException("maxFragmentsPerAssembly");
        }

        this.timeoutMicros = timeoutMicros;
        this.maxAssemblies = maxAssemblies;
        this.maxFragmentsPerAssembly = maxFragmentsPerAssembly;
    }

    public long timeoutMicros() {
        return timeoutMicros;
    }

    public void setTimeoutMicros(long timeoutMicros) {
        if (timeoutMicros <= 0) {
            throw new IllegalArgumentException("timeoutMicros");
        }
        this.timeoutMicros = timeoutMicros;
    }

    public ReassemblyResult accept(
            byte[] rawFragment,
            long nowMicros
    ) {
        expire(nowMicros);

        RawIpv4Packet packet =
                RawIpv4Decoder.decode(rawFragment);

        if (!packet.checksumValid()) {
            return ReassemblyResult.rejected(
                    null,
                    "BAD_HEADER_CHECKSUM"
            );
        }

        if (packet.dontFragment()
                && (packet.moreFragments()
                || packet.fragmentOffset() != 0)) {
            return ReassemblyResult.rejected(
                    keyOf(packet),
                    "DF_FRAGMENT_CONFLICT"
            );
        }

        if (!packet.moreFragments()
                && packet.fragmentOffset() == 0) {
            return ReassemblyResult.complete(
                    Arrays.copyOf(
                            rawFragment,
                            packet.totalLength()
                    )
            );
        }

        byte[] payload = packet.payload();
        int offsetBytes = packet.fragmentOffset() * 8;

        if (packet.moreFragments()
                && (payload.length & 7) != 0) {
            return ReassemblyResult.rejected(
                    keyOf(packet),
                    "NONFINAL_ALIGNMENT"
            );
        }

        long end = (long) offsetBytes + payload.length;

        if (end > 65535L - packet.headerBytes()) {
            return ReassemblyResult.rejected(
                    keyOf(packet),
                    "DATAGRAM_TOO_LARGE"
            );
        }

        RawIpv4ReassemblyKey key = keyOf(packet);

        Assembly assembly = assemblies.get(key);

        if (assembly == null) {
            if (assemblies.size() >= maxAssemblies) {
                return ReassemblyResult.rejected(
                        key,
                        "ASSEMBLY_LIMIT"
                );
            }

            assembly = new Assembly(
                    nowMicros,
                    packet
            );
            assemblies.put(
                    key,
                    assembly
            );
        }

        if (!assembly.compatible(packet)) {
            assemblies.remove(key);
            return ReassemblyResult.rejected(
                    key,
                    "HEADER_MISMATCH"
            );
        }

        Fragment candidate = new Fragment(
                offsetBytes,
                payload,
                packet.moreFragments()
        );

        Fragment exact = assembly.exact(candidate);

        if (exact != null) {
            assembly.lastUpdateMicros = nowMicros;
            return ReassemblyResult.duplicate(
                    key,
                    assembly.fragments.size()
            );
        }

        if (assembly.overlaps(candidate)) {
            assemblies.remove(key);
            return ReassemblyResult.rejected(
                    key,
                    "OVERLAP"
            );
        }

        if (assembly.fragments.size()
                >= maxFragmentsPerAssembly) {
            assemblies.remove(key);
            return ReassemblyResult.rejected(
                    key,
                    "FRAGMENT_LIMIT"
            );
        }

        int candidateFinalLength =
                offsetBytes + payload.length;

        if (!packet.moreFragments()) {
            if (assembly.finalLength >= 0
                    && assembly.finalLength
                    != candidateFinalLength) {
                assemblies.remove(key);
                return ReassemblyResult.rejected(
                        key,
                        "FINAL_LENGTH_CONFLICT"
                );
            }

            assembly.finalLength =
                    candidateFinalLength;
        } else if (assembly.finalLength >= 0
                && candidateFinalLength
                > assembly.finalLength) {
            assemblies.remove(key);
            return ReassemblyResult.rejected(
                    key,
                    "PAST_FINAL_LENGTH"
            );
        }

        assembly.fragments.add(candidate);
        assembly.lastUpdateMicros = nowMicros;

        if (offsetBytes == 0) {
            assembly.zeroFragment =
                    Arrays.copyOf(
                            rawFragment,
                            packet.totalLength()
                    );
        }

        if (!assembly.complete()) {
            return ReassemblyResult.waiting(
                    key,
                    assembly.fragments.size()
            );
        }

        byte[] reassembledPayload =
                assembly.payload();

        byte[] raw =
                RawIpv4Encoder.encode(
                        packet.sourceAddress(),
                        packet.destinationAddress(),
                        assembly.first.dscpEcn(),
                        packet.identification(),
                        false,
                        false,
                        0,
                        assembly.first.ttl(),
                        packet.protocol(),
                        reassembledPayload
                );

        assemblies.remove(key);

        return ReassemblyResult.complete(raw, key);
    }

    public int expire(long nowMicros) {
        return expireDetailed(nowMicros).size();
    }

    public List<ExpiredAssembly> expireDetailed(
            long nowMicros
    ) {
        List<ExpiredAssembly> expired =
                new ArrayList<>();

        var iterator =
                assemblies.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry =
                    iterator.next();

            Assembly assembly =
                    entry.getValue();

            if (nowMicros
                    - assembly.lastUpdateMicros
                    < timeoutMicros) {
                continue;
            }

            expired.add(
                    assembly.expired(
                            entry.getKey()
                    )
            );

            iterator.remove();
        }

        return List.copyOf(expired);
    }

    public ExpiredAssembly expireKey(
            RawIpv4ReassemblyKey key,
            long nowMicros
    ) {
        if (key == null) {
            return null;
        }

        Assembly assembly =
                assemblies.get(key);

        if (assembly == null
                || nowMicros
                - assembly.lastUpdateMicros
                < timeoutMicros) {
            return null;
        }

        assemblies.remove(key);

        return assembly.expired(key);
    }

    public boolean contains(
            RawIpv4ReassemblyKey key
    ) {
        return key != null
                && assemblies.containsKey(key);
    }

    public int pendingAssemblies() {
        return assemblies.size();
    }

    public void clear() {
        assemblies.clear();
    }

    private static RawIpv4ReassemblyKey keyOf(
            RawIpv4Packet packet
    ) {
        return new RawIpv4ReassemblyKey(
                packet.sourceAddress(),
                packet.destinationAddress(),
                packet.protocol(),
                packet.identification()
        );
    }

    private static final class Assembly {
        private final RawIpv4Packet first;
        private final List<Fragment> fragments =
                new ArrayList<>();

        private long lastUpdateMicros;
        private int finalLength = -1;
        private byte[] zeroFragment;

        private Assembly(
                long nowMicros,
                RawIpv4Packet first
        ) {
            this.first = first;
            this.lastUpdateMicros = nowMicros;
        }

        private boolean compatible(
                RawIpv4Packet packet
        ) {
            return first.sourceAddress()
                    .equals(packet.sourceAddress())
                    && first.destinationAddress()
                    .equals(packet.destinationAddress())
                    && first.protocol()
                    == packet.protocol()
                    && first.identification()
                    == packet.identification()
                    && first.dscpEcn()
                    == packet.dscpEcn();
        }

        private Fragment exact(
                Fragment candidate
        ) {
            for (Fragment existing : fragments) {
                if (existing.offsetBytes
                        == candidate.offsetBytes
                        && existing.moreFragments
                        == candidate.moreFragments
                        && Arrays.equals(
                        existing.payload,
                        candidate.payload
                )) {
                    return existing;
                }
            }
            return null;
        }

        private boolean overlaps(
                Fragment candidate
        ) {
            int candidateEnd =
                    candidate.offsetBytes
                            + candidate.payload.length;

            for (Fragment existing : fragments) {
                int existingEnd =
                        existing.offsetBytes
                                + existing.payload.length;

                if (candidate.offsetBytes
                        < existingEnd
                        && existing.offsetBytes
                        < candidateEnd) {
                    return true;
                }
            }

            return false;
        }

        private boolean complete() {
            if (finalLength < 0) {
                return false;
            }

            List<Fragment> sorted =
                    new ArrayList<>(fragments);

            sorted.sort(
                    Comparator.comparingInt(
                            fragment ->
                                    fragment.offsetBytes
                    )
            );

            int cursor = 0;

            for (Fragment fragment : sorted) {
                if (fragment.offsetBytes != cursor) {
                    return false;
                }
                cursor += fragment.payload.length;
            }

            return cursor == finalLength;
        }

        private byte[] payload() {
            List<Fragment> sorted =
                    new ArrayList<>(fragments);

            sorted.sort(
                    Comparator.comparingInt(
                            fragment ->
                                    fragment.offsetBytes
                    )
            );

            byte[] out =
                    new byte[finalLength];

            for (Fragment fragment : sorted) {
                System.arraycopy(
                        fragment.payload,
                        0,
                        out,
                        fragment.offsetBytes,
                        fragment.payload.length
                );
            }

            return out;
        }

        private ExpiredAssembly expired(
                RawIpv4ReassemblyKey key
        ) {
            int receivedBytes = 0;

            for (Fragment fragment : fragments) {
                receivedBytes +=
                        fragment.payload.length;
            }

            return new ExpiredAssembly(
                    key,
                    fragments.size(),
                    receivedBytes,
                    finalLength,
                    zeroFragment != null,
                    zeroFragment == null
                            ? new byte[0]
                            : zeroFragment
            );
        }
    }

    private record Fragment(
            int offsetBytes,
            byte[] payload,
            boolean moreFragments
    ) {
        private Fragment {
            payload =
                    payload == null
                            ? new byte[0]
                            : payload.clone();
        }
    }

    public record ExpiredAssembly(
            RawIpv4ReassemblyKey key,
            int fragments,
            int receivedPayloadBytes,
            int expectedPayloadBytes,
            boolean zeroFragmentSeen,
            byte[] zeroFragment
    ) {
        public ExpiredAssembly {
            zeroFragment =
                    zeroFragment == null
                            ? new byte[0]
                            : zeroFragment.clone();
        }

        @Override
        public byte[] zeroFragment() {
            return zeroFragment.clone();
        }
    }

    public record ReassemblyResult(
            Status status,
            byte[] rawPacket,
            RawIpv4ReassemblyKey key,
            int fragments,
            String reason
    ) {
        public ReassemblyResult {
            rawPacket =
                    rawPacket == null
                            ? new byte[0]
                            : rawPacket.clone();
            reason =
                    reason == null
                            ? ""
                            : reason;
        }

        @Override
        public byte[] rawPacket() {
            return rawPacket.clone();
        }

        public static ReassemblyResult complete(
                byte[] rawPacket
        ) {
            return new ReassemblyResult(
                    Status.COMPLETE,
                    rawPacket,
                    null,
                    0,
                    ""
            );
        }

        public static ReassemblyResult complete(
                byte[] rawPacket,
                RawIpv4ReassemblyKey key
        ) {
            return new ReassemblyResult(
                    Status.COMPLETE,
                    rawPacket,
                    key,
                    0,
                    ""
            );
        }

        public static ReassemblyResult waiting(
                RawIpv4ReassemblyKey key,
                int fragments
        ) {
            return new ReassemblyResult(
                    Status.WAITING,
                    new byte[0],
                    key,
                    fragments,
                    ""
            );
        }

        public static ReassemblyResult duplicate(
                RawIpv4ReassemblyKey key,
                int fragments
        ) {
            return new ReassemblyResult(
                    Status.DUPLICATE,
                    new byte[0],
                    key,
                    fragments,
                    "EXACT_DUPLICATE"
            );
        }

        public static ReassemblyResult rejected(
                RawIpv4ReassemblyKey key,
                String reason
        ) {
            return new ReassemblyResult(
                    Status.REJECTED,
                    new byte[0],
                    key,
                    0,
                    reason
            );
        }
    }

    public enum Status {
        WAITING,
        DUPLICATE,
        COMPLETE,
        REJECTED
    }
}
