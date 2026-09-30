package com.dricetea.vh3patch.transformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

public final class TargetJar {
    private TargetJar() {}

    public static ClassNode read(Path path, PatchSpec spec, boolean verifyHash) throws IOException {
        if (verifyHash) {
            String hash = MethodFingerprint.sha256(Files.readAllBytes(path));
            if (!hash.equals(spec.jarHash())) throw new IllegalStateException("Unsupported the_vault JAR SHA-256: " + hash);
        }
        try (JarFile jar = new JarFile(path.toFile())) {
            var entry = jar.getJarEntry(spec.className() + ".class");
            if (entry == null) throw new IllegalStateException("Target class is missing from JAR");
            try (var stream = jar.getInputStream(entry)) {
                ClassNode node = new ClassNode();
                new ClassReader(stream).accept(node, 0);
                return node;
            }
        }
    }
}
