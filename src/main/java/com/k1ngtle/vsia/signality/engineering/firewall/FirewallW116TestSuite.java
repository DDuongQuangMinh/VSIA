package com.k1ngtle.vsia.signality.engineering.firewall;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class FirewallW116TestSuite {
    private FirewallW116TestSuite() {
    }

    public static List<FirewallW116TestResult> runAll() {
        List<FirewallW116TestResult> out = new ArrayList<>();

        check(out, "wifi-w1160-ipv4-prefix",
                IpPrefixMatcher.matches("192.168.10.100", "192.168.10.0/24"),
                "IPv4 CIDR match failed");
        check(out, "wifi-w1160-ipv4-prefix-reject",
                !IpPrefixMatcher.matches("192.168.20.100", "192.168.10.0/24"),
                "IPv4 CIDR false positive");
        check(out, "wifi-w1160-ipv6-prefix",
                IpPrefixMatcher.matches("2001:db8:10::100", "2001:db8:10::/64"),
                "IPv6 CIDR match failed");
        check(out, "wifi-w1160-family-v4",
                IpPrefixMatcher.family("192.0.2.1") == IpFamily.IPV4,
                "IPv4 family detection failed");
        check(out, "wifi-w1160-family-v6",
                IpPrefixMatcher.family("2001:db8::1") == IpFamily.IPV6,
                "IPv6 family detection failed");

        FirewallFlowKey flow = new FirewallFlowKey(
                IpFamily.IPV4, "TCP",
                "192.168.10.100", 51842,
                "198.51.100.20", 443
        );
        check(out, "wifi-w1160-flow-reverse",
                flow.reverse().reverse().equals(flow),
                "flow reverse is not involutive");

        ConntrackTable ct = new ConntrackTable();
        FirewallPacketView syn = tcp(
                "192.168.10.100", 51842,
                "198.51.100.20", 443,
                "LAN", "WAN",
                true, false, false, false
        );
        check(out, "wifi-w1160-ct-new",
                ct.classify(syn, 0).state() == ConntrackState.NEW,
                "new TCP SYN not classified NEW");

        ct.create(syn, 0);
        check(out, "wifi-w1160-ct-created",
                ct.size() == 1,
                "conntrack entry not created");

        FirewallPacketView synAck = tcp(
                "198.51.100.20", 443,
                "192.168.10.100", 51842,
                "WAN", "LAN",
                true, true, false, false
        );
        check(out, "wifi-w1160-ct-synack-established",
                ct.classify(synAck, 10).state() == ConntrackState.ESTABLISHED,
                "SYN-ACK not established/reply");

        FirewallPacketView ack = tcp(
                "192.168.10.100", 51842,
                "198.51.100.20", 443,
                "LAN", "WAN",
                false, true, false, false
        );
        check(out, "wifi-w1160-ct-ack-established",
                ct.classify(ack, 20).state() == ConntrackState.ESTABLISHED,
                "ACK not established");

        FirewallPacketView fin = tcp(
                "192.168.10.100", 51842,
                "198.51.100.20", 443,
                "LAN", "WAN",
                false, true, true, false
        );
        ct.classify(fin, 30);
        check(out, "wifi-w1160-ct-fin",
                ct.entries().get(0).tcpState() == ConntrackEntry.TcpState.FIN_WAIT,
                "FIN state not tracked");

        FirewallPacketView rst = tcp(
                "198.51.100.20", 443,
                "192.168.10.100", 51842,
                "WAN", "LAN",
                false, true, false, true
        );
        ct.classify(rst, 40);
        check(out, "wifi-w1160-ct-rst",
                ct.entries().get(0).tcpState() == ConntrackEntry.TcpState.CLOSED,
                "RST state not tracked");

        ConntrackTable udpCt = new ConntrackTable();
        FirewallPacketView udpOut = packet("UDP","192.168.10.10",53000,"198.51.100.53",53,"LAN","WAN");
        udpCt.create(udpOut,0);
        FirewallPacketView udpReply = packet("UDP","198.51.100.53",53,"192.168.10.10",53000,"WAN","LAN");
        check(out, "wifi-w1160-udp-reply-established",
                udpCt.classify(udpReply,10).state()==ConntrackState.ESTABLISHED,
                "UDP reply not established");

        FirewallPacketView orphanFragment = new FirewallPacketView(
                IpFamily.IPV4,"UDP","192.168.10.10",0,"198.51.100.20",0,
                "LAN","WAN",false,false,false,false,false,null,77,185,true
        );
        check(out, "wifi-w1160-noninitial-invalid",
                new ConntrackTable().classify(orphanFragment,0).state()==ConntrackState.INVALID,
                "orphan non-initial fragment not INVALID");

        Nat44Table nat = new Nat44Table(40000,40010);
        Nat44Mapping m1 = nat.allocatePat(udpOut,"203.0.113.10",0);
        check(out, "wifi-w1160-pat-public-ip",
                m1.insideGlobalIp().equals("203.0.113.10"),
                "PAT public IP wrong");
        check(out, "wifi-w1160-pat-port-range",
                m1.insideGlobalPort()>=40000 && m1.insideGlobalPort()<=40010,
                "PAT port outside pool");
        check(out, "wifi-w1160-pat-stable",
                nat.allocatePat(udpOut,"203.0.113.10",1).insideGlobalPort()==m1.insideGlobalPort(),
                "PAT mapping not stable");

        FirewallPacketView inbound = packet(
                "UDP","198.51.100.53",53,
                "203.0.113.10",m1.insideGlobalPort(),
                "WAN","LAN"
        );
        check(out, "wifi-w1160-pat-reverse",
                nat.findInbound(inbound,2)==m1,
                "reverse PAT lookup failed");

        FirewallPacketView second = packet(
                "UDP","192.168.10.11",53001,
                "198.51.100.53",53,"LAN","WAN"
        );
        Nat44Mapping m2=nat.allocatePat(second,"203.0.113.10",3);
        check(out, "wifi-w1160-pat-collision-avoidance",
                m1.insideGlobalPort()!=m2.insideGlobalPort(),
                "PAT collision not avoided");

        boolean nat6Rejected=false;
        try {
            nat.allocatePat(
                    new FirewallPacketView(
                            IpFamily.IPV6,"UDP","2001:db8::1",5000,"2001:db8::2",5001,
                            "LAN","WAN",false,false,false,false,false,null,0,0,false
                    ),
                    "203.0.113.10",0
            );
        } catch (IllegalArgumentException expected) {
            nat6Rejected=true;
        }
        check(out, "wifi-w1160-nat44-v6-rejected",nat6Rejected,
                "NAT44 accepted IPv6");

        FragmentAssociationTable fat=new FragmentAssociationTable();
        FirewallPacketView firstFrag=new FirewallPacketView(
                IpFamily.IPV4,"UDP","192.168.10.10",53000,"198.51.100.20",53,
                "LAN","WAN",false,false,false,false,false,null,99,0,true
        );
        fat.remember(firstFrag,ConntrackState.ESTABLISHED,m1,0);
        FirewallPacketView laterFrag=new FirewallPacketView(
                IpFamily.IPV4,"UDP","192.168.10.10",0,"198.51.100.20",0,
                "LAN","WAN",false,false,false,false,false,null,99,185,true
        );
        check(out, "wifi-w1160-fragment-association",
                fat.lookup(laterFrag,1)!=null,
                "later fragment did not associate");
        check(out, "wifi-w1160-fragment-nat-association",
                fat.lookup(laterFrag,1).natMapping()==m1,
                "fragment NAT association lost");

        StatefulFirewallEngine fw=new StatefulFirewallEngine();
        fw.setDefaultPolicy(IpFamily.IPV4,FirewallAction.DROP);
        fw.addRule(new FirewallRule(
                10,"allow-lan-web",FirewallAction.ACCEPT,IpFamily.IPV4,"TCP",
                "192.168.10.0/24","ANY",-1,443,"LAN","WAN",
                EnumSet.of(ConntrackState.NEW,ConntrackState.ESTABLISHED)
        ));
        check(out, "wifi-w1160-rule-accept",
                fw.inspect(syn,60,0).allowed(),
                "matching firewall rule did not accept");
        check(out, "wifi-w1160-rule-counter",
                fw.rules().get(0).packets()==1,
                "rule packet counter not incremented");

        FirewallPacketView blocked=packet(
                "UDP","192.168.10.10",50000,"198.51.100.20",9999,"LAN","WAN"
        );
        check(out, "wifi-w1160-default-drop",
                !fw.inspect(blocked,60,1).allowed(),
                "default DROP not enforced");

        StatefulFirewallEngine establishedFw=StatefulFirewallEngine.permissiveCompatibilityEngine();
        establishedFw.inspect(syn,60,0);
        establishedFw.inspect(synAck,60,1);
        FirewallDecision establishedDecision=establishedFw.inspect(ack,60,2);
        check(out, "wifi-w1160-established-rule",
                establishedDecision.allowed()
                        && establishedDecision.state()==ConntrackState.ESTABLISHED,
                "ESTABLISHED flow not recognized");

        StatefulFirewallEngine natFw=StatefulFirewallEngine.permissiveCompatibilityEngine();
        natFw.enableNat44("203.0.113.10");
        FirewallDecision natDecision=natFw.inspect(udpOut,80,0);
        check(out, "wifi-w1160-engine-snat",
                natDecision.allowed() && natDecision.natMapping()!=null && !natDecision.reverseNat(),
                "engine did not allocate outbound PAT");

        Nat44Mapping engineMap=natDecision.natMapping();
        FirewallPacketView natReply=packet(
                "UDP","198.51.100.53",53,
                engineMap.insideGlobalIp(),engineMap.insideGlobalPort(),
                "WAN","LAN"
        );
        FirewallDecision reverseDecision=natFw.inspect(natReply,80,1);
        check(out, "wifi-w1160-engine-dnat",
                reverseDecision.natMapping()!=null && reverseDecision.reverseNat(),
                "engine did not reverse PAT");

        StatefulFirewallEngine v6fw=StatefulFirewallEngine.permissiveCompatibilityEngine();
        FirewallPacketView v6=new FirewallPacketView(
                IpFamily.IPV6,"TCP","2001:db8:10::100",50000,"2001:db8:20::20",443,
                "LAN","WAN",true,false,false,false,false,null,0,0,false
        );
        check(out, "wifi-w1160-ipv6-native-firewall",
                v6fw.inspect(v6,80,0).allowed(),
                "native IPv6 firewall flow failed");
        check(out, "wifi-w1160-ipv6-no-nat",
                v6fw.inspect(v6,80,1).natMapping()==null,
                "IPv6 unexpectedly NATed");

        FirewallRule rejectRule=new FirewallRule(
                1,"reject-telnet",FirewallAction.REJECT,IpFamily.IPV4,"TCP",
                "ANY","ANY",-1,23,"WAN","LAN",EnumSet.allOf(ConntrackState.class)
        );
        StatefulFirewallEngine rejectFw=new StatefulFirewallEngine();
        rejectFw.addRule(rejectRule);
        FirewallPacketView telnet=packet("TCP","198.51.100.9",40000,"192.168.10.9",23,"WAN","LAN");
        check(out, "wifi-w1160-reject-action",
                rejectFw.inspect(telnet,60,0).action()==FirewallAction.REJECT,
                "REJECT action not preserved");

        check(out, "wifi-w1160-ordered-rules",
                fw.rules().get(0).sequence()==10,
                "rule ordering failed");

        StatefulFirewallEngine firstMatch=new StatefulFirewallEngine();
        firstMatch.addRule(new FirewallRule(20,"allow",FirewallAction.ACCEPT,null,"ANY","ANY","ANY",-1,-1,"ANY","ANY",null));
        firstMatch.addRule(new FirewallRule(10,"drop-first",FirewallAction.DROP,null,"ANY","ANY","ANY",-1,-1,"ANY","ANY",null));
        check(out, "wifi-w1160-first-match",
                firstMatch.inspect(blocked,60,0).action()==FirewallAction.DROP,
                "first-match rule semantics failed");

        ConntrackTable relatedTable=new ConntrackTable();
        relatedTable.create(udpOut,0);
        FirewallPacketView related=new FirewallPacketView(
                IpFamily.IPV4,"ICMP","198.51.100.1",0,"192.168.10.10",0,
                "WAN","LAN",false,false,false,false,true,udpOut.flowKey(),0,0,false
        );
        check(out, "wifi-w1160-icmp-related",
                relatedTable.classify(related,1).state()==ConntrackState.RELATED,
                "ICMP error not classified RELATED");

        FirewallPacketView unrelated=new FirewallPacketView(
                IpFamily.IPV4,"ICMP","198.51.100.1",0,"192.168.10.10",0,
                "WAN","LAN",false,false,false,false,true,
                new FirewallFlowKey(IpFamily.IPV4,"UDP","10.0.0.1",1,"10.0.0.2",2),
                0,0,false
        );
        check(out, "wifi-w1160-icmp-invalid",
                relatedTable.classify(unrelated,1).state()==ConntrackState.INVALID,
                "unrelated ICMP error not INVALID");

        Nat44Table expireNat=new Nat44Table();
        expireNat.allocatePat(udpOut,"203.0.113.10",0);
        check(out, "wifi-w1160-nat-timeout",
                expireNat.expire(60_001L)==1,
                "UDP NAT timeout failed");

        ConntrackTable expireCt=new ConntrackTable();
        expireCt.create(udpOut,0);
        check(out, "wifi-w1160-conntrack-timeout",
                expireCt.expire(30_001L)==1,
                "NEW UDP conntrack timeout failed");

        FragmentAssociationTable expireFrag=new FragmentAssociationTable(100);
        expireFrag.remember(firstFrag,ConntrackState.ESTABLISHED,m1,0);
        check(out, "wifi-w1160-fragment-timeout",
                expireFrag.expire(100)==1,
                "fragment association timeout failed");

        check(out, "wifi-w1160-anti-spoof-prefix-primitive",
                IpPrefixMatcher.matches("10.1.2.3","10.0.0.0/8")
                        && !IpPrefixMatcher.matches("11.1.2.3","10.0.0.0/8"),
                "anti-spoof prefix primitive failed");

        check(out, "wifi-w1160-zone-lan-wan",
                syn.ingressInterface().equals("LAN") && syn.egressInterface().equals("WAN"),
                "zone/direction metadata failed");

        check(out, "wifi-w1160-five-tuple",
                flow.protocol().equals("TCP")
                        && flow.sourcePort()==51842
                        && flow.destinationPort()==443,
                "5-tuple representation failed");

        check(out, "wifi-w1160-dualstack-independent",
                !new FirewallFlowKey(IpFamily.IPV6,"TCP","2001:db8::1",1,"2001:db8::2",2)
                        .equals(new FirewallFlowKey(IpFamily.IPV4,"TCP","192.0.2.1",1,"192.0.2.2",2)),
                "address families not independent");

        check(out, "wifi-w1160-pat-table-visible",
                nat.mappings().size()==2,
                "NAT table diagnostics unavailable");

        check(out, "wifi-w1160-conntrack-table-visible",
                ct.entries().size()==1,
                "conntrack table diagnostics unavailable");

        return List.copyOf(out);
    }

    private static FirewallPacketView packet(
            String protocol,
            String sourceIp,
            int sourcePort,
            String destinationIp,
            int destinationPort,
            String ingress,
            String egress
    ) {
        return new FirewallPacketView(
                IpPrefixMatcher.family(sourceIp),
                protocol,
                sourceIp,
                sourcePort,
                destinationIp,
                destinationPort,
                ingress,
                egress,
                false,false,false,false,false,null,
                0,0,false
        );
    }

    private static FirewallPacketView tcp(
            String sourceIp,
            int sourcePort,
            String destinationIp,
            int destinationPort,
            String ingress,
            String egress,
            boolean syn,
            boolean ack,
            boolean fin,
            boolean rst
    ) {
        return new FirewallPacketView(
                IpFamily.IPV4,"TCP",
                sourceIp,sourcePort,destinationIp,destinationPort,
                ingress,egress,syn,ack,fin,rst,false,null,
                0,0,false
        );
    }

    private static void check(
            List<FirewallW116TestResult> out,
            String id,
            boolean passed,
            String detail
    ) {
        out.add(new FirewallW116TestResult(
                id, passed, passed ? "PASS" : detail
        ));
    }
}
