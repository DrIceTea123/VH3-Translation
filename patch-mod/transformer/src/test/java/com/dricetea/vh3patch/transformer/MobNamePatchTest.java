package com.dricetea.vh3patch.transformer;

import com.dricetea.vh3patch.transformer.modules.MobNamesModule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MobNamePatchTest {
    private final PatchSpec spec = new MobNamesModule().spec();
    private final Path targetJar = Path.of(System.getProperty("vh3.test.targetJar"));

    private ClassNode original() throws Exception { return TargetJar.read(targetJar, spec, true); }

    @Test void exactBaselineAndAllOtherMethodsPreserved() throws Exception {
        ClassNode node = original();
        Map<String, String> before = new HashMap<>();
        node.methods.forEach(m -> before.put(m.name + m.desc, MethodFingerprint.of(m)));
        new MobNamesModule(spec).apply(node);
        for (MethodNode method : node.methods) {
            if (!method.name.equals(spec.methodName()) || !method.desc.equals(spec.descriptor())) {
                assertEquals(before.get(method.name + method.desc), MethodFingerprint.of(method));
            }
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(new CheckClassAdapter(writer, false));
        assertDoesNotThrow(() -> new ClassReader(writer.toByteArray()));
    }

    @Test void fingerprintIgnoresDebugMetadata() throws Exception {
        MethodNode method = new MobNamesModule(spec).target(original());
        String before = MethodFingerprint.of(method);
        LabelNode label = new LabelNode();
        method.instructions.insert(label);
        method.instructions.insert(label, new LineNumberNode(12345, label));
        method.maxStack += 10;
        method.maxLocals += 10;
        assertEquals(before, MethodFingerprint.of(method));
    }

    @Test void changedMethodIsRejectedWithoutPartialMutation() throws Exception {
        ClassNode node = original();
        MethodNode method = new MobNamesModule(spec).target(node);
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LdcInsnNode literal && "_".equals(literal.cst)) { literal.cst = "-"; break; }
        }
        String before = MethodFingerprint.of(method);
        assertThrows(IllegalStateException.class, () -> new MobNamesModule(spec).apply(node));
        assertEquals(before, MethodFingerprint.of(new MobNamesModule(spec).target(node)));
    }

    @Test void missingSignatureIsRejected() throws Exception {
        ClassNode node = original();
        node.methods.remove(new MobNamesModule(spec).target(node));
        assertThrows(IllegalStateException.class, () -> new MobNamesModule(spec).apply(node));
    }

    @Test void duplicateApplicationIsRejected() throws Exception {
        ClassNode node = original();
        new MobNamesModule(spec).apply(node);
        assertThrows(IllegalStateException.class, () -> new MobNamesModule(spec).apply(node));
    }

    @Test void wrongJarIsRejected(@TempDir Path temp) throws Exception {
        Path bad = temp.resolve("wrong.jar");
        Files.writeString(bad, "not the bound artifact");
        assertThrows(IllegalStateException.class, () -> TargetJar.read(bad, spec, true));
    }

    @Test void realOriginalMethodExecutesWithHooksAndExactFallback() throws Exception {
        ClassNode baseline = original();
        ClassNode patched = original();
        new MobNamesModule(spec).apply(patched);
        Object before = probe(new MobNamesModule(spec).target(baseline), spec.className());
        Object after = probe(new MobNamesModule(spec).target(patched), spec.className());
        assertEquals("战斗牛", invoke(after, "the_vault:aggressive_cow"));
        assertEquals("战斗牛首领", invoke(after, "the_vault:aggressive_cow_boss"));
        for (String id : new String[]{"unknown:some_beast", "minecraft:zombie", "plain", "", "a:__weird__name", "a:b:c"}) {
            assertEquals(invoke(before, id), invoke(after, id), id);
        }
    }

    private static String invoke(Object probe, String value) throws Exception {
        return (String) probe.getClass().getMethod("formatMobName", String.class).invoke(probe, value);
    }

    private static Object probe(MethodNode originalMethod, String originalOwner) throws Exception {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "probe/MobNames", null, "java/lang/Object", null);
        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();
        MethodNode method = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC, originalMethod.name,
                originalMethod.desc, null, null);
        originalMethod.accept(method);
        // Relocate only the receiver type in stack-map frames to the isolated test owner.
        // Production transformation keeps the original class and all its frames unchanged.
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FrameNode frame) {
                if (frame.local != null) frame.local.replaceAll(value -> originalOwner.equals(value) ? "probe/MobNames" : value);
                if (frame.stack != null) frame.stack.replaceAll(value -> originalOwner.equals(value) ? "probe/MobNames" : value);
            }
        }
        method.accept(writer);
        writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        class ProbeLoader extends ClassLoader {
            ProbeLoader() { super(MobNamePatchTest.class.getClassLoader()); }
            Class<?> define() { return defineClass("probe.MobNames", bytes, 0, bytes.length); }
        }
        return new ProbeLoader().define().getConstructor().newInstance();
    }
}
