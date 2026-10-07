package com.k1ngtle.vsia.cockpit.display;

import com.k1ngtle.vsia.cockpit.display.telemetry.AircraftTelemetry;
import java.util.List;
import java.util.Locale;
import net.minecraft.world.phys.Vec3;

/** Reference-style visor instruments. Sample stores are deliberately not flight/weapon telemetry. */
public final class F35HelmetFlightPainter {
    public static final float WIDTH=520,HEIGHT=350;
    public static final int GREEN=F35HologramPainter.GREEN;
    public static final double METRES_PER_SECOND_TO_KNOTS=1.9438444924406;
    public interface Draw extends F35HologramPainter.Draw {
        default void rotatedText(String text,float x,float y,float size,float degrees,int color){text(text,x,y,size,color);}
    }
    private F35HelmetFlightPainter(){ }
    private static String number(double value){return Double.isFinite(value)?String.format(Locale.ROOT,"%.0f",value):"--";}
    public static String rangeLabel(double metres){
        if(!Double.isFinite(metres)||metres<0)return "RNG --";
        return metres>=1000?String.format(Locale.ROOT,"%.1f KM",metres/1000):number(metres)+" M";
    }
    public static double groundSpeedKnots(AircraftTelemetry t){
        if(t==null||!t.shipDetected()||!F35HudProjection.finite(t.velocity()))return Double.NaN;
        double value=Math.hypot(t.velocity().x,t.velocity().z)*METRES_PER_SECOND_TO_KNOTS;
        return Double.isFinite(value)?value:Double.NaN;
    }
    private static void center(Draw d,String text,float x,float y,float size){d.text(text,x-d.textWidth(text,size)/2,y,size,GREEN);}
    private static void fitted(Draw d,String text,float x,float y,float maxWidth,float size){
        d.text(text,x,y,Math.min(size,maxWidth/Math.max(1,d.textWidth(text,1))),GREEN);
    }
    private static void box(Draw d,String text,float x,float y,float width){
        d.line(x,y,x+width,y,GREEN);d.line(x+width,y,x+width,y+19,GREEN);
        d.line(x+width,y+19,x,y+19,GREEN);d.line(x,y+19,x,y,GREEN);
        float size=Math.min(1.55f,(width-8)/Math.max(1,d.textWidth(text,1)));
        center(d,text,x+width/2,y+3,size);
    }
    private static float[] rotate(float x,float y,float roll){
        double a=Math.toRadians(-roll);
        return new float[]{260+x*(float)Math.cos(a)-y*(float)Math.sin(a),175+x*(float)Math.sin(a)+y*(float)Math.cos(a)};
    }
    private static void rung(Draw d,float a,float b,float c,float e,float roll){
        float[] p=rotate(a,b,roll),q=rotate(c,e,roll);
        // Clip the banked ladder to the central optical area, not the side readouts.
        float dx=q[0]-p[0],dy=q[1]-p[1],lo=0,hi=1;
        float[] edges={-dx,dx,-dy,dy},dist={p[0]-141,379-p[0],p[1]-94,260-p[1]};
        for(int i=0;i<4;i++){
            if(Math.abs(edges[i])<.00001f){if(dist[i]<0)return;continue;}
            float t=dist[i]/edges[i];
            if(edges[i]<0)lo=Math.max(lo,t);else hi=Math.min(hi,t);
            if(lo>hi)return;
        }
        d.line(p[0]+dx*lo,p[1]+dy*lo,p[0]+dx*hi,p[1]+dy*hi,GREEN);
    }
    private static void pitchLabel(Draw d,int mark,float x,float y,float roll){
        float[] p=rotate(x,y,roll);
        // Room for the entire rotated short label, even at an inverted bank.
        if(p[0]<158||p[0]>354||p[1]<110||p[1]>244)return;
        d.rotatedText(Integer.toString(Math.abs(mark)),p[0],p[1],.85f,-roll,GREEN);
    }
    private static void heading(Draw d,double value){
        if(!Double.isFinite(value)){center(d,"HDG --",260,31,1.25f);return;}
        double h=F35HologramPainter.heading(value);
        center(d,String.format(Locale.ROOT,"%03d",Math.round(h)%360),260,31,1.3f);
        // Absolute five-degree ticks slide smoothly; north wraps without duplicate labels.
        for(int tick=(int)Math.ceil((h-35)/5)*5;tick<=h+35;tick+=5){
            float x=260+(float)(tick-h)*3.7f;
            d.line(x,61,x,tick%10==0?49:55,GREEN);
            if(tick%10==0)center(d,Integer.toString((int)F35HologramPainter.heading(tick)/10),x,66,.85f);
        }
        d.line(260,79,256,85,GREEN);d.line(256,85,264,85,GREEN);d.line(264,85,260,79,GREEN);
    }
    private static void ladder(Draw d,AircraftTelemetry t){
        if(!Double.isFinite(t.pitchDeg())||!Double.isFinite(t.rollDeg())){center(d,"ATT --",260,211,.85f);return;}
        float pitch=(float)Math.max(-90,Math.min(90,t.pitchDeg())),roll=(float)(t.rollDeg()%360);
        for(int mark=-90;mark<=90;mark+=5){
            float y=(pitch-mark)*4;if(Math.abs(y)>80)continue;
            float outer=mark==0?102:64,inner=mark==0?19:26;
            if(mark>=0){rung(d,-outer,y,-inner,y,roll);rung(d,inner,y,outer,y,roll);}
            else for(int i=0;i<3;i++){rung(d,-64+i*14,y,-56+i*14,y,roll);rung(d,28+i*14,y,36+i*14,y,roll);}
            if(mark!=0){
                // Positive rungs point down toward the horizon; negative rungs point up.
                float tip=mark>0?4:-4;rung(d,-64,y,-64,y+tip,roll);rung(d,64,y,64,y+tip,roll);
                pitchLabel(d,mark,-82,y-4,roll);pitchLabel(d,mark,69,y-4,roll);
            }
        }
    }
    /** Positive means closing. Only the selected current source's measured velocity is used. */
    public static double closure(F35DisplayState state,F35HudContacts.Contact selected){
        if(state==null||selected==null||selected.id()==null)return Double.NaN;
        Vec3 own=state.ownship().velocity(),target=null;
        if(selected.radar()){for(var track:state.tracks())if(selected.id().equals(track.trackId())){target=track.velocity();break;}}
        else {for(var detection:state.detections())if(selected.id().equals(detection.contactId())){target=detection.velocity();break;}}
        if(!F35HudProjection.finite(own)||!F35HudProjection.finite(target)||!F35HudProjection.finite(selected.position())||!F35HudProjection.finite(state.ownship().position()))return Double.NaN;
        Vec3 los=selected.position().subtract(state.ownship().position());double distance=los.length();
        if(!Double.isFinite(distance)||distance<.001)return Double.NaN;
        double result=own.subtract(target).dot(los.scale(1/distance));
        return Double.isFinite(result)?result:Double.NaN;
    }
    /** Ground-referenced flight path, not aerodynamic AOA. Hidden below 1 m/s or with invalid data. */
    public static Vec3 velocityPoint(AircraftTelemetry t,Vec3 camera){
        if(t==null||!t.shipDetected()||!F35HudProjection.finite(camera)||!F35HudProjection.finite(t.velocity()))return null;
        double speed=t.velocity().length();if(!Double.isFinite(speed)||speed<1)return null;
        Vec3 point=camera.add(t.velocity().scale(64/speed));return F35HudProjection.finite(point)?point:null;
    }
    public static void flightPath(F35ContactSymbols.Lines d,float x,float y,int color){
        F35ContactSymbols.circle(d,x,y,5,color);
        d.line(x-12,y,x-5,y,color);d.line(x+5,y,x+12,y,color);d.line(x,y-10,x,y-5,color);
    }
    public static void paint(Draw d,F35DisplayState state,List<F35HudContacts.Contact> contacts,boolean hasLock,boolean liveLock){
        if(state==null||!state.ownship().shipDetected()){center(d,"HUD / NO SHIP DATA",260,38,1);return;}
        AircraftTelemetry t=state.ownship();heading(d,t.headingDeg());ladder(d,t);
        // Small fixed waterline, not a target marker. The velocity marker is camera-projected separately.
        d.line(246,175,254,175,GREEN);d.line(254,175,257,180,GREEN);d.line(257,180,260,175,GREEN);
        d.line(260,175,263,180,GREEN);d.line(263,180,266,175,GREEN);d.line(266,175,274,175,GREEN);
        d.text("GS KT",40,131,.8f,GREEN);box(d,number(groundSpeedKnots(t)),40,146,62);
        d.text("ALT Y M",401,131,.8f,GREEN);box(d,number(t.altitudeMeters()),401,146,79);
        // These systems have no real provider yet; never copy DebugStoresProvider's sample ammo/fuel.
        d.text("AOA --",40,187,.9f,GREEN);d.text("G --",40,201,.9f,GREEN);d.text("T --",40,215,.9f,GREEN);
        fitted(d,"V/S "+number(t.verticalSpeedMps())+" M/S",40,235,92,.8f);
        d.text("WPN --",40,278,.9f,GREEN);d.text("AMMO --",40,292,.9f,GREEN);
        d.text("NAV --",401,278,.9f,GREEN);d.text("C/F --",401,292,.9f,GREEN);
        var selected=contacts.stream().filter(F35HudContacts.Contact::locked).findFirst().orElse(null);
        fitted(d,"R "+(selected==null?"--":number(selected.position().distanceTo(t.position())))+" M",401,187,100,.9f);
        fitted(d,"VC "+number(closure(state,selected))+" M/S",401,201,100,.9f);
        fitted(d,"IFF "+(selected==null?"--":selected.friendly()?"FRIEND":selected.hostile()?"ENEMY":"UNK"),401,219,100,.9f);
        if(selected==null)center(d,hasLock?(liveLock?"LOCK HIDDEN BY FILTER":"LOCK LOST / IFF UNKNOWN"):"NO LOCK",260,299,.85f);
        else center(d,"LOCK "+selected.label()+" / "+(selected.friendly()?"FRIEND AUTH":selected.hostile()?"ENEMY":"UNKNOWN"),260,299,.85f);
    }
}
