package com.dricetea.vh3patch.transformer;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** 每个被接管内容对应一个模块；不同签名或注入策略可自行实现此接口。 */
public interface PatchModule {
    PatchSpec spec();
    default List<PatchSpec> specs() { return List.of(spec()); }
    default List<PatchSpec> specs(boolean client) { return specs().stream().filter(s -> s.enabled(client)).toList(); }
    MethodNode target(ClassNode node);
    void apply(ClassNode node);
    default void apply(ClassNode node, boolean client) { apply(node); }
    // 每个模块自行识别旧 VP 定位方式，公共层只遍历文件和执行迁移。
    boolean ownsVpRule(JsonObject rule);
    default void validateVpMigration(List<JsonObject> rules) {
        if (rules.size() > 1) throw new IllegalStateException("Duplicate VP ownership groups: " + spec().moduleId());
    }
    default void importMappings(Path vpSource, Path targetJar, Path output) throws IOException {
        throw new UnsupportedOperationException("No importer for module: " + spec().moduleId());
    }
}
