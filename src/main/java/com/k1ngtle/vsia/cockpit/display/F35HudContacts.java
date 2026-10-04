package com.k1ngtle.vsia.cockpit.display;

import com.k1ngtle.vsia.cockpit.client.F35TargetLockClient;
import com.k1ngtle.vsia.cockpit.detection.F35DetectionType;
import java.util.*;
import net.minecraft.world.phys.Vec3;

/** Only current server-supplied contacts; no entity enumeration, team guesses or coasted target extrapolation. */
public final class F35HudContacts {
    public static final int MAX_CUES=24;
    public record Contact(UUID id,String label,Vec3 position,boolean friendly,boolean locked,boolean radar) { }
    private F35HudContacts() { }
    public static boolean eligible(boolean seated,boolean firstPerson,boolean screenOpen,boolean hideGui,boolean validDrive){return seated&&firstPerson&&!screenOpen&&!hideGui&&validDrive;}
    public static boolean hasLiveLock(F35DisplayState state,F35TargetLockClient.LockKind kind,UUID id){
        if(state==null||!state.ownship().shipDetected()||id==null)return false;
        if(kind==F35TargetLockClient.LockKind.RADAR)return state.tracks().stream().anyMatch(t->id.equals(t.trackId())&&!"COASTING".equalsIgnoreCase(t.trackState())&&F35HudProjection.finite(t.position()));
        if(kind==F35TargetLockClient.LockKind.DETECTION)return state.detections().stream().anyMatch(t->id.equals(t.contactId())&&F35HudProjection.finite(t.position()));
        return false;
    }
    public static List<Contact> select(F35DisplayState state,F35DisplaySettings settings,F35TargetLockClient.LockKind kind,UUID lockedId) {
        if(state==null||settings==null||!state.ownship().shipDetected()||!F35HudProjection.finite(state.ownship().position()))return List.of();
        List<Contact> all=new ArrayList<>();
        for(var t:state.tracks()){
            if(t.trackId()==null||!F35HudProjection.finite(t.position())||"COASTING".equalsIgnoreCase(t.trackState()))continue;
            boolean friend=F35ContactSymbols.friendly(t.iffAuthenticated(),t.iffAffiliation());
            int bit=friend?F35DisplaySettings.FRIENDS:("HOSTILE".equalsIgnoreCase(t.iffAffiliation())?F35DisplaySettings.HOSTILES:F35DisplaySettings.UNKNOWNS);
            if(!settings.flag(bit))continue;
            all.add(new Contact(t.trackId(),t.shortId(),t.position(),friend,kind==F35TargetLockClient.LockKind.RADAR&&t.trackId().equals(lockedId),true));
        }
        for(var t:state.detections()){
            if(t.contactId()==null||t.type()==null)continue;
            boolean locked=kind==F35TargetLockClient.LockKind.DETECTION&&t.contactId().equals(lockedId);
            // Keep the forward HUD readable: ships/aircraft and the explicit selected contact, not every mob.
            if(t.type()!=F35DetectionType.SHIP&&!locked)continue;
            int typeBit=switch(t.type()){case SHIP->F35DisplaySettings.SHIPS;case PLAYER->F35DisplaySettings.PLAYERS;case MOB->F35DisplaySettings.MOBS;case MISSILE->F35DisplaySettings.MISSILES;};
            if(!settings.flag(typeBit)||!F35HudProjection.finite(t.position()))continue;
            boolean friend=F35ContactSymbols.friendly(t.iffAuthenticated(),t.iffStatus());
            if(!settings.flag(friend?F35DisplaySettings.FRIENDS:F35DisplaySettings.UNKNOWNS))continue;
            all.add(new Contact(t.contactId(),t.shortId(),t.position(),friend,locked,false));
        }
        all.sort(Comparator.comparingInt((Contact c)->c.locked()?0:c.friendly()?1:2).thenComparingDouble(c->c.position().distanceToSqr(state.ownship().position())).thenComparing(c->c.id().toString()));
        return List.copyOf(all.subList(0,Math.min(MAX_CUES,all.size())));
    }
}
