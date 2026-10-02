package com.dricetea.vh3patch.modules;

import com.dricetea.vh3patch.module.TranslationModule;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

/** 结算页与经验提示的怪物名称模块；共用实体路径映射及语言表回退。 */
public final class MobNamesModule extends TranslationModule {
    // 模块名直接决定用户配置 config/vh3_translation_patch/mob_names.json。
    public static final String ID = "mob_names";
    public static final MobNamesModule INSTANCE = new MobNamesModule();

    private MobNamesModule() { super(ID); }

    @Override protected String mappingKey(String input) {
        if (input == null) return null;
        // 原始参数是 the_vault:aggressive_cow；按用户约定取 aggressive_cow 直接查表。
        // 不先转成英文，也不生成 entity.xxx 语言键；同路径的不同命名空间共用此映射。
        int colon = input.indexOf(':');
        return colon < 0 ? input : input.substring(colon + 1);
    }

    /** ASM 调用的稳定入口：保留原算法的结果，供没有译文时回退。 */
    public static String translate(String id, String originalName) {
        String configured = INSTANCE.configuredTranslation(id);
        if (configured != null) return configured;

        // 用户删掉映射后，仍可复用实体的现有译名；未知 ID 不能误取默认的猪。
        if (id == null) return originalName;
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null || !ForgeRegistries.ENTITIES.containsKey(location)) return originalName;
        EntityType<?> type = ForgeRegistries.ENTITIES.getValue(location);
        if (type != null && I18n.exists(type.getDescriptionId())) return I18n.get(type.getDescriptionId());
        return originalName;
    }
}
