package com.dricetea.vh3patch.modules;
import com.dricetea.vh3patch.module.TemplateModule;
import net.minecraft.network.chat.Component;
/** 卡牌独立词表，通用组件句式及样式保留逻辑由 TemplateModule 共享。 */
public final class CardTextModule extends TemplateModule {
    public static final String ID = "card_text";
    public static final CardTextModule INSTANCE = new CardTextModule();
    private CardTextModule() { super(ID); }
    public static String translate(String text) { return INSTANCE.translateText(text); }
    public static Object translateTooltip(Object value) { return INSTANCE.renderTooltip(value); }
    public static Component translateComponent(Component text) { return INSTANCE.renderComponent(text); }
    public static Component translateName(Component text) { return INSTANCE.renderName(text); }
}
