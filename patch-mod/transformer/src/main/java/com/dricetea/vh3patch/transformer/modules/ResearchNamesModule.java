package com.dricetea.vh3patch.transformer.modules;

import com.dricetea.vh3patch.transformer.*;
import com.google.gson.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** 研究名称只在显示边界查表；研究树、解锁判断、存档和网络中的英文标识不变。 */
public final class ResearchNamesModule implements PatchModule {
    public static final String ID = "research_names";
    private final List<PatchSpec> specs;
    public ResearchNamesModule() {
        this(PatchSpec.loadAll(ID, "com/dricetea/vh3patch/modules/ResearchNamesModule", Set.of()));
    }
    public ResearchNamesModule(List<PatchSpec> specs) { this.specs = List.copyOf(specs); }
    @Override public PatchSpec spec() { return specs.get(0); }
    @Override public List<PatchSpec> specs() { return specs; }
    @Override public MethodNode target(ClassNode node) {
        return VerifiedMethodPatch.target(node, specs.stream().filter(s -> s.className().equals(node.name)).findFirst().orElseThrow());
    }
    @Override public void apply(ClassNode node) { apply(node, true); }
    @Override public void apply(ClassNode node, boolean client) {
        for (PatchSpec spec : specs(client)) {
            if (!spec.className().equals(node.name)) continue;
            StringValuePatch.apply(node, spec, instruction -> select(spec, instruction), spec.methodName().equals("getDisabledText"));
        }
    }

    private static boolean call(AbstractInsnNode instruction, String owner, String name, String descriptor) {
        return instruction instanceof MethodInsnNode m && m.owner.equals(owner) && m.name.equals(name) && m.desc.equals(descriptor);
    }
    private static boolean textConstructor(AbstractInsnNode instruction) {
        return call(instruction, "net/minecraft/network/chat/TextComponent", "<init>", "(Ljava/lang/String;)V");
    }
    private static boolean select(PatchSpec spec, AbstractInsnNode instruction) {
        AbstractInsnNode next = StringValuePatch.nextCode(instruction);
        return switch (spec.methodName()) {
            // 同一字段也用于业务查找，仅选择紧接文本构造或文字排版的那一次读取。
            case "renderHeading" -> instruction instanceof FieldInsnNode f && f.owner.equals(spec.className())
                    && f.name.equals("researchName") && textConstructor(next);
            case "renderHover" -> instruction instanceof FieldInsnNode f && f.owner.equals(spec.className())
                    && f.name.equals("researchName") && next instanceof FieldInsnNode style
                    && style.owner.equals("net/minecraft/network/chat/Style");
            case "lambda$renderHover$1", "lambda$renderHover$2", "lambda$research$3" ->
                    call(instruction, "iskallia/vault/research/type/Research", "getName", "()Ljava/lang/String;");
            case "warnResearchRequirement" -> instruction instanceof VarInsnNode v && v.getOpcode() == Opcodes.ALOAD
                    && v.var == 0 && textConstructor(next);
            case "lambda$appendHoverText$3" -> instruction instanceof VarInsnNode v && v.getOpcode() == Opcodes.ALOAD
                    && v.var == 1 && textConstructor(next);
            case "lambda$onItemTooltip$0" -> instruction instanceof MethodInsnNode m
                    && m.owner.equals("iskallia/vault/research/ResearchTree") && m.name.equals("restrictedBy")
                    && m.desc.endsWith(")Ljava/lang/String;");
            case "lambda$addTabButtons$2" -> instruction instanceof MethodInsnNode m
                    && m.name.equals("getDeckResearchName") && m.desc.equals("()Ljava/lang/String;") && textConstructor(next);
            // 配方会先转标题大小写；复制原文作为键，原格式化结果只作缺失映射的回退。
            case "getDisabledText" -> instruction instanceof MethodInsnNode m && m.name.equals("convertToTitleCase")
                    && m.desc.equals("(Ljava/lang/String;)Ljava/lang/String;");
            // 登录迁移通知的两个名字只在 append 前替换，前面的移除/添加研究操作不受影响。
            case "onPlayerLogin" -> instruction instanceof VarInsnNode v && v.getOpcode() == Opcodes.ALOAD
                    && (v.var == 7 || v.var == 9) && next instanceof MethodInsnNode m && m.name.equals("m_130946_")
                    && m.owner.startsWith("net/minecraft/network/chat/") && m.desc.startsWith("(Ljava/lang/String;)");
            default -> throw new IllegalStateException("Missing research selector: " + spec.methodName());
        };
    }

