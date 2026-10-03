package com.dricetea.vh3patch.modules;
import com.dricetea.vh3patch.module.TemplateModule;
/** 只处理所属显示边界；词表由外部配置提供。 */
public final class QuestNamesModule extends TemplateModule {
    public static final String ID = "quest_names";
    public static final QuestNamesModule INSTANCE = new QuestNamesModule();
    private QuestNamesModule() { super(ID); }
    public static String translate(String text) { return INSTANCE.translateText(text); }
    public static Object translateValue(Object value) { return value instanceof String s ? translate(s) : value; }
    public static net.minecraft.network.chat.Component translateComponent(net.minecraft.network.chat.Component text) { return INSTANCE.renderComponent(text); }

}
