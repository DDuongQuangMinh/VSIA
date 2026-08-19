package com.k1ngtle.vsia.signality.engineering.wifi.ip.routing;
public record Ipv4ForwardingResult(boolean forward,boolean timeExceeded,int outgoingTtl,Ipv4RouteDecision route,String detail){}
