package com.k1ngtle.vsia.cockpit.display;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import net.minecraft.world.phys.Vec3;

/** Lower visor panel: current cockpit sensor picture, not a new radar or simulated vehicle systems. */
public final class F35HelmetTacticalPainter {
    public static final float WIDTH=520,HEIGHT=174;
    public static final int CYAN=0xff5bece6,AMBER=0xffffca53,RED=0xffff6262,WHITE=0xffd9e5e5,DIM=0xff68777b;
    public interface Draw extends F35HelmetFlightPainter.Draw { void panel(float x,float y,float width,float height,int color); }
    public record Point(float right,float forward){ }
    private record Basis(Vec3 own,double cosine,double sine,double range){ }
    private record Plot(F35HudContacts.Contact contact,Point point){ }
    private static final String[] CARDINALS={"N","E","S","W"};
    private static final int[] AXIS_X={0,1,0,-1},AXIS_Z={-1,0,1,0};
    private F35HelmetTacticalPainter(){ }
    public static boolean usable(F35DisplayState state){
        return state!=null&&state.ownship().shipDetected()&&F35HudProjection.finite(state.ownship().position())&&Double.isFinite(state.ownship().headingDeg())&&Double.isFinite(state.radarRangeMeters())&&state.radarRangeMeters()>0;
    }
    /** Same heading/right convention as the existing cockpit radar. No edge-pinned out-of-range tracks. */
    public static Point project(F35DisplayState state,Vec3 position){
        return project(basis(state),position);
    }
    private static Basis basis(F35DisplayState state){
        if(!usable(state))return null;
        double h=Math.toRadians(F35HologramPainter.heading(state.ownship().headingDeg()));
        return new Basis(state.ownship().position(),Math.cos(h),Math.sin(h),state.radarRangeMeters());
    }
    private static Point project(Basis basis,Vec3 position){
        if(basis==null||!F35HudProjection.finite(position))return null;
        double dx=position.x-basis.own().x,dz=position.z-basis.own().z;
        double right=-dx*basis.cosine()-dz*basis.sine(),forward=-dx*basis.sine()+dz*basis.cosine();
        double distance=Math.hypot(right,forward),range=basis.range();
        if(!Double.isFinite(distance)||distance>range)return null;
        return new Point((float)(right/range),(float)(forward/range));
    }
    private static void center(Draw d,String text,float x,float y,float size,int color){d.text(text,x-d.textWidth(text,size)/2,y,size,color);}
    private static void rect(Draw d,float x,float y,float width,float height,int color){
        d.line(x,y,x+width,y,color);d.line(x+width,y,x+width,y+height,color);d.line(x+width,y+height,x,y+height,color);d.line(x,y+height,x,y,color);
    }
    private static void unavailableGauge(Draw d,String name,float x){
        center(d,name,x+6.5f,40,.75f,WHITE);rect(d,x,56,13,76,DIM);
        // Crosshatching denotes NO DATA, not an empty or zero-percent tank/throttle.
        d.line(x+2,130,x+11,58,DIM);d.line(x+2,93,x+11,93,DIM);center(d,"--",x+6.5f,138,.85f,WHITE);
    }
    private static void compass(Draw d,Basis basis,float cx,float cy,float radius){
        for(int i=0;i<4;i++){
            // Use a normalized unit direction to avoid large-world coordinate cancellation.
            double right=-AXIS_X[i]*basis.cosine()-AXIS_Z[i]*basis.sine(),forward=-AXIS_X[i]*basis.sine()+AXIS_Z[i]*basis.cosine();
            center(d,CARDINALS[i],cx+(float)right*(radius-9),cy-(float)forward*(radius-9)-3,.65f,DIM);
        }
    }
    private static void scope(Draw d,Basis basis,List<Plot> plots,float cx,boolean tactical){
        float cy=88,radius=63,plotRadius=50;
        F35ContactSymbols.circle(d,cx,cy,radius,DIM);
        // Outer rim carries compass labels; the inset ring is the true selected-range scale.
        F35ContactSymbols.circle(d,cx,cy,plotRadius,DIM);
        if(tactical){
            F35ContactSymbols.circle(d,cx,cy,plotRadius*.5f,DIM);
            // Heading-up +/-60 degree azimuth reference, not a simulated scan/emission status.
            for(int sign:new int[]{-1,1}){double a=Math.toRadians(sign*60);d.line(cx,cy,cx+(float)Math.sin(a)*radius,cy-(float)Math.cos(a)*radius,0xff356e73);}
        }
        if(basis==null){center(d,"NO DATA",cx,85,.8f,WHITE);return;}
        compass(d,basis,cx,cy,radius);
        d.line(cx-5,cy,cx+5,cy,CYAN);d.line(cx,cy-5,cx,cy+5,CYAN);
        int plotted=0;
        // Draw selected last so its lock outline stays visible in dense contact clusters.
        for(int pass=0;pass<2;pass++)for(var plot:plots){
            var contact=plot.contact();
            if(contact.locked()!=(pass==1))continue;
            Point p=plot.point();plotted++;
            float x=cx+p.right()*plotRadius,y=cy-p.forward()*plotRadius;
            int color=contact.friendly()?CYAN:contact.hostile()?RED:AMBER;
            F35ContactSymbols.draw(d,x,y,3.5f,contact.friendly(),contact.locked(),color,WHITE);
            if(tactical||contact.locked()){
                String id=contact.label()==null?"?":contact.label();if(id.length()>4)id=id.substring(0,4);
                float size=.7f,width=d.textWidth(id,size);
                d.text(id,Math.max(cx-radius+3,Math.min(cx+radius-3-width,x+6)),Math.max(cy-radius+3,Math.min(cy+radius-9,y+5)),size,color);
            }
        }
        if(plotted==0)center(d,"NO CONTACTS",cx,108,.65f,DIM);
    }
    public static void paint(Draw d,F35DisplayState state,List<F35HudContacts.Contact> contacts,boolean hasLock,boolean liveLock){
        d.panel(0,0,WIDTH,HEIGHT,0xb500080a);rect(d,0,0,WIDTH,HEIGHT,0xff314449);
        unavailableGauge(d,"THRT",15);unavailableGauge(d,"FUEL",48);unavailableGauge(d,"AOA",439);
        center(d,"GEAR",484,40,.75f,WHITE);rect(d,477.5f,56,13,76,DIM);
        for(int i=0;i<3;i++)F35ContactSymbols.circle(d,484,68+i*26,3.5f,DIM);
        d.line(479,130,489,58,DIM);center(d,"--",484,138,.85f,WHITE);
        d.text("SRCH",99,8,.9f,WHITE);d.text(usable(state)?"LIVE":"NO DATA",203,8,.75f,DIM);
        d.text("TSD",287,8,.9f,WHITE);
        String heading=usable(state)?String.format(Locale.ROOT,"%03d",Math.round(F35HologramPainter.heading(state.ownship().headingDeg()))%360):"---";
        center(d,heading,352,8,.9f,WHITE);d.text("NAV --",389,8,.7f,DIM);
        // Compute a contact's heading/range projection once for both scopes and track count.
        Basis basis=basis(state);List<Plot> plots=new ArrayList<>(contacts.size());
        if(basis!=null)for(var contact:contacts){var point=project(basis,contact.position());if(point!=null)plots.add(new Plot(contact,point));}
        scope(d,basis,plots,166,false);scope(d,basis,plots,352,true);
        long count=plots.size();
        d.text("TRKS "+(usable(state)?Long.toString(count):"--"),99,153,.8f,WHITE);
        d.text("RNG "+(usable(state)?F35HelmetFlightPainter.rangeLabel(state.radarRangeMeters()):"--"),162,153,.7f,WHITE);
        center(d,"C/F --",260,143,.65f,DIM);
        var selected=usable(state)?contacts.stream().filter(F35HudContacts.Contact::locked).findFirst().orElse(null):null;
        String status=selected==null?(hasLock?(liveLock?"LOCK FILTERED":"LOCK LOST"):"NO LOCK"):"LOCK "+selected.label();
        d.text(status,287,153,.7f,WHITE);
        if(selected!=null){
            d.text(F35HelmetFlightPainter.rangeLabel(selected.position().distanceTo(state.ownship().position())),361,153,.7f,WHITE);
            d.text(selected.friendly()?"FRIEND AUTH":selected.hostile()?"ENEMY":"IFF UNKNOWN",287,164,.65f,selected.friendly()?CYAN:selected.hostile()?RED:AMBER);
        }
    }
}
