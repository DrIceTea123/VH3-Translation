package com.dricetea.vh3patch.transformer;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

public class MissingDisplaysPatchTest {
    public static Function<Object,String> translatedNames(Function<Object,String> original) { return value -> "translated:" + original.apply(value); }
    public static String translatedText(String original) { return "translated:" + original; }

    @Test void bothCrucibleMethodReferenceHooksLinkAndExecute() throws Exception {
        PatchModule module = PatchModules.find("theme_names");
        for (PatchSpec spec : module.specs().stream().filter(s -> s.className().endsWith("VoidCrucibleScreen$ThemeSelect")).toList()) {
            ClassNode node = TargetJar.read(Path.of(System.getProperty("vh3.test.targetJar")), spec, true);
            module.apply(node);
            MethodNode method = VerifiedMethodPatch.target(node, spec);
            var hook = Arrays.stream(method.instructions.toArray()).filter(i -> i instanceof MethodInsnNode c && c.name.equals("translateNames")).findFirst().orElseThrow();
            var dynamic = assertInstanceOf(InvokeDynamicInsnNode.class, hook.getPrevious());
            assertEquals("getName", ((Handle) dynamic.bsmArgs[1]).getName());
            var host = writer(node.name, "java/lang/Object");
            var factory = host.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"names","()Ljava/util/function/Function;",null,null);
            factory.visitCode(); dynamic.accept(factory); hook.accept(factory); factory.visitInsn(Opcodes.ARETURN);factory.visitMaxs(0,0);factory.visitEnd();host.visitEnd();
            Map<String,byte[]> classes = new HashMap<>();classes.put(node.name,host.toByteArray());
            String named = "iskallia/vault/core/data/key/NamedKey", theme = "iskallia/vault/core/data/key/ThemeKey";
            classes.put(named, namedKey(named));
            var child = writer(theme,named);constructor(child,named);child.visitEnd();classes.put(theme,child.toByteArray());
            classes.put(spec.helperClass(), helper(spec.helperClass(),"translateNames","(Ljava/util/function/Function;)Ljava/util/function/Function;","translatedNames"));
            ClassLoader loader = loader(classes);
            @SuppressWarnings("unchecked") Function<Object,String> mapper = (Function<Object,String>) loader.loadClass(node.name.replace('/','.')).getMethod("names").invoke(null);
            Object value = loader.loadClass(theme.replace('/','.')).getConstructor(String.class).newInstance("Easter");
            assertEquals("translated:Easter",mapper.apply(value));
            assertEquals("Easter",value.getClass().getMethod("getName").invoke(value));
        }
    }

    @Test void revivalLambdaExecutesOriginalCountAndMarkupBeforeTranslation() throws Exception {
        PatchModule module = PatchModules.find("gear_affixes");
        PatchSpec spec = module.specs().stream().filter(s -> s.methodName().equals("lambda$static$6")).findFirst().orElseThrow();
        ClassNode node = TargetJar.read(Path.of(System.getProperty("vh3.test.targetJar")),spec,true);module.apply(node);
        MethodNode method = VerifiedMethodPatch.target(node,spec);method.access=Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC;
        var host = writer(node.name,"java/lang/Object");method.accept(host);host.visitEnd();
        ClassLoader loader = loader(Map.of(node.name,host.toByteArray(),spec.helperClass(),helper(spec.helperClass(),"translate","(Ljava/lang/String;)Ljava/lang/String;","translatedText")));
        var formatter = loader.loadClass(node.name.replace('/','.')).getMethod(method.name,Integer.class);
        assertEquals("translated:+Revives you instantly, and heals you fully if you die inside a vault. This effect can occur <$uniqueHighlight>12<reset> times per vault.",formatter.invoke(null,12));
    }

    @Test void registryOtherRulesRemainAllowedButRevivalAndTrailOverlapAreRejected() {
        PatchModule gear = PatchModules.find("gear_affixes");
        assertFalse(gear.ownsVpRule(rule("iskallia.vault.init.ModGearAttributes",null,"Warp Projectile Speed")));
        assertTrue(gear.ownsVpRule(rule("iskallia.vault.init.ModGearAttributes",null,"<reset> times per vault.")));
        assertTrue(gear.ownsVpRule(rule("iskallia.vault.init.ModGearAttributes","lambda$static$6","anything")));
        assertFalse(gear.ownsVpRule(rule("iskallia.vault.gear.attribute.custom.effect.EffectTrialAttribute$Reader","serializeTextElements","Leaves a trail of ")));
        assertTrue(gear.ownsVpRule(rule("iskallia.vault.gear.attribute.custom.effect.EffectTrialAttribute$Reader","getDisplay","Leaves a trail of ")));
    }
    private com.google.gson.JsonObject rule(String owner,String method,String key) {
        var r = new com.google.gson.JsonObject();var t = new com.google.gson.JsonObject();t.addProperty("name",owner);if(method!=null)t.addProperty("method",method);r.add("target_class",t);r.addProperty("key",key);r.addProperty("value","test");return r;
    }
    private ClassWriter writer(String name,String parent) {var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);w.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,name,null,parent,null);return w;}
    private void constructor(ClassWriter w,String parent) {
        var m=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","(Ljava/lang/String;)V",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitVarInsn(Opcodes.ALOAD,1);m.visitMethodInsn(Opcodes.INVOKESPECIAL,parent,"<init>","(Ljava/lang/String;)V",false);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();
    }
    private byte[] namedKey(String name) {
        var w=writer(name,"java/lang/Object");w.visitField(Opcodes.ACC_PRIVATE,"name","Ljava/lang/String;",null,null).visitEnd();
        var m=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","(Ljava/lang/String;)V",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);m.visitVarInsn(Opcodes.ALOAD,0);m.visitVarInsn(Opcodes.ALOAD,1);m.visitFieldInsn(Opcodes.PUTFIELD,name,"name","Ljava/lang/String;");m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(Opcodes.ACC_PUBLIC,"getName","()Ljava/lang/String;",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,name,"name","Ljava/lang/String;");m.visitInsn(Opcodes.ARETURN);m.visitMaxs(0,0);m.visitEnd();w.visitEnd();return w.toByteArray();
    }
    private byte[] helper(String name,String method,String descriptor,String target) {
        var w=writer(name,"java/lang/Object");var m=w.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,method,descriptor,null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitMethodInsn(Opcodes.INVOKESTATIC,getClass().getName().replace('.','/'),target,descriptor,false);m.visitInsn(Opcodes.ARETURN);m.visitMaxs(0,0);m.visitEnd();w.visitEnd();return w.toByteArray();
    }
    private ClassLoader loader(Map<String,byte[]> classes) {
        return new ClassLoader(getClass().getClassLoader()) { @Override protected Class<?> findClass(String name) throws ClassNotFoundException {byte[] data=classes.get(name.replace('.','/'));if(data==null)throw new ClassNotFoundException(name);return defineClass(name,data,0,data.length);} };
    }
}
