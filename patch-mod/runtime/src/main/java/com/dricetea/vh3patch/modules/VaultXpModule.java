package com.dricetea.vh3patch.modules;

import com.dricetea.vh3patch.module.TranslationModule;
import java.util.Locale;

/** 经验提示按完整英文显示名查表，与结算模块的实体 ID 配置互相独立。 */
public final class VaultXpModule extends TranslationModule {
    public static final String ID = "vault_xp";
    public static final VaultXpModule INSTANCE = new VaultXpModule();
    private VaultXpModule() { super(ID); }

    public static String translate(String original) { return translate(original, original); }
    public static String translate(String original, String fallback) {
        String translated = INSTANCE.configuredTranslation(original);
        return translated != null ? translated : fallback;
    }

    public static String translateChest(String fallback, Enum<?> rarity, Enum<?> type, boolean barrel) {
        // VP 已汉化 VaultChestType 的显示字段；稳定的枚举 name 不受影响，避免形成中英混合查表键。
        String key = title(rarity.name()) + " " + title(type.name()) + (barrel ? " Barrel" : " Chest");
        return translate(key, fallback);
    }
    private static String title(String value) {
        return value.substring(0, 1) + value.substring(1).toLowerCase(Locale.ROOT);
    }
}
