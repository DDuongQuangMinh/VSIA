package com.k1ngtle.vsia.cockpit.display;

import com.k1ngtle.vsia.cockpit.display.telemetry.AircraftTelemetry;
import java.util.List;
import java.util.Locale;

/** Transparent flight instruments; no pretend G/AOA/throttle/weapons data. */
public final class F35HologramPainter {
    public static final float WIDTH=520,HEIGHT=350;
    public static final int GREEN=0xff39ff40;
    public static final float STROKE_WIDTH=.85f;
    public interface Draw extends F35ContactSymbols.Lines {
        void text(String text,float x,float y,float scale,int color);
        default float textWidth(String text,float scale){return text.length()*6*scale;}
    }
    private F35HologramPainter() { }
    public static double heading(double value){return ((value%360)+360)%360;}
    private static String n(double v){return Double.isFinite(v)?String.format(Locale.ROOT,"%.0f",v):"--";}
    private static void wing(Draw d,float x,float y){d.line(x-19,y,x-7,y,GREEN);d.line(x-7,y,x,y+5,GREEN);d.line(x,y+5,x+7,y,GREEN);d.line(x+7,y,x+19,y,GREEN);}
    private static float[] rotate(float x,float y,float roll){double a=Math.toRadians(-roll);return new float[]{260+x*(float)Math.cos(a)-y*(float)Math.sin(a),175+x*(float)Math.sin(a)+y*(float)Math.cos(a)};}
    private static void ladderLine(Draw d,float x1,float y1,float x2,float y2,float roll){float[] a=rotate(x1,y1,roll),b=rotate(x2,y2,roll);d.line(a[0],a[1],b[0],b[1],GREEN);}
    private static void centered(Draw d,String text,float center,float y,float size){d.text(text,center-d.textWidth(text,size)/2,y,size,GREEN);}
    // Instrument readout frames only. Contact markers remain circle/diamond.
    private static void readout(Draw d,float x,float y,String text){
        float width=Math.min(WIDTH-x-8,Math.max(58,d.textWidth(text,1.9f)+12));
        float size=Math.min(1.9f,(width-12)/Math.max(1,d.textWidth(text,1)));
        d.line(x,y,x+width,y,GREEN);d.line(x+width,y,x+width,y+23,GREEN);
        d.line(x+width,y+23,x,y+23,GREEN);d.line(x,y+23,x,y,GREEN);
        centered(d,text,x+width/2,y+3,size);
    }
    public static void paint(Draw d,F35DisplayState state,List<F35HudContacts.Contact> contacts,boolean hasLock) {
        paint(d,state,contacts,hasLock,contacts.stream().anyMatch(F35HudContacts.Contact::locked));
    }
    public static void paint(Draw d,F35DisplayState state,List<F35HudContacts.Contact> contacts,boolean hasLock,boolean liveLock) {
        wing(d,260,175);
        if(state==null||!state.ownship().shipDetected()){centered(d,"HUD / NO SHIP DATA",260,38,1);return;}
        AircraftTelemetry t=state.ownship();
        if(Double.isFinite(t.headingDeg())){
            double h=heading(t.headingDeg());centered(d,"HDG "+String.format(Locale.ROOT,"%03d",Math.round(h)%360),260,9,1.4f);
            for(int i=-40;i<=40;i+=5){float x=260+i*4;d.line(x,50,x,i%10==0?36:43,GREEN);if(i%20==0)centered(d,String.format(Locale.ROOT,"%02d",Math.round(heading(h+i)/10)%36),x,60,.85f);}
            d.line(260,56,255,64,GREEN);d.line(255,64,265,64,GREEN);d.line(265,64,260,56,GREEN);
        }
        d.text("SPD M/S",12,129,1,GREEN);readout(d,12,147,n(t.speedMps()));
        d.text("ALT Y M",390,129,1,GREEN);readout(d,390,147,n(t.altitudeMeters()));
        d.text("V/S "+n(t.verticalSpeedMps())+" M/S",12,213,.85f,GREEN);
        if(Double.isFinite(t.pitchDeg())&&Double.isFinite(t.rollDeg())){
            float roll=(float)(t.rollDeg()%360),pitch=(float)Math.max(-90,Math.min(90,t.pitchDeg()));
            for(int mark=-90;mark<=90;mark+=5){float y=(pitch-mark)*4;if(Math.abs(y)>78)continue;
                if(mark==0){ladderLine(d,-98,y,-18,y,roll);ladderLine(d,18,y,98,y,roll);}
                else if(mark>0){ladderLine(d,-64,y,-25,y,roll);ladderLine(d,25,y,64,y,roll);}
                else for(int i=0;i<3;i++){ladderLine(d,-64+i*14,y,-56+i*14,y,roll);ladderLine(d,28+i*14,y,36+i*14,y,roll);}
                if(mark!=0){float[] label=rotate(69,y,roll);d.text(Integer.toString(mark),label[0],label[1]-4,.85f,GREEN);}
            }
        }
        var locked=contacts.stream().filter(F35HudContacts.Contact::locked).findFirst().orElse(null);
        if(locked==null)centered(d,hasLock?(liveLock?"LOCK HIDDEN BY FILTER":"LOCK LOST / IFF UNKNOWN"):"NO LOCK",260,304,1);
        else{
            // No fixed-position target diamond: the only target symbol is projected
            // onto the actual detected contact by the pilot's camera overlay.
            centered(d,"LOCK "+locked.label()+" / "+(locked.friendly()?"FRIEND AUTH":"UNKNOWN"),260,297,1);
            centered(d,"RNG "+n(locked.position().distanceTo(t.position()))+" M",260,320,1);
        }
    }
}
