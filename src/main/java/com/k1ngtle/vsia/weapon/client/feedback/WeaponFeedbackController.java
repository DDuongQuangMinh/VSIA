package com.k1ngtle.vsia.weapon.client.feedback;

import com.k1ngtle.vsia.weapon.api.VSIAWeaponAPI;
import com.k1ngtle.vsia.weapon.client.network.ClientWeaponEventBus;
import com.k1ngtle.vsia.weapon.data.WeaponDefinition;
import com.k1ngtle.vsia.weapon.network.s2c.WeaponEventPacket;
import com.k1ngtle.vsia.weapon.state.WeaponEventType;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class WeaponFeedbackController {
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();
    private static final DustParticleOptions TRACER =
            new DustParticleOptions(new Vector3f(1.0F, 0.72F, 0.22F), 0.65F);

    private WeaponFeedbackController() {}

    public static void initialize() {
        if (INITIALIZED.compareAndSet(false, true)) {
            ClientWeaponEventBus.addListener(WeaponFeedbackController::accept);
        }
    }

    private static void accept(WeaponEventPacket event) {
        if (event.type() != WeaponEventType.SHOT) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;
        Entity source = level.getEntity(event.sourceEntityId());
        if (!(source instanceof LivingEntity shooter)) return;

        ItemStack stack = shooter.getItemInHand(event.hand());
        WeaponDefinition definition = VSIAWeaponAPI.getWeapon(stack).orElse(null);
        playSound(level, shooter);
        spawnMuzzleFlash(level, shooter);
        spawnShell(level, shooter);
        spawnTracer(level, event.start(), event.end());
        spawnImpact(level, event.end());
        if (shooter == minecraft.player && definition != null) applyRecoil(minecraft.player, definition);
    }

    private static void applyRecoil(LocalPlayer player, WeaponDefinition definition) {
        float vertical = definition.recoil().vertical();
        float horizontal = definition.recoil().horizontal();
        float yaw = (player.getRandom().nextFloat() * 2.0F - 1.0F) * horizontal;
        player.setXRot(Mth.clamp(player.getXRot() - vertical, -90.0F, 90.0F));
        player.setYRot(player.getYRot() + yaw);
    }

    private static void playSound(ClientLevel level, LivingEntity shooter) {
        level.playLocalSound(shooter.getX(), shooter.getEyeY(), shooter.getZ(),
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS,
                1.7F, 0.72F + level.random.nextFloat() * 0.08F, false);
    }

    private static void spawnMuzzleFlash(ClientLevel level, LivingEntity shooter) {
        Vec3 muzzle = shooter.getEyePosition().add(shooter.getLookAngle().scale(0.75D));
        level.addParticle(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 0.0D, 0.0D, 0.0D);
        level.addParticle(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 0.0D, 0.01D, 0.0D);
    }

    private static void spawnShell(ClientLevel level, LivingEntity shooter) {
        Vec3 look = shooter.getLookAngle();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 origin = shooter.getEyePosition().add(look.scale(0.25D)).add(right.scale(0.28D));
        Vec3 velocity = right.scale(0.08D).add(0.0D, 0.06D, 0.0D);
        level.addParticle(ParticleTypes.CRIT, origin.x, origin.y, origin.z,
                velocity.x, velocity.y, velocity.z);
    }

    private static void spawnTracer(ClientLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 0.001D) return;
        int samples = Mth.clamp((int) (length / 2.0D), 2, 64);
        Vec3 direction = delta.scale(1.0D / samples);
        for (int index = 1; index < samples; index++) {
            Vec3 point = start.add(direction.scale(index));
            level.addParticle(TRACER, point.x, point.y, point.z, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void spawnImpact(ClientLevel level, Vec3 point) {
        level.addParticle(ParticleTypes.POOF, point.x, point.y, point.z, 0.0D, 0.0D, 0.0D);
    }
}
