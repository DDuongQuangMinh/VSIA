package com.k1ngtle.vsia.phone.subscriber.packet;

import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberClientState;
import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class S2CPhoneSubscriberSnapshotPacket {
    private static final int MAX_STRING = 256;

    private final PhoneSubscriberSnapshot snapshot;

    public S2CPhoneSubscriberSnapshotPacket(PhoneSubscriberSnapshot snapshot) {
        this.snapshot = snapshot == null
                ? PhoneSubscriberSnapshot.empty()
                : snapshot;
    }

    public S2CPhoneSubscriberSnapshotPacket(FriendlyByteBuf buffer) {
        snapshot = new PhoneSubscriberSnapshot(
                buffer.readUtf(MAX_STRING),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readUtf(MAX_STRING),
                buffer.readUtf(MAX_STRING),
                buffer.readUtf(MAX_STRING),
                buffer.readUtf(MAX_STRING),
                buffer.readUtf(MAX_STRING),
                buffer.readUtf(MAX_STRING),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readUtf(MAX_STRING)
        );
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUtf(snapshot.eid(), MAX_STRING);
        buffer.writeBoolean(snapshot.physicalSimPresent());
        buffer.writeBoolean(snapshot.esimPresent());
        buffer.writeUtf(snapshot.activeType(), MAX_STRING);
        buffer.writeUtf(snapshot.carrier(), MAX_STRING);
        buffer.writeUtf(snapshot.plmn(), MAX_STRING);
        buffer.writeUtf(snapshot.msisdn(), MAX_STRING);
        buffer.writeUtf(snapshot.iccid(), MAX_STRING);
        buffer.writeUtf(snapshot.imsi(), MAX_STRING);
        buffer.writeBoolean(snapshot.authenticated());
        buffer.writeBoolean(snapshot.cellularDataEnabled());
        buffer.writeUtf(snapshot.serviceStatus(), MAX_STRING);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () -> PhoneSubscriberClientState.get().apply(snapshot)
                )
        );
        context.setPacketHandled(true);
    }
}
