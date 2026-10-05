package com.dricetea.vh3patch.transformer.modules;
import com.dricetea.vh3patch.transformer.*;
import com.google.gson.JsonObject;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.Handle;
import java.util.List;

/** 不修改主题 getter：ThemeEntityRegistry 的内部索引仍使用原名。 */
public final class ThemeNamesModule extends DisplayMethodPatch {
    public static final String ID = "theme_names";
    private static final String BESTIARY_DETAIL = "iskallia/vault/client/gui/screen/bestiary/element/EntityDefinitionElement";
    public ThemeNamesModule() { super(ID,"com/dricetea/vh3patch/modules/ThemeNamesModule"); }
    public ThemeNamesModule(List<PatchSpec> specs) { super(specs); }
    @Override protected int patch(PatchSpec spec, MethodNode method) {
        int count=0;
        for(var i:method.instructions.toArray()) {
            if(call(i,"iskallia/vault/core/data/key/ThemeKey","getName","()Ljava/lang/String;")) {
                stringHook(method,i,spec,false);count++;
            } else if(spec.className().equals(BESTIARY_DETAIL) && method.name.equals("<init>")
                    && i instanceof InvokeDynamicInsnNode concat
                    && concat.bsm.getOwner().equals("java/lang/invoke/StringConcatFactory")
                    && concat.name.equals("makeConcatWithConstants")
                    && concat.desc.equals("(Ljava/lang/String;)Ljava/lang/String;")
                    && concat.bsmArgs.length == 1 && "-\u0001".equals(concat.bsmArgs[0])) {
                // 图鉴先以原名判断 Missing 占位，再在加列表前缀之前翻译；不改注册表或 getThemes。
                stringHook(method,i,spec,true);count++;
            } else if(i instanceof InvokeDynamicInsnNode dynamic
                    && dynamic.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")
                    && dynamic.bsmArgs.length > 1 && dynamic.bsmArgs[1] instanceof Handle handle
                    && handle.getOwner().equals("iskallia/vault/core/data/key/NamedKey")
                    && handle.getName().equals("getName") && handle.getDesc().equals("()Ljava/lang/String;")) {
                // 虚空坩埚以方法引用获取名称：仅包装显示 mapper，保留原始主题 ID 和 getter。
                hook(method,i,spec,false,"translateNames","(Ljava/util/function/Function;)Ljava/util/function/Function;");count++;
            }
        }
        return count;
    }
    @Override protected boolean ownsTarget(JsonObject t) {
        if (!t.has("name")) return false;
        if (t.get("name").getAsString().replace('.','/').equals("iskallia/vault/core/data/key/ThemeKey")) return true;
        if (t.get("name").getAsString().replace('.','/').equals(BESTIARY_DETAIL)) return super.ownsTarget(t);
        // 同类 Theme: 等固定提示留 VP；只禁止再次接管主题名称 getter 的调用结果。
        return super.ownsTarget(t) && t.has("local") && t.get("local").getAsString().equals("MgetName");
    }
}
