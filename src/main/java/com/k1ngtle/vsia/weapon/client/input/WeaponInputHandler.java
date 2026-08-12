package com.k1ngtle.vsia.weapon.client.input;

import com.k1ngtle.vsia.weapon.api.VSIAWeaponAPI;
import com.k1ngtle.vsia.weapon.client.gameplay.LocalWeaponAim;
import com.k1ngtle.vsia.weapon.client.gameplay.LocalWeaponFireSelect;
import com.k1ngtle.vsia.weapon.client.gameplay.LocalWeaponReload;
import com.k1ngtle.vsia.weapon.client.gameplay.LocalWeaponShoot;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "vsia", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WeaponInputHandler {
    private static final LocalWeaponShoot SHOOT = new LocalWeaponShoot();
    private static final LocalWeaponReload RELOAD = new LocalWeaponReload();
    private static final LocalWeaponFireSelect FIRE_SELECT = new LocalWeaponFireSelect();
    private static final LocalWeaponAim AIM = new LocalWeaponAim();
    private static boolean firing;
    private static boolean aiming;

    private WeaponInputHandler() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        com.k1ngtle.vsia.weapon.client.animation.WeaponAnimationBridge.initialize();
        com.k1ngtle.vsia.weapon.client.feedback.WeaponFeedbackController.initialize();
        Minecraft minecraft = Minecraft.getInstance();
        boolean weapon = minecraft.player != null
                && VSIAWeaponAPI.getWeapon(minecraft.player.getMainHandItem()).isPresent();
        if (weapon) {
            if (minecraft.player.isUsingItem()) minecraft.player.stopUsingItem();
            cancelWeaponSwing(minecraft);
        }
        boolean active = weapon && minecraft.screen == null;
        boolean fireDown = active && minecraft.options.keyAttack.isDown();
        boolean aimDown = active && minecraft.options.keyUse.isDown();
        if (fireDown != firing) {
            if (fireDown) SHOOT.press(InteractionHand.MAIN_HAND); else SHOOT.release(InteractionHand.MAIN_HAND);
            firing = fireDown;
        }
        if (aimDown != aiming) { AIM.setAiming(aimDown); aiming = aimDown; }
        if (weapon) {
            while (WeaponKeyMappings.RELOAD.consumeClick()) RELOAD.request(InteractionHand.MAIN_HAND);
            while (WeaponKeyMappings.FIRE_MODE.consumeClick()) FIRE_SELECT.request(InteractionHand.MAIN_HAND);
        }
        while (WeaponKeyMappings.DEBUG_HUD.consumeClick()) WeaponDebugHud.toggle();
        if (weapon && minecraft.player != null) {
            VSIAWeaponAPI.getWeapon(minecraft.player.getMainHandItem())
                    .ifPresent(definition -> AIM.tick(definition.ads().aimTicks()));
        }
    }

    @SubscribeEvent
    public static void cancelVanilla(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || VSIAWeaponAPI.getWeapon(minecraft.player.getMainHandItem()).isEmpty()) return;
        if (event.isAttack() || event.isUseItem()) {
            event.setSwingHand(false);
            event.setCanceled(true);
            cancelWeaponSwing(minecraft);
        }
    }

    private static void cancelWeaponSwing(Minecraft minecraft) {
        minecraft.player.swinging = false;
        minecraft.player.swingTime = 0;
        minecraft.player.attackAnim = 0.0F;
        minecraft.player.oAttackAnim = 0.0F;
    }
}
