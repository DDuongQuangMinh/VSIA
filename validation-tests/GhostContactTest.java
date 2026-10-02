import com.k1ngtle.vsia.cockpit.detection.*;
import com.k1ngtle.vsia.cockpit.display.*;
import com.k1ngtle.vsia.cockpit.client.F35TargetLockClient;
import com.k1ngtle.vsia.cockpit.network.*;
import com.k1ngtle.vsia.signality.radar.network.*;
import com.k1ngtle.vsia.signality.radar.iff.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Headless regression of the actual eligibility/projection/removal/cache policy. */
public final class GhostContactTest {
    private static int checks;
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
    private static RadarNetworkTrack track(UUID source,long tick,double quality){
        return new RadarNetworkTrack(UUID.randomUUID(),RadarTrackState.CONFIRMED,new Vec3(20,0,0),Vec3.ZERO,1,tick,3,1,Set.of(),10,1,quality,IffResult.noTransponder(),source);
    }
    private static F35LiveContactCatalog.Target target(UUID id,F35DetectionType type,Long ship,boolean crew){
        return new F35LiveContactCatalog.Target(id,type,"TEST",new Vec3(20,0,0),Vec3.ZERO,ship,crew);
    }
    private static F35RadarTrackView view(UUID id){return new F35RadarTrackView(id,"CONFIRMED",Vec3.ZERO,Vec3.ZERO,1,1,1,1,1,"UNKNOWN","NO_REPLY","",0,false);}
    public static void main(String[] args){
        check(F35ContactEligibility.flying(true,false,false,false,1),"Live flying arrow is eligible");
        check(!F35ContactEligibility.flying(true,false,true,false,1),"Embedded arrow is excluded even while alive and moving");
        check(!F35ContactEligibility.flying(true,false,true,true,0),"Ground-embedded stationary arrow is excluded");
        check(!F35ContactEligibility.flying(false,false,false,false,1),"Dead projectile excluded");
        check(!F35ContactEligibility.flying(true,true,false,false,1),"Removed projectile excluded");
        check(!F35ContactEligibility.flying(true,false,false,true,0),"Stopped grounded projectile excluded");
        check(F35ContactEligibility.flying(true,false,false,false,0),"Momentarily stationary airborne projectile remains visible");
        check(!F35ContactEligibility.flying(true,false,false,false,Double.NaN),"Invalid projectile motion rejected");
        UUID arrow=UUID.randomUUID(),mob=UUID.randomUUID(),ownHull=UUID.randomUUID(),ownCrew=UUID.randomUUID(),other=UUID.randomUUID(),helper=UUID.randomUUID();
        var catalog=new F35LiveContactCatalog(Map.of(arrow,target(arrow,F35DetectionType.MISSILE,null,false),mob,target(mob,F35DetectionType.MOB,null,false),ownHull,target(ownHull,F35DetectionType.SHIP,7L,false),ownCrew,target(ownCrew,F35DetectionType.PLAYER,null,true),other,target(other,F35DetectionType.SHIP,8L,false)));
        var all=new F35DetectionFilter(true,true,true,true);
        List<RadarNetworkTrack> input=new ArrayList<>(List.of(track(helper,100,1),track(ownHull,100,1),track(ownCrew,100,1),track(arrow,99,0.9),track(arrow,100,0.1),track(mob,100,1),track(other,100,1)));
        var projected=F35RadarContactProjection.project(input,catalog,all,7,Vec3.ZERO,100);
        check(projected.size()==3,"Helpers, own hull and crew removed; one contact per real source");
        var arrowTrack=projected.stream().filter(t->arrow.equals(t.sourceTargetId())).findFirst().orElseThrow();
        check(arrowTrack.lastMeasurementTick()==100,"Freshest fusion fragment selected over higher-quality stale fragment");
        check(arrowTrack.trackId().equals(F35RadarContactProjection.displayId(arrow)),"Contact ID is stable source identity");
        var refreshed=F35RadarContactProjection.project(List.of(track(arrow,101,1)),catalog,all,7,Vec3.ZERO,101);
        check(refreshed.get(0).trackId().equals(arrowTrack.trackId()),"New fusion track UUID cannot duplicate/reset cockpit identity");
        check(F35RadarContactProjection.project(input,catalog,new F35DetectionFilter(false,true,true,false),7,Vec3.ZERO,100).size()==1,"Mobs and projectile filters apply to network tracks too");
        check(F35RadarContactProjection.project(List.of(track(arrow,79,1)),catalog,all,7,Vec3.ZERO,100).isEmpty(),"Old measurements are not displayed as fresh contacts");
        check(F35RadarContactProjection.project(List.of(track(arrow,101,1)),catalog,all,7,Vec3.ZERO,100).isEmpty(),"Future measurement rejected");
        check(F35RadarContactProjection.project(List.of(track(arrow,100,1)),new F35LiveContactCatalog(Map.of()),all,7,Vec3.ZERO,100).isEmpty(),"Impact/unload cannot survive as a live network contact");
        List<RadarNetworkTrack> crowded=new ArrayList<>();for(int i=0;i<200;i++)crowded.add(track(UUID.randomUUID(),100,1));crowded.add(track(arrow,100,1));
        check(F35RadarContactProjection.project(crowded,catalog,all,7,Vec3.ZERO,100).size()==1,"Filtering occurs before the 128-track cap");
        UUID shipRaw=UUID.randomUUID(),shipRadar=UUID.randomUUID();var aliasTarget=target(shipRaw,F35DetectionType.SHIP,8L,false);
        var aliases=new F35LiveContactCatalog(Map.of(shipRaw,aliasTarget,shipRadar,aliasTarget));
        var aliasProjection=F35RadarContactProjection.project(List.of(track(shipRaw,100,1),track(shipRadar,100,1)),aliases,all,7,Vec3.ZERO,100);
        check(aliasProjection.size()==1&&aliasProjection.get(0).trackId().equals(F35RadarContactProjection.displayId(shipRaw)),"Raw/radar airframe aliases yield one stable target");
        F35DetectionContact raw=catalog.get(arrow).raw();
        check(F35RadarContactProjection.hasRawContact(arrowTrack.trackId(),List.of(raw)),"Raw and radar measurements share a single rendered symbol");
        check(!F35RadarContactProjection.forwardVisible(false,-0.01),"Nearby rear contact clipped from forward TSD");
        check(F35RadarContactProjection.forwardVisible(true,-0.01),"Rear contacts remain valid on 360-degree HSI");
        check(F35RadarContactProjection.forwardVisible(false,0.0001),"Nearby forward contact retained");
        var memory=new F35RadarContactProjection.Memory();
        memory.update(Map.of(arrowTrack.trackId(),arrow),List.of(raw),catalog,7,100);
        var temporary=memory.update(Map.of(),List.of(),catalog,7,105);
        check(temporary.radar().isEmpty()&&temporary.detection().isEmpty(),"Live target's temporary sensor loss does not destroy persistent lock");
        var gone=memory.update(Map.of(),List.of(),new F35LiveContactCatalog(Map.of()),7,110);
        check(gone.radar().contains(arrowTrack.trackId())&&gone.detection().contains(arrow),"Terminal impact/unload explicitly removes both contact identities");
        check(memory.update(Map.of(),List.of(),catalog,7,115).radar().isEmpty(),"Consumed removal is not repeatedly emitted");
        UUID a=UUID.randomUUID(),b=UUID.randomUUID();F35TargetLockClient.clearAll();
        F35TargetLockClient.bind(a);F35TargetLockClient.lockRadar(arrowTrack.trackId());F35TargetLockClient.validate(List.of(view(arrowTrack.trackId())),List.of());
        F35TargetLockClient.bind(b);F35TargetLockClient.lockRadar(arrowTrack.trackId());F35TargetLockClient.invalidate(a,gone.radar(),gone.detection());
        check(F35TargetLockClient.hasLock(),"A impact notification does not clear cockpit B lock");
        F35TargetLockClient.bind(a);check(!F35TargetLockClient.hasLock(),"Confirmed removed radar target immediately clears cockpit A lock");
        F35TargetLockClient.lockDetection(arrow);F35TargetLockClient.invalidate(a,gone.radar(),gone.detection());
        check(!F35TargetLockClient.hasLock(),"Confirmed removed raw arrow immediately clears lock");
        F35TargetLockClient.lockRadar(other);F35TargetLockClient.validate(List.of(view(other)),List.of());F35TargetLockClient.validate(List.of(),List.of());
        check(F35TargetLockClient.hasLock()&&F35TargetLockClient.targetCoasting(),"Ordinary live aircraft lock retains its COAST grace");
        F35ClientRadarCache.clearAll();check(F35ClientRadarCache.acceptSnapshot(a,"default",110,List.of()),"Newest radar snapshot accepted");
        check(!F35ClientRadarCache.acceptSnapshot(a,"default",100,List.of(view(arrowTrack.trackId())))&&F35ClientRadarCache.snapshot(a).tracks().isEmpty(),"Delayed snapshot cannot restore removed arrow");
        F35ClientDetectionCache.clearAll();F35ClientDetectionCache.accept(a,100,List.of(raw),F35ShipSilhouette.empty());F35ClientDetectionCache.invalidate(a,110,List.of(arrow));F35ClientDetectionCache.accept(a,105,List.of(raw),F35ShipSilhouette.empty());
        check(F35ClientDetectionCache.snapshot(a).contacts().isEmpty(),"Delayed raw snapshot cannot restore impacted arrow");
        F35ClientDetectionCache.accept(b,100,List.of(raw),F35ShipSilhouette.empty());
        check(F35ClientDetectionCache.snapshot(b).contacts().size()==1,"Removal/cache ordering remains isolated per cockpit");
        FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.buffer());
        try{
            var packet=new S2CF35RadarSnapshotPacket(a,"default",110,List.of(),gone);packet.toBytes(buffer);int size=buffer.writerIndex();
            var decoded=new S2CF35RadarSnapshotPacket(buffer);
            check(decoded.removals().equals(gone),"Terminal radar/raw IDs survive network round-trip");
            check(buffer.readableBytes()==0,"Packet decoder consumes complete removal payload");
            buffer.clear();decoded.toBytes(buffer);check(buffer.writerIndex()==size,"Removal encoding round-trip has identical size");
            buffer.clear();buffer.writeUUID(a);buffer.writeUtf("default",64);buffer.writeLong(110);buffer.writeVarInt(129);
            boolean rejected=false;try{new S2CF35RadarSnapshotPacket(buffer);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"Oversized track count fails closed instead of desynchronizing packet");
        }finally{buffer.release();}
        F35TrackTrailCache.clear();
        F35TrackTrailCache.update(new F35DisplayState(null,"default",List.of(view(arrowTrack.trackId())),List.of(raw),F35ShipSilhouette.empty(),null,500,null,System.currentTimeMillis()));
        check(!F35TrackTrailCache.radarTrail(arrowTrack.trackId()).isEmpty()&&!F35TrackTrailCache.detectionTrail(arrow).isEmpty(),"Real trail samples exist before terminal removal");
        F35TrackTrailCache.forget(gone.radar(),gone.detection());
        check(F35TrackTrailCache.radarTrail(arrowTrack.trackId()).isEmpty()&&F35TrackTrailCache.detectionTrail(arrow).isEmpty(),"Removed contact has no retained drawable trail");
        System.out.println("GHOST CONTACT REGRESSIONS PASSED: "+checks);
    }
}
