import cpw.mods.cl.JarModuleFinder;
import cpw.mods.cl.ModuleClassLoader;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.*;
import net.minecraftforge.fml.loading.*;
import net.minecraftforge.forgespi.locating.*;
import java.lang.module.*;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;

/** 使用生产 JAR、真实 Forge 扫描器和 ModLauncher 的模块加载器；不启动 Minecraft。 */
public class BundleProbe {
    public static void main(String[] args) throws Exception {
        Path game = Path.of(args[0]).toAbsolutePath();
        Path bundle = Path.of(args[1]).toAbsolutePath();
        String version = args[2];
        new TypesafeMap(IEnvironment.class);
        FMLPaths.loadAbsolutePaths(game);
        var discovered = new ModDirTransformerDiscoverer().candidates(game);
        if (!ModDirTransformerDiscoverer.allExcluded().contains(bundle)) throw new AssertionError("bundle not discovered early");
        var service = layer("SERVICE", List.of(ModuleLayer.boot()), SecureJar.from(bundle));
        var transformer = ServiceLoader.load(service, ITransformationService.class).stream()
                .filter(p -> p.type().getName().equals("com.dricetea.vh3patch.transformer.TranslationTransformationService"))
                .map(ServiceLoader.Provider::get).findFirst().orElseThrow();
        var locator = ServiceLoader.load(service, IModLocator.class).stream()
                .filter(p -> p.type().getName().equals("com.dricetea.vh3patch.transformer.EmbeddedRuntimeLocator"))
                .map(ServiceLoader.Provider::get).findFirst().orElseThrow();
        if (transformer.getClass().getModule().getLayer() != service || locator.getClass().getModule().getLayer() != service)
            throw new AssertionError("providers did not load in SERVICE layer");
        // 独立探针没有 FML 启动器，仅提供 mods.toml 解析所需的版本元数据。
        var field = FMLLoader.class.getDeclaredField("versionInfo"); field.setAccessible(true);
        field.set(null, new VersionInfo("40.3.11", "1.18.2", "20220404.173914", "net.minecraftforge"));
        locator.initArguments(Map.of());
        var mods = locator.scanMods(); // 特意先定位、后 initialize，证明不依赖转换服务初始化顺序。
        if (mods.size() != 1 || mods.get(0).getType() != IModFile.Type.MOD) throw new AssertionError("runtime not discovered");
        var mod = mods.get(0);
        if (!mod.getModInfos().get(0).getModId().equals("vh3_translation_patch")
                || !mod.getModInfos().get(0).getVersion().toString().equals(version)) throw new AssertionError("wrong runtime metadata");
        for (String launch : List.of("forgeclient", "forgeserver")) {
            IEnvironment environment = (IEnvironment) Proxy.newProxyInstance(IEnvironment.class.getClassLoader(), new Class[]{IEnvironment.class}, (proxy, method, values) -> {
                if (method.getName().equals("getProperty")) {
                    if (values[0] == IEnvironment.Keys.GAMEDIR.get()) return Optional.of(game);
                    if (values[0] == IEnvironment.Keys.LAUNCHTARGET.get()) return Optional.of(launch);
                    return Optional.empty();
                }
                throw new UnsupportedOperationException(method.toString());
            });
            transformer.onLoad(environment, Set.of("vh3_translation_patch"));
            transformer.initialize(environment);
            if (!version.equals(System.getProperty("vh3_translation_patch.transformer.ready"))) throw new AssertionError("preflight not ready");
            if (transformer.transformers().isEmpty()) throw new AssertionError("no transformers");
            System.out.println(launch + ": preflight and transformer registration passed (" + transformer.transformers().size() + " classes)");
        }
        var gameLayer = layer("GAME", List.of(service), mod.getSecureJar());
        Class<?> runtime = gameLayer.findLoader(mod.getSecureJar().name()).loadClass("com.dricetea.vh3patch.TranslationPatchMod");
        if (runtime.getModule().getLayer() != gameLayer) throw new AssertionError("runtime is not in GAME layer");
        try {
            service.findLoader("com.dricetea.vh3patch.transformer").loadClass(runtime.getName());
            throw new AssertionError("runtime leaked into SERVICE layer");
        } catch (ClassNotFoundException expected) {}
        System.out.println("SERVICE SPI discovery, Forge MOD metadata and isolated GAME class loading passed; Minecraft lifecycle not executed");
    }

    private static ModuleLayer layer(String name, List<ModuleLayer> parents, SecureJar jar) {
        var configuration = Configuration.resolveAndBind(JarModuleFinder.of(jar), parents.stream().map(ModuleLayer::configuration).toList(), ModuleFinder.of(), List.of(jar.name()));
        var loader = new ModuleClassLoader("VTP-PROBE-" + name, configuration, parents);
        loader.setFallbackClassLoader(ClassLoader.getSystemClassLoader());
        return ModuleLayer.defineModules(configuration, parents, ignored -> loader).layer();
    }
}
