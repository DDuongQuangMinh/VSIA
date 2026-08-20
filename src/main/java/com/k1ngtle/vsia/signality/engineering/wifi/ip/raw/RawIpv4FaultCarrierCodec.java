package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.ExecutionMode;
import com.k1ngtle.vsia.signality.engineering.wifi.link.EtherType;
import com.k1ngtle.vsia.signality.engineering.wifi.link.LlcSnapCodec;
import net.minecraft.nbt.CompoundTag;

public final class RawIpv4FaultCarrierCodec {
    private RawIpv4FaultCarrierCodec() {
    }

    public static CompoundTag wrapUnchecked(
            byte[] rawIpv4,
            String sourceMac,
            String targetMac,
            String faultName
    ) {
        if (rawIpv4 == null
                || rawIpv4.length == 0) {
            throw new IllegalArgumentException(
                    "rawIpv4"
            );
        }

        CompoundTag body =
                new CompoundTag();

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
                sourceMac == null
                        ? ""
                        : sourceMac
        );

        body.putString(
                "dst_mac",
                targetMac == null
                        ? ""
                        : targetMac
        );

        body.putInt(
                "fragment_index",
                0
        );

        body.putInt(
                "fragment_count",
                1
        );

        CompoundTag metadata =
                new CompoundTag();

        metadata.putBoolean(
                "w114_header_fault_probe",
                true
        );

        metadata.putString(
                "w114_fault_name",
                faultName == null
                        ? ""
                        : faultName
        );

        body.put(
                "logical_meta",
                metadata
        );

        return body;
    }
}
