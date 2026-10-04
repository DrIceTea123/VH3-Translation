package cn.vmct.installer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** 所有界面更新在 EDT；哈希计算、解压、下载均在后台线程。 */
public final class Wizard extends JPanel {
    private final Config config;
    private final Texts texts;
    private final CardLayout layout = new CardLayout();
    private final JPanel pages = new JPanel(layout);
    private final JLabel steps = new JLabel();
    final JCheckBox accept;
    final JTextField path;
    final JButton back, next;
    private final JButton close;
    private final JButton browse;
    private final JLabel checking = new JLabel(" ");
    private final JTextArea target = area("");
    private final JTextArea log = area("");
    private final JLabel progressTitle;
    private final JProgressBar progress = new JProgressBar();
    private final Map<String, JCheckBox> choices = new LinkedHashMap<>();
    private int page;
    private boolean busy, force, complete;
    private Path selected;

    public Wizard(Config config, Texts texts, Path defaultDirectory) throws IOException {
        super(new BorderLayout(0, 18));
        configureFonts();
        this.config = config; this.texts = texts;
        setBorder(new EmptyBorder(24, 28, 20, 28));
        setPreferredSize(new Dimension(820, 630));
        JPanel header = new JPanel(new GridLayout(0, 1, 0, 9));
        JLabel title = new JLabel(texts.get("header.title"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        header.add(title);
        header.add(new JLabel(texts.get("header.subtitle")));
        steps.setForeground(new Color(35, 104, 120));
        header.add(steps);
        add(header, BorderLayout.NORTH);

        JPanel notice = page(texts.get("notice.title"));
        JTextArea noticeText = area(texts.notice());
        noticeText.setBackground(Color.WHITE);
        notice.add(new JScrollPane(noticeText), BorderLayout.CENTER);
        accept = new JCheckBox(texts.get("notice.accept"));
        accept.addActionListener(e -> refresh());
        notice.add(accept, BorderLayout.SOUTH);
        pages.add(notice, "0");

        JPanel directory = page(texts.get("directory.title"));
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.add(area(texts.get("directory.description")), BorderLayout.NORTH);
        JPanel picker = new JPanel(new BorderLayout(10, 10));
        path = new JTextField(defaultDirectory.toString());
        browse = new JButton(texts.get("directory.browse"));
        browse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            try { chooser.setCurrentDirectory(Path.of(path.getText()).toFile()); } catch (InvalidPathException ignored) {}
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) path.setText(chooser.getSelectedFile().toString());
        });
        picker.add(path, BorderLayout.CENTER); picker.add(browse, BorderLayout.EAST);
        picker.add(checking, BorderLayout.SOUTH);
        JPanel pickerHolder = new JPanel(new BorderLayout()); pickerHolder.add(picker, BorderLayout.NORTH);
        content.add(pickerHolder, BorderLayout.CENTER);
        directory.add(content); pages.add(directory, "1");

        JPanel components = page(texts.get("components.title"));
        JPanel list = new JPanel(); list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        JTextArea description = area(texts.get("components.description"));
        description.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        description.setAlignmentX(LEFT_ALIGNMENT); list.add(description); list.add(Box.createVerticalStrut(16));
        JCheckBox basic = new JCheckBox(texts.get("components.basic"), true); basic.setEnabled(false); list.add(basic);
        for (Config.Mod mod : config.mods()) {
            JCheckBox box = new JCheckBox(mod.name() + " " + texts.get(mod.required() ? "components.required" : "components.optional"), mod.required());
            choices.put(mod.id(), box); list.add(box);
            list.add(Box.createVerticalStrut(8));
        }
        list.add(Box.createVerticalGlue());
        components.add(list); components.add(target, BorderLayout.SOUTH); pages.add(components, "2");

        JPanel installing = new JPanel(new BorderLayout(0, 12));
        JPanel progressHeader = new JPanel(new BorderLayout(0, 12));
        progressTitle = new JLabel(texts.get("progress.title")); progressTitle.setFont(title.getFont().deriveFont(18f));
        progressHeader.add(progressTitle, BorderLayout.NORTH);
        progressHeader.add(area(texts.get("progress.description")), BorderLayout.CENTER);
        progressHeader.add(progress, BorderLayout.SOUTH); installing.add(progressHeader, BorderLayout.NORTH);
        log.setBackground(Color.WHITE); installing.add(new JScrollPane(log)); pages.add(installing, "3");
        add(pages, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        back = new JButton(texts.get("button.back")); next = new JButton(); close = new JButton(texts.get("button.close"));
        back.addActionListener(e -> { if (page == 3) page = 2; else page--; refresh(); });
        next.addActionListener(e -> advance());
        close.addActionListener(e -> { Window window = SwingUtilities.getWindowAncestor(this); if (window != null) window.dispose(); });
        buttons.add(back); buttons.add(next); buttons.add(close); add(buttons, BorderLayout.SOUTH);
        bodyFonts(this);
        refresh();
    }

    private JPanel page(String title) {
        JPanel result = new JPanel(new BorderLayout(0, 16));
        JLabel heading = new JLabel(title); heading.setFont(heading.getFont().deriveFont(Font.BOLD, 18f));
        result.add(heading, BorderLayout.NORTH); return result;
    }

