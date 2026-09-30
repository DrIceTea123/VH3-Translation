package com.dricetea.vh3patch.transformer;

import com.dricetea.vh3patch.transformer.modules.CombatStatsModule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.dricetea.vh3patch.transformer.modules.SoundNamesModule;
import java.util.stream.Stream;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

class PreflightTest {
    static Stream<PatchModule> modules() { return PatchModules.all().stream(); }

    @ParameterizedTest @MethodSource("modules") void matchingPairAndTargetPass(PatchModule module, @TempDir Path game) throws Exception {
        PatchModule fixture = targetFixture(game, module);
        companion(game, module.spec(), module.spec().patchVersion());
        assertDoesNotThrow(() -> TranslationTransformationService.preflight(game, fixture));
    }

    @ParameterizedTest @MethodSource("modules") void missingCompanionBlocksStartup(PatchModule module, @TempDir Path game) throws Exception {
        PatchModule fixture = targetFixture(game, module);
        assertThrows(IllegalStateException.class, () -> TranslationTransformationService.preflight(game, fixture));
    }

    @ParameterizedTest @MethodSource("modules") void mismatchedCompanionBlocksStartup(PatchModule module, @TempDir Path game) throws Exception {
        PatchModule fixture = targetFixture(game, module);
        companion(game, module.spec(), "wrong-version");
        assertThrows(IllegalStateException.class, () -> TranslationTransformationService.preflight(game, fixture));
    }

    @ParameterizedTest @MethodSource("modules") void duplicateTargetBlocksStartup(PatchModule module, @TempDir Path game) throws Exception {
        PatchModule fixture = targetFixture(game, module);
        companion(game, module.spec(), module.spec().patchVersion());
        Files.copy(game.resolve("mods/target.jar"), game.resolve("mods/duplicate.jar"));
        assertThrows(IllegalStateException.class, () -> TranslationTransformationService.preflight(game, fixture));
    }

    @ParameterizedTest @MethodSource("modules") void vpConflictBlocksStartup(PatchModule module, @TempDir Path game) throws Exception {
        PatchModule fixture = targetFixture(game, module);
        companion(game, module.spec(), module.spec().patchVersion());
        Path config = game.resolve("config/vaultpatcher_asm/rules.json");
        Files.createDirectories(config.getParent());
        Files.copy(Path.of(System.getProperty("vh3.test.legacyVpDirectory"), module.spec().moduleId() + ".json"), config);
        assertThrows(IllegalStateException.class, () -> TranslationTransformationService.preflight(game, fixture));
    }

    private PatchModule targetFixture(Path game, PatchModule module) throws Exception {
        PatchSpec production = module.spec();
        Files.createDirectories(game.resolve("mods"));
        var node = TargetJar.read(Path.of(System.getProperty("vh3.test.targetJar")), production, true);
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        Path jar = game.resolve("mods/target.jar");
        try (var output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry(production.className() + ".class"));
            output.write(writer.toByteArray());
            output.closeEntry();
        }
        PatchSpec fixture = new PatchSpec(production.patchVersion(), production.targetVersion(),
                MethodFingerprint.sha256(Files.readAllBytes(jar)), production.className(), production.methodName(),
                production.descriptor(), production.fingerprint(), production.returnCount(),
                production.moduleId(), production.helperClass(), production.ownedLiterals());
        return module instanceof CombatStatsModule ? new CombatStatsModule(fixture) : new SoundNamesModule(fixture);
    }

    private void companion(Path game, PatchSpec production, String version) throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("VH3-Patch-Runtime", version);
        try (var output = new JarOutputStream(Files.newOutputStream(game.resolve("mods/runtime.jar")), manifest)) {
            for (String name : new String[]{production.helperClass() + ".class", "META-INF/mods.toml"}) {
                output.putNextEntry(new JarEntry(name));
                output.write(new byte[]{0});
                output.closeEntry();
            }
        }
    }
}
