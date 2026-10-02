package com.k1ngtle.vsia.cockpit.detection;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Actual sparse, server-persisted HP shared by cockpits on one physical airframe. */
public final class F35BlockHealthStore extends SavedData {
    private static final int MAX_ENTRIES=200000;
    private final Map<Key,Health> values=new HashMap<>();
    private record Key(long ship,long pos) { }
    private record Health(String block,int maximum,double current) { }
    public static F35BlockHealthStore get(ServerLevel level){return level.getDataStorage().computeIfAbsent(F35BlockHealthStore::load,F35BlockHealthStore::new,"vsia_f35_block_health");}
    public static int maximum(double hardness,double resistance){
        if(!Double.isFinite(hardness)||!Double.isFinite(resistance)||hardness<0)return 0;
        return (int)Math.ceil(Math.min(1000,Math.max(20,20+Math.max(0,hardness)*20+Math.min(100,Math.max(0,resistance))*2)));
    }
    public static double arrowDamage(double base,double speed){return Double.isFinite(base)&&Double.isFinite(speed)?Math.max(0,Math.min(1000,base*speed)):0;}
    public static double explosionDamage(double distance){return Double.isFinite(distance)&&distance>=0?80.0/(1.0+distance*0.25):0;}
    public double current(long ship,long pos,String block,int maximum){
        Health h=values.get(new Key(ship,pos));if(h==null)return maximum;
        if(!h.block().equals(block)||h.maximum()!=maximum){reset(ship,pos);return maximum;}
        return h.current();
    }
    public double hit(long ship,long pos,String block,int maximum,double damage){
        if(maximum<=0||maximum>1000)return maximum;
        if(!Double.isFinite(damage)||damage<=0)return current(ship,pos,block,maximum);
        double old=current(ship,pos,block,maximum),next=Math.max(0,old-damage);Key key=new Key(ship,pos);
        if(!values.containsKey(key)&&values.size()>=MAX_ENTRIES)return old;
        values.put(key,new Health(block,maximum,next));setDirty();return next;
    }
    public void restore(long ship,long pos,String block,int maximum,double hp){
        if(!Double.isFinite(hp)||maximum<=0)return;
        if(hp>=maximum)reset(ship,pos);else{values.put(new Key(ship,pos),new Health(block,maximum,Math.max(0,hp)));setDirty();}
    }
    public void reset(long ship,long pos){if(values.remove(new Key(ship,pos))!=null)setDirty();}
    public static F35BlockHealthStore load(CompoundTag tag){
        F35BlockHealthStore store=new F35BlockHealthStore();ListTag list=tag.getList("Health",Tag.TAG_COMPOUND);
        if(list.size()>MAX_ENTRIES)throw new IllegalArgumentException("F-35 health save exceeds capacity");
        for(int i=0;i<list.size();i++){CompoundTag row=list.getCompound(i);int max=row.getInt("Max");double hp=row.getDouble("HP");String block=row.getString("Block");
            if(max>0&&max<=1000&&Double.isFinite(hp)&&hp>=0&&hp<max&&!block.isBlank()&&block.length()<=128)
                store.values.put(new Key(row.getLong("Ship"),row.getLong("Pos")),new Health(block,max,hp));
        }return store;
    }
    @Override public CompoundTag save(CompoundTag tag){
        ListTag list=new ListTag();values.forEach((key,h)->{CompoundTag row=new CompoundTag();row.putLong("Ship",key.ship());row.putLong("Pos",key.pos());row.putString("Block",h.block());row.putInt("Max",h.maximum());row.putDouble("HP",h.current());list.add(row);});tag.put("Health",list);return tag;
    }
}
