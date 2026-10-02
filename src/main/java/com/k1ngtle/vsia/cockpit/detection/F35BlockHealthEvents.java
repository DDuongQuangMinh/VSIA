package com.k1ngtle.vsia.cockpit.detection;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.iff.F35IffService;
import com.k1ngtle.vsia.signality.integration.vs.VsRuntimeCompat;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** New gameplay damage model, confined to loaded VS hulls carrying an F-35 cockpit. */
@Mod.EventBusSubscriber(modid=Vsia.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class F35BlockHealthEvents {
    private static final Map<AbstractArrow,Impact> IMPACTS=new WeakHashMap<>();
    private record Impact(long pos,long tick) { }
    private F35BlockHealthEvents() { }
    public static int maximum(ServerLevel level,BlockPos pos,BlockState state){return F35BlockHealthStore.maximum(state.getDestroySpeed(level,pos),state.getBlock().getExplosionResistance());}
    public static double current(ServerLevel level,long ship,BlockPos pos,BlockState state){
        if(!F35StructureMonitor.structural(state)){F35BlockHealthStore.get(level).reset(ship,pos.asLong());return 0;}
        int max=maximum(level,pos,state);if(max==0)return 1000; // unbreakable, immune to this model
        return F35BlockHealthStore.get(level).current(ship,pos.asLong(),blockId(state),max);
    }
    private static String blockId(BlockState state){return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();}
    private static Long ship(ServerLevel level,BlockPos pos){
        Object ship=VsRuntimeCompat.findShipManagingPos(level,pos);return ship==null?null:VsRuntimeCompat.shipId(ship);
    }
    private static List<F35CockpitSeatBlockEntity> cockpits(ServerLevel level,long ship){return F35IffService.matching(level,F35IffService.radarShipId(ship));}
    private static boolean permitted(ServerLevel level,Entity owner,BlockPos pos){
        if(owner instanceof Player p)return p.getAbilities().mayBuild&&!p.isSpectator()&&level.mayInteract(p,pos);
        return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void arrow(ProjectileImpactEvent event){
        if(!(event.getProjectile() instanceof AbstractArrow arrow)||!(arrow.level() instanceof ServerLevel level)||
                !(event.getRayTraceResult() instanceof BlockHitResult hit)||!event.getImpactResult().name().equals("DEFAULT"))return;
        BlockPos pos=hit.getBlockPos();if(!permitted(level,arrow.getOwner(),pos))return;
        Long ship=ship(level,pos);if(ship==null)return;var seats=cockpits(level,ship);if(seats.isEmpty())return;
        long now=level.getGameTime();Impact old=IMPACTS.get(arrow);if(old!=null&&old.pos()==pos.asLong()&&now-old.tick()<20)return;
        BlockState state=level.getBlockState(pos);if(!F35StructureMonitor.structural(state))return;
        int max=maximum(level,pos,state);if(max<=0)return;
        double damage=F35BlockHealthStore.arrowDamage(arrow.getBaseDamage(),arrow.getDeltaMovement().length());
        if(damage<=0)return;IMPACTS.put(arrow,new Impact(pos.asLong(),now));
        seats.forEach(F35ShipSilhouetteScanner::scan); // record the intact reference before the first destructive hit
        var store=F35BlockHealthStore.get(level);String id=blockId(state);double before=store.current(ship,pos.asLong(),id,max);
        double hp=store.hit(ship,pos.asLong(),id,max,damage);
        if(hp<=0){
            boolean canceled=arrow.getOwner() instanceof Player p&&net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,state,p));
            if(!canceled&&level.destroyBlock(pos,true,arrow))store.reset(ship,pos.asLong());else store.restore(ship,pos.asLong(),id,max,before);
        }
        seats.forEach(c->c.structureMonitor().invalidateScan());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void explosion(ExplosionEvent.Detonate event){
        if(!(event.getLevel() instanceof ServerLevel level)||!event.getExplosion().interactsWithBlocks())return;
        var store=F35BlockHealthStore.get(level);Map<Long,List<F35CockpitSeatBlockEntity>> hulls=new HashMap<>();
        var iterator=event.getAffectedBlocks().iterator();
        while(iterator.hasNext()){
            BlockPos pos=iterator.next();Long ship=ship(level,pos);if(ship==null)continue;
            var seats=hulls.get(ship);if(seats==null){seats=cockpits(level,ship);hulls.put(ship,seats);seats.forEach(F35ShipSilhouetteScanner::scan);}
            if(seats.isEmpty())continue;BlockState state=level.getBlockState(pos);if(!F35StructureMonitor.structural(state))continue;
            int max=maximum(level,pos,state);if(max<=0){iterator.remove();continue;}
            Object hull=VsRuntimeCompat.findShipManagingPos(level,pos);
            var world=new com.k1ngtle.vsia.signality.integration.vs.VsHookImpl().transformShipToWorld(hull,net.minecraft.world.phys.Vec3.atCenterOf(pos));
            double hp=store.hit(ship,pos.asLong(),blockId(state),max,F35BlockHealthStore.explosionDamage(world.distanceTo(event.getExplosion().getPosition())));
            if(hp>0)iterator.remove(); // surviving blocks are removed from vanilla's destruction list
        }
        hulls.values().forEach(seats->seats.forEach(c->c.structureMonitor().invalidateScan()));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void placed(BlockEvent.EntityPlaceEvent event){reset(event);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void broken(BlockEvent.BreakEvent event){reset(event);}
    private static void reset(BlockEvent event){
        if(!(event.getLevel() instanceof ServerLevel level))return;Long ship=ship(level,event.getPos());
        if(ship!=null)F35BlockHealthStore.get(level).reset(ship,event.getPos().asLong());
    }
    public static String inspect(ServerLevel level,BlockPos pos){
        Long ship=ship(level,pos);if(ship==null||cockpits(level,ship).isEmpty())return "Not a monitored F-35 hull block.";
        BlockState state=level.getBlockState(pos);if(!F35StructureMonitor.structural(state))return "No structural block at the sighted position.";
        int max=maximum(level,pos,state);if(max<=0)return blockId(state)+" UNBREAKABLE";
        double hp=current(level,ship,pos,state);
        return blockId(state)+" HP "+String.format(java.util.Locale.ROOT,"%.1f/%d (%.1f%%)",hp,max,100.0*hp/max);
    }
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){IMPACTS.clear();}
}
