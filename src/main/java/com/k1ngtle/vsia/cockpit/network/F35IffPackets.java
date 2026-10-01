package com.k1ngtle.vsia.cockpit.network;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.iff.F35IffConfig;
import com.k1ngtle.vsia.network.VsiaNetwork;
import net.minecraft.server.level.ServerPlayer;

public final class F35IffPackets {
    private F35IffPackets() { }
    public static void sendSnapshot(ServerPlayer p, F35CockpitSeatBlockEntity c, long now) {
        if (c.cockpitId() == null) return;
        F35IffConfig i = c.iff(); int modes = (i.mode1Enabled()?1:0)|(i.mode2Enabled()?2:0)|(i.mode3aEnabled()?4:0)|(i.mode4Enabled()?8:0)|(i.mode5Enabled()?16:0);
        int telemetry=(i.sendPosition()?1:0)|(i.sendVelocity()?2:0)|(i.sendHeading()?4:0)|(i.sendMission()?8:0);
        F35IffConfig.KeySlot k=i.activeKey(); String status=k.valid(now)?"KEY LOADED":(k.expiresAt()>0?"KEY EXPIRED":"NO KEY");
        VsiaNetwork.sendToPlayer(p,new S2CF35IffSnapshotPacket(c.cockpitId(),i.master().name(),i.mode1(),i.mode2(),i.mode3a(),modes,i.activeSlot(),k.id(),k.expiresAt(),telemetry,status));
    }
}
