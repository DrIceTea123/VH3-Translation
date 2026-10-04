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
            if(spec.className().equals("iskallia/vault/client/gui/screen/bestiary/BestiaryScreen")) {
                if(call(i,spec.className(),"selectGroup","(Ljava/lang/String;)V")) {
                    // 构造器也接受非族类 predicate，保留其原始行为；只给真实族类生成稳定键。
                    InsnList args=new InsnList();args.add(new VarInsnNode(Opcodes.ALOAD,1));
                    args.add(new LdcInsnNode(Type.getObjectType(GROUP)));
                    args.add(new InvokeDynamicInsnNode("apply","()Ljava/util/function/Function;",
                            new Handle(Opcodes.H_INVOKESTATIC,"java/lang/invoke/LambdaMetafactory","metafactory",
                                    "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",false),
                            Type.getMethodType("(Ljava/lang/Object;)Ljava/lang/Object;"),
                            new Handle(Opcodes.H_INVOKEVIRTUAL,GROUP,"getId","()Lnet/minecraft/resources/ResourceLocation;",false),
                            Type.getMethodType("(L"+GROUP+";)Lnet/minecraft/resources/ResourceLocation;")));
                    method.instructions.insertBefore(i,args);
                    hook(method,i,spec,true,"lookupGroup","(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Class;Ljava/util/function/Function;)Ljava/lang/String;");count++;
                }
            } else if(method.name.equals("lambda$new$0")) {
                // 详情页返回必须沿用原始族类 ID，不能把已翻译标题反解析为资源位置。
                if(call(i,"iskallia/vault/util/GroupUtils","getEntityName","(Liskallia/vault/core/world/data/entity/EntityPredicate;)Lnet/minecraft/network/chat/Component;")) {
                    method.instructions.set(i,new MethodInsnNode(Opcodes.INVOKEVIRTUAL,GROUP,"getId","()Lnet/minecraft/resources/ResourceLocation;",false));count++;
                } else if(call(i,"net/minecraft/network/chat/Component","getString","()Ljava/lang/String;")) {
                    method.instructions.set(i,new MethodInsnNode(Opcodes.INVOKESTATIC,spec.helperClass(),"lookupName","(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/String;",false));count++;
                }
            } else if(method.name.equals("getEntityGroupNames")) {
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
