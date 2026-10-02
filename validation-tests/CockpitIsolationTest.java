import com.k1ngtle.vsia.cockpit.display.F35DisplaySettings;
import com.k1ngtle.vsia.cockpit.display.F35ClientDetectionCache;
import com.k1ngtle.vsia.cockpit.detection.F35ShipSilhouette;
import com.k1ngtle.vsia.cockpit.client.F35TargetLockClient;
import com.k1ngtle.vsia.cockpit.iff.F35IffConfig;
import com.k1ngtle.vsia.cockpit.iff.F35IffClientState;
import com.k1ngtle.vsia.cockpit.network.C2SF35DisplayActionPacket;
import com.k1ngtle.vsia.cockpit.network.C2SF35IffActionPacket;
import com.k1ngtle.vsia.cockpit.network.C2SF35IffRequestPacket;
import com.k1ngtle.vsia.cockpit.network.S2CF35IffSnapshotPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Regression checks for cockpit isolation and transport identity without launching Minecraft. */
public final class CockpitIsolationTest {
    private static int checks;
    private static void check(boolean valid,String label){if(!valid)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
    public static void main(String[] args){
        UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        F35DisplaySettings da=new F35DisplaySettings(),db=new F35DisplaySettings();
        da.apply(20);da.apply(22);da.apply(F35DisplaySettings.MOBS);
        check(da.brightnessPercent()==85&&db.brightnessPercent()==100,"A brightness leaves B unchanged");
        check(da.radarRangeIndex()==0&&db.radarRangeIndex()==-1,"A radar range leaves B unchanged");
        check(!da.detectionFilter().mobs()&&db.detectionFilter().mobs(),"Detection filters belong to the cockpit");
        F35DisplaySettings reloaded=new F35DisplaySettings();reloaded.load(da.save());
        check(reloaded.brightnessPercent()==85&&!reloaded.detectionFilter().mobs(),"Cockpit settings survive NBT save/reload");
        reloaded.apply(27);
        check(da.brightnessPercent()==85&&db.brightnessPercent()==100,"Reset affects only its settings instance");
        F35IffConfig ia=new F35IffConfig(),ib=new F35IffConfig();
        ia.setCodes("42","1724","7700");ia.cycleMaster();ia.generateKey(0,"ALPHA",3600);
        ib.setCodes("31","2001","1200");ib.generateKey(0,"BRAVO",3600);
        check(ia.mode1().equals("42")&&ib.mode1().equals("31")&&ia.master()!=ib.master(),"IFF codes and master state remain independent");
        byte[] before=ia.activeKey().secret().clone();ib.zeroize();
        check(Arrays.equals(before,ia.activeKey().secret())&&ib.activeKey().secret().length==0,"Zeroizing B leaves A's mission key intact");
        F35IffConfig iffReloaded=new F35IffConfig();iffReloaded.load(ia.save());
        check(Arrays.equals(before,iffReloaded.activeKey().secret())&&iffReloaded.mode3a().equals("7700"),"Existing cockpit IFF data survives save/reload");
        F35IffClientState.clearAll();
        var sa=new F35IffClientState.Snapshot("NORM","42","1724","7700",31,0,"ALPHA",1,15,"KEY LOADED");
        var sb=new F35IffClientState.Snapshot("STBY","31","2001","1200",31,1,"BRAVO",2,15,"KEY LOADED");
        F35IffClientState.accept(a,sa);check(F35IffClientState.get(b).status().equals("WAIT"),"Opening B cannot display A's IFF snapshot");
        F35IffClientState.accept(b,sb);F35IffClientState.accept(a,sa);
        check(F35IffClientState.get(b).keyId().equals("BRAVO"),"Delayed A reply cannot overwrite B");
        F35TargetLockClient.clearAll();UUID targetA=UUID.randomUUID(),targetB=UUID.randomUUID();
        F35TargetLockClient.bind(a);F35TargetLockClient.lockRadar(targetA);F35TargetLockClient.bind(b);
        check(!F35TargetLockClient.hasLock(),"A's target lock does not transfer to B");
        F35TargetLockClient.lockDetection(targetB);F35TargetLockClient.bind(a);
        check(F35TargetLockClient.isRadarLocked(targetA),"Returning to A restores A's target identity");
        F35TargetLockClient.clear();F35TargetLockClient.bind(b);
        check(F35TargetLockClient.isDetectionLocked(targetB),"Clearing A leaves B's target lock intact");
        F35TargetLockClient.validate(List.of(),List.of());
        check(F35TargetLockClient.targetCoasting(),"Missing contacts still enter COAST");
        F35ClientDetectionCache.clearAll();F35ClientDetectionCache.accept(a,9,List.of(),F35ShipSilhouette.empty());
        check(F35ClientDetectionCache.snapshot(b).serverTick()==0,"B cannot consume A's detection snapshot");
        F35ClientDetectionCache.accept(b,10,List.of(),F35ShipSilhouette.empty());F35ClientDetectionCache.accept(a,11,List.of(),F35ShipSilhouette.empty());
        check(F35ClientDetectionCache.snapshot(b).serverTick()==10,"Delayed detection data remains cockpit-scoped");
        FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.buffer());
        try{
            var display=new C2SF35DisplayActionPacket(a,20);display.toBytes(buffer);
            check(new C2SF35DisplayActionPacket(buffer).equals(display),"Display request preserves cockpit UUID on wire");buffer.clear();
            var action=new C2SF35IffActionPacket(b,31,"");action.toBytes(buffer);
            check(new C2SF35IffActionPacket(buffer).equals(action),"IFF action preserves cockpit UUID on wire");buffer.clear();
            var request=new C2SF35IffRequestPacket(a);request.toBytes(buffer);
            check(new C2SF35IffRequestPacket(buffer).equals(request),"IFF request preserves cockpit UUID on wire");buffer.clear();
            var snapshot=new S2CF35IffSnapshotPacket(b,"NORM","31","2001","1200",31,1,"BRAVO",123,15,"KEY LOADED");snapshot.toBytes(buffer);
            check(new S2CF35IffSnapshotPacket(buffer).equals(snapshot),"IFF snapshot preserves cockpit UUID on wire");
        }finally{buffer.release();}
        F35IffClientState.clearAll();F35TargetLockClient.clearAll();F35ClientDetectionCache.clearAll();
        check(F35IffClientState.get(a).status().equals("WAIT")&&!F35TargetLockClient.hasLock(),"Disconnect reset removes cached client state");
        System.out.println("ISOLATION REGRESSIONS PASSED: "+checks);
    }
}
