package com.k1ngtle.vsia.cockpit.network;

import com.k1ngtle.vsia.cockpit.client.DisplayLaptopScreen;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record S2CDisplayProgramPacket(UUID session,UUID driveId,long revision,InteractionHand hand,boolean open,boolean readOnly,String name,String language,String layout,String source,String status) {
    public S2CDisplayProgramPacket(FriendlyByteBuf b){this(b.readUUID(),b.readUUID(),b.readLong(),b.readEnum(InteractionHand.class),b.readBoolean(),b.readBoolean(),b.readUtf(48),b.readUtf(12),b.readUtf(24576),b.readUtf(com.k1ngtle.vsia.cockpit.program.DisplayProgramLimits.SOURCE_CHARS),b.readUtf(2048));}
    public void toBytes(FriendlyByteBuf b){b.writeUUID(session);b.writeUUID(driveId);b.writeLong(revision);b.writeEnum(hand);b.writeBoolean(open);b.writeBoolean(readOnly);b.writeUtf(name,48);b.writeUtf(language,12);b.writeUtf(layout,24576);b.writeUtf(source,com.k1ngtle.vsia.cockpit.program.DisplayProgramLimits.SOURCE_CHARS);b.writeUtf(status,2048);}
    public void handle(Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->DisplayLaptopScreen.receive(this)));c.setPacketHandled(true);}
}
