package com.dricetea.vh3patch.transformer;

import com.dricetea.vh3patch.transformer.modules.*;
import java.util.*;

/** 早期模块注册表：同类只生成一个转换器，模块可拥有多个方法及不同适用端。 */
public final class PatchModules {
    private static final List<PatchModule> ALL = validate(List.of(new MobNamesModule(), new SoundNamesModule(), new ResearchNamesModule(), new ChestNamesModule(), new CardTextModule(), new QuestNamesModule(), new GearRarityModule(), new BestiaryGroupsModule(), new OverworldNamesModule(), new RoomNamesModule(), new ThemeNamesModule(), new CrystalStatsModule()));
    private PatchModules() {}
    public static List<PatchModule> all() { return ALL; }
    public static List<PatchModule> active(boolean client) {
        return ALL.stream().filter(m -> !m.specs(client).isEmpty()).toList();
    }
    public static PatchModule find(String id) {
        return ALL.stream().filter(m -> m.spec().moduleId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown module: " + id));
    }
    public static Map<String, List<PatchModule>> byClass() { return byClass(true); }
    public static Map<String, List<PatchModule>> byClass(boolean client) {
        Map<String, List<PatchModule>> result = new LinkedHashMap<>();
        for (PatchModule module : active(client)) {
            for (String name : module.specs(client).stream().map(PatchSpec::className).distinct().toList())
                result.computeIfAbsent(name, ignored -> new ArrayList<>()).add(module);
        }
        return result;
    }
    private static List<PatchModule> validate(List<PatchModule> modules) {
        var ids = new HashSet<String>();
        var targets = new HashSet<String>();
        PatchSpec baseline = modules.get(0).spec();
        for (PatchModule module : modules) {
            if (!ids.add(module.spec().moduleId())) throw new IllegalStateException("Duplicate module ID");
            for (PatchSpec spec : module.specs()) {
                if (!targets.add(spec.className() + "." + spec.methodName() + spec.descriptor()))
                    throw new IllegalStateException("Duplicate target: " + spec);
                if (!spec.moduleId().equals(module.spec().moduleId()) || !spec.helperClass().equals(module.spec().helperClass())
                        || !spec.patchVersion().equals(baseline.patchVersion()) || !spec.targetVersion().equals(baseline.targetVersion())
                        || !spec.jarHash().equals(baseline.jarHash())) throw new IllegalStateException("Inconsistent module manifests");
            }
        }
        return List.copyOf(modules);
    }
}