    private static JTextArea area(String value) {
        JTextArea result = new JTextArea(value);
        result.setEditable(false); result.setLineWrap(true); result.setWrapStyleWord(true);
        result.setFont(UIManager.getFont("Label.font").deriveFont(16f));
        result.setOpaque(false); result.setBorder(new EmptyBorder(8, 8, 8, 8));
        return result;
    }

    static void configureFonts() {
        for (Object key : Collections.list(UIManager.getDefaults().keys())) {
            if (UIManager.get(key) instanceof Font font)
                UIManager.put(key, new javax.swing.plaf.FontUIResource(font.deriveFont(16f)));
        }
    }
    private static void bodyFonts(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child.getFont() != null && child.getFont().getSize2D() < 16f) child.setFont(child.getFont().deriveFont(16f));
            if (child instanceof Container container) bodyFonts(container);
        }
    }

    /** 两个对话框都明确选择第二项才允许绕过兼容性校验；关闭窗口等同返回。 */
    static boolean confirmForce(java.util.function.IntSupplier mismatch, java.util.function.IntSupplier risk) {
        return mismatch.getAsInt() == 1 && risk.getAsInt() == 1;
    }

    private void refresh() {
        layout.show(pages, Integer.toString(page));
        steps.setText(texts.get("step." + page));
        back.setEnabled(!busy && page > 0 && !complete);
        next.setText(texts.get(page == 2 ? "button.install" : page == 3 ? "button.retry" : "button.next"));
        next.setEnabled(!busy && !complete && (page != 0 || accept.isSelected()));
        next.setVisible(!complete);
        close.setEnabled(!busy); path.setEnabled(!busy); browse.setEnabled(!busy);
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JFrame frame) frame.setDefaultCloseOperation(busy ? WindowConstants.DO_NOTHING_ON_CLOSE : WindowConstants.DISPOSE_ON_CLOSE);
    }

    private void advance() {
        if (page == 0) { page = 1; refresh(); }
        else if (page == 1) validateDirectory();
        else install();
    }

    private void validateDirectory() {
        final Path candidate;
        try { candidate = Path.of(path.getText().trim()); }
        catch (InvalidPathException e) { error(e.getMessage()); return; }
        busy = true; checking.setText(texts.get("directory.checking")); refresh();
        new SwingWorker<RootValidator.Result, Void>() {
            protected RootValidator.Result doInBackground() throws Exception { return RootValidator.inspect(candidate, config); }
            protected void done() {
                busy = false; checking.setText(" ");
                try {
                    var result = get(); force = false;
                    if (!result.valid()) {
                        boolean confirmed = confirmForce(() -> JOptionPane.showOptionDialog(Wizard.this,
                                texts.get("directory.mismatch.body", "problems", result.message()), texts.get("directory.mismatch.title"),
                                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                                new String[]{texts.get("directory.return"), texts.get("directory.force")}, texts.get("directory.return")),
                                () -> JOptionPane.showOptionDialog(Wizard.this,
                                        texts.get("directory.force.confirm.body"), texts.get("directory.force.confirm.title"),
                                        JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                                        new String[]{texts.get("directory.return"), texts.get("directory.force.confirm.accept")}, texts.get("directory.return")));
                        if (!confirmed) { refresh(); return; }
                        force = true;
                    }
                    selected = result.root();
                    target.setText(texts.get("components.target", "path", selected.toString()) + (force ? "\n" + texts.get("directory.force.note") : ""));
                    page = 2;
                } catch (Exception e) { error(cause(e).getMessage()); }
                refresh();
            }
        }.execute();
    }

    private void install() {
        Set<String> options = new HashSet<>();
        choices.forEach((key, value) -> { if (value.isSelected()) options.add(key); });
        busy = true; page = 3; log.setText(""); progress.setIndeterminate(true);
        progressTitle.setText(texts.get("progress.title")); refresh();
        new SwingWorker<Void, String>() {
            protected Void doInBackground() throws Exception {
                new InstallerEngine(config, Payload.embedded(), Downloader.https(texts)).install(selected, options, force, this::publish);
                return null;
            }
            protected void process(List<String> messages) {
                for (String message : messages) log.append(message + "\n");
                log.setCaretPosition(log.getDocument().getLength());
            }
            protected void done() {
                busy = false; progress.setIndeterminate(false);
                try {
                    get(); complete = true; progress.setValue(100);
                    progressTitle.setText(texts.get("progress.success.title"));
                    JOptionPane.showMessageDialog(Wizard.this, texts.get("progress.success.body", "path", selected.toString()),
                            texts.get("progress.success.title"), JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    String message = cause(e).getMessage(); log.append(message + "\n");
                    progressTitle.setText(texts.get("progress.failure.heading"));
                    JOptionPane.showMessageDialog(Wizard.this, texts.get("progress.failure.body", "error", message),
                            texts.get("progress.failure.title"), JOptionPane.ERROR_MESSAGE);
                }
                refresh();
            }
        }.execute();
    }

    private static Throwable cause(Exception e) { return e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e; }
    private void error(String message) { JOptionPane.showMessageDialog(this, message, texts.get("error.title"), JOptionPane.ERROR_MESSAGE); }
}
