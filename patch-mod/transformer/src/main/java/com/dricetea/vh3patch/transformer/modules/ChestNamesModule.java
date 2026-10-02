package com.dricetea.vh3patch.transformer.modules;

import com.dricetea.vh3patch.transformer.*;
import com.google.gson.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.*;
import java.nio.file.Path;
import java.io.IOException;

/** 宝箱名称统一在显示边界接管；保留枚举 ID、方块判定和统计计算。 */
public final class ChestNamesModule implements PatchModule {
    public static final String ID = "chest_names";
    public static final String XP = "iskallia/vault/client/data/ClientVaultXpTracker";
    private static final Set<String> RARITIES = Set.of("Common", "Rare", "Epic", "Omega");
    private static final Set<String> HUNTER = Set.of("WOODEN", "GILDED", "LIVING", "ORNATE", "HARDENED", "ENIGMA", "FLESH");
    private final List<PatchSpec> specs;
    public ChestNamesModule() { this(PatchSpec.loadAll(ID, "com/dricetea/vh3patch/modules/ChestNamesModule", Set.of())); }
    public ChestNamesModule(List<PatchSpec> specs) { this.specs = List.copyOf(specs); }
    @Override public PatchSpec spec() { return specs.get(0); }
    @Override public List<PatchSpec> specs() { return specs; }
    @Override public MethodNode target(ClassNode node) { return VerifiedMethodPatch.target(node, spec()); }
    @Override public void apply(ClassNode node) { apply(node, true); }
    @Override public void apply(ClassNode node, boolean client) {
        for (PatchSpec spec : specs(client)) {
            if (!spec.className().equals(node.name)) continue;
            switch (spec.methodName()) {
                case "getName" -> StringValuePatch.applyReturns(node, spec);
                case "formatChestName" -> patchChest(node, spec);
                case "m_5446_" -> VerifiedMethodPatch.apply(node, spec, method -> {
                    int count = 0;
                    for (AbstractInsnNode i : method.instructions.toArray()) {
                        if (!(i instanceof MethodInsnNode call) || !call.owner.equals("net/minecraft/network/chat/TextComponent")
                                || !call.name.equals("<init>") || !call.desc.equals("(Ljava/lang/String;)V")) continue;
                        // 只替换构造参数，不重建 Component，不接管父类回退。
                        method.instructions.insertBefore(i, helper(spec, "translate")); count++;
                    }
                    return count;
                });
                case "<init>" -> patchLoot(node, spec);
                case "lambda$buildHunterTab$19" -> VerifiedMethodPatch.apply(node, spec, method -> {
                    int count = 0;
                    for (AbstractInsnNode i : method.instructions.toArray()) if (i.getOpcode() == Opcodes.ARETURN) {
                        method.instructions.insertBefore(i, helper(spec, "translateHunter")); count++;
                    }
                    return count;
                });
                default -> StringValuePatch.apply(node, spec, i -> i instanceof LdcInsnNode ldc
                        && (RARITIES.contains(ldc.cst) || "Common Wooden Chest".equals(ldc.cst)), false);
            }
        }
    }
    private static MethodInsnNode helper(PatchSpec spec, String name) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, spec.helperClass(), name, "(Ljava/lang/String;)Ljava/lang/String;", false);
    }
    private static void patchLoot(ClassNode node, PatchSpec spec) {
        VerifiedMethodPatch.apply(node, spec, method -> {
            int count = 0;
            for (AbstractInsnNode i : method.instructions.toArray()) {
                if (!(i instanceof InvokeDynamicInsnNode call) || call.bsmArgs.length == 0 || !(call.bsmArgs[0] instanceof String recipe)) continue;
                if (recipe.equals("\u0001 Barrel")) {
                    // 上游本地变量 22 是当前桶类型；摘要校验固定其布局。getter 已翻译，不能用拼接后的混合文本作键。
                    InsnList hook = new InsnList(); hook.add(new VarInsnNode(Opcodes.ALOAD, 22));
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, spec.helperClass(), "translateBarrel", "(Ljava/lang/String;Ljava/lang/Enum;)Ljava/lang/String;", false));
                    method.instructions.insert(i, hook); count++;
                } else if (RARITIES.stream().anyMatch(r -> recipe.startsWith(r + ": "))) {
                    method.instructions.insert(i, helper(spec, "translateRarityCount")); count++;
                }
            }
            return count;
        });
    }
    private static void patchChest(ClassNode node, PatchSpec spec) {
        VerifiedMethodPatch.apply(node, spec, 3, method -> {
            int count = 0;
            for (AbstractInsnNode i : method.instructions.toArray()) if (i.getOpcode() == Opcodes.ARETURN) {
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(Opcodes.ALOAD, 1)); hook.add(new VarInsnNode(Opcodes.ALOAD, 2)); hook.add(new VarInsnNode(Opcodes.ILOAD, 3));
                hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, spec.helperClass(), "translateChest", "(Ljava/lang/String;Ljava/lang/Enum;Ljava/lang/Enum;Z)Ljava/lang/String;", false));
                method.instructions.insertBefore(i, hook); count++;
            }
            return count;
        });
    }
    @Override public boolean ownsVpRule(JsonObject rule) {
        if (rule.has("target_class") && rule.get("target_class").isJsonObject() && ownsTarget(rule.getAsJsonObject("target_class"), rule)) return true;
        if (rule.has("target_classes") && rule.get("target_classes").isJsonArray())
            for (JsonElement t : rule.getAsJsonArray("target_classes")) if (t.isJsonObject() && ownsTarget(t.getAsJsonObject(), rule)) return true;
        return false;
    }
    private boolean ownsTarget(JsonObject target, JsonObject rule) {
        if (!target.has("name")) return false;
        String name = target.get("name").getAsString().replace('.', '/');
        String method = target.has("method") ? target.get("method").getAsString() : "";
        if (specs.stream().noneMatch(s -> s.className().equals(name))) return false;
        if (name.endsWith("/VaultChestType")) return method.isEmpty() || method.equals("<clinit>") || method.equals("getName");
        if (name.endsWith("/VaultChestTileEntity")) return method.isEmpty() || method.equals("m_5446_");
        if (name.equals(XP)) return method.isEmpty() || method.equals("formatChestName") || method.equals("createPreviewNotifications")
                || (target.has("local") && Set.of("MformatChestName", "RformatChestName").contains(target.get("local").getAsString()));
        // 混合规则只识别宝箱相关键，结算页总计、XP、矿石和辅助功能其他枚举仍由 VP 处理。
        Set<String> keys = new HashSet<>();
        if (rule.has("key")) keys.add(rule.get("key").getAsString());
        if (rule.has("pairs")) for (JsonElement p : rule.getAsJsonArray("pairs")) if (p.getAsJsonObject().has("key")) keys.add(p.getAsJsonObject().get("key").getAsString());
        if (name.endsWith("/VaultAccessibilityScreen")) return keys.stream().anyMatch(HUNTER::contains);
        if (name.endsWith("/VaultChestIconElement")) return keys.stream().anyMatch(RARITIES::contains);
        return name.endsWith("/LootStatsContainerElement") && keys.stream().anyMatch(k -> k.equals(" Barrel") || RARITIES.contains(k.replace(": ", "")));
    }
    @Override public void validateVpMigration(List<JsonObject> rules) {
        if (!rules.isEmpty()) throw new IllegalStateException("chest_names has mixed VP groups: migrate only reviewed pairs; never remove entire groups automatically");
    }
    @Override public void importMappings(Path source, Path targetJar, Path output) throws IOException {
        for (PatchSpec p : specs) TargetJar.read(targetJar, p, true);
        JsonObject result = new JsonObject();
        for (JsonElement e : JsonFiles.read(source).getAsJsonArray()) {
            JsonObject rule = e.getAsJsonObject();
            if (!ownsVpRule(rule)) continue;
            for (JsonElement p : rule.getAsJsonArray("pairs")) {
                JsonObject pair = p.getAsJsonObject(); String key = pair.get("key").getAsString();
                JsonObject single = rule.deepCopy(); JsonArray one = new JsonArray(); one.add(pair); single.add("pairs", one);
                if (!ownsVpRule(single)) continue;
                if (key.equals(" Barrel") || HUNTER.contains(key)) continue;
                key = key.stripLeading();
                if (result.has(key) && !result.get(key).equals(pair.get("value"))) throw new IllegalStateException("Conflicting chest import: " + key);
                result.add(key, pair.get("value").deepCopy());
            }
        }
        JsonFiles.write(output.resolve(spec().configPath()), result);
    }
}
