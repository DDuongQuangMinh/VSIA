package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.ExecutionMode;
import com.k1ngtle.vsia.signality.engineering.wifi.link.EtherType;
import com.k1ngtle.vsia.signality.engineering.wifi.link.LlcSnapCodec;
import net.minecraft.nbt.CompoundTag;

public final class RawIpv4TransitCarrierCodec {
    private RawIpv4TransitCarrierCodec() {
    }

    public static CompoundTag wrap(
            byte[] rawIpv4,
            String sourceMac,
            String targetMac,
            CompoundTag metadata,
            int fragmentIndex,
            int fragmentCount
    ) {
        if (rawIpv4 == null || rawIpv4.length < 20) {
            throw new IllegalArgumentException("rawIpv4");
        }

        RawIpv4TransitForwarder.inspect(rawIpv4);

        CompoundTag body = new CompoundTag();

        body.putString(
                RawIpv4LiveCarrierCodec.CONTROL_KEY,
                RawIpv4LiveCarrierCodec.CONTROL_VALUE
        );

        body.putString(
                "execution_mode",
                ExecutionMode.CONFORMANCE.name()
        );

        body.putByteArray(
                RawIpv4LiveCarrierCodec.RAW_MSDU_KEY,
                LlcSnapCodec.encodeRfc1042(
                        EtherType.IPV4,
                        rawIpv4
                )
        );

        body.putString(
                "src_mac",
                sourceMac == null ? "" : sourceMac
        );

        body.putString(
                "dst_mac",
                targetMac == null ? "" : targetMac
        );

        body.putInt(
                "fragment_index",
                Math.max(0, fragmentIndex)
        );

        body.putInt(
                "fragment_count",
                Math.max(0, fragmentCount)
        );

        body.put(
                "logical_meta",
                metadata == null
                        ? new CompoundTag()
                        : metadata.copy()
        );

        body.putBoolean(
                "w1112_transit_forwarded",
                true
        );

        return body;
    }
}
