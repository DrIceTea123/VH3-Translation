package com.dricetea.vh3patch.transformer.modules;
import com.dricetea.vh3patch.transformer.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.List;
import com.google.gson.*;
/** 完整词缀显示组件的返回边界；不改变数值计算、技能 ID 或序列化。 */
public final class GearAffixesModule extends DisplayMethodPatch {
    private static final String REGISTRY = "iskallia/vault/init/ModGearAttributes";
    private static final String REVIVAL = "+Revives you instantly, and heals you fully if you die inside a vault. This effect can occur <$uniqueHighlight>\u0001<reset> times per vault.";
    public GearAffixesModule() { super("gear_affixes", "com/dricetea/vh3patch/modules/GearAffixesModule"); }
    public GearAffixesModule(List<PatchSpec> specs) { super(specs); }
    @Override protected int patch(PatchSpec spec, MethodNode method) {
        int count=0;
        for(var i:method.instructions.toArray()) if(i.getOpcode()==Opcodes.ARETURN) {
            if (method.desc.endsWith("Ljava/lang/String;")) stringHook(method,i,spec,true);
            else hook(method,i,spec,true,"translateDisplay","(Lnet/minecraft/network/chat/MutableComponent;)Lnet/minecraft/network/chat/MutableComponent;");
            count++;
        }
        return count;
    }
    @Override public boolean ownsVpRule(JsonObject rule) {
        rule = VpCompatibility.normalizeRule(rule);
        for (JsonObject target : vpTargets(rule)) {
            if (!target.has("name")) continue;
            if (!target.get("name").getAsString().replace('.','/').equals(REGISTRY)) {
                if (super.ownsTarget(target)) return true;
                continue;
            }
            // 注册类的其他名称/词缀仍由 VP 处理；仅阻止触及复活 formatter 的规则。
            if (target.has("method")) {
                if (target.get("method").getAsString().equals("lambda$static$6")) return true;
                continue;
            }
            if (target.has("local")) return true;
            JsonArray pairs = rule.has("pairs") ? rule.getAsJsonArray("pairs").deepCopy() : new JsonArray();
            if (rule.has("key")) pairs.add(rule);
            for (JsonElement pair : pairs) if (pair.isJsonObject() && pair.getAsJsonObject().has("key")) {
                String key = pair.getAsJsonObject().get("key").getAsString();
                if (REVIVAL.contains(key) || key.contains("Revives you instantly")) return true;
            }
        }
        return false;
    }
}
