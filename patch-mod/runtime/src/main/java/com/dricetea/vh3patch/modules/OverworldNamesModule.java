package com.dricetea.vh3patch.modules;
import com.dricetea.vh3patch.module.TemplateModule;
/** 只处理所属显示边界；词表由外部配置提供。 */
public final class OverworldNamesModule extends TemplateModule {
    public static final String ID = "overworld_names";
    public static final OverworldNamesModule INSTANCE = new OverworldNamesModule();
    private OverworldNamesModule() { super(ID); }
    public static String translate(String text) { return INSTANCE.translateText(text); }
    public static Object translateValue(Object value) { return value instanceof String s ? translate(s) : value; }
    public static net.minecraft.network.chat.Component translateComponent(net.minecraft.network.chat.Component text) { return INSTANCE.renderComponent(text); }
    public static String translatePreview(String text, net.minecraft.resources.ResourceLocation id) {
        return id != null && id.getNamespace().equals("the_vault") && id.getPath().startsWith("overworld/") ? translate(text) : text;
    }
}
