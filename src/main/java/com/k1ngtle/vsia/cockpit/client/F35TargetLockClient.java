package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.cockpit.detection.F35DetectionContact;
import com.k1ngtle.vsia.cockpit.display.F35RadarTrackView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class F35TargetLockClient {
    private static final long TARGET_LOST_GRACE_MILLIS = 5_000L;
    public enum LockKind { NONE, RADAR, DETECTION }
    private static final Map<UUID, LockState> LOCKS = new HashMap<>();
    private static final LockState EMPTY = new LockState();
    private static UUID cockpitId;
    private static final class LockState {
        LockKind kind=LockKind.NONE;
        UUID targetId;
        F35RadarTrackView lastRadarTrack;
        F35DetectionContact lastDetection;
        long missingSinceMillis=-1L;
        boolean targetVisible;
    }
    private F35TargetLockClient(){ }
    public static void bind(UUID id){cockpitId=id;}
    private static LockState state(){return cockpitId==null?EMPTY:LOCKS.computeIfAbsent(cockpitId,id->new LockState());}
    public static void clearAll(){LOCKS.clear();cockpitId=null;clear();}
    public static LockKind kind(){return state().kind;}
    @Nullable public static UUID targetId(){return state().targetId;}
    public static boolean hasLock(){return state().kind!=LockKind.NONE && state().targetId!=null;}
    public static void lockRadar(UUID id){if(id==null){clear();return;}setLock(LockKind.RADAR,id);}
    public static void lockDetection(UUID id){if(id==null){clear();return;}setLock(LockKind.DETECTION,id);}
    private static void setLock(LockKind kind,UUID id){
        if(cockpitId==null)return;
        LockState s=state();s.kind=kind;s.targetId=id;s.lastRadarTrack=null;s.lastDetection=null;s.missingSinceMillis=-1;s.targetVisible=true;
    }
    public static void toggleRadar(UUID id){if(isRadarLocked(id))clear();else lockRadar(id);}
    public static void toggleDetection(UUID id){if(isDetectionLocked(id))clear();else lockDetection(id);}
    public static void clear(){
        reset(state());
    }
    private static void reset(LockState s){s.kind=LockKind.NONE;s.targetId=null;s.lastRadarTrack=null;s.lastDetection=null;s.missingSinceMillis=-1;s.targetVisible=false;}
    public static void invalidate(UUID cockpit,List<UUID> radar,List<UUID> detection){
        LockState s=LOCKS.get(cockpit);if(s==null)return;
        if((s.kind==LockKind.RADAR&&radar.contains(s.targetId))||(s.kind==LockKind.DETECTION&&detection.contains(s.targetId)))reset(s);
    }
    public static boolean isRadarLocked(UUID id){return state().kind==LockKind.RADAR&&state().targetId!=null&&state().targetId.equals(id);}
    public static boolean isDetectionLocked(UUID id){return state().kind==LockKind.DETECTION&&state().targetId!=null&&state().targetId.equals(id);}
    @Nullable public static F35RadarTrackView lockedRadarTrack(List<F35RadarTrackView> tracks){
        if(state().kind!=LockKind.RADAR||state().targetId==null)return null;
        F35RadarTrackView found=findRadarTrack(tracks);if(found!=null)return found;F35RadarTrackView old=state().lastRadarTrack;
        return old==null?null:new F35RadarTrackView(old.trackId(),"COASTING",old.position(),old.velocity(),old.quality(),old.uncertaintyMeters(),old.bestSnrLinear(),old.sensorCount(),old.hits(),"UNKNOWN","NO_REPLY","",0,false,"");
    }
    @Nullable public static F35DetectionContact lockedDetection(List<F35DetectionContact> contacts){
        if(state().kind!=LockKind.DETECTION||state().targetId==null)return null;
        F35DetectionContact found=findDetection(contacts);if(found!=null)return found;F35DetectionContact old=state().lastDetection;
        return old==null?null:new F35DetectionContact(old.contactId(),old.type(),old.label(),old.position(),old.velocity(),false,"UNKNOWN/NO_REPLY","");
    }
    public static void validate(List<F35RadarTrackView> tracks,List<F35DetectionContact> contacts){
        if(!hasLock())return;
        LockState s=state();boolean found=s.kind==LockKind.RADAR?findRadarTrack(tracks)!=null:findDetection(contacts)!=null;
        if(found){s.targetVisible=true;s.missingSinceMillis=-1;return;}
        s.targetVisible=false;long now=System.currentTimeMillis();
        if(s.missingSinceMillis<0){s.missingSinceMillis=now;return;}
        if(now-s.missingSinceMillis>=TARGET_LOST_GRACE_MILLIS)clear();
    }
    public static boolean targetVisible(){return hasLock()&&state().targetVisible;}
    public static boolean targetCoasting(){return hasLock()&&!state().targetVisible;}
    @Nullable private static F35RadarTrackView findRadarTrack(List<F35RadarTrackView> tracks){
        if(state().kind!=LockKind.RADAR||state().targetId==null)return null;
        for(F35RadarTrackView track:tracks)if(state().targetId.equals(track.trackId())){state().lastRadarTrack=track;return track;}
        return null;
    }
    @Nullable private static F35DetectionContact findDetection(List<F35DetectionContact> contacts){
        if(state().kind!=LockKind.DETECTION||state().targetId==null)return null;
        for(F35DetectionContact contact:contacts)if(state().targetId.equals(contact.contactId())){state().lastDetection=contact;return contact;}
        return null;
    }
    public static String summary(List<F35RadarTrackView> tracks,List<F35DetectionContact> contacts){
        F35RadarTrackView radar=lockedRadarTrack(tracks);
        if(radar!=null){return "RADAR "+radar.shortId()+" "+com.k1ngtle.vsia.cockpit.display.F35ContactSymbols.label(radar.iffAuthenticated(),radar.iffAffiliation());}
        F35DetectionContact detection=lockedDetection(contacts);
        return detection==null?"NONE":detection.type().name()+" "+detection.shortId()+" "+com.k1ngtle.vsia.cockpit.display.F35ContactSymbols.label(detection.iffAuthenticated(),detection.iffStatus());
    }
}
