package com.dricetea.vh3patch.transformer;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicVerifier;

import java.util.List;

/** 通用返回值钩子：仅适用于实例方法 (String)String，不承担任何模块的翻译逻辑。 */
public final class StringReturnPatch {
    private final PatchSpec spec;

    public StringReturnPatch(PatchSpec spec) {
        if (!spec.descriptor().equals("(Ljava/lang/String;)Ljava/lang/String;")) {
            throw new IllegalArgumentException("StringReturnPatch requires an instance (String)String method");
        }
        this.spec = spec;
    }

    public MethodNode target(ClassNode node) {
        if (!node.name.equals(spec.className())) throw new IllegalStateException("Wrong target class: " + node.name);
        List<MethodNode> matches = node.methods.stream()
                .filter(m -> m.name.equals(spec.methodName()) && m.desc.equals(spec.descriptor())).toList();
        if (matches.size() != 1) throw new IllegalStateException("Expected one target method, found " + matches.size());
        return matches.get(0);
    }

    public void apply(ClassNode node) {
        MethodNode original = target(node);
        for (AbstractInsnNode instruction : original.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(spec.helperClass())) {
                throw new IllegalStateException("Patch already applied; check duplicate installations");
            }
        }
        String actual = MethodFingerprint.of(original);
        if (!actual.equals(spec.fingerprint())) {
            throw new IllegalStateException("Target method changed or another transformer modified it. Expected "
                    + spec.fingerprint() + ", got " + actual);
        }
        if ((original.access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
            throw new IllegalStateException("Unexpected target method access flags");
        }
        // 先改副本，只有摘要、返回点数量和字节码分析全部通过，才替换原方法。
        MethodNode patched = new MethodNode(Opcodes.ASM9, original.access, original.name, original.desc,
                original.signature, original.exceptions.toArray(String[]::new));
        original.accept(patched);
        int returns = 0;
        for (AbstractInsnNode instruction : patched.instructions.toArray()) {
            if (instruction.getOpcode() == Opcodes.ARETURN) {
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
                hook.add(new InsnNode(Opcodes.SWAP));
                hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, spec.helperClass(), "translate",
                        PatchSpec.HELPER_DESCRIPTOR, false));
                patched.instructions.insertBefore(instruction, hook);
                returns++;
            }
        }
        if (returns != spec.returnCount()) throw new IllegalStateException("Unexpected return count: " + returns);
        // 返回前已有原结果；压入原始参数再交换，交给 translate(原始参数, 原结果)。
        // 调用后仍只有一个 String，原栈帧保持有效。
        patched.maxStack = Math.max(patched.maxStack, 2);
        try {
            new Analyzer<>(new BasicVerifier()).analyze(node.name, patched);
        } catch (AnalyzerException e) {
            throw new IllegalStateException("Transformed method failed bytecode analysis", e);
        }
        node.methods.set(node.methods.indexOf(original), patched);
    }
}
