package com.dricetea.vh3patch.transformer;

import com.google.gson.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarFile;

/** Offline commands only; never writes to the input JAR or live game configuration. */
public final class PatchTool {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Set<String> VANILLA = Set.of("chicken", "zombie", "husk", "drowned", "skeleton",
            "stray", "creeper", "spider", "piglin", "wither_skeleton", "enderman", "piglin_brute", "blaze",
            "polar_bear", "rabbit", "slime", "magma_cube", "vindicator", "ravager", "vex", "pillager", "evoker");

    private PatchTool() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException("inspect|verify|prepare-vp|import-names <input> [output]");
        PatchSpec spec = PatchSpec.load();
        Path input = Path.of(args[1]);
        switch (args[0]) {
            case "inspect" -> {
                ClassNode node = TargetJar.read(input, spec, true);
                System.out.println("method.sha256=" + MethodFingerprint.of(new MobNamePatch(spec).target(node)));
            }
            case "verify" -> {
                Path output = Path.of(args[2]);
                ClassNode node = TargetJar.read(input, spec, true);
                new MobNamePatch(spec).apply(node);
                ClassWriter writer = new ClassWriter(0);
                node.accept(writer);
                Path classFile = output.resolve(spec.className() + ".class");
                Files.createDirectories(classFile.getParent());
                Files.write(classFile, writer.toByteArray());
                JsonObject report = new JsonObject();
                report.addProperty("targetVersion", spec.targetVersion());
                report.addProperty("jarSha256", spec.jarHash());
                report.addProperty("method", spec.className() + "." + spec.methodName() + spec.descriptor());
                report.addProperty("fingerprintAlgorithm", "asm-method-v1");
                report.addProperty("methodSha256", spec.fingerprint());
                report.addProperty("returnHooks", spec.returnCount());
                report.addProperty("bytecodeAnalysis", "passed");
                report.addProperty("gameTested", false);
                writeJson(output.resolve("report.json"), report);
                System.out.println("Target hash, method fingerprint and bytecode verification passed.");
            }
            case "prepare-vp" -> {
                Path output = Path.of(args[2]);
                JsonArray original = readJson(input).getAsJsonArray();
                writeJson(output.resolve("config/vaultpatcher_asm/" + input.getFileName()),
                        VpCompatibility.withoutOwnedMethod(original, spec));
                JsonObject report = new JsonObject();
                report.addProperty("inputSha256", MethodFingerprint.sha256(Files.readAllBytes(input)));
                report.addProperty("removedGroups", 1);
                report.addProperty("retainedGroups", original.size() - 1);
                report.addProperty("sourceUnmodified", true);
                writeJson(output.resolve("vp-migration-report.json"), report);
                System.out.println("Generated VP compatibility copy; removed exactly one owned method group.");
            }
            case "import-names" -> importNames(input, Path.of(args[2]), Path.of(args[3]), spec);
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
            String key = "vh3_translation_patch.mob." + namespace + "." + path;
            if (translated.has(key)) throw new IllegalStateException("Duplicate import key: " + key);
            translated.add(key, item.get("value"));
        }
        writeJson(output.resolve("runtime/src/main/resources/assets/vh3_translation_patch/lang/zh_cn.json"), translated);
        JsonObject report = new JsonObject();
        report.addProperty("sourceSha256", MethodFingerprint.sha256(Files.readAllBytes(vpSource)));
        report.addProperty("sourceCount", allPairs.size());
        report.addProperty("importedCount", translated.size());
        report.add("unresolved", unresolved);
        report.add("originalPairs", allPairs);
        writeJson(output.resolve("translations/mob-name-import.json"), report);
        System.out.println("Imported " + translated.size() + " ID-scoped overrides; preserved " + unresolved.size()
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
