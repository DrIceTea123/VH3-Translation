package com.dricetea.vh3patch.modules;

/** Test-only linkage fixture; never packaged in the transformer JAR. */
public final class CombatStatsModule {
    public static String translate(String id, String fallback) {
        if (id.equals("the_vault:aggressive_cow")) return "战斗牛";
        if (id.equals("the_vault:aggressive_cow_boss")) return "战斗牛首领";
        return fallback;
    }
}
