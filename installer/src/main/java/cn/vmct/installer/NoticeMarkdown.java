package cn.vmct.installer;

import java.net.URI;
import java.util.regex.Pattern;

/** 首页说明使用的轻量 Markdown 子集；转义原始 HTML，不加载外部图片或样式。 */
final class NoticeMarkdown {
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern ITEM = Pattern.compile("^([-+*]|\\d+[.)])\\s+(.+)$");
    private NoticeMarkdown() {}

    static String html(String markdown) {
        StringBuilder out = new StringBuilder("<html><head><style>"
                + "body { margin: 12px; color: #222222; }"
                + "h1 { font-size: 140%; } h2 { font-size: 120%; } h3 { font-size: 110%; }"
                + "p { margin-top: 6px; margin-bottom: 12px; }"
                + "li { margin-bottom: 6px; } pre, code { background-color: #f1f3f5; }"
                + "blockquote { color: #555555; margin-left: 16px; } a { color: #176878; }"
                + "</style></head><body>");
        String list = "";
        boolean paragraph = false, code = false;
        for (String raw : markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            String line = raw.strip();
            if (code) {
                if (line.startsWith("```")) { out.append("</pre>"); code = false; }
                else out.append(escape(raw)).append('\n');
                continue;
            }
            var heading = HEADING.matcher(line);
            var item = ITEM.matcher(line);
            boolean isItem = item.matches();
            boolean block = line.isEmpty() || line.startsWith("```") || heading.matches()
                    || isItem || line.startsWith(">") || line.matches("(?:-{3,}|\\*{3,}|_{3,})");
            if (block && paragraph) { out.append("</p>"); paragraph = false; }
            String nextList = isItem ? (Character.isDigit(item.group(1).charAt(0)) ? "ol" : "ul") : "";
            if (!list.equals(nextList)) {
                if (!list.isEmpty()) out.append("</").append(list).append('>');
                list = nextList;
                if (!list.isEmpty()) out.append('<').append(list).append('>');
            }
            if (line.isEmpty()) continue;
            if (line.startsWith("```")) { out.append("<pre>"); code = true; }
            else if (heading.matches()) {
                int level = heading.group(1).length();
                out.append("<h").append(level).append('>').append(inline(heading.group(2)))
                        .append("</h").append(level).append('>');
            } else if (isItem) out.append("<li>").append(inline(item.group(2))).append("</li>");
            else if (line.startsWith(">")) out.append("<blockquote>").append(inline(line.substring(1).strip())).append("</blockquote>");
            else if (line.matches("(?:-{3,}|\\*{3,}|_{3,})")) out.append("<hr>");
            else {
                if (!paragraph) { out.append("<p>"); paragraph = true; } else out.append("<br>");
                out.append(inline(line));
            }
        }
        if (paragraph) out.append("</p>");
        if (!list.isEmpty()) out.append("</").append(list).append('>');
        if (code) out.append("</pre>");
        return out.append("</body></html>").toString();
    }

    private static String inline(String text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length();) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                out.append(escape(text.substring(i + 1, i + 2))); i += 2; continue;
            }
            if (c == '[') {
                int labelEnd = text.indexOf("](", i + 1);
                int urlEnd = labelEnd < 0 ? -1 : text.indexOf(')', labelEnd + 2);
                if (urlEnd >= 0) {
                    String url = text.substring(labelEnd + 2, urlEnd);
                    if (webLink(url)) {
                        out.append("<a href=\"").append(escape(url)).append("\">")
                                .append(escape(text.substring(i + 1, labelEnd))).append("</a>");
                        i = urlEnd + 1; continue;
                    }
                }
            }
            String mark = text.startsWith("**", i) ? "**" : text.startsWith("__", i) ? "__" :
                    c == '`' ? "`" : c == '*' ? "*" : "";
            if (!mark.isEmpty()) {
                int end = text.indexOf(mark, i + mark.length());
                if (end > i + mark.length()) {
                    String tag = mark.equals("`") ? "code" : mark.length() == 2 ? "strong" : "em";
                    String content = text.substring(i + mark.length(), end);
                    out.append('<').append(tag).append('>').append(mark.equals("`") ? escape(content) : inline(content))
                            .append("</").append(tag).append('>');
                    i = end + mark.length(); continue;
                }
            }
            out.append(escape(String.valueOf(c))); i++;
        }
        return out.toString();
    }

    static boolean webLink(String text) {
        try {
            URI uri = URI.create(text);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) && uri.getHost() != null;
        } catch (IllegalArgumentException invalid) { return false; }
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
