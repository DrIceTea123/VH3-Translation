package com.dricetea.vh3patch.modules;

import com.dricetea.vh3patch.module.TranslationModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class CombatStatsModuleTest {
    @Test void usesRawIdAndKeepsCowAndBossDistinct(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("combat_stats.json"),
                "{\"aggressive_cow\":\"战斗牛\",\"aggressive_cow_boss\":\"战斗牛首领\"}");
        CombatStatsModule module = CombatStatsModule.INSTANCE;
        module.initialize(directory);
        assertEquals("combat_stats.json", module.configFileName());
        assertEquals("战斗牛", module.configuredTranslation("the_vault:aggressive_cow"));
        assertEquals("战斗牛", module.configuredTranslation("aggressive_cow"));
        assertEquals("战斗牛首领", module.configuredTranslation("the_vault:aggressive_cow_boss"));
        assertNull(module.configuredTranslation("Aggressive Cow"));
        assertNull(module.configuredTranslation(null));
    }

    @Test void newEntryChangesHookResultWithoutSourceChanges(@TempDir Path directory) throws Exception {
        CombatStatsModule module = CombatStatsModule.INSTANCE;
        Path file = directory.resolve("combat_stats.json");
        Files.writeString(file, "{}");
        module.initialize(directory);
        assertNull(module.configuredTranslation("other:custom"));
        Files.writeString(file, "{\"custom\":\"自定义名称\"}");
        module.reload(directory);
        assertEquals("自定义名称", CombatStatsModule.translate("other:custom", "Custom"));
        assertEquals("Fallback", CombatStatsModule.translate(null, "Fallback"));
    }

    @Test void ordinaryModulesUseEnglishInputUnchanged(@TempDir Path directory) throws Exception {
        TranslationModule module = new TranslationModule("example") {};
        Files.writeString(directory.resolve("example.json"), "{\"Aggressive Cow\":\"战斗牛\"}");
        module.initialize(directory);
        assertEquals("战斗牛", module.configuredTranslation("Aggressive Cow"));
        assertNull(module.configuredTranslation("aggressive_cow"));
        assertNull(module.configuredTranslation("aggressive cow"));
    }

    @Test void missingOrBrokenInitialFileStopsInitializationWithPath(@TempDir Path directory) throws Exception {
        TranslationModule module = new TranslationModule("example") {};
        var missing = assertThrows(IllegalStateException.class, () -> module.initialize(directory));
        assertTrue(missing.getMessage().contains(directory.resolve("example.json").toString()));
        assertFalse(Files.exists(directory.resolve("example.json")));
        Files.writeString(directory.resolve("example.json"), "broken");
        assertThrows(IllegalStateException.class, () -> module.initialize(directory));
        assertNull(module.configuredTranslation("anything"));
        Files.writeString(directory.resolve("example.json"), "{}");
        assertDoesNotThrow(() -> module.initialize(directory));
    }
}
