package com.dricetea.vh3patch.modules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class SoundNamesModuleTest {
    @Test void rawFieldNamesAreCaseSensitiveAndUnknownNamesKeepOriginal(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("sound_names.json"), "{\"RAFFLE_SFX\":\"速通音效\"}");
        SoundNamesModule.INSTANCE.initialize(directory);
        assertEquals("sound_names.json", SoundNamesModule.INSTANCE.configFileName());
        assertEquals("速通音效", SoundNamesModule.translate("RAFFLE_SFX", "Raffle SFX"));
        assertEquals("Fallback", SoundNamesModule.translate("raffle_sfx", "Fallback"));
        assertEquals("Fallback", SoundNamesModule.translate("Raffle SFX", "Fallback"));
        assertEquals("Unknown", SoundNamesModule.translate("UNKNOWN", "Unknown"));
        assertEquals("Null", SoundNamesModule.translate(null, "Null"));
    }

    @Test void addingEntryUsesOnlyExternalConfigAndInvalidReloadKeepsIt(@TempDir Path directory) throws Exception {
        SoundNamesModule sound = SoundNamesModule.INSTANCE;
        CombatStatsModule combat = CombatStatsModule.INSTANCE;
        Files.writeString(directory.resolve("combat_stats.json"), "{\"aggressive_cow\":\"战斗牛\"}");
        Path file = directory.resolve("sound_names.json");
        Files.writeString(file, "{}");
        combat.initialize(directory);
        sound.initialize(directory);
        assertEquals("New Sound", SoundNamesModule.translate("NEW_SOUND", "New Sound"));
        Files.writeString(file, "{\"NEW_SOUND\":\"新增音效\",\"aggressive_cow\":\"声音模块自己的值\"}");
        sound.reload(directory);
        assertEquals("新增音效", SoundNamesModule.translate("NEW_SOUND", "New Sound"));
        assertEquals("战斗牛", combat.configuredTranslation("the_vault:aggressive_cow"));
        Files.writeString(file, "broken");
        assertThrows(java.io.IOException.class, () -> sound.reload(directory));
        assertEquals("新增音效", SoundNamesModule.translate("NEW_SOUND", "New Sound"));
        Files.delete(file);
        assertThrows(java.io.IOException.class, () -> sound.reload(directory));
        assertFalse(Files.exists(file));
        assertEquals("新增音效", SoundNamesModule.translate("NEW_SOUND", "New Sound"));
    }
}
