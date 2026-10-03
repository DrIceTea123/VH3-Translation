package com.dricetea.vh3patch.transformer;

import com.dricetea.vh3patch.transformer.modules.CardTextModule;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CardTextPatchTest {
    private final CardTextModule module = new CardTextModule();
    private ClassNode original(PatchSpec spec) throws Exception { return TargetJar.read(Path.of(System.getProperty("vh3.test.targetJar")), spec, true); }
    @Test void displayHooksLeaveAllGameLogicAndNonTooltipListsUnchanged() throws Exception {
        int hooks = 0;
        for (String name : module.specs().stream().map(PatchSpec::className).distinct().toList()) {
            ClassNode node = original(module.specs().stream().filter(s -> s.className().equals(name)).findFirst().orElseThrow());
            Map<String,String> hashes = new HashMap<>(); node.methods.forEach(m -> hashes.put(m.name+m.desc, MethodFingerprint.of(m)));
            module.apply(node);
            ClassWriter writer = new ClassWriter(0); node.accept(new CheckClassAdapter(writer,false)); new ClassReader(writer.toByteArray());
            for (MethodNode method : node.methods) {
                for (var i : method.instructions.toArray()) if (i instanceof MethodInsnNode call && call.owner.equals(module.spec().helperClass())) {
                    if (call.name.equals("translateTooltip")) {
                        MethodInsnNode next = assertInstanceOf(MethodInsnNode.class, call.getNext());
                        assertEquals("java/util/List", next.owner); assertTrue(Set.of("add","set").contains(next.name));
                    }
                    method.instructions.remove(i); hooks++;
                }
                assertEquals(hashes.get(method.name+method.desc), MethodFingerprint.of(method), name+method.name);
            }
        }
        assertEquals(36,hooks); assertEquals(21,module.specs().size());
    }
    @Test void serverOnlyPatchesFourCommonDisplaysAndRejectsDoubleApplication() throws Exception {
        assertEquals(4,module.specs(false).size());
        for (PatchSpec spec : module.specs()) {
            var single = new CardTextModule(List.of(spec)); ClassNode node = original(spec); single.apply(node,false);
            String hash = MethodFingerprint.of(VerifiedMethodPatch.target(node,spec));
            if (spec.side() == PatchSpec.Side.CLIENT) assertEquals(spec.fingerprint(),hash);
            else { assertNotEquals(spec.fingerprint(),hash); assertThrows(IllegalStateException.class, () -> single.apply(node,false)); }
        }
    }
    @Test void changedBytecodeAndHookCountsFailAtomically() throws Exception {
        for (PatchSpec spec : module.specs()) {
            ClassNode node=original(spec); MethodNode method=VerifiedMethodPatch.target(node,spec);method.instructions.insert(new InsnNode(Opcodes.NOP));
            String before=MethodFingerprint.of(method);assertThrows(IllegalStateException.class,()->new CardTextModule(List.of(spec)).apply(node));
            assertEquals(before,MethodFingerprint.of(VerifiedMethodPatch.target(node,spec)));
            PatchSpec wrong=new PatchSpec(spec.patchVersion(),spec.targetVersion(),spec.jarHash(),spec.className(),spec.methodName(),spec.descriptor(),spec.fingerprint(),spec.hookCount()+1,spec.moduleId(),spec.helperClass(),spec.ownedLiterals(),spec.side());
            ClassNode clean=original(spec);assertThrows(IllegalStateException.class,()->new CardTextModule(List.of(wrong)).apply(clean));
            assertEquals(spec.fingerprint(),MethodFingerprint.of(VerifiedMethodPatch.target(clean,spec)));
        }
    }
    @Test void vpMigrationCoversThreeFilesAndLeavesUiCommandsInVp() throws Exception {
        Path dir=Path.of(System.getProperty("vh3.test.programVpDirectory"));
        assertDoesNotThrow(()->VpCompatibility.assertCompatible(dir,module));
        var legacy=JsonFiles.read(Path.of(System.getProperty("vh3.test.legacyVpDirectory"),"card_text.json")).getAsJsonArray();
        assertEquals(20,legacy.size()); for(var r:legacy)assertTrue(module.ownsVpRule(r.getAsJsonObject()),r.toString());
        assertThrows(IllegalStateException.class,()->VpCompatibility.prepareConfiguration(legacy,module));
        for(String cls:List.of("iskallia.vault.item.CardDeckItem$1","iskallia.vault.command.modify.ModifyCardSubcommand","iskallia.vault.client.gui.screen.CardBinderScreen")) {
            JsonObject target=new JsonObject();target.addProperty("name",cls);JsonObject rule=new JsonObject();rule.add("target_class",target);assertFalse(module.ownsVpRule(rule));
        }
        String main=Files.readString(dir.resolve("the_vault-asm_main.json"));assertTrue(main.contains("iskallia.vault.item.CardDeckItem$1"));assertTrue(main.contains("iskallia.vault.command.modify.ModifyCardSubcommand"));
    }
}
