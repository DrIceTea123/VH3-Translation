package cn.vmct.installer;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.function.Consumer;
import java.util.jar.JarFile;

@FunctionalInterface
public interface Downloader {
    void download(Config.Mod mod, Path destination, Consumer<String> progress) throws IOException;

    static Downloader https(Texts texts) {
        return (mod, destination, progress) -> {
            IOException failure = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    progress.accept(texts.get("download.start", "name", mod.name(), "attempt", Integer.toString(attempt)));
                    fetch(mod.url(), destination, progress, texts);
                    verify(mod, destination);
                    return;
                } catch (IOException e) {
                    failure = e;
                    Files.deleteIfExists(destination);
                }
            }
            throw new IOException("下载失败：" + mod.name() + "\n" + failure.getMessage(), failure);
        };
    }

    static void verify(Config.Mod mod, Path file) throws IOException {
        if (!FilesEx.sha256(file).equals(mod.hash())) throw new IOException("下载文件 SHA-256 不符：" + mod.filename());
        try (JarFile jar = new JarFile(file.toFile())) {
            if (!jar.entries().hasMoreElements()) throw new IOException("下载的 JAR 为空：" + mod.filename());
        }
    }

    private static void fetch(URI original, Path target, Consumer<String> progress, Texts texts) throws IOException {
        URI uri = original;
        for (int redirects = 0; redirects <= 5; redirects++) {
            if (!"https".equalsIgnoreCase(uri.getScheme())) throw new IOException("拒绝非 HTTPS 下载或重定向");
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "VH3-Translation-Installer/1.0");
            try {
                int status = connection.getResponseCode();
                if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("重定向缺少地址");
                    uri = uri.resolve(location);
                    continue;
                }
                if (status != 200) throw new IOException("服务器返回 HTTP " + status);
                long length = connection.getContentLengthLong(), count = 0, last = 0;
                try (InputStream in = connection.getInputStream(); OutputStream out = Files.newOutputStream(target)) {
                    byte[] buf = new byte[65536];
                    for (int n; (n = in.read(buf)) != -1;) {
                        count += n;
                        if (count > 256L * 1024 * 1024) throw new IOException("下载文件超出 256 MiB 上限");
                        out.write(buf, 0, n);
                        if (System.nanoTime() - last > 500_000_000L) {
                            progress.accept(texts.get("download.progress", "received", Long.toString(count / 1024),
                                    "total", length > 0 ? Long.toString(length / 1024) : texts.get("download.unknown")));
                            last = System.nanoTime();
                        }
                    }
                }
                if (length >= 0 && count != length) throw new IOException("下载连接提前结束");
                return;
            } finally { connection.disconnect(); }
        }
        throw new IOException("下载重定向次数过多");
    }
}
