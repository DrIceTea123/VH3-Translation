package com.dricetea.vh3patch.transformer;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;

/** 单文件安装包的运行侧提取；转换服务和定位器共用此入口，不依赖服务初始化顺序。 */
public final class EmbeddedRuntime {
    static final String ENTRY = "META-INF/vh3_translation_patch/runtime.jar";
    static final String METADATA = "META-INF/vh3_translation_patch/runtime.properties";
    static final String SERVICE_CLASS = "com/dricetea/vh3patch/transformer/TranslationTransformationService.class";

    private EmbeddedRuntime() {}

    public static Path resolve(Path gameDir) throws IOException {
        Properties metadata = new Properties();
        try (var input = EmbeddedRuntime.class.getResourceAsStream("/" + METADATA)) {
            if (input == null) throw new IOException("VTP embedded runtime metadata is missing; reinstall the single VTP JAR");
            metadata.load(input);
        }
        return resolve(gameDir, metadata.getProperty("version"), metadata.getProperty("sha256"));
    }

    // 每次核对安装布局和缓存，避免初始化先后顺序与损坏缓存影响结果。
    static synchronized Path resolve(Path gameDir, String version, String hash) throws IOException {
        if (version == null || !version.matches("1\\.\\d+\\.\\d+") || hash == null || !hash.matches("[0-9a-f]{64}"))
            throw new IOException("Invalid VTP embedded runtime metadata");
        Path mods = gameDir.resolve("mods");
        List<Path> bundles = new ArrayList<>();
        List<Path> legacy = new ArrayList<>();
        try (var paths = Files.list(mods)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")).toList()) {
                try (var jar = new JarFile(path.toFile())) {
                    var manifest = jar.getManifest();
                    boolean runtime = manifest != null && manifest.getMainAttributes().getValue("VH3-Patch-Runtime") != null;
                    boolean transformer = jar.getJarEntry(SERVICE_CLASS) != null;
                    if (transformer && jar.getJarEntry(ENTRY) != null && !runtime) bundles.add(path);
                    else if (runtime || transformer) legacy.add(path);
                }
            }
        }
        if (!legacy.isEmpty()) throw new IOException("Remove old standalone VTP runtime/transformer JARs from mods: " + legacy);
        if (bundles.size() != 1) throw new IOException("Expected exactly one VTP bundle in mods; found " + bundles.size() + ": " + bundles);
        byte[] bytes;
        try (var jar = new JarFile(bundles.get(0).toFile())) {
            if (jar.getManifest() == null || !version.equals(jar.getManifest().getMainAttributes().getValue("Implementation-Version")))
                throw new IOException("VTP bundle version does not match the loaded transformation service");
            try (var input = jar.getInputStream(jar.getJarEntry(ENTRY))) { bytes = input.readAllBytes(); }
        }
        if (!hash.equals(MethodFingerprint.sha256(bytes))) throw new IOException("VTP embedded runtime SHA-256 mismatch; reinstall the VTP JAR");
        Path directory = gameDir.toAbsolutePath().normalize().resolve(".vh3_translation_patch/runtime").resolve(version + "-" + hash);
        Path runtime = directory.resolve("runtime.jar");
        try {
            Files.createDirectories(directory);
            if (!Files.isRegularFile(runtime) || !hash.equals(MethodFingerprint.sha256(Files.readAllBytes(runtime)))) {
                Path temporary = Files.createTempFile(directory, "runtime-", ".tmp");
                try {
                    Files.write(temporary, bytes);
                    validate(temporary, version);
                    // 仅发布完整文件；不支持原子移动的文件系统直接报错，避免半份 JAR。
                    Files.move(temporary, runtime, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } finally { Files.deleteIfExists(temporary); }
            }
            validate(runtime, version);
            return runtime;
        } catch (IOException e) {
            throw new IOException("Cannot prepare VTP runtime cache " + runtime + "; check permissions or close other game processes: " + e.getMessage(), e);
        }
    }

    static void validate(Path runtime, String version) throws IOException {
        if (!Files.isRegularFile(runtime)) throw new IOException("VTP embedded runtime is missing: " + runtime);
        try (var jar = new JarFile(runtime.toFile())) {
            if (jar.getManifest() == null || !version.equals(jar.getManifest().getMainAttributes().getValue("VH3-Patch-Runtime"))
                    || jar.getJarEntry("META-INF/mods.toml") == null)
                throw new IOException("Missing or mismatched VTP embedded runtime metadata/resources");
        }
    }
}
