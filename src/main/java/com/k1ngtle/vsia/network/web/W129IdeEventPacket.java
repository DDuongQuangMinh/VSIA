package com.k1ngtle.vsia.network.web;

import com.k1ngtle.vsia.client.web.W129IdeClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class W129IdeEventPacket {
    private static final int MAX_KIND = 32;
    private static final int MAX_TITLE = 256;
    private static final int MAX_PAYLOAD = 262_144;

    private final String kind;
    private final boolean success;
    private final String title;
    private final String payload;

    public W129IdeEventPacket(
            String kind,
            boolean success,
            String title,
            String payload
    ) {
        this.kind = kind == null ? "OUTPUT" : kind;
        this.success = success;
        this.title = title == null ? "" : title;
        this.payload = payload == null ? "" : payload;
    }

    public W129IdeEventPacket(FriendlyByteBuf buf) {
        kind = buf.readUtf(MAX_KIND);
        success = buf.readBoolean();
        title = buf.readUtf(MAX_TITLE);
        payload = buf.readUtf(MAX_PAYLOAD);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(kind, MAX_KIND);
        buf.writeBoolean(success);
        buf.writeUtf(title, MAX_TITLE);
        buf.writeUtf(payload, MAX_PAYLOAD);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(
                () -> DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () -> W129IdeClient.handle(this)
                )
        );

        context.setPacketHandled(true);
    }

    public String kind() {
        return kind;
    }

    public boolean success() {
        return success;
    }

    public String title() {
        return title;
    }

    public String payload() {
        return payload;
    }
}
