import net.minecraftforge.fml.loading.ModDirTransformerDiscoverer;
import net.minecraftforge.fml.loading.moddiscovery.ModsFolderLocator;
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileModLocator;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/** 只在 build/merge-assessment 中调用真实 Forge 扫描器，不启动游戏或修改安装输入。 */
public class JarLayoutProbe {
    public static void main(String[] args) throws Exception {
        Path root=Path.of(args[0]).toAbsolutePath().normalize();
        for(String scenario:List.of("pair","flat","runtime-outer","transformer-outer")) {
            Path game=root.resolve(scenario);
            new ModDirTransformerDiscoverer().candidates(game);
            var constructor=ModsFolderLocator.class.getDeclaredConstructor(Path.class);constructor.setAccessible(true);
            var locator=constructor.newInstance(game.resolve("mods"));
            var candidates=locator.scanCandidates().map(p->p.getFileName().toString()).toList();
            var early=ModDirTransformerDiscoverer.allExcluded().stream().filter(p->p.startsWith(game)).map(p->p.getFileName().toString()).toList();
            System.out.println(scenario+": early="+early+", normal="+candidates);
            if(scenario.equals("pair") && (!early.equals(List.of("transformer.jar")) || !candidates.equals(List.of("runtime.jar"))))throw new AssertionError(scenario);
            if(scenario.equals("flat") && (!early.equals(List.of("flat.jar")) || !candidates.isEmpty()))throw new AssertionError(scenario);
            if(scenario.equals("runtime-outer") && (!early.isEmpty() || !candidates.equals(List.of("runtime-outer.jar"))))throw new AssertionError(scenario);
            if(scenario.equals("transformer-outer") && (!early.equals(List.of("transformer-outer.jar")) || !candidates.isEmpty()))throw new AssertionError(scenario);
        }
        // 专用定位器可把独立运行侧候选交给 Forge；不测试服务发现或完整 @Mod 生命周期。
        // 独立进程没有启动 FML：仅补充 mods.toml 字符串替换所需的版本夹具。
        var versionField=net.minecraftforge.fml.loading.FMLLoader.class.getDeclaredField("versionInfo");
        versionField.setAccessible(true);
        versionField.set(null,new net.minecraftforge.fml.loading.VersionInfo("40.3.11","1.18.2","20220404.173914","net.minecraftforge"));
        var locator=new NestedRuntimeProbe(root.resolve("extracted/runtime.jar"));
        var mods=locator.scanMods();
        if(mods.size()!=1 || mods.get(0).getType()!=net.minecraftforge.forgespi.locating.IModFile.Type.MOD)
            throw new AssertionError("nested runtime not recognized as MOD");
        if(!Files.exists(mods.get(0).findResource("com/dricetea/vh3patch/modules/QuestNamesModule.class")))throw new AssertionError("helper absent");
        System.out.println("explicit-locator: recognized MOD, modId="+mods.get(0).getModInfos().get(0).getModId()+", runtime helper present=true");
    }
    public static class NestedRuntimeProbe extends AbstractJarFileModLocator {
        private final Path jar;
        public NestedRuntimeProbe(Path jar){this.jar=jar;}
        @Override public String name(){return "assessment-only";}
        @Override public Stream<Path> scanCandidates(){return Stream.of(jar);}
        @Override public void initArguments(Map<String,?> args){}
    }
}
