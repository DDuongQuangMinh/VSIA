package com.k1ngtle.vsia.signality.engineering.wifi.dhcp;

import com.k1ngtle.vsia.signality.engineering.wifi.dhcp.live.DhcpRawLiveCarrierCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.link.LlcSnapCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.link.LlcSnapFrame;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public final class DhcpRawTestSuite {
    private DhcpRawTestSuite() {}

    public static List<DhcpRawTestResult> runAll() {
        return List.of(
                discoverRoundTrip(),
                offerRoundTrip(),
                requestRoundTrip(),
                ackRoundTrip(),
                magicCookieReject(),
                udpPorts(),
                llcSnapIpv4(),
                doraTypes(),
                carrierRequestMacPreserved(),
                carrierReplyMacPreserved()
        );
    }

    private static DhcpRawTestResult discoverRoundTrip() {
        OSINetworkPacket p = logical("DISCOVER", "001122334455", 0x12345678, "");
        CompoundTag body = DhcpRawLiveCarrierCodec.encode(p);
        OSINetworkPacket d = DhcpRawLiveCarrierCodec.decode(body);
        return result("wifi-w1102-discover",
                d.payload.getString("type").equals("DISCOVER")
                        && d.payload.getInt("xid") == 0x12345678
                        && d.sourcePort == 68 && d.targetPort == 67,
                "DISCOVER must survive raw BOOTP/DHCP over UDP 68->67");
    }

    private static DhcpRawTestResult offerRoundTrip() {
        OSINetworkPacket p = logical("OFFER", "001122334455", 0x12345678, "192.168.1.100");
        p.sourceMac="AABBCCDDEEFF"; p.targetMac="001122334455";
        p.sourceIp="192.168.1.2"; p.targetIp="255.255.255.255"; p.sourcePort=67;p.targetPort=68;p.isResponse=true;
        p.payload.putString("server_identifier","192.168.1.2");
        p.payload.putString("subnet_mask","255.255.255.0");
        p.payload.putString("router_ip","192.168.1.1");
        p.payload.putString("dns_server","192.168.1.2");
        p.payload.putInt("lease_seconds",3600);
        OSINetworkPacket d=DhcpRawLiveCarrierCodec.decode(DhcpRawLiveCarrierCodec.encode(p));
        return result("wifi-w1102-offer",
                d.payload.getString("type").equals("OFFER")
                        && d.payload.getString("assigned_ip").equals("192.168.1.100"),
                "OFFER must carry yiaddr and server/network options");
    }

    private static DhcpRawTestResult requestRoundTrip() {
        OSINetworkPacket p = logical("REQUEST","001122334455",0x12345678,"");
        p.payload.putString("requested_ip","192.168.1.100");
        p.payload.putString("server_identifier","192.168.1.2");
        OSINetworkPacket d=DhcpRawLiveCarrierCodec.decode(DhcpRawLiveCarrierCodec.encode(p));
        return result("wifi-w1102-request",
                d.payload.getString("requested_ip").equals("192.168.1.100")
                        && d.payload.getString("server_identifier").equals("192.168.1.2"),
                "REQUEST must include options 50 and 54");
    }

    private static DhcpRawTestResult ackRoundTrip() {
        OSINetworkPacket p = logical("ACK","001122334455",0x12345678,"192.168.1.100");
        p.sourceMac="AABBCCDDEEFF";p.targetMac="001122334455";p.sourceIp="192.168.1.2";p.sourcePort=67;p.targetPort=68;p.isResponse=true;
        p.payload.putString("server_identifier","192.168.1.2");
        p.payload.putString("subnet_mask","255.255.255.0");
        p.payload.putString("router_ip","192.168.1.1");
        p.payload.putString("dns_server","192.168.1.2");
        p.payload.putInt("lease_seconds",3600);
        OSINetworkPacket d=DhcpRawLiveCarrierCodec.decode(DhcpRawLiveCarrierCodec.encode(p));
        return result("wifi-w1102-ack",
                d.payload.getString("type").equals("ACK")
                        && d.payload.getInt("lease_seconds")==3600,
                "ACK must carry lease and network configuration options");
    }

    private static DhcpRawTestResult magicCookieReject() {
        DhcpPacket p=new DhcpPacket(1,1,6,0,1,0,0x8000,"0.0.0.0","0.0.0.0","0.0.0.0","0.0.0.0","00:11:22:33:44:55",DhcpCodec.discoverOptions("00:11:22:33:44:55"));
        byte[] b=DhcpCodec.encode(p); b[236]=0;
        boolean rejected=false; try{DhcpCodec.decode(b);}catch(IllegalArgumentException e){rejected=true;}
        return result("wifi-w1102-cookie-reject",rejected,"Decoder must require DHCP magic cookie 63 82 53 63");
    }

    private static DhcpRawTestResult udpPorts() {
        OSINetworkPacket d=DhcpRawLiveCarrierCodec.decode(DhcpRawLiveCarrierCodec.encode(logical("DISCOVER","001122334455",1,"")));
        return result("wifi-w1102-udp-ports",d.sourcePort==68&&d.targetPort==67&&d.ipProtocol==17,"DHCP client traffic must use IPv4 UDP 68->67");
    }

    private static DhcpRawTestResult llcSnapIpv4() {
        CompoundTag body=DhcpRawLiveCarrierCodec.encode(logical("DISCOVER","001122334455",1,""));
        LlcSnapFrame f=LlcSnapCodec.decodeRfc1042(body.getByteArray(DhcpRawLiveCarrierCodec.RAW_MSDU_KEY));
        return result("wifi-w1102-llc-snap",f.etherType()==0x0800,"DHCP must ride LLC/SNAP EtherType 0x0800 because DHCP is inside IPv4");
    }

    private static DhcpRawTestResult doraTypes() {
        return result("wifi-w1102-dora-types",
                DhcpMessageType.DISCOVER.code()==1&&DhcpMessageType.OFFER.code()==2
                        &&DhcpMessageType.REQUEST.code()==3&&DhcpMessageType.ACK.code()==5,
                "DORA message type codes must be 1,2,3,5");
    }

    private static DhcpRawTestResult carrierRequestMacPreserved() {
        OSINetworkPacket request =
                logical(
                        "DISCOVER",
                        "3ade93b46caf",
                        0x10203040,
                        ""
                );

        OSINetworkPacket decoded =
                DhcpRawLiveCarrierCodec.decode(
                        DhcpRawLiveCarrierCodec.encode(
                                request
                        )
                );

        return result(
                "wifi-w11021-request-link-mac",
                "3ade93b46caf".equals(
                        decoded.sourceMac
                )
                        && "3A:DE:93:B4:6C:AF".equals(
                        decoded.payload.getString(
                                "client_hardware_mac"
                        )
                ),
                "DHCP decode must preserve VSIA compact Wi-Fi source MAC while retaining BOOTP chaddr separately"
        );
    }

    private static DhcpRawTestResult carrierReplyMacPreserved() {
        OSINetworkPacket offer =
                logical(
                        "OFFER",
                        "3ade93b46caf",
                        0x10203040,
                        "192.168.1.100"
                );

        offer.sourceMac =
                "c72ec34fc58c";

        offer.targetMac =
                "3ade93b46caf";

        offer.sourceIp =
                "192.168.1.2";

        offer.sourcePort =
                67;

        offer.targetPort =
                68;

        offer.isResponse =
                true;

        offer.payload.putString(
                "server_identifier",
                "192.168.1.2"
        );

        offer.payload.putString(
                "subnet_mask",
                "255.255.255.0"
        );

        offer.payload.putString(
                "router_ip",
                "192.168.1.1"
        );

        offer.payload.putString(
                "dns_server",
                "192.168.1.2"
        );

        offer.payload.putInt(
                "lease_seconds",
                3600
        );

        OSINetworkPacket decoded =
                DhcpRawLiveCarrierCodec.decode(
                        DhcpRawLiveCarrierCodec.encode(
                                offer
                        )
                );

        return result(
                "wifi-w11021-reply-link-mac",
                "c72ec34fc58c".equals(
                        decoded.sourceMac
                )
                        && "3ade93b46caf".equals(
                        decoded.targetMac
                )
                        && "3A:DE:93:B4:6C:AF".equals(
                        decoded.payload.getString(
                                "client_hardware_mac"
                        )
                ),
                "OFFER/ACK must route to the actual compact station MAC instead of the colon-formatted BOOTP chaddr"
        );
    }

    private static OSINetworkPacket logical(String type,String mac,int xid,String assigned) {
        OSINetworkPacket p=new OSINetworkPacket();
        p.sourceMac=mac;p.targetMac="FF:FF:FF:FF:FF:FF";p.sourceIp="0.0.0.0";p.targetIp="255.255.255.255";
        p.sourcePort=68;p.targetPort=67;p.applicationProtocol="DHCP";
        p.payload.putString("type",type);p.payload.putInt("xid",xid);p.payload.putString("assigned_ip",assigned);
        return p;
    }

    private static DhcpRawTestResult result(String id,boolean passed,String detail){return new DhcpRawTestResult(id,passed,detail);}
}
