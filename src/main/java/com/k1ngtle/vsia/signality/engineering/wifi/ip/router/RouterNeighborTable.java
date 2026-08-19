package com.k1ngtle.vsia.signality.engineering.wifi.ip.router;
import java.util.*;
public final class RouterNeighborTable{
 private final Map<String,String> neighbors=new LinkedHashMap<>();
 public void learn(String iface,String ip,String mac){if(iface==null||iface.isBlank()||ip==null||ip.isBlank()||mac==null||mac.isBlank())return;neighbors.put(iface+"|"+ip,mac);}
 public String lookup(String iface,String ip){return neighbors.getOrDefault(iface+"|"+ip,"");}
 public void clear(){neighbors.clear();} public int size(){return neighbors.size();}
}
