package com.dricetea.vh3patch.transformer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.util.jar.*;
import static org.junit.jupiter.api.Assertions.*;

class EmbeddedRuntimeTest {
    private static final String VERSION = "1.0.16";
    @TempDir Path game;

    @Test void extractsOutsideModsAndReusesVerifiedCache() throws Exception {
        byte[] bytes = runtime(VERSION, true); bundle(bytes, VERSION);
        Path path = resolve(bytes);
        assertArrayEquals(bytes, Files.readAllBytes(path));
        assertFalse(path.startsWith(game.resolve("mods")));
        var modified = Files.getLastModifiedTime(path);
        assertEquals(path, resolve(bytes));
        assertEquals(modified, Files.getLastModifiedTime(path));
        try (var paths = Files.list(game.resolve("mods"))) { assertEquals(1, paths.count()); }
    }

    @Test void repairsCorruptCache() throws Exception {
        byte[] bytes = runtime(VERSION, true); bundle(bytes, VERSION);
        Path path = resolve(bytes); Files.writeString(path, "broken");
        assertArrayEquals(bytes, Files.readAllBytes(resolve(bytes)));
    }

    @Test void rejectsWrongEmbeddedHashEvenWithValidCache() throws Exception {
        byte[] bytes = runtime(VERSION, true); bundle(bytes, VERSION); resolve(bytes);
        bundle(runtime("1.0.0", true), VERSION);
        assertTrue(assertThrows(IOException.class, () -> resolve(bytes)).getMessage().contains("SHA-256"));
    }

    @Test void rejectsInnerVersionMismatch() throws Exception {
        byte[] bytes = runtime("1.0.0", true); bundle(bytes, VERSION);
        assertThrows(IOException.class, () -> resolve(bytes));
    }

    @Test void rejectsMissingModsToml() throws Exception {
        byte[] bytes = runtime(VERSION, false); bundle(bytes, VERSION);
        assertThrows(IOException.class, () -> resolve(bytes));
    }

    @Test void rejectsOuterVersionMismatch() throws Exception {
        byte[] bytes = runtime(VERSION, true); bundle(bytes, "1.0.0");
        assertThrows(IOException.class, () -> resolve(bytes));
    }

    @Test void rejectsDuplicateBundleEvenWhenRenamed() throws Exception {
        byte[] bytes = runtime(VERSION, true); bundle(bytes, VERSION);
        Files.copy(game.resolve("mods/bundle.jar"), game.resolve("mods/renamed.jar"));
        assertTrue(assertThrows(IOException.class, () -> resolve(bytes)).getMessage().contains("found 2"));
    }

    @Test void rejectsLegacyRuntimeEvenWhenRenamed() throws Exception {
        byte[] bytes = runtime(VERSION, true); bundle(bytes, VERSION);
        Files.write(game.resolve("mods/renamed.jar"), bytes);
        assertTrue(assertThrows(IOException.class, () -> resolve(bytes)).getMessage().contains("old standalone"));
    }

    @Test void rejectsLegacyTransformerEvenWhenRenamed() throws Exception {
        byte[] bytes = runtime(VERSION, true); bundle(bytes, VERSION);
        try (var jar = new JarOutputStream(Files.newOutputStream(game.resolve("mods/old.jar")))) {
            entry(jar, EmbeddedRuntime.SERVICE_CLASS, new byte[]{0});
        }
        assertTrue(assertThrows(IOException.class, () -> resolve(bytes)).getMessage().contains("old standalone"));
    }

    @Test void cacheCreationFailureHasActionablePath() throws Exception {
        byte[] bytes = runtime(VERSION, true); bundle(bytes, VERSION);
        Files.writeString(game.resolve(".vh3_translation_patch"), "cannot be a directory");
        assertTrue(assertThrows(IOException.class, () -> resolve(bytes)).getMessage().contains("runtime cache"));
    }

    @Test void missingBundleFails() throws Exception {
        Files.createDirectories(game.resolve("mods"));
        assertTrue(assertThrows(IOException.class, () -> resolve(runtime(VERSION, true))).getMessage().contains("found 0"));
    }

    @Test void upgradeAndRollbackUseDifferentVerifiedCaches() throws Exception {
        byte[] old = runtime("1.0.15", true), current = runtime(VERSION, true);
        bundle(old, "1.0.15");
        Path oldPath = EmbeddedRuntime.resolve(game, "1.0.15", MethodFingerprint.sha256(old));
        bundle(current, VERSION); Path newPath = resolve(current);
        assertNotEquals(oldPath, newPath); assertTrue(Files.exists(oldPath));
        bundle(old, "1.0.15");
        assertEquals(oldPath, EmbeddedRuntime.resolve(game, "1.0.15", MethodFingerprint.sha256(old)));
    }

    private Path resolve(byte[] bytes) throws IOException { return EmbeddedRuntime.resolve(game, VERSION, MethodFingerprint.sha256(bytes)); }
    private void bundle(byte[] runtime, String version) throws IOException {
        Files.createDirectories(game.resolve("mods"));
        Manifest manifest = manifest(); manifest.getMainAttributes().putValue("Implementation-Version", version);
        try (var jar = new JarOutputStream(Files.newOutputStream(game.resolve("mods/bundle.jar")), manifest)) {
            entry(jar, EmbeddedRuntime.SERVICE_CLASS, new byte[]{0}); entry(jar, EmbeddedRuntime.ENTRY, runtime);
        }
    }
    private static byte[] runtime(String version, boolean modsToml) throws IOException {
        Manifest manifest = manifest(); manifest.getMainAttributes().putValue("VH3-Patch-Runtime", version);
        var bytes = new ByteArrayOutputStream();
        try (var jar = new JarOutputStream(bytes, manifest)) {
            if (modsToml) entry(jar, "META-INF/mods.toml", new byte[]{0});
        }
        return bytes.toByteArray();
    }
    private static Manifest manifest() {
        var manifest = new Manifest(); manifest.getMainAttributes().putValue("Manifest-Version", "1.0"); return manifest;
    }
    private static void entry(JarOutputStream jar, String name, byte[] bytes) throws IOException {
        jar.putNextEntry(new JarEntry(name)); jar.write(bytes); jar.closeEntry();
    }
}
