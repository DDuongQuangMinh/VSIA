package com.k1ngtle.vsia.cockpit.network;

import com.k1ngtle.vsia.cockpit.program.DisplayProgrammingServer;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record C2SDisplayProgramPacket(UUID session,UUID driveId,long revision,int action,String name,String language,String layout,String source,UUID upload) {
    public C2SDisplayProgramPacket(UUID session,UUID driveId,long revision,int action,String name,String language,String layout,String source){this(session,driveId,revision,action,name,language,layout,source,null);}
    public C2SDisplayProgramPacket(FriendlyByteBuf b){this(b.readUUID(),b.readUUID(),b.readLong(),b.readVarInt(),b.readUtf(48),b.readUtf(12),b.readUtf(24576),b.readUtf(2048),b.readBoolean()?b.readUUID():null);}
    public void toBytes(FriendlyByteBuf b){b.writeUUID(session);b.writeUUID(driveId);b.writeLong(revision);b.writeVarInt(action);b.writeUtf(name,48);b.writeUtf(language,12);b.writeUtf(layout,24576);b.writeUtf(source,2048);b.writeBoolean(upload!=null);if(upload!=null)b.writeUUID(upload);}
    public void handle(Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->{if(c.getSender()!=null)DisplayProgrammingServer.action(c.getSender(),this);});c.setPacketHandled(true);}
}
