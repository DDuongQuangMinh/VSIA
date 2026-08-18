package com.k1ngtle.vsia.signality.engineering.wifi.tcp.options;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.stream.TcpSackBlock;

import java.util.List;

public record TcpOptionSet(
        int mss,
        int windowScale,
        boolean sackPermitted,
        List<TcpSackBlock> sackBlocks,
        long timestampValue,
        long timestampEchoReply
) {
    public static final int ABSENT =
            -1;

    public TcpOptionSet {
        sackBlocks =
                sackBlocks == null
                        ? List.of()
                        : List.copyOf(
                                sackBlocks
                        );
    }

    public static TcpOptionSet none() {
        return new TcpOptionSet(
                ABSENT,
                ABSENT,
                false,
                List.of(),
                ABSENT,
                ABSENT
        );
    }

    public static TcpOptionSet synOffer(
            int mss,
            int windowScale,
            boolean sackPermitted,
            long timestampValue
    ) {
        return new TcpOptionSet(
                mss,
                windowScale,
                sackPermitted,
                List.of(),
                timestampValue,
                0L
        );
    }

    public boolean hasMss() {
        return mss > 0;
    }

    public boolean hasWindowScale() {
        return windowScale >= 0;
    }

    public boolean hasTimestamp() {
        return timestampValue >= 0;
    }

    public boolean hasSackBlocks() {
        return !sackBlocks.isEmpty();
    }
}
