package com.k1ngtle.vsia.weapon.item;

import com.k1ngtle.vsia.weapon.api.IWeapon;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractWeaponItem extends Item implements IWeapon {
    public static final String WEAPON_ID_TAG = "VSIAWeaponId";

    protected AbstractWeaponItem(Properties properties) {
        super(properties);
    }

    @Override
    public Optional<ResourceLocation> getWeaponId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(WEAPON_ID_TAG, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(tag.getString(WEAPON_ID_TAG)));
    }

    @Override
    public void setWeaponId(ItemStack stack, ResourceLocation id) {
        stack.getOrCreateTag().putString(WEAPON_ID_TAG, id.toString());
    }
}
