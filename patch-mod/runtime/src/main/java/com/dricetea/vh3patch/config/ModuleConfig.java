package com.dricetea.vh3patch.config;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** 每个模块独立持有配置快照；这里只负责读写，不决定“键”代表英文还是实体标识。 */
public final class ModuleConfig {
    private final String fileName;
    private final String defaults;
    private volatile Map<String, String> values;

    public ModuleConfig(String moduleId, String defaults) throws IOException {
        if (!moduleId.matches("[a-z][a-z0-9_]*")) throw new IllegalArgumentException("Invalid module ID: " + moduleId);
        this.fileName = moduleId + ".json";
        this.defaults = defaults;
        // 第一次读取用户文件失败时，仍有随 JAR 附带的有效默认配置可用。
        this.values = parse(defaults);
    }

    public String fileName() { return fileName; }
    public String get(String key) { return key == null ? null : values.get(key); }

    /** 成功才整体替换快照；异常交给调用者记录，旧配置完全不变。 */
    public synchronized void reload(Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve(fileName);
        try {
            // CREATE_NEW 保证首次生成不会覆盖用户已经编辑的文件。
            Files.writeString(file, defaults, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException ignored) {
            // 已有文件是用户的配置，始终以它为准。
        }
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
