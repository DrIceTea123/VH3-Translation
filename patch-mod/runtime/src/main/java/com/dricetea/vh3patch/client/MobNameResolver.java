package com.dricetea.vh3patch.client;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

public final class MobNameResolver {
    private MobNameResolver() {}

    public static String translate(String id, String originalName) {
        return NameLookupPolicy.resolve(id, originalName, MobNameResolver::entityLanguageKey,
                I18n::exists, key -> I18n.get(key));
    }

    private static String entityLanguageKey(String value) {
        if (value == null) return null;
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null || !ForgeRegistries.ENTITIES.containsKey(id)) return null;
        EntityType<?> type = ForgeRegistries.ENTITIES.getValue(id);
        return type == null ? null : type.getDescriptionId();
    }
}
