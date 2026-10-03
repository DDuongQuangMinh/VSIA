package com.k1ngtle.vsia.cockpit.network;

import com.k1ngtle.vsia.cockpit.program.*;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record C2SDisplaySourceChunkPacket(UUID session,UUID driveId,long revision,UUID upload,int index,int count,String part){
    public C2SDisplaySourceChunkPacket(FriendlyByteBuf b){this(b.readUUID(),b.readUUID(),b.readLong(),b.readUUID(),b.readVarInt(),b.readVarInt(),b.readUtf(DisplayProgramLimits.UPLOAD_CHARS));}
    public void toBytes(FriendlyByteBuf b){b.writeUUID(session);b.writeUUID(driveId);b.writeLong(revision);b.writeUUID(upload);b.writeVarInt(index);b.writeVarInt(count);b.writeUtf(part,DisplayProgramLimits.UPLOAD_CHARS);}
    public void handle(Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->{if(c.getSender()!=null)DisplayProgrammingServer.chunk(c.getSender(),this);});c.setPacketHandled(true);}
}
