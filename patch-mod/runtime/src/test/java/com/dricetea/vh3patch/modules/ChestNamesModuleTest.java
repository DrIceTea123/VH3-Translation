package com.dricetea.vh3patch.modules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class ChestNamesModuleTest {
    enum Rarity { COMMON, RARE }
    enum ChestType { WOODEN, TREASURE }
    @Test void sharedNamesOverridesMissingPartsAndReload(@TempDir Path dir) throws Exception {
        assertThrows(IllegalStateException.class, () -> ChestNamesModule.INSTANCE.initialize(dir));
        Path file = dir.resolve("chest_names.json");
        Files.writeString(file, "{\"Common\":\"普通\",\"Common: \":\"普通：\",\"Wooden\":\"木制\",\"Wooden Chest\":\"木制宝箱\",\"Wooden Barrel\":\"木制储物桶\",\"Ornate Strongbox\":\"强化华丽宝箱\",\"Common Treasure Barrel\":\"Common Treasure Barrel\"}");
        ChestNamesModule.INSTANCE.initialize(dir);
        assertEquals("木制", ChestNamesModule.translate("Wooden"));
        assertEquals("普通木制宝箱", ChestNamesModule.translateChest("Common 木制 Chest", Rarity.COMMON, ChestType.WOODEN, false));
        assertEquals("普通木制储物桶", ChestNamesModule.translateChest("mixed", Rarity.COMMON, ChestType.WOODEN, true));
        assertEquals("木制储物桶", ChestNamesModule.translateBarrel("木制 Barrel", ChestType.WOODEN));
        assertEquals("普通强化华丽宝箱", ChestNamesModule.translate("Common Ornate Strongbox"));
        assertEquals("Common Treasure Barrel", ChestNamesModule.translateChest("mixed", Rarity.COMMON, ChestType.TREASURE, true));
        assertEquals("Rare 木制 Chest", ChestNamesModule.translateChest("Rare 木制 Chest", Rarity.RARE, ChestType.WOODEN, false));
        assertEquals("木制宝箱", ChestNamesModule.translateHunter("WOODEN"));
        assertEquals("COINS", ChestNamesModule.translateHunter("COINS"));
        assertEquals("钱币堆", ChestNamesModule.translateHunter("钱币堆"));
        assertEquals("普通：24 (480 xp)", ChestNamesModule.translateRarityCount("Common: 24 (480 xp)"));
        assertEquals("Rare: 3", ChestNamesModule.translateRarityCount("Rare: 3"));
        assertEquals("Other", ChestNamesModule.translate("Other"));
        assertNull(ChestNamesModule.translate(null));
        Files.writeString(file, "{\"Common Wooden Chest\":\"自定义完整名\"}");
        ChestNamesModule.INSTANCE.reload(dir);
        assertEquals("自定义完整名", ChestNamesModule.translateChest("mixed", Rarity.COMMON, ChestType.WOODEN, false));
        Files.writeString(file, "broken");
        assertThrows(IOException.class, () -> ChestNamesModule.INSTANCE.reload(dir));
        assertEquals("自定义完整名", ChestNamesModule.translate("Common Wooden Chest"));
    }
}
