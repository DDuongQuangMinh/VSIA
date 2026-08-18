package com.k1ngtle.vsia.signality.engineering.cellular.pdcp;

public record PdcpPdu(
        int sequenceNumber,
        byte[] protectedPayload,
        byte[] integrityTag
) {
    public PdcpPdu {
        protectedPayload = protectedPayload == null
                ? new byte[0]
                : protectedPayload.clone();

        integrityTag = integrityTag == null
                ? new byte[0]
                : integrityTag.clone();
    }

    @Override
    public byte[] protectedPayload() {
        return protectedPayload.clone();
    }

    @Override
    public byte[] integrityTag() {
        return integrityTag.clone();
    }
}
