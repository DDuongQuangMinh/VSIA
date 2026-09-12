package com.k1ngtle.vsia.phone.network.packet;

import com.k1ngtle.vsia.phone.browser.PhoneBrowser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class S2CPhoneBrowserResponsePacket {
    private static final int MAX_URL = 1024;
    private static final int MAX_REASON = 256;
    private static final int MAX_CONTENT_TYPE = 128;
    private static final int MAX_BODY = 262_144;
    private static final int MAX_STYLE_SHEET = 262_144;
    private static final int MAX_TRANSPORT = 32;
    private static final int MAX_ROUTE = 2048;

    private final int requestId;
    private final String url;
    private final int statusCode;
    private final String reason;
    private final String contentType;
    private final String body;
    private final String styleSheet;
    private final String transport;
    private final String routeSummary;

    public S2CPhoneBrowserResponsePacket(
            int requestId,
            String url,
            int statusCode,
            String reason,
            String contentType,
            String body,
            String styleSheet,
            String transport,
            String routeSummary
    ) {
        this.requestId = requestId;
        this.url = safe(url);
        this.statusCode = statusCode;
        this.reason = safe(reason);
        this.contentType = safe(contentType);
        this.body = safe(body);
        this.styleSheet = safe(styleSheet);
        this.transport = safe(transport);
        this.routeSummary = safe(routeSummary);
    }

    public S2CPhoneBrowserResponsePacket(FriendlyByteBuf buf) {
        requestId = buf.readVarInt();
        url = buf.readUtf(MAX_URL);
        statusCode = buf.readVarInt();
        reason = buf.readUtf(MAX_REASON);
        contentType = buf.readUtf(MAX_CONTENT_TYPE);
        body = buf.readUtf(MAX_BODY);
        styleSheet = buf.readUtf(MAX_STYLE_SHEET);
        transport = buf.readUtf(MAX_TRANSPORT);
        routeSummary = buf.readUtf(MAX_ROUTE);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(requestId);
        buf.writeUtf(url, MAX_URL);
        buf.writeVarInt(statusCode);
        buf.writeUtf(reason, MAX_REASON);
        buf.writeUtf(contentType, MAX_CONTENT_TYPE);
        buf.writeUtf(body, MAX_BODY);
        buf.writeUtf(styleSheet, MAX_STYLE_SHEET);
        buf.writeUtf(transport, MAX_TRANSPORT);
        buf.writeUtf(routeSummary, MAX_ROUTE);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(
                () -> DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () ->
                                PhoneBrowser.get()
                                        .acceptServerResponse(this)
                )
        );

        context.setPacketHandled(true);
    }

    public int requestId() {
        return requestId;
    }

    public String url() {
        return url;
    }

    public int statusCode() {
        return statusCode;
    }

    public String reason() {
        return reason;
    }

    public String contentType() {
        return contentType;
    }

    public String body() {
        return body;
    }

    public String styleSheet() {
        return styleSheet;
    }

    public String transport() {
        return transport;
    }

    public String routeSummary() {
        return routeSummary;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
