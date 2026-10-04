package com.dricetea.vh3patch.modules;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class MissingDisplaysTest {
    @TempDir Path config;

    @Test void revivalFormatterPreservesCountAndInlineColorTags() throws Exception {
        Files.writeString(config.resolve("gear_affixes.json"), """
            {"+Revives you instantly, and heals you fully if you die inside a vault. This effect can occur <$uniqueHighlight>{0}<reset> times per vault.":
             "+在宝库中死亡时立即复活，并恢复全部生命值。每次宝库可以触发<$uniqueHighlight>{0}<reset>次。"}
            """);
        GearAffixesModule.INSTANCE.reload(config);
        for (int count : List.of(1, 3, 12)) {
            String original = "+Revives you instantly, and heals you fully if you die inside a vault. This effect can occur <$uniqueHighlight>" + count + "<reset> times per vault.";
            assertEquals("+在宝库中死亡时立即复活，并恢复全部生命值。每次宝库可以触发<$uniqueHighlight>" + count + "<reset>次。", GearAffixesModule.translate(original));
        }
    }

    @Test void trailPreservesPrefixEffectDurationAndStyles() throws Exception {
        Files.writeString(config.resolve("gear_affixes.json"), """
            {"Leaves a trail of {0} for {1}":"移动尾迹-{0}-{1}",
             "{0}Leaves a trail of {1} for {2}":"{0}移动尾迹-{1}-{2}"}
            """);
        GearAffixesModule.INSTANCE.reload(config);
        for (String prefix : List.of("", "+", "◆ ")) {
            MutableComponent input = new TextComponent(prefix).withStyle(ChatFormatting.GRAY)
                    .append("Leaves a trail of ")
                    .append(new TextComponent("寒冷").withStyle(ChatFormatting.AQUA))
                    .append(" for ")
                    .append(new TextComponent("3.5s").withStyle(ChatFormatting.GOLD));
            String before = Component.Serializer.toJson(input);
            Component output = GearAffixesModule.translateDisplay(input);
            assertEquals(prefix + "移动尾迹-寒冷-3.5s", output.getString());
            Map<String,Style> styles = new HashMap<>();
            output.visit((Style style, String text) -> { styles.put(text, style); return Optional.empty(); }, Style.EMPTY);
            assertEquals(TextColor.fromLegacyFormat(ChatFormatting.AQUA), styles.get("寒冷").getColor());
            assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GOLD), styles.get("3.5s").getColor());
            assertEquals(before, Component.Serializer.toJson(input));
        }
        assertNull(GearAffixesModule.translateDisplay(null));
    }

    private record Theme(String id, String name) {}
    @Test void crucibleMapperKeepsIdsAndNamesRawAndUsesReloadedConfiguration() throws Exception {
        Files.writeString(config.resolve("theme_names.json"), """
            {"Andersite Caves":"安山岩洞穴","Easter":"复活节","Raid Vault Orcish":"袭击宝库-兽人"}
            """);
        ThemeNamesModule.INSTANCE.reload(config);
        var themes = List.of(new Theme("a","Andersite Caves"), new Theme("b","Easter"), new Theme("c","Raid Vault Orcish"), new Theme("d","Unknown Theme"));
        Function<Theme,String> mapper = ThemeNamesModule.translateNames(Theme::name);
        var labels = themes.stream().collect(Collectors.toMap(Theme::id, mapper));
        assertEquals(Map.of("a","安山岩洞穴","b","复活节","c","袭击宝库-兽人","d","Unknown Theme"), labels);
        assertEquals("Easter", themes.get(1).name());
        Files.writeString(config.resolve("theme_names.json"), "{\"Easter\":\"新译名\"}");
        ThemeNamesModule.INSTANCE.reload(config);
        assertEquals("新译名", mapper.apply(themes.get(1)));
        Files.writeString(config.resolve("theme_names.json"), "{");
        assertThrows(java.io.IOException.class, () -> ThemeNamesModule.INSTANCE.reload(config));
        assertEquals("新译名", mapper.apply(themes.get(1)));
    }
}
