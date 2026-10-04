package com.dricetea.vh3patch.transformer;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import me.fengming.vaultpatcher_asm.VaultPatcher;
import me.fengming.vaultpatcher_asm.config.*;
import me.fengming.vaultpatcher_asm.core.utils.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.StringReader;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 调用官方 1.5.3-hotfix 发布 JAR；不用项目自写解析器替代兼容性验证。 */
class Vp153FormatTest {
    @TempDir Path game;

    @Test void allPublishedRulesParseWithExactTargetsSelectorsAndPairs() throws Exception {
        Path config=Path.of(System.getProperty("vh3.test.programVpDirectory"));
        var oldPath=VaultPatcher.mcPath;
        try {
            VaultPatcher.mcPath=game;
            int groups=0,pairCount=0;
            for(Path file:VpCompatibility.enabledModuleFiles(config)) {
                JsonArray source=JsonFiles.read(file).getAsJsonArray();
                boolean dynamic=source.get(0).getAsJsonObject().get("dynamic").getAsBoolean();
                assertFalse(source.get(0).getAsJsonObject().get("i18n").getAsBoolean());
                for(int i=1;i<source.size();i++) {
                    JsonObject rule=source.get(i).getAsJsonObject();
                    if(!rule.has("target_class")) continue;
                    Utils.translationInfoMap.clear();Utils.dynTranslationInfos.clear();
                    JsonArray input=new JsonArray();input.add(source.get(0));input.add(rule);
                    new VaultPatcherModule(file.getFileName().toString()).read(new JsonReader(new StringReader(input.toString())));
                    List<TranslationInfo> actual=dynamic ? Utils.dynTranslationInfos : Utils.translationInfoMap.values().stream().flatMap(Set::stream).toList();
                    assertEquals(1,actual.size(),file+" #"+i);
                    TranslationInfo info=actual.get(0);TargetClassInfo target=info.getTargetClassInfo();
                    String name=rule.getAsJsonArray("target_class").size()==0?"":rule.getAsJsonArray("target_class").get(0).getAsString();
                    assertEquals(name,dynamic?target.getDynamicName():info.getTargetClass());
                    JsonObject selectors=rule.has("info")?rule.getAsJsonObject("info"):new JsonObject();
                    assertEquals(selectors.has("method")?selectors.get("method").getAsString():"",target.getMethod());
                    if(selectors.has("local")) {
                        String local=selectors.get("local").getAsString();
                        assertEquals(local.substring(1),target.getLocal());
                        assertTrue(MatchUtils.matchLocal(info,local.substring(1),local.startsWith("M")||local.startsWith("R")));
                    }
                    if(selectors.has("ordinal")) {
                        TargetClassInfo expected=new TargetClassInfo();expected.readJson(new JsonReader(new StringReader(selectors.toString())));
                        for(int ordinal=0;ordinal<30;ordinal++) {
                            boolean matches=false;
                            for(var range:expected.getOrdinal()) matches|=range.first<=ordinal&&(range.second==-1||ordinal<=range.second);
                            assertEquals(matches,MatchUtils.matchOrdinal(info,ordinal));
                        }
                    }
                    Set<String> keys=new HashSet<>();
                    for(JsonElement e:rule.getAsJsonArray("pairs")) {
                        var pair=e.getAsJsonObject();String key=pair.get("key").getAsString(),value=pair.get("value").getAsString();
                        keys.add(key);assertEquals(value,info.getPairs().getValue(key),file+" #"+i+" "+key);
                        if(dynamic&&value.startsWith("@")) assertEquals("prefix "+value.substring(1)+" suffix",MatchUtils.matchPairs(info.getPairs(),"prefix "+key+" suffix",true));
                        pairCount++;
                    }
                    assertEquals(keys.size(),dynamic?info.getPairs().getSet().size():info.getPairs().getMap().size());
                    groups++;
                }
                for(PatchModule module:PatchModules.all()) assertDoesNotThrow(()->VpCompatibility.assertCompatible(config,module));
            }
            assertEquals(715,groups);assertEquals(4166,pairCount);
        } finally { VaultPatcher.mcPath=oldPath;Utils.translationInfoMap.clear();Utils.dynTranslationInfos.clear(); }
    }

