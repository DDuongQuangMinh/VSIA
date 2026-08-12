package com.k1ngtle.vsia.weapon.client.animation;

import com.k1ngtle.vsia.weapon.client.network.ClientWeaponEventBus;
import com.k1ngtle.vsia.weapon.item.ModernGunItem;
import com.k1ngtle.vsia.weapon.network.s2c.WeaponEventPacket;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;

public final class WeaponAnimationBridge {
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();
    private WeaponAnimationBridge() {}
    public static void initialize() {
        if (INITIALIZED.compareAndSet(false, true)) ClientWeaponEventBus.addListener(WeaponAnimationBridge::accept);
    }
    private static void accept(WeaponEventPacket event) {
        if (Minecraft.getInstance().level == null) return;
        Entity source = Minecraft.getInstance().level.getEntity(event.sourceEntityId());
        if (!(source instanceof net.minecraft.world.entity.LivingEntity living)) return;
        ItemStack stack = living.getItemInHand(event.hand());
        if (!(stack.getItem() instanceof ModernGunItem gun)) return;
        String animation = switch (event.type()) {
            case SHOT -> "fire";
            case RELOAD_STARTED -> "empty".equals(event.detail()) ? "reload_empty" : "reload";
            case RELOAD_CANCELLED, RELOAD_COMPLETED -> "idle";
            default -> null;
        };
        if (animation != null) gun.triggerAnim(living, GeoItem.getId(stack), ModernGunItem.CONTROLLER, animation);
    }
}
