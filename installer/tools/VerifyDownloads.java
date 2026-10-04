import cn.vmct.installer.*;
import java.nio.file.*;

/** 对一个已生成的安装器执行实际直链下载验证，不接触任何游戏目录。 */
class VerifyDownloads {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("请指定隔离下载目录");
        Path destination = Path.of(args[0]).toAbsolutePath(); Files.createDirectories(destination);
        Config config = Config.load(); Texts texts = new Texts(config);
        for (Config.Mod mod : config.mods()) {
            Path file = destination.resolve(mod.filename());
            Downloader.https(texts).download(mod, file, System.out::println);
            System.out.println("OK " + mod.filename() + " " + mod.hash());
        }
    }
}
