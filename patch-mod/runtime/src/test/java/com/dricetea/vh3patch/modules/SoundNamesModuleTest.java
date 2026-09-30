package com.dricetea.vh3patch.modules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class SoundNamesModuleTest {
    @Test void rawFieldNamesAreCaseSensitiveAndUnknownNamesKeepOriginal(@TempDir Path directory) throws Exception {
        SoundNamesModule.INSTANCE.reload(directory);
        assertEquals("sound_names.json", SoundNamesModule.INSTANCE.configFileName());
        assertEquals("速通音效", SoundNamesModule.translate("RAFFLE_SFX", "Raffle SFX"));
        assertEquals("蚱蜢：咕咕", SoundNamesModule.translate("GRASSHOPPER_BRRR", "Grasshopper Brrr"));
        assertEquals("Fallback", SoundNamesModule.translate("raffle_sfx", "Fallback"));
        assertEquals("Fallback", SoundNamesModule.translate("Raffle SFX", "Fallback"));
        assertEquals("Unknown", SoundNamesModule.translate("UNKNOWN", "Unknown"));
        assertEquals("Null", SoundNamesModule.translate(null, "Null"));
    }

    @Test void reloadChangesOnlySoundModuleAndInvalidFilePreservesLastValue(@TempDir Path directory) throws Exception {
        SoundNamesModule sound = SoundNamesModule.INSTANCE;
        CombatStatsModule combat = CombatStatsModule.INSTANCE;
        combat.reload(directory);
        sound.reload(directory);
        Path file = directory.resolve("sound_names.json");
        try {
            Files.writeString(file, "{\"RAFFLE_SFX\":\"自定义音效\",\"aggressive_cow\":\"声音模块自己的值\"}");
            sound.reload(directory);
            assertEquals("自定义音效", SoundNamesModule.translate("RAFFLE_SFX", "Raffle SFX"));
            assertEquals("战斗牛", combat.configuredTranslation("the_vault:aggressive_cow"));
            Files.writeString(file, "broken");
            assertThrows(java.io.IOException.class, () -> sound.reload(directory));
            assertEquals("自定义音效", SoundNamesModule.translate("RAFFLE_SFX", "Raffle SFX"));
        } finally {
            Files.delete(file);
            sound.reload(directory);
        }
    }
}
