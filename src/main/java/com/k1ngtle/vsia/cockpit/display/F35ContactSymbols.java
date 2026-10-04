package com.k1ngtle.vsia.cockpit.display;

import java.util.Locale;

/** Shared, non-rectangular IFF/lock symbols across physical HUD, GUI and custom radar. */
public final class F35ContactSymbols {
    @FunctionalInterface public interface Lines { void line(float x1,float y1,float x2,float y2,int color); }
    private F35ContactSymbols() { }
    public static boolean friendly(boolean authenticated,String affiliation) {
        if (!authenticated || affiliation == null) return false;
        String value = affiliation.toUpperCase(Locale.ROOT).split("/",2)[0].trim();
        return value.equals("FRIENDLY") || value.equals("FRIEND");
    }
    public static void circle(Lines d,float x,float y,float radius,int color) {
        float px=x+radius,py=y;
        for(int i=1;i<=40;i++){double a=i*Math.PI/20;float nx=x+(float)Math.cos(a)*radius,ny=y+(float)Math.sin(a)*radius;d.line(px,py,nx,ny,color);px=nx;py=ny;}
    }
    public static void diamond(Lines d,float x,float y,float radius,int color) {
        d.line(x,y-radius,x+radius,y,color);d.line(x+radius,y,x,y+radius,color);
        d.line(x,y+radius,x-radius,y,color);d.line(x-radius,y,x,y-radius,color);
    }
    public static void draw(Lines d,float x,float y,float radius,boolean friend,boolean locked,int color,int lockColor) {
        if(friend)circle(d,x,y,radius,color);else diamond(d,x,y,radius,color);
        if(locked)lock(d,x,y,radius,friend,lockColor);
    }
    public static void lock(Lines d,float x,float y,float radius,boolean friend,int color) {
        float outer=radius+4;
        if(friend)circle(d,x,y,outer,color);else diamond(d,x,y,outer,color);
        // Four short radial ticks identify a lock without surrounding the target in a box.
        for(int i=0;i<4;i++){double a=i*Math.PI/2;float dx=(float)Math.cos(a),dy=(float)Math.sin(a);d.line(x+dx*(outer+2),y+dy*(outer+2),x+dx*(outer+5),y+dy*(outer+5),color);}
    }
}
