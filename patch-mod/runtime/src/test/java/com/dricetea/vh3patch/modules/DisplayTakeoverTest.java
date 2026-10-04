package com.dricetea.vh3patch.modules;

import com.dricetea.vh3patch.module.TemplateModule;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.CsvSource;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class DisplayTakeoverTest {
    @TempDir Path fixture;
    private void sampleAffixes() throws Exception {
        // 语序算法使用独立夹具；正式词表允许用户修改，另测其语义合法性。
        Files.writeString(fixture.resolve("gear_affixes.json"), """
            {
              "Lowers the Cooldown of {0} by {1}": "{0}冷却时间降低{1}",
              "{0} to level of {1}": "{0} {1}技能等级",
              "{0} to level of all Abilities": "{0} 所有技能等级",
              "{0} to level of all {1} abilities": "{0} 所有{1}技能等级",
              "{0} chance to generate {1} Mana per {2} looted": "搜刮{2}时有{0}概率恢复{1}点魔力",
              "{0}, {1} Mana per {2} looted": "搜刮{2}时有{0}概率恢复{1}点魔力",
              "{0} Increased {1}": "{0} {1}提升",
              "{0} Reduced {1}": "{0} {1}降低",
              "Looting {0} has a {1} chance to cast a level {2} {3}": "搜刮{0}时有{1}概率施放{2}级{3}",
              "Every hit you take has a {0} chance to cast a level {1} {2}": "每次受到攻击时有{0}概率施放{1}级{2}",
              "{0} Blocks  of Area Of Effect of {1}": "{1}效果范围{0}格",
              "Fireball": "火球", "Ornate Chest": "华丽宝箱", "Attack Damage": "攻击伤害"
            }
            """);
        GearAffixesModule.INSTANCE.reload(fixture);
    }
    static Stream<TemplateModule> modules() {return Stream.of(CrystalStatsModule.INSTANCE,ThemeNamesModule.INSTANCE,RoomNamesModule.INSTANCE,OverworldNamesModule.INSTANCE,BestiaryGroupsModule.INSTANCE,GearRarityModule.INSTANCE,QuestNamesModule.INSTANCE,GearAffixesModule.INSTANCE,TalentAffixesModule.INSTANCE);}
    private Path shipped() {return Path.of(System.getProperty("vh3.test.configDirectory"));}
    @ParameterizedTest @MethodSource("modules") void shippedConfigurationsAreValidAndUnknownDisplaysStayIntact(TemplateModule module) throws Exception {
        module.reload(shipped());assertEquals("Unknown display 918273",module.translateText("Unknown display 918273"));assertNull(module.translateText(null));
    }
    @ParameterizedTest @CsvSource(delimiter='|',textBlock="""
        Lowers the Cooldown of Fireball by 10s|火球冷却时间降低10s
        +1 to level of Fireball|+1 火球技能等级
        10% chance to generate 8 Mana per Ornate Chest looted|搜刮华丽宝箱时有10%概率恢复8点魔力
        +10% Increased Attack Damage|+10% 攻击伤害提升
        -5% Reduced Attack Damage|-5% 攻击伤害降低
        '10%-20%, 8-12 Mana per 华丽宝箱 looted'|搜刮华丽宝箱时有10%-20%概率恢复8-12点魔力
        Looting 华丽宝箱 has a 10% chance to cast a level 2 火球|搜刮华丽宝箱时有10%概率施放2级火球
        Every hit you take has a 5% chance to cast a level 3 火球|每次受到攻击时有5%概率施放3级火球
        +2 Blocks  of Area Of Effect of 火球|火球效果范围+2格
        +1 to level of all Abilities|+1 所有技能等级
        +1 to level of all 火球 abilities|+1 所有火球技能等级
        """)
    void releaseSentenceExamples(String input,String output) throws Exception {
        sampleAffixes();assertEquals(output,GearAffixesModule.translate(input));
    }
    @Test void sameEnglishLevelSentenceKeepsTalentAndAbilitySemanticsSeparate() throws Exception {
        sampleAffixes();
        Files.writeString(fixture.resolve("talent_affixes.json"),"{\"{0} to level of {1}\":\"{0} {1}天赋等级\"}");
        TalentAffixesModule.INSTANCE.reload(fixture);
        assertEquals("+1 专注天赋等级",TalentAffixesModule.translate("+1 to level of 专注"));
        assertEquals("+1 专注技能等级",GearAffixesModule.translate("+1 to level of 专注"));
        assertNull(GearAffixesModule.translateDisplay(null));
    }
    @Test void movedNumbersKeepColorsAndInputTreeIsNotMutated() throws Exception {
        sampleAffixes();Style chance=Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true);
        Style mana=Style.EMPTY.withColor(ChatFormatting.AQUA);Style name=Style.EMPTY.withColor(ChatFormatting.GREEN);
        MutableComponent input=new TextComponent("10%").setStyle(chance).append(new TextComponent(" chance to generate ").withStyle(ChatFormatting.GRAY))
            .append(new TextComponent("8").setStyle(mana)).append(new TextComponent(" Mana per ").withStyle(ChatFormatting.GRAY))
            .append(new TextComponent("Ornate Chest").setStyle(name)).append(new TextComponent(" looted").withStyle(ChatFormatting.GRAY));
        String before=Component.Serializer.toJson(input);Component output=GearAffixesModule.translateDisplay(input);
        Map<String,Style> styles=new HashMap<>();output.visit((Style s,String t)->{if(!t.isEmpty())styles.put(t,s);return Optional.empty();},Style.EMPTY);
        // 子组件继承根节点的 bold；比较最终生效样式而不是未继承的局部声明。
        assertEquals("搜刮华丽宝箱时有10%概率恢复8点魔力",output.getString());assertEquals(chance,styles.get("10%"));assertEquals(mana.applyTo(chance),styles.get("8"));assertEquals(name.applyTo(chance),styles.get("华丽宝箱"));assertEquals(before,Component.Serializer.toJson(input));
    }
      @Test void bestiaryLookupIgnoresTranslatedNamesAndReloadedDisplay() throws Exception {
        BestiaryGroupsModule.INSTANCE.reload(shipped());assertEquals("Horde",BestiaryGroupsModule.lookupName(new ResourceLocation("the_vault","horde")));
        assertEquals("Dungeon Boss",BestiaryGroupsModule.lookupName(new ResourceLocation("the_vault","dungeon_boss")));
        assertEquals("Dweller",BestiaryGroupsModule.lookupName(new ResourceLocation("the_vault","fighter")));
          assertNotEquals("Horde",BestiaryGroupsModule.translate("Horde"));
          var group=new ResourceLocation("the_vault","horde");
          assertEquals("Horde",BestiaryGroupsModule.lookupGroup("集群怪物",group,ResourceLocation.class,o->(ResourceLocation)o));
          assertEquals("unchanged",BestiaryGroupsModule.lookupGroup("unchanged",new Object(),ResourceLocation.class,o->{throw new AssertionError("非族类不可取 ID");}));
          assertEquals("unchanged",BestiaryGroupsModule.lookupGroup("unchanged",null,ResourceLocation.class,o->{throw new AssertionError("空值不可取 ID");}));
    }
    @Test void previewDoesNotTranslateNonOverworldTemplates(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("overworld_names.json"),"{\"Same\":\"相同\"}");OverworldNamesModule.INSTANCE.reload(dir);
        assertEquals("相同",OverworldNamesModule.translatePreview("Same",new ResourceLocation("the_vault","overworld/example")));
        assertEquals("Same",OverworldNamesModule.translatePreview("Same",new ResourceLocation("the_vault","vault/rooms/example")));
        assertEquals("Same",OverworldNamesModule.translatePreview("Same",new ResourceLocation("another","overworld/example")));
    }
    @Test void statsNeverCoercesNumericValuesAndRarityPreservesStyle(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("crystal_stats.json"),"{\"42\":\"不应替换数值\"}");CrystalStatsModule.INSTANCE.reload(dir);Integer number=42;assertSame(number,CrystalStatsModule.translateValue(number));
        Files.writeString(dir.resolve("gear_rarity.json"),"{\"Rare\":\"稀有\"}");GearRarityModule.INSTANCE.reload(dir);
        Component input=new TextComponent("Rare").withStyle(ChatFormatting.BLUE);Component translated=GearRarityModule.translateComponent(input);
        assertEquals("稀有",translated.getString());assertEquals(input.getStyle(),translated.getStyle());assertEquals("Rare",input.getString());
    }
}
