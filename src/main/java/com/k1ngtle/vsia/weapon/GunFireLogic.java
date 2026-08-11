package com.k1ngtle.vsia.weapon;

import com.k1ngtle.vsia.weapon.network.S2CGunAmmoSyncPacket;
import com.k1ngtle.vsia.weapon.network.S2CGunFirePacket;
import com.k1ngtle.vsia.weapon.network.WeaponNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side authority for firing and reloading. Everything here trusts
 * the server's own read of the player's held item, ammo NBT, and
 * inventory contents - the client only ever says "I fired" or
 * "reload me", never "I hit this" or "I have this much ammo".
 */
public final class GunFireLogic {

    private static final Map<UUID, Long> lastFireTick = new HashMap<>();
    private static final Map<UUID, Long> reloadEndTick = new HashMap<>();
    private static final Map<UUID, Integer> reloadRoundsPending = new HashMap<>();

    // Burst-fire scheduling: shots still owed from the current trigger pull,
    // and the game tick the next of those shots should fire on.
    private static final Map<UUID, Integer> burstShotsRemaining = new HashMap<>();
    private static final Map<UUID, Long> nextBurstShotTick = new HashMap<>();

    private GunFireLogic() {}

    public static void handleFireRequest(ServerPlayer player) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof GunItem gun)) return;

        UUID id = player.getUUID();
        if (isReloading(id)) return;
        if (burstShotsRemaining.getOrDefault(id, 0) > 0) return; // burst already in flight
        if (!gun.canFire(stack)) return;

        long now = player.level().getGameTime();
        long last = lastFireTick.getOrDefault(id, 0L);
        if (now - last < gun.getTicksBetweenShots()) return;

        FireMode mode = gun.getFireMode(stack);
        lastFireTick.put(id, now);
        fireSingleShot(player, stack, gun);

        if (mode == FireMode.BURST && gun.getBurstSize() > 1) {
            burstShotsRemaining.put(id, gun.getBurstSize() - 1);
            nextBurstShotTick.put(id, now + gun.getTicksBetweenShots());
        }
    }

    public static void handleReloadRequest(ServerPlayer player) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof GunItem gun)) return;

        UUID id = player.getUUID();
        if (isReloading(id)) return;
        if (burstShotsRemaining.getOrDefault(id, 0) > 0) return; // don't interrupt an in-flight burst
        if (gun.getAmmo(stack) >= gun.getMaxAmmoCapacity()) return;

        int needed = gun.getMaxAmmoCapacity() - gun.getAmmo(stack);
        int roundsToLoad;

        if (gun.getCompatibleAmmo().isEmpty()) {
            // No ammo item configured for this gun - treat as unlimited/free reload.
            roundsToLoad = needed;
        } else {
            int available = countAmmoInInventory(player, gun);
            if (available <= 0) return; // nothing to reload with
            roundsToLoad = Math.min(needed, available);
            consumeAmmoFromInventory(player, gun, roundsToLoad);
        }

        reloadRoundsPending.put(id, roundsToLoad);
        reloadEndTick.put(id, player.level().getGameTime() + gun.getReloadTicks());

        WeaponNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new S2CGunAmmoSyncPacket(gun.getAmmo(stack), true));
    }

    /** Call once per server tick for every player (see GunServerEvents). */
    public static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        tickReload(player, id);
        tickBurst(player, id);
    }

    private static void tickReload(ServerPlayer player, UUID id) {
        Long endTick = reloadEndTick.get(id);
        if (endTick == null || player.level().getGameTime() < endTick) return;

        reloadEndTick.remove(id);
        Integer roundsToLoad = reloadRoundsPending.remove(id);
        if (roundsToLoad == null) roundsToLoad = 0;

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (stack.getItem() instanceof GunItem gun) {
            gun.setAmmo(stack, gun.getAmmo(stack) + roundsToLoad);
            WeaponNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new S2CGunAmmoSyncPacket(gun.getAmmo(stack), false));
        }
    }

    private static void tickBurst(ServerPlayer player, UUID id) {
        Integer remaining = burstShotsRemaining.get(id);
        if (remaining == null || remaining <= 0) return;

        Long nextTick = nextBurstShotTick.get(id);
        if (nextTick == null || player.level().getGameTime() < nextTick) return;

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof GunItem gun) || !gun.canFire(stack)) {
            // Ran out of ammo (or swapped items) mid-burst - stop here rather than error.
            burstShotsRemaining.remove(id);
            nextBurstShotTick.remove(id);
            return;
        }

        fireSingleShot(player, stack, gun);
        lastFireTick.put(id, player.level().getGameTime());

        int left = remaining - 1;
        if (left > 0) {
            burstShotsRemaining.put(id, left);
            nextBurstShotTick.put(id, player.level().getGameTime() + gun.getTicksBetweenShots());
        } else {
            burstShotsRemaining.remove(id);
            nextBurstShotTick.remove(id);
        }
    }

    private static boolean isReloading(UUID id) {
        return reloadEndTick.containsKey(id);
    }

    private static void fireSingleShot(ServerPlayer player, ItemStack stack, GunItem gun) {
        gun.setAmmo(stack, gun.getAmmo(stack) - 1);
        performHitScan(player, stack, gun);

        WeaponNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new S2CGunFirePacket(player.getId()));

        WeaponNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new S2CGunAmmoSyncPacket(gun.getAmmo(stack), false));
    }

    private static int countAmmoInInventory(ServerPlayer player, GunItem gun) {
        int total = 0;
        for (ItemStack invStack : player.getInventory().items) {
            if (!invStack.isEmpty() && gun.isAmmoCompatible(invStack.getItem())) {
                total += invStack.getCount();
            }
        }
        return total;
    }

    private static void consumeAmmoFromInventory(ServerPlayer player, GunItem gun, int amount) {
        int remaining = amount;
        for (ItemStack invStack : player.getInventory().items) {
            if (remaining <= 0) break;
            if (invStack.isEmpty() || !gun.isAmmoCompatible(invStack.getItem())) continue;
            int take = Math.min(remaining, invStack.getCount());
            invStack.shrink(take);
            remaining -= take;
        }
    }

    private static void performHitScan(ServerPlayer player, ItemStack stack, GunItem gun) {
        double range = gun.getRange();
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(range));

        ClipContext clipContext = new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult blockHit = player.level().clip(clipContext);
        double blockDistance = blockHit.getType() != HitResult.Type.MISS
                ? blockHit.getLocation().distanceTo(start)
                : range;

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player.level(), player, start, end, searchBox,
                e -> !e.isSpectator() && e.isPickable() && e != player);

        if (entityHit != null) {
            double entityDistance = entityHit.getLocation().distanceTo(start);
            if (entityDistance <= blockDistance) {
                Entity target = entityHit.getEntity();
                target.hurt(player.level().damageSources().playerAttack(player),
                        gun.getEffectiveDamage(stack));
            }
        }
        // Block hits: hook in decal/impact particle spawning here if desired.
    }
}