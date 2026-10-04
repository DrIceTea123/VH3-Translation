package cn.vmct.installer;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class Main {
    public static void main(String[] args) {
        try {
            Config config = Config.load();
            Texts texts = new Texts(config);
            if (Arrays.asList(args).contains("--check")) {
                Path tmp = Files.createTempDirectory("vh3-payload-check-");
                try { Payload.embedded().unpack(tmp); }
                finally { FilesEx.deleteTree(tmp); }
                System.out.println("OK 汉化包 V" + config.translationVersion() + " / 导出 " + config.exportSerial());
                return;
            }
            if (Arrays.asList(args).contains("--console")) { console(config, texts); return; }
            if (GraphicsEnvironment.isHeadless()) { System.err.println(texts.get("headless.message")); System.exit(1); }
            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    Wizard.configureFonts();
                    JFrame frame = new JFrame(texts.get("window.title"));
                    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    Wizard wizard = new Wizard(config, texts, defaultDirectory());
                    frame.setContentPane(wizard);
                    frame.setMinimumSize(new Dimension(760, 590));
                    frame.pack();
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                } catch (Exception e) { JOptionPane.showMessageDialog(null, texts.get("error.startup", "error", e.toString())); }
            });
        } catch (Exception e) {
            e.printStackTrace();
            if (!GraphicsEnvironment.isHeadless()) JOptionPane.showMessageDialog(null, e.getMessage(), "VH3 Installer", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    /** 使用 JAR 所在位置，绝不使用 shell 的当前工作目录。 */
    public static Path defaultDirectory() {
        try {
            Path location = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
            return Files.isDirectory(location) ? location : location.getParent();
        } catch (Exception e) { throw new IllegalStateException("无法确定安装器所在目录", e); }
    }

    private static void console(Config config, Texts texts) throws IOException {
        Console terminal = System.console();
        if (terminal == null) throw new IOException(texts.get("console.noinput"));
        terminal.printf("%s%n%n%s%n", texts.get("window.title"), texts.notice());
        if (!"YES".equals(terminal.readLine("%s", texts.get("console.accept")))) return;
        Path root;
        boolean force;
        for (;;) {
            String input = terminal.readLine("%s", texts.get("console.directory", "path", defaultDirectory().toString()));
            if (input == null) return;
            try {
                var validation = RootValidator.inspect(input.isBlank() ? defaultDirectory() : Path.of(input), config);
                root = validation.root(); force = false;
                if (validation.valid()) break;
                terminal.printf("%s%n", validation.message());
                if ("FORCE".equals(terminal.readLine("%s", texts.get("console.force")))) {
                    terminal.printf("%s%n", texts.get("directory.force.confirm.body"));
                    if ("CONFIRM".equals(terminal.readLine("%s", texts.get("console.force.confirm")))) { force = true; break; }
                }
            } catch (IOException | InvalidPathException e) { terminal.printf("%s%n", e.getMessage()); }
        }
        Set<String> options = new HashSet<>();
        terminal.printf("%s%n", texts.get("components.basic"));
        for (Config.Mod mod : config.mods()) {
            String name = mod.name() + " " + texts.get(mod.required() ? "components.required" : "components.optional");
            String answer = terminal.readLine("%s", texts.get(mod.required() ? "console.required" : "console.optional", "name", name));
            if (answer == null) return;
            if ("y".equalsIgnoreCase(answer) || (mod.required() && answer.isBlank())) options.add(mod.id());
        }
        if (!"INSTALL".equals(terminal.readLine("%s", texts.get("console.install")))) return;
        new InstallerEngine(config, Payload.embedded(), Downloader.https(texts)).install(root, options, force, text -> terminal.printf("%s%n", text));
        terminal.printf("%s%n", texts.get("progress.success.body", "path", root.toString()));
    }
}
