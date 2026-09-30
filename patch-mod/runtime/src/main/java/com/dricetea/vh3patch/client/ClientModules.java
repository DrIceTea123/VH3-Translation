package com.dricetea.vh3patch.client;

import com.dricetea.vh3patch.TranslationPatchMod;
import com.dricetea.vh3patch.module.TranslationModule;
import com.dricetea.vh3patch.modules.CombatStatsModule;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.List;

/** 只在客户端注册模块及资源重载监听，专用服务端不会加载模块里的 I18n。 */
@Mod.EventBusSubscriber(modid = TranslationPatchMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModules {
    // 添加运行侧模块时只扩充此表；配置生成、重载与报错由公共层统一处理。
    private static final List<TranslationModule> MODULES = List.of(CombatStatsModule.INSTANCE);
    private ClientModules() {}

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        Path directory = FMLPaths.CONFIGDIR.get().resolve(TranslationPatchMod.MOD_ID);
        reloadAll(directory);
        event.registerReloadListener((ResourceManagerReloadListener) resources -> reloadAll(directory));
    }

    private static void reloadAll(Path directory) {
        for (TranslationModule module : MODULES) {
            try {
                module.reload(directory);
            } catch (Exception e) {
                // 一个文件损坏不能丢掉它的上一份有效快照，也不能阻止其他模块重载。
                System.getLogger(TranslationPatchMod.MOD_ID).log(System.Logger.Level.ERROR,
                        "模块配置重载失败，保留上次有效配置（首次加载使用内置默认值）："
                                + directory.resolve(module.configFileName()), e);
            }
        }
    }
}
