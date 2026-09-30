package com.dricetea.vh3patch.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public record PatchSpec(String patchVersion, String targetVersion, String jarHash, String className,
                        String methodName, String descriptor, String fingerprint, int returnCount) {
    public static final String HELPER = "com/dricetea/vh3patch/client/MobNameResolver";
    public static final String HELPER_DESCRIPTOR = "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;";
    public static final String READY_PROPERTY = "vh3_translation_patch.transformer.ready";

    public static PatchSpec load() {
        Properties values = new Properties();
        try (InputStream stream = PatchSpec.class.getResourceAsStream("/vh3-patch.properties")) {
            if (stream == null) throw new IllegalStateException("Missing vh3-patch.properties");
            values.load(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read patch manifest", e);
        }
        return new PatchSpec(required(values, "patch.version"), required(values, "target.version"),
                required(values, "target.sha256"), required(values, "target.class"),
                required(values, "target.method"), required(values, "target.descriptor"),
                required(values, "target.fingerprint"), Integer.parseInt(required(values, "target.returns")));
    }

    private static String required(Properties values, String key) {
        String value = values.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing manifest key: " + key);
        return value;
    }
}
