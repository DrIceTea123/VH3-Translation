import javax.tools.ToolProvider;
import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

/** JDK 17 单文件构建入口，无 Maven/Gradle/网络依赖。 */
class Build {
    public static void main(String[] args) throws Exception {
        List<String> options = List.of(args);
        int projectArg = options.indexOf("--project");
        Path project = (projectArg >= 0 ? Path.of(args[projectArg + 1]) : Path.of(".")).toAbsolutePath().normalize();
        boolean release = options.contains("--release");
        if (!Files.isDirectory(project.resolve("src/main/java"))) throw new IOException("请从 installer 目录执行，或传入 --project。");
        Path build = project.resolve("build"); Files.createDirectories(build);
        try (var channel = FileChannel.open(build.resolve("build.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.tryLock()) {
            if (lock == null) throw new IOException("另一个打包任务正在运行。");
            run(project, build, release);
        }
    }

    private static void run(Path project, Path build, boolean release) throws Exception {
        Path versionFile = project.resolve("version.properties");
        Properties versions = read(versionFile);
        String version = versions.getProperty("next.version", "");
        if (!version.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) throw new IOException("next.version 必须是4段数字");
        String[] parts = version.split("\\.");
        String nextVersion = parts[0] + "." + parts[1] + "." + parts[2] + "." + Math.addExact(Integer.parseInt(parts[3]), 1);
        Path output = project.getParent().getParent().resolve("[发布文件]/宝藏猎人3汉化安装器-VM汉化组-V" + version + ".jar");
        if (release && Files.exists(output)) throw new IOException("成品已存在，请检查 version.properties：" + output);
        Path work = Files.createTempDirectory(build, "run-");
        try {
            Path classes = Files.createDirectory(work.resolve("classes"));
            compile(project.resolve("src/main/java"), classes, null);
            copyTree(project.resolve("resources"), classes);
            Properties config = read(classes.resolve("installer.properties"));
            config.setProperty("app.version", version);
            write(config, classes.resolve("installer.properties"));
            Path source = project.getParent().resolve("program/汉化包内容");
            packPayload(source, classes);
            Path tests = Files.createDirectory(work.resolve("tests"));
            compile(project.resolve("src/test/java"), tests, classes.toString());
            String classpath = classes + File.pathSeparator + tests;
            command(java(), "-Dfile.encoding=UTF-8", "-Djava.awt.headless=true", "-cp", classpath, "cn.vmct.installer.InstallerTest", project.toString());

            if (!release) { System.out.println("检查通过；未生成发布成品，版本不递增。"); return; }
            Path candidate = work.resolve(output.getFileName());
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
            manifest.getMainAttributes().putValue("Main-Class", "cn.vmct.installer.Main");
            manifest.getMainAttributes().putValue("Implementation-Version", version);
            try (var jar = new JarOutputStream(Files.newOutputStream(candidate), manifest); var files = Files.walk(classes)) {
                for (Path path : files.filter(Files::isRegularFile).sorted().toList()) {
                    String name = classes.relativize(path).toString().replace('\\', '/');
                    JarEntry entry = new JarEntry(name); entry.setTime(0); jar.putNextEntry(entry);
                    Files.copy(path, jar); jar.closeEntry();
                }
            }
            command(java(), "-Dfile.encoding=UTF-8", "-Djava.awt.headless=true", "-jar", candidate.toString(), "--check");
            Files.createDirectories(output.getParent());
            // 两个文件不能跨文件原子提交：版本保存失败时撤销本次新成品，保证重试语义清楚。
            Files.copy(candidate, output);
            try {
                Properties updated = new Properties(); updated.putAll(versions);
                updated.setProperty("last.version", version); updated.setProperty("next.version", nextVersion);
                Path temp = Files.createTempFile(project, "version-", ".tmp");
                try { write(updated, temp); Files.move(temp, versionFile, StandardCopyOption.REPLACE_EXISTING); }
                finally { Files.deleteIfExists(temp); }
            } catch (Exception e) { Files.deleteIfExists(output); throw e; }
            System.out.println("已生成 V" + version + "：" + output);
            System.out.println("SHA-256: " + sha256(output));
            System.out.println("下次生成版本：V" + nextVersion);
        } finally { delete(work, build); }
    }

    private static void compile(Path source, Path out, String classpath) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IOException("需要 JDK 17 或更新版本，不能使用仅运行环境 JRE。");
        List<String> args = new ArrayList<>(List.of("--release", "17", "-encoding", "UTF-8", "-d", out.toString()));
        if (classpath != null) args.addAll(List.of("-classpath", classpath));
        try (var files = Files.walk(source)) { files.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> args.add(p.toString())); }
        if (compiler.run(null, null, null, args.toArray(String[]::new)) != 0) throw new IOException("编译失败，版本未递增。");
    }

