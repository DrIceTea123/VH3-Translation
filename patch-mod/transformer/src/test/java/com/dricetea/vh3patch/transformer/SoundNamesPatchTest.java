package com.dricetea.vh3patch.transformer;

import com.dricetea.vh3patch.transformer.modules.SoundNamesModule;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

class SoundNamesPatchTest {
    private final SoundNamesModule module = new SoundNamesModule();
    private final PatchSpec spec = module.spec();
    private final Path targetJar = Path.of(System.getProperty("vh3.test.targetJar"));

    @Test void allMappingsMatchRealFormatterAndOnlyTargetMethodChanges(@TempDir Path output) throws Exception {
        ClassNode before = TargetJar.read(targetJar, spec, true);
        ClassNode after = TargetJar.read(targetJar, spec, true);
        Map<String, String> methods = new HashMap<>();
        before.methods.forEach(m -> methods.put(m.name + m.desc, MethodFingerprint.of(m)));
        module.apply(after);
        for (MethodNode method : after.methods) {
            if (!method.name.equals(spec.methodName()) || !method.desc.equals(spec.descriptor())) {
                assertEquals(methods.get(method.name + method.desc), MethodFingerprint.of(method));
            }
        }
        Object original = probe(module.target(before), spec.className());
        Object patched = probe(module.target(after), spec.className());
        module.importMappings(Path.of(System.getProperty("vh3.test.legacyVpDirectory"), "sound_names.json"), targetJar, output);
        JsonObject defaults = JsonFiles.read(output.resolve(spec.configPath())).getAsJsonObject();
        com.dricetea.vh3patch.modules.SoundNamesModule.mappings = defaults;
        assertEquals(203, defaults.size());
        for (var entry : defaults.entrySet()) {
            assertEquals(SoundNamesModule.legacyDisplayName(entry.getKey()), invoke(original, entry.getKey()));
            assertEquals(entry.getValue().getAsString(), invoke(patched, entry.getKey()));
        }
        for (String field : new String[]{"UNMAPPED_SFX", "RAFFLE_SFX_NEW", "CUSTOM_SOUND", "other"}) {
            assertEquals(invoke(original, field), invoke(patched, field));
        }
    }

    @Test void changedMethodAndDuplicateAreRejected() throws Exception {
        ClassNode changed = TargetJar.read(targetJar, spec, true);
        module.target(changed).instructions.insert(new InsnNode(Opcodes.NOP));
        String fingerprint = MethodFingerprint.of(module.target(changed));
        assertThrows(IllegalStateException.class, () -> module.apply(changed));
        assertEquals(fingerprint, MethodFingerprint.of(module.target(changed)));
        ClassNode duplicate = TargetJar.read(targetJar, spec, true);
        module.apply(duplicate);
        assertThrows(IllegalStateException.class, () -> module.apply(duplicate));
    }

    @Test void callerSideVpRuleConflictsButOrdinaryUiRulesDoNot(@TempDir Path temp) throws Exception {
        JsonArray legacy = JsonFiles.read(Path.of(System.getProperty("vh3.test.legacyVpDirectory"), "sound_names.json")).getAsJsonArray();
        assertTrue(module.ownsVpRule(legacy.get(0).getAsJsonObject()));
        JsonFiles.write(temp.resolve("sound.json"), legacy);
        assertThrows(IllegalStateException.class, () -> VpCompatibility.assertCompatible(temp, module));
        assertEquals(0, VpCompatibility.prepareConfiguration(legacy, module).size());
        Path currentDir = Path.of(System.getProperty("vh3.test.programVpDirectory"));
        assertDoesNotThrow(() -> VpCompatibility.assertCompatible(currentDir, module));
        JsonArray current = JsonFiles.read(currentDir.resolve("the_vault-asm_complex.json")).getAsJsonArray();
        boolean keptUi = false;
        // 普通界面规则可由维护者移至 main；分类位置不属于声音模块的功能约束。
        current.addAll(JsonFiles.read(currentDir.resolve("the_vault-asm_main.json")).getAsJsonArray());
        for (JsonElement item : current) {
            JsonObject rule = item.getAsJsonObject();
            if (rule.has("target_class") && rule.get("target_class").isJsonObject()
                    && rule.getAsJsonObject("target_class").has("name")
                    && rule.getAsJsonObject("target_class").get("name").getAsString().replace('.', '/').equals(spec.className())) {
                assertFalse(module.ownsVpRule(rule));
                assertEquals(6, rule.getAsJsonArray("pairs").size());
                keptUi = true;
            }
        }
        assertTrue(keptUi);
        JsonObject direct = JsonParser.parseString("{\"target_class\":{\"name\":\"" + spec.className() + "\",\"method\":\"formatSoundName\"}}").getAsJsonObject();
        assertTrue(module.ownsVpRule(direct));
    }

    private static String invoke(Object probe, String value) throws Exception {
        return (String) probe.getClass().getMethod("formatSoundName", String.class).invoke(probe, value);
    }

    private static Object probe(MethodNode originalMethod, String originalOwner) throws Exception {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "probe/SoundNames", null, "java/lang/Object", null);
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
                if (frame.local != null) frame.local.replaceAll(value -> originalOwner.equals(value) ? "probe/SoundNames" : value);
                if (frame.stack != null) frame.stack.replaceAll(value -> originalOwner.equals(value) ? "probe/SoundNames" : value);
            }
        }
        method.accept(writer);
        writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        class ProbeLoader extends ClassLoader {
            ProbeLoader() { super(SoundNamesPatchTest.class.getClassLoader()); }
            Class<?> define() { return defineClass("probe.SoundNames", bytes, 0, bytes.length); }
        }
        return new ProbeLoader().define().getConstructor().newInstance();
    }
}
