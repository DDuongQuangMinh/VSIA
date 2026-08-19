package com.k1ngtle.vsia.signality.engineering.wifi.ip.router;
import java.util.*;

public final class RouterTestSuite {
 private RouterTestSuite(){}
 public static List<RouterTestResult> runAll(){
  return List.of(connectedRoutes(),forwardAcrossSubnets(),arpRequired(),ttlExpired(),noRoute(),localDelivery(),longestPrefix(),metricTie(),preserveDestination(),icmpChecksum(),disabledInterface(),defaultRoute());
 }
 private static RouterEngine router(){
  RouterEngine r=new RouterEngine();
  r.putInterface(new RouterInterface("lan0","192.168.1.1",24,"aa",true));
  r.putInterface(new RouterInterface("lan1","192.168.2.1",24,"bb",true));
  return r;
 }
 private static RouterPacket p(String d,int ttl){return new RouterPacket("192.168.1.101",d,ttl,6,new byte[]{1,2,3});}
 private static RouterTestResult connectedRoutes(){return ok("wifi-w1106-connected-routes",router().routes().size()==2,"two connected routes");}
 private static RouterTestResult forwardAcrossSubnets(){RouterEngine r=router();r.neighbors().learn("lan1","192.168.2.20","cc");var d=r.evaluate("lan0",p("192.168.2.20",64));return ok("wifi-w1106-forward",d.action()==RouterForwardAction.FORWARD&&d.egressInterface().equals("lan1")&&d.outgoingTtl()==63,"cross-subnet forward");}
 private static RouterTestResult arpRequired(){var d=router().evaluate("lan0",p("192.168.2.20",64));return ok("wifi-w1106-arp-required",d.action()==RouterForwardAction.ARP_REQUIRED&&d.nextHopIp().equals("192.168.2.20"),"egress ARP");}
 private static RouterTestResult ttlExpired(){var d=router().evaluate("lan0",p("192.168.2.20",1));return ok("wifi-w1106-ttl",d.action()==RouterForwardAction.ICMP_TIME_EXCEEDED&&d.icmpType()==11,"TTL error");}
 private static RouterTestResult noRoute(){var d=router().evaluate("lan0",p("10.0.0.1",64));return ok("wifi-w1106-unreachable",d.action()==RouterForwardAction.ICMP_DESTINATION_UNREACHABLE&&d.icmpType()==3,"no-route error");}
 private static RouterTestResult localDelivery(){var d=router().evaluate("lan0",p("192.168.2.1",64));return ok("wifi-w1106-local",d.action()==RouterForwardAction.LOCAL_DELIVERY,"router interface local delivery");}
 private static RouterTestResult longestPrefix(){RouterEngine r=router();r.addRoute(new RouterRoute("10.0.0.0",8,"192.168.2.9","lan1",50,"STATIC"));r.addRoute(new RouterRoute("10.20.0.0",16,"192.168.2.8","lan1",100,"STATIC"));r.neighbors().learn("lan1","192.168.2.8","dd");var d=r.evaluate("lan0",p("10.20.4.5",64));return ok("wifi-w1106-lpm",d.nextHopIp().equals("192.168.2.8"),"longest prefix");}
 private static RouterTestResult metricTie(){RouterEngine r=router();r.addRoute(new RouterRoute("10.0.0.0",8,"192.168.2.9","lan1",50,"STATIC"));r.addRoute(new RouterRoute("10.0.0.0",8,"192.168.2.8","lan1",10,"STATIC"));r.neighbors().learn("lan1","192.168.2.8","dd");var d=r.evaluate("lan0",p("10.1.1.1",64));return ok("wifi-w1106-metric",d.nextHopIp().equals("192.168.2.8"),"metric tie-break");}
 private static RouterTestResult preserveDestination(){RouterEngine r=router();r.addRoute(new RouterRoute("0.0.0.0",0,"192.168.2.254","lan1",100,"DEFAULT"));r.neighbors().learn("lan1","192.168.2.254","ee");var d=r.evaluate("lan0",p("203.0.113.7",64));return ok("wifi-w1106-preserve-dst",d.destinationIp().equals("203.0.113.7")&&d.nextHopIp().equals("192.168.2.254"),"final IP preserved");}
 private static RouterTestResult icmpChecksum(){byte[] b=IcmpErrorModel.encode(11,0,new byte[]{1,2,3,4});return ok("wifi-w1106-icmp-checksum",IcmpErrorModel.internetChecksum(b)==0,"ICMP checksum");}
 private static RouterTestResult disabledInterface(){RouterEngine r=router();r.putInterface(new RouterInterface("lan1","192.168.2.1",24,"bb",false));var d=r.evaluate("lan0",p("192.168.2.20",64));return ok("wifi-w1106-disabled-if",d.action()==RouterForwardAction.ICMP_DESTINATION_UNREACHABLE,"disabled egress");}
 private static RouterTestResult defaultRoute(){RouterEngine r=router();r.addRoute(new RouterRoute("0.0.0.0",0,"192.168.2.254","lan1",100,"DEFAULT"));var d=r.evaluate("lan0",p("198.51.100.9",64));return ok("wifi-w1106-default",d.nextHopIp().equals("192.168.2.254"),"default route");}
 private static RouterTestResult ok(String id,boolean p,String d){return new RouterTestResult(id,p,d);}
}
