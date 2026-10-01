package com.dricetea.vh3patch.modules;

import com.dricetea.vh3patch.module.TranslationModule;

/** 两端通用研究名模块；原始英文标识作为键，不依赖客户端语言系统。 */
public final class ResearchNamesModule extends TranslationModule {
    public static final String ID = "research_names";
    public static final ResearchNamesModule INSTANCE = new ResearchNamesModule();
    private ResearchNamesModule() { super(ID); }

    public static String translate(String original) { return translate(original, original); }
    public static String translate(String original, String fallback) {
        String translated = INSTANCE.configuredTranslation(original);
        // restrictedBy 用 null 表示无需研究，必须保留 null，不能变成文字。
        return translated != null ? translated : fallback;
    }
}
