package com.dricetea.vh3patch.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

/** 单个模块的方法清单；不引用游戏侧类型，避免早期服务提前加载 Minecraft。 */
public record PatchSpec(String patchVersion, String targetVersion, String jarHash, String className,
                        String methodName, String descriptor, String fingerprint, int returnCount,
                        String moduleId, String helperClass, Set<String> ownedLiterals) {
    public static final String HELPER_DESCRIPTOR = "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;";
    public static final String READY_PROPERTY = "vh3_translation_patch.transformer.ready";

    public static PatchSpec load(String moduleId, String helperClass, Set<String> ownedLiterals) {
        Properties values = new Properties();
        String resource = "/patches/" + moduleId + ".properties";
        try (InputStream stream = PatchSpec.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing " + resource);
            values.load(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read patch manifest", e);
        }
        return new PatchSpec(required(values, "patch.version"), required(values, "target.version"),
                required(values, "target.sha256"), required(values, "target.class"),
                required(values, "target.method"), required(values, "target.descriptor"),
                required(values, "target.fingerprint"), Integer.parseInt(required(values, "target.returns")),
                moduleId, helperClass, Set.copyOf(ownedLiterals));
    }

    public String defaultConfigResource() { return "module-defaults/" + moduleId + ".json"; }

    private static String required(Properties values, String key) {
        String value = values.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing manifest key: " + key);
        return value;
    }
}
