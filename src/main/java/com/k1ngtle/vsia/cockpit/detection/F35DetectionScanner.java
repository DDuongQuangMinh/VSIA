package com.k1ngtle.vsia.cockpit.detection;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.F35VsShipHelper;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

public final class F35DetectionScanner {
    private F35DetectionScanner() { }
    public static ScanResult scan(ServerPlayer viewer,F35CockpitSeatBlockEntity cockpit,F35DetectionFilter filter) {
        return scan(cockpit,filter,F35LiveContactCatalog.capture(viewer.serverLevel(),viewer));
    }
    public static ScanResult scan(F35CockpitSeatBlockEntity cockpit,F35DetectionFilter filter,F35LiveContactCatalog catalog) {
        var own=F35VsShipHelper.shipSnapshot(cockpit);
        if(!own.detected())return new ScanResult(List.of(),F35ShipSilhouette.empty());
        List<F35DetectionContact> contacts=catalog.targets().values().stream().distinct()
                .filter(t->t.type()!=null&&!t.own(own.shipId())&&F35ContactEligibility.allowed(t.type(),filter))
                .filter(t->t.position().distanceToSqr(own.worldCenter())<=80_000.0*80_000.0)
                .sorted(Comparator.comparingDouble(t->t.position().distanceToSqr(own.worldCenter())))
                .limit(256).map(F35LiveContactCatalog.Target::raw).toList();
        return new ScanResult(contacts,F35ShipSilhouetteScanner.scan(cockpit));
    }
    public record ScanResult(List<F35DetectionContact> contacts,F35ShipSilhouette silhouette) {
        public ScanResult {contacts=List.copyOf(contacts);}
    }
}
