package com.k1ngtle.vsia.cockpit.network;

import com.k1ngtle.vsia.cockpit.detection.*;
import com.k1ngtle.vsia.signality.radar.network.RadarNetworkTrack;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.world.phys.Vec3;

/** One visible contact per live source. Internal fusion fragments are never separate aircraft. */
public final class F35RadarContactProjection {
    private F35RadarContactProjection() { }
    public static UUID displayId(UUID source){return UUID.nameUUIDFromBytes(("vsia:f35-radar:"+source).getBytes(StandardCharsets.UTF_8));}
    public static boolean forwardVisible(boolean fullCircle,double normalizedForward){return fullCircle||normalizedForward>=0.0;}
    public static boolean hasRawContact(UUID radarId,List<F35DetectionContact> contacts){return contacts.stream().anyMatch(c->displayId(c.contactId()).equals(radarId));}
    public static List<RadarNetworkTrack> project(List<RadarNetworkTrack> tracks,F35LiveContactCatalog catalog,F35DetectionFilter filter,long ownShip,Vec3 origin,long tick){
        Map<UUID,RadarNetworkTrack> best=new HashMap<>();
        for(RadarNetworkTrack track:tracks){var target=catalog.get(track.sourceTargetId());
            if(target==null||target.own(ownShip)||!F35ContactEligibility.allowed(target.type(),filter)||target.position().distanceToSqr(origin)>80_000.0*80_000.0||track.lastMeasurementTick()>tick||tick-track.lastMeasurementTick()>20)continue;
            RadarNetworkTrack old=best.get(target.canonicalId());
            if(old==null||track.lastMeasurementTick()>old.lastMeasurementTick()||(track.lastMeasurementTick()==old.lastMeasurementTick()&&track.quality()>old.quality()))best.put(target.canonicalId(),track);
        }
        return best.entrySet().stream().sorted(Comparator.comparingDouble(e->catalog.get(e.getValue().sourceTargetId()).position().distanceToSqr(origin))).limit(128).map(e->{
            RadarNetworkTrack t=e.getValue();return new RadarNetworkTrack(displayId(e.getKey()),t.state(),t.position(),t.velocity(),t.createdTick(),t.lastMeasurementTick(),t.hits(),t.sensorCount(),t.contributingSensors(),t.bestSnrLinear(),t.positionUncertaintyMeters(),t.quality(),t.iff(),t.sourceTargetId());
        }).toList();
    }
    /** Track terminal removals separately from a live aircraft temporarily losing radar coverage. */
    public static final class Memory {
        private final Map<UUID,Seen> radar=new HashMap<>(),detection=new HashMap<>();
        public Removals update(Map<UUID,UUID> sentRadar,Collection<F35DetectionContact> sentDetection,F35LiveContactCatalog catalog,long ownShip,long tick){
            List<UUID> removedRadar=retire(radar,catalog,ownShip,tick),removedDetection=retire(detection,catalog,ownShip,tick);
            sentRadar.forEach((id,source)->radar.put(id,new Seen(source,tick)));
            for(var contact:sentDetection)detection.put(contact.contactId(),new Seen(contact.contactId(),tick));
            return new Removals(removedRadar,removedDetection);
        }
        private static List<UUID> retire(Map<UUID,Seen> previous,F35LiveContactCatalog catalog,long ownShip,long tick){
            List<UUID> removed=new ArrayList<>();var iterator=previous.entrySet().iterator();
            while(iterator.hasNext()){var e=iterator.next();var target=catalog.get(e.getValue().source());
                if(target==null||target.own(ownShip)){removed.add(e.getKey());iterator.remove();}
                else if(tick-e.getValue().tick()>100)iterator.remove();
            }return List.copyOf(removed);
        }
        private record Seen(UUID source,long tick) { }
    }
    public record Removals(List<UUID> radar,List<UUID> detection) {
        public Removals {radar=List.copyOf(radar);detection=List.copyOf(detection);}
    }
}
