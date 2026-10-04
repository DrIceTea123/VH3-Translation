package com.dricetea.vh3patch.transformer;

import com.google.gson.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.*;

/** 显示模块共用精确方法校验，具体定位与 VP 范围由各模块声明。 */
public abstract class DisplayMethodPatch implements PatchModule {
    protected final List<PatchSpec> targets;
    protected DisplayMethodPatch(String id, String helper) { this(PatchSpec.loadAll(id, helper, Set.of())); }
    protected DisplayMethodPatch(List<PatchSpec> specs) { targets = List.copyOf(specs); }
    @Override public PatchSpec spec() { return targets.get(0); }
    @Override public List<PatchSpec> specs() { return targets; }
    @Override public MethodNode target(ClassNode node) {
        return VerifiedMethodPatch.target(node, targets.stream().filter(s -> s.className().equals(node.name)).findFirst().orElseThrow());
    }
    @Override public void apply(ClassNode node) { apply(node, true); }
    @Override public void apply(ClassNode node, boolean client) {
        for (PatchSpec spec : specs(client)) if (spec.className().equals(node.name))
            VerifiedMethodPatch.apply(node, spec, 2, method -> patch(spec, method));
    }
    protected abstract int patch(PatchSpec spec, MethodNode method);
    protected static boolean call(AbstractInsnNode i, String owner, String name, String desc) {
        return i instanceof MethodInsnNode m && m.owner.equals(owner) && m.name.equals(name) && m.desc.equals(desc);
    }
    protected static void stringHook(MethodNode method, AbstractInsnNode site, PatchSpec spec, boolean before) {
        hook(method, site, spec, before, "translate", "(Ljava/lang/String;)Ljava/lang/String;");
    }
    protected static void hook(MethodNode method, AbstractInsnNode site, PatchSpec spec, boolean before, String name, String desc) {
        var call = new MethodInsnNode(Opcodes.INVOKESTATIC, spec.helperClass(), name, desc, false);
        if (before) method.instructions.insertBefore(site, call); else method.instructions.insert(site, call);
    }
    protected static List<JsonObject> vpTargets(JsonObject rule) {
        rule = VpCompatibility.normalizeRule(rule);
        List<JsonObject> result = new ArrayList<>();
        if (rule.has("target_class") && rule.get("target_class").isJsonObject()) result.add(rule.getAsJsonObject("target_class"));
        if (rule.has("target_classes")) for (JsonElement t : rule.getAsJsonArray("target_classes")) if (t.isJsonObject()) result.add(t.getAsJsonObject());
        return result;
    }
    protected boolean ownsTarget(JsonObject target) {
        if (!target.has("name")) return false;
        String owner = target.get("name").getAsString().replace('.', '/');
        return targets.stream().anyMatch(s -> s.className().equals(owner)
                && (!target.has("method") || target.get("method").getAsString().equals(s.methodName())));
    }
    @Override public boolean ownsVpRule(JsonObject rule) { return vpTargets(rule).stream().anyMatch(this::ownsTarget); }
    @Override public void validateVpMigration(List<JsonObject> rules) {
        if (!rules.isEmpty()) throw new IllegalStateException(spec().moduleId()+": reviewed display migration required; refusing automatic deletion");
    }
}
