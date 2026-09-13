package com.k1ngtle.vsia.phone.network.realism.packet;

import com.k1ngtle.vsia.phone.network.PhoneNetworkState;
import com.k1ngtle.vsia.phone.network.realism.PhoneWirelessSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class S2CPhoneWirelessSnapshotPacket {
    private static final int MAX_STRING =
            256;

    private static final int MAX_NETWORKS =
            32;

    private final PhoneWirelessSnapshot snapshot;

    public S2CPhoneWirelessSnapshotPacket(
            PhoneWirelessSnapshot snapshot
    ) {
        this.snapshot =
                snapshot;
    }

    public S2CPhoneWirelessSnapshotPacket(
            FriendlyByteBuf buffer
    ) {
        int count =
                Math.min(
                        MAX_NETWORKS,
                        Math.max(
                                0,
                                buffer.readVarInt()
                        )
                );

        List<PhoneWirelessSnapshot.WifiNetwork> networks =
                new ArrayList<>();

        for (int i = 0;
             i < count;
             i++) {
            networks.add(
                    readWifiNetwork(
                            buffer
                    )
            );
        }

        PhoneWirelessSnapshot.WifiStatus wifi =
                readWifiStatus(
                        buffer
                );

        PhoneWirelessSnapshot.CellularStatus cellular =
                readCellularStatus(
                        buffer
                );

        snapshot =
                new PhoneWirelessSnapshot(
                        networks,
                        wifi,
                        cellular
                );
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        List<PhoneWirelessSnapshot.WifiNetwork> networks =
                snapshot == null
                        ? List.of()
                        : snapshot.wifiNetworks();

        int count =
                Math.min(
                        MAX_NETWORKS,
                        networks.size()
                );

        buffer.writeVarInt(
                count
        );

        for (int i = 0;
             i < count;
             i++) {
            writeWifiNetwork(
                    buffer,
                    networks.get(
                            i
                    )
            );
        }

        writeWifiStatus(
                buffer,
                snapshot == null
                        ? PhoneWirelessSnapshot
                        .WifiStatus
                        .off()
                        : snapshot.wifi()
        );

        writeCellularStatus(
                buffer,
                snapshot == null
                        ? PhoneWirelessSnapshot
                        .CellularStatus
                        .off()
                        : snapshot.cellular()
        );
    }

    public void handle(
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context =
                supplier.get();

        context.enqueueWork(
                () ->
                        DistExecutor
                                .unsafeRunWhenOn(
                                        Dist.CLIENT,
                                        () ->
                                                () ->
                                                        PhoneNetworkState
                                                                .get()
                                                                .applyWirelessSnapshot(
                                                                        snapshot
                                                                )
                                )
        );

        context.setPacketHandled(
                true
        );
    }

    private static void writeWifiNetwork(
            FriendlyByteBuf buffer,
            PhoneWirelessSnapshot.WifiNetwork value
    ) {
        buffer.writeUtf(
                value.ssid(),
                MAX_STRING
        );

        buffer.writeUtf(
                value.bssid(),
                MAX_STRING
        );

        buffer.writeUtf(
                value.security(),
                MAX_STRING
        );

        buffer.writeBoolean(
                value.locked()
        );

        buffer.writeInt(
                value.rssiDbm()
        );

        buffer.writeDouble(
                value.sinrDb()
        );

        buffer.writeInt(
                value.channel()
        );

        buffer.writeDouble(
                value.frequencyHz()
        );

        buffer.writeUtf(
                value.phy(),
                MAX_STRING
        );

        buffer.writeDouble(
                value.distanceBlocks()
        );

        buffer.writeUtf(
                value.quality(),
                MAX_STRING
        );
    }

    private static PhoneWirelessSnapshot.WifiNetwork readWifiNetwork(
            FriendlyByteBuf buffer
    ) {
        return new PhoneWirelessSnapshot.WifiNetwork(
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readBoolean(),
                buffer.readInt(),
                buffer.readDouble(),
                buffer.readInt(),
                buffer.readDouble(),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readDouble(),
                buffer.readUtf(
                        MAX_STRING
                )
        );
    }

    private static void writeWifiStatus(
            FriendlyByteBuf buffer,
            PhoneWirelessSnapshot.WifiStatus value
    ) {
        buffer.writeBoolean(
                value.enabled()
        );

        buffer.writeBoolean(
                value.connected()
        );

        buffer.writeUtf(
                value.stage(),
                MAX_STRING
        );

        buffer.writeUtf(
                value.ssid(),
                MAX_STRING
        );

        buffer.writeUtf(
                value.bssid(),
                MAX_STRING
        );

        buffer.writeUtf(
                value.security(),
                MAX_STRING
        );

        buffer.writeInt(
                value.rssiDbm()
        );

        buffer.writeDouble(
                value.sinrDb()
        );

        buffer.writeInt(
                value.channel()
        );

        buffer.writeDouble(
                value.frequencyHz()
        );

        buffer.writeUtf(
                value.phy(),
                MAX_STRING
        );

        buffer.writeDouble(
                value.distanceBlocks()
        );

        buffer.writeUtf(
                value.quality(),
                MAX_STRING
        );

        buffer.writeUtf(
                value.ipAddress(),
                MAX_STRING
        );

        buffer.writeUtf(
                value.subnetMask(),
                MAX_STRING
        );

        buffer.writeUtf(
                value.gateway(),
                MAX_STRING
        );

        buffer.writeUtf(
                value.dns(),
                MAX_STRING
        );

        buffer.writeUtf(
                value.status(),
                MAX_STRING
        );
    }

    private static PhoneWirelessSnapshot.WifiStatus readWifiStatus(
            FriendlyByteBuf buffer
    ) {
        return new PhoneWirelessSnapshot.WifiStatus(
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readInt(),
                buffer.readDouble(),
                buffer.readInt(),
                buffer.readDouble(),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readDouble(),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readUtf(
                        MAX_STRING
                )
        );
    }

    private static void writeCellularStatus(
            FriendlyByteBuf buffer,
            PhoneWirelessSnapshot.CellularStatus value
    ) {
        buffer.writeBoolean(
                value.enabled()
        );

        buffer.writeBoolean(
                value.registered()
        );

        buffer.writeUtf(
                value.carrier(),
                MAX_STRING
        );

        buffer.writeUtf(
                value.radioLabel(),
                MAX_STRING
        );

        buffer.writeUtf(
                value.architecture(),
                MAX_STRING
        );

        buffer.writeInt(
                value.gnbId()
        );

        buffer.writeInt(
                value.cellId()
        );

        buffer.writeInt(
                value.tac()
        );

        buffer.writeUtf(
                value.plmn(),
                MAX_STRING
        );

        buffer.writeUtf(
                value.band(),
                MAX_STRING
        );

        buffer.writeUtf(
                value.ipAddress(),
                MAX_STRING
        );

        buffer.writeUtf(
                value.dnn(),
                MAX_STRING
        );

        buffer.writeInt(
                value.fiveQi()
        );

        buffer.writeUtf(
                value.rrcState(),
                MAX_STRING
        );

        buffer.writeUtf(
                value.nasState(),
                MAX_STRING
        );

        buffer.writeUtf(
                value.pduState(),
                MAX_STRING
        );

        buffer.writeInt(
                value.rsrpDbm()
        );

        buffer.writeDouble(
                value.rsrqDb()
        );

        buffer.writeDouble(
                value.sinrDb()
        );

        buffer.writeDouble(
                value.distanceBlocks()
        );

        buffer.writeUtf(
                value.quality(),
                MAX_STRING
        );

        buffer.writeDouble(
                value.estimatedDownlinkMbps()
        );

        buffer.writeUtf(
                value.satelliteNtnStatus(),
                MAX_STRING
        );

        buffer.writeUtf(
                value.status(),
                MAX_STRING
        );
    }

    private static PhoneWirelessSnapshot.CellularStatus readCellularStatus(
            FriendlyByteBuf buffer
    ) {
        return new PhoneWirelessSnapshot.CellularStatus(
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readInt(),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readInt(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readDouble(),
                buffer.readUtf(
                        MAX_STRING
                ),
                buffer.readUtf(
                        MAX_STRING
                )
        );
    }
}
