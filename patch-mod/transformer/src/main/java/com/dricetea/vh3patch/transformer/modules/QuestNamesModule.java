package com.dricetea.vh3patch.transformer.modules;
import com.dricetea.vh3patch.transformer.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import com.google.gson.JsonObject;
import java.util.List;

/** Quest ID、进度与 getName 本身不变；标题和通知名称单独查表。 */
public final class QuestNamesModule extends DisplayMethodPatch {
    public static final String ID="quest_names";
    public QuestNamesModule() { super(ID,"com/dricetea/vh3patch/modules/QuestNamesModule"); }
    public QuestNamesModule(List<PatchSpec> specs) { super(specs); }
    @Override protected int patch(PatchSpec spec, MethodNode method) {
        int count=0;
        for(var i:method.instructions.toArray()) {
            if(call(i,"iskallia/vault/quest/base/Quest","getName","()Ljava/lang/String;")) {stringHook(method,i,spec,false);count++;}
            else if(method.name.equals("toast") && i instanceof VarInsnNode v && v.getOpcode()==Opcodes.ALOAD && v.var==0) {stringHook(method,i,spec,false);count++;}
        }
        return count;
    }
    @Override protected boolean ownsTarget(JsonObject t) {
        // 同类普通提示仍可留给 VP，仅禁止显示 getter/通知标题被重复接管。
        return super.ownsTarget(t) && t.has("local") && java.util.Set.of("MgetName","Vtitle").contains(t.get("local").getAsString());
    }
}
