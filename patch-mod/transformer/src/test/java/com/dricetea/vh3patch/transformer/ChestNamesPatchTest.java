package com.dricetea.vh3patch.transformer;

import com.dricetea.vh3patch.transformer.modules.ChestNamesModule;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ChestNamesPatchTest {
    private final ChestNamesModule module = new ChestNamesModule();
    private ClassNode original(PatchSpec spec) throws Exception {
        return TargetJar.read(Path.of(System.getProperty("vh3.test.targetJar")), spec, true);
    }
    @Test void fiftyThreeHooksPreserveEveryOriginalInstructionAndComponentFallback() throws Exception {
        int total = 0;
        for (String name : module.specs().stream().map(PatchSpec::className).distinct().toList()) {
            ClassNode node = original(module.specs().stream().filter(s -> s.className().equals(name)).findFirst().orElseThrow());
            Map<String, String> before = new HashMap<>();
            node.methods.forEach(m -> before.put(m.name + m.desc, MethodFingerprint.of(m)));
            module.apply(node);
            ClassWriter writer = new ClassWriter(0);
            node.accept(new CheckClassAdapter(writer, false)); new ClassReader(writer.toByteArray());
            for (MethodNode m : node.methods) {
                int count = 0;
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (!(i instanceof MethodInsnNode call) || !call.owner.equals(module.spec().helperClass())) continue;
                    if (call.name.equals("translateChest")) {
                        for (int slot = 3; slot >= 1; slot--) {
                            VarInsnNode arg = assertInstanceOf(VarInsnNode.class, call.getPrevious());
                            assertEquals(slot, arg.var); m.instructions.remove(arg);
                        }
                    } else if (call.name.equals("translateBarrel")) {
                        VarInsnNode arg = assertInstanceOf(VarInsnNode.class, call.getPrevious());
                        assertEquals(22, arg.var); m.instructions.remove(arg);
                    }
                    m.instructions.remove(call); count++;
                }
                assertEquals(module.specs().stream().filter(s -> s.className().equals(name) && s.methodName().equals(m.name)).mapToInt(PatchSpec::hookCount).sum(), count, name + m.name);
                assertEquals(before.get(m.name + m.desc), MethodFingerprint.of(m), name + m.name); total += count;
            }
        }
        assertEquals(53, total);
    }
    @Test void alteredDuplicateAndWrongHookCountsFailWithoutMutatingMethod() throws Exception {
        for (PatchSpec spec : module.specs()) {
            var single = new ChestNamesModule(List.of(spec));
            ClassNode duplicate = original(spec); single.apply(duplicate);
            assertThrows(IllegalStateException.class, () -> single.apply(duplicate));
            ClassNode changed = original(spec); MethodNode target = VerifiedMethodPatch.target(changed, spec);
            target.instructions.insert(new InsnNode(Opcodes.NOP)); String hash = MethodFingerprint.of(target);
            assertThrows(IllegalStateException.class, () -> single.apply(changed));
            assertEquals(hash, MethodFingerprint.of(VerifiedMethodPatch.target(changed, spec)));
            PatchSpec wrong = new PatchSpec(spec.patchVersion(), spec.targetVersion(), spec.jarHash(), spec.className(), spec.methodName(), spec.descriptor(), spec.fingerprint(), spec.hookCount()+1, spec.moduleId(), spec.helperClass(), spec.ownedLiterals(), spec.side());
            ClassNode wrongNode = original(spec);
            assertThrows(IllegalStateException.class, () -> new ChestNamesModule(List.of(wrong)).apply(wrongNode));
            assertEquals(spec.fingerprint(), MethodFingerprint.of(VerifiedMethodPatch.target(wrongNode, spec)));
        }
    }
    @Test void currentVpIsCompatibleHistoricalPairsConflictAndMixedGroupsCannotBeDropped(@TempDir Path output) throws Exception {
        assertDoesNotThrow(() -> VpCompatibility.assertCompatible(Path.of(System.getProperty("vh3.test.programVpDirectory")), module));
        Path legacy = Path.of(System.getProperty("vh3.test.legacyVpDirectory"), "chest_names.json");
        var rules = JsonFiles.read(legacy).getAsJsonArray();
        for (JsonElement r : rules) assertTrue(module.ownsVpRule(r.getAsJsonObject()));
        assertThrows(IllegalStateException.class, () -> VpCompatibility.prepareConfiguration(rules, module));
        module.importMappings(legacy, Path.of(System.getProperty("vh3.test.targetJar")), output);
        JsonObject imported = JsonFiles.read(output.resolve(module.spec().configPath())).getAsJsonObject();
        assertEquals(33, imported.size());
        assertEquals("强化华丽宝箱", imported.get("Ornate Strongbox").getAsString());
        assertEquals("普通：", imported.get("Common: ").getAsString());
        assertFalse(imported.has(" Barrel"));
    }
}
