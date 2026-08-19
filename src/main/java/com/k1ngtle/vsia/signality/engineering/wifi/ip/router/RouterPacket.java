package com.k1ngtle.vsia.signality.engineering.wifi.ip.router;
public record RouterPacket(String sourceIp,String destinationIp,int ttl,int protocol,byte[] payload){
 public RouterPacket{payload=payload==null?new byte[0]:payload.clone();}
 @Override public byte[] payload(){return payload.clone();}
 public RouterPacket withTtl(int nextTtl){return new RouterPacket(sourceIp,destinationIp,nextTtl,protocol,payload);}
}
