"""Migrate reviewed VP 1.4.4 modules without changing targets, selectors or translations.

Usage: python upgrade-1.5.3.py OLD_CONFIG_DIRECTORY OUTPUT_GAME_DIRECTORY
The input is read-only; output must be a separate directory.
"""
import argparse
import copy
import hashlib
import json
from pathlib import Path


def read(path):
    # Gson accepts escaped apostrophes; strict JSON does not. Decode their same value.
    return json.loads(path.read_text(encoding="utf-8-sig").replace("\\'", "'"))


def write(path, value, newline="\n"):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes((json.dumps(value, ensure_ascii=False, indent=4) + "\n").replace("\n", newline).encode("utf-8"))


def migrate(data):
    result = [copy.deepcopy(data[0])]
    result[0]["i18n"] = False
    for rule in data[1:]:
        if "target_class" not in rule and "target_classes" not in rule:
            result.append(copy.deepcopy(rule))
            continue
        targets = rule.get("target_classes", [rule.get("target_class", {})])
        pairs = rule.get("pairs", [{"key": rule.get("key"), "value": rule.get("value")}])
        assert all(isinstance(p["key"], str) and isinstance(p["value"], str) for p in pairs)
        # Each old target has its own selectors. Split to avoid widening method/local/ordinal.
        for index, target in enumerate(targets):
            new = {k: copy.deepcopy(v) for k, v in rule.items()
                   if k not in ("target_class", "target_classes", "pairs", "key", "value") and index == 0}
            new["target_class"] = [target["name"]] if "name" in target else []
            info = {k: v for k, v in target.items() if k != "name"}
            if info:
                new["info"] = copy.deepcopy(info)
            new["pairs"] = copy.deepcopy(pairs)
            result.append(new)
    assert semantic_entries(data, False) == semantic_entries(result, True)
    return result


def semantic_entries(data, modern):
    entries = []
    for rule in data[1:]:
        if "target_class" not in rule and "target_classes" not in rule:
            continue
        pairs = rule.get("pairs", [{"key": rule.get("key"), "value": rule.get("value")}])
        targets = ([dict(rule.get("info", {}), name=n) for n in rule["target_class"]]
                   or [rule.get("info", {})]) if modern else rule.get("target_classes", [rule["target_class"]] if "target_class" in rule else [])
        for target in targets:
            entries.append(json.dumps([target, pairs], ensure_ascii=False, sort_keys=True))
    return entries


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    if args.source.resolve() == (args.output / "config/vaultpatcher_asm").resolve():
        parser.error("output must not overwrite the source directory")
    config = read(args.source / "config.json")
    names = config.pop("mods")
    config = dict(modules=names, default_language="zh_cn", load_all_modules=False, **config)
    debug = config["debug_mode"]
    debug["pairs_hide_limit"] = debug.pop("hide_pairs")
    debug.pop("output_format", None)
    debug.pop("missing_warn", None)
    write(args.output / "config/vaultpatcher_asm/config.json", config,
          "\r\n" if b"\r\n" in (args.source / "config.json").read_bytes() else "\n")
    report = {}
    for name in names:
        source = args.source / (name + ".json")
        data = read(source)
        migrated = migrate(data)
        write(args.output / "vaultpatcher/modules" / source.name, migrated,
              "\r\n" if b"\r\n" in source.read_bytes() else "\n")
        entries = semantic_entries(migrated, True)
        report[name] = dict(source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
                            effective_groups=len(entries),
                            effective_pairs=sum(len(json.loads(e)[1]) for e in entries),
                            semantics_sha256=hashlib.sha256("\n".join(entries).encode()).hexdigest())
    write(args.output / "vp-migration-report.json", report)
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
