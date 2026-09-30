package com.k1ngtle.vsia.cockpit.network;

import com.k1ngtle.vsia.cockpit.detection.F35DetectionFilter;
import com.k1ngtle.vsia.cockpit.detection.F35DetectionPreferences;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class C2SF35DetectionFilterPacket {
    private final boolean mobs;
    private final boolean players;
    private final boolean ships;

    public C2SF35DetectionFilterPacket(
            boolean mobs,
            boolean players,
            boolean ships
    ) {
        this.mobs =
                mobs;
        this.players =
                players;
        this.ships =
                ships;
    }

    public C2SF35DetectionFilterPacket(
            FriendlyByteBuf buffer
    ) {
        mobs =
                buffer.readBoolean();
        players =
                buffer.readBoolean();
        ships =
                buffer.readBoolean();
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        buffer.writeBoolean(
                mobs
        );
        buffer.writeBoolean(
                players
        );
        buffer.writeBoolean(
                ships
        );
    }

    public void handle(
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context =
                contextSupplier.get();

        context.enqueueWork(
                () -> {
                    ServerPlayer sender =
                            context.getSender();

                    if (sender == null) {
                        return;
                    }

                    F35DetectionPreferences.set(
                            sender.getUUID(),
                            new F35DetectionFilter(
                                    mobs,
                                    players,
                                    ships
                            )
                    );
                }
        );

        context.setPacketHandled(
                true
        );
    }
}
