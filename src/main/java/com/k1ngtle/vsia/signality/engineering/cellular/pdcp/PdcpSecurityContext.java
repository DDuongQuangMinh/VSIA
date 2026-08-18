package com.k1ngtle.vsia.signality.engineering.cellular.pdcp;

public record PdcpSecurityContext(
        byte[] cipherKey,
        byte[] integrityKey
) {
    public PdcpSecurityContext {
        cipherKey = cipherKey == null ? new byte[0] : cipherKey.clone();
        integrityKey = integrityKey == null ? new byte[0] : integrityKey.clone();
    }

    @Override
    public byte[] cipherKey() {
        return cipherKey.clone();
    }

    @Override
    public byte[] integrityKey() {
        return integrityKey.clone();
    }
}
