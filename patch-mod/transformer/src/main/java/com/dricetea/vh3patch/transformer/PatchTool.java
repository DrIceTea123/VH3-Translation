package com.dricetea.vh3patch.transformer;

import com.google.gson.*;
import com.dricetea.vh3patch.transformer.modules.CombatStatsModule;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarFile;

/** 离线工具：只生成工作产物，不修改上游 JAR 或玩家的游戏配置。 */
public final class PatchTool {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Set<String> VANILLA = Set.of("chicken", "zombie", "husk", "drowned", "skeleton",
            "stray", "creeper", "spider", "piglin", "wither_skeleton", "enderman", "piglin_brute", "blaze",
            "polar_bear", "rabbit", "slime", "magma_cube", "vindicator", "ravager", "vex", "pillager", "evoker");

    private PatchTool() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException("inspect|verify|prepare-vp|import-names <input> [output]");
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
                writeJson(output.resolve("report.json"), reports);
                System.out.println("Target hash, method fingerprint and bytecode verification passed.");
            }
            case "prepare-vp" -> {
                Path output = Path.of(args[2]);
                JsonArray original = readJson(input).getAsJsonArray();
                JsonArray migrated = original;
                for (PatchModule module : PatchModules.all()) {
                    migrated = VpCompatibility.withoutOwnedMethod(migrated, module.spec());
                }
                writeJson(output.resolve("config/vaultpatcher_asm/" + input.getFileName()),
                        migrated);
                JsonObject report = new JsonObject();
                report.addProperty("inputSha256", MethodFingerprint.sha256(Files.readAllBytes(input)));
                report.addProperty("removedGroups", original.size() - migrated.size());
                report.addProperty("retainedGroups", migrated.size());
                report.addProperty("sourceUnmodified", true);
                writeJson(output.resolve("vp-migration-report.json"), report);
                System.out.println("Generated VP compatibility copy; removed registered modules' owned method groups.");
            }
            case "import-names" -> importNames(input, Path.of(args[2]), Path.of(args[3]), new CombatStatsModule().spec());
            default -> throw new IllegalArgumentException("Unknown command: " + args[0]);
        }
    }

    private static void importNames(Path vpSource, Path targetJar, Path output, PatchSpec spec) throws IOException {
        TargetJar.read(targetJar, spec, true);
        JsonObject english;
        try (JarFile jar = new JarFile(targetJar.toFile());
             var reader = new java.io.InputStreamReader(jar.getInputStream(jar.getJarEntry("assets/the_vault/lang/en_us.json")), StandardCharsets.UTF_8)) {
            english = JsonParser.parseReader(reader).getAsJsonObject();
        }
        JsonArray source = readJson(vpSource).getAsJsonArray();
        JsonObject translated = new JsonObject();
        JsonArray unresolved = new JsonArray();
        JsonArray allPairs = null;
        for (JsonElement rule : source) {
            if (rule.isJsonObject() && VpCompatibility.ownsMethod(rule.getAsJsonObject(), spec)) {
                if (allPairs != null) throw new IllegalStateException("Multiple source groups");
                allPairs = rule.getAsJsonObject().getAsJsonArray("pairs").deepCopy();
            }
        }
        if (allPairs == null) throw new IllegalStateException("No source group");
        for (JsonElement pair : allPairs) {
            JsonObject item = pair.getAsJsonObject();
            String path = item.get("key").getAsString().toLowerCase(Locale.ROOT).replace(' ', '_');
            String namespace;
            if (english.has("entity.the_vault." + path)) namespace = "the_vault";
            else if (VANILLA.contains(path)) namespace = "minecraft";
            else { unresolved.add(item.deepCopy()); continue; }
            // 仅此模块以实体路径查表；语言文件用于核实来源，不作为运行期配置格式。
            String key = path;
            if (translated.has(key)) throw new IllegalStateException("Duplicate import key: " + key);
            translated.add(key, item.get("value"));
        }
        writeJson(output.resolve("runtime/src/main/resources/" + spec.defaultConfigResource()), translated);
        JsonObject report = new JsonObject();
        report.addProperty("sourceSha256", MethodFingerprint.sha256(Files.readAllBytes(vpSource)));
        report.addProperty("sourceCount", allPairs.size());
        report.addProperty("importedCount", translated.size());
        report.add("unresolved", unresolved);
        report.add("originalPairs", allPairs);
        writeJson(output.resolve("translations/mob-name-import.json"), report);
        System.out.println("Imported " + translated.size() + " module mappings; preserved " + unresolved.size()
                + " unresolved legacy names in the import report.");
    }

    static JsonElement readJson(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { return JsonParser.parseReader(reader); }
    }

    private static void writeJson(Path path, JsonElement value) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Files.writeString(path, JSON.toJson(value) + "\n", StandardCharsets.UTF_8);
    }
}
