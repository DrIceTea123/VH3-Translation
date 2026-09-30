package com.dricetea.vh3patch.module;

import com.dricetea.vh3patch.config.ModuleConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** 模块公共契约：默认以输入的英文文本查表，特殊模块只需覆盖 mappingKey。 */
public abstract class TranslationModule {
    private final String id;
    private final ModuleConfig config;

    protected TranslationModule(String id) {
        this.id = id;
        String resource = "/module-defaults/" + id + ".json";
        try (var stream = TranslationModule.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IOException("Missing module defaults: " + resource);
            config = new ModuleConfig(id, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot initialize module " + id, e);
        }
    }

    public final String id() { return id; }
    public final String configFileName() { return config.fileName(); }

    protected String mappingKey(String input) { return input; }

    /** 未配置返回 null，具体模块可继续使用游戏语言系统或原始返回值。 */
    public final String configuredTranslation(String input) { return config.get(mappingKey(input)); }

    public final void reload(Path configDirectory) throws IOException { config.reload(configDirectory); }
}
