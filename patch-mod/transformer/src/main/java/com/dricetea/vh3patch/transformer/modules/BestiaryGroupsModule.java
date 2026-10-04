package com.dricetea.vh3patch.transformer.modules;
import com.dricetea.vh3patch.transformer.*;
import com.google.gson.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;

/** 查找链从原始 group ID 生成英文键；显示链单独翻译，不需要中文反查。 */
public final class BestiaryGroupsModule extends DisplayMethodPatch {
    public static final String ID="bestiary_groups";
    private static final String GROUP="iskallia/vault/core/world/data/entity/PartialEntityGroup";
    public BestiaryGroupsModule() { super(ID,"com/dricetea/vh3patch/modules/BestiaryGroupsModule"); }
    public BestiaryGroupsModule(List<PatchSpec> specs) { super(specs); }
    @Override protected int patch(PatchSpec spec, MethodNode method) {
        int count=0;
        for(var i:method.instructions.toArray()) {
            if(method.name.equals("getEntityGroupNames")) {
                if (!(i instanceof InvokeDynamicInsnNode d) || !d.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")) continue;
                if (!(d.bsmArgs[1] instanceof Handle h)) continue;
                if(h.getOwner().equals("iskallia/vault/util/GroupUtils") && h.getName().equals("getEntityName")) {
                    d.bsmArgs[1]=new Handle(Opcodes.H_INVOKEVIRTUAL,GROUP,"getId","()Lnet/minecraft/resources/ResourceLocation;",false);
                    d.bsmArgs[2]=Type.getMethodType("(L"+GROUP+";)Lnet/minecraft/resources/ResourceLocation;");count++;
                } else if(h.getOwner().equals("net/minecraft/network/chat/Component") && h.getName().equals("getString")) {
                    d.bsmArgs[1]=new Handle(Opcodes.H_INVOKESTATIC,spec.helperClass(),"lookupName","(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/String;",false);
                    d.bsmArgs[2]=Type.getMethodType("(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/String;");count++;
                }
            } else if(call(i,"net/minecraft/network/chat/TextComponent","<init>","(Ljava/lang/String;)V")) {
                stringHook(method,i,spec,true);count++;
            }
        }
        return count;
    }
    @Override public boolean ownsVpRule(JsonObject rule) {
        rule = VpCompatibility.normalizeRule(rule);
        for(JsonObject t:vpTargets(rule)) {
            if(!t.has("name"))continue;
            String owner=t.get("name").getAsString().replace('.','/');
            if(owner.endsWith("/EntityGroupElement") || owner.endsWith("/GroupListElement"))return true;
            if(owner.equals("iskallia/vault/util/TextUtil")) {
                // TextUtil 同一方法里的悬赏种类与维度译名仍由 VP 维护。
                JsonArray pairs=rule.has("pairs")?rule.getAsJsonArray("pairs"):new JsonArray();
                if(rule.has("key"))pairs.add(rule);
                for(JsonElement e:pairs) if(e.isJsonObject() && e.getAsJsonObject().has("key") && Set.of("Horde","Assassin","Tank","Dungeon","Guardians","Illagers","Champion","Dungeon Boss","Minions","Elite","Bosses","Vessel","Greed Assassin").contains(e.getAsJsonObject().get("key").getAsString()))return true;
            } else if(super.ownsTarget(t))return true;
        }
        return false;
    }
}
