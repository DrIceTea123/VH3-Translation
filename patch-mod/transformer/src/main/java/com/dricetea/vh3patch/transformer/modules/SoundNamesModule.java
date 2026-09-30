package com.dricetea.vh3patch.transformer.modules;

import com.dricetea.vh3patch.transformer.*;
import com.google.gson.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;

/** 声音名称的目标清单、旧 VP 接管判定和字段名映射导入，均由本模块维护。 */
public final class SoundNamesModule implements PatchModule {
    public static final String ID = "sound_names";
    private final PatchSpec spec;
    private final StringReturnPatch patch;

    public SoundNamesModule() {
        this(PatchSpec.load(ID, "com/dricetea/vh3patch/modules/SoundNamesModule", Set.of("_", "sfx", "SFX", " ")));
    }

    public SoundNamesModule(PatchSpec spec) {
        this.spec = spec;
        this.patch = new StringReturnPatch(spec);
    }

    @Override public PatchSpec spec() { return spec; }
    @Override public MethodNode target(ClassNode node) { return patch.target(node); }
    @Override public void apply(ClassNode node) { patch.apply(node); }

    @Override public boolean ownsVpRule(JsonObject rule) {
        if (!rule.has("target_class") || !rule.get("target_class").isJsonObject()) return false;
        JsonObject target = rule.getAsJsonObject("target_class");
        if (!target.has("name") || !target.get("name").getAsString().replace('.', '/').equals(spec.className())) return false;
        if (target.has("method")) {
            String method = target.get("method").getAsString();
            if (method.equals(spec.methodName())) return true;
            // 旧 VP 在调用方接管 formatSoundName 的返回值，不能只检查被调用的方法名。
            return method.equals("collectSoundEntries") && (!target.has("local")
                    || target.get("local").getAsString().equals("MformatSoundName"));
        }
        if (target.has("local") && target.get("local").getAsString().equals("MformatSoundName")) return true;
        if (rule.has("pairs") && rule.get("pairs").isJsonArray()) {
            for (JsonElement pair : rule.getAsJsonArray("pairs")) {
                if (pair.isJsonObject() && pair.getAsJsonObject().has("key")
                        && spec.ownedLiterals().contains(pair.getAsJsonObject().get("key").getAsString())) return true;
            }
        }
        return false;
    }

    @Override public void importMappings(Path vpSource, Path targetJar, Path output) throws IOException {
        // 先核实完整 JAR 和格式化方法，避免按已变化的算法猜测字段与显示文字的关系。
        MethodNode method = target(TargetJar.read(targetJar, spec, true));
        if (!MethodFingerprint.of(method).equals(spec.fingerprint())) throw new IllegalStateException("Sound formatter changed");
        JsonArray pairs = null;
        for (JsonElement element : JsonFiles.read(vpSource).getAsJsonArray()) {
            if (element.isJsonObject() && ownsVpRule(element.getAsJsonObject())) {
                if (pairs != null) throw new IllegalStateException("Multiple sound VP source groups");
                pairs = element.getAsJsonObject().getAsJsonArray("pairs").deepCopy();
            }
        }
        if (pairs == null) throw new IllegalStateException("Missing sound VP source group");
        Map<String, List<String>> fieldsByDisplay = new LinkedHashMap<>();
        try (JarFile jar = new JarFile(targetJar.toFile())) {
            var entry = jar.getJarEntry("iskallia/vault/init/ModSounds.class");
            if (entry == null) throw new IllegalStateException("Missing ModSounds class");
            try (var stream = jar.getInputStream(entry)) {
                ClassNode sounds = new ClassNode();
                new ClassReader(stream).accept(sounds, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                sounds.fields.stream().filter(field -> (field.access & Opcodes.ACC_STATIC) != 0
                        && field.desc.equals("Lnet/minecraft/sounds/SoundEvent;")).forEach(field ->
                        fieldsByDisplay.computeIfAbsent(legacyDisplayName(field.name), unused -> new ArrayList<>()).add(field.name));
            }
        }
        JsonObject translated = new JsonObject();
        JsonObject sourceNames = new JsonObject();
        for (JsonElement element : pairs) {
            JsonObject pair = element.getAsJsonObject();
            String english = pair.get("key").getAsString();
            List<String> matches = fieldsByDisplay.getOrDefault(english, List.of());
            if (matches.size() != 1) throw new IllegalStateException("Expected one sound field for " + english + ": " + matches);
            String field = matches.get(0);
            if (translated.has(field)) throw new IllegalStateException("Duplicate sound field: " + field);
            translated.add(field, pair.get("value").deepCopy());
            sourceNames.addProperty(field, english);
        }
        JsonFiles.write(output.resolve("runtime/src/main/resources/" + spec.defaultConfigResource()), translated);
        JsonObject report = new JsonObject();
        report.addProperty("module", ID);
        report.addProperty("sourceSha256", MethodFingerprint.sha256(Files.readAllBytes(vpSource)));
        report.addProperty("sourceCount", pairs.size());
        report.addProperty("importedCount", translated.size());
        report.add("fieldToOriginalDisplayName", sourceNames);
        report.add("originalPairs", pairs);
        JsonFiles.write(output.resolve("translations/sound-name-import.json"), report);
        System.out.println("Imported " + translated.size() + " raw sound-field mappings.");
    }

    /** 离线导入时复现上游英文格式；测试逐字段与真实方法比对，不用于游戏翻译。 */
    public static String legacyDisplayName(String field) {
        StringBuilder result = new StringBuilder();
        for (String word : field.toLowerCase(Locale.ROOT).split("_")) {
            if (word.equals("sfx")) result.append("SFX");
            else result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
        }
        return result.toString().trim();
    }
}
