package com.k1ngtle.vsia.weapon.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Singleton so the GeoItemRenderer (and its animation state) isn't
 * rebuilt on every render call. Wire this up from GunItem - see the
 * commented-out initializeClient override in GunItem.java. If your
 * installed GeckoLib/Forge version expects a different hook, this is
 * the only class you need to adjust the wiring for; the renderer
 * itself (GunItemRenderer/GunGeoModel) stays the same either way.
 */
public final class GunItemClientExtensions implements IClientItemExtensions {

    public static final GunItemClientExtensions INSTANCE = new GunItemClientExtensions();

    private GunItemRenderer renderer;

    private GunItemClientExtensions() {}

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (renderer == null) {
            renderer = new GunItemRenderer();
        }
        return renderer;
    }
}