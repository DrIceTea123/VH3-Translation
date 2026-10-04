package cn.vmct.installer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** 基础目录只在构建时读取，发布 JAR 自身携带完整 ZIP 与逐文件校验清单。 */
public final class Payload {
    @FunctionalInterface public interface Source { InputStream open() throws IOException; }
    private final Source zip;
    private final Map<String, String> hashes;

    public Payload(Source zip, Map<String, String> hashes) { this.zip = zip; this.hashes = Map.copyOf(hashes); }

    public static Payload embedded() throws IOException {
        Properties p = new Properties();
        try (var in = Config.resource("payload.properties")) { p.load(new InputStreamReader(in, StandardCharsets.UTF_8)); }
        Map<String, String> hashes = new TreeMap<>();
        for (String path : p.stringPropertyNames()) hashes.put(path, Config.hash(p.getProperty(path)));
        if (hashes.isEmpty()) throw new IOException("基础内容清单为空");
        return new Payload(() -> Config.resource("payload.zip"), hashes);
    }

    public Set<String> paths() { return hashes.keySet(); }

    public List<String> unpack(Path directory) throws IOException {
        Set<String> seen = new HashSet<>();
        List<String> directories = new ArrayList<>();
        try (var in = new ZipInputStream(zip.open(), StandardCharsets.UTF_8)) {
            for (ZipEntry entry; (entry = in.getNextEntry()) != null;) {
                String name = entry.getName();
                Path target = FilesEx.target(directory, name);
                if (!seen.add(name)) throw new IOException("基础压缩包条目重复：" + name);
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    directories.add(name);
                } else {
                    if (!hashes.containsKey(name)) throw new IOException("基础压缩包存在未登记的文件：" + name);
                    Files.createDirectories(target.getParent());
                    Files.copy(in, target);
                    if (!FilesEx.sha256(target).equals(hashes.get(name))) throw new IOException("内置基础文件损坏：" + name);
                }
                in.closeEntry();
            }
        }
        if (!seen.containsAll(hashes.keySet())) throw new IOException("基础压缩包缺少清单中的文件");
        return directories;
    }
}
