package com.k1ngtle.vsia.signality.engineering.wifi.ip.routing;
public record Ipv4RouteDecision(Ipv4RouteKind kind,String destinationIp,String nextHopIp,String matchedNetwork,int prefixLength,int metric,String source,String detail){
 public boolean reachable(){return kind!=Ipv4RouteKind.UNREACHABLE;}
}
