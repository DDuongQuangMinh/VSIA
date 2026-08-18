package com.k1ngtle.vsia.signality.engineering.conformance;

public record PacketInspection(
        int lengthBytes,
        String hex,
        long crc32,
        String sha256
) {
}
