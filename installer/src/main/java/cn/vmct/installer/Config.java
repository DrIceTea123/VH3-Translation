package cn.vmct.installer;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** 可编辑发布配置；运行侧拒绝未固定的下载或错误的文件名。 */
public record Config(String exportSerial, String translationVersion, String packVersion,
                     String vaultVersion, String vaultFilename, String vaultHash, List<Mod> mods) {
    public record Mod(String id, String name, boolean required, String filename, URI url, String hash) {}

    public static Config load() throws IOException {
        Properties p = new Properties();
        try (var in = resource("installer.properties")) {
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        List<Mod> mods = new ArrayList<>();
        for (String id : value(p, "mods").split(",")) {
            String file = filename(value(p, id + ".filename"));
            URI url = URI.create(value(p, id + ".url"));
            if (!"https".equals(url.getScheme()) || url.getHost() == null)
                throw new IOException("下载地址必须使用 HTTPS：" + id);
            mods.add(new Mod(id, value(p, id + ".name"),
                    Boolean.parseBoolean(value(p, id + ".required")), file, url,
                    hash(value(p, id + ".sha256"))));
        }
        if (mods.stream().map(Mod::filename).distinct().count() != mods.size())
            throw new IOException("模组文件名重复");
        for (String id : List.of("vp", "i18n")) {
            if (mods.stream().noneMatch(m -> m.id().equals(id) && m.required()))
                throw new IOException(id + " 必须设为必装");
        }
        String serial = value(p, "export.serial");
        if (!serial.matches("[1-9][0-9]*")) throw new IOException("export.serial 必须是正整数");
        return new Config(serial, value(p, "translation.version"), value(p, "pack.version"), value(p, "vault.version"),
                filename(value(p, "vault.filename")), hash(value(p, "vault.sha256")), List.copyOf(mods));
    }

    public List<Mod> selected(Set<String> selected) {
        return mods.stream().filter(m -> selected.contains(m.id())).toList();
    }

    static String filename(String name) throws IOException {
        if (!name.matches("[a-zA-Z0-9_+.\\-]+\\.jar")) throw new IOException("非法 JAR 文件名：" + name);
        return name;
    }

    static String hash(String value) throws IOException {
        if (!value.matches("[a-fA-F0-9]{64}")) throw new IOException("必须填写完整 SHA-256");
        return value.toLowerCase(Locale.ROOT);
    }

    private static String value(Properties p, String key) throws IOException {
        String value = p.getProperty(key, "").trim();
        if (value.isEmpty()) throw new IOException("缺少配置：" + key);
        return value;
    }

    static InputStream resource(String name) throws IOException {
        InputStream in = Config.class.getResourceAsStream("/" + name);
        if (in != null) return in;
        // 目录名以 ! 结尾时 jar: URL 的 !/ 分隔符可能截错位置；按真实代码来源读取同一 JAR。
        try {
            var location = java.nio.file.Path.of(Config.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (java.nio.file.Files.isRegularFile(location)) {
                var jar = new java.util.jar.JarFile(location.toFile());
                try {
                    var entry = jar.getJarEntry(name);
                    if (entry != null) return new FilterInputStream(jar.getInputStream(entry)) {
                        @Override public void close() throws IOException { try { super.close(); } finally { jar.close(); } }
                    };
                } catch (IOException | RuntimeException failure) { jar.close(); throw failure; }
                jar.close();
            }
        } catch (java.net.URISyntaxException invalid) { throw new IOException("安装器所在路径无效", invalid); }
        throw new IOException("安装器缺少内置资源：" + name);
    }
}
