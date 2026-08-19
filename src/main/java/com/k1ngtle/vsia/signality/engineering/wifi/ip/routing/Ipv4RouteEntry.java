package com.k1ngtle.vsia.signality.engineering.wifi.ip.routing;
public record Ipv4RouteEntry(String network,int prefixLength,String nextHop,int metric,String source){
 public Ipv4RouteEntry{
  if(prefixLength<0||prefixLength>32)throw new IllegalArgumentException("prefixLength");
  if(metric<0)throw new IllegalArgumentException("metric");
  network=Ipv4Prefix.network(network,prefixLength); nextHop=nextHop==null?"":nextHop; source=source==null?"":source;
 }
 public boolean onLink(){return nextHop.isBlank()||"0.0.0.0".equals(nextHop);}
 public boolean matches(String d){return Ipv4Prefix.matches(d,network,prefixLength);}
}
