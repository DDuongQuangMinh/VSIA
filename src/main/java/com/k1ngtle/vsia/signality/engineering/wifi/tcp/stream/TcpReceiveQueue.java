package com.k1ngtle.vsia.signality.engineering.wifi.tcp.stream;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpSequence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TcpReceiveQueue {
    private final int capacityBytes;

    private final List<Range> ranges =
            new ArrayList<>();

    private long receiveNext;

    public TcpReceiveQueue(
            long initialReceiveNext,
            int capacityBytes
    ) {
        if (capacityBytes <= 0) {
            throw new IllegalArgumentException(
                    "capacityBytes"
            );
        }

        this.receiveNext =
                TcpSequence.normalize(
                        initialReceiveNext
                );

        this.capacityBytes =
                capacityBytes;
    }

    public long receiveNext() {
        return receiveNext;
    }

    public int bufferedBytes() {
        return ranges.stream()
                .mapToInt(
                        Range::length
                )
                .sum();
    }

    public int advertisedWindowBytes() {
        return Math.max(
                0,
                capacityBytes
                        - bufferedBytes()
        );
    }

    public List<TcpSackBlock> sackBlocks(
            int maxBlocks
    ) {
        if (maxBlocks <= 0) {
            return List.of();
        }

        return ranges.stream()
                .filter(
                        range ->
                                range.start()
                                        != receiveNext
                )
                .sorted(
                        Comparator.comparingLong(
                                Range::start
                        ).reversed()
                )
                .limit(
                        maxBlocks
                )
                .map(
                        range ->
                                new TcpSackBlock(
                                        range.start(),
                                        range.end()
                                )
                )
                .toList();
    }

    public TcpReceiveResult accept(
            long sequence,
            int payloadBytes
    ) {
        if (payloadBytes <= 0) {
            return snapshotResult(
                    0,
                    0,
                    false,
                    false
            );
        }

        long start =
                TcpSequence.normalize(
                        sequence
                );

        long end =
                TcpSequence.add(
                        start,
                        payloadBytes
                );

        if (TcpSequence.beforeOrEqual(
                end,
                receiveNext
        )) {
            return snapshotResult(
                    0,
                    payloadBytes,
                    false,
                    true
            );
        }

        int duplicateBytes =
                0;

        if (TcpSequence.before(
                start,
                receiveNext
        )) {
            duplicateBytes =
                    (
                            int
                    ) Math.min(
                            payloadBytes,
                            TcpSequence.distance(
                                    start,
                                    receiveNext
                            )
                    );

            start =
                    receiveNext;
        }

        int requested =
                (
                        int
                ) TcpSequence.distance(
                        start,
                        end
                );

        int room =
                advertisedWindowBytes();

        int accepted =
                Math.min(
                        requested,
                        room
                );

        if (accepted <= 0) {
            return snapshotResult(
                    0,
                    duplicateBytes,
                    start != receiveNext,
                    false
            );
        }

        end =
                TcpSequence.add(
                        start,
                        accepted
                );

        int overlap =
                overlapBytes(
                        start,
                        end
                );

        int newlyAccepted =
                Math.max(
                        0,
                        accepted
                                - overlap
                );

        duplicateBytes +=
                overlap;

        if (newlyAccepted > 0) {
            insertAndMerge(
                    start,
                    end
            );
        }

        boolean outOfOrder =
                start != receiveNext;

        advanceContiguous();

        return snapshotResult(
                newlyAccepted,
                duplicateBytes,
                outOfOrder,
                newlyAccepted == 0
        );
    }

    public void resetReceiveNext(
            long value
    ) {
        receiveNext =
                TcpSequence.normalize(
                        value
                );

        ranges.clear();
    }

    private TcpReceiveResult snapshotResult(
            int newlyAccepted,
            int duplicateBytes,
            boolean outOfOrder,
            boolean duplicateOnly
    ) {
        return new TcpReceiveResult(
                receiveNext,
                newlyAccepted,
                duplicateBytes,
                bufferedBytes(),
                advertisedWindowBytes(),
                outOfOrder,
                duplicateOnly,
                sackBlocks(
                        4
                )
        );
    }

    private int overlapBytes(
            long start,
            long end
    ) {
        int overlap =
                0;

        for (Range range : ranges) {
            long overlapStart =
                    later(
                            start,
                            range.start()
                    );

            long overlapEnd =
                    earlier(
                            end,
                            range.end()
                    );

            if (TcpSequence.before(
                    overlapStart,
                    overlapEnd
            )) {
                overlap +=
                        (
                                int
                        ) TcpSequence.distance(
                                overlapStart,
                                overlapEnd
                        );
            }
        }

        return overlap;
    }

    private void insertAndMerge(
            long start,
            long end
    ) {
        ranges.add(
                new Range(
                        start,
                        end
                )
        );

        ranges.sort(
                Comparator.comparingLong(
                        range ->
                                TcpSequence.distance(
                                        receiveNext,
                                        range.start()
                                )
                )
        );

        List<Range> merged =
                new ArrayList<>();

        for (Range current : ranges) {
            if (merged.isEmpty()) {
                merged.add(
                        current
                );

                continue;
            }

            Range previous =
                    merged.get(
                            merged.size()
                                    - 1
                    );

            if (!TcpSequence.after(
                    current.start(),
                    previous.end()
            )) {
                merged.set(
                        merged.size() - 1,
                        new Range(
                                previous.start(),
                                later(
                                        previous.end(),
                                        current.end()
                                )
                        )
                );
            } else {
                merged.add(
                        current
                );
            }
        }

        ranges.clear();
        ranges.addAll(
                merged
        );
    }

    private void advanceContiguous() {
        boolean progressed;

        do {
            progressed =
                    false;

            for (int i = 0; i < ranges.size(); i++) {
                Range range =
                        ranges.get(
                                i
                        );

                if (range.start()
                        == receiveNext) {
                    receiveNext =
                            range.end();

                    ranges.remove(
                            i
                    );

                    progressed =
                            true;

                    break;
                }
            }
        } while (progressed);
    }

    private static long earlier(
            long a,
            long b
    ) {
        return TcpSequence.before(
                a,
                b
        )
                ? a
                : b;
    }

    private static long later(
            long a,
            long b
    ) {
        return TcpSequence.after(
                a,
                b
        )
                ? a
                : b;
    }

    private record Range(
            long start,
            long end
    ) {
        private int length() {
            return (
                    int
            ) TcpSequence.distance(
                    start,
                    end
            );
        }
    }
}
