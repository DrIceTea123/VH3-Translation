package com.dricetea.vh3patch.transformer;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 执行真实详情页返回回调；显示名不应流入 selectGroup。 */
class BestiaryNavigationTest {
    @Test void detailBackUsesRawGroupIdEvenWhenDisplayCannotBeUsedForLookup() throws Exception {
        var module=PatchModules.find("bestiary_groups");
        var spec=module.specs().stream().filter(s->s.methodName().equals("lambda$new$0")).findFirst().orElseThrow();
        var node=TargetJar.read(Path.of(System.getProperty("vh3.test.targetJar")),spec,true);
        module.apply(node);
        var callback=VerifiedMethodPatch.target(node,spec);
        String resource="net/minecraft/resources/ResourceLocation",group="iskallia/vault/core/world/data/entity/PartialEntityGroup";
        String screen="iskallia/vault/client/gui/screen/bestiary/BestiaryScreen";
        Map<String,byte[]> classes=new HashMap<>();
        classes.put(resource,valueClass(resource,"Ljava/lang/String;","getPath"));
        classes.put(group,valueClass(group,"L"+resource+";","getId"));
        ClassWriter host=empty(node.name);
        host.visitField(Opcodes.ACC_PUBLIC,"parent","L"+screen+";",null,null).visitEnd();
        host.visitField(Opcodes.ACC_PUBLIC,"group","L"+group+";",null,null).visitEnd();
        callback.access=Opcodes.ACC_PUBLIC;callback.accept(host);host.visitEnd();classes.put(node.name,host.toByteArray());
        ClassWriter parent=empty(screen);
        parent.visitField(Opcodes.ACC_PUBLIC,"selected","Ljava/lang/String;",null,null).visitEnd();
        var m=parent.visitMethod(Opcodes.ACC_PUBLIC,"selectGroup","(Ljava/lang/String;)V",null,null);
        m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitVarInsn(Opcodes.ALOAD,1);m.visitFieldInsn(Opcodes.PUTFIELD,screen,"selected","Ljava/lang/String;");m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();parent.visitEnd();classes.put(screen,parent.toByteArray());
        // helper 的 ID 格式化另有运行侧测试；这里检查真实回调的调用链和链接。
        ClassWriter helper=empty(spec.helperClass());
        m=helper.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"lookupName","(L"+resource+";)Ljava/lang/String;",null,null);
        m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitMethodInsn(Opcodes.INVOKEVIRTUAL,resource,"getPath","()Ljava/lang/String;",false);m.visitInsn(Opcodes.ARETURN);m.visitMaxs(0,0);m.visitEnd();helper.visitEnd();classes.put(spec.helperClass(),helper.toByteArray());
        // 不提供 GroupUtils 或 Component：任何误用显示链都会在执行时失败。
        ClassLoader loader=new ClassLoader(getClass().getClassLoader()) {
            @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bytes=classes.get(name.replace('.','/'));if(bytes==null)throw new ClassNotFoundException(name);
                return defineClass(name,bytes,0,bytes.length);
            }
        };
        Class<?> r=loader.loadClass(resource.replace('/','.')),g=loader.loadClass(group.replace('/','.'));
        Class<?> c=loader.loadClass(node.name.replace('/','.')),s=loader.loadClass(screen.replace('/','.'));
        Object detail=c.getConstructor().newInstance(),view=s.getConstructor().newInstance();c.getField("parent").set(detail,view);
        for(String id:List.of("horde","fighter","dungeon_boss","custom_group")) {
            c.getField("group").set(detail,g.getConstructor(r).newInstance(r.getConstructor(String.class).newInstance(id)));
            for(int click=0;click<2;click++) { c.getMethod("lambda$new$0").invoke(detail);assertEquals(id,s.getField("selected").get(view)); }
        }
    }
    private static ClassWriter empty(String name) {
        ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);w.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,name,null,"java/lang/Object",null);
        var m=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();return w;
    }
    private static byte[] valueClass(String name,String type,String getter) {
        ClassWriter w=empty(name);w.visitField(Opcodes.ACC_PRIVATE,"value",type,null,null).visitEnd();
        var m=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","("+type+")V",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);m.visitVarInsn(Opcodes.ALOAD,0);m.visitVarInsn(Opcodes.ALOAD,1);m.visitFieldInsn(Opcodes.PUTFIELD,name,"value",type);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(Opcodes.ACC_PUBLIC,getter,"()"+type,null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,name,"value",type);m.visitInsn(Opcodes.ARETURN);m.visitMaxs(0,0);m.visitEnd();w.visitEnd();return w.toByteArray();
    }
}
