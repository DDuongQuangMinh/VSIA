package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Decoder;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Packet;

public final class RawIcmpErrorPolicy {
    private RawIcmpErrorPolicy() {
    }

    public static Decision evaluate(byte[] originalRawIpv4) {
        if (originalRawIpv4 == null
                || originalRawIpv4.length < 20) {
            return Decision.suppress(
                    "INVALID_OR_MISSING_ORIGINAL"
            );
        }

        RawIpv4Packet packet;

        try {
            packet =
                    RawIpv4Decoder.decode(
                            originalRawIpv4
                    );
        } catch (IllegalArgumentException exception) {
            return Decision.suppress(
                    "INVALID_IPV4"
            );
        }

        if (!packet.checksumValid()) {
            return Decision.suppress(
                    "BAD_IPV4_CHECKSUM"
            );
        }

        if (packet.fragmentOffset() != 0) {
            return Decision.suppress(
                    "NON_INITIAL_FRAGMENT"
            );
        }

        if (isUnspecified(packet.sourceAddress())
                || isBroadcast(packet.sourceAddress())
                || isMulticast(packet.sourceAddress())) {
            return Decision.suppress(
                    "INVALID_SOURCE_ADDRESS"
            );
        }

        if (isBroadcast(packet.destinationAddress())
                || isMulticast(packet.destinationAddress())) {
            return Decision.suppress(
                    "BROADCAST_OR_MULTICAST_DESTINATION"
            );
        }

        if (packet.protocol() == 1) {
            byte[] icmp =
                    packet.payload();

            if (icmp.length >= 1
                    && isIcmpErrorType(
                    icmp[0] & 0xFF
            )) {
                return Decision.suppress(
                        "ICMP_ERROR_TO_ICMP_ERROR"
                );
            }
        }

        return Decision.allow();
    }

    public static boolean shouldSend(
            byte[] originalRawIpv4
    ) {
        return evaluate(
                originalRawIpv4
        ).allowed();
    }

    public static boolean isIcmpErrorType(
            int type
    ) {
        return switch (type) {
            case 3, 4, 5, 11, 12 -> true;
            default -> false;
        };
    }

    private static boolean isUnspecified(
            String ip
    ) {
        return "0.0.0.0".equals(ip);
    }

    private static boolean isBroadcast(
            String ip
    ) {
        return "255.255.255.255".equals(ip);
    }

    private static boolean isMulticast(
            String ip
    ) {
        if (ip == null || ip.isBlank()) {
            return false;
        }

        int dot =
                ip.indexOf('.');

        if (dot <= 0) {
            return false;
        }

        try {
            int first =
                    Integer.parseInt(
                            ip.substring(
                                    0,
                                    dot
                            )
                    );

            return first >= 224
                    && first <= 239;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public record Decision(
            boolean allowed,
            String reason
    ) {
        public Decision {
            reason =
                    reason == null
                            ? ""
                            : reason;
        }

        public static Decision allow() {
            return new Decision(
                    true,
                    ""
            );
        }

        public static Decision suppress(
                String reason
        ) {
            return new Decision(
                    false,
                    reason
            );
        }
    }
}
