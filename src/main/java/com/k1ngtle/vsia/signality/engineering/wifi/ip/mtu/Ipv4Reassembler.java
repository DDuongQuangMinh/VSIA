package com.k1ngtle.vsia.signality.engineering.wifi.ip.mtu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Ipv4Reassembler {
    private Ipv4Reassembler() {
    }

    public static byte[] reassemble(
            List<Ipv4Fragment> fragments
    ) {
        if (fragments == null
                || fragments.isEmpty()) {
            throw new IllegalArgumentException(
                    "No IPv4 fragments"
            );
        }

        List<Ipv4Fragment> sorted =
                new ArrayList<>(
                        fragments
                );

        sorted.sort(
                Comparator.comparingInt(
                        Ipv4Fragment::offsetBytes
                )
        );

        int identification =
                sorted.get(0)
                        .identification();

        int expectedOffset =
                0;

        int totalPayload =
                0;

        boolean sawFinal =
                false;

        for (Ipv4Fragment fragment
                : sorted) {
            if (fragment.identification()
                    != identification) {
                throw new IllegalArgumentException(
                        "Mixed IPv4 identification values"
                );
            }

            if (fragment.offsetBytes()
                    != expectedOffset) {
                throw new IllegalArgumentException(
                        fragment.offsetBytes()
                                < expectedOffset
                                ? "Overlapping IPv4 fragments"
                                : "Missing IPv4 fragment range"
                );
            }

            if (sawFinal) {
                throw new IllegalArgumentException(
                        "Data exists after final IPv4 fragment"
                );
            }

            expectedOffset +=
                    fragment.payload().length;

            totalPayload +=
                    fragment.payload().length;

            if (!fragment.moreFragments()) {
                sawFinal =
                        true;
            }
        }

        if (!sawFinal) {
            throw new IllegalArgumentException(
                    "Final IPv4 fragment is missing"
            );
        }

        byte[] output =
                new byte[
                        totalPayload
                ];

        int write =
                0;

        for (Ipv4Fragment fragment
                : sorted) {
            byte[] payload =
                    fragment.payload();

            System.arraycopy(
                    payload,
                    0,
                    output,
                    write,
                    payload.length
            );

            write +=
                    payload.length;
        }

        return output;
    }
}
