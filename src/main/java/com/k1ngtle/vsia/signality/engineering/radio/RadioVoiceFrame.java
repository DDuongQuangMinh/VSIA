package com.k1ngtle.vsia.signality.engineering.radio;

public record RadioVoiceFrame(
        int sequenceNumber,
        byte[] encodedAudio,
        boolean endOfTransmission
) {
    public RadioVoiceFrame {
        encodedAudio = encodedAudio == null
                ? new byte[0]
                : encodedAudio.clone();
    }

    @Override
    public byte[] encodedAudio() {
        return encodedAudio.clone();
    }
}
