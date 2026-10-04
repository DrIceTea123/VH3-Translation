package cn.vmct.installer;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** 不依赖界面的安装过程，所有写入均限定在选定的真实根目录内。 */
public final class InstallerEngine {
    private final Config config;
    private final Payload payload;
    private final Downloader downloader;

    public InstallerEngine(Config config, Payload payload, Downloader downloader) {
        this.config = config; this.payload = payload; this.downloader = downloader;
    }

    public void install(Path selected, Set<String> options, boolean force, Consumer<String> log) throws IOException {
        var validation = RootValidator.inspect(selected, config);
        if (!validation.valid() && !force) throw new IOException(validation.message());
        Path root = validation.root();
        Path lock = FilesEx.target(root, ".vh3-installer.lock");
        // 保留零字节锁文件，避免 Unix 上删除仍被打开的锁造成并发穿透。
        try (var channel = FileChannel.open(lock, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            FileLock held;
            try { held = channel.tryLock(); }
            catch (OverlappingFileLockException e) { throw new IOException("此目录已有安装任务正在运行。", e); }
            if (held == null) throw new IOException("此目录已有安装任务正在运行。");
            try (held) { installLocked(root, options, log); }
        }
    }

    private void installLocked(Path root, Set<String> options, Consumer<String> log) throws IOException {
        Texts texts = new Texts(config);
        List<Config.Mod> mods = config.selected(options);
        Path work = Files.createTempDirectory(root, ".vh3-install-");
        Transaction transaction = new Transaction(root, Files.createDirectory(work.resolve("backup")));
        boolean preserveRecovery = false;
        try {
            log.accept(texts.get("install.verify"));
            Path staged = Files.createDirectory(work.resolve("payload"));
            List<String> directories = payload.unpack(staged);
            Set<String> keepJars = new HashSet<>();
            for (String name : payload.paths()) {
                FilesEx.target(root, name);
                if (name.startsWith("mods/") && name.endsWith(".jar")) keepJars.add(name.substring(5));
            }
            for (String name : directories) FilesEx.target(root, name);
            for (Config.Mod mod : mods) {
                FilesEx.target(root, "mods/" + mod.filename());
                keepJars.add(mod.filename());
            }
            // 跳过 VP 仅跳过下载；仍清理错误版本，保留配置指定版本。
            config.mods().stream().filter(mod -> mod.id().equals("vp")).forEach(mod -> keepJars.add(mod.filename()));
            List<Path> obsolete = obsolete(root, keepJars);

            log.accept(texts.get("install.cache"));
            FilesEx.deleteTree(FilesEx.target(root, "vaultpatcher/cache"));
            log.accept(texts.get("install.base"));
            for (String name : directories) Files.createDirectories(FilesEx.target(root, name));
            for (String name : new TreeSet<>(payload.paths())) transaction.put(staged.resolve(name), FilesEx.target(root, name));
            log.accept(texts.get("install.download"));
            for (Config.Mod mod : mods) {
                Path installed = FilesEx.target(root, "mods/" + mod.filename());
                if (matchesInstalled(mod, installed)) {
                    log.accept(texts.get("install.mod.skip", "name", mod.name()));
                    continue;
                }
                Path file = work.resolve(mod.filename());
                downloader.download(mod, file, log);
                Downloader.verify(mod, file); // 不信任注入的下载实现；统一在写入 mods 前验证。
                transaction.put(file, installed);
                log.accept(texts.get("install.mod.done", "name", mod.name()));
            }
            log.accept(texts.get("install.cleanup"));
            for (Path path : obsolete) {
                transaction.remove(path);
                log.accept(texts.get("install.removed", "name", path.getFileName().toString()));
            }
            log.accept(texts.get("install.done"));
        } catch (IOException | RuntimeException e) {
            log.accept(texts.get("install.rollback"));
            try { transaction.rollback(); }
            catch (IOException rollback) {
                preserveRecovery = true;
                e.addSuppressed(rollback);
                throw new IOException(texts.get("install.recovery", "path", work.toString(), "error", e.getMessage()), e);
            }
            throw new IOException(texts.get("install.rolledback", "error", e.getMessage()), e);
        } finally {
            if (!preserveRecovery) {
                try { FilesEx.deleteTree(work); }
                catch (IOException e) { log.accept(texts.get("install.temp", "path", work.toString())); }
            }
        }
    }

    private static boolean matchesInstalled(Config.Mod mod, Path file) {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) return false;
        try {
            Downloader.verify(mod, file);
            return true;
        } catch (IOException invalid) {
            // 同名旧内容、损坏或不可读文件不能作为已安装凭据，继续正常下载/替换流程。
            return false;
        }
    }

