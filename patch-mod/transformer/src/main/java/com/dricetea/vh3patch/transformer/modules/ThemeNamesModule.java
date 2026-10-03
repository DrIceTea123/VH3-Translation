package com.dricetea.vh3patch.transformer.modules;
import com.dricetea.vh3patch.transformer.*;
import com.google.gson.JsonObject;
import org.objectweb.asm.tree.*;
import java.util.List;

/** 不修改主题 getter：ThemeEntityRegistry 的内部索引仍使用原名。 */
public final class ThemeNamesModule extends DisplayMethodPatch {
    public static final String ID = "theme_names";
    public ThemeNamesModule() { super(ID,"com/dricetea/vh3patch/modules/ThemeNamesModule"); }
    public ThemeNamesModule(List<PatchSpec> specs) { super(specs); }
    @Override protected int patch(PatchSpec spec, MethodNode method) {
        int count=0;
        for(var i:method.instructions.toArray()) if(call(i,"iskallia/vault/core/data/key/ThemeKey","getName","()Ljava/lang/String;")) {
            stringHook(method,i,spec,false);count++;
        }
        return count;
    }
    @Override protected boolean ownsTarget(JsonObject t) {
        if (!t.has("name")) return false;
        if (t.get("name").getAsString().replace('.','/').equals("iskallia/vault/core/data/key/ThemeKey")) return true;
        // 同类 Theme: 等固定提示留 VP；只禁止再次接管主题名称 getter 的调用结果。
        return super.ownsTarget(t) && t.has("local") && t.get("local").getAsString().equals("MgetName");
    }
}
