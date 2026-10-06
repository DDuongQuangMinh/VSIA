package com.k1ngtle.vsia.cockpit.detection;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.F35SeatController;
import com.k1ngtle.vsia.cockpit.F35VsShipHelper;
import com.k1ngtle.vsia.cockpit.iff.F35IffService;
import com.k1ngtle.vsia.signality.Signality;
import com.k1ngtle.vsia.signality.api.radar.IRadarTarget;
import com.k1ngtle.vsia.signality.api.radar.RadarRegistry;
import com.k1ngtle.vsia.signality.integration.vs.VsHookImpl;
import com.k1ngtle.vsia.signality.integration.vs.VsRuntimeCompat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Captured on the main server thread. Missing/removed/in-ground sources cannot generate GUI ghosts. */
public final class F35LiveContactCatalog {
    private static final VsHookImpl VS=new VsHookImpl();
    private final Map<UUID,Target> targets;
    public F35LiveContactCatalog(Map<UUID,Target> targets){this.targets=Map.copyOf(targets);}
    public Target get(UUID source){return source==null?null:targets.get(source);}
    /** Authenticate the host ship, not a radar beacon/provider's unrelated display UUID. */
    public UUID iffIdentity(UUID source){Target target=get(source);return target==null?source:target.iffIdentity();}
    public Map<UUID,Target> targets(){return targets;}
    public static F35LiveContactCatalog capture(ServerLevel level,ServerPlayer viewer) {
        Map<UUID,Target> result=new HashMap<>();
        for(Entity e:level.getAllEntities()) {
            F35DetectionType type=F35ContactEligibility.classify(e);if(type==null)continue;
            Vec3 position=e.position(),velocity=e.getDeltaMovement().scale(20);Long host=null;
            Object ship=VsRuntimeCompat.findShipManagingPos(level,e.blockPosition());
            if(ship!=null){host=VsRuntimeCompat.shipId(ship);position=VS.transformShipToWorld(ship,position);velocity=VS.transformDirShipToWorld(ship,velocity).add(VS.shipVelocity(ship));}
            if(e instanceof ServerPlayer rider){F35CockpitSeatBlockEntity seat=F35SeatController.cockpitFor(rider);if(seat!=null){var hull=F35VsShipHelper.shipSnapshot(seat);if(hull.detected())host=hull.shipId();}}
            boolean ownCrew=e==viewer||e.getRootVehicle()==viewer.getRootVehicle();
            if(finite(position)&&finite(velocity))result.put(e.getUUID(),new Target(e.getUUID(),type,e.getName().getString(),position,velocity,host,ownCrew));
        }
        for(var ship:F35VsShipHelper.loadedShips(level))if(ship.detected()&&finite(ship.worldCenter())&&finite(ship.velocity())){
            UUID raw=F35VsShipHelper.shipContactId(ship.shipId());Target target=new Target(raw,F35DetectionType.SHIP,ship.shipSlug(),ship.worldCenter(),ship.velocity(),ship.shipId(),false);
            result.put(raw,target);result.put(F35IffService.radarShipId(ship.shipId()),target);
        }
        // Preserve intentionally registered radar beacons/custom target providers.
        for(var provider:RadarRegistry.targetSources())try(Stream<IRadarTarget> stream=provider.apply(level)){
            if(stream!=null)stream.forEach(t->{if(t==null||t.level()!=level||!t.detectable()||result.containsKey(t.id()))return;
                if(t instanceof net.minecraft.world.level.block.entity.BlockEntity block&&
                        (!level.hasChunkAt(block.getBlockPos())||level.getBlockEntity(block.getBlockPos())!=block))return;
                // Explicit registration must not resurrect an embedded/dead/helper entity.
                Entity e=level.getEntity(t.id());if(e!=null&&F35ContactEligibility.classify(e)==null)return;
                Vec3 p=t.positionWorld(),v=t.velocityWorld();if(!finite(p)||!finite(v))return;
                Object ship=t.vsShip();Long host=ship==null?null:VsRuntimeCompat.shipId(ship);
                result.put(t.id(),new Target(t.id(),ship==null?null:F35DetectionType.SHIP,"RADAR TARGET",p,v,host,false));
            });
        }catch(RuntimeException failure){Signality.LOGGER.debug("F-35 live target provider unavailable",failure);}
        return new F35LiveContactCatalog(result);
    }
    private static boolean finite(Vec3 v){return Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z);}
    public record Target(UUID canonicalId,F35DetectionType type,String label,Vec3 position,Vec3 velocity,Long shipId,boolean ownCrew){
        public UUID iffIdentity(){return type==F35DetectionType.SHIP&&shipId!=null?F35VsShipHelper.shipContactId(shipId):canonicalId;}
        public boolean own(long ownShip){return ownCrew||(shipId!=null&&shipId==ownShip);}
        public F35DetectionContact raw(){return type==null?null:new F35DetectionContact(canonicalId,type,label,position,velocity);}
    }
}
