package com.dricetea.vh3patch.transformer;

import com.dricetea.vh3patch.transformer.modules.MobNamesModule;
import me.fengming.vaultpatcher_asm.config.VaultPatcherConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VpEnabledModulesTest {
    @TempDir Path game;
    Path directory;
    Path modules;
    @org.junit.jupiter.api.BeforeEach void setup() throws IOException {
        directory=game.resolve("config/vaultpatcher_asm");modules=game.resolve("vaultpatcher/modules");
        Files.createDirectories(directory);Files.createDirectories(modules);
    }
    private final PatchModule module=new MobNamesModule();
    private void settings(String text) throws IOException {Files.writeString(directory.resolve("config.json"),text);}
    private void conflict(String filename) throws IOException {
        Path file=modules.resolve(filename);Files.createDirectories(file.getParent());
        Files.copy(Path.of(System.getProperty("vh3.test.vpSource")),file);
    }
    @Test void unlistedConflictsAndMalformedBackupsAreIgnored() throws Exception {
        settings("{\"modules\":[\"active\"]}");Files.writeString(modules.resolve("active.json"),"[]");
        conflict("old.json");conflict("backup/old.json");Files.writeString(modules.resolve("broken.json"),"not json");
        assertDoesNotThrow(()->VpCompatibility.assertCompatible(directory,module));
        settings("{\"modules\":[\"active\",\"old\"]}");
        assertThrows(IllegalStateException.class,()->VpCompatibility.assertCompatible(directory,module));
    }
    @Test void explicitNestedModuleIsCheckedWithoutScanningItsSiblings() throws Exception {
        conflict("nested/active.json");Files.writeString(modules.resolve("nested/broken.json"),"not json");
        settings("{\"modules\":[\"nested/active\"]}");
        assertThrows(IllegalStateException.class,()->VpCompatibility.assertCompatible(directory,module));
    }
    @Test void absentOrEmptyLoadingListDoesNotEnableLooseFiles() throws Exception {
        conflict("old.json");assertDoesNotThrow(()->VpCompatibility.assertCompatible(directory,module));
        for(String config:List.of("{}","{\"modules\":[]}","{\"m\":[]}")) {
            settings(config);assertDoesNotThrow(()->VpCompatibility.assertCompatible(directory,module));
        }
    }
    @Test void obsoleteAliasesAndFilenameSuffixMatchActualVpConfigReader() throws Exception {
        var oldMods=VaultPatcherConfig.mods;var oldPath=VaultPatcherConfig.config;var oldFile=VaultPatcherConfig.configFile;
        try {
            for(String config:List.of("{\"modules\":[\"old\"],\"m\":[\"nested/live\",\"literal.json\"]}","{\"m\":[\"old\"],\"modules\":[\"live\"]}","{\"modules\":[\"old\"],\"modules\":[\"live\"]}")) {
                settings(config);VaultPatcherConfig.mods=new ArrayList<>();VaultPatcherConfig.readConfig(directory);
                assertEquals(VaultPatcherConfig.getMods().stream().map(n->modules.resolve(n+".json")).toList(),VpCompatibility.enabledModuleFiles(directory));
            }
        } finally {VaultPatcherConfig.mods=oldMods;VaultPatcherConfig.config=oldPath;VaultPatcherConfig.configFile=oldFile;}
    }
    @Test void malformedEnabledModuleStillBlocksValidation() throws Exception {
        settings("{\"modules\":[\"broken\"]}");Files.writeString(modules.resolve("broken.json"),"{ broken");
        assertThrows(IllegalStateException.class,()->VpCompatibility.assertCompatible(directory,module));
    }
    @Test void missingEnabledFileIsEmptyAndPreflightDoesNotCreateIt() throws Exception {
        settings("{\"modules\":[\"missing\"]}");conflict("old.json");
        assertDoesNotThrow(()->VpCompatibility.assertCompatible(directory,module));assertFalse(Files.exists(modules.resolve("missing.json")));
    }
    @Test void invalidLoadingConfigurationIsNotSilentlyTreatedAsDisabled() throws Exception {
        for(String config:List.of("{ broken","{\"modules\":null}","{\"modules\":[null]}","{} {}")) {
            settings(config);assertThrows(IOException.class,()->VpCompatibility.assertCompatible(directory,module));
        }
    }
    @Test void loadAllScansOnlyTopLevelAndIgnoresExplicitListAndLegacyBackups() throws Exception {
        conflict("nested/old.json");Files.writeString(modules.resolve("active.json"),"[]");
        Files.copy(Path.of(System.getProperty("vh3.test.vpSource")),directory.resolve("old.json"));
        settings("{\"modules\":[\"nested/old\",\"old\"],\"load_all_modules\":true}");
        assertEquals(List.of(modules.resolve("active.json")),VpCompatibility.enabledModuleFiles(directory));
        assertDoesNotThrow(()->VpCompatibility.assertCompatible(directory,module));
        conflict("old.json");assertThrows(IllegalStateException.class,()->VpCompatibility.assertCompatible(directory,module));
    }
    @Test void selectedModernFileWinsAndMissingModernFileFallsBackToLegacyBasename() throws Exception {
        Files.copy(Path.of(System.getProperty("vh3.test.vpSource")),directory.resolve("old.json"));
        settings("{\"modules\":[\"nested/old\"]}");
        assertEquals(List.of(directory.resolve("old.json")),VpCompatibility.enabledModuleFiles(directory));
        assertThrows(IllegalStateException.class,()->VpCompatibility.assertCompatible(directory,module));
        Files.createDirectories(modules.resolve("nested"));Files.writeString(modules.resolve("nested/old.json"),"[]");
        assertDoesNotThrow(()->VpCompatibility.assertCompatible(directory,module));
        assertTrue(Files.exists(directory.resolve("old.json"))); // read-only preflight
    }
    @Test void configMetadataIsNotASelectedPatch() throws Exception {
        settings("{\"modules\":[],\"notes\":"+Files.readString(Path.of(System.getProperty("vh3.test.vpSource")))+"}");
        assertDoesNotThrow(()->VpCompatibility.assertCompatible(directory,module));
    }
}
