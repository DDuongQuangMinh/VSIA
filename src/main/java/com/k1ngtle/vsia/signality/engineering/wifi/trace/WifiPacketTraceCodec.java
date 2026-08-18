package com.k1ngtle.vsia.signality.engineering.wifi.trace;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public final class WifiPacketTraceCodec {
    public static final int MAX_EVENTS_PER_PACKET = 128;

    private WifiPacketTraceCodec() {
    }

    public static void write(
            FriendlyByteBuf buf,
            List<WifiPacketTraceEvent> events
    ) {
        int count =
                Math.min(
                        MAX_EVENTS_PER_PACKET,
                        events == null
                                ? 0
                                : events.size()
                );

        buf.writeVarInt(count);

        int start =
                events == null
                        ? 0
                        : Math.max(
                                0,
                                events.size() - count
                        );

        for (int i = start;
             events != null && i < events.size();
             i++) {
            writeEvent(
                    buf,
                    events.get(i)
            );
        }
    }

    public static List<WifiPacketTraceEvent> read(
            FriendlyByteBuf buf
    ) {
        int count =
                Math.min(
                        MAX_EVENTS_PER_PACKET,
                        Math.max(
                                0,
                                buf.readVarInt()
                        )
                );

        List<WifiPacketTraceEvent> out =
                new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            out.add(
                    readEvent(buf)
            );
        }

        return List.copyOf(out);
    }

    private static void writeEvent(
            FriendlyByteBuf buf,
            WifiPacketTraceEvent event
    ) {
        buf.writeVarLong(event.sequence());
        buf.writeVarLong(event.timestampMicros());
        buf.writeEnum(event.direction());
        buf.writeUtf(event.frameType(), 32);
        buf.writeVarInt(event.subtype());
        buf.writeUtf(event.sourceMac(), 32);
        buf.writeUtf(event.destinationMac(), 32);
        buf.writeVarInt(event.mcsIndex());
        buf.writeUtf(event.phyGeneration(), 32);
        buf.writeVarInt(event.frameBytes());
        buf.writeVarLong(Math.max(0L, event.airtimeMicros()));
        buf.writeDouble(event.rssiDbm());
        buf.writeDouble(event.snrDb());
        buf.writeDouble(event.sinrDb());
        buf.writeBoolean(event.retry());
        buf.writeUtf(event.detailedPhyPath(), 64);
        buf.writeEnum(event.outcome());
        buf.writeUtf(event.detail(), 512);
    }

    private static WifiPacketTraceEvent readEvent(
            FriendlyByteBuf buf
    ) {
        return new WifiPacketTraceEvent(
                buf.readVarLong(),
                buf.readVarLong(),
                buf.readEnum(WifiPacketDirection.class),
                buf.readUtf(32),
                buf.readVarInt(),
                buf.readUtf(32),
                buf.readUtf(32),
                buf.readVarInt(),
                buf.readUtf(32),
                buf.readVarInt(),
                buf.readVarLong(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readBoolean(),
                buf.readUtf(64),
                buf.readEnum(WifiPacketOutcome.class),
                buf.readUtf(512)
        );
    }
}
