package com.k1ngtle.vsia.cockpit.detection;

import com.k1ngtle.vsia.cockpit.F35VsShipHelper;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/** Per-cockpit server-owned reference hull. No fake damage or client-submitted health. */
public final class F35StructureMonitor {
    public static final int MAX_BLOCKS=32768,MAX_VOLUME=131072;
    private long shipId=Long.MIN_VALUE,lastLossTick=-1,lastObservationTick=-1,lastScanTick=-1;
    private final Set<Long> baseline=new HashSet<>(),previousMissing=new HashSet<>();
    private final Map<Long,Integer> referenceHp=new HashMap<>();
    private F35ShipSilhouette cached=F35ShipSilhouette.empty();
    private long revision;
    public long revision(){return revision;}
    public void invalidateScan(){lastScanTick=-1;}
    public F35ShipSilhouette scan(ServerLevel level,F35VsShipHelper.ShipBounds bounds,long ship,Direction forward,BlockPos anchor,long tick,boolean rebaseline){
        if(!rebaseline&&ship==shipId&&lastScanTick>=0&&tick>=lastScanTick&&tick-lastScanTick<20)return cached;
        lastScanTick=tick;
        Set<Long> present=new HashSet<>();
        if(rebaseline||baseline.isEmpty()||ship!=shipId){
            if(bounds.sizeX()>MAX_VOLUME||bounds.sizeY()>MAX_VOLUME||bounds.sizeZ()>MAX_VOLUME)return unavailable("SCAN LIMIT");
            long volume=(long)bounds.sizeX()*bounds.sizeY()*bounds.sizeZ();
            if(!bounds.available()||volume<=0||volume>MAX_VOLUME)return unavailable("SCAN LIMIT / NO BOUNDS");
            for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++)
                if(!level.hasChunkAt(new BlockPos(x<<4,bounds.minY(),z<<4)))return unavailable("CHUNKS UNLOADED");
            BlockPos.MutableBlockPos cursor=new BlockPos.MutableBlockPos();
            for(int x=bounds.minX();x<=bounds.maxX();x++)for(int z=bounds.minZ();z<=bounds.maxZ();z++)for(int y=bounds.minY();y<=bounds.maxY();y++){
                cursor.set(x,y,z);if(structural(level.getBlockState(cursor)))present.add(cursor.asLong());
                if(present.size()>MAX_BLOCKS)return unavailable("HULL LIMIT");
            }
            if(present.isEmpty())return unavailable("NO STRUCTURE");
            capture(ship,present);
            for(long packed:baseline){BlockPos pos=BlockPos.of(packed);int hp=F35BlockHealthEvents.maximum(level,pos,level.getBlockState(pos));referenceHp.put(packed,hp<=0?1000:hp);}
        }else{
            for(long packed:baseline){BlockPos pos=BlockPos.of(packed);
                if(!level.hasChunkAt(pos))return unavailable("CHUNKS UNLOADED");
                if(structural(level.getBlockState(pos)))present.add(packed);
            }
        }
        Map<Long,Double> hp=new HashMap<>();for(long packed:present){BlockPos pos=BlockPos.of(packed);hp.put(packed,F35BlockHealthEvents.current(level,ship,pos,level.getBlockState(pos)));}
        return observe(ship,forward,anchor,present,hp,tick);
    }
    private F35ShipSilhouette unavailable(String status){cached=F35ShipSilhouette.empty(F35HullDamage.unknown(status));return cached;}
    public static boolean structural(net.minecraft.world.level.block.state.BlockState state){return !state.isAir()&&!state.liquid()&&!state.canBeReplaced();}
    /** Explicit reset only after a validated scan; callers must confirm intentional rebaselining. */
    public void capture(long ship,Collection<Long> blocks){
        if(blocks.isEmpty()||blocks.size()>MAX_BLOCKS)throw new IllegalArgumentException("Invalid reference hull");
        shipId=ship;baseline.clear();baseline.addAll(blocks);referenceHp.clear();blocks.forEach(p->referenceHp.put(p,20));previousMissing.clear();lastLossTick=-1;lastObservationTick=-1;
        revision++;
    }
    public F35ShipSilhouette observe(long ship,Direction forward,BlockPos anchor,Set<Long> present,long tick){
        Map<Long,Double> hp=new HashMap<>();present.forEach(p->hp.put(p,(double)referenceHp.getOrDefault(p,20)));
        return observe(ship,forward,anchor,present,hp,tick);
    }
    public F35ShipSilhouette observe(long ship,Direction forward,BlockPos anchor,Set<Long> present,Map<Long,Double> blockHp,long tick){
        if(baseline.isEmpty()||ship!=shipId)return unavailable("NO BASELINE");
        if(forward.getAxis()==Direction.Axis.Y)return unavailable("INVALID ORIENTATION");
        Set<Long> missing=new HashSet<>(baseline);missing.removeAll(present);
        if(!missing.equals(previousMissing)||lastObservationTick>tick)revision++;
        if(lastObservationTick>tick)lastLossTick=-1;
        if(missing.stream().anyMatch(p->!previousMissing.contains(p)))lastLossTick=tick;
        previousMissing.clear();previousMissing.addAll(missing);lastObservationTick=tick;
        int rx=-forward.getStepZ(),rz=forward.getStepX(),fx=forward.getStepX(),fz=forward.getStepZ();
        int minR=Integer.MAX_VALUE,maxR=Integer.MIN_VALUE,minF=Integer.MAX_VALUE,maxF=Integer.MIN_VALUE;
        for(long packed:baseline){BlockPos p=BlockPos.of(packed);int r=p.getX()*rx+p.getZ()*rz,f=p.getX()*fx+p.getZ()*fz;minR=Math.min(minR,r);maxR=Math.max(maxR,r);minF=Math.min(minF,f);maxF=Math.max(maxF,f);}
        int spanR=maxR-minR+1,spanF=maxF-minF+1;
        if(spanR<=0||spanF<=0||spanR>MAX_VOLUME||spanF>MAX_VOLUME)return unavailable("INVALID HULL EXTENT");
        int width=Math.min(42,spanR),height=Math.min(54,spanF);boolean[] occupied=new boolean[width*height];int[] expected=new int[occupied.length],loss=new int[occupied.length],perMille=new int[occupied.length];
        double[] maxHp=new double[occupied.length],remaining=new double[occupied.length];
        for(long packed:baseline){BlockPos p=BlockPos.of(packed);
            int x=grid(p.getX()*rx+p.getZ()*rz-minR,spanR,width),y=grid(maxF-(p.getX()*fx+p.getZ()*fz),spanF,height),index=y*width+x;
            occupied[index]=true;expected[index]++;if(missing.contains(packed))loss[index]++;
            int max=referenceHp.getOrDefault(packed,20);maxHp[index]+=max;double hp=blockHp.getOrDefault(packed,0.0);remaining[index]+=present.contains(packed)&&Double.isFinite(hp)?Math.max(0,Math.min(max,hp)):0;
        }
        for(int i=0;i<perMille.length;i++)perMille[i]=maxHp[i]==0?1000:(int)Math.floor(1000*remaining[i]/maxHp[i]+1e-9);
        int ar=anchor.getX()*rx+anchor.getZ()*rz,af=anchor.getX()*fx+anchor.getZ()*fz;
        int ax=ar<minR||ar>maxR?-1:grid(ar-minR,spanR,width),ay=af<minF||af>maxF?-1:grid(maxF-af,spanF,height);
        cached=new F35ShipSilhouette(width,height,ax,ay,spanR,spanF,occupied,
                new F35HullDamage(true,"TRACKING",baseline.size(),missing.size(),lastLossTick,tick,expected,loss,perMille));
        return cached;
    }
    private static int grid(int offset,int span,int cells){return Math.min(cells-1,Math.max(0,(int)((long)offset*cells/span)));}
    public CompoundTag save(){
        CompoundTag tag=new CompoundTag();tag.putInt("Version",1);tag.putLong("Ship",shipId);
        long[] ordered=baseline.stream().mapToLong(Long::longValue).sorted().toArray();tag.putLongArray("Baseline",ordered);
        tag.putIntArray("ReferenceHP",Arrays.stream(ordered).mapToInt(p->referenceHp.getOrDefault(p,20)).toArray());
        tag.putLongArray("Missing",previousMissing.stream().mapToLong(Long::longValue).sorted().toArray());
        tag.putLong("LastLoss",lastLossTick);tag.putLong("LastObserved",lastObservationTick);return tag;
    }
    public void load(CompoundTag tag){
        baseline.clear();referenceHp.clear();previousMissing.clear();cached=F35ShipSilhouette.empty();lastScanTick=-1;shipId=Long.MIN_VALUE;lastLossTick=-1;lastObservationTick=-1;
        long[] blocks=tag.getLongArray("Baseline");if(tag.getInt("Version")!=1||blocks.length==0||blocks.length>MAX_BLOCKS)return;
        shipId=tag.getLong("Ship");for(long p:blocks)baseline.add(p);
        int[] hp=tag.getIntArray("ReferenceHP");for(int i=0;i<blocks.length;i++)referenceHp.put(blocks[i],i<hp.length?Math.max(1,Math.min(1000,hp[i])):20);
        for(long p:tag.getLongArray("Missing"))if(baseline.contains(p))previousMissing.add(p);
        lastLossTick=tag.getLong("LastLoss");lastObservationTick=tag.getLong("LastObserved");
    }
}
