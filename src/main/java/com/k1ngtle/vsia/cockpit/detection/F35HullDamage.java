package com.k1ngtle.vsia.cockpit.detection;

import java.util.*;

/** Measured baseline block loss, not hit points, airworthiness or predicted survival. */
public record F35HullDamage(boolean known,String status,int baselineBlocks,int missingBlocks,
        long lastLossTick,long observationTick,int[] expected,int[] missing,int[] healthPermille) {
    public F35HullDamage(boolean known,String status,int baselineBlocks,int missingBlocks,long lastLossTick,long observationTick,int[] expected,int[] missing){
        this(known,status,baselineBlocks,missingBlocks,lastLossTick,observationTick,expected,missing,null);
    }
    public F35HullDamage {
        status=status==null?"NO DATA":status.substring(0,Math.min(32,status.length()));
        if(baselineBlocks<0||baselineBlocks>32768||missingBlocks<0||missingBlocks>baselineBlocks)
            throw new IllegalArgumentException("Invalid hull block counts");
        expected=expected==null?new int[0]:expected.clone();missing=missing==null?new int[0]:missing.clone();
        if(healthPermille==null){healthPermille=new int[expected.length];for(int i=0;i<expected.length;i++)healthPermille[i]=expected[i]==0?1000:1000*(expected[i]-missing[i])/expected[i];}
        else healthPermille=healthPermille.clone();
        if(healthPermille.length!=expected.length)throw new IllegalArgumentException("Invalid hull health grid");
        for(int hp:healthPermille)if(hp<0||hp>1000)throw new IllegalArgumentException("Invalid hull health percentage");
        if(expected.length!=missing.length||expected.length>4096)throw new IllegalArgumentException("Invalid hull damage grid");
        for(int i=0;i<expected.length;i++)if(expected[i]<0||expected[i]>32768||missing[i]<0||missing[i]>expected[i])throw new IllegalArgumentException("Invalid hull damage cell");
        if(known&&(baselineBlocks==0||Arrays.stream(expected).sum()!=baselineBlocks||Arrays.stream(missing).sum()!=missingBlocks))
            throw new IllegalArgumentException("Inconsistent hull damage totals");
    }
    @Override public int[] expected(){return expected.clone();}
    @Override public int[] missing(){return missing.clone();}
    @Override public int[] healthPermille(){return healthPermille.clone();}
    public double toughnessAt(int index){return !known||index<0||index>=healthPermille.length?Double.NaN:healthPermille[index]/10.0;}
    public boolean damagedAt(int index){return known&&index>=0&&index<expected.length&&expected[index]>0&&(missing[index]>0||healthPermille[index]<1000);}
    public static F35HullDamage unknown(String status){return new F35HullDamage(false,status,0,0,-1,0,null,null);}
    public int missingAt(int index){return index<0||index>=missing.length?0:missing[index];}
    public double cellRetainedPercent(int index){return !known||index<0||index>=expected.length||expected[index]==0?Double.NaN:100.0*(expected[index]-missing[index])/expected[index];}
    public double retainedPercent(){return known?100.0*(baselineBlocks-missingBlocks)/baselineBlocks:Double.NaN;}
    public long lossAgeSeconds(){return lastLossTick<0||lastLossTick>observationTick?-1:(observationTick-lastLossTick)/20;}
    public String zones(int width,int height){
        if(!known)return "UNKNOWN";
        int[] total=new int[9],lost=new int[9];String[] rows={"FWD","MID","AFT"},cols={"L","C","R"};
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){int region=Math.min(2,y*3/Math.max(1,height))*3+Math.min(2,x*3/Math.max(1,width)),index=y*width+x;
            if(index<expected.length){total[region]+=expected[index]*1000;lost[region]+=expected[index]*(1000-healthPermille[index]);}}
        List<Integer> zones=new ArrayList<>();for(int i=0;i<9;i++)if(lost[i]>0)zones.add(i);
        if(zones.isEmpty())return "NONE";
        zones.sort(Comparator.comparingDouble(i->(double)(total[i]-lost[i])/total[i]));
        return String.join(" ",zones.stream().limit(2).map(i->rows[i/3]+"-"+cols[i%3]+" "+Math.round(100.0*(total[i]-lost[i])/total[i])+"%").toList())+(zones.size()>2?" +"+(zones.size()-2):"");
    }
}
