package com.k1ngtle.vsia.signality.engineering.wifi.ip.router;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.routing.Ipv4Prefix;
import java.util.*;

public final class RouterEngine {
    private final Map<String,RouterInterface> interfaces=new LinkedHashMap<>();
    private final List<RouterRoute> staticRoutes=new ArrayList<>();
    private final RouterNeighborTable neighbors=new RouterNeighborTable();

    public void putInterface(RouterInterface iface){interfaces.put(iface.name(),iface);}
    public void removeInterface(String name){interfaces.remove(name);}
    public Collection<RouterInterface> interfaces(){return List.copyOf(interfaces.values());}
    public RouterNeighborTable neighbors(){return neighbors;}
    public void addRoute(RouterRoute route){staticRoutes.add(route);}
    public void clearStaticRoutes(){staticRoutes.clear();}

    public List<RouterRoute> routes(){
        List<RouterRoute> out=new ArrayList<>();
        for(RouterInterface i:interfaces.values()){
            if(i.enabled()) out.add(new RouterRoute(i.network(),i.prefixLength(),"",i.name(),0,"CONNECTED"));
        }
        out.addAll(staticRoutes);
        return List.copyOf(out);
    }

    public RouterForwardDecision evaluate(String ingress, RouterPacket packet){
        if(packet==null)return drop("null packet");
        for(RouterInterface i:interfaces.values()){
            if(i.enabled()&&i.ipv4Address().equals(packet.destinationIp()))
                return decision(RouterForwardAction.LOCAL_DELIVERY,ingress,i.name(),packet,"", "", packet.ttl(),-1,-1,0,0,"LOCAL "+i.name());
        }
        if(packet.ttl()<=1)
            return decision(RouterForwardAction.ICMP_TIME_EXCEEDED,ingress,"",packet,"","",0,-1,-1,11,0,"TTL expired");
        RouterRoute best=routes().stream().filter(r->r.matches(packet.destinationIp()))
            .min(Comparator.comparingInt(RouterRoute::prefixLength).reversed().thenComparingInt(RouterRoute::metric)).orElse(null);
        if(best==null)
            return decision(RouterForwardAction.ICMP_DESTINATION_UNREACHABLE,ingress,"",packet,"","",packet.ttl(),-1,-1,3,0,"No route");
        RouterInterface out=interfaces.get(best.egressInterface());
        if(out==null||!out.enabled())
            return decision(RouterForwardAction.ICMP_DESTINATION_UNREACHABLE,ingress,best.egressInterface(),packet,"","",packet.ttl(),best.prefixLength(),best.metric(),3,0,"Egress unavailable");
        String nextHop=best.connected()?packet.destinationIp():best.nextHop();
        String mac=neighbors.lookup(out.name(),nextHop);
        if(mac.isBlank())
            return decision(RouterForwardAction.ARP_REQUIRED,ingress,out.name(),packet,nextHop,"",packet.ttl()-1,best.prefixLength(),best.metric(),0,0,"ARP required for "+nextHop);
        return decision(RouterForwardAction.FORWARD,ingress,out.name(),packet,nextHop,mac,packet.ttl()-1,best.prefixLength(),best.metric(),0,0,
            "FORWARD "+packet.destinationIp()+" via "+nextHop+" on "+out.name()+" TTL "+packet.ttl()+"->"+(packet.ttl()-1));
    }

    private static RouterForwardDecision decision(RouterForwardAction a,String in,String out,RouterPacket p,String nh,String mac,int ttlOut,int prefix,int metric,int it,int ic,String d){
        return new RouterForwardDecision(a,in,out,p.sourceIp(),p.destinationIp(),nh,mac,p.ttl(),ttlOut,"",prefix,metric,it,ic,d);
    }
    private static RouterForwardDecision drop(String d){return new RouterForwardDecision(RouterForwardAction.DROP,"","","","","","",0,0,"",-1,Integer.MAX_VALUE,0,0,d);}
}
