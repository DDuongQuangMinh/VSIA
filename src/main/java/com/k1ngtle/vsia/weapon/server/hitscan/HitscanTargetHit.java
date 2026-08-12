package com.k1ngtle.vsia.weapon.server.hitscan;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public record HitscanTargetHit(LivingEntity target, Vec3 position, float damage, boolean headshot) {
}
