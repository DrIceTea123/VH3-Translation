package com.dricetea.vh3patch.config;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** 每个模块独立持有外部配置快照；只读取用户文件，不生成或内置译文。 */
public final class ModuleConfig {
    private final String fileName;
    private volatile Map<String, String> values = Map.of();

    public ModuleConfig(String moduleId) {
        if (!moduleId.matches("[a-z][a-z0-9_]*")) throw new IllegalArgumentException("Invalid module ID: " + moduleId);
        this.fileName = moduleId + ".json";
    }

    public String fileName() { return fileName; }
    public String get(String key) { return key == null ? null : values.get(key); }

    /** 成功才整体替换快照；异常交给调用者记录，旧配置完全不变。 */
    public synchronized void reload(Path directory) throws IOException {
        Path file = directory.resolve(fileName);
        // 文件必须由汉化包提供。缺失或损坏时抛错，且不会写回或补齐任何条目。
        Map<String, String> next = parse(Files.readString(file, StandardCharsets.UTF_8));
        values = next;
    }

    private static Map<String, String> parse(String json) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            // 不使用 Gson 的宽松对象解析：重复键、非字符串值、注释和尾随内容都应报错。
            reader.setLenient(false);
            if (reader.peek() != JsonToken.BEGIN_OBJECT) throw new IOException("Expected a JSON object");
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (reader.peek() != JsonToken.STRING) throw new IOException("Expected string value for: " + key);
                String value = reader.nextString();
                if (result.putIfAbsent(key, value) != null) throw new IOException("Duplicate key: " + key);
            }
            reader.endObject();
            if (reader.peek() != JsonToken.END_DOCUMENT) throw new IOException("Unexpected trailing JSON content");
        } catch (IllegalStateException e) {
            throw new IOException("Invalid module JSON: " + e.getMessage(), e);
        }
        return Map.copyOf(result);
    }
}
