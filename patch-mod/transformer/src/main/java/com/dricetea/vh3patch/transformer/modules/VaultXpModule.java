package com.dricetea.vh3patch.transformer.modules;

import com.dricetea.vh3patch.transformer.*;
import com.google.gson.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.*;

/** 只接管经验提示文字；计数、经验倍率、通知寿命与预览数值保留上游实现。 */
public final class VaultXpModule implements PatchModule {
    public static final String ID = "vault_xp";
    public static final String TARGET = "iskallia/vault/client/data/ClientVaultXpTracker";
    private static final Set<String> DELTA_LABELS = Set.of("Treasure Door", "Coin Pile", "Treasure Sand", "Bonus");
    private final List<PatchSpec> specs;
    public VaultXpModule() {
        this(PatchSpec.loadAll(ID, "com/dricetea/vh3patch/modules/VaultXpModule", Set.of()));
    }
    public VaultXpModule(List<PatchSpec> specs) { this.specs = List.copyOf(specs); }
    @Override public PatchSpec spec() { return specs.get(0); }
    @Override public List<PatchSpec> specs() { return specs; }
    @Override public MethodNode target(ClassNode node) { return VerifiedMethodPatch.target(node, spec()); }
    @Override public void apply(ClassNode node) { apply(node, true); }
    @Override public void apply(ClassNode node, boolean client) {
        for (PatchSpec spec : specs(client)) {
            if (!spec.className().equals(node.name)) continue;
            switch (spec.methodName()) {
                case "formatChestName" -> patchChest(node, spec);
                case "formatOreName", "formatMobName" -> StringValuePatch.applyReturns(node, spec);
                case "handleDeltas" -> StringValuePatch.apply(node, spec,
                        i -> i instanceof LdcInsnNode ldc && ldc.cst instanceof String s && DELTA_LABELS.contains(s), false);
                case "createPreviewNotifications" -> StringValuePatch.apply(node, spec,
                        i -> i instanceof LdcInsnNode ldc && "Common Wooden Chest".equals(ldc.cst), false);
                default -> throw new IllegalStateException("Missing XP selector: " + spec.methodName());
            }
        }
    }

    private static void patchChest(ClassNode node, PatchSpec spec) {
        VerifiedMethodPatch.apply(node, spec, 3, method -> {
            int count = 0;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction.getOpcode() != Opcodes.ARETURN) continue;
                // 原显示结果仍在栈上；两个枚举参数与桶标记都未被上游覆盖。
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
                hook.add(new VarInsnNode(Opcodes.ALOAD, 2));
                hook.add(new VarInsnNode(Opcodes.ILOAD, 3));
                hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, spec.helperClass(), "translateChest",
                        "(Ljava/lang/String;Ljava/lang/Enum;Ljava/lang/Enum;Z)Ljava/lang/String;", false));
                method.instructions.insertBefore(instruction, hook);
                count++;
            }
            return count;
        });
    }

    @Override public boolean ownsVpRule(JsonObject rule) {
        if (rule.has("target_class") && rule.get("target_class").isJsonObject()
                && ownsTarget(rule.getAsJsonObject("target_class"))) return true;
        if (rule.has("target_classes") && rule.get("target_classes").isJsonArray())
            for (JsonElement e : rule.getAsJsonArray("target_classes"))
                if (e.isJsonObject() && ownsTarget(e.getAsJsonObject())) return true;
        return false;
    }
    private static boolean ownsTarget(JsonObject target) {
        // 当前发布配置没有该类的旧规则。拒绝后来加入的类级/方法级规则，避免部分译文破坏完整英文键。
        return target.has("name") && TARGET.equals(target.get("name").getAsString().replace('.', '/'));
    }
    @Override public void validateVpMigration(List<JsonObject> rules) {
        if (!rules.isEmpty()) throw new IllegalStateException(
                "No historical vault_xp VP rules: review conflicting ClientVaultXpTracker rules manually before migration");
    }
}
