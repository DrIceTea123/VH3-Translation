package cn.vmct.installer;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.*;
import java.util.HexFormat;

final class FilesEx {
    private FilesEx() {}
    static String sha256(Path path) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(path)) {
                byte[] buf = new byte[65536];
                for (int n; (n = in.read(buf)) != -1;) md.update(buf, 0, n);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }

    // 不跟随符号链接 / Windows junction，防止覆盖或清理越过用户选定的目录。
    static void plain(Path path) throws IOException {
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            var a = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (a.isSymbolicLink() || a.isOther()) throw new IOException("路径包含链接或特殊文件，请使用实际目录：" + path);
        }
    }

    static Path target(Path root, String relative) throws IOException {
        if (relative.isEmpty() || relative.contains("\\") || relative.contains(":") || relative.startsWith("/"))
            throw new IOException("非法安装路径：" + relative);
        Path rel = Path.of(relative);
        for (Path part : rel) if (part.toString().equals("..") || part.toString().equals("."))
            throw new IOException("非法安装路径：" + relative);
        Path result = root.resolve(rel).normalize();
        if (!result.startsWith(root) || result.equals(root)) throw new IOException("安装路径越界：" + relative);
        plain(root);
        Path cursor = root;
        for (Path part : root.relativize(result)) { cursor = cursor.resolve(part); plain(cursor); }
        return result;
    }

    static void replace(Path source, Path destination) throws IOException {
        try { Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) { Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING); }
    }

    static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return;
        // 先完整预检再删除，避免发现链接时已经部分删除。
        try (var paths = Files.walk(root)) {
            for (Path path : paths.toList()) plain(path);
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file); return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                if (e != null) throw e;
                Files.delete(dir); return FileVisitResult.CONTINUE;
            }
        });
    }
}
