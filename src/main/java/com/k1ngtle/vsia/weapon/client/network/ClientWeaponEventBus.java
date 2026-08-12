package com.k1ngtle.vsia.weapon.client.network;

import com.k1ngtle.vsia.weapon.network.s2c.WeaponEventPacket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class ClientWeaponEventBus {
    private static final List<Consumer<WeaponEventPacket>> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile WeaponEventPacket latest;
    private ClientWeaponEventBus() {}
    public static void addListener(Consumer<WeaponEventPacket> listener) { LISTENERS.add(listener); }
    public static void removeListener(Consumer<WeaponEventPacket> listener) { LISTENERS.remove(listener); }
    public static WeaponEventPacket latest() { return latest; }
    public static void publish(WeaponEventPacket event) { latest = event; LISTENERS.forEach(listener -> listener.accept(event)); }
}
