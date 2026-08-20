package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Ipv6FragmentReassemblyTable {
    public static final long DEFAULT_TIMEOUT_MICROS = 60_000_000L;
    public static final int DEFAULT_MAX_ASSEMBLIES = 64;
    public static final int DEFAULT_MAX_FRAGMENTS = 64;

    private final long timeoutMicros;
    private final int maxAssemblies;
    private final int maxFragments;
    private final Map<Key, Assembly> assemblies = new LinkedHashMap<>();

    public Ipv6FragmentReassemblyTable() {
        this(DEFAULT_TIMEOUT_MICROS, DEFAULT_MAX_ASSEMBLIES, DEFAULT_MAX_FRAGMENTS);
    }

    public Ipv6FragmentReassemblyTable(
            long timeoutMicros,
            int maxAssemblies,
            int maxFragments
    ) {
        if (timeoutMicros <= 0) throw new IllegalArgumentException("timeoutMicros");
        if (maxAssemblies < 1) throw new IllegalArgumentException("maxAssemblies");
        if (maxFragments < 1) throw new IllegalArgumentException("maxFragments");

        this.timeoutMicros = timeoutMicros;
        this.maxAssemblies = maxAssemblies;
        this.maxFragments = maxFragments;
    }

    public Result accept(byte[] rawIpv6, long nowMicros) {
        expire(nowMicros);

        RawIpv6Packet packet = RawIpv6Codec.decode(rawIpv6);
        if (packet.nextHeader() != Ipv6FragmentHeader.NEXT_HEADER_FRAGMENT) {
            return Result.complete(Arrays.copyOf(rawIpv6, packet.totalLength()));
        }

        byte[] payload = packet.payload();
        if (payload.length < 8) {
            return Result.rejected(null, "TRUNCATED_FRAGMENT_HEADER");
        }

        Ipv6FragmentHeader header =
                Ipv6FragmentHeader.decode(
                        Arrays.copyOfRange(payload, 0, 8)
                );

        byte[] fragmentData =
                Arrays.copyOfRange(
                        payload,
                        8,
                        payload.length
                );

        if (header.moreFragments()
                && (fragmentData.length & 7) != 0) {
            return Result.rejected(
                    key(packet, header),
                    "NONFINAL_ALIGNMENT"
            );
        }

        int start = header.fragmentOffsetUnits() * 8;
        long endLong = (long) start + fragmentData.length;
        if (endLong > 65535L) {
            return Result.rejected(
                    key(packet, header),
                    "REASSEMBLED_PAYLOAD_TOO_LARGE"
            );
        }

        Key key = key(packet, header);
        Assembly assembly = assemblies.get(key);

        if (assembly == null) {
            if (assemblies.size() >= maxAssemblies) {
                return Result.rejected(key, "ASSEMBLY_LIMIT");
            }

            assembly = new Assembly(
                    packet.trafficClass(),
                    packet.flowLabel(),
                    packet.hopLimit(),
                    nowMicros
            );

            assemblies.put(key, assembly);
        }

        Fragment candidate =
                new Fragment(
                        start,
                        fragmentData,
                        header.moreFragments()
                );

        if (assembly.exact(candidate)) {
            assembly.lastUpdateMicros = nowMicros;
            return Result.duplicate(
                    key,
                    assembly.fragments.size()
            );
        }

        if (assembly.overlaps(candidate)) {
            assemblies.remove(key);
            return Result.rejected(key, "OVERLAP");
        }

        if (assembly.fragments.size() >= maxFragments) {
            assemblies.remove(key);
            return Result.rejected(key, "FRAGMENT_LIMIT");
        }

        int end = start + fragmentData.length;

        if (!header.moreFragments()) {
            if (assembly.finalLength >= 0
                    && assembly.finalLength != end) {
                assemblies.remove(key);
                return Result.rejected(
                        key,
                        "FINAL_LENGTH_CONFLICT"
                );
            }

            assembly.finalLength = end;
        }

        assembly.fragments.add(candidate);
        assembly.lastUpdateMicros = nowMicros;

        if (!assembly.complete()) {
            return Result.waiting(
                    key,
                    assembly.fragments.size()
            );
        }

        byte[] reassembledPayload = assembly.payload();

        byte[] complete =
                RawIpv6Codec.encode(
                        key.source(),
                        key.destination(),
                        assembly.trafficClass,
                        assembly.flowLabel,
                        key.upperLayerNextHeader(),
                        assembly.hopLimit,
                        reassembledPayload
                );

        assemblies.remove(key);

        return Result.complete(complete);
    }

    public int expire(long nowMicros) {
        int count = 0;
        var iterator = assemblies.entrySet().iterator();

        while (iterator.hasNext()) {
            Assembly assembly = iterator.next().getValue();

            if (nowMicros - assembly.lastUpdateMicros >= timeoutMicros) {
                iterator.remove();
                count++;
            }
        }

        return count;
    }

    public int pendingAssemblies() {
        return assemblies.size();
    }

    private static Key key(
            RawIpv6Packet packet,
            Ipv6FragmentHeader header
    ) {
        return new Key(
                packet.source(),
                packet.destination(),
                header.nextHeader(),
                header.identification()
        );
    }

    public record Key(
            Ipv6Address source,
            Ipv6Address destination,
            int upperLayerNextHeader,
            long identification
    ) {
    }

    public enum Status {
        WAITING,
        DUPLICATE,
        COMPLETE,
        REJECTED
    }

    public record Result(
            Status status,
            byte[] rawPacket,
            Key key,
            int fragments,
            String reason
    ) {
        public Result {
            rawPacket = rawPacket == null ? new byte[0] : rawPacket.clone();
            reason = reason == null ? "" : reason;
        }

        @Override
        public byte[] rawPacket() {
            return rawPacket.clone();
        }

        public static Result complete(byte[] raw) {
            return new Result(Status.COMPLETE, raw, null, 0, "");
        }

        public static Result waiting(Key key, int fragments) {
            return new Result(Status.WAITING, new byte[0], key, fragments, "");
        }

        public static Result duplicate(Key key, int fragments) {
            return new Result(Status.DUPLICATE, new byte[0], key, fragments, "EXACT_DUPLICATE");
        }

        public static Result rejected(Key key, String reason) {
            return new Result(Status.REJECTED, new byte[0], key, 0, reason);
        }
    }

    private static final class Assembly {
        private final int trafficClass;
        private final int flowLabel;
        private final int hopLimit;
        private final List<Fragment> fragments = new ArrayList<>();
        private long lastUpdateMicros;
        private int finalLength = -1;

        private Assembly(
                int trafficClass,
                int flowLabel,
                int hopLimit,
                long nowMicros
        ) {
            this.trafficClass = trafficClass;
            this.flowLabel = flowLabel;
            this.hopLimit = hopLimit;
            this.lastUpdateMicros = nowMicros;
        }

        private boolean exact(Fragment candidate) {
            return fragments.stream().anyMatch(existing ->
                    existing.start == candidate.start
                            && existing.more == candidate.more
                            && Arrays.equals(existing.payload, candidate.payload)
            );
        }

        private boolean overlaps(Fragment candidate) {
            int candidateEnd = candidate.start + candidate.payload.length;

            for (Fragment existing : fragments) {
                int existingEnd = existing.start + existing.payload.length;

                if (candidate.start < existingEnd
                        && existing.start < candidateEnd) {
                    return true;
                }
            }

            return false;
        }

        private boolean complete() {
            if (finalLength < 0) return false;

            List<Fragment> ordered = new ArrayList<>(fragments);
            ordered.sort(Comparator.comparingInt(fragment -> fragment.start));

            int cursor = 0;
            for (Fragment fragment : ordered) {
                if (fragment.start != cursor) return false;
                cursor += fragment.payload.length;
            }

            return cursor == finalLength;
        }

        private byte[] payload() {
            byte[] out = new byte[finalLength];

            for (Fragment fragment : fragments) {
                System.arraycopy(
                        fragment.payload,
                        0,
                        out,
                        fragment.start,
                        fragment.payload.length
                );
            }

            return out;
        }
    }

    private record Fragment(
            int start,
            byte[] payload,
            boolean more
    ) {
        private Fragment {
            payload = payload == null ? new byte[0] : payload.clone();
        }
    }
}
