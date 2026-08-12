package com.k1ngtle.vsia.weapon.resource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.k1ngtle.vsia.weapon.data.AdsDefinition;
import com.k1ngtle.vsia.weapon.data.AmmoDefinition;
import com.k1ngtle.vsia.weapon.data.BallisticsDefinition;
import com.k1ngtle.vsia.weapon.data.FireControlDefinition;
import com.k1ngtle.vsia.weapon.data.RecoilDefinition;
import com.k1ngtle.vsia.weapon.data.ReloadDefinition;
import com.k1ngtle.vsia.weapon.data.WeaponDefinition;
import com.k1ngtle.vsia.weapon.state.FireMode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public final class WeaponDefinitionParser {
    private WeaponDefinitionParser() {}

    public static WeaponDefinition parse(ResourceLocation id, JsonObject json) {
        JsonObject ammoJson = GsonHelper.getAsJsonObject(json, "ammo");
        AmmoDefinition ammo = new AmmoDefinition(
                requiredId(ammoJson, "id"),
                GsonHelper.getAsInt(ammoJson, "magazine_capacity"),
                GsonHelper.getAsBoolean(ammoJson, "chambered", true));

        JsonObject fireJson = GsonHelper.getAsJsonObject(json, "fire_control");
        List<FireMode> modes = new ArrayList<>();
        GsonHelper.getAsJsonArray(fireJson, "modes").forEach(element ->
                modes.add(parseMode(element.getAsString())));
        FireControlDefinition fire = new FireControlDefinition(
                GsonHelper.getAsFloat(fireJson, "rounds_per_minute"), modes,
                parseMode(GsonHelper.getAsString(fireJson, "default_mode")),
                GsonHelper.getAsInt(fireJson, "burst_size", 3));

        JsonObject reloadJson = GsonHelper.getAsJsonObject(json, "reload");
        ReloadDefinition reload = new ReloadDefinition(
                GsonHelper.getAsInt(reloadJson, "tactical_ticks"),
                GsonHelper.getAsInt(reloadJson, "empty_ticks"),
                GsonHelper.getAsBoolean(reloadJson, "detachable_magazine", true));

        JsonObject recoilJson = GsonHelper.getAsJsonObject(json, "recoil");
        RecoilDefinition recoil = new RecoilDefinition(
                GsonHelper.getAsFloat(recoilJson, "vertical"),
                GsonHelper.getAsFloat(recoilJson, "horizontal"),
                GsonHelper.getAsFloat(recoilJson, "recovery"));

        JsonObject adsJson = GsonHelper.getAsJsonObject(json, "ads");
        AdsDefinition ads = new AdsDefinition(
                GsonHelper.getAsInt(adsJson, "aim_ticks"),
                GsonHelper.getAsFloat(adsJson, "fov_multiplier"),
                GsonHelper.getAsFloat(adsJson, "movement_multiplier"));

        JsonObject ballisticsJson = GsonHelper.getAsJsonObject(json, "ballistics");
        BallisticsDefinition ballistics = new BallisticsDefinition(
                GsonHelper.getAsFloat(ballisticsJson, "damage"),
                GsonHelper.getAsFloat(ballisticsJson, "range"),
                GsonHelper.getAsFloat(ballisticsJson, "minimum_damage_multiplier", 0.5F),
                GsonHelper.getAsFloat(ballisticsJson, "spread_degrees", 0.0F),
                GsonHelper.getAsFloat(ballisticsJson, "headshot_multiplier", 1.5F),
                GsonHelper.getAsInt(ballisticsJson, "entity_penetration", 0),
                GsonHelper.getAsFloat(ballisticsJson, "penetration_damage_multiplier", 0.65F));
        return new WeaponDefinition(id, ammo, fire, reload, recoil, ads, ballistics);
    }

    private static ResourceLocation requiredId(JsonObject json, String key) {
        ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(json, key));
        if (id == null) throw new JsonParseException("Invalid resource location in '" + key + "'");
        return id;
    }

    private static FireMode parseMode(String name) {
        try { return FireMode.valueOf(name.toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new JsonParseException("Unknown fire mode: " + name, exception); }
    }
}
