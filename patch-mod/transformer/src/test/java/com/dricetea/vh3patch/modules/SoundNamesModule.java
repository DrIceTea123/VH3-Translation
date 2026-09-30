package com.dricetea.vh3patch.modules;

import com.dricetea.vh3patch.transformer.JsonFiles;
import com.google.gson.JsonObject;
import java.nio.file.Path;

/** 隔离执行真实字节码时使用的测试入口；不打包到任何发布 JAR。 */
public final class SoundNamesModule {
    public static String translate(String field, String original) throws Exception {
        JsonObject map = JsonFiles.read(Path.of(System.getProperty("vh3.test.soundDefaults"))).getAsJsonObject();
        return map.has(field) ? map.get(field).getAsString() : original;
    }
}
