package com.dricetea.vh3patch.transformer;

import com.dricetea.vh3patch.transformer.modules.MobNamesModule;
import com.dricetea.vh3patch.transformer.modules.ChestNamesModule;
import com.google.gson.*;
import me.fengming.vaultpatcher_asm.config.*;
import me.fengming.vaultpatcher_asm.core.node.NodeHandlerParameters;
import me.fengming.vaultpatcher_asm.core.node.handlers.*;
import me.fengming.vaultpatcher_asm.core.utils.MatchUtils;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class XpVpRulesTest {
    @Test void directAndCallerRulesConflictForBothTargetFormsButOreAndLabelsStayAllowed() {
        var mob = new MobNamesModule(); var chest = new ChestNamesModule();
        for (String property : List.of("target_class", "target_classes")) for (String local : List.of("MformatMobName", "RformatMobName", "MformatChestName", "RformatChestName", "MformatOreName")) {
            JsonObject target = new JsonObject(); target.addProperty("name", ChestNamesModule.XP.replace('/', '.'));
            target.addProperty("method", "handleDeltas"); target.addProperty("local", local);
            JsonObject rule = new JsonObject(); JsonArray targets = new JsonArray(); targets.add(target);
            rule.add(property, property.equals("target_class") ? target : targets);
            assertEquals(local.endsWith("MobName"), mob.ownsVpRule(rule));
            assertEquals(local.endsWith("ChestName"), chest.ownsVpRule(rule));
            if (mob.ownsVpRule(rule)) assertThrows(IllegalStateException.class, () -> mob.validateVpMigration(List.of(rule)));
        }
    }
    @Test void realVpHandlerPlacesOneOreHookAfterCallAndReplacesFourLabelsWithoutChangingIds() throws Exception {
        var mob = new MobNamesModule(); var chest = new ChestNamesModule();
        ClassNode node = TargetJar.read(Path.of(System.getProperty("vh3.test.targetJar")), mob.specs().get(1), true);
        mob.apply(node); chest.apply(node);
        var rules = JsonFiles.read(Path.of(System.getProperty("vh3.test.programVpDirectory"), "the_vault-asm_main.json")).getAsJsonArray();
        MethodNode delta = node.methods.stream().filter(m -> m.name.equals("handleDeltas")).findFirst().orElseThrow();
        int hooks=0, labels=0, groups=0;
        for (JsonElement e : rules) {
            JsonObject rule=e.getAsJsonObject();
            if (!rule.has("target_class") || !rule.getAsJsonObject("target_class").get("name").getAsString().equals(ChestNamesModule.XP.replace('/','.'))) continue;
            groups++;
            JsonObject target=rule.getAsJsonObject("target_class");
            assertEquals("handleDeltas", target.get("method").getAsString());
            assertFalse(mob.ownsVpRule(rule)); assertFalse(chest.ownsVpRule(rule));
            TranslationInfo info=new TranslationInfo();
            info.getTargetClassInfo().setName(target.get("name").getAsString());
            info.getTargetClassInfo().setMethod(target.get("method").getAsString());
            for (JsonElement p : rule.getAsJsonArray("pairs")) {
                info.setKey(p.getAsJsonObject().get("key").getAsString()); info.setValue(p.getAsJsonObject().get("value").getAsString());
            }
            var params=new NodeHandlerParameters(false,false,node,delta,new HashMap<>(),info);
            if (target.has("local")) {
                assertEquals("MformatOreName",target.get("local").getAsString());
                info.getTargetClassInfo().setLocal(target.get("local").getAsString());
                for (AbstractInsnNode i : delta.instructions.toArray()) if (i instanceof MethodInsnNode call) {
                    new MethodNodeHandler(call,params) { @Override public void debugInfo(int ordinal,String action,String old,String value) {} }.modifyNode();
                    if (call.name.equals("formatOreName")) {
                        MethodInsnNode hook=assertInstanceOf(MethodInsnNode.class, call.getNext());
                        assertEquals("__vp_replace",hook.name); assertEquals("(Ljava/lang/Object;)Ljava/lang/String;",hook.desc); hooks++;
                    }
                }
                assertEquals(rule.getAsJsonArray("pairs").size(),info.getPairs().getMap().size());
                // 验证 VP 真实匹配行为，不把当前译文或历史英文占位冻结成测试要求。
                for (JsonElement p : rule.getAsJsonArray("pairs")) {
                    JsonObject pair = p.getAsJsonObject();
                    assertEquals(pair.get("value").getAsString(),
                            MatchUtils.matchPairs(info.getPairs(),pair.get("key").getAsString(),false));
                }
                assertEquals("Unknown Ore",MatchUtils.matchPairs(info.getPairs(),"Unknown Ore",false));
            } else {
                for (AbstractInsnNode i : delta.instructions.toArray()) if (i instanceof LdcInsnNode ldc && info.getPairs().getMap().containsKey(ldc.cst)) {
                    new LdcNodeHandler(ldc,params) { @Override public void debugInfo(int ordinal,String action,String old,String value) {} }.modifyNode(); labels++;
                }
            }
        }
        assertEquals(2,groups); assertEquals(1,hooks); assertEquals(4,labels);
        // VP 后执行；算术、实体标识和原方法体没有被新规则替换。
        new Analyzer<>(new BasicVerifier()).analyze(node.name,delta);
        assertFalse(Arrays.stream(delta.instructions.toArray()).filter(i->i instanceof LdcInsnNode).map(i->((LdcInsnNode)i).cst).anyMatch("Treasure Door"::equals));
    }
}
