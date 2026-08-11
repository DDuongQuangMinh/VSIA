package com.k1ngtle.vsia.weapon.client;

import com.k1ngtle.vsia.weapon.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Handles client feedback received from the server.
 *
 * GeckoLib fire and reload animations are triggered authoritatively
 * from GunFireLogic using GunItem#triggerAnim. This class must not
 * write transient animation triggers into ItemStack NBT.
 */
public final class ClientGunFeedback {

    public static volatile int currentAmmo = 0;
    public static volatile boolean isReloading = false;

    private ClientGunFeedback() {}

    public static void onGunFired(
            int shooterEntityId
    ) {
        Minecraft mc =
                Minecraft.getInstance();

        if (mc.level == null) {
            return;
        }

        Entity shooter =
                mc.level.getEntity(shooterEntityId);

        if (shooter == null) {
            return;
        }

        SoundEvent fireSound =
                SoundEvents.GENERIC_EXPLODE;

        if (shooter instanceof LivingEntity livingShooter) {
            ItemStack held =
                    livingShooter.getItemInHand(
                            InteractionHand.MAIN_HAND
                    );

            if (held.getItem() instanceof GunItem gun) {
                fireSound =
                        gun.getFireSound();

                if (shooter == mc.player) {
                    RecoilHandler.applyKick(
                            gun.getEffectiveRecoilPitch(held),
                            gun.getRecoilYaw(),
                            gun.getRecoilRecoverySpeed()
                    );
                }
            }
        }

        mc.level.playLocalSound(
                shooter.getX(),
                shooter.getY(),
                shooter.getZ(),
                fireSound,
                SoundSource.PLAYERS,
                1.0f,
                1.0f,
                false
        );

        mc.level.addParticle(
                ParticleTypes.SMOKE,
                shooter.getX(),
                shooter.getEyeY(),
                shooter.getZ(),
                0,
                0.05,
                0
        );
    }

    public static void onAmmoSync(
            int ammo,
            boolean reloading
    ) {
        currentAmmo = ammo;
        isReloading = reloading;
    }
}