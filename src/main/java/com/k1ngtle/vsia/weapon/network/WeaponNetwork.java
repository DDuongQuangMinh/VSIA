package com.k1ngtle.vsia.weapon.network;

import com.k1ngtle.vsia.weapon.registry.WeaponItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class WeaponNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(WeaponItems.MODID, "weapon"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextId = 0;
    private static int id() {
        return nextId++;
    }

    private WeaponNetwork() {}

    /** Call once from your mod constructor / common setup. */
    public static void register() {
        CHANNEL.registerMessage(id(), C2SFirePacket.class,
                C2SFirePacket::encode, C2SFirePacket::decode, C2SFirePacket::handle);

        CHANNEL.registerMessage(id(), C2SReloadPacket.class,
                C2SReloadPacket::encode, C2SReloadPacket::decode, C2SReloadPacket::handle);

        CHANNEL.registerMessage(id(), C2SCycleFireModePacket.class,
                C2SCycleFireModePacket::encode, C2SCycleFireModePacket::decode, C2SCycleFireModePacket::handle);

        CHANNEL.registerMessage(id(), S2CGunFirePacket.class,
                S2CGunFirePacket::encode, S2CGunFirePacket::decode, S2CGunFirePacket::handle);

        CHANNEL.registerMessage(id(), S2CGunAmmoSyncPacket.class,
                S2CGunAmmoSyncPacket::encode, S2CGunAmmoSyncPacket::decode, S2CGunAmmoSyncPacket::handle);
    }
}