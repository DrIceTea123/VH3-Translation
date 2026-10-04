package com.dricetea.vh3patch.transformer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class DisplayTakeoverTest {
    static Stream<PatchModule> modules() { return PatchModules.all().stream().filter(m -> m instanceof DisplayMethodPatch); }
    private ClassNode original(PatchSpec spec) throws Exception { return TargetJar.read(Path.of(System.getProperty("vh3.test.targetJar")), spec, true); }
    private PatchModule single(PatchModule module, PatchSpec spec) throws Exception { return module.getClass().getConstructor(List.class).newInstance(List.of(spec)); }

    @ParameterizedTest @MethodSource("modules")
    void onlyReviewedDisplayEditsChangeAndBusinessMethodsRemainExact(PatchModule module) throws Exception {
        int total=0;
        for(String owner:module.specs().stream().map(PatchSpec::className).distinct().toList()) {
            PatchSpec first=module.specs().stream().filter(s->s.className().equals(owner)).findFirst().orElseThrow();
            ClassNode clean=original(first), node=original(first);module.apply(node);
            ClassWriter writer=new ClassWriter(0);node.accept(new CheckClassAdapter(writer,false));new ClassReader(writer.toByteArray());
            for(int n=0;n<node.methods.size();n++) {
                MethodNode changed=node.methods.get(n), before=clean.methods.get(n);
                var oldDynamics=Arrays.stream(before.instructions.toArray()).filter(i->i instanceof InvokeDynamicInsnNode).map(i->(InvokeDynamicInsnNode)i).toList();int dynamic=0;
                for(var i:changed.instructions.toArray()) {
                    if(i instanceof InvokeDynamicInsnNode d) {
                        if(changed.name.equals("<init>") && node.name.equals("iskallia/vault/client/gui/screen/bestiary/BestiaryScreen")
                                && d.bsmArgs[1] instanceof Handle h && h.getName().equals("getId")) continue;
                        var old=oldDynamics.get(dynamic++);
                        if(!Arrays.equals(d.bsmArgs,old.bsmArgs)) { assertEquals("bestiary_groups",module.spec().moduleId());assertEquals("getEntityGroupNames",changed.name);d.bsmArgs=old.bsmArgs;total++; }
                    }
                    if(i instanceof MethodInsnNode call && call.owner.equals(module.spec().helperClass())) {
                        if(call.name.equals("lookupGroup")) {
                            var mapper=assertInstanceOf(InvokeDynamicInsnNode.class,i.getPrevious());
                            var type=assertInstanceOf(LdcInsnNode.class,mapper.getPrevious());
                            var predicate=assertInstanceOf(VarInsnNode.class,type.getPrevious());assertEquals(1,predicate.var);
                            changed.instructions.remove(mapper);changed.instructions.remove(type);changed.instructions.remove(predicate);
                        }
                        if(call.name.equals("lookupName") && changed.name.equals("lambda$new$0")) {
                            var id=assertInstanceOf(MethodInsnNode.class,i.getPrevious());
                            assertEquals("getId",id.name);
                            changed.instructions.set(id,new MethodInsnNode(Opcodes.INVOKESTATIC,"iskallia/vault/util/GroupUtils","getEntityName","(Liskallia/vault/core/world/data/entity/EntityPredicate;)Lnet/minecraft/network/chat/Component;",false));
                            changed.instructions.set(i,new MethodInsnNode(Opcodes.INVOKEINTERFACE,"net/minecraft/network/chat/Component","getString","()Ljava/lang/String;",true));
                            total+=2;continue;
                        }
                        if(call.name.equals("translatePreview")) {
                            var field=assertInstanceOf(FieldInsnNode.class,i.getPrevious());assertEquals("currentStructureId",field.name);
                            var self=assertInstanceOf(VarInsnNode.class,field.getPrevious());assertEquals(0,self.var);
                            changed.instructions.remove(field);changed.instructions.remove(self);
                        }
                        changed.instructions.remove(i);total++;
                    }
                }
                assertEquals(MethodFingerprint.of(before),MethodFingerprint.of(changed),owner+changed.name+changed.desc);
            }
        }
        assertEquals(module.specs().stream().mapToInt(PatchSpec::hookCount).sum(),total);
    }
    @ParameterizedTest @MethodSource("modules")
    void serverScopeDuplicateInjectionAndChangedInputsAreEnforced(PatchModule module) throws Exception {
        for(PatchSpec spec:module.specs()) {
            var patch=single(module,spec);var node=original(spec);patch.apply(node,false);
            if(spec.side()==PatchSpec.Side.CLIENT) assertEquals(spec.fingerprint(),MethodFingerprint.of(VerifiedMethodPatch.target(node,spec)));
            else assertThrows(IllegalStateException.class,()->patch.apply(node,false));
            var changed=original(spec);var method=VerifiedMethodPatch.target(changed,spec);method.instructions.insert(new InsnNode(Opcodes.NOP));
            String hash=MethodFingerprint.of(method);assertThrows(IllegalStateException.class,()->patch.apply(changed));
            assertEquals(hash,MethodFingerprint.of(VerifiedMethodPatch.target(changed,spec)));
            PatchSpec bad=new PatchSpec(spec.patchVersion(),spec.targetVersion(),spec.jarHash(),spec.className(),spec.methodName(),spec.descriptor(),spec.fingerprint(),spec.hookCount()+1,spec.moduleId(),spec.helperClass(),spec.ownedLiterals(),spec.side());
            var wrong=single(module,bad);var intact=original(spec);assertThrows(IllegalStateException.class,()->wrong.apply(intact));
            assertEquals(spec.fingerprint(),MethodFingerprint.of(VerifiedMethodPatch.target(intact,spec)));
        }
    }
    @ParameterizedTest @MethodSource("modules")
    void shippedVpHasNoOverlapButLegacyConflictsAreDetected(PatchModule module) throws Exception {
        assertDoesNotThrow(()->VpCompatibility.assertCompatible(Path.of(System.getProperty("vh3.test.programVpDirectory")),module));
        if(module.spec().moduleId().equals("theme_names")) {
            // 主题原译文来自 JSON 数据而非 VP；验证新 getter 覆盖仍会触发冲突保护。
            var rule=com.google.gson.JsonParser.parseString("{\"target_class\":{\"name\":\"iskallia.vault.core.data.key.ThemeKey\",\"method\":\"getName\"},\"key\":\"Example\",\"value\":\"示例\"}").getAsJsonObject();
            assertTrue(module.ownsVpRule(rule));return;
        }
        var legacy=JsonFiles.read(Path.of(System.getProperty("vh3.test.legacyVpDirectory"),module.spec().moduleId()+".json")).getAsJsonArray();
        assertTrue(legacy.size()>0);assertTrue(java.util.stream.StreamSupport.stream(legacy.spliterator(),false).map(e->e.getAsJsonObject()).anyMatch(module::ownsVpRule));
    }
    // 实际执行变更后的 invokedynamic 引导，而不仅检查 ASM 栈形状。
    @Test void bestiaryLookupLambdasLinkAndExecuteWithRawIds() throws Exception {
        var module=PatchModules.find("bestiary_groups");var spec=module.specs().stream().filter(s->s.methodName().equals("getEntityGroupNames")).findFirst().orElseThrow();
        ClassNode node=original(spec);single(module,spec).apply(node);MethodNode method=VerifiedMethodPatch.target(node,spec);
        ClassWriter host=new ClassWriter(ClassWriter.COMPUTE_MAXS);host.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,node.name,null,"java/lang/Object",null);
        host.visitField(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"ENTITY_GROUPS","Ljava/util/HashMap;",null,null).visitEnd();method.accept(host);host.visitEnd();
        String resource="net/minecraft/resources/ResourceLocation",group="iskallia/vault/core/world/data/entity/PartialEntityGroup";
        Map<String,byte[]> classes=new HashMap<>();classes.put(node.name,host.toByteArray());classes.put(resource,valueClass(resource,"Ljava/lang/String;","getPath"));classes.put(group,valueClass(group,"L"+resource+";","getId"));
        ClassWriter helper=new ClassWriter(ClassWriter.COMPUTE_MAXS);helper.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,spec.helperClass(),null,"java/lang/Object",null);
        var h=helper.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"lookupName","(L"+resource+";)Ljava/lang/String;",null,null);h.visitCode();h.visitVarInsn(Opcodes.ALOAD,0);h.visitMethodInsn(Opcodes.INVOKEVIRTUAL,resource,"getPath","()Ljava/lang/String;",false);h.visitInsn(Opcodes.ARETURN);h.visitMaxs(0,0);h.visitEnd();helper.visitEnd();classes.put(spec.helperClass(),helper.toByteArray());
        ClassLoader loader=new ClassLoader(getClass().getClassLoader()) { @Override protected Class<?> findClass(String name) throws ClassNotFoundException {byte[] data=classes.get(name.replace('.','/'));if(data==null)throw new ClassNotFoundException(name);return defineClass(name,data,0,data.length);} };
        Class<?> r=loader.loadClass(resource.replace('/','.')),g=loader.loadClass(group.replace('/','.')),c=loader.loadClass(node.name.replace('/','.'));
        HashMap<Object,Object> entries=new HashMap<>();for(String id:List.of("horde","dungeon_boss"))entries.put(g.getConstructor(r).newInstance(r.getConstructor(String.class).newInstance(id)),new Object());
        c.getField("ENTITY_GROUPS").set(null,entries);assertEquals(List.of("dungeon_boss","horde"),c.getMethod("getEntityGroupNames").invoke(null));
    }
    private byte[] valueClass(String name,String type,String getter) {
        ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);w.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,name,null,"java/lang/Object",null);w.visitField(Opcodes.ACC_PRIVATE,"value",type,null,null).visitEnd();
        var m=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","("+type+")V",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);m.visitVarInsn(Opcodes.ALOAD,0);m.visitVarInsn(Opcodes.ALOAD,1);m.visitFieldInsn(Opcodes.PUTFIELD,name,"value",type);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(Opcodes.ACC_PUBLIC,getter,"()"+type,null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,name,"value",type);m.visitInsn(Opcodes.ARETURN);m.visitMaxs(0,0);m.visitEnd();w.visitEnd();return w.toByteArray();
    }
}
