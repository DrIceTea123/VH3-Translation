package com.dricetea.vh3patch.module;

import com.dricetea.vh3patch.module.TranslationModule;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.Style;
import java.io.IOException;
import java.util.*;
import java.util.regex.*;

/** 仅重建卡牌显示组件；原卡牌数据、判定用类型/套组及传入组件均不修改。 */
public abstract class TemplateModule extends TranslationModule {
    private static final Pattern ARGUMENT = Pattern.compile("\\{([0-9]+)}");
    private static final Pattern SEPARATOR = Pattern.compile(", and |, | or |/");
    private volatile Compiled cached;
    private record Template(String key, String value, Pattern pattern, List<Integer> arguments, int literalLength) {}
    private record Compiled(Map<String,String> values, List<Template> templates) {}
    private record Run(int start, int end, String text, Style style) {}
    protected TemplateModule(String id) { super(id); }

    @Override protected void validateConfiguration(Map<String,String> values) throws IOException { compile(values); }
    private Compiled compiled() {
        Map<String,String> values = configurationSnapshot();
        Compiled result = cached;
        if (result != null && result.values == values) return result;
        try { result = compile(values); } catch (IOException impossible) { throw new IllegalStateException(impossible); }
        cached = result;
        return result;
    }
    private static Compiled compile(Map<String,String> values) throws IOException {
        List<Template> templates = new ArrayList<>();
        for (var entry : values.entrySet()) {
            String key = entry.getKey(); Matcher matcher = ARGUMENT.matcher(key);
            List<Integer> ids = new ArrayList<>(); StringBuilder regex = new StringBuilder("^");
            int start = 0, literalLength = 0;
            while (matcher.find()) {
                int id;
                try { id = Integer.parseInt(matcher.group(1)); } catch (NumberFormatException e) { throw new IOException("Invalid placeholder: " + key, e); }
                if (id > 7 || ids.contains(id) || (!ids.isEmpty() && matcher.start() == start)) throw new IOException("Ambiguous display template: " + key);
                String literal = key.substring(start, matcher.start()); literalLength += literal.length();
                regex.append(Pattern.quote(literal)).append("(.+?)"); ids.add(id); start = matcher.end();
            }
            if (ids.isEmpty()) continue;
            String end = key.substring(start); literalLength += end.length();
            if (literalLength == 0 || ids.stream().anyMatch(i -> i >= ids.size())) throw new IOException("Invalid display template arguments: " + key);
            regex.append(Pattern.quote(end)).append('$');
            String withoutArgs = ARGUMENT.matcher(key).replaceAll("");
            if (withoutArgs.contains("{") || withoutArgs.contains("}")) throw new IOException("Invalid template braces: " + key);
            Set<Integer> output = new HashSet<>(); Matcher translated = ARGUMENT.matcher(entry.getValue());
            while (translated.find()) {
                try { output.add(Integer.parseInt(translated.group(1))); } catch (NumberFormatException e) { throw new IOException("Invalid translated placeholder: " + key, e); }
            }
            String remaining = ARGUMENT.matcher(entry.getValue()).replaceAll("");
            // 空译文仍是显式隐藏；非空句式不得遗漏数值或引入不存在的参数。
            if ((!entry.getValue().isEmpty() && !output.equals(new HashSet<>(ids))) || remaining.contains("{") || remaining.contains("}"))
                throw new IOException("Display template must preserve its arguments: " + key);
            templates.add(new Template(key, entry.getValue(), Pattern.compile(regex.toString(), Pattern.DOTALL), List.copyOf(ids), literalLength));
        }
        // 具体句式优先；不依赖 JSON/HashMap 遍历顺序。同等长度按原文排序保证稳定。
        templates.sort(Comparator.comparingInt(Template::literalLength).reversed().thenComparing(Template::key));
        return new Compiled(values, List.copyOf(templates));
    }

    public final String translateText(String text) {
        return text == null ? null : render(new TextComponent(text), compiled(), 0, false).getString();
    }
    public final Object renderTooltip(Object value) {
        return value instanceof Component component ? renderComponent(component) : value;
    }
    public final Component renderComponent(Component component) {
        return component == null ? null : render(component, compiled(), 0, false);
    }
    /** 卡牌物品名称只允许精确覆盖，不把名称中的英文词当作条件/效果句式。 */
    public final Component renderName(Component component) {
        if (component == null) return null;
        String value = configuredTranslation(component.getString());
        return value == null ? component : new TextComponent(value).setStyle(component.getStyle());
    }

