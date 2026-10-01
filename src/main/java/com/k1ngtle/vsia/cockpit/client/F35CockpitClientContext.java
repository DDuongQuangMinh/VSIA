package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.F35SeatController;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

/** Resolves the synchronized cockpit block identity, including VS ship-local block positions. */
public final class F35CockpitClientContext {
    private F35CockpitClientContext(){ }
    public static F35CockpitSeatBlockEntity seated(){
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return null;
        Entity seat=mc.player.getVehicle();if(!F35SeatController.isSeatRenderMarker(seat))return null;
        BlockPos pos=F35SeatController.clientCockpitPos(seat);if(pos==null)return null;
        return mc.level.getBlockEntity(pos) instanceof F35CockpitSeatBlockEntity c ? c : null;
    }
    public static boolean bindSeat(UUID expected){
        F35CockpitSeatBlockEntity c=seated();if(c==null||expected==null||!expected.equals(c.cockpitId()))return false;
        F35DisplayClientConfig.bind(c);F35TargetLockClient.bind(expected);return true;
    }
    public static String label(UUID id){return id==null?"WAIT":id.toString().substring(0,8).toUpperCase(java.util.Locale.ROOT);}
}
