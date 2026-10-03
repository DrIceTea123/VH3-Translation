package com.dricetea.vh3patch.modules;
import com.dricetea.vh3patch.module.TemplateModule;
/** 只处理所属显示边界；词表由外部配置提供。 */
public final class BestiaryGroupsModule extends TemplateModule {
    public static final String ID = "bestiary_groups";
    public static final BestiaryGroupsModule INSTANCE = new BestiaryGroupsModule();
    private BestiaryGroupsModule() { super(ID); }
    public static String translate(String text) { return INSTANCE.translateText(text); }
    public static Object translateValue(Object value) { return value instanceof String s ? translate(s) : value; }
    public static net.minecraft.network.chat.Component translateComponent(net.minecraft.network.chat.Component text) { return INSTANCE.renderComponent(text); }
    /** 与原 getFilterByName 配对；不查翻译表，不读取组件显示文本。 */
    public static String lookupName(net.minecraft.resources.ResourceLocation id) {
        String path=id.getPath();
        if (path.equalsIgnoreCase("fighter")) return "Dweller";
        StringBuilder result=new StringBuilder();
        for(String word:path.split("_")) {
            if(result.length()>0) result.append(' ');
            if(!word.isEmpty()) result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString().trim();
    }
}
