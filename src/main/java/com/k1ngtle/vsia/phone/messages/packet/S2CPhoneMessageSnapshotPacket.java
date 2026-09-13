package com.k1ngtle.vsia.phone.messages.packet;

import com.k1ngtle.vsia.phone.messages.PhoneMessagesClientState;
import com.k1ngtle.vsia.phone.messages.PhoneMessagesSnapshot;
import com.k1ngtle.vsia.phone.messages.PhoneSmsMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class S2CPhoneMessageSnapshotPacket {
    private static final int MAX_STRING = 384;
    private static final int MAX_MESSAGES = 100;

    private final PhoneMessagesSnapshot snapshot;

    public S2CPhoneMessageSnapshotPacket(PhoneMessagesSnapshot snapshot) {
        this.snapshot = snapshot == null
                ? PhoneMessagesSnapshot.empty()
                : snapshot;
    }

    public S2CPhoneMessageSnapshotPacket(FriendlyByteBuf buffer) {
        String own = buffer.readUtf(MAX_STRING);
        boolean service = buffer.readBoolean();
        String status = buffer.readUtf(MAX_STRING);
        int count = Math.min(MAX_MESSAGES, Math.max(0, buffer.readVarInt()));
        List<PhoneSmsMessage> messages = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            messages.add(new PhoneSmsMessage(
                    buffer.readUUID(),
                    buffer.readUtf(MAX_STRING),
                    buffer.readUtf(MAX_STRING),
                    buffer.readUtf(MAX_STRING),
                    buffer.readLong(),
                    buffer.readUtf(MAX_STRING)
            ));
        }

        snapshot = new PhoneMessagesSnapshot(own, service, status, messages);
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUtf(snapshot.ownNumber(), MAX_STRING);
        buffer.writeBoolean(snapshot.serviceAvailable());
        buffer.writeUtf(snapshot.status(), MAX_STRING);

        int count = Math.min(MAX_MESSAGES, snapshot.messages().size());
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            PhoneSmsMessage message = snapshot.messages().get(i);
            buffer.writeUUID(message.id());
            buffer.writeUtf(message.from(), MAX_STRING);
            buffer.writeUtf(message.to(), MAX_STRING);
            buffer.writeUtf(message.body(), MAX_STRING);
            buffer.writeLong(message.timestampMillis());
            buffer.writeUtf(message.state(), MAX_STRING);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () -> PhoneMessagesClientState.get().apply(snapshot)
                )
        );
        context.setPacketHandled(true);
    }
}
