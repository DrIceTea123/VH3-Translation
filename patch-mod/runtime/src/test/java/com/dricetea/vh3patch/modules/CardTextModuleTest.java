package com.dricetea.vh3patch.modules;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CardTextModuleTest {
    @TempDir Path fixture;
    private void sampleConfiguration() throws IOException {
        // 最小测试夹具独立于发布词表；用户增删改正式配置无需同步这些断言。
        Map<String,String> values = new LinkedHashMap<>();
        values.put("Red", "红色"); values.put("Blue", "蓝色"); values.put("Green", "绿色");
        values.put("Stat", "数值"); values.put("Resource", "资源"); values.put("Above", "上方"); values.put("Below", "下方");
        values.put(", ", "、"); values.put(" or ", "或"); values.put("Tier {0}", "等级{0}");
        values.put(" > If there are between {0} and {1} {2} Cards", " > 需要有{0}到{1}张符合以下条件的卡牌：{2}");
        values.put(" > For Each {0} Card", " > 每张符合以下条件的卡牌提供1层效果：{0}");
        values.put("x{0} to {1} Cards {2}", "{2}的{1}卡牌效果变为{0}倍");
        values.put("{0} efficiency for {1} {2} cards", "最多{1}张{2}卡牌效果{0}");
        values.put("+{0} Crate Tiers Completing a vault with at least {1} Resource Cards", "携带至少{1}张资源卡完成宝库时，板条箱等级+{0}");
        values.put("{0} (Currently {1})", "{0}（当前{1}）");
        values.put("{0} (Slots: {1} - {2} | Value: {3}%-{4}%)", "{0}（槽位：{1}～{2} | 值：{3}%～{4}%）");
        values.put("{0} (Crate Tier: {1} - {2} | Resource Cards Required: {3} - {4})", "{0}（板条箱等级：{1}～{2} | 资源卡需求量：{3}～{4}）");
        Files.writeString(fixture.resolve("card_text.json"), new com.google.gson.Gson().toJson(values));
        CardTextModule.INSTANCE.reload(fixture);
    }
    @Test void shippedConfigurationPassesSemanticValidationWithoutFixedEntryLists() throws Exception {
        CardTextModule.INSTANCE.reload(Path.of(System.getProperty("vh3.test.configDirectory")));
    }
    @ParameterizedTest @CsvSource(delimiter='|', textBlock="""
        ' > If there are between 2 and 4 Red or Blue, Stat, Tier 1-3 Cards'|' > 需要有2到4张符合以下条件的卡牌：红色或蓝色、数值、等级1-3'
        ' > For Each Green, Stat or Resource Card'|' > 每张符合以下条件的卡牌提供1层效果：绿色、数值或资源'
        'x1.5 to Red/Blue Cards Above/Below'|'上方/下方的红色/蓝色卡牌效果变为1.5倍'
        '+25% efficiency for 3 Resource cards'|'最多3张资源卡牌效果+25%'
        '+2 Crate Tiers Completing a vault with at least 3 Resource Cards'|'携带至少3张资源卡完成宝库时，板条箱等级+2'
        """)
    void configurableSentencesPreserveCountsFiltersAndWordOrder(String source, String expected) throws Exception {
        sampleConfiguration(); assertEquals(expected, CardTextModule.translate(source));
    }

    @Test void shiftRangesAndNestedCurrentValues() throws Exception {
        sampleConfiguration();
        assertEquals("最多2张数值卡牌效果+30%（槽位：1～3 | 值：10%～40%）", CardTextModule.translate("+30% efficiency for 2 Stat cards (Slots: 1 - 3 | Value: 10%-40%)"));
        assertEquals("携带至少3张资源卡完成宝库时，板条箱等级+2（板条箱等级：1～3 | 资源卡需求量：2～4）", CardTextModule.translate("+2 Crate Tiers Completing a vault with at least 3 Resource Cards (Crate Tier: 1 - 3 | Resource Cards Required: 2 - 4)"));
        assertEquals("伤害+5（当前伤害+8）", CardTextModule.translate("伤害+5 (Currently 伤害+8)"));
    }

    @Test void movingArgumentsPreservesColorHoverClickAndOriginalTree() throws Exception {
        sampleConfiguration();
        Style number = Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("details")))
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/example"));
        MutableComponent input = new TextComponent("x").setStyle(number).append(new TextComponent("1.5").setStyle(number))
                .append(new TextComponent(" to ").withStyle(ChatFormatting.GRAY))
                .append(new TextComponent("Red").withStyle(ChatFormatting.RED))
                .append(new TextComponent(" Cards ").withStyle(ChatFormatting.GRAY))
                .append(new TextComponent("Above").withStyle(ChatFormatting.WHITE));
        String before = Component.Serializer.toJson(input);
        Component output = CardTextModule.translateComponent(input);
        assertEquals("上方的红色卡牌效果变为1.5倍", output.getString());
        Map<String,Style> styles = new HashMap<>();
        output.visit((Style s, String text) -> { if (!text.isEmpty()) styles.put(text,s); return Optional.empty(); },Style.EMPTY);
        assertEquals(number, styles.get("1.5"));
        assertEquals(ChatFormatting.RED.getColor(), styles.get("红色").getColor().getValue());
        assertEquals(ChatFormatting.GRAY.getColor(), styles.get("卡牌效果变为").getColor().getValue());
        assertEquals(before, Component.Serializer.toJson(input));
    }

    @Test void exactNamesUnknownTextAndEmptyConfigRemainSafe(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("card_text.json"), "{\"New Card\":\"新卡\",\"Stat\":\"数值\"}");
        CardTextModule.INSTANCE.reload(dir);
        Component name = new TextComponent("New Card").withStyle(ChatFormatting.AQUA);
        assertEquals("新卡", CardTextModule.translateName(name).getString());
        assertEquals(name.getStyle(), CardTextModule.translateName(name).getStyle());
        Component unknown = new TranslatableComponent("item.example.unknown");
        assertSame(unknown, CardTextModule.translateName(unknown));
        assertEquals("New Stat Card", CardTextModule.translate("New Stat Card"));
        assertNull(CardTextModule.translateName(null));
        Object data = new Object(); assertSame(data, CardTextModule.translateTooltip(data));
        Files.writeString(dir.resolve("card_text.json"), "{}"); CardTextModule.INSTANCE.reload(dir);
        assertEquals("New Card", CardTextModule.translateName(name).getString());
    }

    @Test void invalidSentenceReloadKeepsWholeSnapshotAndValidEditsApply(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("card_text.json");
        Files.writeString(file, "{\"Value {0} to {1}\":\"{1}收到{0}\"}"); CardTextModule.INSTANCE.reload(dir);
        assertEquals("B收到A", CardTextModule.translate("Value A to B"));
        for (String broken : List.of("{\"Value {0} to {1}\":\"丢失{0}\"}", "{\"Value {0}{1}\":\"{0}{1}\"}", "{\"Value {2}\":\"{2}\"}", "{\"Value {0}\":\"{1}\"}")) {
            Files.writeString(file, broken); assertThrows(IOException.class, () -> CardTextModule.INSTANCE.reload(dir));
            assertEquals("B收到A", CardTextModule.translate("Value A to B"));
        }
        Files.writeString(file, "{\"Value {0} to {1}\":\"{0}传给{1}\"}"); CardTextModule.INSTANCE.reload(dir);
        assertEquals("A传给B", CardTextModule.translate("Value A to B"));
    }
}