    static List<Path> obsolete(Path root, Set<String> keep) throws IOException {
        Path mods = FilesEx.target(root, "mods");
        if (!Files.isDirectory(mods)) return List.of();
        List<Path> result = new ArrayList<>();
        try (var files = Files.list(mods)) {
            for (Path file : files.sorted().toList()) {
                String name = file.getFileName().toString();
                boolean owned = name.matches("(?i)vaultpatcher(?:-all)?-[0-9][a-z0-9.+_\\-]*\\.jar")
                        || name.matches("(?i)vh3_translation_patch(?:-transformer)?-[0-9][a-z0-9.+_\\-]*\\.jar");
                boolean retained = keep.contains(name);
                // Windows / 常见 macOS 卷的大小写别名可能与新文件指向同一文件。
                if (owned && !retained) {
                    for (String current : keep) {
                        Path expected = mods.resolve(current);
                        if (Files.exists(expected) && Files.isSameFile(file, expected)) { retained = true; break; }
                    }
                }
                if (owned && !retained) {
                    FilesEx.plain(file);
                    if (!Files.isRegularFile(file)) throw new IOException("旧模组不是普通文件：" + file);
                    result.add(file);
                }
            }
        }
        return result;
    }

    private static final class Transaction {
        private final Path root, backup;
        private final LinkedHashMap<Path, Path> originals = new LinkedHashMap<>();
        Transaction(Path root, Path backup) { this.root = root; this.backup = backup; }

        private void remember(Path target) throws IOException {
            if (originals.containsKey(target)) return;
            Path copy = null;
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) throw new IOException("目标不是普通文件：" + target);
                copy = backup.resolve(Integer.toString(originals.size()));
                Files.copy(target, copy, StandardCopyOption.COPY_ATTRIBUTES);
            }
            originals.put(target, copy);
            Properties index = new Properties();
            originals.forEach((path, original) -> index.setProperty(root.relativize(path).toString().replace('\\', '/'),
                    original == null ? "NEW_FILE" : original.getFileName().toString()));
            try (var writer = Files.newBufferedWriter(backup.resolve("recovery.properties"), java.nio.charset.StandardCharsets.UTF_8)) {
                index.store(writer, "relative target = backup filename; NEW_FILE means remove on rollback");
            }
        }

        void put(Path source, Path target) throws IOException {
            FilesEx.target(root, root.relativize(target).toString().replace('\\', '/'));
            remember(target);
            Files.createDirectories(target.getParent());
            Path temporary = Files.createTempFile(target.getParent(), ".vh3-write-", ".tmp");
            try {
                Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
                FilesEx.replace(temporary, target);
            } finally { Files.deleteIfExists(temporary); }
        }

        void remove(Path target) throws IOException {
            FilesEx.target(root, root.relativize(target).toString().replace('\\', '/'));
            remember(target);
            Files.deleteIfExists(target);
        }

        void rollback() throws IOException {
            IOException failure = null;
            List<Path> paths = new ArrayList<>(originals.keySet());
            Collections.reverse(paths);
            for (Path path : paths) {
                try {
                    FilesEx.target(root, root.relativize(path).toString().replace('\\', '/'));
                    Path original = originals.get(path);
                    if (original == null) Files.deleteIfExists(path);
                    else {
                        Path temporary = Files.createTempFile(path.getParent(), ".vh3-restore-", ".tmp");
                        try {
                            Files.copy(original, temporary, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                            FilesEx.replace(temporary, path);
                        } finally { Files.deleteIfExists(temporary); }
                    }
                } catch (IOException e) {
                    if (failure == null) failure = e; else failure.addSuppressed(e);
                }
            }
            if (failure != null) throw failure;
        }
    }
}
