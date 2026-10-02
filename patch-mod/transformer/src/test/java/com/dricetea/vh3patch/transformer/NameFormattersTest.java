package com.dricetea.vh3patch.transformer;

import com.dricetea.vh3patch.transformer.modules.ChestNamesModule;
import com.dricetea.vh3patch.transformer.modules.MobNamesModule;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class NameFormattersTest {
    private ClassNode original() throws Exception {
        return TargetJar.read(Path.of(System.getProperty("vh3.test.targetJar")), new MobNamesModule().specs().get(1), true);
    }
    @Test void bothModulesExecuteTogetherWithStableIdsAndAllSeventyTwoChestCombinations() throws Exception {
        ClassNode node = original();
        Object raw = probe(original());
        new MobNamesModule().apply(node); new ChestNamesModule().apply(node);
        Object patched = probe(node);
        Map<String,String> mappings = new HashMap<>();
        for (Rarity rarity : Rarity.values()) for (ChestType type : ChestType.values()) for (boolean barrel : new boolean[]{false,true}) {
            String key = chest(raw, rarity, type, barrel); mappings.put(key, "translated:" + key);
        }
        com.dricetea.vh3patch.modules.ChestNamesModule.mappings = mappings;
        ChestType.translated = true;
        try {
            for (Rarity rarity : Rarity.values()) for (ChestType type : ChestType.values()) for (boolean barrel : new boolean[]{false,true}) {
                String key = title(rarity.name()) + " " + title(type.name()) + (barrel ? " Barrel" : " Chest");
                assertEquals("translated:" + key, chest(patched, rarity, type, barrel));
            }
            assertEquals("战斗牛", name(patched, "formatMobName", "aggressive_cow"));
            assertEquals("战斗牛首领", name(patched, "formatMobName", "aggressive_cow_boss"));
            for (String path : List.of("custom_mob", "mob_type/dungeon_boss"))
                assertEquals(name(raw,"formatMobName",path),name(patched,"formatMobName",path));
            for (String path : List.of("ore_benitoite", "chromatic_iron_ore", "custom_ore"))
                assertEquals(name(raw,"formatOreName",path),name(patched,"formatOreName",path));
            com.dricetea.vh3patch.modules.ChestNamesModule.mappings = Map.of();
            assertEquals("Common 木制 Chest", chest(patched,Rarity.COMMON,ChestType.WOODEN,false));
        } finally { ChestType.translated = false; }
    }
    @Test void xpMobHookPreservesAllOtherMethodsAndRejectsWrongBaseline() throws Exception {
        var module = new MobNamesModule(); var spec = module.specs().get(1);
        ClassNode node = original(); Map<String,String> hashes = new HashMap<>();
        node.methods.forEach(m->hashes.put(m.name+m.desc,MethodFingerprint.of(m)));
        module.apply(node);
        for (MethodNode m : node.methods) if(!m.name.equals("formatMobName")) assertEquals(hashes.get(m.name+m.desc),MethodFingerprint.of(m));
        assertThrows(IllegalStateException.class,()->module.apply(node));
        ClassNode changed=original(); VerifiedMethodPatch.target(changed,spec).instructions.insert(new InsnNode(Opcodes.NOP));
        assertThrows(IllegalStateException.class,()->module.apply(changed));
        ClassNode server=original(); module.apply(server,false);
        assertEquals(spec.fingerprint(),MethodFingerprint.of(VerifiedMethodPatch.target(server,spec)));
        assertEquals(2,PatchModules.byClass().get(ChestNamesModule.XP).size());
    }
    public enum Rarity { COMMON, RARE, EPIC, OMEGA }
    public enum ChestType {
        WOODEN, GILDED, LIVING, ORNATE, TREASURE, ALTAR, HARDENED, ENIGMA, FLESH;
        static boolean translated;
        public String getName() { return translated && this == WOODEN ? "木制" : title(name()); }
    }
    public record Resource(String path) { public String m_135815_() { return path; } public String toString() { return "the_vault:" + path; } }
    private static String title(String name) { return name.substring(0, 1) + name.substring(1).toLowerCase(Locale.ROOT); }
    private static String chest(Object probe, Rarity rarity, ChestType type, boolean barrel) throws Exception {
        return (String) probe.getClass().getMethod("formatChestName", Rarity.class, ChestType.class, boolean.class).invoke(probe, rarity, type, barrel);
    }
    private static String name(Object probe, String method, String path) throws Exception {
        return (String) probe.getClass().getMethod(method, Resource.class).invoke(probe, new Resource(path));
    }
    private static Object probe(ClassNode original) throws Exception {
        String owner = "probe/VaultXp";
        Map<String, String> types = Map.of(ChestNamesModule.XP, owner,
                "iskallia/vault/util/VaultRarity", Type.getInternalName(Rarity.class),
                "iskallia/vault/core/vault/stat/VaultChestType", Type.getInternalName(ChestType.class),
                "net/minecraft/resources/ResourceLocation", Type.getInternalName(Resource.class));
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);
        MethodVisitor ctor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.visitCode(); ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(Opcodes.RETURN); ctor.visitMaxs(1, 1); ctor.visitEnd();
        for (MethodNode originalMethod : original.methods) {
            if (!Set.of("formatChestName", "formatOreName", "formatMobName", "capitalize").contains(originalMethod.name)) continue;
            String descriptor = originalMethod.desc;
            for (var e : types.entrySet()) descriptor = descriptor.replace("L" + e.getKey() + ";", "L" + e.getValue() + ";");
            MethodNode method = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC, originalMethod.name, descriptor, null, null);
            originalMethod.accept(method);
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call) call.owner = types.getOrDefault(call.owner, call.owner);
                if (insn instanceof FrameNode frame) {
                    if (frame.local != null) frame.local.replaceAll(v -> v instanceof String s ? types.getOrDefault(s, s) : v);
                    if (frame.stack != null) frame.stack.replaceAll(v -> v instanceof String s ? types.getOrDefault(s, s) : v);
                }
            }
            method.accept(writer);
        }
        writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        class Loader extends ClassLoader {
            Class<?> define() { return defineClass("probe.VaultXp", bytes, 0, bytes.length); }
        }
        return new Loader().define().getConstructor().newInstance();
    }
}
