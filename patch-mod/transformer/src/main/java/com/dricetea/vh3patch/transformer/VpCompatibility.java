package com.dricetea.vh3patch.transformer;

import com.google.gson.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VpCompatibility {
    private VpCompatibility() {}

    public static void assertCompatible(Path directory, PatchModule module) throws IOException {
        if (!Files.exists(directory)) return;
        try (var files = Files.walk(directory)) {
            for (Path path : files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).toList()) {
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

    /** 发布输入可能已经移除接管规则；无冲突时原样复制，有一组时迁移，重复组仍报错。 */
    public static JsonArray prepareConfiguration(JsonArray original, PatchModule module) {
        for (JsonElement element : original) {
            if (element.isJsonObject() && module.ownsVpRule(element.getAsJsonObject())) {
                return withoutOwnedMethod(original, module);
            }
        }
        return original.deepCopy();
    }
}
