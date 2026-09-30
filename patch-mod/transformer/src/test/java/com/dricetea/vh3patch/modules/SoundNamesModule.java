package com.dricetea.vh3patch.modules;

import com.google.gson.JsonObject;

/** 隔离执行真实字节码时使用的测试入口；不打包到任何发布 JAR。 */
public final class SoundNamesModule {
    public static JsonObject mappings = new JsonObject();
    public static String translate(String field, String original) throws Exception {
        return mappings.has(field) ? mappings.get(field).getAsString() : original;
    }
}
