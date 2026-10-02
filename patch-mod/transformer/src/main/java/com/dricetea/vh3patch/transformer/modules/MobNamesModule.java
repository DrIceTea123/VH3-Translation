package com.dricetea.vh3patch.transformer.modules;

import com.dricetea.vh3patch.transformer.PatchModule;
import com.dricetea.vh3patch.transformer.PatchSpec;
import com.dricetea.vh3patch.transformer.StringReturnPatch;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Set;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import com.dricetea.vh3patch.transformer.VerifiedMethodPatch;
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

/** 结算与经验提示共用实体路径映射；只在名称返回点读取原始实体 ID。 */
public final class MobNamesModule implements PatchModule {
    public static final String ID = "mob_names";
    private final PatchSpec spec;
    private final List<PatchSpec> specs;

    public MobNamesModule() {
        this(PatchSpec.loadAll(ID, "com/dricetea/vh3patch/modules/MobNamesModule", Set.of(":", "_", " ")));
    }

    public MobNamesModule(PatchSpec spec) {
        this(List.of(spec));
    }
    public MobNamesModule(List<PatchSpec> specs) { this.specs = List.copyOf(specs); this.spec = specs.get(0); }

    @Override public PatchSpec spec() { return spec; }
    @Override public List<PatchSpec> specs() { return specs; }
    @Override public MethodNode target(ClassNode node) { return VerifiedMethodPatch.target(node, spec); }
    @Override public void apply(ClassNode node) { apply(node, true); }
    @Override public void apply(ClassNode node, boolean client) {
        for (PatchSpec p : specs(client)) {
            if (!p.className().equals(node.name)) continue;
            if (p.descriptor().equals("(Ljava/lang/String;)Ljava/lang/String;")) new StringReturnPatch(p).apply(node);
            else VerifiedMethodPatch.apply(node, p, method -> {
                int count = 0;
                for (AbstractInsnNode i : method.instructions.toArray()) {
                    if (i.getOpcode() != Opcodes.ARETURN) continue;
                    InsnList hook = new InsnList();
                    hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    hook.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;", false));
                    hook.add(new InsnNode(Opcodes.SWAP));
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, p.helperClass(), "translate", PatchSpec.HELPER_DESCRIPTOR, false));
                    method.instructions.insertBefore(i, hook);
                    count++;
                }
                return count;
            });
        }
    }

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
        // 导入结果仅作候选文件；构建任务将 output 指向 build，不覆盖人工维护的工程配置。
        JsonFiles.write(output.resolve(spec.configPath()), translated);
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
        if (rule.has("target_class") && rule.get("target_class").isJsonObject()
                && ownsTarget(rule.getAsJsonObject("target_class"), rule)) return true;
        if (rule.has("target_classes") && rule.get("target_classes").isJsonArray())
            for (JsonElement t : rule.getAsJsonArray("target_classes"))
                if (t.isJsonObject() && ownsTarget(t.getAsJsonObject(), rule)) return true;
        return false;
    }
    private boolean ownsTarget(JsonObject target, JsonObject rule) {
        if (!target.has("name")) return false;
        String name = target.get("name").getAsString().replace('.', '/');
        if (name.equals("iskallia/vault/client/data/ClientVaultXpTracker"))
            return !target.has("method") || target.get("method").getAsString().equals("formatMobName")
                    || (target.has("local") && Set.of("MformatMobName", "RformatMobName").contains(target.get("local").getAsString()));
        if (!name.equals(spec.className())) return false;
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

    @Override public void validateVpMigration(List<JsonObject> rules) {
        PatchModule.super.validateVpMigration(rules);
        // 只允许已审查的结算单方法历史组自动迁移；新增 XP 或多目标规则不能被整组删除。
        for (JsonObject rule : rules) {
            JsonObject target = rule.has("target_class") && rule.get("target_class").isJsonObject()
                    ? rule.getAsJsonObject("target_class") : null;
            if (rule.has("target_classes") || target == null || !target.has("name") || !target.has("method")
                    || !target.get("name").getAsString().replace('.', '/').equals("iskallia/vault/client/gui/screen/summary/element/CombatStatsContainerElement")
                    || !target.get("method").getAsString().equals("formatMobName"))
                throw new IllegalStateException("Review new or mixed mob_names VP rules manually before migration");
        }
    }

}
