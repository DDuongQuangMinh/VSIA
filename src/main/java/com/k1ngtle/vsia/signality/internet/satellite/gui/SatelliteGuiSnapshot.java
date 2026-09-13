package com.k1ngtle.vsia.signality.internet.satellite.gui;

import com.k1ngtle.vsia.signality.internet.satellite.SatelliteLinkAssessment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record SatelliteGuiSnapshot(
        BlockPos pos,
        boolean valid,
        String band,
        double uplinkHz,
        double downlinkHz,
        double bandwidthHz,
        double minimumElevationDeg,
        boolean internetGatewayEnabled,
        SatelliteLinkAssessment assessment,
        String lastPacket,
        String status
) {
    public SatelliteGuiSnapshot {
        if (pos == null) {
            pos = BlockPos.ZERO;
        }

        band = safe(band);
        lastPacket = safe(lastPacket);
        status = safe(status);

        if (assessment == null) {
            assessment =
                    SatelliteLinkAssessment
                            .unavailable();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(valid);
        buffer.writeUtf(band, 32);
        buffer.writeDouble(uplinkHz);
        buffer.writeDouble(downlinkHz);
        buffer.writeDouble(bandwidthHz);
        buffer.writeDouble(minimumElevationDeg);
        buffer.writeBoolean(internetGatewayEnabled);

        buffer.writeBoolean(
                assessment.visible()
        );

        buffer.writeUtf(
                assessment.satelliteName(),
                64
        );

        buffer.writeDouble(
                assessment.sourceElevationDeg()
        );

        buffer.writeDouble(
                assessment.targetElevationDeg()
        );

        buffer.writeDouble(
                assessment.sourceSlantRangeMeters()
        );

        buffer.writeDouble(
                assessment.targetSlantRangeMeters()
        );

        buffer.writeDouble(
                assessment.uplinkReceivedPowerDbm()
        );

        buffer.writeDouble(
                assessment.downlinkReceivedPowerDbm()
        );

        buffer.writeDouble(
                assessment.uplinkSnrDb()
        );

        buffer.writeDouble(
                assessment.downlinkSnrDb()
        );

        buffer.writeDouble(
                assessment.propagationDelayMs()
        );

        buffer.writeDouble(
                assessment.uplinkDopplerHz()
        );

        buffer.writeDouble(
                assessment.downlinkDopplerHz()
        );

        buffer.writeDouble(
                assessment.packetSuccessProbability()
        );

        buffer.writeUtf(
                lastPacket,
                1024
        );

        buffer.writeUtf(
                status,
                512
        );
    }

    public static SatelliteGuiSnapshot decode(
            FriendlyByteBuf buffer
    ) {
        BlockPos pos =
                buffer.readBlockPos();

        boolean valid =
                buffer.readBoolean();

        String band =
                buffer.readUtf(32);

        double uplink =
                buffer.readDouble();

        double downlink =
                buffer.readDouble();

        double bandwidth =
                buffer.readDouble();

        double mask =
                buffer.readDouble();

        boolean gateway =
                buffer.readBoolean();

        SatelliteLinkAssessment assessment =
                new SatelliteLinkAssessment(
                        buffer.readBoolean(),
                        buffer.readUtf(64),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble()
                );

        return new SatelliteGuiSnapshot(
                pos,
                valid,
                band,
                uplink,
                downlink,
                bandwidth,
                mask,
                gateway,
                assessment,
                buffer.readUtf(1024),
                buffer.readUtf(512)
        );
    }

    public static SatelliteGuiSnapshot invalid(
            BlockPos pos,
            String status
    ) {
        return new SatelliteGuiSnapshot(
                pos,
                false,
                "",
                0.0,
                0.0,
                0.0,
                0.0,
                false,
                SatelliteLinkAssessment
                        .unavailable(),
                "",
                status
        );
    }
}
