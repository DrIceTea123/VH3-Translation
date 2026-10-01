package com.dricetea.vh3patch.transformer;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** 通用实例 (String)String 返回值钩子，不承担模块翻译逻辑。 */
public final class StringReturnPatch {
    private final PatchSpec spec;
    public StringReturnPatch(PatchSpec spec) {
        if (!spec.descriptor().equals("(Ljava/lang/String;)Ljava/lang/String;"))
            throw new IllegalArgumentException("StringReturnPatch requires an instance (String)String method");
        this.spec = spec;
    }
    public MethodNode target(ClassNode node) { return VerifiedMethodPatch.target(node, spec); }
    public void apply(ClassNode node) {
        VerifiedMethodPatch.apply(node, spec, method -> {
            if ((method.access & Opcodes.ACC_STATIC) != 0) throw new IllegalStateException("Unexpected static target");
            int count = 0;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction.getOpcode() != Opcodes.ARETURN) continue;
                // 原结果仍保留在栈上；传给 translate(原始参数, 原结果)，不改变原算法。
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
                hook.add(new InsnNode(Opcodes.SWAP));
                hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, spec.helperClass(), "translate", PatchSpec.HELPER_DESCRIPTOR, false));
                method.instructions.insertBefore(instruction, hook);
                count++;
            }
            return count;
        });
    }
}
