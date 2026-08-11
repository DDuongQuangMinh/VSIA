package com.k1ngtle.vsia.weapon.registry;

import com.k1ngtle.vsia.weapon.AmmoItem;
import com.k1ngtle.vsia.weapon.FireMode;
import com.k1ngtle.vsia.weapon.GunItem;
import com.k1ngtle.vsia.weapon.attachment.AttachmentCategory;
import com.k1ngtle.vsia.weapon.attachment.AttachmentItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Wholly self-contained - no ordering games with another mod's
 * constructor, since everything registers on VSIA's own event bus.
 * Call WeaponItems.register(modEventBus) once from your main mod class.
 */
public final class WeaponItems {

    // Replace with your mod id constant if you already have one (e.g. Vsia.MODID)
    public static final String MODID = "vsia";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    // ---- Ammo ----
    public static final RegistryObject<Item> AMMO_9MM = ITEMS.register("ammo_9mm",
            () -> new AmmoItem(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> AMMO_556 = ITEMS.register("ammo_556",
            () -> new AmmoItem(new Item.Properties().stacksTo(64)));

    // ---- Attachments ----
    public static final RegistryObject<Item> SCOPE_REFLEX = ITEMS.register("scope_reflex",
            () -> new AttachmentItem.Builder()
                    .withCategory(AttachmentCategory.SCOPE)
                    .withCompatibilityGroup("pistol", "smg", "ar")
                    .withAimingZoomModifier(0.1)
                    .build());

    public static final RegistryObject<Item> MUZZLE_COMPENSATOR = ITEMS.register("muzzle_compensator",
            () -> new AttachmentItem.Builder()
                    .withCategory(AttachmentCategory.MUZZLE)
                    .withCompatibilityGroup("pistol", "ar")
                    .withRecoilModifier(-0.25f)
                    .build());

    // ---- Guns ----
    public static final RegistryObject<Item> PISTOL_M9 = ITEMS.register("pistol_m9",
            () -> new GunItem.Builder()
                    .withName("pistol_m9")
                    .withCompatibilityGroup("pistol")
                    .withProperties(new Item.Properties().stacksTo(1))
                    .withRpm(400)
                    .withFireModes(FireMode.SINGLE)
                    .withMaxAmmoCapacity(15)
                    .withReloadTicks(50)
                    .withDamage(5.0f)
                    .withRange(80.0)
                    .withRecoil(2.0, 0.4, 0.15)
                    .withInaccuracy(1.5)
                    .withAimingZoom(1.1)
                    .withCompatibleAmmo(() -> AMMO_9MM.get())
                    .withAttachmentSlot(AttachmentCategory.SCOPE, AttachmentCategory.MUZZLE)
                    .build());

    public static final RegistryObject<Item> ASSAULT_RIFLE_M4A1 = ITEMS.register("m4a1",
            () -> new GunItem.Builder()
                    .withName("m4a1") // This string connects to your .geo.json, .png, and .animation.json
                    .withCompatibilityGroup("ar")
                    .withProperties(new Item.Properties().stacksTo(1))
                    .withRpm(800) // 800 rounds per minute
                    .withFireModes(FireMode.AUTOMATIC, FireMode.SINGLE)
                    .withMaxAmmoCapacity(30)
                    .withReloadTicks(60) // 3 seconds
                    .withDamage(8.0f)
                    .withRange(150.0)
                    .withRecoil(1.5, 0.3, 0.1)
                    .withInaccuracy(0.5)
                    .withAimingZoom(1.5)
                    .withCompatibleAmmo(() -> AMMO_556.get())
                    .withAttachmentSlot(AttachmentCategory.SCOPE, AttachmentCategory.MUZZLE, AttachmentCategory.UNDERBARREL, AttachmentCategory.LASER, AttachmentCategory.MAGAZINE)
                    .build());

    private WeaponItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}