package com.k1ngtle.vsia.cockpit.display;

import com.k1ngtle.vsia.cockpit.detection.F35DetectionFilter;
import net.minecraft.nbt.CompoundTag;

/** Saved independently by each cockpit block entity; no client or player-wide storage. */
public final class F35DisplaySettings {
    private int brightnessPercent = 100;
    private int displayScalePercent = 100;
    private int radarRangeIndex = -1;
    private int flags = 0xFFF;
    public static final int LABELS=0, MOBS=1, PLAYERS=2, SHIPS=3, FRIENDS=4, HOSTILES=5,
            UNKNOWNS=6, MISSILES=7, TRAILS=8, MISSILE_TRAILS=9, VECTORS=10;
    public int brightnessPercent() { return brightnessPercent; }
    public int displayScalePercent() { return displayScalePercent; }
    public int radarRangeIndex() { return radarRangeIndex; }
    public boolean flag(int bit) { return (flags & (1 << bit)) != 0; }
    private void flag(int bit, boolean on) { flags = on ? flags | (1 << bit) : flags & ~(1 << bit); }
    public void apply(int action) {
        if (action >= 0 && action <= VECTORS) { flags ^= 1 << action; return; }
        switch (action) {
            case 20 -> brightnessPercent = brightnessPercent > 40 ? brightnessPercent - 15 : 100;
            case 21 -> displayScalePercent = displayScalePercent > 80 ? displayScalePercent - 5 : 100;
            case 22 -> radarRangeIndex = radarRangeIndex >= 6 ? -1 : radarRangeIndex + 1;
            case 23, 24 -> { boolean on = action == 23; flag(MOBS,on); flag(PLAYERS,on); flag(SHIPS,on); }
            case 25, 26 -> { boolean on = action == 25; flag(FRIENDS,on); flag(HOSTILES,on); flag(UNKNOWNS,on); flag(MISSILES,on); }
            case 27 -> { brightnessPercent=100; displayScalePercent=100; radarRangeIndex=-1; flags=0xFFF; }
            default -> { }
        }
    }
    public void setDetection(boolean mobs, boolean players, boolean ships, boolean missiles) {
        flag(MOBS,mobs); flag(PLAYERS,players); flag(SHIPS,ships); flag(MISSILES,missiles);
    }
    public F35DetectionFilter detectionFilter() { return new F35DetectionFilter(flag(MOBS),flag(PLAYERS),flag(SHIPS),flag(MISSILES)); }
    public CompoundTag save() {
        CompoundTag tag=new CompoundTag(); tag.putInt("Brightness",brightnessPercent);
        tag.putInt("Scale",displayScalePercent); tag.putInt("Range",radarRangeIndex); tag.putInt("Flags",flags); return tag;
    }
    public void load(CompoundTag tag) {
        brightnessPercent=tag.contains("Brightness")?clamp(tag.getInt("Brightness"),25,100):100;
        displayScalePercent=tag.contains("Scale")?clamp(tag.getInt("Scale"),80,100):100;
        radarRangeIndex=tag.contains("Range")?clamp(tag.getInt("Range"),-1,6):-1;
        flags=tag.contains("Flags")?tag.getInt("Flags") & 0xFFF:0xFFF;
    }
    private static int clamp(int value,int min,int max) { return Math.max(min,Math.min(max,value)); }
}
