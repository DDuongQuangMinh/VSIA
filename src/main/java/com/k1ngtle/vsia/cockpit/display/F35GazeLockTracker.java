package com.k1ngtle.vsia.cockpit.display;

import java.util.*;

/** Continuous gaze dwell, separate from the persistent cockpit-owned target lock. */
public final class F35GazeLockTracker {
    public static final long DWELL_NANOS=4_000_000_000L,MAX_GAP_NANOS=250_000_000L;
    public record Candidate(UUID id,boolean radar,float x,float y,double range){ }
    private Candidate target;
    private long since,last;
    private boolean fired;
    public void reset(){target=null;since=last=0;fired=false;}
    public Candidate target(){return target;}
    public float progress(long now){return target==null||now<since?0:Math.min(1,(now-since)/(float)DWELL_NANOS);}
    public boolean update(Candidate next,long now){
        if(next==null||next.id()==null){reset();return false;}
        if(target==null||!target.id().equals(next.id())||target.radar()!=next.radar()||now<last||now-last>MAX_GAP_NANOS){target=next;since=last=now;fired=false;return false;}
        target=next;last=now;
        if(!fired&&now-since>=DWELL_NANOS){fired=true;return true;}
        return false;
    }
    public static float radius(int pixelHeight){return Math.max(12,Math.min(80,pixelHeight*.025f));}
    /** Choose one actual projected live cue nearest the crosshair; stable tie breaks prevent flicker. */
    public static Candidate pick(List<Candidate> candidates,int pixelWidth,int pixelHeight){
        if(pixelWidth<=0||pixelHeight<=0)return null;
        float cx=pixelWidth/2f,cy=pixelHeight/2f;double limit=radius(pixelHeight)*radius(pixelHeight);
        return candidates.stream().filter(c->c!=null&&c.id()!=null&&Float.isFinite(c.x())&&Float.isFinite(c.y())&&Double.isFinite(c.range())&&c.range()>=0)
                .filter(c->distance(c,cx,cy)<=limit)
                .min(Comparator.comparingDouble((Candidate c)->distance(c,cx,cy)).thenComparingDouble(Candidate::range).thenComparing(c->c.id().toString()).thenComparing(Candidate::radar)).orElse(null);
    }
    private static double distance(Candidate c,float x,float y){double dx=c.x()-x,dy=c.y()-y;return dx*dx+dy*dy;}
}
