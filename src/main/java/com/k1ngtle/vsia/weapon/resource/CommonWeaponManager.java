package com.k1ngtle.vsia.weapon.resource;

import com.k1ngtle.vsia.weapon.data.WeaponDefinition;
import com.k1ngtle.vsia.weapon.resource.index.CommonWeaponIndex;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

public final class CommonWeaponManager {
    private static final CommonWeaponManager INSTANCE = new CommonWeaponManager();
    private final Map<ResourceLocation, CommonWeaponIndex> weapons = new ConcurrentHashMap<>();

    private CommonWeaponManager() {}
    public static CommonWeaponManager getInstance() { return INSTANCE; }

    public void register(WeaponDefinition definition) {
        weapons.put(definition.id(), new CommonWeaponIndex(definition.id(), definition));
    }

    public void replaceAll(Collection<WeaponDefinition> definitions) {
        Map<ResourceLocation, CommonWeaponIndex> replacement = new ConcurrentHashMap<>();
        for (WeaponDefinition definition : definitions) {
            replacement.put(definition.id(), new CommonWeaponIndex(definition.id(), definition));
        }
        weapons.clear();
        weapons.putAll(replacement);
    }

    public Optional<WeaponDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(weapons.get(id)).map(CommonWeaponIndex::definition);
    }

    public Collection<CommonWeaponIndex> all() { return List.copyOf(weapons.values()); }
    public void clear() { weapons.clear(); }
}
