package cn.vmct.installer;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class RootValidator {
    public record Result(Path root, List<String> problems) {
        public boolean valid() { return problems.isEmpty(); }
        public String message() { return String.join("\n", problems); }
    }

    public static Result inspect(Path selected, Config config) throws IOException {
        Texts texts = new Texts(config);
        if (!Files.isDirectory(selected)) throw new IOException(texts.get("validation.directory"));
        Path root = selected.toRealPath();
        Path mods = FilesEx.target(root, "mods");
        List<String> problems = new ArrayList<>();
        if (!Files.isDirectory(mods)) {
            problems.add(texts.get("validation.mods"));
            return new Result(root, List.copyOf(problems));
        }
        List<Path> vaults;
        try (var files = Files.list(mods)) {
            vaults = files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).matches("the_vault.*\\.jar"))
                    .sorted().toList();
        }
        if (vaults.size() != 1) problems.add(texts.get("validation.count", "count", Integer.toString(vaults.size())));
        Path expected = FilesEx.target(root, "mods/" + config.vaultFilename());
        if (!Files.isRegularFile(expected)) {
            problems.add(texts.get("validation.filename", "filename", config.vaultFilename()));
        } else if (!FilesEx.sha256(expected).equals(config.vaultHash())) {
            problems.add(texts.get("validation.hash"));
        }
        return new Result(root, List.copyOf(problems));
    }
}
