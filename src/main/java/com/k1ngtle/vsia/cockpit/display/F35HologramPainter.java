package com.k1ngtle.vsia.cockpit.display;

import com.k1ngtle.vsia.cockpit.display.telemetry.AircraftTelemetry;
import java.util.List;
import java.util.Locale;

/** Transparent line-only flight instruments; no pretend G/AOA/throttle/weapons data. */
public final class F35HologramPainter {
    public static final float WIDTH=520,HEIGHT=350;
    public static final int GREEN=0xee62ff7c;
    public interface Draw extends F35ContactSymbols.Lines { void text(String text,float x,float y,float scale,int color); }
    private F35HologramPainter() { }
    public static double heading(double value){return ((value%360)+360)%360;}
    private static String n(double v){return Double.isFinite(v)?String.format(Locale.ROOT,"%.0f",v):"--";}
    private static void wing(Draw d,float x,float y){d.line(x-19,y,x-7,y,GREEN);d.line(x-7,y,x,y+5,GREEN);d.line(x,y+5,x+7,y,GREEN);d.line(x+7,y,x+19,y,GREEN);}
    private static float[] rotate(float x,float y,float roll){double a=Math.toRadians(-roll);return new float[]{260+x*(float)Math.cos(a)-y*(float)Math.sin(a),175+x*(float)Math.sin(a)+y*(float)Math.cos(a)};}
    private static void ladderLine(Draw d,float x1,float y1,float x2,float y2,float roll){float[] a=rotate(x1,y1,roll),b=rotate(x2,y2,roll);d.line(a[0],a[1],b[0],b[1],GREEN);}
    public static void paint(Draw d,F35DisplayState state,List<F35HudContacts.Contact> contacts,boolean hasLock) {
        paint(d,state,contacts,hasLock,contacts.stream().anyMatch(F35HudContacts.Contact::locked));
    }
    public static void paint(Draw d,F35DisplayState state,List<F35HudContacts.Contact> contacts,boolean hasLock,boolean liveLock) {
        wing(d,260,175);
        if(state==null||!state.ownship().shipDetected()){d.text("HUD / NO SHIP DATA",177,38,.95f,GREEN);return;}
        AircraftTelemetry t=state.ownship();
        if(Double.isFinite(t.headingDeg())){
            double h=heading(t.headingDeg());d.text("HDG "+String.format(Locale.ROOT,"%03d",Math.round(h)%360),222,16,1.1f,GREEN);
            for(int i=-40;i<=40;i+=5){float x=260+i*4;d.line(x,50,x,i%10==0?39:44,GREEN);if(i%20==0)d.text(String.format(Locale.ROOT,"%02d",Math.round(heading(h+i)/10)%36),x-7,60,.7f,GREEN);}
            d.line(260,56,255,64,GREEN);d.line(255,64,265,64,GREEN);d.line(265,64,260,56,GREEN);
        }
        d.text("SPD M/S",18,140,.85f,GREEN);d.text(n(t.speedMps()),18,157,1.3f,GREEN);
        d.text("ALT Y M",416,140,.85f,GREEN);d.text(n(t.altitudeMeters()),416,157,1.3f,GREEN);
        d.text("V/S "+n(t.verticalSpeedMps())+" M/S",18,215,.75f,GREEN);
        if(Double.isFinite(t.pitchDeg())&&Double.isFinite(t.rollDeg())){
            float roll=(float)(t.rollDeg()%360),pitch=(float)Math.max(-90,Math.min(90,t.pitchDeg()));
            for(int mark=-90;mark<=90;mark+=5){float y=(pitch-mark)*4;if(Math.abs(y)>78)continue;
                if(mark==0){ladderLine(d,-98,y,-18,y,roll);ladderLine(d,18,y,98,y,roll);}
                else if(mark>0){ladderLine(d,-64,y,-25,y,roll);ladderLine(d,25,y,64,y,roll);}
                else for(int i=0;i<3;i++){ladderLine(d,-64+i*14,y,-56+i*14,y,roll);ladderLine(d,28+i*14,y,36+i*14,y,roll);}
                if(mark!=0){float[] label=rotate(69,y,roll);d.text(Integer.toString(mark),label[0],label[1]-4,.65f,GREEN);}
            }
        }
        var locked=contacts.stream().filter(F35HudContacts.Contact::locked).findFirst().orElse(null);
        if(locked==null)d.text(hasLock?(liveLock?"LOCK HIDDEN BY FILTER":"LOCK LOST / IFF UNKNOWN"):"NO LOCK",189,305,.8f,GREEN);
        else{
            F35ContactSymbols.draw(d,260,278,6,locked.friendly(),true,GREEN,GREEN);
            d.text("LOCK "+locked.label()+" / "+(locked.friendly()?"FRIEND AUTH":"UNKNOWN"),143,305,.8f,GREEN);
            d.text("RNG "+n(locked.position().distanceTo(t.position()))+" M",213,324,.8f,GREEN);
        }
    }
}
