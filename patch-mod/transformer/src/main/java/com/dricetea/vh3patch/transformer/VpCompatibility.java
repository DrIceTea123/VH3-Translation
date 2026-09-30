package com.dricetea.vh3patch.transformer;

import com.google.gson.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VpCompatibility {
    private VpCompatibility() {}

    public static boolean ownsMethod(JsonObject rule, PatchSpec spec) {
        if (!rule.has("target_class") || !rule.get("target_class").isJsonObject()) return false;
        JsonObject target = rule.getAsJsonObject("target_class");
        if (!target.has("name") || !target.get("name").getAsString().replace('.', '/').equals(spec.className())) return false;
        if (target.has("method")) return spec.methodName().equals(target.get("method").getAsString());
        // 仅当整类规则修改了该模块声明的相关常量时，才判定它与方法补丁冲突。
        if (rule.has("pairs") && rule.get("pairs").isJsonArray()) {
            for (JsonElement pair : rule.getAsJsonArray("pairs")) {
                if (pair.isJsonObject() && pair.getAsJsonObject().has("key")
                        && spec.ownedLiterals().contains(pair.getAsJsonObject().get("key").getAsString())) return true;
            }
        }
        return false;
    }

    public static void assertCompatible(Path directory, PatchSpec spec) throws IOException {
        if (!Files.exists(directory)) return;
        try (var files = Files.walk(directory)) {
            for (Path path : files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).toList()) {
                JsonElement root;
                try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    root = JsonParser.parseReader(reader);
                } catch (JsonParseException e) {
                    throw new IllegalStateException("Cannot validate VP configuration: " + path.getFileName(), e);
                }
                if (hasConflict(root, spec)) {
                    throw new IllegalStateException("VP still owns " + spec.moduleId() + "/" + spec.methodName() + " in " + path.getFileName()
                            + ". Generate and install the reviewed compatibility configuration, then clear VP cache.");
                }
            }
        }
    }

    private static boolean hasConflict(JsonElement element, PatchSpec spec) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) if (hasConflict(child, spec)) return true;
        } else if (element.isJsonObject()) {
            if (ownsMethod(element.getAsJsonObject(), spec)) return true;
            for (var entry : element.getAsJsonObject().entrySet()) if (hasConflict(entry.getValue(), spec)) return true;
        }
        return false;
    }

    public static JsonArray withoutOwnedMethod(JsonArray original, PatchSpec spec) {
        JsonArray result = new JsonArray();
        int removed = 0;
        for (JsonElement element : original) {
            if (element.isJsonObject() && ownsMethod(element.getAsJsonObject(), spec)) removed++;
            else result.add(element.deepCopy());
        }
        if (removed != 1) throw new IllegalStateException("Expected exactly one VP ownership group; got " + removed);
        return result;
    }
}
