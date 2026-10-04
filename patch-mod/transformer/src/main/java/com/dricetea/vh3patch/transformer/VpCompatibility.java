package com.dricetea.vh3patch.transformer;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class VpCompatibility {
    private VpCompatibility() {}

    public static void assertCompatible(Path directory, PatchModule module) throws IOException {
        for (Path path : enabledModuleFiles(directory)) {
            // VP 会为缺失模块创建空模板；预检无需创建文件，也不能转而扫描其他文件。
            if (Files.notExists(path)) continue;
            JsonElement root;
            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader);
            } catch (JsonParseException e) {
                throw new IllegalStateException("Cannot validate VP configuration: " + path.getFileName(), e);
            }
            if (hasConflict(root, module)) {
                throw new IllegalStateException("VP still owns " + module.spec().moduleId() + "/" + module.spec().methodName() + " in " + path.getFileName()
                        + ". Generate and install the reviewed compatibility configuration, then clear VP cache.");
            }
        }
    }

    /** 对齐 VP 1.4.4 的 readConfig / _init：mods（别名 m）最后出现者生效，逐项追加 .json。 */
    static List<Path> enabledModuleFiles(Path directory) throws IOException {
        Path config=directory.resolve("config.json");
        // VP 首次生成配置时 mods 默认为空；无配置不代表加载目录里的全部 JSON。
        if (Files.notExists(config)) return List.of();
        List<String> names=new ArrayList<>();
        try (JsonReader reader=new JsonReader(Files.newBufferedReader(config,StandardCharsets.UTF_8))) {
            reader.beginObject();
            while (reader.hasNext()) {
                String key=reader.nextName();
                if (!key.equals("mods") && !key.equals("m")) { reader.skipValue();continue; }
                names.clear();reader.beginArray();
                while (reader.hasNext()) {
                    if (reader.peek()!=JsonToken.STRING) throw new IOException("VP mods must contain module names as strings");
                    names.add(reader.nextString());
                }
                reader.endArray();
            }
            reader.endObject();
            if (reader.peek()!=JsonToken.END_DOCUMENT) throw new IOException("Trailing content in VP config.json");
        } catch (IOException | IllegalStateException e) {
            throw new IOException("Cannot read VP enabled modules from " + config,e);
        }
        Set<Path> files=new LinkedHashSet<>();
        for (String name:names) files.add(directory.resolve(name+".json"));
        return List.copyOf(files);
    }

    private static boolean hasConflict(JsonElement element, PatchModule module) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) if (hasConflict(child, module)) return true;
        } else if (element.isJsonObject()) {
            if (module.ownsVpRule(element.getAsJsonObject())) return true;
            for (var entry : element.getAsJsonObject().entrySet()) if (hasConflict(entry.getValue(), module)) return true;
        }
        return false;
    }

    public static JsonArray withoutOwnedMethod(JsonArray original, PatchModule module) {
        JsonArray result = new JsonArray();
        int removed = 0;
        for (JsonElement element : original) {
            if (element.isJsonObject() && module.ownsVpRule(element.getAsJsonObject())) removed++;
            else result.add(element.deepCopy());
        }
        if (removed != 1) throw new IllegalStateException("Expected exactly one VP ownership group; got " + removed);
        return result;
    }

    /** 无冲突时复制；多组迁移的重复/混合范围判定由模块负责。 */
    public static JsonArray prepareConfiguration(JsonArray original, PatchModule module) {
        var owned = new java.util.ArrayList<JsonObject>();
        JsonArray result = new JsonArray();
        for (JsonElement element : original) {
            if (element.isJsonObject() && module.ownsVpRule(element.getAsJsonObject())) owned.add(element.getAsJsonObject());
            else result.add(element.deepCopy());
        }
        module.validateVpMigration(owned);
        return result;
    }
}
