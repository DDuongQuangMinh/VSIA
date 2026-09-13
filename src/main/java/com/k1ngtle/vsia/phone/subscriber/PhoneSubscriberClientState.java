package com.k1ngtle.vsia.phone.subscriber;

import com.k1ngtle.vsia.phone.subscriber.packet.C2SPhoneSubscriberActionPacket;
import com.k1ngtle.vsia.signality.internet.field.FieldDeviceNetwork;

public final class PhoneSubscriberClientState {
    private static final PhoneSubscriberClientState INSTANCE =
            new PhoneSubscriberClientState();

    private volatile PhoneSubscriberSnapshot snapshot =
            PhoneSubscriberSnapshot.empty();

    private PhoneSubscriberClientState() {
    }

    public static PhoneSubscriberClientState get() {
        return INSTANCE;
    }

    public PhoneSubscriberSnapshot snapshot() {
        return snapshot;
    }

    public void apply(PhoneSubscriberSnapshot value) {
        snapshot = value == null
                ? PhoneSubscriberSnapshot.empty()
                : value;
    }

    public boolean hasActiveSubscription() {
        return snapshot.hasActiveSubscription();
    }

    public boolean canUseCellularData() {
        return snapshot.canUseCellularData();
    }

    public void requestRefresh() {
        FieldDeviceNetwork.sendToServer(
                C2SPhoneSubscriberActionPacket.refresh()
        );
    }
}
