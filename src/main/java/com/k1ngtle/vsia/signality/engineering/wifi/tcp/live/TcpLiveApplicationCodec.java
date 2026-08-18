package com.k1ngtle.vsia.signality.engineering.wifi.tcp.live;

import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public final class TcpLiveApplicationCodec {
    private TcpLiveApplicationCodec() {
    }

    public static byte[] encode(
            OSINetworkPacket packet
    ) {
        if (packet == null) {
            throw new IllegalArgumentException(
                    "packet"
            );
        }

        try {
            ByteArrayOutputStream bytes =
                    new ByteArrayOutputStream();

            NbtIo.write(
                    packet.serializeNBT(),
                    new DataOutputStream(
                            bytes
                    )
            );

            return bytes.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to encode TCP application packet",
                    exception
            );
        }
    }

    public static OSINetworkPacket decode(
            byte[] bytes
    ) {
        if (bytes == null
                || bytes.length == 0) {
            throw new IllegalArgumentException(
                    "bytes"
            );
        }

        try {
            CompoundTag tag =
                    NbtIo.read(
                            new DataInputStream(
                                    new ByteArrayInputStream(
                                            bytes
                                    )
                            )
                    );

            return OSINetworkPacket.deserializeNBT(
                    tag
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to decode TCP application packet",
                    exception
            );
        }
    }
}
