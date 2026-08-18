package com.k1ngtle.vsia.signality.engineering.vm;

public interface ProtocolVmHost {
    void sendFrame(
            byte[] frame
    );

    void deliverToHost(
            byte[] payload
    );

    long currentTick();
}
