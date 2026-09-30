package com.dricetea.vh3patch;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(TranslationPatchMod.MOD_ID)
public final class TranslationPatchMod {
    public static final String MOD_ID = "vh3_translation_patch";

    public TranslationPatchMod() {
        // 入口不引用客户端类；模块由客户端专属事件注册，避免服务端加载 I18n。
        if (FMLEnvironment.dist == Dist.CLIENT) {
            String version = ModLoadingContext.get().getActiveContainer().getModInfo().getVersion().toString();
            if (!version.equals(System.getProperty("vh3_translation_patch.transformer.ready"))) {
                throw new IllegalStateException("VH3 Translation Patch requires the matching transformer JAR. "
                        + "Install both files from the same build and check earlier preflight errors.");
            }
        }
    }
}
