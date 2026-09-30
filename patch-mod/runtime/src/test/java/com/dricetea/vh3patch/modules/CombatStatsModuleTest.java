package com.dricetea.vh3patch.modules;

import com.dricetea.vh3patch.module.TranslationModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CombatStatsModuleTest {
    @Test void usesRawIdAndKeepsCowAndBossDistinct(@TempDir Path directory) throws Exception {
        CombatStatsModule module = CombatStatsModule.INSTANCE;
        module.reload(directory);
        assertEquals("combat_stats.json", module.configFileName());
        assertEquals("战斗牛", module.configuredTranslation("the_vault:aggressive_cow"));
        assertEquals("战斗牛", module.configuredTranslation("aggressive_cow"));
        assertEquals("战斗牛首领", module.configuredTranslation("the_vault:aggressive_cow_boss"));
        assertNull(module.configuredTranslation("Aggressive Cow"));
        assertNull(module.configuredTranslation(null));
    }

    @Test void editedConfigChangesHookResultWithoutLanguageFile(@TempDir Path directory) throws Exception {
        CombatStatsModule module = CombatStatsModule.INSTANCE;
        Files.writeString(directory.resolve("combat_stats.json"), "{\"aggressive_cow\":\"新名称\",\"custom\":\"自定义名称\"}");
        try {
            module.reload(directory);
            assertEquals("新名称", CombatStatsModule.translate("the_vault:aggressive_cow", "Aggressive Cow"));
            assertEquals("自定义名称", CombatStatsModule.translate("other:custom", "Custom"));
            assertEquals("Fallback", CombatStatsModule.translate(null, "Fallback"));
        } finally {
            // 单例只在客户端持有；测试恢复内置配置，避免用例之间共享修改后的词典。
            Files.delete(directory.resolve("combat_stats.json"));
            module.reload(directory);
        }
    }

    @Test void ordinaryModulesUseEnglishInputUnchanged() {
        TranslationModule module = new TranslationModule("example") {};
        assertEquals("战斗牛", module.configuredTranslation("Aggressive Cow"));
        assertNull(module.configuredTranslation("aggressive_cow"));
        assertNull(module.configuredTranslation("aggressive cow"));
    }
}
