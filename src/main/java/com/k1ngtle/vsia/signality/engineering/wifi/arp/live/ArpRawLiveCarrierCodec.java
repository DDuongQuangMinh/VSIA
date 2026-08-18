package com.k1ngtle.vsia.signality.engineering.wifi.arp.live;

import com.k1ngtle.vsia.signality.engineering.ExecutionMode;
import com.k1ngtle.vsia.signality.engineering.wifi.arp.ArpCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.arp.ArpOperation;
import com.k1ngtle.vsia.signality.engineering.wifi.arp.ArpPacket;
import com.k1ngtle.vsia.signality.engineering.wifi.link.EtherType;
import com.k1ngtle.vsia.signality.engineering.wifi.link.LlcSnapCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.link.LlcSnapFrame;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

public final class ArpRawLiveCarrierCodec {
    public static final String CONTROL_KEY =
            "vsia_raw_network_control";

    public static final String CONTROL_VALUE =
            "LLC_SNAP_ARP_V1";

    public static final String RAW_MSDU_KEY =
            "raw_llc_snap_msdu";

    private ArpRawLiveCarrierCodec() {
    }

    public static boolean isRawArpCarrier(
            CompoundTag body
    ) {
        return body != null
                && CONTROL_VALUE.equals(
                body.getString(
                        CONTROL_KEY
                )
        )
                && body.contains(
                RAW_MSDU_KEY
        );
    }

    public static CompoundTag encode(
            OSINetworkPacket logical
    ) {
        if (logical == null
                || !"ARP".equalsIgnoreCase(
                logical.applicationProtocol
        )) {
            throw new IllegalArgumentException(
                    "Raw ARP carrier requires an ARP OSINetworkPacket"
            );
        }

        String operation =
                logical.payload.getString(
                        "operation"
                );

        ArpPacket arp =
                "REPLY".equalsIgnoreCase(
                        operation
                )
                        ? ArpCodec.reply(
                        logical.payload.getString(
                                "sender_mac"
                        ),
                        logical.payload.getString(
                                "sender_ip"
                        ),
                        logical.targetMac,
                        logical.payload.getString(
                                "target_ip"
                        )
                )
                        : ArpCodec.request(
                        logical.payload.getString(
                                "sender_mac"
                        ),
                        logical.payload.getString(
                                "sender_ip"
                        ),
                        logical.payload.getString(
                                "target_ip"
                        )
                );

        byte[] msdu =
                LlcSnapCodec.encodeRfc1042(
                        EtherType.ARP,
                        ArpCodec.encode(
                                arp
                        )
                );

        CompoundTag body =
                new CompoundTag();

        body.putString(
                CONTROL_KEY,
                CONTROL_VALUE
        );

        body.putString(
                "execution_mode",
                ExecutionMode.CONFORMANCE.name()
        );

        body.putByteArray(
                RAW_MSDU_KEY,
                msdu
        );

        body.putString(
                "src_mac",
                logical.sourceMac
        );

        body.putString(
                "dst_mac",
                logical.targetMac
        );

        body.putLong(
                "w1_request_id",
                logical.payload.getLong(
                        "w1_request_id"
                )
        );

        body.putLong(
                "sent_us",
                logical.payload.getLong(
                        "sent_us"
                )
        );

        return body;
    }

    public static OSINetworkPacket decode(
            CompoundTag body
    ) {
        if (!isRawArpCarrier(
                body
        )) {
            throw new IllegalArgumentException(
                    "Not a VSIA raw ARP carrier"
            );
        }

        LlcSnapFrame frame =
                LlcSnapCodec.decodeRfc1042(
                        body.getByteArray(
                                RAW_MSDU_KEY
                        )
                );

        if (frame.etherType()
                != EtherType.ARP.value()) {
            throw new IllegalArgumentException(
                    "ARP carrier requires EtherType 0x0806"
            );
        }

        ArpPacket arp =
                ArpCodec.decode(
                        frame.payload()
                );

        OSINetworkPacket logical =
                new OSINetworkPacket();

        logical.sourceMac =
                arp.senderMac();

        logical.targetMac =
                arp.operation()
                        == ArpOperation.REQUEST
                        ? "FF:FF:FF:FF:FF:FF"
                        : arp.targetMac();

        logical.sourceIp =
                arp.senderIp();

        logical.targetIp =
                arp.targetIp();

        logical.applicationProtocol =
                "ARP";

        logical.ipProtocol =
                0;

        logical.isResponse =
                arp.operation()
                        == ArpOperation.REPLY;

        logical.payload.putString(
                "operation",
                arp.operation()
                        .name()
        );

        logical.payload.putString(
                "sender_ip",
                arp.senderIp()
        );

        logical.payload.putString(
                "sender_mac",
                arp.senderMac()
        );

        logical.payload.putString(
                "target_ip",
                arp.targetIp()
        );

        logical.payload.putLong(
                "w1_request_id",
                body.getLong(
                        "w1_request_id"
                )
        );

        logical.payload.putLong(
                "sent_us",
                body.getLong(
                        "sent_us"
                )
        );

        return logical;
    }
}
