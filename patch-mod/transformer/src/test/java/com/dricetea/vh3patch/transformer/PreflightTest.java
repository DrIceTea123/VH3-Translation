package com.dricetea.vh3patch.transformer;

import com.dricetea.vh3patch.transformer.modules.MobNamesModule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.dricetea.vh3patch.transformer.modules.SoundNamesModule;
import com.dricetea.vh3patch.transformer.modules.ResearchNamesModule;
import com.dricetea.vh3patch.transformer.modules.ChestNamesModule;
import com.dricetea.vh3patch.transformer.modules.CardTextModule;
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

    @ParameterizedTest @MethodSource("modules") void matchingEmbeddedRuntimeAndTargetPass(PatchModule module, @TempDir Path game) throws Exception {
        PatchModule fixture = targetFixture(game, module);
        companion(game, module.spec(), module.spec().patchVersion());
        assertDoesNotThrow(() -> TranslationTransformationService.preflight(game, fixture, true, game.resolve("runtime.jar")));
    }

    @ParameterizedTest @MethodSource("modules") void missingEmbeddedRuntimeBlocksStartup(PatchModule module, @TempDir Path game) throws Exception {
        PatchModule fixture = targetFixture(game, module);
        assertThrows(java.io.IOException.class, () -> TranslationTransformationService.preflight(game, fixture, true, game.resolve("runtime.jar")));
    }

    @ParameterizedTest @MethodSource("modules") void mismatchedEmbeddedRuntimeBlocksStartup(PatchModule module, @TempDir Path game) throws Exception {
        PatchModule fixture = targetFixture(game, module);
        companion(game, module.spec(), "wrong-version");
        assertThrows(java.io.IOException.class, () -> TranslationTransformationService.preflight(game, fixture, true, game.resolve("runtime.jar")));
    }

    @ParameterizedTest @MethodSource("modules") void duplicateTargetBlocksStartup(PatchModule module, @TempDir Path game) throws Exception {
        PatchModule fixture = targetFixture(game, module);
        companion(game, module.spec(), module.spec().patchVersion());
        Files.copy(game.resolve("mods/target.jar"), game.resolve("mods/duplicate.jar"));
        assertThrows(IllegalStateException.class, () -> TranslationTransformationService.preflight(game, fixture, true, game.resolve("runtime.jar")));
    }

    @ParameterizedTest @MethodSource("modules") void vpConflictBlocksStartup(PatchModule module, @TempDir Path game) throws Exception {
        PatchModule fixture = targetFixture(game, module);
        companion(game, module.spec(), module.spec().patchVersion());
        Path config = game.resolve("config/vaultpatcher_asm/rules.json");
        Files.createDirectories(config.getParent());
        Files.writeString(config.getParent().resolve("config.json"),"{\"modules\":[\"rules\"]}");
        if(module.spec().moduleId().equals("theme_names"))
            Files.writeString(config,"[{\"target_class\":{\"name\":\"iskallia.vault.core.data.key.ThemeKey\",\"method\":\"getName\"},\"key\":\"Example\",\"value\":\"示例\"}]");
        else Files.copy(Path.of(System.getProperty("vh3.test.legacyVpDirectory"), module.spec().moduleId() + ".json"), config);
        assertThrows(IllegalStateException.class, () -> TranslationTransformationService.preflight(game, fixture, true, game.resolve("runtime.jar")));
    }

    @Test void dedicatedServerNeedsOnlyCommonTargets(@TempDir Path game) throws Exception {
        var module = new ResearchNamesModule();
        PatchModule fixture = targetFixture(game, module, false);
        companion(game, module.spec(), module.spec().patchVersion());
        assertDoesNotThrow(() -> TranslationTransformationService.preflight(game, fixture, false, game.resolve("runtime.jar")));
        assertThrows(IllegalStateException.class, () -> TranslationTransformationService.preflight(game, fixture, true, game.resolve("runtime.jar")));
        assertEquals(java.util.Set.of("research_names", "chest_names", "card_text", "theme_names", "gear_rarity", "quest_names", "gear_affixes", "talent_affixes"), new java.util.HashSet<>(PatchModules.active(false).stream().map(m -> m.spec().moduleId()).toList()));
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
        if (module instanceof CardTextModule) return new CardTextModule(fixtures);
        if (module instanceof ChestNamesModule) return new ChestNamesModule(fixtures);
        if (module instanceof DisplayMethodPatch) return module.getClass().getConstructor(List.class).newInstance(fixtures);
        return module instanceof MobNamesModule ? new MobNamesModule(fixtures) : new SoundNamesModule(fixtures.get(0));
    }

    private void companion(Path game, PatchSpec production, String version) throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("VH3-Patch-Runtime", version);
        try (var output = new JarOutputStream(Files.newOutputStream(game.resolve("runtime.jar")), manifest)) {
            for (String name : Stream.concat(PatchModules.all().stream().filter(m -> m.spec().moduleId().equals(production.moduleId())).flatMap(m -> m.specs().stream()).map(p -> p.helperClass() + ".class"), Stream.of("META-INF/mods.toml")).distinct().toList()) {
                output.putNextEntry(new JarEntry(name));
                output.write(new byte[]{0});
                output.closeEntry();
            }
        }
    }
}
