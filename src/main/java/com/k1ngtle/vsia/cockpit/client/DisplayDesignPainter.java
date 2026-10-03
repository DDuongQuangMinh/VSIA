package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.cockpit.program.DisplayDesign;
import com.k1ngtle.vsia.cockpit.display.*;
import com.k1ngtle.vsia.cockpit.detection.*;
import java.util.*;
import net.minecraft.world.phys.Vec3;

/** Same widgets in editor preview and physical display. Preview samples never enter radar caches. */
public final class DisplayDesignPainter {
    public interface Draw {
        void line(float x1,float y1,float x2,float y2,int color);
        void text(String text,float x,float y,float scale,int color);
        void clip(float x,float y,float w,float h);
        default void rect(float x,float y,float w,float h,int c){line(x,y,x+w,y,c);line(x+w,y,x+w,y+h,c);line(x+w,y+h,x,y+h,c);line(x,y+h,x,y,c);}
        default void circle(float x,float y,float r,int c){float px=x+r,py=y;for(int i=1;i<=40;i++){double a=i*Math.PI/20;float nx=x+(float)Math.cos(a)*r,ny=y+(float)Math.sin(a)*r;line(px,py,nx,ny,c);px=nx;py=ny;}}
    }
    private static final int GREEN=0xff9fe19f,AMBER=0xffffca53,RED=0xffff6262,DIM=0xff657474;
    private DisplayDesignPainter(){}
    public static int color(String theme){return switch(theme){case "GREEN"->GREEN;case "AMBER"->AMBER;case "WHITE"->0xffeeeeee;default->0xff7fdddd;};}
    public static double value(String binding,F35DisplayState s){
        if(s==null)return switch(binding){case "SPEED"->185;case "ALTITUDE"->1200;case "HEADING"->90;case "FUEL","STRUCTURE"->82;case "FUEL_KG"->1400;case "CONTACT_COUNT"->3;case "LOCK_RANGE"->850;default->0;};
        if(!s.ownship().shipDetected())return Double.NaN;
        return switch(binding){case "SPEED"->s.ownship().speedMps();case "ALTITUDE"->s.ownship().altitudeMeters();case "HEADING"->s.ownship().headingDeg();case "PITCH"->s.ownship().pitchDeg();case "ROLL"->s.ownship().rollDeg();case "VSPEED"->s.ownship().verticalSpeedMps();case "FUEL"->s.stores().fuelPercent();case "FUEL_KG"->s.stores().fuelKg();case "CONTACT_COUNT"->s.totalContactCount();case "STRUCTURE"->s.shipSilhouette().damage().retainedPercent();case "LOCK_RANGE"->{Vec3 p=lockedPosition(s);yield p==null?Double.NaN:p.distanceTo(s.ownship().position());}default->Double.NaN;};
    }
    private static String num(double v){return Double.isFinite(v)?String.format(Locale.ROOT,"%.1f",v):"NO DATA";}
    private static String shortText(String s,int n){return s==null?"":s.substring(0,Math.min(n,s.length()));}
    public static void paint(Draw d,DisplayDesign design,F35DisplayState state){
        boolean noShip=state!=null&&!state.ownship().shipDetected();
        int c=color(design.theme());for(DisplayDesign.Widget w:design.widgets()){
            d.clip(w.x(),w.y(),w.w(),w.h());float x=w.x()+8,y=w.y()+8,bw=w.w()-16,bh=w.h()-16;
            if(w.variant()!=4){d.rect(w.x()+1,w.y()+1,w.w()-2,w.h()-2,w.variant()==2?c:DIM);if(w.variant()==3){d.line(x,y+16,x+bw,y+16,c);d.rect(x,y,bw,bh,DIM);}}
            if(!w.type().equals("LABEL"))d.text(w.text().isEmpty()?w.type():w.text(),x,y,.75f,c);
            // A placed cockpit still displays its layout, never preview or stale aircraft data.
            if(noShip&&!w.type().equals("LABEL")&&!w.type().equals("PANEL")&&!w.type().equals("CLOCK")){
                d.text("NO SHIP DETECTED",x,y+Math.min(26,Math.max(0,bh-10)),.7f,DIM);continue;
            }
            switch(w.type()) {
                case "LABEL" -> d.text(w.text().isEmpty()?"LABEL":w.text(),x,y,w.variant()==1?1.6f:1,c);
                case "PANEL" -> {if(w.variant()==1)for(int k=32;k<w.h();k+=24)d.line(x,w.y()+k,x+bw,w.y()+k,DIM);}
                case "CLOCK" -> d.text(state==null?"12:00:00":java.time.Instant.ofEpochMilli(state.localTimeMillis()).atZone(java.time.ZoneId.systemDefault()).toLocalTime().withNano(0).toString(),x,y+30,1.5f,c);
                case "VALUE","BAR","GAUGE" -> instrument(d,w,state,c,x,y,bw,bh);
                case "RADAR360","RADARFORWARD" -> radar(d,w,state,c);
                case "CONTACTS" -> contacts(d,w,state,c,x,y,bh);
                case "LOCK" -> lock(d,w,state,c,x,y);
                case "DAMAGE" -> hull(d,w,state,c,x,y,bw,bh);
                case "HORIZON" -> {
                    float cx=x+bw/2,cy=y+bh/2;double roll=Math.toRadians(value("ROLL",state));float slope=(float)Math.sin(roll)*bw*.35f;
                    float pitch=(float)Math.max(-bh*.3,Math.min(bh*.3,value("PITCH",state)*2));
                    d.line(cx-bw*.35f,cy+pitch-slope,cx+bw*.35f,cy+pitch+slope,c);d.line(cx-16,cy,cx+16,cy,AMBER);d.line(cx,cy-6,cx,cy+6,AMBER);
                    if(w.variant()!=4)for(int k=-2;k<=2;k++)d.line(cx-24,cy+pitch+k*20,cx+24,cy+pitch+k*20,DIM);
                    d.text("PITCH "+num(value("PITCH",state))+" ROLL "+num(value("ROLL",state)),x,y+bh-12,.7f,c);
                }
                case "COMPASS" -> {
                    float cx=x+bw/2,cy=y+bh/2,r=Math.max(5,Math.min(bw,bh-18)/2-5);d.circle(cx,cy,r,c);
                    double heading=Math.toRadians(value("HEADING",state));d.line(cx,cy,cx+(float)Math.sin(heading)*r,cy-(float)Math.cos(heading)*r,AMBER);
                    d.text("N",cx-3,cy-r, .8f,c);d.text(num(value("HEADING",state))+" DEG",x,y+bh-12,.8f,c);
                }
                case "STORES" -> {
                    d.text("FUEL "+num(value("FUEL",state))+"%",x,y+24,1,c);
                    d.text(state==null?"GUN: PREVIEW":sText(state.stores().gunLabel())+" "+state.stores().gunRounds(),x,y+44,.8f,c);
                    if(state!=null){int row=0;for(var station:state.stores().stations()){if(65+row*16>bh)break;d.text(station.index()+" "+shortText(station.label(),20)+" x"+station.count(),x,y+65+row++*16,.75f,station.selected()?AMBER:c);}}
                }
            }
        }
        d.clip(0,0,DisplayDesign.WIDTH,DisplayDesign.HEIGHT);
    }
    private static String sText(String s){return shortText(s,20);}
    private static void instrument(Draw d,DisplayDesign.Widget w,F35DisplayState state,int c,float x,float y,float width,float height){
        double v=value(w.binding(),state);d.text(w.binding(),x,y+20,.8f,DIM);d.text(num(v),x,y+40,w.variant()==1?1.7f:1.2f,c);
        double maximum=switch(w.binding()){case "SPEED"->400;case "ALTITUDE"->5000;case "HEADING"->360;case "FUEL_KG"->2000;case "CONTACT_COUNT"->128;case "LOCK_RANGE"->Math.max(1,state==null?5000:state.radarRangeMeters());case "PITCH","ROLL","VSPEED"->90;default->100;};
        float f=(float)Math.max(0,Math.min(1,Double.isFinite(v)?v/maximum:0));
        if(w.type().equals("BAR")){float cy=y+height-18;d.rect(x,cy,width,12,DIM);if(Double.isFinite(v))d.rect(x,cy,width*f,12,c);}
        if(w.type().equals("GAUGE")){float r=Math.max(4,Math.min(width,height-50)/2),cx=x+width/2,cy=y+height-r;d.circle(cx,cy,r,c);double a=Math.toRadians(135+270*f);d.line(cx,cy,cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,Double.isFinite(v)?AMBER:DIM);}
    }
    private static boolean friendly(boolean auth,String affiliation){return auth&&affiliation!=null&&affiliation.startsWith("FRIEND");}
    public static void focus(Draw d,DisplayDesign design,int section){if(section<1||section>6)return;for(int i=0;i<design.widgets().size();i++){var w=design.widgets().get(i);if(com.k1ngtle.vsia.cockpit.program.CustomDisplaySections.section(w,i)==section)d.rect(w.x()+2,w.y()+2,w.w()-4,w.h()-4,0xffffffff);}}
    public record Pick(java.util.UUID id,boolean radar){}
    private record Point(float x,float y){}
    private static Point project(DisplayDesign.Widget w,F35DisplayState state,Vec3 position){
        boolean sector=w.type().equals("RADARFORWARD");float cx=w.x()+w.w()/2f,cy=sector?w.y()+w.h()-22:w.y()+w.h()/2f+8,r=Math.max(1,Math.min(w.w()/2f-18,sector?w.h()-44:w.h()/2f-24));double range=Math.max(1,state.radarRangeMeters());
        Vec3 delta=position.subtract(state.ownship().position());double heading=Math.toRadians(state.ownship().headingDeg());double right=-delta.x*Math.cos(heading)-delta.z*Math.sin(heading),fwd=-delta.x*Math.sin(heading)+delta.z*Math.cos(heading);
        if(Math.hypot(right,fwd)>range||(sector&&fwd<0))return null;return new Point(cx+(float)(right/range)*r,cy-(float)(fwd/range)*r);
    }
    public static Pick pick(DisplayDesign design,F35DisplayState state,float x,float y,float tolerance){
        if(state==null||!state.ownship().shipDetected())return null;
        for(int i=design.widgets().size()-1;i>=0;i--){var w=design.widgets().get(i);if(x<w.x()||x>w.x()+w.w()||y<w.y()||y>w.y()+w.h())continue;
            if(w.type().equals("CONTACTS")){int row=(int)Math.floor((y-w.y()-28)/16),limit=Math.max(0,(w.h()-38)/16);if(row<0||row>=limit)return null;if(row<state.tracks().size())return new Pick(state.tracks().get(row).trackId(),true);row-=state.tracks().size();return row<state.detections().size()?new Pick(state.detections().get(row).contactId(),false):null;}
            if(!w.type().equals("RADAR360")&&!w.type().equals("RADARFORWARD"))return null;
            Pick hit=null;double best=tolerance*tolerance;for(var t:state.tracks()){Point p=project(w,state,t.position());if(p!=null){double distance=(p.x-x)*(p.x-x)+(p.y-y)*(p.y-y);if(distance<=best){best=distance;hit=new Pick(t.trackId(),true);}}}for(var t:state.detections()){Point p=project(w,state,t.position());if(p!=null){double distance=(p.x-x)*(p.x-x)+(p.y-y)*(p.y-y);if(distance<=best){best=distance;hit=new Pick(t.contactId(),false);}}}return hit;
        }return null;
    }
    private static void symbol(Draw d,float x,float y,String id,boolean friend,boolean locked,int variant){int c=friend?GREEN:AMBER;if(friend||variant==2)d.rect(x-4,y-4,8,8,c);else{d.line(x,y-5,x+5,y,c);d.line(x+5,y,x,y+5,c);d.line(x,y+5,x-5,y,c);d.line(x-5,y,x,y-5,c);}if(locked)d.rect(x-8,y-8,16,16,0xffffffff);if(variant!=4)d.text(id,x+7,y-5,.65f,c);}
    private static void radar(Draw d,DisplayDesign.Widget w,F35DisplayState state,int c){
        boolean forward=w.type().equals("RADARFORWARD");float cx=w.x()+w.w()/2f,cy=forward?w.y()+w.h()-22:w.y()+w.h()/2f+8;
        float radius=Math.max(1,Math.min(w.w()/2f-18,forward?w.h()-44:w.h()/2f-24));
        for(int i=1;i<=3;i++){float r=radius*i/3; if(!forward)d.circle(cx,cy,r,i==3?c:DIM);else{float px=cx-r,py=cy;for(int k=1;k<=30;k++){double a=Math.PI+k*Math.PI/30;float nx=cx+(float)Math.cos(a)*r,ny=cy+(float)Math.sin(a)*r;d.line(px,py,nx,ny,DIM);px=nx;py=ny;}}}
        d.line(cx-8,cy,cx+8,cy,c);d.line(cx,cy-8,cx,cy+8,c);
        if(state==null){symbol(d,cx+radius*.25f,cy-radius*.35f,"DEMO",false,false,w.variant());d.text("SAMPLE DATA",w.x()+8,w.y()+w.h()-14,.65f,DIM);return;}
        double range=Math.max(1,state.radarRangeMeters());d.text("RNG "+Math.round(range)+" M",w.x()+8,w.y()+22,.65f,c);
        for(F35RadarTrackView t:state.tracks())plot(d,w,state,t.position(),t.shortId(),friendly(t.iffAuthenticated(),t.iffAffiliation()),F35TargetLockClient.isRadarLocked(t.trackId()),cx,cy,radius,range,forward);
        for(F35DetectionContact t:state.detections())plot(d,w,state,t.position(),t.shortId(),friendly(t.iffAuthenticated(),t.iffStatus()),F35TargetLockClient.isDetectionLocked(t.contactId()),cx,cy,radius,range,forward);
    }
    private static void plot(Draw d,DisplayDesign.Widget w,F35DisplayState state,Vec3 position,String id,boolean friend,boolean locked,float cx,float cy,float r,double range,boolean sector){
        Point p=project(w,state,position);if(p!=null)symbol(d,p.x,p.y,id,friend,locked,w.variant());
    }
    private static void contacts(Draw d,DisplayDesign.Widget w,F35DisplayState state,int c,float x,float y,float height){
        if(state==null){d.text("PREVIEW CONTACT LIST",x,y+28,.8f,DIM);return;}
        int row=0,limit=Math.max(0,(int)(height-22)/16);
        for(var t:state.tracks()){if(row>=limit)break;boolean f=friendly(t.iffAuthenticated(),t.iffAffiliation());d.text(t.shortId()+" "+(f?"FRIEND AUTH":"UNKNOWN")+" "+Math.round(t.position().distanceTo(state.ownship().position()))+"M",x,y+24+row++*16,.7f,f?GREEN:AMBER);}
        for(var t:state.detections()){if(row>=limit)break;boolean f=friendly(t.iffAuthenticated(),t.iffStatus());d.text(t.shortId()+" "+(f?"FRIEND AUTH":t.type().name())+" "+Math.round(t.position().distanceTo(state.ownship().position()))+"M",x,y+24+row++*16,.7f,f?GREEN:AMBER);}
        if(row==0)d.text("NO CONTACTS",x,y+24,.8f,DIM);
    }
    private static Vec3 lockedPosition(F35DisplayState state){if(!state.ownship().shipDetected())return null;if(state.selectedTrack()!=null)return state.selectedTrack().position();var t=F35TargetLockClient.lockedDetection(state.detections());return t==null?null:t.position();}
    private static void lock(Draw d,DisplayDesign.Widget w,F35DisplayState state,int c,float x,float y){
        if(state==null){d.text("LOCK DEMO  850 M",x,y+26,1,DIM);return;}
        Vec3 p=lockedPosition(state);d.text(p==null?"NO LOCK":F35TargetLockClient.summary(state.tracks(),state.detections()),x,y+24,.9f,p==null?DIM:c);
        if(p!=null){d.text("RANGE "+num(p.distanceTo(state.ownship().position()))+" M",x,y+44,.85f,c);d.text(F35TargetLockClient.targetCoasting()?"COAST - AUTH UNKNOWN":"TRACKING",x,y+64,.8f,AMBER);}
    }
    private static void hull(Draw d,DisplayDesign.Widget w,F35DisplayState state,int c,float x,float y,float width,float height){
        if(state==null){d.rect(x+width*.3f,y+30,width*.4f,height-65,c);d.text("PREVIEW HULL",x,y+height-12,.7f,DIM);return;}
        var s=state.shipSilhouette();var damage=s.damage();if(!s.available()){d.text("NO HULL DATA",x,y+30,.8f,DIM);return;}
        F35ShipPlanLayout fit=F35ShipPlanLayout.fit(s,x,y+22,width,Math.max(1,height-64));
        int[] expected=damage.expected(),missing=damage.missing();
        for(int row=0;row<s.height();row++)for(int col=0;col<s.width();col++)if(s.occupied(col,row)){int i=row*s.width()+col;int color=damage.damagedAt(i)?(i<expected.length&&missing[i]==expected[i]?RED:AMBER):c;d.rect(fit.originX()+col*fit.cellWidth(),fit.originY()+row*fit.cellHeight(),Math.max(.1f,fit.cellWidth()-.4f),Math.max(.1f,fit.cellHeight()-.4f),color);}
        d.text("STRUCT "+num(damage.retainedPercent())+"%",x,y+height-28,.75f,c);d.text("HP "+damage.zones(s.width(),s.height()),x,y+height-12,.7f,AMBER);
    }
}
