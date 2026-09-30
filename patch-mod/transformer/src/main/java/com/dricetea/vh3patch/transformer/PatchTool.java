package com.dricetea.vh3patch.transformer;

import com.google.gson.*;
import static com.dricetea.vh3patch.transformer.JsonFiles.read;
import static com.dricetea.vh3patch.transformer.JsonFiles.write;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Files;
import java.nio.file.Path;

/** 离线工具：只生成工作产物，不修改上游 JAR 或玩家的游戏配置。 */
public final class PatchTool {



    private PatchTool() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException("inspect|verify|prepare-vp|import-module <input> [output]");
        if (args[0].equals("import-module")) {
            if (args.length != 5) throw new IllegalArgumentException("import-module <moduleId> <vpSource> <targetJar> <output>");
            PatchModules.find(args[1]).importMappings(Path.of(args[2]), Path.of(args[3]), Path.of(args[4]));
            return;
        }
        Path input = Path.of(args[1]);
        switch (args[0]) {
            case "inspect" -> {
                for (PatchModule module : PatchModules.all()) {
                    ClassNode node = TargetJar.read(input, module.spec(), true);
                    System.out.println(module.spec().moduleId() + ".method.sha256=" + MethodFingerprint.of(module.target(node)));
                }
            }
            case "verify" -> {
                Path output = Path.of(args[2]);
                JsonArray reports = new JsonArray();
                for (var group : PatchModules.byClass().values()) {
                    ClassNode node = TargetJar.read(input, group.get(0).spec(), true);
                    for (PatchModule module : group) {
                        PatchSpec spec = module.spec();
                        TargetJar.read(input, spec, true);
                        module.apply(node);
                        JsonObject report = new JsonObject();
                        report.addProperty("module", spec.moduleId());
                        report.addProperty("targetVersion", spec.targetVersion());
                        report.addProperty("jarSha256", spec.jarHash());
                        report.addProperty("method", spec.className() + "." + spec.methodName() + spec.descriptor());
                        report.addProperty("fingerprintAlgorithm", "asm-method-v1");
                        report.addProperty("methodSha256", spec.fingerprint());
                        report.addProperty("returnHooks", spec.returnCount());
                        report.addProperty("bytecodeAnalysis", "passed");
                        report.addProperty("gameTested", false);
                        reports.add(report);
                    }
                    ClassWriter writer = new ClassWriter(0);
                    node.accept(writer);
                    Path classFile = output.resolve(node.name + ".class");
                    Files.createDirectories(classFile.getParent());
                    Files.write(classFile, writer.toByteArray());
                }
                write(output.resolve("report.json"), reports);
                System.out.println("Target hash, method fingerprint and bytecode verification passed.");
            }
            case "prepare-vp" -> {
                Path output = Path.of(args[2]);
                JsonArray original = read(input).getAsJsonArray();
                JsonArray migrated = original;
                for (PatchModule module : PatchModules.all()) {
                    migrated = VpCompatibility.prepareConfiguration(migrated, module);
                }
                write(output.resolve("config/vaultpatcher_asm/" + input.getFileName()),
                        migrated);
                JsonObject report = new JsonObject();
                report.addProperty("inputSha256", MethodFingerprint.sha256(Files.readAllBytes(input)));
                report.addProperty("removedGroups", original.size() - migrated.size());
                report.addProperty("retainedGroups", migrated.size());
                report.addProperty("sourceUnmodified", true);
                write(output.resolve("vp-migration-report.json"), report);
                System.out.println("Generated VP compatibility copy; removed groups: " + (original.size() - migrated.size()));
            }
            default -> throw new IllegalArgumentException("Unknown command: " + args[0]);
        }
    }

}
