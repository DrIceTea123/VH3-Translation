package com.dricetea.vh3patch.transformer.modules;
import com.dricetea.vh3patch.transformer.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.List;

/** 预览绘制前翻译并重新测量宽度，模板加载与通用 getter 不变。 */
public final class OverworldNamesModule extends DisplayMethodPatch {
    public static final String ID="overworld_names";
    public OverworldNamesModule() { super(ID,"com/dricetea/vh3patch/modules/OverworldNamesModule"); }
    public OverworldNamesModule(List<PatchSpec> specs) { super(specs); }
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
