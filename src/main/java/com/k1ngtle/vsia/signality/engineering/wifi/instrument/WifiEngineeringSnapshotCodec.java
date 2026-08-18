package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

import com.k1ngtle.vsia.signality.engineering.wifi.live.WifiLivePhyMode;
import com.k1ngtle.vsia.signality.engineering.wifi.live.WifiLivePhyPath;
import com.k1ngtle.vsia.signality.engineering.wifi.phy.WifiPhyGeneration;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public final class WifiEngineeringSnapshotCodec {
    private WifiEngineeringSnapshotCodec() {
    }

    public static void write(
            FriendlyByteBuf buf,
            WifiEngineeringSnapshot value
    ) {
        buf.writeUUID(
                value.deviceId()
        );
        buf.writeUtf(
                value.networkProfile(),
                256
        );
        buf.writeDouble(
                value.frequencyHz()
        );
        buf.writeUtf(
                value.wifiMode(),
                64
        );
        buf.writeUtf(
                value.stationState(),
                64
        );
        buf.writeUtf(
                value.securityState(),
                64
        );
        buf.writeVarInt(
                value.mcsIndex()
        );
        buf.writeUtf(
                value.generation() == null
                        ? ""
                        : value.generation()
                        .name(),
                32
        );
        buf.writeVarInt(
                value.channelWidthMhz()
        );
        buf.writeDouble(
                value.guardIntervalUs()
        );
        buf.writeVarInt(
                value.spatialStreams()
        );
        buf.writeDouble(
                value.estimatedPhyRateBps()
        );
        buf.writeDouble(
                value.dopplerIciFraction()
        );
        buf.writeDouble(
                value.receivedPowerDbm()
        );
        buf.writeDouble(
                value.snrDb()
        );
        buf.writeDouble(
                value.bitErrorRate()
        );
        buf.writeDouble(
                value.frameErrorRate()
        );
        buf.writeDouble(
                value.correctedSinrDb()
        );
        buf.writeDouble(
                value.propagationDelayMicros()
        );
        buf.writeLong(
                value.airtimeMicros()
        );
        buf.writeDouble(
                value.temporalInterferenceFactor()
        );
        buf.writeDouble(
                value.captureMarginDb()
        );
        buf.writeBoolean(
                value.captured()
        );
        buf.writeBoolean(
                value.mediumBusy()
        );
        buf.writeDouble(
                value.mediumEnergyDbm()
        );
        buf.writeVarInt(
                value.overlappingTransmitters()
        );
        buf.writeEnum(
                value.liveMode()
        );
        buf.writeEnum(
                value.livePath()
        );
        buf.writeBoolean(
                value.liveEvaluated()
        );
        buf.writeBoolean(
                value.liveDelivered()
        );
        buf.writeVarInt(
                value.liveCodewords()
        );
        buf.writeVarInt(
                value.liveDecoderIterations()
        );
        buf.writeUtf(
                value.liveDetail(),
                1024
        );
    }

    public static WifiEngineeringSnapshot read(
            FriendlyByteBuf buf
    ) {
        UUID deviceId =
                buf.readUUID();

        String networkProfile =
                buf.readUtf(
                        256
                );

        double frequencyHz =
                buf.readDouble();

        String wifiMode =
                buf.readUtf(
                        64
                );

        String stationState =
                buf.readUtf(
                        64
                );

        String securityState =
                buf.readUtf(
                        64
                );

        int mcsIndex =
                buf.readVarInt();

        String generationName =
                buf.readUtf(
                        32
                );

        WifiPhyGeneration generation =
                generationName.isBlank()
                        ? null
                        : WifiPhyGeneration.valueOf(
                                generationName
                        );

        return new WifiEngineeringSnapshot(
                deviceId,
                networkProfile,
                frequencyHz,
                wifiMode,
                stationState,
                securityState,
                mcsIndex,
                generation,
                buf.readVarInt(),
                buf.readDouble(),
                buf.readVarInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readLong(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readDouble(),
                buf.readVarInt(),
                buf.readEnum(
                        WifiLivePhyMode.class
                ),
                buf.readEnum(
                        WifiLivePhyPath.class
                ),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readUtf(
                        1024
                )
        );
    }
}
