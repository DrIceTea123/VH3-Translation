package com.dricetea.vh3patch.transformer;

import com.dricetea.vh3patch.transformer.modules.CombatStatsModule;
import com.dricetea.vh3patch.transformer.modules.SoundNamesModule;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 早期模块注册表；新增模块后，启动校验和离线工具均使用同一份列表。 */
public final class PatchModules {
    private static final List<PatchModule> ALL = validate(List.of(new CombatStatsModule(), new SoundNamesModule()));
    private PatchModules() {}

    public static List<PatchModule> all() { return ALL; }

    public static PatchModule find(String id) {
        return ALL.stream().filter(module -> module.spec().moduleId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown module: " + id));
    }

    // 同类的多个方法按注册顺序一起变换，避免多个转换器互相覆盖输出。
    public static Map<String, List<PatchModule>> byClass() {
        return ALL.stream().collect(Collectors.groupingBy(module -> module.spec().className(),
                java.util.LinkedHashMap::new, Collectors.toList()));
    }

    private static List<PatchModule> validate(List<PatchModule> modules) {
        if (modules.isEmpty()) throw new IllegalStateException("No patch modules registered");
        var ids = new HashSet<String>();
        var targets = new HashSet<String>();
        for (PatchModule module : modules) {
            PatchSpec spec = module.spec();
            if (!ids.add(spec.moduleId()) || !targets.add(spec.className() + "." + spec.methodName() + spec.descriptor())) {
                throw new IllegalStateException("Duplicate module ID or target: " + spec.moduleId());
            }
            if (!spec.patchVersion().equals(modules.get(0).spec().patchVersion())) {
                throw new IllegalStateException("Inconsistent module versions");
            }
        }
        return List.copyOf(modules);
    }
}
