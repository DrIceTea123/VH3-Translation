package com.dricetea.vh3patch.transformer;

import com.dricetea.vh3patch.transformer.modules.CombatStatsModule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.dricetea.vh3patch.transformer.modules.SoundNamesModule;
import com.dricetea.vh3patch.transformer.modules.ResearchNamesModule;
import com.dricetea.vh3patch.transformer.modules.VaultXpModule;
import java.util.List;
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
        if (module instanceof VaultXpModule) {
            Files.writeString(config, "[{\"target_class\":{\"name\":\"" + VaultXpModule.TARGET
                    + "\",\"method\":\"formatMobName\"},\"key\":\"Cow\",\"value\":\"Conflict\"}]");
        } else Files.copy(Path.of(System.getProperty("vh3.test.legacyVpDirectory"), module.spec().moduleId() + ".json"), config);
        assertThrows(IllegalStateException.class, () -> TranslationTransformationService.preflight(game, fixture));
    }

    @Test void dedicatedServerNeedsOnlyCommonTargets(@TempDir Path game) throws Exception {
        var module = new ResearchNamesModule();
        PatchModule fixture = targetFixture(game, module, false);
        companion(game, module.spec(), module.spec().patchVersion());
        assertDoesNotThrow(() -> TranslationTransformationService.preflight(game, fixture, false));
        assertThrows(Exception.class, () -> TranslationTransformationService.preflight(game, fixture, true));
        assertEquals(List.of("research_names"), PatchModules.active(false).stream().map(m -> m.spec().moduleId()).toList());
        assertEquals(2, PatchModules.byClass(false).size());
        assertTrue(PatchModules.byClass(false).keySet().stream().noneMatch(n -> n.contains("/client/")));
    }

    private PatchModule targetFixture(Path game, PatchModule module) throws Exception { return targetFixture(game, module, true); }
    private PatchModule targetFixture(Path game, PatchModule module, boolean client) throws Exception {
        Files.createDirectories(game.resolve("mods"));
        Path jar = game.resolve("mods/target.jar");
        try (var output = new JarOutputStream(Files.newOutputStream(jar))) {
            for (String name : module.specs(client).stream().map(PatchSpec::className).distinct().toList()) {
                var production = module.specs().stream().filter(s -> s.className().equals(name)).findFirst().orElseThrow();
                var node = TargetJar.read(Path.of(System.getProperty("vh3.test.targetJar")), production, true);
                ClassWriter writer = new ClassWriter(0);
                node.accept(writer);
                output.putNextEntry(new JarEntry(name + ".class"));
                output.write(writer.toByteArray());
                output.closeEntry();
            }
        }
        String hash = MethodFingerprint.sha256(Files.readAllBytes(jar));
        var fixtures = module.specs().stream().map(p -> new PatchSpec(p.patchVersion(), p.targetVersion(), hash,
                p.className(), p.methodName(), p.descriptor(), p.fingerprint(), p.returnCount(),
                p.moduleId(), p.helperClass(), p.ownedLiterals(), p.side())).toList();
        if (module instanceof ResearchNamesModule) return new ResearchNamesModule(fixtures);
        if (module instanceof VaultXpModule) return new VaultXpModule(fixtures);
        return module instanceof CombatStatsModule ? new CombatStatsModule(fixtures.get(0)) : new SoundNamesModule(fixtures.get(0));
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
