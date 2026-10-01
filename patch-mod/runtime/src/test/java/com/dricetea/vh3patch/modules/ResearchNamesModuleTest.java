package com.dricetea.vh3patch.modules;

import com.dricetea.vh3patch.module.CommonModules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ResearchNamesModuleTest {
    @Test void commonStartupNeedsOnlyResearchAndNewEntriesNeedNoCode(@TempDir Path dir) throws Exception {
        assertEquals(List.of("research_names"), CommonModules.all().stream().map(m -> m.id()).toList());
        assertThrows(IllegalStateException.class, () -> CommonModules.initialize(dir));
        Files.writeString(dir.resolve("research_names.json"), "{\"New Custom Research\":\"新研究\"}");
        CommonModules.initialize(dir);
        assertEquals("新研究", ResearchNamesModule.translate("New Custom Research"));
        assertEquals("new custom research", ResearchNamesModule.translate("new custom research"));
        assertEquals("Original Title", ResearchNamesModule.translate("original title", "Original Title"));
        assertNull(ResearchNamesModule.translate(null));
        Files.writeString(dir.resolve("research_names.json"), "{\"New Custom Research\":\"新译名\"}");
        ResearchNamesModule.INSTANCE.reload(dir);
        assertEquals("新译名", ResearchNamesModule.translate("New Custom Research", "Fallback"));
        Files.writeString(dir.resolve("research_names.json"), "broken");
        assertThrows(IOException.class, () -> ResearchNamesModule.INSTANCE.reload(dir));
        assertEquals("新译名", ResearchNamesModule.translate("New Custom Research"));
    }
}
