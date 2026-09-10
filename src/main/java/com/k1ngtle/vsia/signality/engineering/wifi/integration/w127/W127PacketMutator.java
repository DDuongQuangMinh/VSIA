package com.k1ngtle.vsia.signality.engineering.wifi.integration.w127;

import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;

public final class W127PacketMutator {
    private W127PacketMutator() {
    }

    public static OSINetworkPacket copy(OSINetworkPacket packet) {
        return OSINetworkPacket.deserializeNBT(
                packet.serializeNBT().copy()
        );
    }

    public static OSINetworkPacket corruptRrsig(OSINetworkPacket packet) {
        OSINetworkPacket copy = copy(packet);

        mutateText(copy, "dnssec_rrsig");
        mutateText(copy, "record_signature");
        mutateText(copy, "cname_signature");

        return copy;
    }

    public static OSINetworkPacket expireRrsig(
            OSINetworkPacket packet,
            long nowEpochSeconds
    ) {
        OSINetworkPacket copy = copy(packet);

        copy.payload.putLong(
                "dnssec_inception",
                Math.max(0L, nowEpochSeconds - 7200L)
        );

        copy.payload.putLong(
                "dnssec_expiration",
                Math.max(0L, nowEpochSeconds - 1L)
        );

        return copy;
    }

    public static OSINetworkPacket corruptDnskey(OSINetworkPacket packet) {
        OSINetworkPacket copy = copy(packet);

        String value = copy.payload.getString(
                "dnssec_public_raw"
        );

        if (!value.isBlank()) {
            char first = value.charAt(0);
            char replacement = first == 'A' ? 'B' : 'A';

            copy.payload.putString(
                    "dnssec_public_raw",
                    replacement + value.substring(1)
            );
        }

        return copy;
    }

    public static OSINetworkPacket forceTransferSerial(
            OSINetworkPacket packet,
            long serial
    ) {
        OSINetworkPacket copy = copy(packet);

        copy.payload.putLong(
                "from_serial",
                serial
        );

        return copy;
    }

    public static OSINetworkPacket servfail(OSINetworkPacket packet) {
        OSINetworkPacket copy = copy(packet);

        copy.payload.putInt(
                "rcode",
                2
        );

        copy.payload.putString(
                "detail",
                "W1.27 injected SERVFAIL"
        );

        return copy;
    }

    private static void mutateText(
            OSINetworkPacket packet,
            String key
    ) {
        if (!packet.payload.contains(key)) {
            return;
        }

        String value = packet.payload.getString(key);

        if (value.isBlank()) {
            return;
        }

        char first = value.charAt(0);
        char replacement = first == 'A' ? 'B' : 'A';

        packet.payload.putString(
                key,
                replacement + value.substring(1)
        );
    }
}
