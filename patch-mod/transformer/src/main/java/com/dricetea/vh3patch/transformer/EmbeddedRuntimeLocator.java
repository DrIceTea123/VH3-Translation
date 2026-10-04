package com.dricetea.vh3patch.transformer;

import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileModLocator;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

/** SERVICE 层只定位运行侧，运行侧类由 Forge 在 GAME 层加载。 */
public final class EmbeddedRuntimeLocator extends AbstractJarFileModLocator {
    @Override public String name() { return "vh3_translation_patch_embedded_runtime"; }
    @Override public void initArguments(Map<String, ?> arguments) {}
    @Override public Stream<Path> scanCandidates() {
        try {
            return Stream.of(EmbeddedRuntime.resolve(FMLPaths.GAMEDIR.get()));
        } catch (Exception e) {
            throw new IllegalStateException("VTP embedded runtime discovery FAILED: " + e.getMessage(), e);
        }
    }
}
