package com.dricetea.vh3patch.transformer.modules;
import com.dricetea.vh3patch.transformer.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.List;
/** 完整词缀显示组件的返回边界；不改变数值计算、技能 ID 或序列化。 */
public final class GearAffixesModule extends DisplayMethodPatch {
    public GearAffixesModule() { super("gear_affixes", "com/dricetea/vh3patch/modules/GearAffixesModule"); }
    public GearAffixesModule(List<PatchSpec> specs) { super(specs); }
    @Override protected int patch(PatchSpec spec, MethodNode method) {
        int count=0;
        for(var i:method.instructions.toArray()) if(i.getOpcode()==Opcodes.ARETURN) {
            hook(method,i,spec,true,"translateDisplay","(Lnet/minecraft/network/chat/MutableComponent;)Lnet/minecraft/network/chat/MutableComponent;");count++;
        }
        return count;
    }
}
