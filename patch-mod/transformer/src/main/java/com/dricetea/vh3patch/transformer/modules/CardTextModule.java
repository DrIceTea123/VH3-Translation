package com.dricetea.vh3patch.transformer.modules;

import com.dricetea.vh3patch.transformer.*;
import com.google.gson.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import java.util.*;

/** 在显示组件完成之后翻译，类型、套组、筛选器与序列化数据始终保留原值。 */
public final class CardTextModule implements PatchModule {
    public static final String ID = "card_text";
    private final List<PatchSpec> specs;
    public CardTextModule() { this(PatchSpec.loadAll(ID, "com/dricetea/vh3patch/modules/CardTextModule", Set.of())); }
    public CardTextModule(List<PatchSpec> specs) { this.specs = List.copyOf(specs); }
    @Override public PatchSpec spec() { return specs.get(0); }
    @Override public List<PatchSpec> specs() { return specs; }
    @Override public MethodNode target(ClassNode node) { return VerifiedMethodPatch.target(node, spec()); }
    @Override public void apply(ClassNode node) { apply(node, true); }
    @Override public void apply(ClassNode node, boolean client) {
        for (PatchSpec spec : specs(client)) {
            if (!spec.className().equals(node.name)) continue;
            VerifiedMethodPatch.apply(node, spec, 0, method -> {
                List<AbstractInsnNode> sites = sites(node.name, method);
                for (AbstractInsnNode site : sites) {
                    String helper, descriptor;
                    if (method.name.equals("m_7626_")) {
                        helper = "translateName";
                        descriptor = "(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;";
                    } else if (Set.of("getText", "getColoredText", "sendLockedMessage").contains(method.name)) {
                        helper = "translate"; descriptor = "(Ljava/lang/String;)Ljava/lang/String;";
                    } else {
                        helper = "translateTooltip"; descriptor = "(Ljava/lang/Object;)Ljava/lang/Object;";
                    }
                    method.instructions.insertBefore(site, new MethodInsnNode(Opcodes.INVOKESTATIC, spec.helperClass(), helper, descriptor, false));
                }
                return sites.size();
            });
        }
    }

    /** 只选择直接写入 tooltip 参数的 add/set；不碰同方法中存放逻辑值的其他列表。 */
    public static List<AbstractInsnNode> sites(String owner, MethodNode method) {
        List<AbstractInsnNode> result = new ArrayList<>();
        if (method.name.equals("m_7626_")) {
            for (var i : method.instructions) if (i.getOpcode() == Opcodes.ARETURN) result.add(i);
            return result;
        }
        if (Set.of("getText", "getColoredText", "sendLockedMessage").contains(method.name)) {
            for (var i : method.instructions) if (i instanceof MethodInsnNode call
                    && call.owner.equals("net/minecraft/network/chat/TextComponent") && call.name.equals("<init>")
                    && call.desc.equals("(Ljava/lang/String;)V")) result.add(i);
            return result;
        }
        int slot = (method.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0, listSlot = -1;
        for (Type type : Type.getArgumentTypes(method.desc)) {
            if (type.getDescriptor().equals("Ljava/util/List;")) { listSlot = slot; break; }
            slot += type.getSize();
        }
        if (listSlot < 0) throw new IllegalStateException("No tooltip argument: " + method.name);
        try {
            Frame<SourceValue>[] frames = new Analyzer<>(new SourceInterpreter()).analyze(owner, method);
            int index = 0;
            for (var i : method.instructions) {
                Frame<SourceValue> frame = frames[index++];
                if (frame == null || !(i instanceof MethodInsnNode call) || !call.owner.equals("java/util/List")) continue;
                int arguments = call.name.equals("add") && call.desc.equals("(Ljava/lang/Object;)Z") ? 1
                        : call.name.equals("set") && call.desc.equals("(ILjava/lang/Object;)Ljava/lang/Object;") ? 2 : -1;
                if (arguments < 0) continue;
                SourceValue receiver = frame.getStack(frame.getStackSize() - arguments - 1);
                int parameter = listSlot;
                if (!receiver.insns.isEmpty() && receiver.insns.stream().allMatch(s -> s instanceof VarInsnNode v
                        && v.getOpcode() == Opcodes.ALOAD && v.var == parameter)) result.add(i);
            }
        } catch (AnalyzerException e) { throw new IllegalStateException("Cannot identify tooltip writes", e); }
        return result;
    }

    @Override public boolean ownsVpRule(JsonObject rule) {
        if (rule.has("target_class") && rule.get("target_class").isJsonObject() && ownsTarget(rule.getAsJsonObject("target_class"))) return true;
        if (rule.has("target_classes")) for (JsonElement target : rule.getAsJsonArray("target_classes"))
            if (target.isJsonObject() && ownsTarget(target.getAsJsonObject())) return true;
        return false;
    }
    private boolean ownsTarget(JsonObject target) {
        if (!target.has("name")) return false;
        String name = target.get("name").getAsString().replace('.', '/');
        // 旧 VP 修改匿名显示列表和枚举初始化；新补丁从最终显示值接管这些相同内容。
        if (name.endsWith("/GlobalDeckModifier$1") || name.endsWith("/SlotDeckModifier$1"))
            return name.startsWith("iskallia/vault/core/card/modifier/deck/");
        if (specs.stream().noneMatch(s -> s.className().equals(name))) return false;
        if (!target.has("method")) return true;
        String method = target.get("method").getAsString();
        return specs.stream().anyMatch(s -> s.className().equals(name) && s.methodName().equals(method))
                || (name.startsWith("iskallia/vault/core/card/") && (method.equals("<clinit>") || method.startsWith("lambda$addText$") || method.startsWith("lambda$appendDeckModifierDetails$") || method.equals("describeRequirement")));
    }
    @Override public void validateVpMigration(List<JsonObject> rules) {
        if (!rules.isEmpty()) throw new IllegalStateException("card_text requires reviewed sentence templates; do not automatically flatten or remove VP fragments");
    }
}
