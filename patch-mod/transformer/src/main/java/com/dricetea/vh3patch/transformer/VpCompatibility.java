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

    /** VP 1.5.3-hotfix：总配置仍在旧目录，模块位于游戏根目录 vaultpatcher/modules。 */
    static List<Path> enabledModuleFiles(Path directory) throws IOException {
        Path config=directory.resolve("config.json");
        // 无配置时 modules 为空、load_all_modules=false，不扫描松散文件。
        if (Files.notExists(config)) return List.of();
        List<String> names=new ArrayList<>();
        boolean loadAll=false;
        try (JsonReader reader=new JsonReader(Files.newBufferedReader(config,StandardCharsets.UTF_8))) {
            reader.beginObject();
            while (reader.hasNext()) {
                String key=reader.nextName();
                if (key.equals("load_all_modules")) { loadAll=reader.nextBoolean();continue; }
                if (!key.equals("modules")) { reader.skipValue();continue; }
                names.clear();reader.beginArray();
                while (reader.hasNext()) {
                    if (reader.peek()!=JsonToken.STRING) throw new IOException("VP modules must contain module names as strings");
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
        Path modules=moduleDirectory(directory);
        if (loadAll) {
            // 与 VP 的 DirectoryStream("*.json") 一致：只看第一层，显式列表不再生效。
            if (Files.isDirectory(modules)) try (var stream=Files.newDirectoryStream(modules,"*.json")) {
                for (Path file:stream) files.add(file);
            }
        } else for (String name:names) {
            Path file=modules.resolve(name+".json");
            // VP 会把同名旧模块移到新位置；只检查实际选中的回退文件，不在预检中移动它。
            Path legacy=directory.resolve(file.getFileName());
            files.add(Files.notExists(file) && Files.exists(legacy) ? legacy : file);
        }
        return List.copyOf(files);
    }

    static Path moduleDirectory(Path configDirectory) {
        return configDirectory.toAbsolutePath().normalize().getParent().getParent().resolve("vaultpatcher/modules");
    }

    /** 将新版数组 + info 转为模块既有判定模型；保留旧输入以支持历史导入。不会修改原对象。 */
    public static JsonObject normalizeRule(JsonObject rule) {
        JsonArray targets=new JsonArray(), pairs=new JsonArray();
        JsonObject selectors=new JsonObject();boolean hasTargets=false,hasPairs=false;
        // 新解析器累加类名和 pairs；info 内各字段按读取顺序覆盖。
        for (var entry:rule.entrySet()) switch (entry.getKey()) {
            case "target_class", "target_classes", "t", "s" -> {
                JsonElement value=entry.getValue();
                if (value.isJsonObject() || (value.isJsonArray() && value.getAsJsonArray().size()>0
                        && value.getAsJsonArray().get(0).isJsonObject())) return normalizeLegacyRule(rule);
                if (!value.isJsonArray()) throw new IllegalStateException("VP target classes must be an array");
                targets.addAll(value.getAsJsonArray());hasTargets=true;
            }
            case "info", "i" -> {
                for(var field:entry.getValue().getAsJsonObject().entrySet()) {
                    String key=switch(field.getKey()) { case "m" -> "method"; case "l" -> "local"; case "o" -> "ordinal"; default -> field.getKey(); };
                    selectors.add(key,field.getValue().deepCopy());
                }
            }
            case "pairs", "p" -> { pairs.addAll(entry.getValue().getAsJsonArray());hasPairs=true; }
            default -> { }
        }
        if (!hasTargets) return rule;
        for(String key:List.of("method","local"))
            if(selectors.has(key) && selectors.get(key).getAsString().isBlank()) selectors.remove(key);
        JsonObject normalized=rule.deepCopy();
        for (String key:List.of("target_class","target_classes","t","s","info","i","p")) normalized.remove(key);
        JsonArray legacyTargets=new JsonArray();
        for (JsonElement name:targets) {
            JsonObject target=selectors.deepCopy();target.addProperty("name",name.getAsString());legacyTargets.add(target);
        }
        if (legacyTargets.size()==1) normalized.add("target_class",legacyTargets.get(0));
        else normalized.add("target_classes",legacyTargets);
        if (hasPairs) {
            JsonArray normalizedPairs=new JsonArray();
            for (JsonElement pair:pairs) {
                JsonObject p=new JsonObject();
                for (var entry:pair.getAsJsonObject().entrySet()) {
                    String key=switch(entry.getKey()) { case "k" -> "key"; case "v" -> "value"; default -> entry.getKey(); };
                    p.add(key,entry.getValue().deepCopy());
                }
                normalizedPairs.add(p);
            }
            normalized.add("pairs",normalizedPairs);
        }
        return normalized;
    }

    private static JsonObject normalizeLegacyRule(JsonObject rule) {
        JsonObject normalized=rule.deepCopy();
        List<JsonObject> targets=new ArrayList<>();
        if(normalized.has("target_class") && normalized.get("target_class").isJsonObject()) targets.add(normalized.getAsJsonObject("target_class"));
        if(normalized.has("target_classes")) for(JsonElement target:normalized.getAsJsonArray("target_classes"))
            if(target.isJsonObject()) targets.add(target.getAsJsonObject());
        for(JsonObject target:targets) for(String key:List.of("method","local"))
            if(target.has(key) && target.get(key).getAsString().isBlank()) target.remove(key);
        return normalized;
    }

    private static boolean hasConflict(JsonElement element, PatchModule module) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) if (hasConflict(child, module)) return true;
        } else if (element.isJsonObject()) {
            if (module.ownsVpRule(normalizeRule(element.getAsJsonObject()))) return true;
        }
        return false;
    }

    public static JsonArray withoutOwnedMethod(JsonArray original, PatchModule module) {
        JsonArray result = new JsonArray();
        int removed = 0;
        for (JsonElement element : original) {
            if (element.isJsonObject() && module.ownsVpRule(normalizeRule(element.getAsJsonObject()))) removed++;
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
            if (element.isJsonObject() && module.ownsVpRule(normalizeRule(element.getAsJsonObject()))) owned.add(normalizeRule(element.getAsJsonObject()));
            else result.add(element.deepCopy());
        }
        module.validateVpMigration(owned);
        return result;
    }
}
