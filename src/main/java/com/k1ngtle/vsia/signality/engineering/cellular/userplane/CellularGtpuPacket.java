package com.k1ngtle.vsia.signality.engineering.cellular.userplane;

import com.k1ngtle.vsia.signality.engineering.cellular.core.PduSession;

// CELLULAR_USER_PLANE_V1
public record CellularGtpuPacket(
        long teid,
        int sequence,
        int innerPacketBytes
) {
    public CellularGtpuPacket {
        if (teid <= 0L || teid > 0xffffffffL) {
            throw new IllegalArgumentException("TEID must be an unsigned 32-bit value");
        }
        if (innerPacketBytes <= 0) {
            throw new IllegalArgumentException("innerPacketBytes must be positive");
        }
    }

    public static CellularGtpuPacket forSession(
            PduSession session,
            int sequence,
            int innerPacketBytes
    ) {
        if (session == null) {
            throw new IllegalArgumentException("session cannot be null");
        }

        int mixed =
                session.ueId().hashCode()
                        ^ Integer.rotateLeft(session.sessionId(), 13)
                        ^ 0x47545055;

        long teid = Integer.toUnsignedLong(mixed);
        if (teid == 0L) {
            teid = 1L;
        }

        return new CellularGtpuPacket(
                teid,
                sequence & 0xffff,
                innerPacketBytes
        );
    }

    public int gtpuBytes() {
        return 8 + innerPacketBytes;
    }

    public int n3WireBytes() {
        return 20 + 8 + gtpuBytes();
    }
}
