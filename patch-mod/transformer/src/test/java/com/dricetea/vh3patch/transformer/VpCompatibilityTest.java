package com.dricetea.vh3patch.transformer;

import com.dricetea.vh3patch.transformer.modules.MobNamesModule;

import com.google.gson.JsonArray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VpCompatibilityTest {
    private final MobNamesModule module = new MobNamesModule();
    private final Path vpSource = Path.of(System.getProperty("vh3.test.vpSource"));

    @Test void originalConflictFailsAndCompatibilityPreservesOtherRules(@TempDir Path temp) throws Exception {
        JsonArray original = JsonFiles.read(vpSource).getAsJsonArray();
        JsonArray compatible = VpCompatibility.withoutOwnedMethod(original, module);
        assertEquals(original.size() - 1, compatible.size());
        int i = 0;
        for (var rule : original) {
            if (!rule.isJsonObject() || !module.ownsVpRule(rule.getAsJsonObject())) {
                assertEquals(rule, compatible.get(i++));
            }
        }
        Path config = temp.resolve("rules.json");
        Files.writeString(config, original.toString());
        assertThrows(IllegalStateException.class, () -> VpCompatibility.assertCompatible(temp, module));
        Files.writeString(config, compatible.toString());
        assertDoesNotThrow(() -> VpCompatibility.assertCompatible(temp, module));
    }

    @Test void rerunningMigrationOnAlreadyMigratedSourceFails() throws Exception {
        JsonArray compatible = VpCompatibility.withoutOwnedMethod(JsonFiles.read(vpSource).getAsJsonArray(), module);
        assertThrows(IllegalStateException.class, () -> VpCompatibility.withoutOwnedMethod(compatible, module));
    }

    @Test void currentProgramIsCompatibleAndKeepsTakeoverComment() throws Exception {
        Path directory = Path.of(System.getProperty("vh3.test.programVpDirectory"));
        assertDoesNotThrow(() -> VpCompatibility.assertCompatible(directory, module));
        JsonArray current = JsonFiles.read(directory.resolve("the_vault-asm_complex.json")).getAsJsonArray();
        assertEquals(current, VpCompatibility.prepareConfiguration(current, module));
        assertTrue(java.util.stream.StreamSupport.stream(current.spliterator(), false).anyMatch(item -> item.isJsonObject()
                && item.getAsJsonObject().entrySet().stream().anyMatch(e -> e.getKey().startsWith("_comment")
                && e.getValue().getAsString().contains("VTP") && e.getValue().getAsString().contains("mob_names"))));
    }

    @Test void publishedPreparationRemovesSingleConflictButRejectsDuplicates() throws Exception {
        JsonArray source = JsonFiles.read(vpSource).getAsJsonArray();
        assertEquals(VpCompatibility.withoutOwnedMethod(source, module), VpCompatibility.prepareConfiguration(source, module));
        JsonArray duplicate = source.deepCopy();
        duplicate.addAll(source.deepCopy());
        assertThrows(IllegalStateException.class, () -> VpCompatibility.prepareConfiguration(duplicate, module));
    }
}