    private static void packPayload(Path source, Path classes) throws Exception {
        if (!Files.isDirectory(source)) throw new IOException("缺少基础内容目录：" + source);
        List<Path> paths;
        try (var files = Files.walk(source)) { paths = files.sorted().toList(); }
        long vtps = paths.stream().filter(p -> p.getParent().equals(source.resolve("mods"))
                && p.getFileName().toString().matches("vh3_translation_patch-[0-9]+\\.[0-9]+\\.[0-9]+\\.jar")).count();
        if (vtps != 1) throw new IOException("基础内容 mods 必须包含一个正式 VTP 单包");
        Properties hashes = new Properties();
        try (var zip = new ZipOutputStream(Files.newOutputStream(classes.resolve("payload.zip")), StandardCharsets.UTF_8)) {
            for (Path path : paths) {
                if (path.equals(source)) continue;
                var attrs = Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (attrs.isSymbolicLink() || attrs.isOther()) throw new IOException("基础内容中不允许链接：" + path);
                String name = source.relativize(path).toString().replace('\\', '/');
                if (name.equals("vaultpatcher/cache") || name.startsWith("vaultpatcher/cache/")) throw new IOException("基础内容中不能打包 VP 缓存");
                if (name.matches("mods/vh3_translation_patch-transformer-.*\\.jar")) throw new IOException("基础内容残留旧 VTP 双包");
                if (attrs.isDirectory()) name += "/";
                ZipEntry entry = new ZipEntry(name); entry.setTime(0); zip.putNextEntry(entry);
                if (attrs.isRegularFile()) { Files.copy(path, zip); hashes.setProperty(name, sha256(path)); }
                zip.closeEntry();
            }
        }
        write(hashes, classes.resolve("payload.properties"));
        System.out.println("内置基础文件：" + hashes.size());
    }

    private static void copyTree(Path source, Path target) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path p : paths.toList()) {
                Path dest = target.resolve(source.relativize(p));
                if (Files.isDirectory(p)) Files.createDirectories(dest); else Files.copy(p, dest);
            }
        }
    }
    private static Properties read(Path path) throws IOException {
        Properties p = new Properties();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { p.load(reader); }
        return p;
    }
    private static void write(Properties p, Path path) throws IOException {
        try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) { p.store(writer, "UTF-8 / generated by installer build"); }
    }
    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var in = Files.newInputStream(path)) {
            byte[] bytes = new byte[65536]; for (int n; (n = in.read(bytes)) != -1;) digest.update(bytes, 0, n);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    private static String java() { return Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString(); }
    private static void command(String... command) throws Exception {
        if (new ProcessBuilder(command).inheritIO().start().waitFor() != 0) throw new IOException("验证失败，版本未递增。");
    }
    private static void delete(Path directory, Path allowed) throws IOException {
        if (!directory.toAbsolutePath().normalize().startsWith(allowed.toAbsolutePath().normalize()) || directory.equals(allowed)) throw new IOException("清理目录越界");
        try (var paths = Files.walk(directory)) { for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(p); }
    }
}
