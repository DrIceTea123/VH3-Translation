package com.dricetea.vh3patch.transformer;

import org.junit.jupiter.api.Test;
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
    private final PatchSpec production = PatchSpec.load();

    @Test void matchingPairAndTargetPass(@TempDir Path game) throws Exception {
        PatchSpec fixture = targetFixture(game);
        companion(game, production.patchVersion());
        assertDoesNotThrow(() -> TranslationTransformationService.preflight(game, fixture));
    }

    @Test void missingCompanionBlocksStartup(@TempDir Path game) throws Exception {
        PatchSpec fixture = targetFixture(game);
        assertThrows(IllegalStateException.class, () -> TranslationTransformationService.preflight(game, fixture));
    }

    @Test void mismatchedCompanionBlocksStartup(@TempDir Path game) throws Exception {
        PatchSpec fixture = targetFixture(game);
        companion(game, "wrong-version");
        assertThrows(IllegalStateException.class, () -> TranslationTransformationService.preflight(game, fixture));
    }

    @Test void duplicateTargetBlocksStartup(@TempDir Path game) throws Exception {
        PatchSpec fixture = targetFixture(game);
        companion(game, production.patchVersion());
        Files.copy(game.resolve("mods/target.jar"), game.resolve("mods/duplicate.jar"));
        assertThrows(IllegalStateException.class, () -> TranslationTransformationService.preflight(game, fixture));
    }

    @Test void vpConflictBlocksStartup(@TempDir Path game) throws Exception {
        PatchSpec fixture = targetFixture(game);
        companion(game, production.patchVersion());
        Path config = game.resolve("config/vaultpatcher_asm/rules.json");
        Files.createDirectories(config.getParent());
        Files.copy(Path.of(System.getProperty("vh3.test.vpSource")), config);
        assertThrows(IllegalStateException.class, () -> TranslationTransformationService.preflight(game, fixture));
    }

    private PatchSpec targetFixture(Path game) throws Exception {
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
        return new PatchSpec(production.patchVersion(), production.targetVersion(),
                MethodFingerprint.sha256(Files.readAllBytes(jar)), production.className(), production.methodName(),
                production.descriptor(), production.fingerprint(), production.returnCount());
    }

    private void companion(Path game, String version) throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("VH3-Patch-Runtime", version);
        try (var output = new JarOutputStream(Files.newOutputStream(game.resolve("mods/runtime.jar")), manifest)) {
            for (String name : new String[]{PatchSpec.HELPER + ".class", "META-INF/mods.toml",
                    "assets/vh3_translation_patch/lang/zh_cn.json"}) {
                output.putNextEntry(new JarEntry(name));
                output.write(new byte[]{0});
                output.closeEntry();
            }
        }
    }
}
