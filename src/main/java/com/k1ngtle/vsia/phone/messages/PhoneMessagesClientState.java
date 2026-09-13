package com.k1ngtle.vsia.phone.messages;

import com.k1ngtle.vsia.phone.messages.packet.C2SPhoneMessageActionPacket;
import com.k1ngtle.vsia.signality.internet.field.FieldDeviceNetwork;

public final class PhoneMessagesClientState {
    private static final PhoneMessagesClientState INSTANCE =
            new PhoneMessagesClientState();

    private volatile PhoneMessagesSnapshot snapshot =
            PhoneMessagesSnapshot.empty();

    private PhoneMessagesClientState() {
    }

    public static PhoneMessagesClientState get() {
        return INSTANCE;
    }

    public PhoneMessagesSnapshot snapshot() {
        return snapshot;
    }

    public void apply(PhoneMessagesSnapshot value) {
        snapshot = value == null ? PhoneMessagesSnapshot.empty() : value;
    }

    public void requestRefresh() {
        FieldDeviceNetwork.sendToServer(C2SPhoneMessageActionPacket.refresh());
    }
}
