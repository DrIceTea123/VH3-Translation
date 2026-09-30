package com.dricetea.vh3patch.modules;

import com.dricetea.vh3patch.module.TranslationModule;

/** 声音设置名称模块；输入是 ModSounds 的原始 Java 字段名，保持大写和下划线。 */
public final class SoundNamesModule extends TranslationModule {
    public static final String ID = "sound_names";
    public static final SoundNamesModule INSTANCE = new SoundNamesModule();

    private SoundNamesModule() { super(ID); }

    public static String translate(String fieldName, String originalName) {
        // 例如 RAFFLE_SFX -> 速通音效。缺少映射时保留原方法的英文结果。
        String translated = INSTANCE.configuredTranslation(fieldName);
        return translated == null ? originalName : translated;
    }
}
