package com.k1ngtle.vsia.cockpit.display;

/** Pure visibility/layout rules; no world scans or remembered wireless cockpit. */
public final class F35HelmetHudRules {
    public record Layout(float left,float top,float scale){ }
    private F35HelmetHudRules(){ }
    public static boolean visible(boolean headSlotHelmet,boolean firstPerson,boolean screenOpen,boolean hiddenGui){return headSlotHelmet&&firstPerson&&!screenOpen&&!hiddenGui;}
    public static boolean suppressPhysical(boolean visible,boolean sameCockpit,boolean supportedDrive){return visible&&sameCockpit&&supportedDrive;}
    public static Layout layout(int pixelWidth,int pixelHeight){
        if(pixelWidth<=0||pixelHeight<=0)return null;
        float scale=Math.min(1.5f,Math.min(pixelWidth*.7f/F35HelmetFlightPainter.WIDTH,pixelHeight*.62f/F35HelmetFlightPainter.HEIGHT));
        return new Layout((pixelWidth-F35HelmetFlightPainter.WIDTH*scale)/2,(pixelHeight-F35HelmetFlightPainter.HEIGHT*scale)/2,scale);
    }
}
