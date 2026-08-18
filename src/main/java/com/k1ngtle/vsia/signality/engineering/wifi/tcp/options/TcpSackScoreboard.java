package com.k1ngtle.vsia.signality.engineering.wifi.tcp.options;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpSequence;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.stream.TcpSackBlock;

import java.util.ArrayList;
import java.util.List;

public final class TcpSackScoreboard {
    private final List<TcpSackBlock> blocks =
            new ArrayList<>();

    public void update(
            long cumulativeAck,
            List<TcpSackBlock> newBlocks
    ) {
        blocks.removeIf(
                block ->
                        TcpSequence.beforeOrEqual(
                                block.rightEdge(),
                                cumulativeAck
                        )
        );

        if (newBlocks != null) {
            for (TcpSackBlock block
                    : newBlocks) {
                if (TcpSequence.after(
                        block.rightEdge(),
                        cumulativeAck
                )) {
                    addMerged(
                            block
                    );
                }
            }
        }
    }

    public boolean isSacked(
            long leftEdge,
            long rightEdge
    ) {
        for (TcpSackBlock block
                : blocks) {
            if (TcpSequence.beforeOrEqual(
                    block.leftEdge(),
                    leftEdge
            )
                    && TcpSequence.beforeOrEqual(
                    rightEdge,
                    block.rightEdge()
            )) {
                return true;
            }
        }

        return false;
    }

    public List<TcpSackBlock> blocks() {
        return List.copyOf(
                blocks
        );
    }

    public void clear() {
        blocks.clear();
    }

    private void addMerged(
            TcpSackBlock candidate
    ) {
        long left =
                candidate.leftEdge();

        long right =
                candidate.rightEdge();

        List<TcpSackBlock> keep =
                new ArrayList<>();

        for (TcpSackBlock block
                : blocks) {
            boolean disjoint =
                    TcpSequence.before(
                            right,
                            block.leftEdge()
                    )
                            || TcpSequence.before(
                            block.rightEdge(),
                            left
                    );

            if (disjoint) {
                keep.add(
                        block
                );
            } else {
                if (TcpSequence.before(
                        block.leftEdge(),
                        left
                )) {
                    left =
                            block.leftEdge();
                }

                if (TcpSequence.after(
                        block.rightEdge(),
                        right
                )) {
                    right =
                            block.rightEdge();
                }
            }
        }

        keep.add(
                new TcpSackBlock(
                        left,
                        right
                )
        );

        blocks.clear();

        blocks.addAll(
                keep
        );
    }
}
