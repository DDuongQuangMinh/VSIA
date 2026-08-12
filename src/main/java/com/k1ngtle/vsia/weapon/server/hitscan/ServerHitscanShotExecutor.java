package com.k1ngtle.vsia.weapon.server.hitscan;

import com.k1ngtle.vsia.weapon.data.BallisticsDefinition;
import com.k1ngtle.vsia.weapon.data.WeaponDefinition;
import com.k1ngtle.vsia.weapon.server.ShotExecutor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ServerHitscanShotExecutor implements ShotExecutor {
    @Override
    public void execute(ServerPlayer player, ItemStack weapon, WeaponDefinition definition) {
        ServerLevel level = player.serverLevel();
        BallisticsDefinition ballistics = definition.ballistics();
        Vec3 start = player.getEyePosition();
        Vec3 direction = applySpread(player.getLookAngle(), ballistics.spreadDegrees(), level);
        Vec3 desiredEnd = start.add(direction.scale(ballistics.range()));
        BlockHitResult blockHit = level.clip(new ClipContext(start, desiredEnd,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? desiredEnd : blockHit.getLocation();

        AABB search = new AABB(start, end).inflate(1.0D);
        List<Candidate> candidates = new ArrayList<>();
        for (Entity entity : level.getEntities(player, search, Entity::isPickable)) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;
            Optional<Vec3> intersection = living.getBoundingBox().inflate(0.1D).clip(start, end);
            intersection.ifPresent(point -> candidates.add(new Candidate(living, point, start.distanceTo(point))));
        }
        candidates.sort(Comparator.comparingDouble(Candidate::distance));

        List<HitscanTargetHit> applied = new ArrayList<>();
        int maximumHits = 1 + ballistics.entityPenetration();
        for (int index = 0; index < Math.min(maximumHits, candidates.size()); index++) {
            Candidate candidate = candidates.get(index);
            float rangeFactor = Mth.clamp((float) (candidate.distance / ballistics.range()), 0.0F, 1.0F);
            float damage = ballistics.damage() * Mth.lerp(rangeFactor, 1.0F,
                    ballistics.minimumDamageMultiplier());
            damage *= (float) Math.pow(ballistics.penetrationDamageMultiplier(), index);
            boolean headshot = candidate.point.y >= candidate.entity.getBoundingBox().minY
                    + candidate.entity.getBbHeight() * 0.75D;
            if (headshot) damage *= ballistics.headshotMultiplier();
            candidate.entity.hurt(player.damageSources().playerAttack(player), damage);
            applied.add(new HitscanTargetHit(candidate.entity, candidate.point, damage, headshot));
        }
        HitscanEvents.publish(new HitscanResult(player.getId(), start, end, applied));
    }

    private static Vec3 applySpread(Vec3 forward, float degrees, ServerLevel level) {
        if (degrees <= 0.0F) return forward;
        double radians = Math.toRadians(degrees);
        Vec3 upReference = Math.abs(forward.y) > 0.99D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = forward.cross(upReference).normalize();
        Vec3 up = right.cross(forward).normalize();
        double radius = Math.sqrt(level.random.nextDouble()) * Math.tan(radians);
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        return forward.add(right.scale(Math.cos(angle) * radius))
                .add(up.scale(Math.sin(angle) * radius)).normalize();
    }

    private record Candidate(LivingEntity entity, Vec3 point, double distance) {}
}
