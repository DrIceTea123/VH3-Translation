package com.dricetea.vh3patch;

import com.dricetea.vh3patch.module.CommonModules;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(TranslationPatchMod.MOD_ID)
public final class TranslationPatchMod {
    public static final String MOD_ID = "vh3_translation_patch";

    public TranslationPatchMod() {
        // 两端均检查转换器，并仅初始化通用模块；客户端专属模块由客户端事件初始化。
            String version = ModLoadingContext.get().getActiveContainer().getModInfo().getVersion().toString();
            if (!version.equals(System.getProperty("vh3_translation_patch.transformer.ready"))) {
                throw new IllegalStateException("VH3 Translation Patch requires its bundled transformation service to complete preflight. "
                        + "Reinstall the single VTP JAR, remove old standalone VTP JARs and check earlier preflight errors.");
            }
        CommonModules.initialize(FMLPaths.CONFIGDIR.get().resolve(MOD_ID));
    }
}
