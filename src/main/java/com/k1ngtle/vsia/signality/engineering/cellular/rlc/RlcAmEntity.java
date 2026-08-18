package com.k1ngtle.vsia.signality.engineering.cellular.rlc;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RlcAmEntity {
    private int nextSequenceNumber;
    private final Map<Integer, RlcPdu[]> receiveBuffer = new HashMap<>();

    public List<RlcPdu> segment(byte[] sdu, int maximumPayloadBytes) {
        if (maximumPayloadBytes < 1) {
            throw new IllegalArgumentException("maximumPayloadBytes");
        }

        byte[] source = sdu == null ? new byte[0] : sdu;

        int sequenceNumber = nextSequenceNumber++ & 0x0FFF;
        int count = Math.max(
                1,
                (source.length + maximumPayloadBytes - 1) / maximumPayloadBytes
        );

        List<RlcPdu> result = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            int start = index * maximumPayloadBytes;
            int end = Math.min(source.length, start + maximumPayloadBytes);
            byte[] piece = java.util.Arrays.copyOfRange(source, start, end);

            result.add(new RlcPdu(
                    sequenceNumber,
                    index,
                    count,
                    piece
            ));
        }

        return result;
    }

    public byte[] receive(RlcPdu pdu) {
        RlcPdu[] segments = receiveBuffer.computeIfAbsent(
                pdu.sequenceNumber(),
                ignored -> new RlcPdu[pdu.segmentCount()]
        );

        if (pdu.segmentIndex() < 0
                || pdu.segmentIndex() >= segments.length) {
            return null;
        }

        segments[pdu.segmentIndex()] = pdu;

        for (RlcPdu segment : segments) {
            if (segment == null) {
                return null;
            }
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            for (RlcPdu segment : segments) {
                out.write(segment.payload());
            }

            receiveBuffer.remove(pdu.sequenceNumber());
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to reassemble RLC SDU",
                    exception
            );
        }
    }
}
