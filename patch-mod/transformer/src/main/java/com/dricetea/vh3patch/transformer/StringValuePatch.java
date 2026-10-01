package com.dricetea.vh3patch.transformer;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.function.Predicate;

/** 在已确认的 String 产生点查表；可复用于字段读取、getter 返回值和局部变量。 */
public final class StringValuePatch {
    private StringValuePatch() {}

    /** 任意参数布局的 String 返回方法；仅用完整显示结果作键，不读取或改写业务参数。 */
    public static void applyReturns(ClassNode node, PatchSpec spec) {
        if (!org.objectweb.asm.Type.getReturnType(spec.descriptor()).equals(org.objectweb.asm.Type.getType(String.class)))
            throw new IllegalArgumentException("Expected a String return type");
        VerifiedMethodPatch.apply(node, spec, method -> {
            int count = 0;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction.getOpcode() != Opcodes.ARETURN) continue;
                method.instructions.insertBefore(instruction, new MethodInsnNode(Opcodes.INVOKESTATIC,
                        spec.helperClass(), "translate", "(Ljava/lang/String;)Ljava/lang/String;", false));
                count++;
            }
            return count;
        });
    }

    public static AbstractInsnNode nextCode(AbstractInsnNode instruction) {
        do { instruction = instruction.getNext(); } while (instruction != null && instruction.getOpcode() < 0);
        return instruction;
    }

    public static void apply(ClassNode node, PatchSpec spec, Predicate<AbstractInsnNode> selector, boolean wrapFormatter) {
        VerifiedMethodPatch.apply(node, spec, method -> {
            int count = 0;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!selector.test(instruction)) continue;
                if (wrapFormatter) {
                    // 调用前复制原文，调用后得到 [原文, 格式化结果]；缺少配置时保留原格式。
                    if (!(instruction instanceof MethodInsnNode call) || call.getOpcode() != Opcodes.INVOKESTATIC
                            || !call.desc.equals("(Ljava/lang/String;)Ljava/lang/String;")) {
                        throw new IllegalStateException("Expected static String formatter");
                    }
                    method.instructions.insertBefore(instruction, new InsnNode(Opcodes.DUP));
                }
                method.instructions.insert(instruction, new MethodInsnNode(Opcodes.INVOKESTATIC,
                        spec.helperClass(), "translate", wrapFormatter ? PatchSpec.HELPER_DESCRIPTOR
                        : "(Ljava/lang/String;)Ljava/lang/String;", false));
                count++;
            }
            return count;
        });
    }
}
