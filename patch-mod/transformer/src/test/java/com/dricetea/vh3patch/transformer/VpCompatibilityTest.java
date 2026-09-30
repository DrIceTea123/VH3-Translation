package com.dricetea.vh3patch.transformer;

import com.dricetea.vh3patch.transformer.modules.CombatStatsModule;

import com.google.gson.JsonArray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VpCompatibilityTest {
    private final PatchSpec spec = new CombatStatsModule().spec();
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

    @Test void currentProgramIsCompatibleAndKeepsTakeoverComment() throws Exception {
        Path directory = Path.of(System.getProperty("vh3.test.programVpDirectory"));
        assertDoesNotThrow(() -> VpCompatibility.assertCompatible(directory, spec));
        JsonArray current = PatchTool.readJson(directory.resolve("the_vault-asm_complex.json")).getAsJsonArray();
        assertEquals(current, VpCompatibility.prepareConfiguration(current, spec));
        assertTrue(java.util.stream.StreamSupport.stream(current.spliterator(), false).anyMatch(item -> item.isJsonObject()
                && item.getAsJsonObject().has("_comment")
                && item.getAsJsonObject().get("_comment").getAsString().contains("VTP")
                && item.getAsJsonObject().get("_comment").getAsString().contains("combat_stats")));
    }

    @Test void publishedPreparationRemovesSingleConflictButRejectsDuplicates() throws Exception {
        JsonArray source = PatchTool.readJson(vpSource).getAsJsonArray();
        assertEquals(VpCompatibility.withoutOwnedMethod(source, spec), VpCompatibility.prepareConfiguration(source, spec));
        JsonArray duplicate = source.deepCopy();
        duplicate.addAll(source.deepCopy());
        assertThrows(IllegalStateException.class, () -> VpCompatibility.prepareConfiguration(duplicate, spec));
    }
}
