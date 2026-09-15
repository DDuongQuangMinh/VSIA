package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.api.radar.IRadarTarget;
import com.k1ngtle.vsia.signality.radar.iff.IIffTransponder;
import com.k1ngtle.vsia.signality.radar.iff.IffCrypto;
import com.k1ngtle.vsia.signality.radar.iff.IffNetworkKeyRegistry;
import com.k1ngtle.vsia.signality.radar.iff.IffRegistry;
import com.k1ngtle.vsia.signality.radar.iff.IffTransponderState;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class RadarBeaconBlockEntity
        extends BlockEntity
        implements IRadarTarget, IIffTransponder {
    public static final double[] RCS_PRESETS =
            new double[]{0.1, 1.0, 10.0, 100.0, 1000.0};

    private static final Set<RadarBeaconBlockEntity> ACTIVE =
            ConcurrentHashMap.newKeySet();

    private UUID id = UUID.randomUUID();
    private int rcsIndex = 1;
    private int iffPresetIndex = 0;

    public RadarBeaconBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType<?>) SignalityBlocks.RADAR_BEACON_BE.get(), pos, state);
    }

    public static Stream<IRadarTarget> beaconsIn(ServerLevel level) {
        return ACTIVE.stream()
                .filter(beacon -> beacon.level == level && !beacon.isRemoved())
                .map(beacon -> (IRadarTarget) beacon);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            ACTIVE.add(this);
            IffRegistry.register(this);
        }
    }

    @Override
    public void setRemoved() {
        ACTIVE.remove(this);
        IffRegistry.unregister(id);
        super.setRemoved();
    }

    public double rcs() {
        return RCS_PRESETS[rcsIndex];
    }

    public double cycleRcs() {
        rcsIndex = (rcsIndex + 1) % RCS_PRESETS.length;
        setChanged();
        return rcs();
    }

    public String cycleIffTestPreset() {
        iffPresetIndex = (iffPresetIndex + 1) % 4;
        setChanged();
        return iffPresetName();
    }

    public String iffPresetName() {
        return switch (iffPresetIndex) {
            case 0 -> "FRIENDLY_AUTHENTICATED";
            case 1 -> "FRIENDLY_EMERGENCY_7700";
            case 2 -> "WRONG_CRYPTO_KEY";
            default -> "TRANSPONDER_OFF";
        };
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public ServerLevel level() {
        return (ServerLevel) level;
    }

    @Override
    public Vec3 positionWorld() {
        return Vec3.atCenterOf(worldPosition);
    }

    @Override
    public Vec3 velocityWorld() {
        return Vec3.ZERO;
    }

    @Override
    public double boundingRadius() {
        return 0.75;
    }

    @Override
    public double radarCrossSection(double aspectAngleRad, double wavelengthMeters) {
        return rcs();
    }

    @Override
    public UUID iffTargetId() {
        return id;
    }

    @Override
    public ServerLevel iffLevel() {
        return (ServerLevel) level;
    }

    @Override
    public Vec3 iffPositionWorld() {
        return positionWorld();
    }

    @Override
    public IffTransponderState iffState() {
        return switch (iffPresetIndex) {
            case 1 -> IffTransponderState.EMERGENCY;
            case 3 -> IffTransponderState.OFF;
            default -> IffTransponderState.NORMAL;
        };
    }

    @Override
    public String iffCallsign() {
        String shortId = id.toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase(Locale.ROOT);
        return "TEST-" + shortId;
    }

    @Override
    public int iffSquawkCode() {
        return iffPresetIndex == 1 ? 7700 : 1200;
    }

    @Override
    public int iffModeSAddress() {
        int address = (int) (id.getLeastSignificantBits() & 0x00FFFFFFL);
        return address == 0 ? 1 : address;
    }

    @Override
    public byte[] respondToChallenge(long challengeNonce) {
        if (!iffState().repliesToInterrogation()) {
            return null;
        }

        String key = iffPresetIndex == 2
                ? "VSIA-IFF-FOREIGN-TEST-KEY"
                : IffNetworkKeyRegistry.DEFAULT_TEST_KEY;

        return IffCrypto.response(
                key,
                challengeNonce,
                iffModeSAddress(),
                iffSquawkCode()
        );
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putUUID("Id", id);
        tag.putInt("RcsIndex", rcsIndex);
        tag.putInt("IffPresetIndex", iffPresetIndex);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("Id")) {
            id = tag.getUUID("Id");
        }
        if (tag.contains("RcsIndex")) {
            int value = tag.getInt("RcsIndex");
            if (value >= 0 && value < RCS_PRESETS.length) {
                rcsIndex = value;
            }
        }
        if (tag.contains("IffPresetIndex")) {
            int value = tag.getInt("IffPresetIndex");
            if (value >= 0 && value < 4) {
                iffPresetIndex = value;
            }
        }
    }
}
