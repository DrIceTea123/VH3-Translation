package com.dricetea.vh3patch.transformer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class ModuleImportTest {
    static Stream<PatchModule> modules() { return PatchModules.all().stream(); }

    @ParameterizedTest @MethodSource("modules")
    void moduleImporterReproducesCurrentDefaults(PatchModule module, @TempDir Path output) throws Exception {
        String id = module.spec().moduleId();
        module.importMappings(Path.of(System.getProperty("vh3.test.legacyVpDirectory"), id + ".json"),
                Path.of(System.getProperty("vh3.test.targetJar")), output);
        assertEquals(JsonFiles.read(Path.of(System.getProperty("vh3.test.defaultDirectory"), id + ".json")),
                JsonFiles.read(output.resolve("runtime/src/main/resources/" + module.spec().defaultConfigResource())));
    }
}
