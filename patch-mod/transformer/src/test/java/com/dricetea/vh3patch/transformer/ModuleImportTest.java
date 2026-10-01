package com.dricetea.vh3patch.transformer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class ModuleImportTest {
    // 这里只测试单组历史导入；research_names 有专属测试，vault_xp 没有旧 VP 规则。
    static Stream<PatchModule> modules() { return Stream.of(PatchModules.find("combat_stats"), PatchModules.find("sound_names")); }

    @ParameterizedTest @MethodSource("modules")
    void importerWritesHistoricalCandidatesOutsideSource(PatchModule module, @TempDir Path output) throws Exception {
        String id = module.spec().moduleId();
        module.importMappings(Path.of(System.getProperty("vh3.test.legacyVpDirectory"), id + ".json"),
                Path.of(System.getProperty("vh3.test.targetJar")), output);
        var candidate = JsonFiles.read(output.resolve(module.spec().configPath())).getAsJsonObject();
        // 导入器只负责历史数据；人工维护的正式配置可以自由增删改，不要求与历史完全相等。
        assertEquals(id.equals("combat_stats") ? 235 : 203, candidate.size());
        var report = JsonFiles.read(output.resolve("translations/"
                + (id.equals("combat_stats") ? "mob-name-import.json" : "sound-name-import.json"))).getAsJsonObject();
        assertEquals(candidate.size(), report.get("importedCount").getAsInt());
        for (var pair : report.getAsJsonArray("originalPairs")) {
            var item = pair.getAsJsonObject();
            String original = item.get("key").getAsString();
            if (id.equals("combat_stats")) {
                String key = original.toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
                if (candidate.has(key)) assertEquals(item.get("value"), candidate.get(key));
                else assertTrue(original.equals("Black Widow Spider") || original.equals("Mummy"));
            } else {
                String field = report.getAsJsonObject("fieldToOriginalDisplayName").entrySet().stream()
                        .filter(entry -> entry.getValue().getAsString().equals(original)).findFirst().orElseThrow().getKey();
                assertEquals(item.get("value"), candidate.get(field));
            }
        }
        assertFalse(java.nio.file.Files.exists(output.resolve("runtime")));
    }
}
