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
    private final List<PatchModule> modules = PatchModules.all();
    private boolean client;

    @Override public String name() { return "vh3_translation_patch"; }
    @Override public void onLoad(IEnvironment environment, Set<String> services) {}

    @Override public void initialize(IEnvironment environment) {
        System.clearProperty(PatchSpec.READY_PROPERTY);
        String launch = environment.getProperty(IEnvironment.Keys.LAUNCHTARGET.get()).orElse("");
        // 仅支持生产客户端；专用服务端和数据生成不注册这些显示补丁。
        if (launch.equals("forgeserver") || launch.equals("forgedatagen")) return;
        if (!launch.equals("forgeclient")) throw new IllegalStateException("VH3 patch does not support launch target: " + launch);
        client = true;
        Path gameDir = environment.getProperty(IEnvironment.Keys.GAMEDIR.get())
                .orElseThrow(() -> new IllegalStateException("Missing game directory"));
        try {
            for (PatchModule module : modules) preflight(gameDir, module);
            System.setProperty(PatchSpec.READY_PROPERTY, modules.get(0).spec().patchVersion());
            System.getLogger(name()).log(System.Logger.Level.INFO,
                    "Preflight passed; registered modules: " + modules.stream().map(m -> m.spec().moduleId()).toList());
        } catch (Exception e) {
            throw new IllegalStateException("VH3 Translation Patch preflight FAILED: " + e.getMessage(), e);
        }
    }

    static void preflight(Path gameDir, PatchModule module) throws Exception {
        PatchSpec spec = module.spec();
        List<Path> targets = new ArrayList<>();
        List<Path> companions = new ArrayList<>();
        try (var files = Files.list(gameDir.resolve("mods"))) {
            for (Path path : files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".jar")).toList()) {
                try (JarFile jar = new JarFile(path.toFile())) {
                    if (jar.getJarEntry(spec.className() + ".class") != null) targets.add(path);
                    if (jar.getJarEntry(spec.helperClass() + ".class") != null) {
                        String version = jar.getManifest() == null ? null
                                : jar.getManifest().getMainAttributes().getValue("VH3-Patch-Runtime");
                        if (!spec.patchVersion().equals(version)
                                || jar.getJarEntry("META-INF/mods.toml") == null) {
                            throw new IllegalStateException("Missing or mismatched runtime metadata/resources: " + path.getFileName());
                        }
                        companions.add(path);
                    }
                }
            }
        }
        if (targets.size() != 1) throw new IllegalStateException("Expected one the_vault JAR; found " + targets.size());
        if (companions.size() != 1) throw new IllegalStateException("Expected one matching runtime JAR; found " + companions.size());
        // 此处检查原 JAR；类真正加载时仍会再验一次，发现其他转换器的冲突就中止。
        module.apply(TargetJar.read(targets.get(0), spec, true));
        VpCompatibility.assertCompatible(gameDir.resolve("config/vaultpatcher_asm"), module);
    }

    @Override public List<ITransformer> transformers() {
        if (!client) return List.of();
        List<ITransformer> result = new ArrayList<>();
        PatchModules.byClass().forEach((className, group) -> result.add(new Transformer(className, group)));
        return result;
    }

    private record Transformer(String className, List<PatchModule> modules) implements ITransformer<ClassNode> {
        @Override public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
            for (PatchModule module : modules) {
                module.apply(input);
                System.getLogger("vh3_translation_patch").log(System.Logger.Level.INFO,
                        "Applied module: " + module.spec().moduleId());
            }
            return input;
        }
        @Override public TransformerVoteResult castVote(ITransformerVotingContext context) { return TransformerVoteResult.YES; }
        @Override public Set<Target> targets() { return Set.of(Target.targetClass(className.replace('/', '.'))); }
    }
}
