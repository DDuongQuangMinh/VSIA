package com.k1ngtle.vsia.phone.network.packet;

import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.phone.browser.PhoneBrowserServerService;
import com.k1ngtle.vsia.phone.network.realism.PhoneWirelessServerService;
import com.k1ngtle.vsia.signality.internet.satellite.internet.LongHaulBrowserService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class C2SPhoneBrowserRequestPacket {
    private static final int MAX_URL =
            1024;

    private static final int MAX_TRANSPORT =
            32;

    private final int requestId;
    private final String url;
    private final String transport;

    public C2SPhoneBrowserRequestPacket(
            int requestId,
            String url,
            String transport
    ) {
        this.requestId =
                requestId;

        this.url =
                url == null
                        ? ""
                        : url;

        this.transport =
                transport == null
                        ? ""
                        : transport;
    }

    public C2SPhoneBrowserRequestPacket(
            FriendlyByteBuf buffer
    ) {
        requestId =
                buffer.readVarInt();

        url =
                buffer.readUtf(
                        MAX_URL
                );

        transport =
                buffer.readUtf(
                        MAX_TRANSPORT
                );
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        buffer.writeVarInt(
                requestId
        );

        buffer.writeUtf(
                url,
                MAX_URL
        );

        buffer.writeUtf(
                transport,
                MAX_TRANSPORT
        );
    }

    public void handle(
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context =
                supplier.get();

        ServerPlayer player =
                context.getSender();

        if (player != null) {
            context.enqueueWork(
                    () ->
                            handleServer(
                                    player
                            )
            );
        }

        context.setPacketHandled(
                true
        );
    }

    private void handleServer(
            ServerPlayer player
    ) {
        PhoneWirelessServerService.AccessDecision access =
                PhoneWirelessServerService
                        .validateDataAccess(
                                player,
                                transport
                        );

        if (!access.allowed()) {
            VsiaNetwork.sendToPlayer(
                    player,
                    new S2CPhoneBrowserResponsePacket(
                            requestId,
                            url,
                            503,
                            "Wireless Access Unavailable",
                            "text/plain; charset=utf-8",
                            "The phone has no usable "
                                    + transport
                                    + " radio access.\n\n"
                                    + access.detail()
                                    + "\n\n"
                                    + "Wi-Fi and Cellular are now range-, obstruction-, interference-, and signal-quality dependent. "
                                    + "Satellite does not replace the local phone radio unless a direct-to-device NTN service is explicitly provisioned.",
                            "",
                            transport,
                            "Browser request blocked by authoritative local RF access validation"
                    )
            );

            return;
        }

        PhoneBrowserServerService.ServerPage page =
                LongHaulBrowserService.fetch(
                        player,
                        url,
                        transport
                );

        String route =
                access.detail();

        if (page.routeSummary() != null
                && !page.routeSummary()
                .isBlank()) {
            route =
                    route
                            + " | "
                            + page.routeSummary();
        }

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
                        route
                )
        );
    }
}