    private static List<JsonObject> targets(JsonObject rule) {
        List<JsonObject> targets = new ArrayList<>();
        if (rule.has("target_class") && rule.get("target_class").isJsonObject()) targets.add(rule.getAsJsonObject("target_class"));
        if (rule.has("target_classes") && rule.get("target_classes").isJsonArray())
            for (JsonElement e : rule.getAsJsonArray("target_classes")) if (e.isJsonObject()) targets.add(e.getAsJsonObject());
        return targets;
    }
    private boolean ownsTarget(JsonObject target) {
        if (!target.has("name")) return false;
        String name = target.get("name").getAsString().replace('.', '/');
        if (specs.stream().noneMatch(s -> s.className().equals(name))) return false;
        if (!target.has("local")) return false; // 同类仍由 VP 翻译普通提示语，不能一起删除。
        return Set.of("GresearchName", "MgetName", "MrestrictedBy", "Vresearch", "MgetDeckResearchName")
                .contains(target.get("local").getAsString());
    }
    @Override public boolean ownsVpRule(JsonObject rule) {
        rule = VpCompatibility.normalizeRule(rule); return targets(rule).stream().anyMatch(this::ownsTarget); }
    @Override public void validateVpMigration(List<JsonObject> rules) {
        Set<String> groups = new HashSet<>();
        for (JsonObject rule : rules) {
            if (targets(rule).stream().anyMatch(t -> !ownsTarget(t)))
                throw new IllegalStateException("Research VP group also contains unrelated targets; split it before migration");
            String key = rule.has("target_classes") ? "research-list" : "deck";
            if (!groups.add(key)) throw new IllegalStateException("Duplicate research VP group: " + key);
        }
    }

    @Override public void importMappings(Path vpSource, Path targetJar, Path output) throws IOException {
        for (PatchSpec spec : specs) {
            if (!MethodFingerprint.of(VerifiedMethodPatch.target(TargetJar.read(targetJar, spec, true), spec)).equals(spec.fingerprint()))
                throw new IllegalStateException("Research target changed: " + spec.methodName());
        }
        List<JsonObject> rules = new ArrayList<>();
        for (JsonElement e : JsonFiles.read(vpSource).getAsJsonArray())
            if (e.isJsonObject() && ownsVpRule(e.getAsJsonObject())) rules.add(e.getAsJsonObject());
        validateVpMigration(rules);
        if (rules.size() != 2) throw new IllegalStateException("Expected both historical research groups");
        JsonObject result = new JsonObject();
        JsonArray originals = new JsonArray();
        for (JsonObject rule : rules) {
            JsonArray pairs = rule.has("pairs") ? rule.getAsJsonArray("pairs") : new JsonArray();
            if (!rule.has("pairs")) pairs.add(rule);
            for (JsonElement e : pairs) {
                JsonObject pair = e.getAsJsonObject();
                String key = pair.get("key").getAsString();
                JsonElement value = pair.get("value");
                // 用户选择旧文件中后出现的 Waystones 译名；不把任何译文内置到模块。
                if (result.has(key) && !result.get(key).equals(value) && !key.equals("Waystones"))
                    throw new IllegalStateException("Conflicting research translation: " + key);
                result.add(key, value.deepCopy());
                originals.add(pair.deepCopy());
            }
        }
        JsonFiles.write(output.resolve(spec().configPath()), result);
        JsonObject report = new JsonObject();
        report.addProperty("module", ID);
        report.addProperty("sourceSha256", MethodFingerprint.sha256(Files.readAllBytes(vpSource)));
        report.addProperty("sourceCount", originals.size());
        report.addProperty("importedCount", result.size());
        report.add("originalPairs", originals);
        JsonFiles.write(output.resolve("translations/research-name-import.json"), report);
    }
}
