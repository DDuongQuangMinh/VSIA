package com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.TcpFlags;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.options.TcpOptionSet;

public record RawTcpPacket(
        int sourcePort,
        int destinationPort,
        long sequenceNumber,
        long acknowledgementNumber,
        int dataOffsetWords,
        TcpFlags flags,
        int window,
        int checksum,
        int urgentPointer,
        byte[] optionBytes,
        TcpOptionSet options,
        byte[] payload,
        boolean checksumValid
) {
    public RawTcpPacket {
        optionBytes =
                optionBytes == null
                        ? new byte[0]
                        : optionBytes.clone();

        options =
                options == null
                        ? TcpOptionSet.none()
                        : options;

        payload =
                payload == null
                        ? new byte[0]
                        : payload.clone();
    }

    @Override
    public byte[] optionBytes() {
        return optionBytes.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    public int headerBytes() {
        return dataOffsetWords
                * 4;
    }
}
