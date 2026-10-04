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
    @TempDir Path directory;
    private final PatchModule module=new MobNamesModule();
    private void settings(String text) throws IOException {Files.writeString(directory.resolve("config.json"),text);}
    private void conflict(String filename) throws IOException {
        Path file=directory.resolve(filename);Files.createDirectories(file.getParent());
        Files.copy(Path.of(System.getProperty("vh3.test.vpSource")),file);
    }
    @Test void unlistedConflictsAndMalformedBackupsAreIgnored() throws Exception {
        settings("{\"mods\":[\"active\"]}");Files.writeString(directory.resolve("active.json"),"[]");
        conflict("old.json");conflict("backup/old.json");Files.writeString(directory.resolve("broken.json"),"not json");
        assertDoesNotThrow(()->VpCompatibility.assertCompatible(directory,module));
        settings("{\"mods\":[\"active\",\"old\"]}");
        assertThrows(IllegalStateException.class,()->VpCompatibility.assertCompatible(directory,module));
    }
    @Test void explicitNestedModuleIsCheckedWithoutScanningItsSiblings() throws Exception {
        conflict("nested/active.json");Files.writeString(directory.resolve("nested/broken.json"),"not json");
        settings("{\"mods\":[\"nested/active\"]}");
        assertThrows(IllegalStateException.class,()->VpCompatibility.assertCompatible(directory,module));
    }
    @Test void absentOrEmptyLoadingListDoesNotEnableLooseFiles() throws Exception {
        conflict("old.json");assertDoesNotThrow(()->VpCompatibility.assertCompatible(directory,module));
        for(String config:List.of("{}","{\"mods\":[]}","{\"m\":[]}")) {
            settings(config);assertDoesNotThrow(()->VpCompatibility.assertCompatible(directory,module));
        }
    }
    @Test void aliasOrderAndFilenameSuffixMatchActualVpConfigReader() throws Exception {
        var oldMods=VaultPatcherConfig.mods;var oldPath=VaultPatcherConfig.config;var oldFile=VaultPatcherConfig.configFile;
        try {
            for(String config:List.of("{\"mods\":[\"old\"],\"m\":[\"nested/live\",\"literal.json\"]}","{\"m\":[\"old\"],\"mods\":[\"live\"]}","{\"mods\":[\"old\"],\"mods\":[\"live\"]}")) {
                settings(config);VaultPatcherConfig.mods=new ArrayList<>();VaultPatcherConfig.readConfig(directory);
                assertEquals(VaultPatcherConfig.getMods().stream().map(n->directory.resolve(n+".json")).toList(),VpCompatibility.enabledModuleFiles(directory));
            }
        } finally {VaultPatcherConfig.mods=oldMods;VaultPatcherConfig.config=oldPath;VaultPatcherConfig.configFile=oldFile;}
    }
    @Test void malformedEnabledModuleStillBlocksValidation() throws Exception {
        settings("{\"mods\":[\"broken\"]}");Files.writeString(directory.resolve("broken.json"),"{ broken");
        assertThrows(IllegalStateException.class,()->VpCompatibility.assertCompatible(directory,module));
    }
    @Test void missingEnabledFileIsEmptyAndPreflightDoesNotCreateIt() throws Exception {
        settings("{\"mods\":[\"missing\"]}");conflict("old.json");
        assertDoesNotThrow(()->VpCompatibility.assertCompatible(directory,module));assertFalse(Files.exists(directory.resolve("missing.json")));
    }
    @Test void invalidLoadingConfigurationIsNotSilentlyTreatedAsDisabled() throws Exception {
        for(String config:List.of("{ broken","{\"mods\":null}","{\"mods\":[null]}","{} {}")) {
            settings(config);assertThrows(IOException.class,()->VpCompatibility.assertCompatible(directory,module));
        }
    }
    @Test void configMetadataIsNotASelectedPatch() throws Exception {
        settings("{\"mods\":[],\"notes\":"+Files.readString(Path.of(System.getProperty("vh3.test.vpSource")))+"}");
        assertDoesNotThrow(()->VpCompatibility.assertCompatible(directory,module));
    }
}
