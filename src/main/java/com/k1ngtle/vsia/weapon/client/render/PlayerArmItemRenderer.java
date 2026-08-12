package com.k1ngtle.vsia.weapon.client.render;

import com.k1ngtle.vsia.weapon.item.ModernGunItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

final class PlayerArmItemRenderer extends GeoItemRenderer<ModernGunItem> {
    PlayerArmItemRenderer() { super(new PlayerArmGeoModel()); }
}
