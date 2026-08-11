package com.k1ngtle.vsia.weapon.registry;

import com.k1ngtle.vsia.weapon.AmmoItem;
import com.k1ngtle.vsia.weapon.FireMode;
import com.k1ngtle.vsia.weapon.GunItem;
import com.k1ngtle.vsia.weapon.attachment.AttachmentCategory;
import com.k1ngtle.vsia.weapon.attachment.AttachmentItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class WeaponItems {

    public static final String MODID = "vsia";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    // Creates an independent Sound Registry specifically for weapons
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

    // AK74 Sound
    public static final RegistryObject<SoundEvent> AK74_FIRE = SOUNDS.register("ak74_fire",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "ak74_fire")));

    // ---- Ammo ----
    public static final RegistryObject<Item> AMMO_556 = ITEMS.register("ammo_556",
            () -> new AmmoItem(new Item.Properties().stacksTo(64)));

    // 5.45x39mm Ammo for the AK
    public static final RegistryObject<Item> AMMO_545 = ITEMS.register("ammo_545",
            () -> new AmmoItem(new Item.Properties().stacksTo(64)));

    // ---- Attachments ----
    public static final RegistryObject<Item> SCOPE_REFLEX = ITEMS.register("scope_reflex",
            () -> new AttachmentItem.Builder()
                    .withCategory(AttachmentCategory.SCOPE)
                    .withCompatibilityGroup("pistol", "smg")
                    .withAimingZoomModifier(0.1)
                    .build());

    public static final RegistryObject<Item> MUZZLE_COMPENSATOR = ITEMS.register("muzzle_compensator",
            () -> new AttachmentItem.Builder()
                    .withCategory(AttachmentCategory.MUZZLE)
                    .withCompatibilityGroup("pistol")
                    .withRecoilModifier(-0.25f)
                    .build());

    // ---- Guns ----
    // AK74 SU VSOP!
    public static final RegistryObject<Item> AK74SU_VSOP = ITEMS.register("ak74suvsop",
            () -> new GunItem.Builder()
                    .withName("ak74suvsop")
                    .withCompatibilityGroup("rifle")
                    .withProperties(new Item.Properties().stacksTo(1))
                    .withRpm(650)
                    .withFireModes(FireMode.SINGLE, FireMode.AUTOMATIC)
                    .withMaxAmmoCapacity(30)
                    .withReloadTicks(54)
                    .withDamage(7.5f)
                    .withRange(130.0)
                    .withRecoil(0.85, 0.15, 0.18)
                    .withInaccuracy(0.7)
                    .withAimingZoom(1.3)
                    .withCompatibleAmmo(() -> AMMO_545.get()) // Unique AK ammo
                    .withAttachmentSlot(AttachmentCategory.SCOPE, AttachmentCategory.MUZZLE, AttachmentCategory.UNDERBARREL)
                    .withFireSound(AK74_FIRE)
                    .build());

    private WeaponItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        SOUNDS.register(modEventBus);
    }
}