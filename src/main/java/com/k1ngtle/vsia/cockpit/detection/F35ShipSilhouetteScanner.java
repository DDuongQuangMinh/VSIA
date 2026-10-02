package com.k1ngtle.vsia.cockpit.detection;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlock;
import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.F35VsShipHelper;
import net.minecraft.server.level.ServerLevel;

public final class F35ShipSilhouetteScanner {
    private F35ShipSilhouetteScanner() { }
    public static F35ShipSilhouette scan(F35CockpitSeatBlockEntity cockpit){return scan(cockpit,false);}
    public static F35ShipSilhouette scan(F35CockpitSeatBlockEntity cockpit,boolean rebaseline){
        if(!(cockpit.getLevel() instanceof ServerLevel level))return F35ShipSilhouette.empty();
        var ship=F35VsShipHelper.shipSnapshot(cockpit);
        if(!ship.detected())return F35ShipSilhouette.empty(F35HullDamage.unknown("NO SHIP"));
        F35StructureMonitor monitor=cockpit.structureMonitor();long before=monitor.revision();
        F35ShipSilhouette result=monitor.scan(level,F35VsShipHelper.shipLocalBounds(cockpit),ship.shipId(),
                cockpit.getBlockState().getValue(F35CockpitSeatBlock.FACING).getOpposite(),cockpit.getBlockPos(),level.getGameTime(),rebaseline);
        if(before!=monitor.revision())cockpit.setChanged();
        return result;
    }
}
