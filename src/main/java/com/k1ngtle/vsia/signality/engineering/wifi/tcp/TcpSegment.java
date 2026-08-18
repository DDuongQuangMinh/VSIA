package com.k1ngtle.vsia.signality.engineering.wifi.tcp;

public record TcpSegment(
        int sourcePort,
        int destinationPort,
        long sequenceNumber,
        long acknowledgementNumber,
        TcpFlags flags,
        int window,
        int payloadBytes,
        long sentAtMicros,
        boolean retransmission
) {
    public TcpSegment {
        sequenceNumber =
                TcpSequence.normalize(
                        sequenceNumber
                );

        acknowledgementNumber =
                TcpSequence.normalize(
                        acknowledgementNumber
                );

        if (sourcePort < 0
                || sourcePort > 65535
                || destinationPort < 0
                || destinationPort > 65535) {
            throw new IllegalArgumentException(
                    "TCP port out of range"
            );
        }

        if (window < 0
                || window > 65535) {
            throw new IllegalArgumentException(
                    "TCP window out of range"
            );
        }

        if (payloadBytes < 0) {
            throw new IllegalArgumentException(
                    "payloadBytes"
            );
        }

        if (flags == null) {
            flags =
                    TcpFlags.ackOnly();
        }
    }

    public long sequenceSpaceLength() {
        long length =
                payloadBytes;

        if (flags.syn()) {
            length++;
        }

        if (flags.fin()) {
            length++;
        }

        return length;
    }

    public long endSequenceExclusive() {
        return TcpSequence.add(
                sequenceNumber,
                sequenceSpaceLength()
        );
    }
}