    private static Component render(Component original, Compiled config, int depth, boolean terms) {
        if (depth > 16) return original;
        String text = original.getString();
        String exact = config.values.get(text);
        if (exact != null) return new TextComponent(exact).setStyle(original.getStyle());
        for (Template template : config.templates) {
            Matcher match = template.pattern.matcher(text);
            if (!match.matches()) continue;
            if (template.value.isEmpty()) return new TextComponent("").setStyle(original.getStyle());
            List<Run> runs = runs(original); Style sentenceStyle = sentenceStyle(runs, match, original.getStyle()); Component[] args = new Component[template.arguments.size()];
            for (int i = 0; i < args.length; i++) args[template.arguments.get(i)] = render(slice(runs, match.start(i+1), match.end(i+1)), config, depth+1, true);
            MutableComponent result = new TextComponent("").setStyle(sentenceStyle);
            Matcher placeholders = ARGUMENT.matcher(template.value); int start = 0;
            while (placeholders.find()) {
                result.append(new TextComponent(template.value.substring(start, placeholders.start())).setStyle(sentenceStyle));
                result.append(args[Integer.parseInt(placeholders.group(1))]); start = placeholders.end();
            }
            result.append(new TextComponent(template.value.substring(start)).setStyle(sentenceStyle));
            return result;
        }
        if (terms) {
            // 只按卡牌列表的明确分隔符拆词，不在任意名称内部做全局单词替换。
            Matcher separators = SEPARATOR.matcher(text);
            if (separators.find()) {
                List<Run> runs = runs(original); MutableComponent result = new TextComponent(""); int start = 0;
                do {
                    result.append(render(slice(runs, start, separators.start()), config, depth+1, false));
                    result.append(render(slice(runs, separators.start(), separators.end()), config, depth+1, false)); start = separators.end();
                } while (separators.find());
                result.append(render(slice(runs, start, text.length()), config, depth+1, false)); return result;
            }
        }
        // 未匹配句式时逐个原组件精确查表，保留非文本组件、样式和未知文本。
        MutableComponent copy = original.plainCopy().setStyle(original.getStyle());
        if (original instanceof TextComponent literal) {
            String translated = config.values.get(literal.getText());
            if (translated != null) copy = new TextComponent(translated).setStyle(original.getStyle());
        }
        for (Component child : original.getSiblings()) copy.append(render(child, config, depth+1, terms));
        return copy;
    }
    private static Style sentenceStyle(List<Run> runs, Matcher match, Style fallback) {
        // 译文的固定文字沿用原句固定文字的主要样式；移动的数值单独保留原样式。
        Map<Style,Integer> weights = new LinkedHashMap<>();
        int start = 0;
        for (int i = 1; i <= match.groupCount()+1; i++) {
            int end = i <= match.groupCount() ? match.start(i) : match.end();
            for (Run run : runs) {
                int length = Math.min(end, run.end)-Math.max(start, run.start);
                if (length > 0) weights.merge(run.style, length, Integer::sum);
            }
            if (i <= match.groupCount()) start = match.end(i);
        }
        return weights.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(fallback);
    }
    private static List<Run> runs(Component component) {
        List<Run> runs = new ArrayList<>(); int[] index = {0};
        component.visit((Style style, String text) -> {
            runs.add(new Run(index[0], index[0]+text.length(), text, style)); index[0] += text.length(); return Optional.empty();
        }, Style.EMPTY);
        return runs;
    }
    private static Component slice(List<Run> runs, int start, int end) {
        MutableComponent result = new TextComponent("");
        for (Run run : runs) {
            int left = Math.max(start,run.start), right = Math.min(end,run.end);
            if (right > left) {
                if (result.getSiblings().isEmpty()) result.setStyle(run.style);
                result.append(new TextComponent(run.text.substring(left-run.start,right-run.start)).setStyle(run.style));
            }
        }
        return result;
    }
}
