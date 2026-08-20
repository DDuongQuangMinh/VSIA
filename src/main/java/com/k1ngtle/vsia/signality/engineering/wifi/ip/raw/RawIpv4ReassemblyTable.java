package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Decoder;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Packet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RawIpv4ReassemblyTable {
    public static final long DEFAULT_TIMEOUT_MICROS =
            30_000_000L;

    private final long timeoutMicros;

    private final Map<RawIpv4ReassemblyKey, Assembly> assemblies =
            new LinkedHashMap<>();

    public RawIpv4ReassemblyTable() {
        this(DEFAULT_TIMEOUT_MICROS);
    }

    public RawIpv4ReassemblyTable(
            long timeoutMicros
    ) {
        if (timeoutMicros <= 0) {
            throw new IllegalArgumentException(
                    "timeoutMicros"
            );
        }

        this.timeoutMicros =
                timeoutMicros;
    }

    public ReassemblyResult accept(
            byte[] rawFragment,
            long nowMicros
    ) {
        expire(
                nowMicros
        );

        RawIpv4Packet packet =
                RawIpv4Decoder.decode(
                        rawFragment
                );

        if (!packet.checksumValid()) {
            return ReassemblyResult.rejected(
                    "BAD_HEADER_CHECKSUM"
            );
        }

        if (packet.dontFragment()
                && (packet.moreFragments()
                || packet.fragmentOffset() != 0)) {
            return ReassemblyResult.rejected(
                    "DF_FRAGMENT_CONFLICT"
            );
        }

        if (!packet.moreFragments()
                && packet.fragmentOffset() == 0) {
            return ReassemblyResult.complete(
                    java.util.Arrays.copyOf(
                            rawFragment,
                            packet.totalLength()
                    )
            );
        }

        int offsetBytes =
                packet.fragmentOffset() * 8;

        byte[] payload =
                packet.payload();

        if (packet.moreFragments()
                && (payload.length & 7) != 0) {
            return ReassemblyResult.rejected(
                    "NONFINAL_ALIGNMENT"
            );
        }

        RawIpv4ReassemblyKey key =
                new RawIpv4ReassemblyKey(
                        packet.sourceAddress(),
                        packet.destinationAddress(),
                        packet.protocol(),
                        packet.identification()
                );

        Assembly assembly =
                assemblies.computeIfAbsent(
                        key,
                        ignored ->
                                new Assembly(
                                        nowMicros,
                                        packet
                                )
                );

        if (!assembly.compatible(
                packet
        )) {
            assemblies.remove(
                    key
            );

            return ReassemblyResult.rejected(
                    "HEADER_MISMATCH"
            );
        }

        Fragment fragment =
                new Fragment(
                        offsetBytes,
                        payload,
                        packet.moreFragments()
                );

        if (assembly.overlaps(
                fragment
        )) {
            assemblies.remove(
                    key
            );

            return ReassemblyResult.rejected(
                    "OVERLAP"
            );
        }

        assembly.fragments.add(
                fragment
        );

        assembly.lastUpdateMicros =
                nowMicros;

        if (!packet.moreFragments()) {
            assembly.finalLength =
                    offsetBytes
                            + payload.length;
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

        assemblies.remove(
                key
        );

        return ReassemblyResult.complete(
                raw
        );
    }

    public int expire(
            long nowMicros
    ) {
        int before =
                assemblies.size();

        assemblies.entrySet()
                .removeIf(
                        entry ->
                                nowMicros
                                        - entry.getValue()
                                        .lastUpdateMicros
                                        >= timeoutMicros
                );

        return before
                - assemblies.size();
    }

    public int pendingAssemblies() {
        return assemblies.size();
    }

    private static final class Assembly {
        private final RawIpv4Packet first;

        private final List<Fragment> fragments =
                new ArrayList<>();

        private long lastUpdateMicros;

        private int finalLength =
                -1;

        private Assembly(
                long nowMicros,
                RawIpv4Packet first
        ) {
            this.first =
                    first;

            this.lastUpdateMicros =
                    nowMicros;
        }

        private boolean compatible(
                RawIpv4Packet packet
        ) {
            return first.sourceAddress()
                    .equals(
                            packet.sourceAddress()
                    )
                    && first.destinationAddress()
                    .equals(
                            packet.destinationAddress()
                    )
                    && first.protocol()
                    == packet.protocol()
                    && first.identification()
                    == packet.identification();
        }

        private boolean overlaps(
                Fragment candidate
        ) {
            int candidateEnd =
                    candidate.offsetBytes
                            + candidate.payload.length;

            for (Fragment existing
                    : fragments) {
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
                    new ArrayList<>(
                            fragments
                    );

            sorted.sort(
                    Comparator.comparingInt(
                            fragment ->
                                    fragment.offsetBytes
                    )
            );

            int cursor =
                    0;

            for (Fragment fragment
                    : sorted) {
                if (fragment.offsetBytes
                        != cursor) {
                    return false;
                }

                cursor +=
                        fragment.payload.length;
            }

            return cursor
                    == finalLength;
        }

        private byte[] payload() {
            List<Fragment> sorted =
                    new ArrayList<>(
                            fragments
                    );

            sorted.sort(
                    Comparator.comparingInt(
                            fragment ->
                                    fragment.offsetBytes
                    )
            );

            byte[] out =
                    new byte[
                            finalLength
                    ];

            for (Fragment fragment
                    : sorted) {
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

        public static ReassemblyResult rejected(
                String reason
        ) {
            return new ReassemblyResult(
                    Status.REJECTED,
                    new byte[0],
                    null,
                    0,
                    reason
            );
        }
    }

    public enum Status {
        WAITING,
        COMPLETE,
        REJECTED
    }
}
