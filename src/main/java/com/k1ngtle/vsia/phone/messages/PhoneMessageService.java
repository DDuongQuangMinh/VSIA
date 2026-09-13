package com.k1ngtle.vsia.phone.messages;

import com.k1ngtle.vsia.phone.messages.packet.S2CPhoneMessageSnapshotPacket;
import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberProfile;
import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberSavedData;
import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberService;
import com.k1ngtle.vsia.signality.internet.field.FieldDeviceNetwork;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

public final class PhoneMessageService {
    private static final int MAX_BODY = 320;

    private PhoneMessageService() {
    }

    public static void refresh(ServerPlayer player) {
        if (player == null) {
            return;
        }

        Optional<PhoneSubscriberProfile> profile =
                PhoneSubscriberService.activeProfile(player);

        if (profile.isEmpty()) {
            send(player, PhoneMessagesSnapshot.empty());
            return;
        }

        boolean service = PhoneSubscriberService.normalCellularServiceAvailable(player);
        String ownNumber = profile.get().msisdn();

        PhoneSmsSavedData store = PhoneSmsSavedData.get(player.serverLevel());
        if (service) {
            store.markDeliveredTo(ownNumber);
        }

        send(
                player,
                new PhoneMessagesSnapshot(
                        ownNumber,
                        service,
                        service
                                ? "SMS service available"
                                : "No cellular service",
                        store.forNumber(ownNumber)
                )
        );
    }

    public static void sendSms(
            ServerPlayer sender,
            String destination,
            String body
    ) {
        if (sender == null) {
            return;
        }

        Optional<PhoneSubscriberProfile> source =
                PhoneSubscriberService.activeProfile(sender);

        if (source.isEmpty()) {
            sender.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "[VSIA SMS] Insert or activate a SIM first."
                    )
            );
            refresh(sender);
            return;
        }

        if (!PhoneSubscriberService.normalCellularServiceAvailable(sender)) {
            sender.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "[VSIA SMS] No cellular service."
                    )
            );
            refresh(sender);
            return;
        }

        String to = PhoneSubscriberSavedData.normalizeNumber(destination);
        String text = sanitizeBody(body);

        if (to.isBlank() || text.isBlank()) {
            refresh(sender);
            return;
        }

        PhoneSubscriberSavedData subscribers =
                PhoneSubscriberSavedData.get(sender.serverLevel());

        if (subscribers.profileByMsisdn(to).isEmpty()) {
            sender.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "[VSIA SMS] Number not found: " + to
                    )
            );
            refresh(sender);
            return;
        }

        boolean delivered = false;
        Optional<UUID> destinationDevice = subscribers.activeDeviceForMsisdn(to);
        if (destinationDevice.isPresent()) {
            ServerPlayer recipient = sender.getServer() == null
                    ? null
                    : sender.getServer()
                    .getPlayerList()
                    .getPlayer(destinationDevice.get());

            if (recipient != null
                    && PhoneSubscriberService.normalCellularServiceAvailable(recipient)) {
                delivered = true;
            }
        }

        PhoneSmsSavedData.get(sender.serverLevel()).add(
                source.get().msisdn(),
                to,
                text,
                delivered ? "DELIVERED" : "STORED"
        );

        refresh(sender);

        if (destinationDevice.isPresent()) {
            ServerPlayer recipient = sender.getServer() == null
                    ? null
                    : sender.getServer()
                    .getPlayerList()
                    .getPlayer(destinationDevice.get());
            if (recipient != null) {
                refresh(recipient);
            }
        }
    }

    private static void send(
            ServerPlayer player,
            PhoneMessagesSnapshot snapshot
    ) {
        FieldDeviceNetwork.sendToPlayer(
                player,
                new S2CPhoneMessageSnapshotPacket(snapshot)
        );
    }

    private static String sanitizeBody(String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() > MAX_BODY) {
            text = text.substring(0, MAX_BODY);
        }
        return text;
    }
}
