package cn.vmct.installer;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class RootValidator {
    public record Result(Path root, List<String> problems) {
        public boolean valid() { return problems.isEmpty(); }
        public String message() { return String.join("\n", problems); }
    }

    public static Result inspect(Path selected, Config config) throws IOException {
        if (!Files.isDirectory(selected)) throw new IOException("请选择整合包根目录文件夹。");
        Path root = selected.toRealPath();
        Path mods = FilesEx.target(root, "mods");
        List<String> problems = new ArrayList<>();
        if (!Files.isDirectory(mods)) {
            problems.add("根目录文件夹验证失败，选择的目录中未检测到宝藏猎人整合包。");
            return new Result(root, List.copyOf(problems));
        }
        List<Path> vaults;
        try (var files = Files.list(mods)) {
            vaults = files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).matches("the_vault.*\\.jar"))
                    .sorted().toList();
        }
        if (vaults.size() != 1) problems.add("整合包校验失败，发现" + vaults.size() + "个不同的宝藏猎人核心模组文件。");
        Path expected = FilesEx.target(root, "mods/" + config.vaultFilename());
        if (!Files.isRegularFile(expected)) {
            problems.add("整合包校验失败，宝藏猎人核心模组版本应为" + config.vaultFilename() + "。可能是整合包版本与当前汉化包不符。");
        } else if (!FilesEx.sha256(expected).equals(config.vaultHash())) {
            problems.add("宝藏猎人核心模组校验失败，可能是模组文件损坏或已修改。");
        }
        return new Result(root, List.copyOf(problems));
    }
}
