package com.k1ngtle.vsia.network.wifi;

import com.k1ngtle.vsia.client.wifi.WifiEngineeringClientPacketHandler;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringProbe;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSnapshotCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.workflow.WifiEngineeringWorkflowService;
import com.k1ngtle.vsia.signality.engineering.wifi.workflow.WifiEngineeringWorkflowSnapshot;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class WifiMultiEngineeringDeviceSnapshotPacket {
    private final UUID deviceId;
    private final boolean resolved;
    private final BlockPos storagePos;
    private final double worldX;
    private final double worldY;
    private final double worldZ;
    private final WifiEngineeringSnapshot snapshot;
    private final WifiEngineeringWorkflowSnapshot workflow;
    private final String status;

    public static WifiMultiEngineeringDeviceSnapshotPacket unresolved(
            UUID deviceId,
            String status
    ) {
        return new WifiMultiEngineeringDeviceSnapshotPacket(
                deviceId,
                false,
                BlockPos.ZERO,
                0.0D,
                0.0D,
                0.0D,
                null,
                null,
                status
        );
    }

    public static WifiMultiEngineeringDeviceSnapshotPacket capture(
            NetworkDeviceBlockEntity device,
            String status
    ) {
        Vec3 world = device.positionWorld();

        return new WifiMultiEngineeringDeviceSnapshotPacket(
                device.id(),
                true,
                device.getBlockPos().immutable(),
                world.x,
                world.y,
                world.z,
                WifiEngineeringProbe.capture(device),
                WifiEngineeringWorkflowService.snapshot(
                        device,
                        status == null ? "" : status
                ),
                status
        );
    }

    private WifiMultiEngineeringDeviceSnapshotPacket(
            UUID deviceId,
            boolean resolved,
            BlockPos storagePos,
            double worldX,
            double worldY,
            double worldZ,
            WifiEngineeringSnapshot snapshot,
            WifiEngineeringWorkflowSnapshot workflow,
            String status
    ) {
        this.deviceId = deviceId;
        this.resolved = resolved;
        this.storagePos = storagePos;
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldZ = worldZ;
        this.snapshot = snapshot;
        this.workflow = workflow;
        this.status = status == null ? "" : status;
    }

    public WifiMultiEngineeringDeviceSnapshotPacket(FriendlyByteBuf buf) {
        deviceId = buf.readUUID();
        resolved = buf.readBoolean();

        if (resolved) {
            storagePos = buf.readBlockPos();
            worldX = buf.readDouble();
            worldY = buf.readDouble();
            worldZ = buf.readDouble();
            snapshot = WifiEngineeringSnapshotCodec.read(buf);
            workflow = WifiEngineeringWorkflowSnapshotCodec.read(buf);
        } else {
            storagePos = BlockPos.ZERO;
            worldX = 0.0D;
            worldY = 0.0D;
            worldZ = 0.0D;
            snapshot = null;
            workflow = null;
        }

        status = buf.readUtf(1024);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(deviceId);
        buf.writeBoolean(resolved);

        if (resolved) {
            buf.writeBlockPos(storagePos);
            buf.writeDouble(worldX);
            buf.writeDouble(worldY);
            buf.writeDouble(worldZ);
            WifiEngineeringSnapshotCodec.write(buf, snapshot);
            WifiEngineeringWorkflowSnapshotCodec.write(buf, workflow);
        }

        buf.writeUtf(status, 1024);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(
                () -> DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () ->
                                WifiEngineeringClientPacketHandler
                                        .handleMultiDeviceSnapshot(this)
                )
        );

        context.setPacketHandled(true);
    }

    public UUID deviceId() {
        return deviceId;
    }

    public boolean resolved() {
        return resolved;
    }

    public BlockPos storagePos() {
        return storagePos;
    }

    public double worldX() {
        return worldX;
    }

    public double worldY() {
        return worldY;
    }

    public double worldZ() {
        return worldZ;
    }

    public WifiEngineeringSnapshot snapshot() {
        return snapshot;
    }

    public WifiEngineeringWorkflowSnapshot workflow() {
        return workflow;
    }

    public String status() {
        return status;
    }
}
