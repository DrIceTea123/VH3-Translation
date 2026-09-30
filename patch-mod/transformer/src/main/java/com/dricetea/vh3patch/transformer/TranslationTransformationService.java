package com.dricetea.vh3patch.transformer;

import cpw.mods.modlauncher.api.*;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

public final class TranslationTransformationService implements ITransformationService {
    private final PatchSpec spec = PatchSpec.load();
    private boolean client;

    @Override public String name() { return "vh3_translation_patch"; }
    @Override public void onLoad(IEnvironment environment, Set<String> services) {}

    @Override public void initialize(IEnvironment environment) {
        System.clearProperty(PatchSpec.READY_PROPERTY);
        String launch = environment.getProperty(IEnvironment.Keys.LAUNCHTARGET.get()).orElse("");
        // Supported production targets only; dev launches require separate integration work.
        if (launch.equals("forgeserver") || launch.equals("forgedatagen")) return;
        if (!launch.equals("forgeclient")) throw new IllegalStateException("VH3 patch does not support launch target: " + launch);
        client = true;
        Path gameDir = environment.getProperty(IEnvironment.Keys.GAMEDIR.get())
                .orElseThrow(() -> new IllegalStateException("Missing game directory"));
        try {
            preflight(gameDir, spec);
            System.setProperty(PatchSpec.READY_PROPERTY, spec.patchVersion());
            System.getLogger(name()).log(System.Logger.Level.INFO, "Preflight passed; one mob-name method patch registered.");
        } catch (Exception e) {
            throw new IllegalStateException("VH3 Translation Patch preflight FAILED: " + e.getMessage(), e);
        }
    }

    static void preflight(Path gameDir, PatchSpec spec) throws Exception {
        List<Path> targets = new ArrayList<>();
        List<Path> companions = new ArrayList<>();
        try (var files = Files.list(gameDir.resolve("mods"))) {
            for (Path path : files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".jar")).toList()) {
                try (JarFile jar = new JarFile(path.toFile())) {
                    if (jar.getJarEntry(spec.className() + ".class") != null) targets.add(path);
                    if (jar.getJarEntry(PatchSpec.HELPER + ".class") != null) {
                        String version = jar.getManifest() == null ? null
                                : jar.getManifest().getMainAttributes().getValue("VH3-Patch-Runtime");
                        if (!spec.patchVersion().equals(version)
                                || jar.getJarEntry("META-INF/mods.toml") == null
                                || jar.getJarEntry("assets/vh3_translation_patch/lang/zh_cn.json") == null) {
                            throw new IllegalStateException("Missing or mismatched runtime metadata/resources: " + path.getFileName());
                        }
                        companions.add(path);
                    }
                }
            }
        }
        if (targets.size() != 1) throw new IllegalStateException("Expected one the_vault JAR; found " + targets.size());
        if (companions.size() != 1) throw new IllegalStateException("Expected one matching runtime JAR; found " + companions.size());
        // Check a fresh copy now, and validate the actual transform input again when the class loads.
        new MobNamePatch(spec).apply(TargetJar.read(targets.get(0), spec, true));
        VpCompatibility.assertCompatible(gameDir.resolve("config/vaultpatcher_asm"), spec);
    }

    @Override public List<ITransformer> transformers() {
        return client ? List.of(new Transformer(spec)) : List.of();
    }

    private record Transformer(PatchSpec spec) implements ITransformer<ClassNode> {
        @Override public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
            new MobNamePatch(spec).apply(input);
            System.getLogger("vh3_translation_patch").log(System.Logger.Level.INFO,
                    "Applied formatMobName patch (two return hooks).");
            return input;
        }
        @Override public TransformerVoteResult castVote(ITransformerVotingContext context) { return TransformerVoteResult.YES; }
        @Override public Set<Target> targets() { return Set.of(Target.targetClass(spec.className().replace('/', '.'))); }
    }
}
