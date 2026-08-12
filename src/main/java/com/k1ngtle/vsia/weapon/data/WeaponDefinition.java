package com.k1ngtle.vsia.weapon.data;

import net.minecraft.resources.ResourceLocation;

public record WeaponDefinition(
        ResourceLocation id,
        AmmoDefinition ammo,
        FireControlDefinition fireControl,
        ReloadDefinition reload,
        RecoilDefinition recoil,
        AdsDefinition ads,
        BallisticsDefinition ballistics) {
}
