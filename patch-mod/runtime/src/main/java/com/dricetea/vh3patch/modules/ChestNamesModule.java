package com.dricetea.vh3patch.modules;

import com.dricetea.vh3patch.module.TranslationModule;
import java.util.Locale;
import java.util.Set;

/** 宝箱、桶及强化宝箱共用显示词表；服务端命名同样可用，不依赖客户端 I18n。 */
public final class ChestNamesModule extends TranslationModule {
    public static final String ID = "chest_names";
    public static final ChestNamesModule INSTANCE = new ChestNamesModule();
    private static final Set<String> RARITIES = Set.of("Common", "Rare", "Epic", "Omega");
    private static final Set<String> HUNTER_CHESTS = Set.of("WOODEN", "GILDED", "LIVING", "ORNATE", "HARDENED", "ENIGMA", "FLESH");
    private ChestNamesModule() { super(ID); }

    public static String translate(String original) { return translate(original, original); }
    public static String translate(String original, String fallback) {
        String exact = INSTANCE.configuredTranslation(original);
        if (exact != null) return exact;
        if (original == null) return fallback;
        int space = original.indexOf(' ');
        if (space > 0 && RARITIES.contains(original.substring(0, space))) {
            String rarity = INSTANCE.configuredTranslation(original.substring(0, space));
            String name = INSTANCE.configuredTranslation(original.substring(space + 1));
            // 两部分都已配置才组合；完整条目优先，允许单项定制及整项英文占位。
            if (rarity != null && name != null) return rarity + name;
        }
        return fallback;
    }
    public static String translateChest(String fallback, Enum<?> rarity, Enum<?> type, boolean barrel) {
        return translate(title(rarity.name()) + " " + title(type.name()) + (barrel ? " Barrel" : " Chest"), fallback);
    }
    public static String translateBarrel(String fallback, Enum<?> type) {
        return translate(title(type.name()) + " Barrel", fallback);
    }
    public static String translateRarityCount(String original) {
        int colon = original.indexOf(": ");
        if (colon < 0 || !RARITIES.contains(original.substring(0, colon))) return original;
        String prefix = INSTANCE.configuredTranslation(original.substring(0, colon + 2));
        return prefix == null ? original : prefix + original.substring(colon + 2);
    }
    public static String translateHunter(String original) {
        return HUNTER_CHESTS.contains(original) ? translate(title(original) + " Chest", original) : original;
    }
    private static String title(String value) {
        return value.substring(0, 1) + value.substring(1).toLowerCase(Locale.ROOT);
    }
}
