package com.k1ngtle.vsia.signality.engineering.radio;

public final class PttController {
    private boolean transmitting;

    public boolean transmitting() {
        return transmitting;
    }

    public boolean press() {
        if (transmitting) {
            return false;
        }

        transmitting = true;
        return true;
    }

    public void release() {
        transmitting = false;
    }
}
