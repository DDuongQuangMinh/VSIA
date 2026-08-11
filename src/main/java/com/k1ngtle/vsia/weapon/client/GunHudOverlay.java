package com.k1ngtle.vsia.weapon.client;

import com.k1ngtle.vsia.weapon.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Ammo count is read straight off the held ItemStack's own NBT
 * (GunItem#getAmmo), not from ClientGunFeedback - the stack's NBT
 * already syncs to its owner automatically as part of normal
 * inventory replication, so this is accurate even before any shot
 * has been fired this session. Reload/fire-mode state isn't stored
 * in NBT, so those still come from the S2C sync packet via
 * ClientGunFeedback.
 */
public final class GunHudOverlay implements IGuiOverlay {

    public static final GunHudOverlay INSTANCE = new GunHudOverlay();

    private static final int COLOR_NORMAL = 0xFFFFFFFF;
    private static final int COLOR_LOW = 0xFFFF5555;
    private static final int COLOR_LABEL = 0xFFAAAAAA;

    private GunHudOverlay() {}

    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics graphics,
                       float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        ItemStack held = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(held.getItem() instanceof GunItem gun)) return;

        int ammo = gun.getAmmo(held);
        int maxAmmo = gun.getMaxAmmoCapacity();
        boolean reloading = ClientGunFeedback.isReloading;

        String ammoText = reloading ? "RELOADING..." : ammo + " / " + maxAmmo;
        int ammoColor = reloading ? COLOR_LOW : (ammo == 0 ? COLOR_LOW : COLOR_NORMAL);

        String modeText = gun.getFireMode(held).name();

        int marginX = 10;
        int marginY = 30;
        int x = screenWidth - marginX;
        int y = screenHeight - marginY;

        int ammoWidth = mc.font.width(ammoText);
        graphics.drawString(mc.font, ammoText, x - ammoWidth, y, ammoColor, true);

        String modeLine = gun.getGunName() + "  [" + modeText + "]";
        int modeWidth = mc.font.width(modeLine);
        graphics.drawString(mc.font, modeLine, x - modeWidth, y - 12, COLOR_LABEL, true);
    }
}