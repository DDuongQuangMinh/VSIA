package com.k1ngtle.vsia.weapon.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.k1ngtle.vsia.weapon.data.WeaponDefinition;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public final class WeaponDefinitionReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Logger LOGGER = LogUtils.getLogger();

    public WeaponDefinitionReloadListener() {
        super(GSON, "vsia/weapons");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resources, ProfilerFiller profiler) {
        ArrayList<WeaponDefinition> definitions = new ArrayList<>();
        entries.forEach((id, element) -> {
            try {
                definitions.add(WeaponDefinitionParser.parse(id, element.getAsJsonObject()));
            } catch (JsonParseException | IllegalArgumentException exception) {
                LOGGER.error("Could not load VS:IA weapon definition {}", id, exception);
            }
        });
        CommonWeaponManager.getInstance().replaceAll(definitions);
        LOGGER.info("Loaded {} VS:IA weapon definitions", definitions.size());
    }
}
