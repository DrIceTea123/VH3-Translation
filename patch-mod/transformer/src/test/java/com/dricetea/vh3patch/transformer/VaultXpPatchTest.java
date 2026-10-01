package com.dricetea.vh3patch.transformer;

import com.dricetea.vh3patch.transformer.modules.VaultXpModule;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class VaultXpPatchTest {
    private final VaultXpModule module = new VaultXpModule();
    private ClassNode original() throws Exception {
        return TargetJar.read(Path.of(System.getProperty("vh3.test.targetJar")), module.spec(), true);
    }

    @Test void nineHooksCoverFiveMethodsAndRemovingThemRestoresEveryOriginalInstruction() throws Exception {
        ClassNode node = original();
        Map<String, String> before = new HashMap<>();
        node.methods.forEach(m -> before.put(m.name + m.desc, MethodFingerprint.of(m)));
        module.apply(node);
        ClassWriter writer = new ClassWriter(0);
        node.accept(new CheckClassAdapter(writer, false));
        new ClassReader(writer.toByteArray());
        int total = 0;
        for (MethodNode method : node.methods) {
            int count = 0;
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (!(insn instanceof MethodInsnNode call) || !call.owner.equals(module.spec().helperClass())) continue;
                if (call.name.equals("translateChest")) {
                    for (int slot = 3; slot >= 1; slot--) {
                        VarInsnNode arg = assertInstanceOf(VarInsnNode.class, call.getPrevious());
                        assertEquals(slot, arg.var);
                        assertEquals(slot == 3 ? Opcodes.ILOAD : Opcodes.ALOAD, arg.getOpcode());
                        method.instructions.remove(arg);
                    }
                } else assertEquals("(Ljava/lang/String;)Ljava/lang/String;", call.desc);
                if (method.name.startsWith("format")) assertEquals(Opcodes.ARETURN, call.getNext().getOpcode());
                else assertInstanceOf(LdcInsnNode.class, call.getPrevious());
                method.instructions.remove(call);
                count++;
            }
            int expected = module.specs().stream().filter(s -> s.methodName().equals(method.name))
                    .mapToInt(PatchSpec::hookCount).sum();
            assertEquals(expected, count, method.name);
            total += count;
            // 包括 handleDeltas 内的 XP 算术、业务 ID，以及所有非目标方法。
            assertEquals(before.get(method.name + method.desc), MethodFingerprint.of(method), method.name);
        }
        assertEquals(9, total);
        assertEquals(5, module.specs().size());
    }

    @Test void realFormattersExecuteBothOreBranchesAllChestCombinationsAndUnknownFallback() throws Exception {
        ClassNode before = original();
        ClassNode after = original();
        module.apply(after);
        Object raw = probe(before);
        Object patched = probe(after);
        var mappings = new HashMap<String, String>();
        for (Rarity rarity : Rarity.values()) for (ChestType type : ChestType.values()) for (boolean barrel : new boolean[]{false, true}) {
            String expected = title(rarity.name()) + " " + type.getName() + (barrel ? " Barrel" : " Chest");
            assertEquals(expected, chest(raw, rarity, type, barrel));
            mappings.put(expected, "translated:" + expected);
        }
        mappings.put("Ore Benitoite", "蓝锥矿石");
        mappings.put("Chromatic Iron Ore", "异色铁矿石");
        mappings.put("Aggressive Cow", "战斗牛");
        mappings.put("Aggressive Cow Boss", "战斗牛首领");
        com.dricetea.vh3patch.modules.VaultXpModule.mappings = mappings;
        for (Rarity rarity : Rarity.values()) for (ChestType type : ChestType.values()) for (boolean barrel : new boolean[]{false, true})
            assertEquals("translated:" + chest(raw, rarity, type, barrel), chest(patched, rarity, type, barrel));
        for (String method : List.of("formatOreName", "formatMobName"))
            for (String path : List.of("ore_benitoite", "chromatic_iron_ore", "aggressive_cow", "aggressive_cow_boss", "custom_ore", "unmapped_mob", "mob_type/dungeon_boss")) {
                String fallback = name(raw, method, path);
                assertEquals(mappings.getOrDefault(fallback, fallback), name(patched, method, path));
            }
        com.dricetea.vh3patch.modules.VaultXpModule.mappings = Map.of();
        assertEquals(chest(raw, Rarity.COMMON, ChestType.WOODEN, false), chest(patched, Rarity.COMMON, ChestType.WOODEN, false));
    }

    @Test void vpTranslatedChestTypeStillUsesEnglishKeyAndPreservesMixedFallback() throws Exception {
        ClassNode node = original();
        module.apply(node);
        Object patched = probe(node);
        ChestType.translated = true;
        try {
            com.dricetea.vh3patch.modules.VaultXpModule.mappings = Map.of("Common Wooden Chest", "普通木制宝箱");
            assertEquals("普通木制宝箱", chest(patched, Rarity.COMMON, ChestType.WOODEN, false));
            com.dricetea.vh3patch.modules.VaultXpModule.mappings = Map.of();
            assertEquals("Common 木制 Chest", chest(patched, Rarity.COMMON, ChestType.WOODEN, false));
        } finally { ChestType.translated = false; }
    }

    @Test void serverSkipsModuleAndChangedDuplicateOrWrongCountTargetsAreRejected() throws Exception {
        ClassNode server = original();
        String before = MethodFingerprint.of(module.target(server));
        module.apply(server, false);
        assertEquals(before, MethodFingerprint.of(module.target(server)));
        assertTrue(module.specs(false).isEmpty());
        assertFalse(PatchModules.active(false).stream().anyMatch(m -> m instanceof VaultXpModule));
        ClassNode duplicate = original();
        module.apply(duplicate);
        assertThrows(IllegalStateException.class, () -> module.apply(duplicate));
        for (PatchSpec spec : module.specs()) {
            ClassNode changed = original();
            MethodNode target = VerifiedMethodPatch.target(changed, spec);
            target.instructions.insert(new InsnNode(Opcodes.NOP));
            String hash = MethodFingerprint.of(target);
            assertThrows(IllegalStateException.class, () -> new VaultXpModule(List.of(spec)).apply(changed));
            assertEquals(hash, MethodFingerprint.of(VerifiedMethodPatch.target(changed, spec)));
            ClassNode wrongCount = original();
            PatchSpec wrong = new PatchSpec(spec.patchVersion(), spec.targetVersion(), spec.jarHash(), spec.className(),
                    spec.methodName(), spec.descriptor(), spec.fingerprint(), spec.hookCount() + 1,
                    spec.moduleId(), spec.helperClass(), spec.ownedLiterals(), spec.side());
            assertThrows(IllegalStateException.class, () -> new VaultXpModule(List.of(wrong)).apply(wrongCount));
            assertEquals(spec.fingerprint(), MethodFingerprint.of(VerifiedMethodPatch.target(wrongCount, spec)));
        }
    }

    @Test void currentVpIsCompatibleAndNewConflictsCannotBeSilentlyMigrated() {
        Path vp = Path.of(System.getProperty("vh3.test.programVpDirectory"));
        assertDoesNotThrow(() -> VpCompatibility.assertCompatible(vp, module));
        for (String property : List.of("target_class", "target_classes")) {
            JsonObject target = new JsonObject();
            target.addProperty("name", VaultXpModule.TARGET.replace('/', '.'));
            target.addProperty("method", "formatChestName");
            JsonObject rule = new JsonObject();
            JsonArray targets = new JsonArray(); targets.add(target);
            rule.add(property, property.equals("target_class") ? target : targets);
            assertTrue(module.ownsVpRule(rule));
            assertThrows(IllegalStateException.class, () -> module.validateVpMigration(List.of(rule)));
        }
        assertFalse(module.ownsVpRule(JsonParser.parseString("{\"target_class\":{\"name\":\"other.Class\"}}").getAsJsonObject()));
    }

    public enum Rarity { COMMON, RARE, EPIC, OMEGA }
    public enum ChestType {
        WOODEN, GILDED, LIVING, ORNATE, TREASURE, ALTAR, HARDENED, ENIGMA, FLESH;
        static boolean translated;
        public String getName() { return translated && this == WOODEN ? "木制" : title(name()); }
    }
    public record Resource(String path) { public String m_135815_() { return path; } }
    private static String title(String name) { return name.substring(0, 1) + name.substring(1).toLowerCase(Locale.ROOT); }
    private static String chest(Object probe, Rarity rarity, ChestType type, boolean barrel) throws Exception {
        return (String) probe.getClass().getMethod("formatChestName", Rarity.class, ChestType.class, boolean.class).invoke(probe, rarity, type, barrel);
    }
    private static String name(Object probe, String method, String path) throws Exception {
        return (String) probe.getClass().getMethod(method, Resource.class).invoke(probe, new Resource(path));
    }
    private static Object probe(ClassNode original) throws Exception {
        String owner = "probe/VaultXp";
        Map<String, String> types = Map.of(VaultXpModule.TARGET, owner,
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
