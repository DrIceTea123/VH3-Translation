package com.dricetea.vh3patch.modules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class VaultXpModuleTest {
    enum Rarity { COMMON }
    enum ChestType { WOODEN }
    @Test void exactEnglishKeysFallbackAndReloadRemainIndependent(@TempDir Path dir) throws Exception {
        assertThrows(IllegalStateException.class, () -> VaultXpModule.INSTANCE.initialize(dir));
        Path file = dir.resolve("vault_xp.json");
        Files.writeString(file, "{\"Common Wooden Chest\":\"普通木制宝箱\",\"Ore Benitoite\":\"蓝锥矿石\",\"Cow\":\"经验牛\",\"Bonus\":\"\"}");
        Files.writeString(dir.resolve("combat_stats.json"), "{\"cow\":\"结算牛\"}");
        VaultXpModule.INSTANCE.initialize(dir);
        CombatStatsModule.INSTANCE.initialize(dir);
        assertEquals("普通木制宝箱", VaultXpModule.translate("Common Wooden Chest"));
        assertEquals("普通木制宝箱", VaultXpModule.translateChest("Common 木制 Chest", Rarity.COMMON, ChestType.WOODEN, false));
        assertEquals("Common 木制 Barrel", VaultXpModule.translateChest("Common 木制 Barrel", Rarity.COMMON, ChestType.WOODEN, true));
        assertEquals("蓝锥矿石", VaultXpModule.translate("Ore Benitoite"));
        assertEquals("经验牛", VaultXpModule.translate("Cow"));
        assertEquals("", VaultXpModule.translate("Bonus"));
        for (String unknown : new String[]{"cow", "minecraft:cow", "Common Wooden Barrel", "Unknown Ore"})
            assertEquals(unknown, VaultXpModule.translate(unknown));
        assertNull(VaultXpModule.translate(null));
        Files.writeString(file, "{\"Cow\":\"新经验牛\"}");
        VaultXpModule.INSTANCE.reload(dir);
        assertEquals("新经验牛", VaultXpModule.translate("Cow"));
        assertEquals("结算牛", CombatStatsModule.translate("minecraft:cow", "Cow"));
        assertEquals("Common Wooden Chest", VaultXpModule.translate("Common Wooden Chest"));
        Files.writeString(file, "broken");
        assertThrows(IOException.class, () -> VaultXpModule.INSTANCE.reload(dir));
        assertEquals("新经验牛", VaultXpModule.translate("Cow"));
    }
}
