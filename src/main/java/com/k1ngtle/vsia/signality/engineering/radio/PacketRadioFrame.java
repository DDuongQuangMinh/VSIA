package com.k1ngtle.vsia.signality.engineering.radio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.zip.CRC32;

public record PacketRadioFrame(
        UUID sourceId,
        UUID destinationId,
        int sequenceNumber,
        int ttl,
        byte[] payload
) {
    public PacketRadioFrame {
        if (sourceId == null) {
            throw new IllegalArgumentException("sourceId");
        }

        destinationId =
                destinationId == null
                        ? new UUID(0L, 0L)
                        : destinationId;

        ttl =
                Math.max(
                        0,
                        Math.min(255, ttl)
                );

        payload =
                payload == null
                        ? new byte[0]
                        : payload.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    public boolean broadcast() {
        return destinationId.getMostSignificantBits() == 0L
                && destinationId.getLeastSignificantBits() == 0L;
    }

    public PacketRadioFrame decrementTtl() {
        return new PacketRadioFrame(
                sourceId,
                destinationId,
                sequenceNumber,
                Math.max(0, ttl - 1),
                payload
        );
    }

    public byte[] encode() {
        ByteBuffer body =
                ByteBuffer
                        .allocate(
                                16 + 16 + 4 + 1 + 4 + payload.length
                        )
                        .order(
                                ByteOrder.BIG_ENDIAN
                        );

        body.putLong(
                sourceId.getMostSignificantBits()
        );

        body.putLong(
                sourceId.getLeastSignificantBits()
        );

        body.putLong(
                destinationId.getMostSignificantBits()
        );

        body.putLong(
                destinationId.getLeastSignificantBits()
        );

        body.putInt(
                sequenceNumber
        );

        body.put(
                (byte) ttl
        );

        body.putInt(
                payload.length
        );

        body.put(
                payload
        );

        byte[] withoutCrc =
                body.array();

        CRC32 crc =
                new CRC32();

        crc.update(
                withoutCrc
        );

        ByteBuffer complete =
                ByteBuffer
                        .allocate(
                                withoutCrc.length + 4
                        )
                        .order(
                                ByteOrder.BIG_ENDIAN
                        );

        complete.put(
                withoutCrc
        );

        complete.putInt(
                (int) crc.getValue()
        );

        return complete.array();
    }

    public static PacketRadioFrame decode(
            byte[] encoded
    ) {
        if (encoded == null
                || encoded.length < 45) {
            throw new IllegalArgumentException(
                    "Packet-radio frame too short"
            );
        }

        byte[] withoutCrc =
                Arrays.copyOf(
                        encoded,
                        encoded.length - 4
                );

        CRC32 crc =
                new CRC32();

        crc.update(
                withoutCrc
        );

        long expected =
                Integer.toUnsignedLong(
                        ByteBuffer
                                .wrap(
                                        encoded,
                                        encoded.length - 4,
                                        4
                                )
                                .order(
                                        ByteOrder.BIG_ENDIAN
                                )
                                .getInt()
                );

        if (crc.getValue() != expected) {
            throw new IllegalArgumentException(
                    "Packet-radio CRC mismatch"
            );
        }

        ByteBuffer buffer =
                ByteBuffer
                        .wrap(
                                withoutCrc
                        )
                        .order(
                                ByteOrder.BIG_ENDIAN
                        );

        UUID source =
                new UUID(
                        buffer.getLong(),
                        buffer.getLong()
                );

        UUID destination =
                new UUID(
                        buffer.getLong(),
                        buffer.getLong()
                );

        int sequence =
                buffer.getInt();

        int ttl =
                Byte.toUnsignedInt(
                        buffer.get()
                );

        int payloadLength =
                buffer.getInt();

        if (payloadLength < 0
                || payloadLength > buffer.remaining()) {
            throw new IllegalArgumentException(
                    "Invalid packet-radio payload length"
            );
        }

        byte[] payload =
                new byte[payloadLength];

        buffer.get(
                payload
        );

        return new PacketRadioFrame(
                source,
                destination,
                sequence,
                ttl,
                payload
        );
    }
}
