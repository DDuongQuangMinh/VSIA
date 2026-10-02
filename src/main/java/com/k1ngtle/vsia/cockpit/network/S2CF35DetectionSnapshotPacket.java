package com.k1ngtle.vsia.cockpit.network;

import com.k1ngtle.vsia.cockpit.detection.F35DetectionContact;
import com.k1ngtle.vsia.cockpit.detection.F35DetectionType;
import com.k1ngtle.vsia.cockpit.detection.F35ShipSilhouette;
import com.k1ngtle.vsia.cockpit.display.F35ClientDetectionCache;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class S2CF35DetectionSnapshotPacket {
    private static final int MAX_CONTACTS =
            256;

    private static final int MAX_GRID_CELLS =
            64 * 64;

    private final long serverTick;
    private final UUID cockpitId;
    private final List<F35DetectionContact> contacts;
    private final F35ShipSilhouette silhouette;

    public S2CF35DetectionSnapshotPacket(
            long serverTick,
            List<F35DetectionContact> contacts,
            F35ShipSilhouette silhouette
    ) {
        this(new UUID(0L, 0L), serverTick, contacts, silhouette);
    }

    public S2CF35DetectionSnapshotPacket(
            UUID cockpitId, long serverTick,
            List<F35DetectionContact> contacts, F35ShipSilhouette silhouette
    ) {
        this.cockpitId = cockpitId == null ? new UUID(0L, 0L) : cockpitId;
        this.serverTick =
                serverTick;

        this.contacts =
                List.copyOf(
                        contacts.subList(
                                0,
                                Math.min(
                                        MAX_CONTACTS,
                                        contacts.size()
                                )
                        )
                );

        this.silhouette =
                silhouette == null
                        ? F35ShipSilhouette.empty()
                        : silhouette;
    }

    public S2CF35DetectionSnapshotPacket(
            FriendlyByteBuf buffer
    ) {
        cockpitId = buffer.readUUID();
        serverTick =
                buffer.readLong();

        int count =
                Math.min(
                        MAX_CONTACTS,
                        buffer.readVarInt()
                );

        List<F35DetectionContact> decoded =
                new ArrayList<>(
                        count
                );

        for (int i = 0;
             i < count;
             i++) {
            decoded.add(
                    readContact(
                            buffer
                    )
            );
        }

        contacts =
                List.copyOf(
                        decoded
                );

        silhouette =
                readSilhouette(
                        buffer
                );
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        buffer.writeUUID(cockpitId);
        buffer.writeLong(
                serverTick
        );

        buffer.writeVarInt(
                contacts.size()
        );

        for (F35DetectionContact contact :
                contacts) {
            writeContact(
                    buffer,
                    contact
            );
        }

        writeSilhouette(
                buffer,
                silhouette
        );
    }

    public void handle(
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context =
                contextSupplier.get();

        context.enqueueWork(
                () ->
                        DistExecutor.unsafeRunWhenOn(
                                Dist.CLIENT,
                                () ->
                                        () ->
                                                F35ClientDetectionCache.accept(
                                                        cockpitId,
                                                        serverTick,
                                                        contacts,
                                                        silhouette
                                                )
                        )
        );

        context.setPacketHandled(
                true
        );
    }

    private static void writeContact(
            FriendlyByteBuf buffer,
            F35DetectionContact contact
    ) {
        buffer.writeUUID(
                contact.contactId()
        );

        buffer.writeVarInt(
                contact.type()
                        .ordinal()
        );

        buffer.writeUtf(
                contact.label(),
                96
        );

        writeVec3(
                buffer,
                contact.position()
        );

        writeVec3(
                buffer,
                contact.velocity()
        );
        buffer.writeBoolean(contact.iffAuthenticated());buffer.writeUtf(contact.iffStatus(),64);buffer.writeUtf(contact.iffTelemetry(),256);
    }

    private static F35DetectionContact readContact(
            FriendlyByteBuf buffer
    ) {
        UUID id =
                buffer.readUUID();

        int ordinal =
                buffer.readVarInt();

        F35DetectionType[] values =
                F35DetectionType.values();

        F35DetectionType type =
                values[
                        Math.max(
                                0,
                                Math.min(
                                        values.length - 1,
                                        ordinal
                                )
                        )
                        ];

        String label =
                buffer.readUtf(
                        96
                );

        Vec3 position =
                readVec3(
                        buffer
                );

        Vec3 velocity =
                readVec3(
                        buffer
                );

        return new F35DetectionContact(
                id,
                type,
                label,
                position,
                velocity, buffer.readBoolean(), buffer.readUtf(64), buffer.readUtf(256)
        );
    }

    private static void writeSilhouette(FriendlyByteBuf buffer,F35ShipSilhouette s) {
        buffer.writeBoolean(s.available());
        if(s.available()){
            buffer.writeVarInt(s.width());buffer.writeVarInt(s.height());buffer.writeVarInt(s.anchorX());buffer.writeVarInt(s.anchorY());
            buffer.writeVarInt(s.sourceWidthBlocks());buffer.writeVarInt(s.sourceLengthBlocks());
            for(int y=0;y<s.height();y++)for(int x=0;x<s.width();x++)buffer.writeBoolean(s.occupied(x,y));
        }
        var damage=s.damage();buffer.writeBoolean(damage.known());buffer.writeUtf(damage.status(),32);
        buffer.writeVarInt(damage.baselineBlocks());buffer.writeVarInt(damage.missingBlocks());
        buffer.writeLong(damage.lastLossTick());buffer.writeLong(damage.observationTick());
        int[] expected=damage.expected(),missing=damage.missing(),health=damage.healthPermille();buffer.writeVarInt(expected.length);
        for(int i=0;i<expected.length;i++){buffer.writeVarInt(expected[i]);buffer.writeVarInt(missing[i]);buffer.writeVarInt(health[i]);}
    }
    private static F35ShipSilhouette readSilhouette(FriendlyByteBuf buffer) {
        boolean available=buffer.readBoolean();int width=0,height=0,ax=-1,ay=-1,sw=0,sl=0;boolean[] occupied=new boolean[0];
        if(available){
            width=bounded(buffer.readVarInt(),1,64);height=bounded(buffer.readVarInt(),1,64);
            ax=bounded(buffer.readVarInt(),-1,width-1);ay=bounded(buffer.readVarInt(),-1,height-1);
            sw=bounded(buffer.readVarInt(),1,131072);sl=bounded(buffer.readVarInt(),1,131072);
            occupied=new boolean[width*height];for(int i=0;i<occupied.length;i++)occupied[i]=buffer.readBoolean();
        }
        boolean known=buffer.readBoolean();String status=buffer.readUtf(32);
        int total=bounded(buffer.readVarInt(),0,32768),lost=bounded(buffer.readVarInt(),0,total);
        long lastLoss=buffer.readLong(),observed=buffer.readLong();
        int count=bounded(buffer.readVarInt(),0,MAX_GRID_CELLS);
        if((known&&(!available||count!=width*height))||(!known&&count!=0))throw new IllegalArgumentException("Invalid F-35 hull damage shape");
        int[] expected=new int[count],missing=new int[count],health=new int[count];
        for(int i=0;i<count;i++){expected[i]=bounded(buffer.readVarInt(),0,32768);missing[i]=bounded(buffer.readVarInt(),0,expected[i]);health[i]=bounded(buffer.readVarInt(),0,1000);}
        var damage=new com.k1ngtle.vsia.cockpit.detection.F35HullDamage(known,status,total,lost,lastLoss,observed,expected,missing,health);
        return new F35ShipSilhouette(width,height,ax,ay,sw,sl,occupied,damage);
    }
    private static int bounded(int value,int min,int max){if(value<min||value>max)throw new IllegalArgumentException("Invalid F-35 hull packet value");return value;}

    private static void writeVec3(
            FriendlyByteBuf buffer,
            Vec3 value
    ) {
        buffer.writeDouble(
                value.x
        );
        buffer.writeDouble(
                value.y
        );
        buffer.writeDouble(
                value.z
        );
    }

    private static Vec3 readVec3(
            FriendlyByteBuf buffer
    ) {
        return new Vec3(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        );
    }
}
