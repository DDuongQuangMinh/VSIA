package com.k1ngtle.vsia.signality.engineering.wifi.ip.mtu;

import java.util.ArrayList;
import java.util.List;

public final class Ipv4Fragmenter {
    private Ipv4Fragmenter() {
    }

    public static List<Ipv4Fragment> fragment(
            int identification,
            int headerBytes,
            int mtu,
            byte[] payload
    ) {
        byte[] body =
                payload == null
                        ? new byte[0]
                        : payload.clone();

        if (mtu < headerBytes + 8) {
            throw new IllegalArgumentException(
                    "MTU is too small to carry a useful IPv4 fragment"
            );
        }

        if (headerBytes + body.length
                <= mtu) {
            return List.of(
                    new Ipv4Fragment(
                            identification,
                            0,
                            false,
                            headerBytes,
                            body
                    )
            );
        }

        int maxPayload =
                (
                        (
                                mtu - headerBytes
                        )
                                / 8
                )
                        * 8;

        if (maxPayload <= 0) {
            throw new IllegalArgumentException(
                    "MTU leaves no fragment payload"
            );
        }

        List<Ipv4Fragment> fragments =
                new ArrayList<>();

        int offset =
                0;

        while (offset
                < body.length) {
            int remaining =
                    body.length - offset;

            int length =
                    Math.min(
                            maxPayload,
                            remaining
                    );

            boolean more =
                    offset + length
                            < body.length;

            if (more
                    && length % 8 != 0) {
                throw new IllegalStateException(
                        "Non-final IPv4 fragment is not 8-byte aligned"
                );
            }

            byte[] part =
                    java.util.Arrays.copyOfRange(
                            body,
                            offset,
                            offset + length
                    );

            fragments.add(
                    new Ipv4Fragment(
                            identification,
                            offset,
                            more,
                            headerBytes,
                            part
                    )
            );

            offset +=
                    length;
        }

        return List.copyOf(
                fragments
        );
    }
}
