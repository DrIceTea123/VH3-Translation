package com.dricetea.vh3patch.modules;

import java.util.Map;

/** 隔离执行真实格式化字节码时使用的运行侧替身，不进入成品。 */
public final class VaultXpModule {
    public static Map<String, String> mappings = Map.of();
    public static String translate(String original) { return mappings.getOrDefault(original, original); }
    public static String translateChest(String fallback, Enum<?> rarity, Enum<?> type, boolean barrel) {
        String key = title(rarity.name()) + " " + title(type.name()) + (barrel ? " Barrel" : " Chest");
        return mappings.getOrDefault(key, fallback);
    }
    private static String title(String name) { return name.substring(0, 1) + name.substring(1).toLowerCase(java.util.Locale.ROOT); }
}
