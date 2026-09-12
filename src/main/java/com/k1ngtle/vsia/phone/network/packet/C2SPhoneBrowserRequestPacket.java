package com.k1ngtle.vsia.phone.network.packet;

import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.phone.browser.PhoneBrowserServerService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class C2SPhoneBrowserRequestPacket {
    private static final int MAX_URL = 1024;
    private static final int MAX_TRANSPORT = 32;

    private final int requestId;
    private final String url;
    private final String transport;

    public C2SPhoneBrowserRequestPacket(
            int requestId,
            String url,
            String transport
    ) {
        this.requestId = requestId;
        this.url = url == null ? "" : url;
        this.transport = transport == null ? "" : transport;
    }

    public C2SPhoneBrowserRequestPacket(FriendlyByteBuf buf) {
        requestId = buf.readVarInt();
        url = buf.readUtf(MAX_URL);
        transport = buf.readUtf(MAX_TRANSPORT);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(requestId);
        buf.writeUtf(url, MAX_URL);
        buf.writeUtf(transport, MAX_TRANSPORT);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();

        if (player != null) {
            context.enqueueWork(() -> {
                PhoneBrowserServerService.ServerPage page =
                        PhoneBrowserServerService.fetch(
                                player,
                                url,
                                transport
                        );

                VsiaNetwork.sendToPlayer(
                        player,
                        new S2CPhoneBrowserResponsePacket(
                                requestId,
                                page.url(),
                                page.statusCode(),
                                page.reason(),
                                page.contentType(),
                                page.body(),
                                page.styleSheet(),
                                page.transport(),
                                page.routeSummary()
                        )
                );
            });
        }

        context.setPacketHandled(true);
    }
}
