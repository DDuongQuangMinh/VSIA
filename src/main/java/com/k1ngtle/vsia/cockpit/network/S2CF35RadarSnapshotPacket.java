package com.k1ngtle.vsia.cockpit.network;

import com.k1ngtle.vsia.cockpit.display.F35ClientRadarCache;
import com.k1ngtle.vsia.cockpit.display.F35RadarTrackView;
import com.k1ngtle.vsia.signality.radar.network.RadarNetworkTrack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class S2CF35RadarSnapshotPacket {
    private static final int MAX_TRACKS =
            128;

    private final String networkId;
    private final long serverTick;
    private final List<F35RadarTrackView> tracks;

    public S2CF35RadarSnapshotPacket(
            String networkId,
            long serverTick,
            List<RadarNetworkTrack> source
    ) {
        this.networkId =
                networkId;

        this.serverTick =
                serverTick;

        int count =
                Math.min(
                        MAX_TRACKS,
                        source.size()
                );

        List<F35RadarTrackView> converted =
                new ArrayList<>(
                        count
                );

        for (int i = 0;
             i < count;
             i++) {
            RadarNetworkTrack track =
                    source.get(
                            i
                    );

            converted.add(
                    fromTrack(
                            track
                    )
            );
        }

        tracks =
                List.copyOf(
                        converted
                );
    }

    public S2CF35RadarSnapshotPacket(
            FriendlyByteBuf buffer
    ) {
        networkId =
                buffer.readUtf(
                        64
                );

        serverTick =
                buffer.readLong();

        int count =
                Math.min(
                        MAX_TRACKS,
                        buffer.readVarInt()
                );

        List<F35RadarTrackView> decoded =
                new ArrayList<>(
                        count
                );

        for (int i = 0;
             i < count;
             i++) {
            decoded.add(
                    decodeTrack(
                            buffer
                    )
            );
        }

        tracks =
                List.copyOf(
                        decoded
                );
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        buffer.writeUtf(
                networkId,
                64
        );

        buffer.writeLong(
                serverTick
        );

        buffer.writeVarInt(
                tracks.size()
        );

        for (F35RadarTrackView track :
                tracks) {
            encodeTrack(
                    buffer,
                    track
            );
        }
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
                                                F35ClientRadarCache.accept(
                                                        networkId,
                                                        serverTick,
                                                        tracks
                                                )
                        )
        );

        context.setPacketHandled(
                true
        );
    }

    private static F35RadarTrackView fromTrack(
            RadarNetworkTrack track
    ) {
        return new F35RadarTrackView(
                track.trackId(),
                track.state().name(),
                track.position(),
                track.velocity(),
                track.quality(),
                track.positionUncertaintyMeters(),
                track.bestSnrLinear(),
                track.sensorCount(),
                track.hits(),
                track.iff()
                        .affiliation()
                        .name(),
                track.iff()
                        .replyStatus()
                        .name(),
                track.iff()
                        .callsign(),
                track.iff()
                        .squawkCode(),
                track.iff()
                        .authenticated()
        );
    }

    private static void encodeTrack(
            FriendlyByteBuf buffer,
            F35RadarTrackView track
    ) {
        buffer.writeUUID(
                track.trackId()
        );

        buffer.writeUtf(
                track.trackState(),
                32
        );

        writeVec3(
                buffer,
                track.position()
        );

        writeVec3(
                buffer,
                track.velocity()
        );

        buffer.writeDouble(
                track.quality()
        );

        buffer.writeDouble(
                track.uncertaintyMeters()
        );

        buffer.writeDouble(
                track.bestSnrLinear()
        );

        buffer.writeVarInt(
                track.sensorCount()
        );

        buffer.writeVarInt(
                track.hits()
        );

        buffer.writeUtf(
                track.iffAffiliation(),
                32
        );

        buffer.writeUtf(
                track.iffReplyStatus(),
                32
        );

        buffer.writeUtf(
                track.iffCallsign(),
                64
        );

        buffer.writeInt(
                track.iffSquawk()
        );

        buffer.writeBoolean(
                track.iffAuthenticated()
        );
    }

    private static F35RadarTrackView decodeTrack(
            FriendlyByteBuf buffer
    ) {
        UUID trackId =
                buffer.readUUID();

        String state =
                buffer.readUtf(
                        32
                );

        Vec3 position =
                readVec3(
                        buffer
                );

        Vec3 velocity =
                readVec3(
                        buffer
                );

        double quality =
                buffer.readDouble();

        double uncertainty =
                buffer.readDouble();

        double snr =
                buffer.readDouble();

        int sensorCount =
                buffer.readVarInt();

        int hits =
                buffer.readVarInt();

        String affiliation =
                buffer.readUtf(
                        32
                );

        String reply =
                buffer.readUtf(
                        32
                );

        String callsign =
                buffer.readUtf(
                        64
                );

        int squawk =
                buffer.readInt();

        boolean authenticated =
                buffer.readBoolean();

        return new F35RadarTrackView(
                trackId,
                state,
                position,
                velocity,
                quality,
                uncertainty,
                snr,
                sensorCount,
                hits,
                affiliation,
                reply,
                callsign,
                squawk,
                authenticated
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
