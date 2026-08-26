package com.k1ngtle.vsia.network.wifi;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiMode;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiSecurityState;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiStationState;
import com.k1ngtle.vsia.signality.engineering.wifi.workflow.WifiEngineeringWorkflowSnapshot;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public final class WifiEngineeringWorkflowSnapshotCodec {
    private static final int MAX_LIST_ENTRIES = 64;

    private WifiEngineeringWorkflowSnapshotCodec() {
    }

    public static void write(
            FriendlyByteBuf buf,
            WifiEngineeringWorkflowSnapshot snapshot
    ) {
        buf.writeUtf(snapshot.macAddress(), 64);
        buf.writeEnum(snapshot.mode());
        buf.writeEnum(snapshot.stationState());
        buf.writeEnum(snapshot.securityState());

        writeStrings(buf, snapshot.discoveredSsids());
        writeStrings(buf, snapshot.associatedStations());

        buf.writeVarInt(snapshot.pendingDataTransmissions());
        buf.writeUtf(snapshot.securityDiagnostic(), 1024);
        buf.writeUtf(snapshot.status(), 1024);
    }

    public static WifiEngineeringWorkflowSnapshot read(
            FriendlyByteBuf buf
    ) {
        String macAddress = buf.readUtf(64);
        WifiMode mode = buf.readEnum(WifiMode.class);
        WifiStationState stationState =
                buf.readEnum(WifiStationState.class);
        WifiSecurityState securityState =
                buf.readEnum(WifiSecurityState.class);

        List<String> discovered = readStrings(buf);
        List<String> associated = readStrings(buf);

        int pending = buf.readVarInt();
        String securityDiagnostic = buf.readUtf(1024);
        String status = buf.readUtf(1024);

        return new WifiEngineeringWorkflowSnapshot(
                macAddress,
                mode,
                stationState,
                securityState,
                discovered,
                associated,
                pending,
                securityDiagnostic,
                status
        );
    }

    private static void writeStrings(
            FriendlyByteBuf buf,
            List<String> values
    ) {
        int count = Math.min(
                MAX_LIST_ENTRIES,
                values == null ? 0 : values.size()
        );

        buf.writeVarInt(count);

        for (int index = 0; index < count; index++) {
            buf.writeUtf(values.get(index), 256);
        }
    }

    private static List<String> readStrings(FriendlyByteBuf buf) {
        int count = Math.min(
                MAX_LIST_ENTRIES,
                Math.max(0, buf.readVarInt())
        );

        List<String> values = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            values.add(buf.readUtf(256));
        }

        return List.copyOf(values);
    }
}
