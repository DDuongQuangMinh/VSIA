package com.k1ngtle.vsia.network;

import com.k1ngtle.vsia.signality.internet.server.StorageServerBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.StoredFile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UploadFilePacket {
    public final BlockPos pos;
    public final String fileName;
    public final String language;
    public final String content;

    public UploadFilePacket(BlockPos pos, String fileName, String language, String content) {
        this.pos = pos;
        this.fileName = fileName;
        this.language = language;
        this.content = content;
    }

    public UploadFilePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.fileName = buf.readUtf(256);
        this.language = buf.readUtf(64);
        // Uses an overridden maxLength to allow up to 10MB if Forge's channel payload allows it
        this.content = buf.readUtf(10 * 1024 * 1024);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(fileName, 256);
        buf.writeUtf(language, 64);
        buf.writeUtf(content, 10 * 1024 * 1024);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity be = player.level().getBlockEntity(pos);
                if (be instanceof StorageServerBlockEntity storageBE) {
                    // Permanently adds the file to the Server
                    storageBE.addStoredFile(new StoredFile(fileName, language, content));
                }
            }
        });
        context.setPacketHandled(true);
    }
}