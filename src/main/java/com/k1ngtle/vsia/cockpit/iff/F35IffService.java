package com.k1ngtle.vsia.cockpit.iff;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.F35VsShipHelper;
import com.k1ngtle.vsia.signality.radar.iff.IffAffiliation;
import com.k1ngtle.vsia.signality.radar.iff.IffReplyStatus;
import com.k1ngtle.vsia.signality.radar.iff.IffResult;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Vsia.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class F35IffService {
    private static final Set<F35CockpitSeatBlockEntity> LOADED=new HashSet<>();
    private F35IffService() { }
    public static void register(F35CockpitSeatBlockEntity cockpit){LOADED.add(cockpit);}
    public static void unregister(F35CockpitSeatBlockEntity cockpit){LOADED.remove(cockpit);}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){LOADED.clear();F35IffCrypto.clear();}
    public static UUID radarShipId(long id){return new UUID(5864145284199612416L,id);}
    public static List<F35CockpitSeatBlockEntity> matching(ServerLevel level,UUID target){
        List<F35CockpitSeatBlockEntity> out=new ArrayList<>();if(target==null)return out;
        for(F35CockpitSeatBlockEntity c:LOADED){if(c.isRemoved()||c.getLevel()!=level)continue;
            F35VsShipHelper.ShipSnapshot s=F35VsShipHelper.shipSnapshot(c);
            if(s.detected()&&(target.equals(radarShipId(s.shipId()))||target.equals(F35VsShipHelper.shipContactId(s.shipId()))))out.add(c);
        }return out;
    }
    public static F35CockpitSeatBlockEntity forShip(ServerLevel level,long ship){List<F35CockpitSeatBlockEntity> list=matching(level,radarShipId(ship)).stream().filter(c->operating(c.iff())).toList();return list.size()==1?list.get(0):null;}
    public static IffResult interrogate(F35CockpitSeatBlockEntity source,UUID targetId){
        if(source==null||!(source.getLevel() instanceof ServerLevel level))return IffResult.unknown(IffReplyStatus.NO_REPLY);
        List<F35CockpitSeatBlockEntity> matches=matching(level,targetId);
        if(matches.isEmpty())return IffResult.noTransponder();
        matches=matches.stream().filter(c->operating(c.iff())).toList();
        if(matches.isEmpty())return IffResult.unknown(IffReplyStatus.NO_REPLY);
        if(matches.size()!=1)return IffResult.unknown(IffReplyStatus.AUTH_FAILED);
        F35CockpitSeatBlockEntity target=matches.get(0);F35IffConfig own=source.iff(),remote=target.iff();
        if(!operating(own)||!operating(remote))return IffResult.unknown(IffReplyStatus.NO_REPLY);
        F35VsShipHelper.ShipSnapshot a=F35VsShipHelper.shipSnapshot(source),b=F35VsShipHelper.shipSnapshot(target);
        if(!a.detected()||!b.detected()||a.shipId()==b.shipId()||a.worldCenter().distanceTo(b.worldCenter())>200000)return IffResult.unknown(IffReplyStatus.NO_REPLY);
        try{
            UUID aircraft=radarShipId(b.shipId());
            byte[] payload=own.mode5Enabled()&&remote.mode5Enabled()?F35IffTelemetry.encode(target,aircraft):new byte[0];
            IffResult result=authenticate(source.cockpitId(),aircraft,target.cockpitId(),own,remote,F35IffMissions.get(level.getServer()),payload,a.worldCenter().distanceTo(b.worldCenter()));
            source.setChanged();return result;
        }catch(java.io.IOException failure){return IffResult.unknown(IffReplyStatus.AUTH_FAILED);}
    }
    /** Actual handshake path shared by radar, raw contacts and headless regression tests. */
    public static IffResult authenticate(UUID sourceId,UUID aircraft,UUID targetCockpit,F35IffConfig own,F35IffConfig remote,F35IffMissions vault,byte[] payload,double distance){
        if(!operating(own)||!operating(remote)||!Double.isFinite(distance)||distance<0||distance>200000)return IffResult.unknown(IffReplyStatus.NO_REPLY);
        int mode=own.mode5Enabled()&&remote.mode5Enabled()?5:(own.mode4Enabled()&&remote.mode4Enabled()?4:0);
        if(mode==0){
            String codes=(own.mode1Enabled()&&remote.mode1Enabled()?"M1 "+remote.mode1()+" ":"")+(own.mode2Enabled()&&remote.mode2Enabled()?"M2 "+remote.mode2():"");
            boolean m3=own.mode3aEnabled()&&remote.mode3aEnabled();
            return codes.isBlank()&&!m3?IffResult.unknown(IffReplyStatus.NO_REPLY):new IffResult(IffAffiliation.UNKNOWN,IffReplyStatus.CODE_REPLY,codes,m3?Integer.parseInt(remote.mode3a(),8):0,0,false,0,"");
        }
        long now=Instant.now().getEpochSecond();
        F35IffConfig.KeySlot responder=remote.activeKey();if(!vault.usable(responder,now))return IffResult.unknown(IffReplyStatus.AUTH_FAILED);
        F35IffConfig.KeySlot verifier=null;
        for(int j=0;j<2;j++){F35IffConfig.KeySlot k=own.key(j);if(vault.usable(k,now)&&k.id().equals(responder.id())&&responder.mission().equals(k.mission())&&k.generation()==responder.generation()){verifier=k;break;}}
        if(verifier==null)return IffResult.unknown(IffReplyStatus.AUTH_FAILED);
        try{
            F35IffCrypto.Challenge challenge=F35IffCrypto.challenge(sourceId,aircraft,verifier.id(),mode,own.nextSequence());
            String telemetry="M4 AUTH";boolean emergency=remote.master()==F35IffConfig.Master.EMER;
            int squawk=remote.mode3aEnabled()?Integer.parseInt(remote.mode3a(),8):0;
            if(mode==5){byte[] response=F35IffCrypto.mode5Encrypt(responder,challenge,aircraft,payload);
                byte[] clear=F35IffCrypto.verifyMode5(verifier,challenge,aircraft,response);
                F35IffTelemetry.Verified verified=F35IffTelemetry.decode(clear,aircraft,targetCockpit,verifier.mission());
                telemetry=verified.summary();emergency=verified.emergency();squawk=verified.squawk().isEmpty()?0:Integer.parseInt(verified.squawk(),8);
            }else if(!F35IffCrypto.verifyMode4(verifier,challenge,aircraft,F35IffCrypto.mode4(responder,challenge,aircraft)))throw new GeneralSecurityException("Invalid proof");
            return new IffResult(emergency?IffAffiliation.FRIENDLY_EMERGENCY:IffAffiliation.FRIENDLY,IffReplyStatus.AUTHENTICATED,
                    "F35-"+targetCockpit.toString().substring(0,8).toUpperCase(),squawk,0,true,
                    distance*2/299792458.0*1e6+3,telemetry);
        }catch(GeneralSecurityException|java.io.IOException|IllegalArgumentException|IllegalStateException failure){return IffResult.unknown(IffReplyStatus.AUTH_FAILED);}
    }
    public static boolean operating(F35IffConfig i){return i.master()==F35IffConfig.Master.NORM||i.master()==F35IffConfig.Master.EMER;}
}