    @Test void modernRulesAndAliasesKeepOwnership() throws Exception {
        for(PatchModule module:PatchModules.all()) {
            Path fixture=Path.of(System.getProperty("vh3.test.legacyVpDirectory"),module.spec().moduleId()+".json");
            JsonArray old=Files.exists(fixture)?JsonFiles.read(fixture).getAsJsonArray():new JsonArray();
            // theme_names 没有独立旧表；为每个真实目标也验证现代格式识别，覆盖所有模块。
            for(PatchSpec spec:module.specs()) {
                JsonObject target=new JsonObject();target.addProperty("name",spec.className());target.addProperty("method",spec.methodName());
                JsonObject rule=new JsonObject();rule.add("target_class",target);old.add(rule);
            }
            for(JsonElement element:old) {
                JsonObject rule=element.getAsJsonObject();
                List<JsonElement> targets=new ArrayList<>();
                if(rule.has("target_class"))targets.add(rule.get("target_class"));
                if(rule.has("target_classes"))rule.getAsJsonArray("target_classes").forEach(targets::add);
                for(JsonElement targetElement:targets) {
                    JsonObject target=targetElement.getAsJsonObject();JsonObject single=rule.deepCopy();
                    single.remove("target_classes");single.add("target_class",target);
                    JsonObject modern=single.deepCopy();JsonArray names=new JsonArray();
                    if(target.has("name"))names.add(target.get("name"));
                    JsonObject info=target.deepCopy();info.remove("name");modern.add("target_class",names);modern.add("info",info);
                    assertEquals(module.ownsVpRule(single),module.ownsVpRule(modern),module.spec().moduleId());
                    modern.add("t",modern.remove("target_class"));modern.add("i",modern.remove("info"));
                    assertEquals(module.ownsVpRule(single),module.ownsVpRule(modern));
                    names.add("unrelated.Example");
                    assertEquals(module.ownsVpRule(single),module.ownsVpRule(modern),"multi-class "+module.spec().moduleId());
                }
            }
        }
    }

    @Test void realVpTransformerRunsAfterVtpOnEverySharedCoreClass() throws Exception {
        var oldPath=VaultPatcher.mcPath;boolean oldCache=Utils.debug.isUseCache();
        try {
            VaultPatcher.mcPath=game;Utils.debug.setUseCache(false);
            Utils.translationInfoMap.clear();Utils.dynTranslationInfos.clear();
            for(Path file:VpCompatibility.enabledModuleFiles(Path.of(System.getProperty("vh3.test.programVpDirectory"))))
                try(var reader=Files.newBufferedReader(file)) { new VaultPatcherModule(file.getFileName().toString()).read(new JsonReader(reader)); }
            int shared=0;
            for(var entry:PatchModules.byClass().entrySet()) {
                var infos=Utils.translationInfoMap.get(entry.getKey().replace('/','.'));
                if(infos==null)continue;
                PatchSpec spec=entry.getValue().stream().flatMap(m->m.specs().stream()).filter(s->s.className().equals(entry.getKey())).findFirst().orElseThrow();
                var node=TargetJar.read(Path.of(System.getProperty("vh3.test.targetJar")),spec,false);
                for(PatchModule module:entry.getValue())module.apply(node);
                new me.fengming.vaultpatcher_asm.core.transformers.VPClassTransformer(infos).accept(node);
                // VP 新建 __vp_init/<clinit> 时用 visitMaxs(0,0)，实际写出阶段才计算 maxs。
                var writer=new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_MAXS);
                node.accept(writer);var written=new org.objectweb.asm.tree.ClassNode();
                new org.objectweb.asm.ClassReader(writer.toByteArray()).accept(written,0);
                for(var method:written.methods) if(method.instructions.size()>0) {
                    try {new org.objectweb.asm.tree.analysis.Analyzer<>(new org.objectweb.asm.tree.analysis.BasicVerifier()).analyze(written.name,method);}
                    catch(Exception e){throw new AssertionError(written.name+"."+method.name+method.desc,e);}
                }
                shared++;
            }
            assertTrue(shared>10,"Expected real overlapping VTP/VP targets");
            System.out.println("VP 1.5.3-hotfix follows VTP on "+shared+" shared core classes; all methods pass ASM analysis.");
        } finally {VaultPatcher.mcPath=oldPath;Utils.debug.setUseCache(oldCache);Utils.translationInfoMap.clear();Utils.dynTranslationInfos.clear();}
    }
}
