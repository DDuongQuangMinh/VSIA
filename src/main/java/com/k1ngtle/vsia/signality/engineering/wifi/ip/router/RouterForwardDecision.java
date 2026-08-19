package com.k1ngtle.vsia.signality.engineering.wifi.ip.router;
public record RouterForwardDecision(RouterForwardAction action,String ingressInterface,String egressInterface,String sourceIp,String destinationIp,String nextHopIp,String nextHopMac,int incomingTtl,int outgoingTtl,String matchedNetwork,int prefixLength,int metric,int icmpType,int icmpCode,String detail){
 public boolean forwarded(){return action==RouterForwardAction.FORWARD;}
}
