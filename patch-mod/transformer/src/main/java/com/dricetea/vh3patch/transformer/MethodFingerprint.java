package com.dricetea.vh3patch.transformer;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.Set;

/** 摘要算法 v1：忽略栈帧、行号、局部变量名和无用调试标签，只在副本上归一化。 */
public final class MethodFingerprint {
    private MethodFingerprint() {}

    public static String of(MethodNode method) {
        MethodNode copy = new MethodNode(Opcodes.ASM9, method.access, method.name, method.desc,
                method.signature, method.exceptions.toArray(String[]::new));
        method.accept(copy);
        copy.localVariables = null;
        copy.visibleLocalVariableAnnotations = null;
        copy.invisibleLocalVariableAnnotations = null;
        // 跳转与异常处理所依赖的标签必须保留，不能把真实控制流当作调试信息删掉。
        Set<LabelNode> used = Collections.newSetFromMap(new IdentityHashMap<>());
        for (AbstractInsnNode instruction : copy.instructions) {
            if (instruction instanceof JumpInsnNode jump) used.add(jump.label);
            if (instruction instanceof TableSwitchInsnNode table) {
                used.add(table.dflt);
                used.addAll(table.labels);
            }
            if (instruction instanceof LookupSwitchInsnNode lookup) {
                used.add(lookup.dflt);
                used.addAll(lookup.labels);
            }
        }
        for (TryCatchBlockNode block : copy.tryCatchBlocks) {
            used.add(block.start);
            used.add(block.end);
            used.add(block.handler);
        }
        for (AbstractInsnNode instruction : copy.instructions.toArray()) {
            if (instruction instanceof LineNumberNode || instruction instanceof FrameNode
                    || instruction instanceof LabelNode label && !used.contains(label)) {
                copy.instructions.remove(instruction);
            }
        }
        copy.maxLocals = 0;
        copy.maxStack = 0;
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "vh3/PatchFingerprintV1", null, "java/lang/Object", null);
        copy.accept(writer);
        writer.visitEnd();
        return sha256(writer.toByteArray());
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
