package com.dricetea.vh3patch.transformer.modules;
import com.dricetea.vh3patch.transformer.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.List;
import java.util.Set;
import com.google.gson.*;

/** 预览绘制前翻译并重新测量宽度，模板加载与通用 getter 不变。 */
public final class OverworldNamesModule extends DisplayMethodPatch {
    public static final String ID="overworld_names";
    public OverworldNamesModule() { super(ID,"com/dricetea/vh3patch/modules/OverworldNamesModule"); }
    public OverworldNamesModule(List<PatchSpec> specs) { super(specs); }
    @Override public boolean ownsVpRule(JsonObject rule) {
        for(JsonObject t:vpTargets(rule)) {
            if(!t.has("name") || !t.get("name").getAsString().replace('.','/').equals(spec().className())) continue;
            if(t.has("local") && t.get("local").getAsString().equals("MgetDisplayName")) return true;
            if(t.has("method") && t.get("method").getAsString().equals("getDisplayName")) return true;
            if(!super.ownsTarget(t)) continue;
            JsonArray pairs=rule.has("pairs")?rule.getAsJsonArray("pairs"):new JsonArray();if(rule.has("key"))pairs.add(rule);
            // 状态提示不属于结构名称，保留既有 VP 覆盖。
            for(JsonElement p:pairs) if(p.isJsonObject() && p.getAsJsonObject().has("key")
                && !Set.of("Loading","Unknown","Not Found").contains(p.getAsJsonObject().get("key").getAsString())) return true;
        }
        return false;
    }
    @Override protected int patch(PatchSpec spec, MethodNode method) {
        int count=0;
        for(var i:method.instructions.toArray()) if(call(i,spec.className(),"getDisplayName","()Ljava/lang/String;")) {
            InsnList hook=new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD,0));
            hook.add(new FieldInsnNode(Opcodes.GETFIELD,spec.className(),"currentStructureId","Lnet/minecraft/resources/ResourceLocation;"));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,spec.helperClass(),"translatePreview","(Ljava/lang/String;Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/String;",false));
            method.instructions.insert(i,hook);count++;
        }
        return count;
    }
}
