package com.k1ngtle.vsia.weapon;

import net.minecraft.world.item.Item;

/**
 * Plain ammo item. Kept intentionally simple - if you later want
 * ammo types with different damage/velocity modifiers, extend this
 * the same way GunItem/AttachmentItem carry their stats.
 */
public class AmmoItem extends Item {
    public AmmoItem(Properties properties) {
        super(properties);
    }
}