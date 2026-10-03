package com.dricetea.vh3patch.transformer.modules;
import com.dricetea.vh3patch.transformer.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import com.google.gson.JsonObject;
import java.util.List;

/** 等上游 ID 解析、分类及详细模式格式化完成，才翻译最终提示。 */
public final class RoomNamesModule extends DisplayMethodPatch {
    public static final String ID="room_names";
    public RoomNamesModule() { super(ID,"com/dricetea/vh3patch/modules/RoomNamesModule"); }
    public RoomNamesModule(List<PatchSpec> specs) { super(specs); }
    @Override protected int patch(PatchSpec spec, MethodNode method) {
        int count=0;
        for(var i:method.instructions.toArray()) if(i.getOpcode()==Opcodes.ARETURN) { stringHook(method,i,spec,true);count++; }
        return count;
    }
    @Override protected boolean ownsTarget(JsonObject t) {
        return t.has("name") && t.get("name").getAsString().replace('.','/').equals("iskallia/vault/client/map/VaultMapRenderHelper");
    }
}
