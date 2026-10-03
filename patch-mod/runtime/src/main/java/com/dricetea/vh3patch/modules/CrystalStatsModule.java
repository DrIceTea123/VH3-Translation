package com.dricetea.vh3patch.modules;
import com.dricetea.vh3patch.module.TemplateModule;
/** 只处理所属显示边界；词表由外部配置提供。 */
public final class CrystalStatsModule extends TemplateModule {
    public static final String ID = "crystal_stats";
    public static final CrystalStatsModule INSTANCE = new CrystalStatsModule();
    private CrystalStatsModule() { super(ID); }
    public static String translate(String text) { return INSTANCE.translateText(text); }
    public static Object translateValue(Object value) { return value instanceof String s ? translate(s) : value; }
    public static net.minecraft.network.chat.Component translateComponent(net.minecraft.network.chat.Component text) { return INSTANCE.renderComponent(text); }

}
