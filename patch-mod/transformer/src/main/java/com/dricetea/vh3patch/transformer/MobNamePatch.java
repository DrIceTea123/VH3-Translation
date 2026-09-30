package com.dricetea.vh3patch.transformer;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicVerifier;

import java.util.List;

/** Never loads Minecraft or the_vault classes in the SERVICE layer. */
public final class MobNamePatch {
    private final PatchSpec spec;

    public MobNamePatch(PatchSpec spec) { this.spec = spec; }

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
            if (instruction instanceof MethodInsnNode call && call.owner.equals(PatchSpec.HELPER)) {
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
        MethodNode patched = new MethodNode(Opcodes.ASM9, original.access, original.name, original.desc,
                original.signature, original.exceptions.toArray(String[]::new));
        original.accept(patched);
        int returns = 0;
        for (AbstractInsnNode instruction : patched.instructions.toArray()) {
            if (instruction.getOpcode() == Opcodes.ARETURN) {
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
                hook.add(new InsnNode(Opcodes.SWAP));
                hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, PatchSpec.HELPER, "translate",
                        PatchSpec.HELPER_DESCRIPTOR, false));
                patched.instructions.insertBefore(instruction, hook);
                returns++;
            }
        }
        if (returns != spec.returnCount()) throw new IllegalStateException("Unexpected return count: " + returns);
        // The hook consumes [fallback, id] and leaves one String, so existing frames remain valid.
        patched.maxStack = Math.max(patched.maxStack, 2);
        try {
            new Analyzer<>(new BasicVerifier()).analyze(node.name, patched);
        } catch (AnalyzerException e) {
            throw new IllegalStateException("Transformed method failed bytecode analysis", e);
        }
        node.methods.set(node.methods.indexOf(original), patched);
    }
}
