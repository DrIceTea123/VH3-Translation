package cn.vmct.installer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class Texts {
    private final Properties values = new Properties();
    private final Config config;
    public Texts(Config config) throws IOException {
        this.config = config;
        try (var in = Config.resource("ui.properties")) { values.load(new InputStreamReader(in, StandardCharsets.UTF_8)); }
    }
    public String get(String key, String... replacements) {
        String text = values.getProperty(key);
        if (text == null) throw new IllegalArgumentException("缺少界面文案：" + key);
        text = text.replace("{version}", config.version()).replace("{pack}", config.packVersion()).replace("{vault}", config.vaultVersion());
        for (int i = 0; i < replacements.length; i += 2) text = text.replace("{" + replacements[i] + "}", replacements[i + 1]);
        return text;
    }
    public String notice() throws IOException {
        try (var in = Config.resource("notice.txt")) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return text.isBlank() ? get("notice.empty") : text;
        }
    }
}
