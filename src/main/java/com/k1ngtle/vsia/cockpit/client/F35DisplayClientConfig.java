package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.display.F35DisplaySettings;
import com.k1ngtle.vsia.cockpit.network.C2SF35DisplayActionPacket;
import com.k1ngtle.vsia.network.VsiaNetwork;
import net.minecraft.client.Minecraft;
import java.util.Locale;

/** Compatibility facade for the existing renderer, bound to its cockpit before use. */
public final class F35DisplayClientConfig {
    private static F35CockpitSeatBlockEntity cockpit;
    private static final F35DisplaySettings DEFAULTS = new F35DisplaySettings();
    private F35DisplayClientConfig() { }
    public static void bind(F35CockpitSeatBlockEntity value) { cockpit=value; }
    public static void clearContext() { cockpit=null; }
    public static void ensureLoaded() { } // Minecraft loads and synchronizes the cockpit's NBT.
    private static F35DisplaySettings settings() { return cockpit==null?DEFAULTS:cockpit.displaySettings(); }
    private static void action(int operation) {
        F35CockpitSeatBlockEntity seated=F35CockpitClientContext.seated();
        if(cockpit==null || seated==null || cockpit.cockpitId()==null || !cockpit.cockpitId().equals(seated.cockpitId()))return;
        if(Minecraft.getInstance().getConnection()==null)return;
        VsiaNetwork.sendToServer(new C2SF35DisplayActionPacket(cockpit.cockpitId(),operation));
    }
    public static int brightnessPercent(){return settings().brightnessPercent();}
    public static void cycleBrightness(){action(20);}
    public static int displayScalePercent(){return settings().displayScalePercent();}
    public static float displayScale(){return displayScalePercent()/100.0F;}
    public static void cycleDisplayScale(){action(21);}
    public static boolean trackLabels(){return settings().flag(0);}
    public static void toggleTrackLabels(){action(0);}
    public static boolean detectMobs(){return settings().flag(1);}
    public static void toggleDetectMobs(){action(1);}
    public static boolean detectPlayers(){return settings().flag(2);}
    public static void toggleDetectPlayers(){action(2);}
    public static boolean detectShips(){return settings().flag(3);}
    public static void toggleDetectShips(){action(3);}
    public static boolean showFriendlyTracks(){return settings().flag(4);}
    public static void toggleShowFriendlyTracks(){action(4);}
    public static boolean showHostileTracks(){return settings().flag(5);}
    public static void toggleShowHostileTracks(){action(5);}
    public static boolean showUnknownTracks(){return settings().flag(6);}
    public static void toggleShowUnknownTracks(){action(6);}
    public static boolean showMissiles(){return settings().flag(7);}
    public static void toggleShowMissiles(){action(7);}
    public static boolean trackTrails(){return settings().flag(8);}
    public static void toggleTrackTrails(){action(8);}
    public static boolean missileTrails(){return settings().flag(9);}
    public static void toggleMissileTrails(){action(9);}
    public static boolean velocityVectors(){return settings().flag(10);}
    public static void toggleVelocityVectors(){action(10);}
    public static void setAllDetection(boolean on){action(on?23:24);}
    public static void setAllRadarTargets(boolean on){action(on?25:26);}
    public static void resetDefaults(){action(27);}
    public static int radarRangeIndex(){return settings().radarRangeIndex();}
    public static void cycleRadarRange(){action(22);}
    public static double forcedRadarRangeMeters(){return switch(radarRangeIndex()){case 0->500;case 1->1000;case 2->5000;case 3->10000;case 4->20000;case 5->40000;case 6->80000;default->-1;};}
    public static String radarRangeLabel(){return switch(radarRangeIndex()){case 0->"500 m";case 1->"1 km";case 2->"5 km";case 3->"10 km";case 4->"20 km";case 5->"40 km";case 6->"80 km";default->"AUTO";};}
    public static boolean radarTrackVisible(String affiliation){String value=affiliation==null?"":affiliation.toUpperCase(Locale.ROOT);return value.contains("FRIENDLY")?showFriendlyTracks():value.contains("HOSTILE")?showHostileTracks():showUnknownTracks();}
    public static String targetSelectionSummary(){String v=(showFriendlyTracks()?"F":"")+(showHostileTracks()?"H":"")+(showUnknownTracks()?"U":"")+(showMissiles()?"M":"");return v.isEmpty()?"NONE":v;}
    public static String detectionSummary(){String v=(detectMobs()?"MOB+":"")+(detectPlayers()?"PLY+":"")+(detectShips()?"SHIP+":"");return v.isEmpty()?"NONE":v.substring(0,v.length()-1);}
    public static int applyBrightness(int color){float f=brightnessPercent()/100.0F;return (color & 0xFF000000)|(Math.round(((color>>>16)&255)*f)<<16)|(Math.round(((color>>>8)&255)*f)<<8)|Math.round((color&255)*f);}
}
