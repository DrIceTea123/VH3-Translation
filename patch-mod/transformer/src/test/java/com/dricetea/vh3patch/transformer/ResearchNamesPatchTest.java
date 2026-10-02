package com.dricetea.vh3patch.transformer;

import com.dricetea.vh3patch.transformer.modules.ResearchNamesModule;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ResearchNamesPatchTest {
    private final ResearchNamesModule module = new ResearchNamesModule();
    private final Path jar = Path.of(System.getProperty("vh3.test.targetJar"));

    @Test void preClassStagePrecedesRetainedVpClassTranslationsOnBothSides() {
        for (boolean client : new boolean[]{true, false}) {
            var transformers = TranslationTransformationService.transformersFor(client);
            assertEquals(client ? 16 : 4, transformers.size());
            for (var transformer : transformers) for (Object item : transformer.targets()) {
                var target = (cpw.mods.modlauncher.api.ITransformer.Target) item;
                assertEquals("PRE_CLASS", target.getTargetType().name());
                if (!client) assertFalse(target.getClassName().contains(".client."));
            }
        }
    }

    @Test void allDisplaySitesPatchAndRemovingOnlyHooksRestoresEveryOriginalMethod() throws Exception {
        assertEquals(11, module.specs().size());
        assertEquals(12, module.specs().stream().mapToInt(PatchSpec::hookCount).sum());
        assertEquals(2, module.specs(false).size());
        int methods = 0;
        for (String name : module.specs().stream().map(PatchSpec::className).distinct().toList()) {
            PatchSpec first = module.specs().stream().filter(s -> s.className().equals(name)).findFirst().orElseThrow();
            ClassNode node = TargetJar.read(jar, first, true);
            Map<String, String> before = new HashMap<>();
            node.methods.forEach(m -> before.put(m.name + m.desc, MethodFingerprint.of(m)));
            module.apply(node);
            ClassWriter writer = new ClassWriter(0);
            node.accept(new CheckClassAdapter(writer, false));
            new ClassReader(writer.toByteArray());
            for (MethodNode method : node.methods) {
                int hooks = 0;
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (!(insn instanceof MethodInsnNode call) || !call.owner.equals(first.helperClass())) continue;
                    hooks++;
                    if (call.desc.equals(PatchSpec.HELPER_DESCRIPTOR)) {
                        // 配方的原始参数复制也属于钩子；移除后必须逐指令恢复上游方法。
                        AbstractInsnNode dup = call.getPrevious().getPrevious();
                        assertEquals(Opcodes.DUP, dup.getOpcode());
                        method.instructions.remove(dup);
                    }
                    method.instructions.remove(insn);
                }
                var target = module.specs().stream().filter(s -> s.className().equals(name)
                        && s.methodName().equals(method.name) && s.descriptor().equals(method.desc)).findFirst();
                assertEquals(target.map(PatchSpec::hookCount).orElse(0), hooks, name + "." + method.name);
                if (hooks > 0) methods++;
                assertEquals(before.get(method.name + method.desc), MethodFingerprint.of(method), name + "." + method.name);
            }
        }
        assertEquals(11, methods);
    }

    @Test void serverDoesNotModifyClientSitesAndDuplicateApplicationFails() throws Exception {
        for (PatchSpec spec : module.specs()) {
            ClassNode node = TargetJar.read(jar, spec, true);
            String before = MethodFingerprint.of(VerifiedMethodPatch.target(node, spec));
            module.apply(node, false);
            String after = MethodFingerprint.of(VerifiedMethodPatch.target(node, spec));
            if (spec.side() == PatchSpec.Side.CLIENT) assertEquals(before, after);
            else {
                assertNotEquals(before, after);
                assertThrows(IllegalStateException.class, () -> module.apply(node, false));
            }
        }
    }

    @Test void changedDisplaySiteFailsBeforeReplacingMethod() throws Exception {
        PatchSpec spec = module.specs(false).get(0);
        ClassNode node = TargetJar.read(jar, spec, true);
        MethodNode method = VerifiedMethodPatch.target(node, spec);
        method.instructions.insert(new InsnNode(Opcodes.NOP));
        String changed = MethodFingerprint.of(method);
        assertThrows(IllegalStateException.class, () -> module.apply(node, false));
        assertEquals(changed, MethodFingerprint.of(VerifiedMethodPatch.target(node, spec)));
    }

    @Test void importsBothHistoricalGroupsAndResolvesChosenWaystones(@TempDir Path temp) throws Exception {
        Path source = Path.of(System.getProperty("vh3.test.legacyVpDirectory"), "research_names.json");
        module.importMappings(source, jar, temp);
        JsonObject mappings = JsonFiles.read(temp.resolve(module.spec().configPath())).getAsJsonObject();
        assertEquals(54, mappings.size());
        assertEquals("传送石碑", mappings.get("Waystones").getAsString());
        assertEquals("宝库卡组包", mappings.get("Vault Decks").getAsString());
        JsonArray old = JsonFiles.read(source).getAsJsonArray();
        assertEquals(0, VpCompatibility.prepareConfiguration(old, module).size());
        old.add(old.get(0).deepCopy());
        assertThrows(IllegalStateException.class, () -> VpCompatibility.prepareConfiguration(old, module));
    }

    @Test void remainingVpRulesAndTakeoverCommentsArePreserved() throws Exception {
        Path dir = Path.of(System.getProperty("vh3.test.programVpDirectory"));
        assertDoesNotThrow(() -> VpCompatibility.assertCompatible(dir, module));
        JsonArray complex = JsonFiles.read(dir.resolve("the_vault-asm_complex.json")).getAsJsonArray();
        assertEquals(complex, VpCompatibility.prepareConfiguration(complex, module));
        long comments = java.util.stream.StreamSupport.stream(complex.spliterator(), false).filter(e -> e.isJsonObject()
                && e.getAsJsonObject().has("_comment") && e.toString().contains("research_names")).count();
        assertEquals(2, comments);
        JsonObject unrelated = JsonParser.parseString("{\"target_class\":{\"name\":\"iskallia.vault.client.gui.screen.player.legacy.widget.ResearchWidget\"},\"key\":\"Requires\",\"value\":\"需要\"}").getAsJsonObject();
        assertFalse(module.ownsVpRule(unrelated));
    }
}
