package cn.vmct.installer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.*;
import java.util.zip.*;

/** 无第三方测试依赖；所有写入只发生在 build 下的隔离临时目录。 */
public final class InstallerTest {
    private static int passed, skipped;
    private static Path area, artifacts;
    private static Config fixture;
    private static byte[] jar;
    private static final String VTP = "mods/vh3_translation_patch-1.0.18.jar";

    public static void main(String[] args) throws Exception {
        Path project = Path.of(args[0]);
        area = Files.createTempDirectory(project.resolve("build"), "tests-").toRealPath();
        artifacts = project.resolve("build/verification"); Files.createDirectories(artifacts);
        try {
            jar = jarBytes();
            Path jarFile = area.resolve("fixture.jar"); Files.write(jarFile, jar);
            String hash = FilesEx.sha256(jarFile);
            List<Config.Mod> mods = new ArrayList<>();
            for (String id : List.of("i18n", "vp", "jech", "oculus")) {
                mods.add(new Config.Mod(id, id, id.equals("i18n") || id.equals("vp"),
                        id.equals("vp") ? "vaultpatcher-all-1.5.3-fix.jar" : id + "-1.0.jar", URI.create("https://example.invalid/" + id), hash));
            }
            fixture = new Config("1", "3.7", "3.21.7", "1.18.2-3.21.6.6884", "the_vault-1.18.2-3.21.6.6884.jar", hash, mods);
            test("发布配置固定版本、HTTPS 和哈希", () -> {
                Config c = Config.load();
                check(c.selected(Set.of()).isEmpty(), "全部取消则不下载");
                check(c.selected(Set.of("vp")).size() == 1, "只下载明确选中项");
                check(c.mods().stream().noneMatch(m -> m.id().equals("vmct")), "禁用 VMCT");
                check(c.mods().stream().filter(m -> m.id().equals("jech")).findFirst().orElseThrow().filename().equals("jecharacters-1.18.2-4.3.11.jar"), "JECh 固定");
            });
            test("取消必装模组不下载，仍删除错误 VP 版本", () -> {
                Path root = root(); seed(root); AtomicInteger downloads = new AtomicInteger();
                write(root, "mods/vaultpatcher-all-1.5.3-fix.jar", "matching version");
                engine((mod, dest, log) -> { downloads.incrementAndGet(); throw new IOException("不应下载"); })
                        .install(root, Set.of(), false, text -> {});
                check(downloads.get() == 0, "取消后不能强制下载");
                check(!Files.exists(root.resolve("mods/vaultpatcher-all-1.0.jar")), "跳过下载仍删除错误版本");
                check(Files.readString(root.resolve("mods/vaultpatcher-all-1.5.3-fix.jar")).equals("matching version"), "保留目标版本，不重新下载");
                check(Files.exists(root.resolve(VTP)), "基础包继续安装");
                check(!Files.exists(root.resolve("mods/vh3_translation_patch-1.0.1.jar")), "基础 VTP 仍正常升级");
            });
            test("强制安装需要两次明确确认", () -> {
                for (int first : new int[]{-1, 0, 1}) for (int second : new int[]{-1, 0, 1}) {
                    AtomicInteger calls = new AtomicInteger();
                    boolean force = Wizard.confirmForce(() -> first, () -> { calls.incrementAndGet(); return second; });
                    check(force == (first == 1 && second == 1), "关闭或取消不能强制安装");
                    check(calls.get() == (first == 1 ? 1 : 0), "只在选择强制安装后显示风险框");
                }
            });
            test("目录校验提示来自可编辑文案", () -> {
                Texts text = new Texts(fixture);
                Path root = Files.createTempDirectory(area, "missing-mods-");
                check(RootValidator.inspect(root, fixture).message().equals(text.get("validation.mods")), "缺失 mods 提示");
                Files.createDirectory(root.resolve("mods"));
                String message = RootValidator.inspect(root, fixture).message();
                check(message.contains(text.get("validation.count", "count", "0")), "核心数量占位符");
                check(message.contains(text.get("validation.filename", "filename", fixture.vaultFilename())), "文件名占位符");
            });
            test("安装器默认目录与工作目录无关", () -> {
                Path before = Main.defaultDirectory(); String old = System.getProperty("user.dir");
                try { System.setProperty("user.dir", area.toString()); check(Main.defaultDirectory().equals(before), "不能读取 cwd"); }
                finally { System.setProperty("user.dir", old); }
            });
            test("合法整合包通过校验", () -> check(RootValidator.inspect(root(), fixture).valid(), "有效目录"));
            test("哈希、版本、重复核心识别", () -> {
                Path root = root(), core = root.resolve("mods/" + fixture.vaultFilename());
                Files.writeString(core, "modified"); check(!RootValidator.inspect(root, fixture).valid(), "错误哈希");
                Files.move(core, root.resolve("mods/the_vault-other.jar")); check(!RootValidator.inspect(root, fixture).valid(), "错误版本");
                Files.write(core, jar); check(!RootValidator.inspect(root, fixture).valid(), "重复核心");
            });
            test("目录检查没有清缓存或删模组副作用", () -> {
                Path root = root(); write(root, "vaultpatcher/cache/test", "cached");
                write(root, "mods/vaultpatcher-all-1.0.jar", "old"); RootValidator.inspect(root, fixture);
                check(Files.exists(root.resolve("vaultpatcher/cache/test")), "校验不得删缓存");
                check(Files.exists(root.resolve("mods/vaultpatcher-all-1.0.jar")), "校验不得删模组");
            });
            test("拒绝不匹配目录且不写入", () -> {
                Path root = Files.createTempDirectory(area, "invalid-");
                fails(() -> engine(ok()).install(root, Set.of("vp", "i18n"), false, s -> {}));
                try (var files = Files.list(root)) { check(files.count() == 0, "无写入"); }
            });
            test("强制安装可创建缺失 mods", () -> {
                Path root = Files.createTempDirectory(area, "forced-"); engine(ok()).install(root, Set.of("vp", "i18n"), true, s -> {});
                check(Files.exists(root.resolve(VTP)), "基础 VTP");
            });
            test("安装顺序、必装、完整覆盖、精确清理", () -> {
                Path root = root(); seed(root); AtomicInteger downloads = new AtomicInteger();
                Downloader verifyOrder = (mod, dest, log) -> {
                    check(!Files.exists(root.resolve("vaultpatcher/cache")), "先清缓存");
                    check(Files.readString(root.resolve("config/test.txt")).equals("new"), "再覆盖基础");
                    check(Files.exists(root.resolve("mods/vaultpatcher-all-1.0.jar")), "下载完成前不删旧 VP");
                    check(Files.exists(root.resolve("mods/vh3_translation_patch-1.0.1.jar")), "下载完成前不删旧 VTP");
                    downloads.incrementAndGet(); Files.write(dest, jar);
                };
                engine(verifyOrder).install(root, Set.of("vp", "i18n"), false, s -> {});
                check(downloads.get() == 2, "必装下载"); assertInstalled(root);
                check(!Files.exists(root.resolve("mods/jech-1.0.jar")), "选装默认关闭");
                check(Files.isDirectory(root.resolve("empty")), "保留空文件夹");
            });
            test("选装与重复安装", () -> {
                Path root = root(); engine(ok()).install(root, Set.of("vp", "i18n", "jech", "oculus"), false, s -> {});
                var timestamp = java.nio.file.attribute.FileTime.fromMillis(1600000000000L);
                for (Config.Mod mod : fixture.mods()) Files.setLastModifiedTime(root.resolve("mods/" + mod.filename()), timestamp);
                write(root, "mods/vaultpatcher-all-1.0.jar", "old vp");
                List<String> logs = new ArrayList<>();
                engine((mod, dest, log) -> { throw new AssertionError("有效文件不应重新下载：" + mod.id()); })
                        .install(root, Set.of("vp", "i18n", "jech", "oculus"), false, logs::add);
                for (Config.Mod mod : fixture.mods()) {
                    check(Files.getLastModifiedTime(root.resolve("mods/" + mod.filename())).equals(timestamp), "跳过时不重写模组");
                    check(logs.contains(new Texts(fixture).get("install.mod.skip", "name", mod.name())), "显示跳过日志");
                }
                check(!Files.exists(root.resolve("mods/vaultpatcher-all-1.0.jar")), "复用当前 VP 仍清理旧版本");
                check(Files.exists(root.resolve("mods/jech-1.0.jar")), "JECh");
                check(Files.exists(root.resolve("mods/oculus-1.0.jar")), "Oculus");
                check(Files.exists(root.resolve(VTP)), "重装保留当前 VTP");
                check(Files.exists(root.resolve("mods/vaultpatcher-all-1.5.3-fix.jar")), "重装保留当前 VP");
            });
            test("仅复用同名且哈希正确的模组，旧版损坏和缺失仍下载", () -> {
                Path root = root();
                Files.write(root.resolve("mods/i18n-1.0.jar"), jar);
                write(root, "mods/vaultpatcher-all-1.5.3-fix.jar", "corrupt");
                Files.write(root.resolve("mods/jech-old.jar"), jar);
                Set<String> downloaded = new HashSet<>();
                engine((mod, dest, log) -> { downloaded.add(mod.id()); Files.write(dest, jar); })
                        .install(root, Set.of("vp", "i18n", "jech", "oculus"), false, s -> {});
                check(downloaded.equals(Set.of("vp", "jech", "oculus")), "不能凭其他文件名或损坏内容跳过");
                for (Config.Mod mod : fixture.mods()) Downloader.verify(mod, root.resolve("mods/" + mod.filename()));
                check(Files.exists(root.resolve("mods/jech-old.jar")), "不扩大原有模组清理范围");
            });
            test("复用模组后下载失败，原文件及还原行为保持", () -> {
                Path root = root(); seed(root);
                Path existing = root.resolve("mods/i18n-1.0.jar"); Files.write(existing, jar);
                var timestamp = java.nio.file.attribute.FileTime.fromMillis(1600000000000L);
                Files.setLastModifiedTime(existing, timestamp);
                write(root, "mods/vaultpatcher-all-1.5.3-fix.jar", "corrupt");
                fails(() -> engine((mod, dest, log) -> {
                    check(mod.id().equals("vp"), "有效 i18n 不应下载"); throw new IOException("network interrupted");
                }).install(root, Set.of("vp", "i18n"), false, s -> {}));
                check(Arrays.equals(Files.readAllBytes(existing), jar), "保留复用文件");
                check(Files.getLastModifiedTime(existing).equals(timestamp), "复用文件不进入还原写入");
                check(Files.readString(root.resolve("mods/vaultpatcher-all-1.5.3-fix.jar")).equals("corrupt"), "下载失败不损坏原件");
                check(Files.readString(root.resolve("config/test.txt")).equals("old longer file"), "基础配置还原");
                check(Files.exists(root.resolve("mods/vaultpatcher-all-1.0.jar")), "失败时保留旧 VP");
            });
            test("下载中断还原原文件、保留旧模组", () -> {
                Path root = root(); seed(root); AtomicInteger calls = new AtomicInteger();
                fails(() -> engine((mod, dest, log) -> {
                    if (calls.incrementAndGet() == 2) throw new IOException("network interrupted");
                    Files.write(dest, jar);
                }).install(root, Set.of("vp", "i18n"), false, s -> {}));
                check(Files.readString(root.resolve("config/test.txt")).equals("old longer file"), "原配置还原");
                check(Files.exists(root.resolve("mods/vaultpatcher-all-1.0.jar")), "旧 VP 保留");
                check(Files.exists(root.resolve("mods/vh3_translation_patch-1.0.1.jar")), "旧 VTP 保留");
                check(!Files.exists(root.resolve(VTP)), "撤销新 VTP");
                check(!Files.exists(root.resolve("mods/i18n-1.0.jar")), "撤销新下载");
                check(!Files.exists(root.resolve("vaultpatcher/cache")), "缓存仍清空");
            });
            test("大小写不敏感文件系统保留当前版本", () -> {
                Path root = root();
                write(root, "mods/VAULTPATCHER-ALL-1.5.3-FIX.JAR", "old bytes");
                write(root, "mods/VH3_TRANSLATION_PATCH-1.0.18.JAR", "old bytes");
                engine(ok()).install(root, Set.of("vp", "i18n"), false, s -> {});
                check(Files.exists(root.resolve("mods/vaultpatcher-all-1.5.3-fix.jar")), "VP 不被大小写别名误删");
                check(Files.exists(root.resolve(VTP)), "VTP 不被大小写别名误删");
                check(FilesEx.sha256(root.resolve("mods/vaultpatcher-all-1.5.3-fix.jar")).equals(fixture.mods().get(1).hash()), "新内容确实覆盖");
            });
            test("坏下载不能替换有效文件", () -> {
                Path root = root(); write(root, "mods/i18n-1.0.jar", "original");
                fails(() -> engine((mod, dest, log) -> Files.writeString(dest, "truncated")).install(root, Set.of("vp", "i18n"), false, s -> {}));
                check(Files.readString(root.resolve("mods/i18n-1.0.jar")).equals("original"), "不覆盖");
            });
            test("禁止安装 HTML 等非 JAR 内容", () -> {
                Path html = area.resolve("page.html"); Files.writeString(html, "<html>error</html>");
                Config.Mod spec = new Config.Mod("bad", "bad", true, "bad.jar", URI.create("https://example.invalid"), FilesEx.sha256(html));
                fails(() -> Downloader.verify(spec, html));
            });
            test("ZIP 路径穿越、清单缺失和多余文件", () -> {
                Path stage = Files.createTempDirectory(area, "zip-");
                fails(() -> payload(Map.of("../escape", "bad"), Map.of()).unpack(stage));
                fails(() -> payload(Map.of("surprise", "bad"), Map.of()).unpack(stage));
                fails(() -> payload(Map.of(), Map.of("missing", "0".repeat(64))).unpack(stage));
                check(!Files.exists(area.resolve("escape")), "没有越界写入");
            });
            test("内置内容损坏时不修改安装目标", () -> {
                Path root = root(); seed(root);
                Payload corrupt = payload(Map.of("config/test.txt", "bad"), Map.of("config/test.txt", "0".repeat(64)));
                fails(() -> new InstallerEngine(fixture, corrupt, ok()).install(root, Set.of("vp", "i18n"), false, s -> {}));
                check(Files.readString(root.resolve("config/test.txt")).equals("old longer file"), "保留目标");
                check(Files.exists(root.resolve("vaultpatcher/cache/old")), "损坏检查在清缓存前");
            });
            test("拒绝链接造成的目录逃逸", () -> {
                Path root = root(), outside = Files.createTempDirectory(area, "outside-");
                Path link = root.resolve("config");
                try { Files.createSymbolicLink(link, outside); }
                catch (IOException | UnsupportedOperationException e) { throw new Skip("当前系统不允许创建符号链接"); }
                fails(() -> engine(ok()).install(root, Set.of("vp", "i18n"), false, s -> {}));
                check(!Files.exists(outside.resolve("test.txt")), "外部目录没有写入"); Files.delete(link);
            });
            test("并发安装锁", () -> {
                Path root = root();
                try (var channel = java.nio.channels.FileChannel.open(root.resolve(".vh3-installer.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE); var lock = channel.lock()) {
                    fails(() -> engine(ok()).install(root, Set.of("vp", "i18n"), false, s -> {}));
                    check(!Files.exists(root.resolve(VTP)), "并发任务无写入");
                }
            });
            test("真实基础内容逐文件字节一致", () -> {
                Path unpacked = Files.createTempDirectory(area, "real-payload-"); Payload payload = Payload.embedded(); payload.unpack(unpacked);
                Path source = project.getParent().resolve("translate-packs");
                for (String name : payload.paths()) check(Files.mismatch(source.resolve(name), unpacked.resolve(name)) == -1, "基础内容未原样打包：" + name);
            });
            test("图标与安装输入一致，说明使用 Markdown", () -> {
                try (var icon = Config.resource("icon.png")) {
                    check(Arrays.equals(icon.readAllBytes(), Files.readAllBytes(project.getParent().resolve("translate-packs/icon.png"))), "图标字节一致");
                }
                check(Main.icon().getWidth() > 0, "可解码图标");
                check(new Texts(Config.load()).notice().equals(Files.readString(project.resolve("resources/notice.md"))), "内置 Markdown 正文一致");
            });
            test("感叹号目录中的 JAR 仍可读取内置资源", () -> {
                Path folder = Files.createDirectory(area.resolve("中文 空格 & (test)!"));
                Path sample = folder.resolve("resource.jar");
                try (var out = new JarOutputStream(Files.newOutputStream(sample))) {
                    for (String name : List.of("cn/vmct/installer/Config.class", "cn/vmct/installer/Config$Mod.class", "cn/vmct/installer/Config$1.class")) {
                        out.putNextEntry(new JarEntry(name));
                        try (var in = Config.class.getResourceAsStream("/" + name)) { in.transferTo(out); }
                        out.closeEntry();
                    }
                    out.putNextEntry(new JarEntry("sample.txt")); out.write("内置资源".getBytes(StandardCharsets.UTF_8)); out.closeEntry();
                }
                try (var loader = new java.net.URLClassLoader(new java.net.URL[]{sample.toUri().toURL()}, null)) {
                    var method = loader.loadClass("cn.vmct.installer.Config").getDeclaredMethod("resource", String.class); method.setAccessible(true);
                    try (var in = (InputStream)method.invoke(null, "sample.txt")) {
                        check(new String(in.readAllBytes(), StandardCharsets.UTF_8).equals("内置资源"), "特殊路径下资源内容不丢失");
                    }
                }
                Files.delete(sample); // 流和 JAR 句柄必须正常关闭。
            });
            test("Markdown 排版、中文、代码转义与链接边界", () -> {
                String html = NoticeMarkdown.html("# 中文标题\n\n**粗体**和*斜体*与`<code>`\n\n- 第一项\n- 第二项\n\n1. 条款\n\n> 引用\n\n---\n\n```text\n<script>**原样**</script>\n```\n\n[主页](https://example.org/?a=1&b=2)\n[本地](file:///C:/test)\n<img src=\"https://example.org/image.png\">");
                for (String expected : List.of("<h1>中文标题</h1>", "<strong>粗体</strong>", "<em>斜体</em>",
                        "<code>&lt;code&gt;</code>", "<ul><li>第一项</li><li>第二项</li></ul>", "<ol><li>条款</li></ol>",
                        "<blockquote>引用</blockquote>", "<hr>", "&lt;script&gt;**原样**&lt;/script&gt;", "href=\"https://example.org/?a=1&amp;b=2\""))
                    check(html.contains(expected), "缺少 Markdown 渲染结果：" + expected);
                check(!html.contains("<img") && !html.contains("href=\"file:"), "不加载原始 HTML 或本地链接");
                check(!NoticeMarkdown.webLink("javascript:alert(1)"), "拒绝脚本链接");
                check(NoticeMarkdown.html("**未闭合 <tag>").contains("**未闭合 &lt;tag&gt;"), "未闭合标记作为正文");
            });
            test("界面说明必须确认、中文文案与离屏渲染", () -> SwingUtilities.invokeAndWait(() -> {
                try {
                    Config actual = Config.load(); Wizard wizard = new Wizard(actual, new Texts(actual), Path.of("/整合包/Vault Hunters"));
                    check(!wizard.next.isEnabled(), "尚未同意不得下一步"); wizard.accept.doClick(); check(wizard.next.isEnabled(), "同意后继续");
                    var choicesField = Wizard.class.getDeclaredField("choices"); choicesField.setAccessible(true);
                    @SuppressWarnings("unchecked") Map<String, JCheckBox> choices = (Map<String, JCheckBox>) choicesField.get(wizard);
                    for (String id : List.of("vp", "i18n")) {
                        JCheckBox box = choices.get(id); check(box.isEnabled() && box.isSelected(), "必装默认勾选且允许取消");
                        box.doClick(); check(!box.isSelected() && box.getText().contains("必装"), "取消仍保留必装字样");
                    }
                    render(wizard, artifacts.resolve("01-notice.png")); wizard.next.doClick();
                    check(wizard.path.getText().equals(Path.of("/整合包/Vault Hunters").toString()), "默认路径");
                    render(wizard, artifacts.resolve("02-directory.png"));
                    var field = Wizard.class.getDeclaredField("page"); field.setAccessible(true);
                    var refresh = Wizard.class.getDeclaredMethod("refresh"); refresh.setAccessible(true);
                    field.setInt(wizard, 2); refresh.invoke(wizard); render(wizard, artifacts.resolve("03-components.png"));
                    field.setInt(wizard, 3); refresh.invoke(wizard); render(wizard, artifacts.resolve("04-progress.png"));
                } catch (Exception e) { throw new RuntimeException(e); }
            }));
            System.out.println("PASS " + passed + " / SKIP " + skipped);
            Files.writeString(artifacts.resolve("test-result.txt"), "PASS " + passed + " / SKIP " + skipped + "\nJava " + System.getProperty("java.version") + "\n", StandardCharsets.UTF_8);
        } finally { FilesEx.deleteTree(area); }
    }

    private static void render(Wizard wizard, Path output) throws IOException {
        wizard.setSize(wizard.getPreferredSize()); layout(wizard);
        BufferedImage image = new BufferedImage(wizard.getWidth(), wizard.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); wizard.printAll(graphics); graphics.dispose(); ImageIO.write(image, "png", output.toFile());
    }
    private static void layout(Container parent) { parent.doLayout(); for (Component child : parent.getComponents()) if (child instanceof Container c) layout(c); }
    private static Path root() throws IOException {
        Path root = Files.createTempDirectory(area, "整合包 带空格-");
        Files.createDirectory(root.resolve("mods")); Files.write(root.resolve("mods/" + fixture.vaultFilename()), jar); return root;
    }
    private static void seed(Path root) throws IOException {
        write(root, "config/test.txt", "old longer file"); write(root, "vaultpatcher/cache/old", "cache");
        write(root, "vaultpatcher/modules/custom.json", "custom"); write(root, "mods/vaultpatcher-all-1.0.jar", "old vp");
        write(root, "mods/vh3_translation_patch-1.0.1.jar", "old runtime"); write(root, "mods/vh3_translation_patch-transformer-1.0.1.jar", "old transformer");
        write(root, "mods/other-mod.jar", "keep"); write(root, "mods/vaultpatcher-helper-1.0.jar", "keep helper");
    }
    private static void assertInstalled(Path root) throws IOException {
        check(Files.readString(root.resolve("config/test.txt")).equals("new"), "强制替换而非追加");
        check(Files.readString(root.resolve("vaultpatcher/modules/custom.json")).equals("custom"), "保留自定义规则");
        check(Files.exists(root.resolve(VTP)), "不误删本次 VTP");
        check(!Files.exists(root.resolve("mods/vaultpatcher-all-1.0.jar")), "删除旧 VP");
        check(!Files.exists(root.resolve("mods/vh3_translation_patch-1.0.1.jar")), "删除旧 VTP");
        check(!Files.exists(root.resolve("mods/vh3_translation_patch-transformer-1.0.1.jar")), "删除旧 transformer");
        check(Files.exists(root.resolve("mods/other-mod.jar")), "不删其他模组");
        check(Files.exists(root.resolve("mods/vaultpatcher-helper-1.0.jar")), "不扩大前缀范围");
    }
    private static void write(Path root, String relative, String content) throws IOException {
        Path target = root.resolve(relative); Files.createDirectories(target.getParent()); Files.writeString(target, content);
    }
    private static InstallerEngine engine(Downloader downloader) throws Exception {
        Map<String, String> content = Map.of("config/test.txt", "new", VTP, "fixture VTP", "vaultpatcher/modules/main.json", "new rules");
        Map<String, String> hashes = new HashMap<>();
        for (var entry : content.entrySet()) {
            Path file = Files.createTempFile(area, "hash", ".tmp"); Files.writeString(file, entry.getValue()); hashes.put(entry.getKey(), FilesEx.sha256(file));
        }
        return new InstallerEngine(fixture, payload(content, hashes), downloader);
    }
    private static Downloader ok() { return (mod, destination, log) -> Files.write(destination, jar); }
    private static Payload payload(Map<String, String> content, Map<String, String> hashes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("empty/")); zip.closeEntry();
            for (var entry : content.entrySet()) { zip.putNextEntry(new ZipEntry(entry.getKey())); zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8)); zip.closeEntry(); }
        }
        byte[] bytes = out.toByteArray(); return new Payload(() -> new ByteArrayInputStream(bytes), hashes);
    }
    private static byte[] jarBytes() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream out = new JarOutputStream(bytes)) { out.putNextEntry(new JarEntry("fixture.txt")); out.write("fixture".getBytes(StandardCharsets.UTF_8)); out.closeEntry(); }
        return bytes.toByteArray();
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static final class Skip extends Exception { Skip(String reason) { super(reason); } }
    private static void test(String name, Action action) throws Exception {
        try { action.run(); passed++; System.out.println("PASS: " + name); }
        catch (Skip e) { skipped++; System.out.println("SKIP: " + name + " / " + e.getMessage()); }
    }
    private static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
    private static void fails(Action action) throws Exception {
        try { action.run(); } catch (IOException e) { return; }
        throw new AssertionError("预期拒绝但操作成功");
    }
}
