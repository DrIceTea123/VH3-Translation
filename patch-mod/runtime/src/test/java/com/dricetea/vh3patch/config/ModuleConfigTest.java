package com.dricetea.vh3patch.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ModuleConfigTest {
    @TempDir Path directory;

    @Test void firstLoadCreatesUtf8DefaultsAndSubsequentLoadPreservesEdits() throws Exception {
        var config = new ModuleConfig("example", "{\"Aggressive Cow\":\"战斗牛\"}");
        config.reload(directory);
        Path file = directory.resolve("example.json");
        assertEquals("{\"Aggressive Cow\":\"战斗牛\"}", Files.readString(file));
        Files.writeString(file, "{\"Aggressive Cow\":\"修改后的牛\"}");
        assertEquals("战斗牛", config.get("Aggressive Cow"));
        config.reload(directory);
        assertEquals("修改后的牛", config.get("Aggressive Cow"));
        assertNull(config.get("aggressive_cow"));
        assertEquals("{\"Aggressive Cow\":\"修改后的牛\"}", Files.readString(file));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "{\"cow\":3}", "{\"cow\":null}", "{\"cow\":true}",
            "{\"cow\":{}}", "{\"cow\":\"a\",\"cow\":\"b\"}", "{\"cow\":\"a\",}",
            "{cow:'a'}", "{\"cow\":\"a\"} {}", "{\"cow\":\"a\",/*comment*/\"x\":\"b\"}",
            "{\"cow\":\"a\",\"later\":"})
    void invalidReloadPreservesEntireLastValidSnapshot(String broken) throws Exception {
        var config = new ModuleConfig("example", "{\"cow\":\"默认值\"}");
        Path file = directory.resolve("example.json");
        Files.writeString(file, "{\"cow\":\"有效编辑\",\"extra\":\"保留\"}");
        config.reload(directory);
        Files.writeString(file, broken);
        assertThrows(IOException.class, () -> config.reload(directory));
        assertEquals("有效编辑", config.get("cow"));
        assertEquals("保留", config.get("extra"));
        assertEquals(broken, Files.readString(file));
        Files.writeString(file, "{\"cow\":\"修复\"}");
        config.reload(directory);
        assertEquals("修复", config.get("cow"));
        assertNull(config.get("extra"));
    }

    @Test void invalidFirstLoadKeepsBundledDefaultsAndDoesNotOverwriteFile() throws Exception {
        var config = new ModuleConfig("example", "{\"key\":\"内置\"}");
        Files.writeString(directory.resolve("example.json"), "broken");
        assertThrows(IOException.class, () -> config.reload(directory));
        assertEquals("内置", config.get("key"));
        assertEquals("broken", Files.readString(directory.resolve("example.json")));
    }

    @Test void modulesWithSameKeyStayIndependent() throws Exception {
        var first = new ModuleConfig("first", "{\"Cow\":\"牛一\"}");
        var second = new ModuleConfig("second", "{\"Cow\":\"牛二\"}");
        first.reload(directory);
        second.reload(directory);
        Files.writeString(directory.resolve("first.json"), "{\"Cow\":\"新译名\"}");
        first.reload(directory);
        assertEquals("新译名", first.get("Cow"));
        assertEquals("牛二", second.get("Cow"));
    }

    @Test void emptyObjectRemovesOverridesAndEmptyStringIsAnExplicitValue() throws Exception {
        var config = new ModuleConfig("example", "{\"Cow\":\"牛\"}");
        Files.writeString(directory.resolve("example.json"), "{\"Cow\":\"\"}");
        config.reload(directory);
        assertEquals("", config.get("Cow"));
        Files.writeString(directory.resolve("example.json"), "{}");
        config.reload(directory);
        assertNull(config.get("Cow"));
        assertNull(config.get(null));
    }
}
