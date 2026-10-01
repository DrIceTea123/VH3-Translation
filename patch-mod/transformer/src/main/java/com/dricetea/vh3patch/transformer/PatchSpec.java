package com.dricetea.vh3patch.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/** 单个模块的方法清单；不引用游戏侧类型，避免早期服务提前加载 Minecraft。 */
public record PatchSpec(String patchVersion, String targetVersion, String jarHash, String className,
                        String methodName, String descriptor, String fingerprint, int returnCount,
                        String moduleId, String helperClass, Set<String> ownedLiterals, Side side) {
    public enum Side { CLIENT, BOTH }
    public PatchSpec(String patchVersion, String targetVersion, String jarHash, String className,
                     String methodName, String descriptor, String fingerprint, int returnCount,
                     String moduleId, String helperClass, Set<String> ownedLiterals) {
        this(patchVersion, targetVersion, jarHash, className, methodName, descriptor, fingerprint,
                returnCount, moduleId, helperClass, ownedLiterals, Side.CLIENT);
    }
    // 旧单方法清单的 returns 与新清单的 hooks 均表示必须命中的注入点数量。
    public int hookCount() { return returnCount; }
    public boolean enabled(boolean client) { return client || side == Side.BOTH; }
    public static final String HELPER_DESCRIPTOR = "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;";
    public static final String READY_PROPERTY = "vh3_translation_patch.transformer.ready";

    public static PatchSpec load(String moduleId, String helperClass, Set<String> ownedLiterals) {
        List<PatchSpec> specs = loadAll(moduleId, helperClass, ownedLiterals);
        if (specs.size() != 1) throw new IllegalStateException("Expected a single-method module: " + moduleId);
        return specs.get(0);
    }

    public static List<PatchSpec> loadAll(String moduleId, String helperClass, Set<String> ownedLiterals) {
        Properties values = new Properties();
        String resource = "/patches/" + moduleId + ".properties";
        try (InputStream stream = PatchSpec.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing " + resource);
            values.load(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read patch manifest", e);
        }
        List<PatchSpec> result = new ArrayList<>();
        int count = Integer.parseInt(values.getProperty("target.count", "1"));
        if (count < 1) throw new IllegalStateException("Empty target manifest");
        for (int i = 0; i < count; i++) {
            String prefix = values.containsKey("target.count") ? "target." + i + "." : "target.";
            result.add(new PatchSpec(required(values, "patch.version"), required(values, "target.version"),
                    required(values, "target.sha256"), required(values, prefix + "class"),
                    required(values, prefix + "method"), required(values, prefix + "descriptor"),
                    required(values, prefix + "fingerprint"), Integer.parseInt(required(values,
                    prefix + (values.containsKey(prefix + "hooks") ? "hooks" : "returns"))),
                    moduleId, helperClass, Set.copyOf(ownedLiterals),
                    Side.valueOf(values.getProperty(prefix + "side", "CLIENT"))));
        }
        return List.copyOf(result);
    }

    public String configPath() { return "config/vh3_translation_patch/" + moduleId + ".json"; }

    private static String required(Properties values, String key) {
        String value = values.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing manifest key: " + key);
        return value;
    }
}
