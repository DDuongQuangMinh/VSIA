package com.k1ngtle.vsia.network.wifi;

import com.k1ngtle.vsia.client.wifi.WifiEngineeringClientPacketHandler;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class WifiMultiEngineeringOpenPacket {
    public static final int DEVICE_COUNT = 4;

    private final List<UUID> deviceIds;

    public WifiMultiEngineeringOpenPacket(List<UUID> deviceIds) {
        this.deviceIds = validateIds(deviceIds);
    }

    public WifiMultiEngineeringOpenPacket(
            List<BlockPos> positions,
            List<WifiEngineeringSnapshot> snapshots
    ) {
        if (positions == null
                || snapshots == null
                || positions.size() != DEVICE_COUNT
                || snapshots.size() != DEVICE_COUNT) {
            throw new IllegalArgumentException(
                    "W1.23.3 requires exactly four positions and four snapshots"
            );
        }

        List<UUID> ids = new ArrayList<>(DEVICE_COUNT);

        for (WifiEngineeringSnapshot snapshot : snapshots) {
            if (snapshot == null || snapshot.deviceId() == null) {
                throw new IllegalArgumentException(
                        "Every W1.23.3 target must have a persistent device UUID"
                );
            }

            ids.add(snapshot.deviceId());
        }

        deviceIds = validateIds(ids);
    }

    public WifiMultiEngineeringOpenPacket(FriendlyByteBuf buf) {
        List<UUID> ids = new ArrayList<>(DEVICE_COUNT);

        for (int index = 0; index < DEVICE_COUNT; index++) {
            ids.add(buf.readUUID());
        }

        deviceIds = validateIds(ids);
    }

    public void toBytes(FriendlyByteBuf buf) {
        for (UUID deviceId : deviceIds) {
            buf.writeUUID(deviceId);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(
                () -> DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () ->
                                WifiEngineeringClientPacketHandler
                                        .handleMultiOpen(deviceIds)
                )
        );

        context.setPacketHandled(true);
    }

    private static List<UUID> validateIds(List<UUID> ids) {
        if (ids == null
                || ids.size() != DEVICE_COUNT
                || ids.stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException(
                    "W1.23.3 requires exactly four non-null persistent device UUIDs"
            );
        }

        Set<UUID> unique = new HashSet<>(ids);

        if (unique.size() != DEVICE_COUNT) {
            throw new IllegalArgumentException(
                    "W1.23.3 refuses duplicate device UUID targets"
            );
        }

        return List.copyOf(ids);
    }
}
