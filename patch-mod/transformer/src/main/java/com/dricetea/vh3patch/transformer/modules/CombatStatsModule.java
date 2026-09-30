package com.dricetea.vh3patch.transformer.modules;

import com.dricetea.vh3patch.transformer.PatchModule;
import com.dricetea.vh3patch.transformer.PatchSpec;
import com.dricetea.vh3patch.transformer.StringReturnPatch;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Set;

/** 结算怪物名的早期模块：声明目标、运行侧入口以及会冲突的 VP 常量。 */
public final class CombatStatsModule implements PatchModule {
    public static final String ID = "combat_stats";
    private final PatchSpec spec;
    private final StringReturnPatch patch;

    public CombatStatsModule() {
        this(PatchSpec.load(ID, "com/dricetea/vh3patch/modules/CombatStatsModule", Set.of(":", "_", " ")));
    }

    public CombatStatsModule(PatchSpec spec) {
        this.spec = spec;
        this.patch = new StringReturnPatch(spec);
    }

    @Override public PatchSpec spec() { return spec; }
    @Override public MethodNode target(ClassNode node) { return patch.target(node); }
    @Override public void apply(ClassNode node) { patch.apply(node); }
}
