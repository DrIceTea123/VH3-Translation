package com.dricetea.vh3patch.transformer.modules;
import com.dricetea.vh3patch.transformer.*;
import com.google.gson.*;
import org.objectweb.asm.tree.*;
import java.util.*;

/** 只在已确定为装备/护符的显示边界翻译；不改 RollType/getColor 或枚举名称。 */
public final class GearRarityModule extends DisplayMethodPatch {
    public static final String ID="gear_rarity";
    public GearRarityModule() { super(ID,"com/dricetea/vh3patch/modules/GearRarityModule"); }
    public GearRarityModule(List<PatchSpec> specs) { super(specs); }
    @Override protected int patch(PatchSpec spec, MethodNode method) {
        int count=0;
        for(var i:method.instructions.toArray()) {
            boolean before=false, selected=false;
            if(spec.className().endsWith("VaultGearRarity") || spec.className().endsWith("TransmogTableScreen$Filter")) {
                selected=call(i,"net/minecraft/network/chat/TextComponent","<init>","(Ljava/lang/String;)V"); before=true;
            } else if(call(i,"iskallia/vault/config/gear/VaultGearTypeConfig$RollType","getName","()Ljava/lang/String;")) selected=true;
            else if(call(i,"iskallia/vault/config/gear/VaultGearCraftingConfig$ProficiencyStep","getPool","()Ljava/lang/String;"))
                selected=call(StringValuePatch.nextCode(i),"net/minecraft/network/chat/TextComponent","<init>","(Ljava/lang/String;)V");
            if(selected){stringHook(method,i,spec,before);count++;}
        }
        return count;
    }
    @Override public boolean ownsVpRule(JsonObject rule) {
        rule = VpCompatibility.normalizeRule(rule);
        for(JsonObject t:vpTargets(rule)) {
            if(!t.has("name"))continue;
            String name=t.get("name").getAsString().replace('.','/');
            if(name.endsWith("/VaultGearRarity"))return true;
            if(!targets.stream().anyMatch(s->s.className().equals(name)))continue;
            if(t.has("local") && Set.of("MgetName","MgetPool").contains(t.get("local").getAsString()))return true;
            // 允许同类固定 UI 提示；检测名称片段重复接管，不按整个类删除。
            var pairs=rule.has("pairs")?rule.getAsJsonArray("pairs"):new JsonArray(); if(rule.has("key"))pairs.add(rule);
            for(JsonElement p:pairs)if(p.isJsonObject() && p.getAsJsonObject().has("key") && Set.of("Scrappy","Common","Rare","Epic","Omega","Unique","Special","Chaotic","Royale","Greed").contains(p.getAsJsonObject().get("key").getAsString()))return true;
        }
        return false;
    }
}
