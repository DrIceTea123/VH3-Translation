package com.dricetea.vh3patch.modules;
import com.dricetea.vh3patch.module.TemplateModule;
/** 只处理所属显示边界；词表由外部配置提供。 */
public final class ThemeNamesModule extends TemplateModule {
    public static final String ID = "theme_names";
    public static final ThemeNamesModule INSTANCE = new ThemeNamesModule();
    private ThemeNamesModule() { super(ID); }
    public static String translate(String text) { return INSTANCE.translateText(text); }
    /** 每次调用时读取当前译表；原 getter、主题标识与自定义条目保持原值。 */
    public static <T> java.util.function.Function<T,String> translateNames(java.util.function.Function<T,String> names) {
        return value -> translate(names.apply(value));
    }
    public static Object translateValue(Object value) { return value instanceof String s ? translate(s) : value; }
    public static net.minecraft.network.chat.Component translateComponent(net.minecraft.network.chat.Component text) { return INSTANCE.renderComponent(text); }

}
