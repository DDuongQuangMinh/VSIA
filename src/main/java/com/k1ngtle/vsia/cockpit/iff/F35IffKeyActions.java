package com.k1ngtle.vsia.cockpit.iff;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.network.F35IffPackets;
import java.time.Instant;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.level.ServerPlayer;

/** Shared GUI/command entry point. Caller must have validated the currently occupied cockpit. */
public final class F35IffKeyActions {
    private F35IffKeyActions() { }
    public static void perform(ServerPlayer player,F35CockpitSeatBlockEntity cockpit,int action,String value){
        F35IffMissions vault=F35IffMissions.get(player.server);F35IffConfig i=cockpit.iff();long now=Instant.now().getEpochSecond();
        try{
            switch(action){
                case 30,70 -> {F35IffMissions.Mission m=vault.create(player.getUUID(),value,now);vault.provision(m.id,player.getUUID(),i,i.activeSlot(),now);missionMessage(player,"Created "+m.name+"; share this public mission ID: ",m.id);}
                case 71,74 -> {UUID mission=requireMission(i);ServerPlayer other=player.server.getPlayerList().getPlayerByName(value);
                    if(other==null)throw new IllegalArgumentException("Player must be online; use their exact name");
                    if(action==71){vault.invite(mission,player.getUUID(),other.getUUID());missionMessage(other,"IFF invitation from "+player.getGameProfile().getName()+". Sit in your cockpit and load: ",mission);message(player,"Invitation sent. Their cockpit settings remain unchanged.");}
                    else{vault.revoke(mission,player.getUUID(),other.getUUID());message(player,"Access revoked; that player's provisioned keys are no longer usable.");message(other,"Your access to this IFF mission was revoked.");}}
                case 72 -> {String[] parts=value.split(":",2);if(parts.length!=2||!parts[1].matches("(?i)[AB]"))throw new IllegalArgumentException("Choose slot A or B");UUID mission=UUID.fromString(parts[0]);int slot=parts[1].equalsIgnoreCase("B")?1:0;
                    vault.provision(mission,player.getUUID(),i,slot,now);message(player,"Mission key loaded into slot "+(slot==0?"A":"B")+". Select that slot and NORM when ready.");}
                case 73 -> {UUID mission=requireMission(i);vault.rotate(mission,player.getUUID(),now);int spare=1-i.activeSlot();vault.provision(mission,player.getUUID(),i,spare,now);
                    for(F35IffMissions.Mission m:vault.list(player.getUUID()))if(m.id.equals(mission))for(ServerPlayer p:player.server.getPlayerList().getPlayers()){
                        try{vault.authorized(mission,p.getUUID());missionMessage(p,"IFF rotated. Load latest into spare slot; previous key expires within 120 seconds: ",mission);}catch(IllegalArgumentException ignored){}}
                    message(player,"New key staged in spare slot "+(spare==0?"A":"B")+"; active slot was not changed.");}
                case 75 -> {for(F35IffMissions.Mission m:vault.list(player.getUUID()))missionMessage(player,m.name+(m.owner.equals(player.getUUID())?" (OWNER) ":" "),m.id);}
                case 76 -> {UUID mission=requireMission(i);vault.retire(mission,player.getUUID());message(player,"Mission retired. All its generations are invalid; other cockpit settings were not changed.");}
                default -> {return;}
            }
            cockpit.sync();F35IffPackets.sendSnapshot(player,cockpit,now);
        }catch(IllegalArgumentException failure){message(player,"IFF: "+failure.getMessage());}
    }
    private static UUID requireMission(F35IffConfig i){UUID mission=i.activeKey().mission();if(mission==null)throw new IllegalArgumentException("Select a slot containing a shared mission key first");return mission;}
    private static void message(ServerPlayer p,String s){p.sendSystemMessage(Component.literal(s));}
    private static void missionMessage(ServerPlayer p,String prefix,UUID id){p.sendSystemMessage(Component.literal(prefix).append(Component.literal(id.toString()).withStyle(s->s.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD,id.toString())))));}
}
