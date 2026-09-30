package com.dricetea.vh3patch.transformer;

import com.google.gson.JsonArray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VpCompatibilityTest {
    private final PatchSpec spec = PatchSpec.load();
    private final Path vpSource = Path.of(System.getProperty("vh3.test.vpSource"));

    @Test void originalConflictFailsAndCompatibilityPreservesOtherRules(@TempDir Path temp) throws Exception {
        JsonArray original = PatchTool.readJson(vpSource).getAsJsonArray();
        JsonArray compatible = VpCompatibility.withoutOwnedMethod(original, spec);
        assertEquals(original.size() - 1, compatible.size());
        int i = 0;
        for (var rule : original) {
            if (!rule.isJsonObject() || !VpCompatibility.ownsMethod(rule.getAsJsonObject(), spec)) {
                assertEquals(rule, compatible.get(i++));
            }
        }
        Path config = temp.resolve("rules.json");
        Files.writeString(config, original.toString());
        assertThrows(IllegalStateException.class, () -> VpCompatibility.assertCompatible(temp, spec));
        Files.writeString(config, compatible.toString());
        assertDoesNotThrow(() -> VpCompatibility.assertCompatible(temp, spec));
    }

    @Test void rerunningMigrationOnAlreadyMigratedSourceFails() throws Exception {
        JsonArray compatible = VpCompatibility.withoutOwnedMethod(PatchTool.readJson(vpSource).getAsJsonArray(), spec);
        assertThrows(IllegalStateException.class, () -> VpCompatibility.withoutOwnedMethod(compatible, spec));
    }
}
