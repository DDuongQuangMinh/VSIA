package com.k1ngtle.vsia.network;

import com.k1ngtle.vsia.signality.internet.server.StorageServerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeleteFilePacket {
    public final BlockPos pos;
    public final String fileName;

    public DeleteFilePacket(BlockPos pos, String fileName) {
        this.pos = pos;
        this.fileName = fileName;
    }

    public DeleteFilePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.fileName = buf.readUtf(256);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(fileName, 256);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                BlockEntity be = player.level().getBlockEntity(pos);
                if (be instanceof StorageServerBlockEntity storageBE) {
                    // Permanently deletes the file from the Server
                    storageBE.getStoredFiles().removeIf(f -> f.getName().equals(fileName));
                    storageBE.setChanged();
                }
            }
        });
        context.setPacketHandled(true);
    }
}