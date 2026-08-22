package com.k1ngtle.vsia.signality.engineering.host.w120;

import com.k1ngtle.vsia.signality.engineering.firewall.w117.W117ArpFrame;
import com.k1ngtle.vsia.signality.engineering.firewall.w117.W117HostEndpoint;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import java.util.List;
import java.util.Objects;

public final class W120HostStack {
    private W117HostEndpoint endpoint;
    private String name="", ipv4="0.0.0.0", mask="255.255.255.0", gateway="0.0.0.0", mac="";
    private long ethTx,ethRx,arpTx,arpRx,ipTx,ipRx,echoReq,echoReply,noLinkDrops;
    private String lastEvent="READY";

    public void configure(String name,String ipv4,String mask,String gateway,String mac){
        String n=safe(name,"host"), i=safe(ipv4,"0.0.0.0"), m=safe(mask,"255.255.255.0"),
               g=safe(gateway,"0.0.0.0"), a=safe(mac,"");
        if(endpoint!=null&&Objects.equals(this.name,n)&&Objects.equals(this.ipv4,i)
                &&Objects.equals(this.mask,m)&&Objects.equals(this.gateway,g)&&Objects.equals(this.mac,a))return;
        this.name=n;this.ipv4=i;this.mask=m;this.gateway=g;this.mac=a;
        endpoint=new W117HostEndpoint(n,i,m,g,a);
        lastEvent="CONFIGURED "+i+" mask="+m+" gw="+g+" mac="+a;
    }

    public List<OSINetworkPacket> ping(String target,long now){
        OSINetworkPacket p=new OSINetworkPacket();
        p.sourceIp=ipv4;p.targetIp=target;p.sourceMac=mac;p.ttl=64;p.ipProtocol=1;p.ipPacketLength=84;
        p.applicationProtocol="ICMP";p.sessionId="W1.20-PING-"+Long.toUnsignedString(now);
        // W1.20.8 canonical ICMP request metadata + W117 compatibility
        int identifier=(int)(now&0xFFFFL), sequence=(int)((now>>>16)&0xFFFFL);
        p.payload.putString("type","ECHO_REQUEST");
        p.payload.putInt("identifier",identifier);
        p.payload.putInt("sequence",sequence);
        p.payload.putByteArray("data",new byte[32]);
        p.payload.putLong("w1_request_id",now);
        p.payload.putBoolean("w117_echo_request",true);
        p.payload.putBoolean("w117_echo_reply",false);
        p.payload.putBoolean("w120_real_host",true);
        echoReq++;
        List<OSINetworkPacket> out=endpoint.sendIpv4(p,now); account(out);
        lastEvent=out.isEmpty()?"PING_PENDING_ARP target="+target:
                (W117ArpFrame.isArp(out.get(0))?"ARP_TX nextHop="+W117ArpFrame.targetIp(out.get(0)):"IPV4_TX target="+target);
        return out;
    }

    public List<OSINetworkPacket> receive(OSINetworkPacket p,long now){
        if(p==null)return List.of();
        boolean arp=W117ArpFrame.isArp(p);
        boolean forMac=!mac.isBlank()&&mac.equalsIgnoreCase(p.targetMac);
        boolean group="ff:ff:ff:ff:ff:ff".equalsIgnoreCase(p.targetMac);
        boolean forIp=ipv4.equals(p.targetIp);
        if(!arp&&!forMac&&!group&&!forIp)return List.of();
        ethRx++;if(arp)arpRx++;else if(forIp)ipRx++;
        boolean icmpReply =
                p.payload != null
                        && (
                        p.payload.getBoolean("w117_echo_reply")
                                || "ECHO_REPLY".equals(
                                p.payload.getString("type")
                        )
                );

        if (icmpReply) {
            echoReply++;
            lastEvent="ICMP_ECHO_REPLY "+p.sourceIp;
        } else {
            lastEvent=arp?"ARP_RX "+p.sourceIp+" "+p.sourceMac:"IPV4_RX "+p.sourceIp+" -> "+p.targetIp;
        }
        List<OSINetworkPacket> out=endpoint.receive(p,now);account(out);return out;
    }

    public List<OSINetworkPacket> tick(long now){List<OSINetworkPacket> out=endpoint.tick(now);account(out);return out;}
    public void noteNoPhysicalLink(int count){if(count>0){noLinkDrops+=count;lastEvent="DROP_NO_WIRED_LINK frames="+count;}}

    public String status(long now){
        return "W1.20 REAL HOST | ip="+ipv4+" mask="+mask+" gw="+gateway+" mac="+mac
                +" | neighbors="+endpoint.neighborCount(now)+" pending="+endpoint.pendingCount()
                +" | ethTx="+ethTx+" ethRx="+ethRx+" arpTx="+arpTx+" arpRx="+arpRx
                +" ipTx="+ipTx+" ipRx="+ipRx+" echoReq="+echoReq+" echoReply="+echoReply
                +" noLinkDrops="+noLinkDrops+" | last="+lastEvent;
    }

    private void account(List<OSINetworkPacket> packets){
        if(packets==null)return;
        for(OSINetworkPacket p:packets){if(p==null)continue;ethTx++;if(W117ArpFrame.isArp(p))arpTx++;else ipTx++;}
    }
    private static String safe(String v,String f){return v==null||v.isBlank()?f:v.trim();}
}
