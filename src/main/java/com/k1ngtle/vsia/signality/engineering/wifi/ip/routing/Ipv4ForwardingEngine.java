package com.k1ngtle.vsia.signality.engineering.wifi.ip.routing;
public final class Ipv4ForwardingEngine{
 private Ipv4ForwardingEngine(){}
 public static Ipv4ForwardingResult evaluate(Ipv4RoutingTable t,String d,int ttl){
  if(ttl<=1)return new Ipv4ForwardingResult(false,true,0,null,"TTL expired; ICMP Time Exceeded required");
  Ipv4RouteDecision r=t.resolve(d);
  if(!r.reachable())return new Ipv4ForwardingResult(false,false,ttl,r,"No route; ICMP Destination Unreachable foundation");
  return new Ipv4ForwardingResult(true,false,ttl-1,r,"Forward with TTL "+(ttl-1)+" via "+r.nextHopIp());
 }
}
