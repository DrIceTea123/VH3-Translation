package com.dricetea.vh3patch.transformer;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import java.util.function.ToIntFunction;

/** 共享方法校验与原子替换；具体模块只决定在哪些显示边界插入钩子。 */
public final class VerifiedMethodPatch {
    private VerifiedMethodPatch() {}

    public static MethodNode target(ClassNode node, PatchSpec spec) {
        if (!node.name.equals(spec.className())) throw new IllegalStateException("Wrong target class: " + node.name);
        var matches = node.methods.stream().filter(m -> m.name.equals(spec.methodName())
                && m.desc.equals(spec.descriptor())).toList();
        if (matches.size() != 1) throw new IllegalStateException("Expected one target method, found " + matches.size());
        return matches.get(0);
    }

    public static void apply(ClassNode node, PatchSpec spec, ToIntFunction<MethodNode> edit) {
        MethodNode original = target(node, spec);
        for (AbstractInsnNode instruction : original.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(spec.helperClass())) {
                throw new IllegalStateException("Patch already applied; check duplicate installations");
            }
        }
        String actual = MethodFingerprint.of(original);
        if (!actual.equals(spec.fingerprint())) throw new IllegalStateException("Target method changed: "
                + spec.className() + "." + spec.methodName() + "; expected " + spec.fingerprint() + ", got " + actual);
        if ((original.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
            throw new IllegalStateException("Unexpected target method access flags");
        }
        // 在副本上编辑，摘要、命中数和字节码均通过后才替换，失败不留下半个方法。
        MethodNode patched = new MethodNode(Opcodes.ASM9, original.access, original.name, original.desc,
                original.signature, original.exceptions.toArray(String[]::new));
        original.accept(patched);
        int count = edit.applyAsInt(patched);
        if (count != spec.hookCount()) throw new IllegalStateException("Unexpected hook count for "
                + spec.methodName() + ": " + count + ", expected " + spec.hookCount());
        // 共享钩子最多额外压入一个引用；原帧的每个分支边界栈结构不变。
        patched.maxStack = original.maxStack + 1;
        try {
            new Analyzer<>(new BasicVerifier()).analyze(node.name, patched);
        } catch (AnalyzerException e) {
            throw new IllegalStateException("Transformed method failed bytecode analysis", e);
        }
        node.methods.set(node.methods.indexOf(original), patched);
    }
}
