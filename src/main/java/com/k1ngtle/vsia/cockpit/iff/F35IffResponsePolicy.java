package com.k1ngtle.vsia.cockpit.iff;

import com.k1ngtle.vsia.signality.radar.iff.*;
import java.util.*;

/** Server-only game rule, not a cryptographic claim that silence proves hostility. */
public final class F35IffResponsePolicy {
    public static final long TIMEOUT_TICKS=200; // Ten seconds at normal 20 TPS; paused servers do not count.
    private final Map<UUID,Long> failedSince=new HashMap<>();
    private final Map<UUID,Observation> decisions=new HashMap<>();
    private Set<UUID> observed=Set.of();
    private Object context;
    private long tick=-1;
    private boolean enabled;
    public void begin(Object level,long gameTick,Set<UUID> liveShips,boolean interrogatorReady){
        if(context!=level||gameTick<tick||(tick>=0&&gameTick-tick>10)||!interrogatorReady){failedSince.clear();decisions.clear();}
        context=level;tick=gameTick;enabled=interrogatorReady&&level!=null&&gameTick>=0;
        observed=enabled?Set.copyOf(liveShips):Set.of();
        failedSince.keySet().retainAll(observed);
        decisions.keySet().retainAll(observed);
    }
    public IffResult apply(UUID canonicalShip,IffResult reply){
        if(reply==null)reply=IffResult.unknown(IffReplyStatus.NO_REPLY);
        if(!enabled||canonicalShip==null||!observed.contains(canonicalShip))return reply;
        boolean authenticated=reply.authenticated()&&reply.replyStatus()==IffReplyStatus.AUTHENTICATED
                &&(reply.affiliation()==IffAffiliation.FRIENDLY||reply.affiliation()==IffAffiliation.FRIENDLY_EMERGENCY);
        // Mode 1/2/3 replies answer the interrogation but are not proof of FRIEND.
        if(authenticated){failedSince.remove(canonicalShip);return remember(canonicalShip,reply,reply.replyStatus(),0);}
        reply=new IffResult(IffAffiliation.UNKNOWN,reply.replyStatus(),reply.callsign(),reply.squawkCode(),reply.modeSAddress(),false,reply.roundTripTimeMicros(),"");
        if(reply.replyStatus()==IffReplyStatus.CODE_REPLY){failedSince.remove(canonicalShip);return remember(canonicalShip,reply,reply.replyStatus(),0);}
        long start=failedSince.computeIfAbsent(canonicalShip,id->tick);
        long elapsed=tick-start;
        if(elapsed<TIMEOUT_TICKS)return remember(canonicalShip,reply,reply.replyStatus(),elapsed);
        IffResult hostile=new IffResult(IffAffiliation.HOSTILE,IffReplyStatus.NO_REPLY_TIMEOUT,reply.callsign(),reply.squawkCode(),reply.modeSAddress(),false,reply.roundTripTimeMicros(),"");
        return remember(canonicalShip,hostile,reply.replyStatus(),elapsed);
    }
    private IffResult remember(UUID ship,IffResult result,IffReplyStatus rawReply,long failedTicks){
        decisions.put(ship,new Observation(ship,rawReply,result.affiliation(),failedTicks,tick));return result;
    }
    public record Observation(UUID ship,IffReplyStatus reply,IffAffiliation affiliation,long failedTicks,long lastTick){ }
    public List<Observation> observations(){return decisions.values().stream().sorted(Comparator.comparing(Observation::ship)).toList();}
    public int pendingCount(){return failedSince.size();}
}
