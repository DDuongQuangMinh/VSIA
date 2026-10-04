package com.k1ngtle.vsia.cockpit.network;

import com.k1ngtle.vsia.cockpit.client.DisplayLaptopScreen;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record S2CDisplayProgramPacket(UUID session,UUID driveId,long revision,InteractionHand hand,boolean open,boolean readOnly,String name,String language,String layout,String source,String status,BlockPos laptopPos,UUID laptopId) {
    public S2CDisplayProgramPacket {
        if ((laptopPos == null) != (laptopId == null)) throw new IllegalArgumentException("Incomplete laptop anchor");
        if (laptopPos != null) laptopPos = laptopPos.immutable();
    }
    /** Existing handheld callers keep their source-compatible constructor. Wire protocol is 2.7.5. */
    public S2CDisplayProgramPacket(UUID session,UUID driveId,long revision,InteractionHand hand,boolean open,boolean readOnly,String name,String language,String layout,String source,String status) {
        this(session,driveId,revision,hand,open,readOnly,name,language,layout,source,status,null,null);
    }
    private record Anchor(BlockPos pos,UUID id) { }
    private static Anchor anchor(FriendlyByteBuf b) { return b.readBoolean() ? new Anchor(b.readBlockPos(),b.readUUID()) : new Anchor(null,null); }
    private S2CDisplayProgramPacket(UUID session,UUID driveId,long revision,InteractionHand hand,boolean open,boolean readOnly,String name,String language,String layout,String source,String status,Anchor a) {
        this(session,driveId,revision,hand,open,readOnly,name,language,layout,source,status,a.pos(),a.id());
    }
    public S2CDisplayProgramPacket(FriendlyByteBuf b){this(b.readUUID(),b.readUUID(),b.readLong(),b.readEnum(InteractionHand.class),b.readBoolean(),b.readBoolean(),b.readUtf(48),b.readUtf(12),b.readUtf(24576),b.readUtf(com.k1ngtle.vsia.cockpit.program.DisplayProgramLimits.SOURCE_CHARS),b.readUtf(2048),anchor(b));}
    public boolean placed() { return laptopPos != null; }
    public void toBytes(FriendlyByteBuf b){b.writeUUID(session);b.writeUUID(driveId);b.writeLong(revision);b.writeEnum(hand);b.writeBoolean(open);b.writeBoolean(readOnly);b.writeUtf(name,48);b.writeUtf(language,12);b.writeUtf(layout,24576);b.writeUtf(source,com.k1ngtle.vsia.cockpit.program.DisplayProgramLimits.SOURCE_CHARS);b.writeUtf(status,2048);b.writeBoolean(placed());if(placed()){b.writeBlockPos(laptopPos);b.writeUUID(laptopId);}}
    public void handle(Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->DisplayLaptopScreen.receive(this)));c.setPacketHandled(true);}
}
