package com.k1ngtle.vsia.signality.engineering.wifi.tcp.options;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.stream.TcpSackBlock;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public final class TcpOptionCodec {
    public static final int KIND_EOL =
            0;

    public static final int KIND_NOP =
            1;

    public static final int KIND_MSS =
            2;

    public static final int KIND_WINDOW_SCALE =
            3;

    public static final int KIND_SACK_PERMITTED =
            4;

    public static final int KIND_SACK =
            5;

    public static final int KIND_TIMESTAMP =
            8;

    public static final int MAX_OPTION_BYTES =
            40;

    private TcpOptionCodec() {
    }

    public static byte[] encode(
            TcpOptionSet options
    ) {
        if (options == null) {
            return new byte[0];
        }

        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        if (options.hasMss()) {
            out.write(
                    KIND_MSS
            );

            out.write(
                    4
            );

            put16(
                    out,
                    Math.max(
                            536,
                            Math.min(
                                    65_535,
                                    options.mss()
                            )
                    )
            );
        }

        if (options.sackPermitted()) {
            alignWithNop(
                    out
            );

            out.write(
                    KIND_SACK_PERMITTED
            );

            out.write(
                    2
            );
        }

        if (options.hasWindowScale()) {
            alignWithNop(
                    out
            );

            out.write(
                    KIND_WINDOW_SCALE
            );

            out.write(
                    3
            );

            out.write(
                    Math.max(
                            0,
                            Math.min(
                                    14,
                                    options.windowScale()
                            )
                    )
            );
        }

        if (options.hasTimestamp()) {
            alignWithNop(
                    out
            );

            alignWithNop(
                    out
            );

            out.write(
                    KIND_TIMESTAMP
            );

            out.write(
                    10
            );

            put32(
                    out,
                    options.timestampValue()
            );

            put32(
                    out,
                    Math.max(
                            0L,
                            options.timestampEchoReply()
                    )
            );
        }

        if (options.hasSackBlocks()) {
            List<TcpSackBlock> blocks =
                    options.sackBlocks()
                            .stream()
                            .limit(
                                    4
                            )
                            .toList();

            int available =
                    MAX_OPTION_BYTES
                            - out.size();

            int maxBlocks =
                    Math.max(
                            0,
                            Math.min(
                                    blocks.size(),
                                    (
                                            available
                                                    - 2
                                    )
                                            / 8
                            )
                    );

            if (maxBlocks > 0) {
                out.write(
                        KIND_SACK
                );

                out.write(
                        2
                                + maxBlocks
                                * 8
                );

                for (int i = 0;
                     i < maxBlocks;
                     i++) {
                    TcpSackBlock block =
                            blocks.get(
                                    i
                            );

                    put32(
                            out,
                            block.leftEdge()
                    );

                    put32(
                            out,
                            block.rightEdge()
                    );
                }
            }
        }

        while (
                (
                        out.size()
                                & 3
                )
                        != 0
                && out.size()
                < MAX_OPTION_BYTES
        ) {
            out.write(
                    KIND_EOL
            );
        }

        byte[] encoded =
                out.toByteArray();

        if (encoded.length > MAX_OPTION_BYTES) {
            throw new IllegalStateException(
                    "TCP options exceed 40-byte header option limit"
            );
        }

        return encoded;
    }

    public static TcpOptionSet decode(
            byte[] encoded
    ) {
        if (encoded == null
                || encoded.length == 0) {
            return TcpOptionSet.none();
        }

        int mss =
                TcpOptionSet.ABSENT;

        int windowScale =
                TcpOptionSet.ABSENT;

        boolean sackPermitted =
                false;

        List<TcpSackBlock> sacks =
                new ArrayList<>();

        long tsVal =
                TcpOptionSet.ABSENT;

        long tsEcr =
                TcpOptionSet.ABSENT;

        int i =
                0;

        while (i < encoded.length) {
            int kind =
                    encoded[i]
                            & 0xFF;

            if (kind == KIND_EOL) {
                break;
            }

            if (kind == KIND_NOP) {
                i++;
                continue;
            }

            if (i + 1
                    >= encoded.length) {
                break;
            }

            int length =
                    encoded[i + 1]
                            & 0xFF;

            if (length < 2
                    || i + length
                    > encoded.length) {
                break;
            }

            switch (kind) {
                case KIND_MSS -> {
                    if (length == 4) {
                        mss =
                                read16(
                                        encoded,
                                        i + 2
                                );
                    }
                }

                case KIND_WINDOW_SCALE -> {
                    if (length == 3) {
                        windowScale =
                                Math.min(
                                        14,
                                        encoded[i + 2]
                                                & 0xFF
                                );
                    }
                }

                case KIND_SACK_PERMITTED ->
                        sackPermitted =
                                length == 2;

                case KIND_SACK -> {
                    int payload =
                            length - 2;

                    if (
                            (
                                    payload
                                            % 8
                            )
                                    == 0
                    ) {
                        int blocks =
                                Math.min(
                                        4,
                                        payload / 8
                                );

                        for (int block = 0;
                             block < blocks;
                             block++) {
                            int offset =
                                    i
                                            + 2
                                            + block
                                            * 8;

                            sacks.add(
                                    new TcpSackBlock(
                                            read32(
                                                    encoded,
                                                    offset
                                            ),
                                            read32(
                                                    encoded,
                                                    offset + 4
                                            )
                                    )
                            );
                        }
                    }
                }

                case KIND_TIMESTAMP -> {
                    if (length == 10) {
                        tsVal =
                                read32(
                                        encoded,
                                        i + 2
                                );

                        tsEcr =
                                read32(
                                        encoded,
                                        i + 6
                                );
                    }
                }

                default -> {
                }
            }

            i +=
                    length;
        }

        return new TcpOptionSet(
                mss,
                windowScale,
                sackPermitted,
                sacks,
                tsVal,
                tsEcr
        );
    }

    private static void alignWithNop(
            ByteArrayOutputStream out
    ) {
        if (
                (
                        out.size()
                                & 3
                )
                        != 0
        ) {
            out.write(
                    KIND_NOP
            );
        }
    }

    private static void put16(
            ByteArrayOutputStream out,
            int value
    ) {
        out.write(
                (
                        value
                                >>> 8
                )
                        & 0xFF
        );

        out.write(
                value
                        & 0xFF
        );
    }

    private static void put32(
            ByteArrayOutputStream out,
            long value
    ) {
        long normalized =
                value
                        & 0xFFFF_FFFFL;

        out.write(
                (
                        int
                ) (
                        normalized
                                >>> 24
                )
                        & 0xFF
        );

        out.write(
                (
                        int
                ) (
                        normalized
                                >>> 16
                )
                        & 0xFF
        );

        out.write(
                (
                        int
                ) (
                        normalized
                                >>> 8
                )
                        & 0xFF
        );

        out.write(
                (
                        int
                ) normalized
                        & 0xFF
        );
    }

    private static int read16(
            byte[] data,
            int offset
    ) {
        return (
                (
                        data[offset]
                                & 0xFF
                )
                        << 8
        )
                | (
                data[offset + 1]
                        & 0xFF
        );
    }

    private static long read32(
            byte[] data,
            int offset
    ) {
        return (
                (
                        long
                ) (
                        data[offset]
                                & 0xFF
                )
                        << 24
        )
                | (
                (
                        long
                ) (
                        data[offset + 1]
                                & 0xFF
                )
                        << 16
        )
                | (
                (
                        long
                ) (
                        data[offset + 2]
                                & 0xFF
                )
                        << 8
        )
                | (
                long
        ) (
                data[offset + 3]
                        & 0xFF
        );
    }
}
