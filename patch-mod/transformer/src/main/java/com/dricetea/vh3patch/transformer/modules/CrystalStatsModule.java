package com.dricetea.vh3patch.transformer.modules;
import com.dricetea.vh3patch.transformer.*;
import com.google.gson.JsonObject;
import org.objectweb.asm.tree.*;
import java.util.List;

/** 统计值先完成计算/分类，直到显示 builder 才转换；数字保持原对象。 */
public final class CrystalStatsModule extends DisplayMethodPatch {
    public static final String ID = "crystal_stats";
    public CrystalStatsModule() { super(ID, "com/dricetea/vh3patch/modules/CrystalStatsModule"); }
    public CrystalStatsModule(List<PatchSpec> specs) { super(specs); }
    @Override protected int patch(PatchSpec spec, MethodNode method) {
        int count = 0;
        for (var i : method.instructions.toArray()) {
            if (!(i instanceof MethodInsnNode m)) continue;
            if (m.owner.endsWith("$Builder") && m.owner.startsWith("iskallia/vault/client/gui/screen/summary/element/")) {
                if ((m.name.equals("name") || m.name.equals("description")) && m.desc.startsWith("(Ljava/lang/String;)")) {
                    stringHook(method,i,spec,true); count++;
                } else if (m.name.equals("value") && m.desc.startsWith("(Ljava/lang/Object;)")) {
                    hook(method,i,spec,true,"translateValue","(Ljava/lang/Object;)Ljava/lang/Object;"); count++;
                }
            } else if (call(i,"net/minecraft/network/chat/TextComponent","<init>","(Ljava/lang/String;)V")) {
                stringHook(method,i,spec,true); count++;
            }
        }
        return count;
    }
    @Override protected boolean ownsTarget(JsonObject t) {
        return super.ownsTarget(t) || (t.has("name") && t.get("name").getAsString().equals("@iskallia.vault.client.gui.screen.summary.element"));
    }
}
