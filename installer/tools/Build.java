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
        if (!Files.isDirectory(project.resolve("src/main/java"))) throw new IOException("\u8bf7\u4ece installer \u76ee\u5f55\u6267\u884c\uff0c\u6216\u4f20\u5165 --project\u3002");
        Path build = project.resolve("build"); Files.createDirectories(build);
        try (var channel = FileChannel.open(build.resolve("build.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.tryLock()) {
            if (lock == null) throw new IOException("\u53e6\u4e00\u4e2a\u6253\u5305\u4efb\u52a1\u6b63\u5728\u8fd0\u884c\u3002");
            run(project, build, release);
        }
    }

    private static void run(Path project, Path build, boolean release) throws Exception {
        Path serialFile = project.resolve("export.properties");
        Properties serials = read(serialFile);
        String serial = serials.getProperty("next.serial", "");
        if (!serial.matches("[1-9][0-9]*")) throw new IOException("next.serial \u5fc5\u987b\u662f\u6b63\u6574\u6570");
        String nextSerial = Long.toString(Math.addExact(Long.parseLong(serial), 1));
        Properties sourceConfig = read(project.resolve("resources/installer.properties"));
        String filename = outputName(sourceConfig, serial);
        Path output = project.getParent().getParent().resolve("[\u53d1\u5e03\u6587\u4ef6]").resolve(filename);
        if (release && Files.exists(output)) throw new IOException("\u6210\u54c1\u5df2\u5b58\u5728\uff0c\u8bf7\u68c0\u67e5 export.properties\uff1a" + output);
        Path work = Files.createTempDirectory(build, "run-");
        try {
            Path classes = Files.createDirectory(work.resolve("classes"));
            compile(project.resolve("src/main/java"), classes, null);
            copyTree(project.resolve("resources"), classes);
            Properties config = read(classes.resolve("installer.properties"));
            config.setProperty("export.serial", serial);
            write(config, classes.resolve("installer.properties"));
            Path source = project.getParent().resolve("translate-packs");
            // 图标唯一来源；直接内置供窗口和首页使用，不维护第二份资源。
            Files.copy(source.resolve("icon.png"), classes.resolve("icon.png"), StandardCopyOption.REPLACE_EXISTING);
            packPayload(source, classes);
            Path tests = Files.createDirectory(work.resolve("tests"));
            compile(project.resolve("src/test/java"), tests, classes.toString());
            compile(project.resolve("tools"), tests, classes.toString());
            String classpath = classes + File.pathSeparator + tests;
            command(java(), "-Dfile.encoding=UTF-8", "-cp", classpath, "BuildTest", project.toString());
            command(java(), "-Dfile.encoding=UTF-8", "-Djava.awt.headless=true", "-cp", classpath, "cn.vmct.installer.InstallerTest", project.toString());

            if (!release) { System.out.println("\u68c0\u67e5\u901a\u8fc7\uff1b\u672a\u751f\u6210\u53d1\u5e03\u6210\u54c1\uff0c\u5e8f\u5217\u53f7\u4e0d\u9012\u589e\u3002"); return; }
            Path candidate = work.resolve(output.getFileName());
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
            manifest.getMainAttributes().putValue("Main-Class", "cn.vmct.installer.Main");
            manifest.getMainAttributes().putValue("Translation-Version", config.getProperty("translation.version"));
            manifest.getMainAttributes().putValue("Export-Serial", serial);
            try (var jar = new JarOutputStream(Files.newOutputStream(candidate), manifest); var files = Files.walk(classes)) {
                for (Path path : files.filter(Files::isRegularFile).sorted().toList()) {
                    String name = classes.relativize(path).toString().replace('\\', '/');
                    JarEntry entry = new JarEntry(name); entry.setTime(0); jar.putNextEntry(entry);
                    Files.copy(path, jar); jar.closeEntry();
                }
            }
            command(java(), "-Dfile.encoding=UTF-8", "-Djava.awt.headless=true", "-jar", candidate.toString(), "--check");
            Files.createDirectories(output.getParent());
            // 成品全部生成成功才更新序列；后续失败只撤销本轮新成品。
            boolean copiedJar = false;
            Map<Path, byte[]> previousLaunchers = new LinkedHashMap<>();
            try {
                Files.copy(candidate, output); copiedJar = true;
                for (var entry : launcherFiles(project.resolve("launchers")).entrySet()) {
                    Path path = output.getParent().resolve(entry.getKey());
                    previousLaunchers.put(path, Files.exists(path) ? Files.readAllBytes(path) : null);
                    Files.write(path, entry.getValue());
                    if (!entry.getKey().endsWith(".cmd") && Files.getFileStore(path).supportsFileAttributeView("posix"))
                        Files.setPosixFilePermissions(path, java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));
                }
                Properties updated = new Properties(); updated.putAll(serials);
                updated.setProperty("last.serial", serial); updated.setProperty("next.serial", nextSerial);
                Path temp = Files.createTempFile(project, "serial-", ".tmp");
                try { write(updated, temp); Files.move(temp, serialFile, StandardCopyOption.REPLACE_EXISTING); }
                finally { Files.deleteIfExists(temp); }
            } catch (Exception e) {
                for (var entry : previousLaunchers.entrySet()) {
                    try {
                        if (entry.getValue() == null) Files.deleteIfExists(entry.getKey());
                        else Files.write(entry.getKey(), entry.getValue());
                    } catch (IOException restore) { e.addSuppressed(restore); }
                }
                if (copiedJar) Files.deleteIfExists(output);
                throw e;
            }
            System.out.println("\u5df2\u751f\u6210\uff0c\u5bfc\u51fa\u5e8f\u5217\u53f7 " + serial + "\uff1a" + output);
            System.out.println("SHA-256: " + sha256(output));
            System.out.println("\u4e0b\u6b21\u5bfc\u51fa\u5e8f\u5217\u53f7\uff1a" + nextSerial);
        } finally { delete(work, build); }
    }

    static Map<String, byte[]> launcherFiles(Path scripts) throws IOException {
        Map<String, byte[]> result = new LinkedHashMap<>();
        for (String name : List.of("windows系统点我启动.cmd", "macOS系统点我启动.command", "Linux系统点我启动.sh")) {
            String source = name.endsWith(".cmd") ? name : "unix.sh";
            String content = Files.readString(scripts.resolve(source), StandardCharsets.UTF_8).replace("\r\n", "\n").replace("\r", "\n");
            if (name.endsWith(".cmd")) content = content.replace("\n", "\r\n");
            result.put(name, content.getBytes(StandardCharsets.UTF_8));
        }
        return result;
    }

    static String outputName(Properties config, String serial) throws IOException {
        String name = config.getProperty("export.filename", "");
        for (var entry : Map.of("modpackVersion", "pack.version", "translationVersion", "translation.version").entrySet()) {
            String value = config.getProperty(entry.getValue(), "").trim();
            if (value.isEmpty()) throw new IOException("\u7f3a\u5c11\u914d\u7f6e\uff1a" + entry.getValue());
            name = name.replace("{" + entry.getKey() + "}", value);
        }
        name = name.replace("{exportSerial}", serial);
        // 只接受单个跨平台文件名，模板不能逃逸发布目录。
        if (name.isBlank() || !name.endsWith(".jar") || name.matches(".*[\\\\/:*?\"<>|{}\\p{Cntrl}].*") || name.endsWith(". "))
            throw new IOException("export.filename \u5fc5\u987b\u662f\u5e26 .jar \u540e\u7f00\u7684\u5355\u4e2a\u6587\u4ef6\u540d\uff0c\u4e14\u4e0d\u80fd\u542b\u672a\u77e5\u5360\u4f4d\u7b26\uff1a" + name);
        return name;
    }

    private static void compile(Path source, Path out, String classpath) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IOException("\u9700\u8981 JDK 17 \u6216\u66f4\u65b0\u7248\u672c\uff0c\u4e0d\u80fd\u4f7f\u7528\u4ec5\u8fd0\u884c\u73af\u5883 JRE\u3002");
        List<String> args = new ArrayList<>(List.of("--release", "17", "-encoding", "UTF-8", "-d", out.toString()));
        if (classpath != null) args.addAll(List.of("-classpath", classpath));
        try (var files = Files.walk(source)) { files.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> args.add(p.toString())); }
        if (compiler.run(null, null, null, args.toArray(String[]::new)) != 0) throw new IOException("\u7f16\u8bd1\u5931\u8d25\uff0c\u5e8f\u5217\u53f7\u672a\u9012\u589e\u3002");
    }

    private static void packPayload(Path source, Path classes) throws Exception {
        if (!Files.isDirectory(source)) throw new IOException("\u7f3a\u5c11\u57fa\u7840\u5185\u5bb9\u76ee\u5f55\uff1a" + source);
        List<Path> paths;
        try (var files = Files.walk(source)) { paths = files.sorted().toList(); }
        long vtps = paths.stream().filter(p -> p.getParent().equals(source.resolve("mods"))
                && p.getFileName().toString().matches("vh3_translation_patch-[0-9]+\\.[0-9]+\\.[0-9]+\\.jar")).count();
        if (vtps != 1) throw new IOException("\u57fa\u7840\u5185\u5bb9 mods \u5fc5\u987b\u5305\u542b\u4e00\u4e2a\u6b63\u5f0f VTP \u5355\u5305");
        Properties hashes = new Properties();
        try (var zip = new ZipOutputStream(Files.newOutputStream(classes.resolve("payload.zip")), StandardCharsets.UTF_8)) {
            for (Path path : paths) {
                if (path.equals(source)) continue;
                var attrs = Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (attrs.isSymbolicLink() || attrs.isOther()) throw new IOException("\u57fa\u7840\u5185\u5bb9\u4e2d\u4e0d\u5141\u8bb8\u94fe\u63a5\uff1a" + path);
                String name = source.relativize(path).toString().replace('\\', '/');
                if (name.equals("vaultpatcher/cache") || name.startsWith("vaultpatcher/cache/")) throw new IOException("\u57fa\u7840\u5185\u5bb9\u4e2d\u4e0d\u80fd\u6253\u5305 VP \u7f13\u5b58");
                if (name.matches("mods/vh3_translation_patch-transformer-.*\\.jar")) throw new IOException("\u57fa\u7840\u5185\u5bb9\u6b8b\u7559\u65e7 VTP \u53cc\u5305");
                if (attrs.isDirectory()) name += "/";
                ZipEntry entry = new ZipEntry(name); entry.setTime(0); zip.putNextEntry(entry);
                if (attrs.isRegularFile()) { Files.copy(path, zip); hashes.setProperty(name, sha256(path)); }
                zip.closeEntry();
            }
        }
        write(hashes, classes.resolve("payload.properties"));
        System.out.println("\u5185\u7f6e\u57fa\u7840\u6587\u4ef6\uff1a" + hashes.size());
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
        // JDK 17 的控制台编码可能覆盖 file.encoding；同时设置新旧 JDK 的标准流编码。
        List<String> utf8 = new ArrayList<>(List.of(command));
        utf8.addAll(1, List.of("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8",
                "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8"));
        if (new ProcessBuilder(utf8).inheritIO().start().waitFor() != 0) throw new IOException("\u9a8c\u8bc1\u5931\u8d25\uff0c\u5e8f\u5217\u53f7\u672a\u9012\u589e\u3002");
    }
    private static void delete(Path directory, Path allowed) throws IOException {
        if (!directory.toAbsolutePath().normalize().startsWith(allowed.toAbsolutePath().normalize()) || directory.equals(allowed)) throw new IOException("\u6e05\u7406\u76ee\u5f55\u8d8a\u754c");
        try (var paths = Files.walk(directory)) { for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(p); }
    }
}
