package com.dricetea.vh3patch.transformer.modules;

import com.dricetea.vh3patch.transformer.PatchModule;
import com.dricetea.vh3patch.transformer.PatchSpec;
import com.dricetea.vh3patch.transformer.StringReturnPatch;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Set;
import java.util.Locale;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.jar.JarFile;
import com.google.gson.*;
import com.dricetea.vh3patch.transformer.JsonFiles;
import com.dricetea.vh3patch.transformer.TargetJar;
import com.dricetea.vh3patch.transformer.MethodFingerprint;

/** 结算怪物名的早期模块：声明目标、运行侧入口以及会冲突的 VP 常量。 */
public final class CombatStatsModule implements PatchModule {
    public static final String ID = "combat_stats";
    private final PatchSpec spec;
    private final StringReturnPatch patch;

    public CombatStatsModule() {
        this(PatchSpec.load(ID, "com/dricetea/vh3patch/modules/CombatStatsModule", Set.of(":", "_", " ")));
    }

    public CombatStatsModule(PatchSpec spec) {
        this.spec = spec;
        this.patch = new StringReturnPatch(spec);
    }

    @Override public PatchSpec spec() { return spec; }
    @Override public MethodNode target(ClassNode node) { return patch.target(node); }
    @Override public void apply(ClassNode node) { patch.apply(node); }

    private static final Set<String> VANILLA = Set.of("chicken", "zombie", "husk", "drowned", "skeleton",
            "stray", "creeper", "spider", "piglin", "wither_skeleton", "enderman", "piglin_brute", "blaze",
            "polar_bear", "rabbit", "slime", "magma_cube", "vindicator", "ravager", "vex", "pillager", "evoker");

    // 旧 VP 名称导入及接管判定仅属于结算模块，公共工具不再持有这些知识。
    @Override
    public void importMappings(Path vpSource, Path targetJar, Path output) throws IOException {
        TargetJar.read(targetJar, spec, true);
        JsonObject english;
        try (JarFile jar = new JarFile(targetJar.toFile());
             var reader = new java.io.InputStreamReader(jar.getInputStream(jar.getJarEntry("assets/the_vault/lang/en_us.json")), StandardCharsets.UTF_8)) {
            english = JsonParser.parseReader(reader).getAsJsonObject();
        }
        JsonArray source = JsonFiles.read(vpSource).getAsJsonArray();
        JsonObject translated = new JsonObject();
        JsonArray unresolved = new JsonArray();
        JsonArray allPairs = null;
        for (JsonElement rule : source) {
            if (rule.isJsonObject() && ownsVpRule(rule.getAsJsonObject())) {
                if (allPairs != null) throw new IllegalStateException("Multiple source groups");
                allPairs = rule.getAsJsonObject().getAsJsonArray("pairs").deepCopy();
            }
        }
        if (allPairs == null) throw new IllegalStateException("No source group");
        for (JsonElement pair : allPairs) {
            JsonObject item = pair.getAsJsonObject();
            String path = item.get("key").getAsString().toLowerCase(Locale.ROOT).replace(' ', '_');
            if (!english.has("entity.the_vault." + path) && !VANILLA.contains(path)) {
                unresolved.add(item.deepCopy());
                continue;
            }
            // 仅此模块以实体路径查表；语言文件用于核实来源，不作为运行期配置格式。
            String key = path;
            if (translated.has(key)) throw new IllegalStateException("Duplicate import key: " + key);
            translated.add(key, item.get("value"));
        }
        JsonFiles.write(output.resolve("runtime/src/main/resources/" + spec.defaultConfigResource()), translated);
        JsonObject report = new JsonObject();
        report.addProperty("sourceSha256", MethodFingerprint.sha256(Files.readAllBytes(vpSource)));
        report.addProperty("sourceCount", allPairs.size());
        report.addProperty("importedCount", translated.size());
        report.add("unresolved", unresolved);
        report.add("originalPairs", allPairs);
        JsonFiles.write(output.resolve("translations/mob-name-import.json"), report);
        System.out.println("Imported " + translated.size() + " module mappings; preserved " + unresolved.size()
                + " unresolved legacy names in the import report.");
    }

    @Override
    public boolean ownsVpRule(JsonObject rule) {
        if (!rule.has("target_class") || !rule.get("target_class").isJsonObject()) return false;
        JsonObject target = rule.getAsJsonObject("target_class");
        if (!target.has("name") || !target.get("name").getAsString().replace('.', '/').equals(spec.className())) return false;
        if (target.has("method")) return spec.methodName().equals(target.get("method").getAsString());
        // 仅当整类规则修改了该模块声明的相关常量时，才判定它与方法补丁冲突。
        if (rule.has("pairs") && rule.get("pairs").isJsonArray()) {
            for (JsonElement pair : rule.getAsJsonArray("pairs")) {
                if (pair.isJsonObject() && pair.getAsJsonObject().has("key")
                        && spec.ownedLiterals().contains(pair.getAsJsonObject().get("key").getAsString())) return true;
            }
        }
        return false;
    }

}
