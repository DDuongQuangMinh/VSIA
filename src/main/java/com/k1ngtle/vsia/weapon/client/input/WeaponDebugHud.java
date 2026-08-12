package com.k1ngtle.vsia.weapon.client.input;

import com.k1ngtle.vsia.weapon.api.IWeapon;
import com.k1ngtle.vsia.weapon.data.WeaponDefinition;
import com.k1ngtle.vsia.weapon.item.ModernGunItem;
import com.k1ngtle.vsia.weapon.state.FireMode;
import com.k1ngtle.vsia.weapon.state.WeaponRuntimeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "vsia", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WeaponDebugHud {
    private static boolean visible = true;

    private WeaponDebugHud() {}

    static void toggle() {
        visible = !visible;
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (!visible || minecraft.player == null || minecraft.options.hideGui) return;

        ItemStack stack = minecraft.player.getMainHandItem();
        if (!(stack.getItem() instanceof IWeapon weapon)) return;

        WeaponDefinition definition = weapon.getDefinition(stack).orElse(null);
        if (definition == null) return;

        WeaponRuntimeState state = weapon.getRuntimeState(stack);
        int loaded = state.getAvailableRounds();
        int reserve = countAmmo(minecraft.player.getInventory(), definition);
        String fireMode = fireModeLabel(state.getFireMode());
        String weaponName = definition.id().getPath().replace('_', ' ').toUpperCase(java.util.Locale.ROOT);

        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int panelWidth = 184;
        int panelHeight = 57;
        int x = width - panelWidth - 10;
        int y = height - panelHeight - 10;

        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0x52000000);
        graphics.drawString(minecraft.font, "HOLDING", x + 5, y + 3, 0xFFFF55, true);
        graphics.drawString(minecraft.font, weaponName, x + 48, y + 3, 0xD8D8D8, true);

        ItemStack hudStack = stack.copy();
        hudStack.setCount(1);
        hudStack.getOrCreateTag().putBoolean(ModernGunItem.HUD_PREVIEW_TAG, true);
        hudStack.getOrCreateTag().remove("GeckoLibID");

        graphics.pose().pushPose();
        graphics.pose().translate(x + 4.0F, y + 17.0F, 0.0F);
        graphics.pose().scale(2.15F, 2.15F, 1.0F);
        graphics.renderItem(hudStack, 0, 0);
        graphics.pose().popPose();

        graphics.fill(x + 75, y + 17, x + 78, y + 53, 0xFFFFFFFF);
        drawScaledString(graphics, minecraft, Integer.toString(loaded), x + 85, y + 18, 2.15F, 0xFFFFFF);
        drawScaledString(graphics, minecraft, "/ " + reserve, x + 126, y + 22, 1.25F, 0xC7C7C7);
        graphics.drawString(minecraft.font, fireMode, x + 87, y + 44,
                state.getFireMode() == FireMode.SAFE ? 0xFF5555 : 0xFFFFFF, true);
        graphics.drawString(minecraft.font, "AMMO", x + 128, y + 44, 0xA8A8A8, true);
    }

    private static void drawScaledString(GuiGraphics graphics, Minecraft minecraft, String text,
                                         int x, int y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(minecraft.font, text, 0, 0, color, true);
        graphics.pose().popPose();
    }

    private static int countAmmo(Inventory inventory, WeaponDefinition definition) {
        Item ammoItem = ForgeRegistries.ITEMS.getValue(definition.ammo().ammoId());
        if (ammoItem == null) return 0;

        int total = 0;
        for (ItemStack candidate : inventory.items) {
            if (candidate.is(ammoItem)) total += candidate.getCount();
        }
        for (ItemStack candidate : inventory.offhand) {
            if (candidate.is(ammoItem)) total += candidate.getCount();
        }
        return total;
    }

    private static String fireModeLabel(FireMode mode) {
        return switch (mode) {
            case SAFE -> "SAFE";
            case SEMI -> "SEMI";
            case BURST -> "BURST";
            case AUTO -> "AUTO";
        };
    }
}
