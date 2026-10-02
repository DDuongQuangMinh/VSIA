import com.k1ngtle.vsia.cockpit.iff.*;
import com.k1ngtle.vsia.cockpit.F35VsShipHelper;
import com.k1ngtle.vsia.cockpit.client.F35TargetLockClient;
import com.k1ngtle.vsia.cockpit.detection.*;
import com.k1ngtle.vsia.cockpit.display.*;
import com.k1ngtle.vsia.cockpit.network.*;
import com.k1ngtle.vsia.signality.radar.iff.*;
import com.k1ngtle.vsia.signality.radar.network.*;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.*;

/** Exercises the actual policy/cryptography path used by the server radar adapter. */
public final class IffAuthenticationTest {
    private static int checks;
    private static void check(boolean valid,String label){if(!valid)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
    @FunctionalInterface private interface Attempt{void run()throws Exception;}
    private static void rejects(Attempt action,String label)throws Exception{boolean failed=false;try{action.run();}catch(GeneralSecurityException|IllegalArgumentException|java.io.IOException e){failed=true;}check(failed,label);}
    private static F35IffConfig norm(){F35IffConfig c=new F35IffConfig();c.cycleMaster();return c;}
    private static byte[] telemetry(F35IffConfig config,UUID aircraft,UUID cockpit)throws Exception{return F35IffTelemetry.encode(config,aircraft,cockpit,new F35VsShipHelper.ShipSnapshot(true,7,"TEST",new Vec3(100,250,300),new Vec3(20,1,5),90,0,0),90);}
    public static void main(String[] args)throws Exception{
        long now=Instant.now().getEpochSecond();UUID owner=UUID.randomUUID(),ally=UUID.randomUUID(),stranger=UUID.randomUUID(),observer=UUID.randomUUID(),aircraft=F35IffService.radarShipId(7),targetCockpit=UUID.randomUUID();
        byte[] ikm=new byte[22];Arrays.fill(ikm,(byte)0x0b);
        byte[] okm=F35IffCrypto.hkdf(ikm,HexFormat.of().parseHex("000102030405060708090a0b0c"),HexFormat.of().parseHex("f0f1f2f3f4f5f6f7f8f9"),42);
        check(HexFormat.of().formatHex(okm).equals("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865"),"HKDF matches RFC 5869 test vector 1");
        F35IffMissions vault=new F35IffMissions();var mission=vault.create(owner,"ALPHA",now);F35IffConfig a=norm(),b=norm(),c=norm();
        vault.provision(mission.id,owner,a,0,now);
        rejects(()->vault.provision(mission.id,ally,b,0,now),"Public mission ID alone cannot load a key");
        vault.invite(mission.id,owner,ally);vault.provision(mission.id,ally,b,0,now);
        check(Arrays.equals(a.activeKey().secret(),b.activeKey().secret()),"Invited pilot receives matching server-owned key");
        check(c.activeKey().secret().length==0&&b.activeSlot()==0,"Provisioning does not configure another cockpit");
        byte[] exported=a.activeKey().secret();exported[0]^=1;check(!Arrays.equals(exported,a.activeKey().secret()),"Key getter returns defensive copy");
        rejects(()->vault.rotate(mission.id,ally,now),"Invited member cannot rotate owner's mission");
        byte[] data=telemetry(b,aircraft,targetCockpit);
        IffResult result=F35IffService.authenticate(observer,aircraft,targetCockpit,a,b,vault,data,1000);
        check(result.authenticated()&&result.affiliation()==IffAffiliation.FRIENDLY&&result.authenticatedTelemetry().contains("POS 100/250/300")&&result.authenticatedTelemetry().contains("SPD"),"Actual radar handshake produces FRIEND with authenticated telemetry");
        var unknown=F35IffService.authenticate(observer,aircraft,targetCockpit,c,b,vault,data,1000);
        check(!unknown.authenticated()&&unknown.affiliation()==IffAffiliation.UNKNOWN,"No shared key never becomes FRIEND");
        var key=a.activeKey();var challenge=F35IffCrypto.challenge(observer,aircraft,key.id(),4,a.nextSequence());byte[] proof=F35IffCrypto.mode4(b.activeKey(),challenge,aircraft);
        check(F35IffCrypto.verifyMode4(key,challenge,aircraft,proof),"Valid Mode-4-style HMAC accepted");
        check(!F35IffCrypto.verifyMode4(key,challenge,aircraft,proof),"Repeated response rejected");
        challenge=F35IffCrypto.challenge(observer,aircraft,key.id(),4,a.nextSequence());proof=F35IffCrypto.mode4(b.activeKey(),challenge,aircraft);
        var altered=new F35IffCrypto.Challenge(observer,aircraft,key.id(),4,challenge.nonce(),challenge.timestamp(),challenge.sequence()+1);
        check(!F35IffCrypto.verifyMode4(key,altered,aircraft,proof),"Sequence alteration rejected");
        check(F35IffCrypto.verifyMode4(key,challenge,aircraft,proof),"Altered challenge cannot consume genuine issued challenge");
        var issued=F35IffCrypto.challenge(observer,aircraft,key.id(),4,a.nextSequence());var stale=new F35IffCrypto.Challenge(observer,aircraft,key.id(),4,issued.nonce(),now-11,issued.sequence());
        check(!F35IffCrypto.verifyMode4(key,stale,aircraft,new byte[32]),"Stale timestamp rejected");
        var m5=F35IffCrypto.challenge(observer,aircraft,key.id(),5,a.nextSequence());byte[] encrypted=F35IffCrypto.mode5Encrypt(b.activeKey(),m5,aircraft,data);
        check(!Arrays.equals(encrypted,data),"Mode 5 payload is encrypted");
        check(Arrays.equals(F35IffCrypto.verifyMode5(key,m5,aircraft,encrypted),data),"AES-GCM authenticates/decrypts complete telemetry");
        rejects(()->F35IffCrypto.verifyMode5(key,m5,aircraft,encrypted),"Mode 5 replay rejected");
        var damaged=F35IffCrypto.challenge(observer,aircraft,key.id(),5,a.nextSequence());byte[] corrupt=F35IffCrypto.mode5Encrypt(key,damaged,aircraft,data);corrupt[0]^=1;
        rejects(()->F35IffCrypto.verifyMode5(key,damaged,aircraft,corrupt),"Tampered encrypted telemetry rejected");
        var wrongIdentity=F35IffCrypto.challenge(observer,aircraft,key.id(),5,a.nextSequence());byte[] bound=F35IffCrypto.mode5Encrypt(key,wrongIdentity,aircraft,data);
        rejects(()->F35IffCrypto.verifyMode5(key,wrongIdentity,UUID.randomUUID(),bound),"Response cannot be transplanted to another aircraft");
        F35IffConfig imposter=norm();imposter.generateKey(0,key.id(),3600);var badKeyChallenge=F35IffCrypto.challenge(observer,aircraft,key.id(),4,a.nextSequence());
        check(!F35IffCrypto.verifyMode4(key,badKeyChallenge,aircraft,F35IffCrypto.mode4(imposter.activeKey(),badKeyChallenge,aircraft)),"Same key ID with different secret fails authentication");
        rejects(()->F35IffTelemetry.decode(data,aircraft,UUID.randomUUID(),mission.id),"Payload cockpit identity is checked after decryption");
        b.toggleTelemetry(0);b.toggleTelemetry(1);b.toggleTelemetry(2);b.toggleTelemetry(3);
        byte[] hidden=telemetry(b,aircraft,targetCockpit);check(F35IffTelemetry.decode(hidden,aircraft,targetCockpit,mission.id).summary().equals("M5"),"Disabled telemetry fields are absent, not dummy zero fields");
        for(int j=0;j<4;j++)b.toggleTelemetry(j);
        b.cycleMaster();result=F35IffService.authenticate(observer,aircraft,targetCockpit,a,b,vault,telemetry(b,aircraft,targetCockpit),1000);
        check(result.authenticated()&&result.affiliation()==IffAffiliation.FRIENDLY_EMERGENCY&&result.squawkCode()==Integer.parseInt("7700",8),"EMER sends authenticated emergency flag and squawk 7700");
        b.cycleMaster();check(!F35IffService.authenticate(observer,aircraft,targetCockpit,a,b,vault,data,1000).authenticated(),"OFF transponder does not answer");
        b.cycleMaster();check(!F35IffService.authenticate(observer,aircraft,targetCockpit,a,b,vault,data,1000).authenticated(),"STBY transponder does not answer");b.cycleMaster();
        check(!F35IffService.authenticate(observer,aircraft,targetCockpit,a,b,vault,data,200001).authenticated(),"Out-of-range interrogation fails closed");
        a.toggleMode(5);b.toggleMode(5);result=F35IffService.authenticate(observer,aircraft,targetCockpit,a,b,vault,new byte[0],1000);
        check(result.authenticated()&&result.authenticatedTelemetry().equals("M4 AUTH"),"Common Mode 4 works when Mode 5 is deliberately disabled");
        a.toggleMode(4);b.toggleMode(4);result=F35IffService.authenticate(observer,aircraft,targetCockpit,a,b,vault,new byte[0],1000);
        check(!result.authenticated()&&result.affiliation()==IffAffiliation.UNKNOWN&&result.replyStatus()==IffReplyStatus.CODE_REPLY,"Modes 1/2/3A identify without declaring FRIEND");
        a.toggleMode(4);b.toggleMode(4);a.toggleMode(5);b.toggleMode(5);
        vault.rotate(mission.id,owner,now);vault.provision(mission.id,owner,a,1,now);
        check(a.activeSlot()==0&&a.key(1).generation()==2&&b.activeKey().generation()==1,"Rotation stages next generation without altering ally or active slot");
        check(vault.effectiveExpiry(b.activeKey())==now+120&&vault.usable(b.activeKey(),now+119)&&!vault.usable(b.activeKey(),now+120),"Previous key expires at bounded two-minute changeover");
        a.selectSlot(1);check(F35IffService.authenticate(observer,aircraft,targetCockpit,a,b,vault,data,1000).authenticated(),"Spare receive slot recognizes old generation during changeover");
        vault.provision(mission.id,ally,b,1,now);b.selectSlot(1);check(F35IffService.authenticate(observer,aircraft,targetCockpit,a,b,vault,data,1000).authenticated(),"Both pilots can switch to rotated key");
        CompoundTag saved=vault.save(new CompoundTag());F35IffMissions restored=F35IffMissions.load(saved);F35IffConfig reload=new F35IffConfig();reload.load(b.save());
        check(restored.usable(reload.activeKey(),now)&&reload.activeKey().mission().equals(mission.id),"Mission membership, generation and cockpit key survive NBT reload");
        check(!restored.usable(reload.activeKey(),now+21600),"Current generation expires after six hours");
        vault.revoke(mission.id,owner,ally);check(!F35IffService.authenticate(observer,aircraft,targetCockpit,a,b,vault,data,1000).authenticated(),"Revocation invalidates already-provisioned keys");
        byte[] ownerKey=a.activeKey().secret();b.zeroize();check(b.master()==F35IffConfig.Master.OFF&&b.key(0).secret().length==0&&b.key(1).secret().length==0&&Arrays.equals(ownerKey,a.activeKey().secret()),"Zeroize clears both local slots, not another cockpit");
        vault.retire(mission.id,owner);check(!vault.usable(a.activeKey(),now),"Retirement invalidates all mission generations");
        a.setCodes("73","7777","7500");check(a.mode1().equals("73")&&a.mode2().equals("7777"),"Public Mode 1/2 code ranges supported");
        F35RadarTrackView friendly=new F35RadarTrackView(UUID.randomUUID(),"CONFIRMED",Vec3.ZERO,Vec3.ZERO,1,1,10,1,3,"FRIENDLY","AUTHENTICATED","TEST",512,true,"M5 POS TEST");
        F35ClientRadarCache.clearAll();F35ClientRadarCache.accept(observer,"default",1,List.of(friendly));F35ClientRadarCache.accept(targetCockpit,"default",2,List.of());F35ClientRadarCache.accept(observer,"default",3,List.of(friendly));
        check(F35ClientRadarCache.snapshot(targetCockpit).tracks().isEmpty()&&F35ClientRadarCache.snapshot(targetCockpit).serverTick()==2,"Cockpit B cannot consume delayed cockpit A radar authentication");
        F35TargetLockClient.clearAll();F35TargetLockClient.bind(observer);F35TargetLockClient.lockRadar(friendly.trackId());F35TargetLockClient.validate(List.of(friendly),List.of());F35TargetLockClient.validate(List.of(),List.of());
        check(F35TargetLockClient.hasLock()&&F35TargetLockClient.targetCoasting()&&!F35TargetLockClient.lockedRadarTrack(List.of()).iffAuthenticated(),"COAST preserves lock but immediately removes old FRIEND proof");
        F35DetectionContact contact=new F35DetectionContact(UUID.randomUUID(),F35DetectionType.SHIP,"TEST",Vec3.ZERO,Vec3.ZERO,true,"FRIENDLY/AUTHENTICATED","M5");
        F35TargetLockClient.lockDetection(contact.contactId());F35TargetLockClient.validate(List.of(),List.of(contact));F35TargetLockClient.validate(List.of(),List.of());
        check(!F35TargetLockClient.lockedDetection(List.of()).iffAuthenticated(),"Raw contact COAST also removes authentication");
        FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.buffer());try{
            var detection=new S2CF35DetectionSnapshotPacket(observer,77,List.of(contact),F35ShipSilhouette.empty());detection.toBytes(buffer);byte[] original=io.netty.buffer.ByteBufUtil.getBytes(buffer,0,buffer.writerIndex());var decoded=new S2CF35DetectionSnapshotPacket(buffer);FriendlyByteBuf again=new FriendlyByteBuf(Unpooled.buffer());try{decoded.toBytes(again);check(Arrays.equals(original,io.netty.buffer.ByteBufUtil.getBytes(again,0,again.writerIndex())),"Detection packet round-trip carries scoped authentication");}finally{again.release();}buffer.clear();
            RadarNetworkTrack rt=new RadarNetworkTrack(friendly.trackId(),RadarTrackState.CONFIRMED,Vec3.ZERO,Vec3.ZERO,1,2,3,1,Set.of(observer),10,1,1,new IffResult(IffAffiliation.FRIENDLY,IffReplyStatus.AUTHENTICATED,"TEST",512,0,true,3,"M5"),aircraft);
            var radar=new S2CF35RadarSnapshotPacket(observer,"default",7,List.of(rt));radar.toBytes(buffer);int size=buffer.writerIndex();var decodedRadar=new S2CF35RadarSnapshotPacket(buffer);buffer.clear();decodedRadar.toBytes(buffer);check(buffer.writerIndex()==size,"Radar packet round-trip includes cockpit ID and telemetry");
        }finally{buffer.release();}
        RadarNetwork network=new RadarNetwork("test",new RadarNetworkConfig(0,0,0,1,30,200,512,4096,6));UUID sensor=UUID.randomUUID(),otherTarget=UUID.randomUUID();
        RadarMeasurement ma=new RadarMeasurement(sensor,aircraft,1,Vec3.ZERO,new Vec3(100,0,0),Vec3.ZERO,100,0,0,0,100,100,1,1,true,IffResult.noTransponder());
        RadarMeasurement mb=new RadarMeasurement(sensor,otherTarget,1,Vec3.ZERO,new Vec3(100.1,0,0),Vec3.ZERO,100.1,0,0,0,100,100,1,1,true,IffResult.noTransponder());
        network.accept(new RadarSensorReport("test",1,ma));network.accept(new RadarSensorReport("test",1,mb));network.tick(1);
        check(network.tracks(1).size()==2&&network.tracks(1).stream().anyMatch(t->aircraft.equals(t.sourceTargetId())),"Nearby different aircraft cannot merge or inherit target identity");
        F35IffCrypto.clear();System.out.println("IFF AUTHENTICATION REGRESSIONS PASSED: "+checks);
    }
}
