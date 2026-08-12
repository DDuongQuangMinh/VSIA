package com.k1ngtle.vsia.weapon.server.hitscan;

import java.util.List;
import net.minecraft.world.phys.Vec3;

public record HitscanResult(int shooterEntityId, Vec3 start, Vec3 end, List<HitscanTargetHit> hits) {
    public HitscanResult {
        hits = List.copyOf(hits);
    }
}
