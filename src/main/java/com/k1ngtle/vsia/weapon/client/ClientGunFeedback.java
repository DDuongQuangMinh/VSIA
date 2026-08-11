package com.k1ngtle.vsia.weapon.client;

import com.k1ngtle.vsia.weapon.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Client-only handling for the two S2C packets. Kept separate from the
 * packet classes themselves so those stay import-clean for the dedicated
 * server (see the Dist.CLIENT check before these are ever called).
 */
public final class ClientGunFeedback {

    public static volatile int currentAmmo = 0;
    public static volatile boolean isReloading = false;

    private ClientGunFeedback() {}

    public static void onGunFired(int shooterEntityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity shooter = mc.level.getEntity(shooterEntityId);
        if (shooter == null) return;

        // Muzzle flash / fire sound for everyone who can see the shooter.
        mc.level.playLocalSound(shooter.getX(), shooter.getY(), shooter.getZ(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 1.0f, 1.0f, false);

        mc.level.addParticle(ParticleTypes.SMOKE,
                shooter.getX(), shooter.getEyeY(), shooter.getZ(), 0, 0.05, 0);

        if (shooter == mc.player) {
            // Apply physical camera recoil to the local player
            ItemStack held = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
            if (held.getItem() instanceof GunItem gun) {
                RecoilHandler.applyKick(
                        gun.getEffectiveRecoilPitch(held),
                        gun.getRecoilYaw(),
                        gun.getRecoilRecoverySpeed());
            }
        } else if (shooter instanceof LivingEntity livingShooter) {
            // Force the firing animation to play visually on OTHER players in the world!
            ItemStack held = livingShooter.getItemInHand(InteractionHand.MAIN_HAND);
            if (held.getItem() instanceof GunItem) {
                held.getOrCreateTag().putString("TriggerAnim", "fire");
            }
        }
    }

    public static void onAmmoSync(int ammo, boolean reloading) {
        boolean reloadJustStarted = reloading && !isReloading;

        currentAmmo = ammo;
        isReloading = reloading;

        if (reloadJustStarted) {
            Minecraft mc = Minecraft.getInstance();

            if (mc.player != null) {
                ItemStack held =
                        mc.player.getItemInHand(InteractionHand.MAIN_HAND);

                if (held.getItem() instanceof GunItem) {
                    held.getOrCreateTag().putString(
                            "TriggerAnim",
                            "reload"
                    );
                }
            }
        }
    }
}