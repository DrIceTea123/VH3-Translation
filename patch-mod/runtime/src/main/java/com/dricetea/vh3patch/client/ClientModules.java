package com.dricetea.vh3patch.client;

import com.dricetea.vh3patch.TranslationPatchMod;
import com.dricetea.vh3patch.module.TranslationModule;
import com.dricetea.vh3patch.modules.BestiaryGroupsModule;
import com.dricetea.vh3patch.modules.OverworldNamesModule;
import com.dricetea.vh3patch.modules.RoomNamesModule;
import com.dricetea.vh3patch.modules.CrystalStatsModule;
import com.dricetea.vh3patch.module.CommonModules;
import com.dricetea.vh3patch.modules.MobNamesModule;
import com.dricetea.vh3patch.modules.SoundNamesModule;

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
    // 添加运行侧模块时只扩充此表；外部配置读取、重载与报错由公共层统一处理。
    private static final List<TranslationModule> MODULES = List.of(BestiaryGroupsModule.INSTANCE, OverworldNamesModule.INSTANCE, RoomNamesModule.INSTANCE, CrystalStatsModule.INSTANCE, MobNamesModule.INSTANCE, SoundNamesModule.INSTANCE);
    private ClientModules() {}

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        Path directory = FMLPaths.CONFIGDIR.get().resolve(TranslationPatchMod.MOD_ID);
        // 首次加载不吞掉异常；F3+T 则允许退回每个模块各自的有效快照。
        for (TranslationModule module : MODULES) module.initialize(directory);
        event.registerReloadListener((ResourceManagerReloadListener) resources -> reloadAll(directory));
    }

    private static void reloadAll(Path directory) {
        // 客户端 F3+T 同时更新通用模块；专用服务端无资源重载事件，修改后重启生效。
        for (TranslationModule module : java.util.stream.Stream.concat(MODULES.stream(), CommonModules.all().stream()).toList()) {
            try {
                module.reload(directory);
            } catch (Exception e) {
                // 一个文件损坏不能丢掉它的上一份有效快照，也不能阻止其他模块重载。
                System.getLogger(TranslationPatchMod.MOD_ID).log(System.Logger.Level.ERROR,
                        "模块配置重载失败，保留上次有效配置，请修复文件后再次按 F3+T："
                                + directory.resolve(module.configFileName()), e);
            }
        }
    }
}
