package com.dricetea.vh3patch.module;

import com.dricetea.vh3patch.config.ModuleConfig;

import java.io.IOException;
import java.nio.file.Path;

/** 模块公共契约：默认以输入的英文文本查表，特殊模块只需覆盖 mappingKey。 */
public abstract class TranslationModule {
    private final String id;
    private final ModuleConfig config;

    protected TranslationModule(String id) {
        this.id = id;
        config = new ModuleConfig(id);
    }

    public final String id() { return id; }
    public final String configFileName() { return config.fileName(); }

    protected String mappingKey(String input) { return input; }

    /** 未配置返回 null，具体模块可继续使用游戏语言系统或原始返回值。 */
    public final String configuredTranslation(String input) { return config.get(mappingKey(input)); }

    public final void reload(Path configDirectory) throws IOException { config.reload(configDirectory); }

    /** 启动阶段没有可回退的译文；异常必须向 Forge 传播，阻止缺失配置时继续启动。 */
    public final void initialize(Path configDirectory) {
        try {
            reload(configDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("VTP 模块配置首次加载失败，请修复后重新启动："
                    + configDirectory.resolve(configFileName()), e);
        }
    }
}
