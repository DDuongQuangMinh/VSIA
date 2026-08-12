package com.k1ngtle.vsia.weapon.client.input;

import com.k1ngtle.vsia.weapon.api.IWeapon;
import com.k1ngtle.vsia.weapon.client.gameplay.ClientWeaponContext;
import com.k1ngtle.vsia.weapon.state.WeaponRuntimeState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "vsia", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WeaponDebugHud {
    private static boolean visible;
    private WeaponDebugHud() {}
    static void toggle() { visible = !visible; }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!visible || minecraft.player == null || minecraft.options.hideGui) return;
        ItemStack stack = minecraft.player.getMainHandItem();
        if (!(stack.getItem() instanceof IWeapon weapon)) return;
        String id = weapon.getWeaponId(stack).map(Object::toString).orElse("unassigned");
        WeaponRuntimeState state = weapon.getRuntimeState(stack);
        int color = 0xFFFFFF;
        event.getGuiGraphics().drawString(minecraft.font, "VS:IA " + id, 8, 8, color, true);
        event.getGuiGraphics().drawString(minecraft.font,
                "Ammo " + state.getMagazineAmmo() + "+" + (state.isChamberLoaded() ? 1 : 0)
                        + " | " + state.getFireMode(), 8, 20, color, true);
        event.getGuiGraphics().drawString(minecraft.font,
                "Reload " + state.isReloading() + " | ADS "
                        + String.format(java.util.Locale.ROOT, "%.2f", ClientWeaponContext.getInstance().getAimProgress()),
                8, 32, color, true);
    }
}
