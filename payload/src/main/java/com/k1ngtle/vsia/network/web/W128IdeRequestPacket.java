package com.k1ngtle.vsia.network.web;

import com.k1ngtle.vsia.signality.internet.web.W128IdeAction;
import com.k1ngtle.vsia.signality.internet.web.W128IdeServer;
import com.k1ngtle.vsia.signality.internet.web.W128WebRegistrySavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class W128IdeRequestPacket {
    private static final int MAX_ACTION = 32;
    private static final int MAX_HOST = 253;
    private static final int MAX_PATH = 512;

    private final W128IdeAction action;
    private final String host;
    private final String path;
    private final String content;

    public W128IdeRequestPacket(W128IdeAction action, String host, String path, String content) {
        this.action = action == null ? W128IdeAction.REFRESH : action;
        this.host = host == null ? "" : host;
        this.path = path == null ? "" : path;
        this.content = content == null ? "" : content;
    }

    public W128IdeRequestPacket(FriendlyByteBuf buf) {
        W128IdeAction decoded;
        try {
            decoded = W128IdeAction.valueOf(buf.readUtf(MAX_ACTION));
        } catch (Exception ignored) {
            decoded = W128IdeAction.REFRESH;
        }

        action = decoded;
        host = buf.readUtf(MAX_HOST);
        path = buf.readUtf(MAX_PATH);
        content = buf.readUtf(W128WebRegistrySavedData.MAX_FILE_CHARACTERS);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(action.name(), MAX_ACTION);
        buf.writeUtf(host, MAX_HOST);
        buf.writeUtf(path, MAX_PATH);
        buf.writeUtf(content, W128WebRegistrySavedData.MAX_FILE_CHARACTERS);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();

        if (player != null) {
            context.enqueueWork(() -> W128IdeServer.handle(player, this));
        }

        context.setPacketHandled(true);
    }

    public W128IdeAction action() {
        return action;
    }

    public String host() {
        return host;
    }

    public String path() {
        return path;
    }

    public String content() {
        return content;
    }
}
