package com.dricetea.vh3patch.module;

import com.dricetea.vh3patch.modules.ResearchNamesModule;
import com.dricetea.vh3patch.modules.ChestNamesModule;
import com.dricetea.vh3patch.modules.CardTextModule;
import com.dricetea.vh3patch.modules.ThemeNamesModule;
import java.nio.file.Path;
import java.util.List;

/** 两端通用模块入口，不得引用 ClientModules、Minecraft 或客户端 I18n。 */
public final class CommonModules {
    private static final List<TranslationModule> MODULES = List.of(ThemeNamesModule.INSTANCE, ResearchNamesModule.INSTANCE, ChestNamesModule.INSTANCE, CardTextModule.INSTANCE);
    private CommonModules() {}
    public static List<TranslationModule> all() { return MODULES; }
    public static void initialize(Path directory) {
        for (TranslationModule module : MODULES) module.initialize(directory);
    }
}
