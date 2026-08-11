package com.k1ngtle.vsia.weapon.client;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.weapon.FireMode;
import com.k1ngtle.vsia.weapon.GunItem;
import com.k1ngtle.vsia.weapon.network.C2SCycleFireModePacket;
import com.k1ngtle.vsia.weapon.network.C2SFirePacket;
import com.k1ngtle.vsia.weapon.network.C2SReloadPacket;
import com.k1ngtle.vsia.weapon.network.WeaponNetwork;
import com.k1ngtle.vsia.weapon.registry.WeaponItems;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WeaponItems.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientGunHandler {

    public static KeyMapping FIRE_KEY;
    public static KeyMapping RELOAD_KEY;
    public static KeyMapping INSPECT_KEY;
    public static KeyMapping FIRE_MODE_KEY;

    private static long clientLastFireTick = 0;

    public static boolean isAiming = false;
    public static float currentAdsProgress = 0f;
    public static float prevAdsProgress = 0f;

    private static boolean wasZoomActive = false;

    private ClientGunHandler() {}

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        FIRE_KEY = new KeyMapping("key.vsia.fire", InputConstants.Type.MOUSE, 0, "key.categories.vsia");
        RELOAD_KEY = new KeyMapping("key.vsia.reload", InputConstants.Type.KEYSYM, InputConstants.KEY_R, "key.categories.vsia");
        INSPECT_KEY = new KeyMapping("key.vsia.inspect", InputConstants.Type.KEYSYM, InputConstants.KEY_F, "key.categories.vsia");
        FIRE_MODE_KEY = new KeyMapping("key.vsia.fire_mode", InputConstants.Type.KEYSYM, InputConstants.KEY_X, "key.categories.vsia");

        event.register(FIRE_KEY);
        event.register(RELOAD_KEY);
        event.register(INSPECT_KEY);
        event.register(FIRE_MODE_KEY);
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack held = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.getItem() instanceof GunItem) {
            if (event.isAttack() || event.isUseItem()) {
                event.setCanceled(true);
                event.setSwingHand(false);
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide() && event.getItemStack().getItem() instanceof GunItem) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        prevAdsProgress = currentAdsProgress;

        ItemStack held = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(held.getItem() instanceof GunItem gun)) {
            isAiming = false;
            currentAdsProgress = 0f;
            prevAdsProgress = 0f;
            return;
        }

        if (mc.screen != null) {
            isAiming = false;
            return;
        }

        // Inspect is a first-person-only animation.
        if (INSPECT_KEY != null && INSPECT_KEY.consumeClick()) {
            if (mc.options.getCameraType().isFirstPerson()) {
                held.getOrCreateTag().putString("TriggerAnim", "inspect");
            }
        }

        if (RELOAD_KEY != null && RELOAD_KEY.consumeClick()) {
            WeaponNetwork.CHANNEL.sendToServer(new C2SReloadPacket());
        }

        // TRIGGER: Fire Mode
        if (FIRE_MODE_KEY != null && FIRE_MODE_KEY.consumeClick()) {
            WeaponNetwork.CHANNEL.sendToServer(new C2SCycleFireModePacket());
        }

        isAiming = mc.options.keyUse.isDown();
        boolean isFiring = FIRE_KEY != null && FIRE_KEY.isDown();

        if (isAiming) {
            currentAdsProgress = Math.min(1f, currentAdsProgress + 0.15f);
        } else {
            currentAdsProgress = Math.max(0f, currentAdsProgress - 0.15f);
        }

        if (!isFiring) {
            return;
        }

        // TRIGGER: Fire
        boolean automatic = gun.getFireModes().contains(FireMode.AUTOMATIC);
        long now = mc.level.getGameTime();

        if (automatic) {
            if (now - clientLastFireTick >= gun.getTicksBetweenShots()) {
                clientLastFireTick = now;
                held.getOrCreateTag().putString("TriggerAnim", "fire");
                WeaponNetwork.CHANNEL.sendToServer(new C2SFirePacket());
            }
        } else {
            if (FIRE_KEY.consumeClick()) {
                clientLastFireTick = now;
                held.getOrCreateTag().putString("TriggerAnim", "fire");
                WeaponNetwork.CHANNEL.sendToServer(new C2SFirePacket());
            }
        }
    }

    public static float getLerpedAds() {
        Minecraft mc = Minecraft.getInstance();
        float partialTick = mc.getFrameTime();
        return Mth.lerp(partialTick, prevAdsProgress, currentAdsProgress);
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) {
            wasZoomActive = false;
            return;
        }

        ItemStack held = mc.player.getItemInHand(InteractionHand.MAIN_HAND);

        if (!(held.getItem() instanceof GunItem gun)) {
            wasZoomActive = false;
            return;
        }

        float ads = Mth.clamp(getLerpedAds(), 0.0f, 1.0f);

        if (ads <= 0.0f) {
            if (wasZoomActive) {
                Vsia.LOGGER.info("Weapon ADS zoom ended; camera FOV returned to normal");
                wasZoomActive = false;
            }
            return;
        }

        double effectiveZoom = Math.max(1.0, gun.getEffectiveAimingZoom(held));
        double zoomAtCurrentProgress = Mth.lerp(ads, 1.0, effectiveZoom);
        double baseFov = event.getFOV();
        double resultingFov = baseFov / zoomAtCurrentProgress;

        event.setFOV(resultingFov);

        if (!wasZoomActive) {
            Vsia.LOGGER.info(
                    "Weapon ADS zoom started: gun={}, baseFov={}, effectiveZoom={}, targetFov={}",
                    gun.getGunName(),
                    String.format("%.2f", baseFov),
                    String.format("%.2f", effectiveZoom),
                    String.format("%.2f", baseFov / effectiveZoom)
            );
            wasZoomActive = true;
        }
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (event.getEntity().getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof GunItem) {
            event.getRenderer().getModel().rightArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
            event.getRenderer().getModel().leftArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
    }
}