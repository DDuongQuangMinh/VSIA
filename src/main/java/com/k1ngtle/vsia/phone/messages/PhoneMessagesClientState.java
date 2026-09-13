package com.k1ngtle.vsia.phone.messages;

import com.k1ngtle.vsia.phone.client.PhoneNotificationManager;
import com.k1ngtle.vsia.phone.messages.packet.C2SPhoneMessageActionPacket;
import com.k1ngtle.vsia.signality.internet.field.FieldDeviceNetwork;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PhoneMessagesClientState {
    private static final PhoneMessagesClientState INSTANCE =
            new PhoneMessagesClientState();

    private volatile PhoneMessagesSnapshot snapshot =
            PhoneMessagesSnapshot.empty();

    private final Set<UUID> knownMessageIds =
            new HashSet<>();

    private boolean initialized;

    private PhoneMessagesClientState() {
    }

    public static PhoneMessagesClientState get() {
        return INSTANCE;
    }

    public PhoneMessagesSnapshot snapshot() {
        return snapshot;
    }

    public synchronized void apply(
            PhoneMessagesSnapshot value
    ) {
        PhoneMessagesSnapshot next =
                value == null
                        ? PhoneMessagesSnapshot.empty()
                        : value;

        if (!initialized) {
            for (PhoneSmsMessage message : next.messages()) {
                knownMessageIds.add(message.id());
            }

            initialized = true;
            snapshot = next;
            return;
        }

        String ownNumber = next.ownNumber();

        for (PhoneSmsMessage message : next.messages()) {
            boolean newlySeen =
                    knownMessageIds.add(message.id());

            if (!newlySeen) {
                continue;
            }

            boolean incoming =
                    !ownNumber.isBlank()
                            && ownNumber.equals(message.to());

            if (incoming) {
                PhoneNotificationManager.get()
                        .postMessage(
                                message.from(),
                                message.body()
                        );
            }
        }

        snapshot = next;
    }

    public void requestRefresh() {
        FieldDeviceNetwork.sendToServer(
                C2SPhoneMessageActionPacket.refresh()
        );
    }
}
