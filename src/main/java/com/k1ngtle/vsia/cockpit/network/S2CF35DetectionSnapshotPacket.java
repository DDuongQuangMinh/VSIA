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
                velocity
        );
    }

    private static void writeSilhouette(
            FriendlyByteBuf buffer,
            F35ShipSilhouette silhouette
    ) {
        buffer.writeBoolean(
                silhouette.available()
        );

        if (!silhouette.available()) {
            return;
        }

        int width =
                Math.min(
                        64,
                        silhouette.width()
                );

        int height =
                Math.min(
                        64,
                        silhouette.height()
                );

        buffer.writeVarInt(
                width
        );

        buffer.writeVarInt(
                height
        );

        buffer.writeVarInt(
                silhouette.anchorX()
        );

        buffer.writeVarInt(
                silhouette.anchorY()
        );

        buffer.writeVarInt(
                silhouette.sourceWidthBlocks()
        );

        buffer.writeVarInt(
                silhouette.sourceLengthBlocks()
        );

        int cellCount =
                Math.min(
                        MAX_GRID_CELLS,
                        width * height
                );

        buffer.writeVarInt(
                cellCount
        );

        for (int i = 0;
             i < cellCount;
             i++) {
            int x =
                    i % width;

            int y =
                    i / width;

            buffer.writeBoolean(
                    silhouette.occupied(
                            x,
                            y
                    )
            );
        }
    }

    private static F35ShipSilhouette readSilhouette(
            FriendlyByteBuf buffer
    ) {
        if (!buffer.readBoolean()) {
            return F35ShipSilhouette.empty();
        }

        int width =
                Math.min(
                        64,
                        Math.max(
                                0,
                                buffer.readVarInt()
                        )
                );

        int height =
                Math.min(
                        64,
                        Math.max(
                                0,
                                buffer.readVarInt()
                        )
                );

        int anchorX =
                buffer.readVarInt();

        int anchorY =
                buffer.readVarInt();

        int sourceWidth =
                buffer.readVarInt();

        int sourceLength =
                buffer.readVarInt();

        int declaredCells =
                Math.min(
                        MAX_GRID_CELLS,
                        Math.max(
                                0,
                                buffer.readVarInt()
                        )
                );

        int targetCells =
                Math.min(
                        MAX_GRID_CELLS,
                        width * height
                );

        boolean[] occupied =
                new boolean[
                        targetCells
                        ];

        for (int i = 0;
             i < declaredCells;
             i++) {
            boolean value =
                    buffer.readBoolean();

            if (i < occupied.length) {
                occupied[i] =
                        value;
            }
        }

        return new F35ShipSilhouette(
                width,
                height,
                anchorX,
                anchorY,
                sourceWidth,
                sourceLength,
                occupied
        );
    }

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
