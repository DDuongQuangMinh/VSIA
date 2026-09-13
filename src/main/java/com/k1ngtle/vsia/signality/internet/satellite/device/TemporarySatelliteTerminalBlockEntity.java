package com.k1ngtle.vsia.signality.internet.satellite.device;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.internet.satellite.SatelliteBandPreset;
import com.k1ngtle.vsia.signality.internet.satellite.SatelliteLinkAssessment;
import com.k1ngtle.vsia.signality.internet.satellite.SatelliteNetworkManager;
import com.k1ngtle.vsia.signality.internet.satellite.SatellitePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public final class TemporarySatelliteTerminalBlockEntity
        extends BlockEntity {

    private UUID terminalId =
            UUID.randomUUID();

    private long transmitSequence;

    private long lastReceivedSequence =
            -1L;

    private UUID lastReceivedSource;

    private SatelliteBandPreset band =
            SatelliteBandPreset.KU;

    private double minimumElevationDeg =
            5.0;

    private SatelliteLinkAssessment lastAssessment =
            SatelliteLinkAssessment
                    .unavailable();

    private String lastPacket =
            "";

    private String status =
            "Satellite terminal ready";

    public TemporarySatelliteTerminalBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                SignalityBlocks
                        .TEMPORARY_SATELLITE_TERMINAL_BE
                        .get(),
                pos,
                state
        );
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (level != null
                && !level.isClientSide()) {
            SatelliteNetworkManager.register(
                    this
            );
        }
    }

    @Override
    public void setRemoved() {
        SatelliteNetworkManager.unregister(
                this
        );

        super.setRemoved();
    }

    public UUID terminalId() {
        return terminalId;
    }

    public SatellitePacket createPacket(
            UUID destination,
            String payload
    ) {
        return new SatellitePacket(
                terminalId,
                destination,
                transmitSequence++,
                8,
                System.nanoTime(),
                payload
        );
    }

    public SatelliteBandPreset band() {
        return band;
    }

    public double minimumElevationDeg() {
        return minimumElevationDeg;
    }

    public SatelliteLinkAssessment lastAssessment() {
        return lastAssessment;
    }

    public String lastPacket() {
        return lastPacket;
    }

    public String status() {
        return status;
    }

    public void cycleBand() {
        band =
                band.next();

        lastAssessment =
                SatelliteLinkAssessment
                        .unavailable();

        status =
                "Band changed to "
                        + band.name();

        setChanged();
    }

    public void adjustMinimumElevation(
            double deltaDeg
    ) {
        minimumElevationDeg =
                Math.max(
                        0.0,
                        Math.min(
                                45.0,
                                minimumElevationDeg
                                        + deltaDeg
                        )
                );

        status =
                String.format(
                        java.util.Locale.ROOT,
                        "Elevation mask %.1f deg",
                        minimumElevationDeg
                );

        setChanged();
    }

    public void setLastAssessment(
            SatelliteLinkAssessment assessment
    ) {
        lastAssessment =
                assessment == null
                        ? SatelliteLinkAssessment
                        .unavailable()
                        : assessment;

        setChanged();
    }

    public void setStatus(
            String status
    ) {
        this.status =
                status == null
                        ? ""
                        : status;

        setChanged();
    }

    public void receivePacket(
            SatellitePacket packet,
            SatelliteLinkAssessment assessment
    ) {
        if (packet == null
                || !terminalId.equals(
                packet.destinationTerminalId()
        )
                || packet.ttl() <= 0
                || (
                packet.sourceTerminalId()
                        .equals(
                                lastReceivedSource
                        )
                        && packet.sequenceNumber()
                        <= lastReceivedSequence
        )) {
            return;
        }

        lastReceivedSource =
                packet.sourceTerminalId();

        lastReceivedSequence =
                packet.sequenceNumber();

        lastPacket =
                packet.payload();

        lastAssessment =
                assessment == null
                        ? SatelliteLinkAssessment
                        .unavailable()
                        : assessment;

        status =
                "SATCOM datagram received seq="
                        + packet.sequenceNumber();

        setChanged();
    }

    public SatelliteLinkAssessment testNearestLink() {
        TemporarySatelliteTerminalBlockEntity target =
                SatelliteNetworkManager
                        .nearestOther(
                                this
                        );

        if (target == null) {
            lastAssessment =
                    SatelliteNetworkManager
                            .assessSelf(
                                    this
                            );

            status =
                    lastAssessment.visible()
                            ? "Satellite visible: "
                            + lastAssessment
                            .satelliteName()
                            : "No satellite visible";

            setChanged();
            return lastAssessment;
        }

        lastAssessment =
                SatelliteNetworkManager
                        .assess(
                                this,
                                target
                        );

        status =
                lastAssessment.visible()
                        ? "Common satellite "
                        + lastAssessment
                        .satelliteName()
                        : "No common satellite with nearest terminal";

        setChanged();
        return lastAssessment;
    }

    public boolean packetTest() {
        TemporarySatelliteTerminalBlockEntity target =
                SatelliteNetworkManager
                        .nearestOther(
                                this
                        );

        if (target == null) {
            status =
                    "Place a second satellite terminal for packet test";

            setChanged();
            return false;
        }

        return SatelliteNetworkManager
                .sendPacket(
                        this,
                        target,
                        "Satellite terminal packet test"
                );
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag
    ) {
        super.saveAdditional(
                tag
        );

        tag.putUUID(
                "SatelliteTerminalId",
                terminalId
        );

        tag.putLong(
                "SatelliteTransmitSequence",
                transmitSequence
        );

        tag.putLong(
                "SatelliteLastReceivedSequence",
                lastReceivedSequence
        );

        if (lastReceivedSource != null) {
            tag.putUUID(
                    "SatelliteLastReceivedSource",
                    lastReceivedSource
            );
        }

        tag.putString(
                "SatelliteBand",
                band.name()
        );

        tag.putDouble(
                "MinimumElevationDeg",
                minimumElevationDeg
        );

        tag.putString(
                "LastSatellitePacket",
                lastPacket
        );

        tag.putString(
                "SatelliteStatus",
                status
        );
    }

    @Override
    public void load(
            CompoundTag tag
    ) {
        super.load(
                tag
        );

        if (tag.hasUUID(
                "SatelliteTerminalId"
        )) {
            terminalId =
                    tag.getUUID(
                            "SatelliteTerminalId"
                    );
        }

        transmitSequence =
                tag.getLong(
                        "SatelliteTransmitSequence"
                );

        lastReceivedSequence =
                tag.contains(
                        "SatelliteLastReceivedSequence"
                )
                        ? tag.getLong(
                        "SatelliteLastReceivedSequence"
                )
                        : -1L;

        lastReceivedSource =
                tag.hasUUID(
                        "SatelliteLastReceivedSource"
                )
                        ? tag.getUUID(
                        "SatelliteLastReceivedSource"
                )
                        : null;

        try {
            band =
                    SatelliteBandPreset
                            .valueOf(
                                    tag.getString(
                                            "SatelliteBand"
                                    )
                            );
        } catch (Exception ignored) {
            band =
                    SatelliteBandPreset.KU;
        }

        minimumElevationDeg =
                tag.contains(
                        "MinimumElevationDeg"
                )
                        ? tag.getDouble(
                        "MinimumElevationDeg"
                )
                        : 5.0;

        lastPacket =
                tag.getString(
                        "LastSatellitePacket"
                );

        status =
                tag.contains(
                        "SatelliteStatus"
                )
                        ? tag.getString(
                        "SatelliteStatus"
                )
                        : "Satellite terminal ready";

        lastAssessment =
                SatelliteLinkAssessment
                        .unavailable();
    }
}
