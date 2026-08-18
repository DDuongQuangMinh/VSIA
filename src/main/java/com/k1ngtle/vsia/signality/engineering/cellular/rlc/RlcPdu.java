package com.k1ngtle.vsia.signality.engineering.cellular.rlc;

public record RlcPdu(
        int sequenceNumber,
        int segmentIndex,
        int segmentCount,
        byte[] payload
) {
    public RlcPdu {
        payload = payload == null ? new byte[0] : payload.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }
}
