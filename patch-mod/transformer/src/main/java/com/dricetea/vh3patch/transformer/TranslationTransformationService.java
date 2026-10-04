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
    private boolean enabled;

    @Override public String name() { return "vh3_translation_patch"; }
    @Override public void onLoad(IEnvironment environment, Set<String> services) {}

    @Override public void initialize(IEnvironment environment) {
        System.clearProperty(PatchSpec.READY_PROPERTY);
        String launch = environment.getProperty(IEnvironment.Keys.LAUNCHTARGET.get()).orElse("");
        // 服务端只注册 BOTH 目标；客户端也含集成服务器，需要同时注册两类目标。
        if (!launch.equals("forgeclient") && !launch.equals("forgeserver"))
            throw new IllegalStateException("VH3 patch does not support launch target: " + launch);
        client = launch.equals("forgeclient");
        enabled = true;
        Path gameDir = environment.getProperty(IEnvironment.Keys.GAMEDIR.get())
                .orElseThrow(() -> new IllegalStateException("Missing game directory"));
        try {
            Path runtime = EmbeddedRuntime.resolve(gameDir);
            for (PatchModule module : PatchModules.active(client)) preflight(gameDir, module, client, runtime);
            System.setProperty(PatchSpec.READY_PROPERTY, modules.get(0).spec().patchVersion());
            System.getLogger(name()).log(System.Logger.Level.INFO,
                    "Preflight passed; registered modules: " + PatchModules.active(client).stream().map(m -> m.spec().moduleId()).toList());
        } catch (Exception e) {
            throw new IllegalStateException("VH3 Translation Patch preflight FAILED: " + e.getMessage(), e);
        }
    }

    static void preflight(Path gameDir, PatchModule module, boolean client, Path runtime) throws Exception {
        List<PatchSpec> active = module.specs(client);
        if (active.isEmpty()) return;
        PatchSpec spec = active.get(0);
        List<Path> targets = new ArrayList<>();
        EmbeddedRuntime.validate(runtime, spec.patchVersion());
        try (JarFile jar = new JarFile(runtime.toFile())) {
            for (PatchSpec target : active) {
                if (jar.getJarEntry(target.helperClass() + ".class") == null)
                    throw new IllegalStateException("Missing VTP runtime helper: " + target.helperClass());
            }
        }
        try (var files = Files.list(gameDir.resolve("mods"))) {
            for (Path path : files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".jar")).toList()) {
                try (JarFile jar = new JarFile(path.toFile())) {
                    if (jar.getJarEntry(spec.className() + ".class") != null) targets.add(path);

                }
            }
        }
        if (targets.size() != 1) throw new IllegalStateException("Expected one the_vault JAR; found " + targets.size());
        // 此处检查原 JAR；类真正加载时仍会再验一次，发现其他转换器的冲突就中止。
        for (String name : active.stream().map(PatchSpec::className).distinct().toList()) {
            PatchSpec classSpec = active.stream().filter(s -> s.className().equals(name)).findFirst().orElseThrow();
            module.apply(TargetJar.read(targets.get(0), classSpec, true), client);
        }
        VpCompatibility.assertCompatible(gameDir.resolve("config/vaultpatcher_asm"), module);
    }

    @Override public List<ITransformer> transformers() {
        if (!enabled) return List.of();
        return transformersFor(client);
    }

    static List<ITransformer> transformersFor(boolean client) {
        List<ITransformer> result = new ArrayList<>();
        PatchModules.byClass(client).forEach((className, group) -> result.add(new Transformer(className, group, client)));
        return result;
    }

    private record Transformer(String className, List<PatchModule> modules, boolean client) implements ITransformer<ClassNode> {
        @Override public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
            for (PatchModule module : modules) {
                module.apply(input, client);
                System.getLogger("vh3_translation_patch").log(System.Logger.Level.INFO,
                        "Applied module: " + module.spec().moduleId());
            }
            return input;
        }
        @Override public TransformerVoteResult castVote(ITransformerVotingContext context) { return TransformerVoteResult.YES; }
        // VP 的普通翻译使用 CLASS；先在 PRE_CLASS 校验原方法，避免把保留的 VP 提示语当作冲突。
        @Override public Set<Target> targets() { return Set.of(Target.targetPreClass(className.replace('/', '.'))); }
    }
}
