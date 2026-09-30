package com.dricetea.vh3patch.client;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class NameLookupPolicyTest {
    private static String lookup(String id, String fallback, Map<String, String> registry, Map<String, String> language) {
        return NameLookupPolicy.resolve(id, fallback, registry::get, language::containsKey, language::get);
    }

    @Test void dedicatedOverridesKeepCowAndBossDistinct() {
        Map<String, String> registry = Map.of("the_vault:aggressive_cow", "entity.cow",
                "the_vault:aggressive_cow_boss", "entity.cow");
        Map<String, String> language = Map.of("entity.cow", "牛",
                "vh3_translation_patch.mob.the_vault.aggressive_cow", "战斗牛",
                "vh3_translation_patch.mob.the_vault.aggressive_cow_boss", "战斗牛首领");
        assertEquals("战斗牛", lookup("the_vault:aggressive_cow", "Aggressive Cow", registry, language));
        assertEquals("战斗牛首领", lookup("the_vault:aggressive_cow_boss", "Aggressive Cow Boss", registry, language));
    }

    @Test void realEntityKeyIsUsedInsteadOfGuessingKeyFromId() {
        assertEquals("注册表译名", lookup("a:mob", "Mob", Map.of("a:mob", "custom.actual_key"),
                Map.of("custom.actual_key", "注册表译名")));
    }

    @Test void unknownIdNeverUsesRegistryDefaultOrStrayOverride() {
        assertEquals("Unknown", lookup("a:missing", "Unknown", Map.of(),
                Map.of("vh3_translation_patch.mob.a.missing", "错误覆盖", "entity.minecraft.pig", "猪")));
    }

    @Test void languageSwitchAndReloadTakeEffectWithoutCache() {
        Map<String, String> registry = Map.of("a:mob", "entity.actual");
        Map<String, String> language = new HashMap<>(Map.of("entity.actual", "中文"));
        assertEquals("中文", lookup("a:mob", "Original", registry, language));
        language.put("entity.actual", "English");
        assertEquals("English", lookup("a:mob", "Original", registry, language));
        language.clear();
        assertEquals("Original", lookup("a:mob", "Original", registry, language));
    }
}
