package com.k1ngtle.vsia.cockpit.display;

/** Pure visibility/layout rules; no world scans or remembered wireless cockpit. */
public final class F35HelmetHudRules {
    public record Layout(float left,float top,float scale){ }
    public record Presentation(Layout flight,Layout tactical,float bottomReserve){ }
    private F35HelmetHudRules(){ }
    public static boolean visible(boolean headSlotHelmet,boolean firstPerson,boolean screenOpen,boolean hiddenGui){return headSlotHelmet&&firstPerson&&!screenOpen&&!hiddenGui;}
    public static boolean suppressPhysical(boolean visible,boolean sameCockpit,boolean supportedDrive){return visible&&sameCockpit&&supportedDrive;}
    public static Layout layout(int pixelWidth,int pixelHeight){
        if(pixelWidth<=0||pixelHeight<=0)return null;
        float scale=Math.min(1.5f,Math.min(pixelWidth*.7f/F35HelmetFlightPainter.WIDTH,pixelHeight*.62f/F35HelmetFlightPainter.HEIGHT));
        return new Layout((pixelWidth-F35HelmetFlightPainter.WIDTH*scale)/2,(pixelHeight-F35HelmetFlightPainter.HEIGHT*scale)/2,scale);
    }
    /** Keep waterline at view center; fit the new panel below all flight text and above vanilla HUD. */
    public static Presentation presentation(int pixelWidth,int pixelHeight,double guiScale){
        Layout base=layout(pixelWidth,pixelHeight);if(base==null)return null;
        double safeGui=Double.isFinite(guiScale)&&guiScale>0?guiScale:1;
        float reserve=(float)Math.min(pixelHeight*.22,Math.max(40,44*safeGui));
        float scale=Math.min(base.scale(),(pixelHeight*.5f-reserve-8)/(139+F35HelmetTacticalPainter.HEIGHT));
        // Avoid unreadable postage-stamp scopes on very small/high-GUI-scale viewports.
        if(scale<.65f)return new Presentation(base,null,reserve);
        Layout flight=new Layout((pixelWidth-F35HelmetFlightPainter.WIDTH*scale)/2,(pixelHeight-F35HelmetFlightPainter.HEIGHT*scale)/2,scale);
        Layout tactical=new Layout((pixelWidth-F35HelmetTacticalPainter.WIDTH*scale)/2,pixelHeight*.5f+139*scale+8,scale);
        return new Presentation(flight,tactical,reserve);
    }
}
