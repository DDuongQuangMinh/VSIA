package com.k1ngtle.vsia.weapon.client.resource;

import com.k1ngtle.vsia.weapon.client.resource.index.ClientWeaponIndex;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
public final class ClientWeaponManager {
    private static final ClientWeaponManager INSTANCE = new ClientWeaponManager();
    private final Map<ResourceLocation, ClientWeaponIndex> weapons = new ConcurrentHashMap<>();

    private ClientWeaponManager() {}
    public static ClientWeaponManager getInstance() { return INSTANCE; }
    public void register(ClientWeaponIndex index) { weapons.put(index.id(), index); }
    public Optional<ClientWeaponIndex> get(ResourceLocation id) { return Optional.ofNullable(weapons.get(id)); }
    public Collection<ClientWeaponIndex> all() { return List.copyOf(weapons.values()); }
    public void clear() { weapons.clear(); }
}
