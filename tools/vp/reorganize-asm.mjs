// One-time migration of the original main + ulti layout. Node.js 18+; dry run by default.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';

const repo = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const dir = path.join(repo, 'program/基础+硬编码汉化/config/vaultpatcher_asm');
const filenames = ['the_vault-asm_main.json', 'the_vault-asm_ulti.json'];
const input = filenames.map(name => ({ name, text: fs.readFileSync(path.join(dir, name), 'utf8') }));
const hash = text => createHash('sha256').update(text).digest('hex');
const cmp = (a, b) => a < b ? -1 : a > b ? 1 : 0;
const pairs = r => r.pairs ?? (Object.hasOwn(r, 'key') ? [{ key: r.key, value: r.value }] : []);
const targets = r => r.target_classes ?? (r.target_class ? [r.target_class] : []);
const field = (value, key) => value !== null && typeof value === 'object'
    && (Object.hasOwn(value, key) || Object.values(value).some(child => field(child, key)));

// Preserve every existing rule/comment verbatim, including escapes and formatting.
function rawEntries(text) {
    const parsed = JSON.parse(text);
    assert(Array.isArray(parsed));
    const entries = [];
    let cursor = text.indexOf('[') + 1;
    for (const value of parsed) {
        while (/[\s,]/.test(text[cursor])) cursor++;
        assert.equal(text[cursor], '{');
        const start = cursor;
        let depth = 0, quoted = false, escaped = false;
        for (; cursor < text.length; cursor++) {
            const char = text[cursor];
            if (quoted) {
                if (escaped) escaped = false;
                else if (char === '\\') escaped = true;
                else if (char === '"') quoted = false;
            } else if (char === '"') quoted = true;
            else if (char === '{' || char === '[') depth++;
            else if (char === '}' || char === ']') {
                depth--;
                if (depth === 0) { cursor++; break; }
            }
        }
        const raw = text.slice(start, cursor);
        assert.deepEqual(JSON.parse(raw), value);
        entries.push({ value, raw });
    }
    return entries;
}

const rules = [], originals = [];
for (const source of input) {
    source.entries = rawEntries(source.text);
    let comments = [];
    for (const [index, entry] of source.entries.entries()) {
        if (index === 0) { assert(entry.value.name && entry.value.dynamic === false); continue; }
        originals.push(entry.raw);
        if (!targets(entry.value).length) {
            assert(Object.keys(entry.value).every(k => k.startsWith('_comment')), 'Unknown standalone entry');
            comments.push(entry);
        } else {
            rules.push({ ...entry, comments, source: source.name, index, order: rules.length,
                families: [...new Set(targets(entry.value).map(t => t.name.split('$')[0]))] });
            comments = [];
        }
    }
    assert.equal(comments.length, 0, 'Trailing comments require manual placement');
}

const parents = rules.map((_, i) => i);
function root(i) { return parents[i] === i ? i : (parents[i] = root(parents[i])); }
function join(a, b) { parents[root(b)] = root(a); }
const firstByFamily = new Map();
for (const rule of rules) for (const family of rule.families) {
    if (firstByFamily.has(family)) join(firstByFamily.get(family), rule.order);
    else firstByFamily.set(family, rule.order);
}

// Reviewed semantic relationship: translated group labels -> restored lookup names.
// TextUtil and GroupUtils are called by the bestiary; Greed Assassin shares its entity label.
// The equipment appearance named Champion is unrelated and intentionally not joined.
const semanticFamilies = [
    'iskallia.vault.client.gui.screen.bestiary.element.GroupListElement',
    'iskallia.vault.client.gui.screen.bestiary.element.EntityGroupElement',
    'iskallia.vault.util.TextUtil',
    'iskallia.vault.util.GroupUtils',
    'iskallia.vault.event.GreedAssassinSpawnHandler',
];
for (const name of semanticFamilies) assert(firstByFamily.has(name), `Missing semantic target: ${name}`);
for (const name of semanticFamilies.slice(1)) join(firstByFamily.get(semanticFamilies[0]), firstByFamily.get(name));

const groups = new Map();
for (const rule of rules) {
    const key = root(rule.order);
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(rule);
}
const buckets = { main: [], long: [], complex: [] };
const decisions = [];
for (const group of groups.values()) {
    group.sort((a, b) => a.order - b.order);
    const long = group.some(e => pairs(e.value).length >= 50);
    const ordinal = group.some(e => field(e.value, 'ordinal'));
    const local = group.some(e => field(e.value, 'local'));
    const bucket = ordinal || (long && local) ? 'complex' : long ? 'long' : 'main';
    const key = group.flatMap(e => e.families).sort(cmp)[0];
    buckets[bucket].push({ key, group });
    decisions.push({ bucket, key, group, reason: ordinal ? 'ordinal 及关联规则' : local && long
        ? '至少 50 对且关联组涉及 local' : long ? '至少 50 对，关联组无 local/ordinal' : '其余规则' });
}
for (const bucket of Object.values(buckets)) bucket.sort((a, b) => cmp(a.key, b.key));
const outputRules = Object.fromEntries(Object.entries(buckets).map(([k, v]) => [k, v.flatMap(g => g.group)]));
// All original ulti groups qualify for extraction. Keep main's existing package order exactly.
assert(outputRules.main.every(e => e.source === filenames[0]));
outputRules.main.sort((a, b) => a.order - b.order);
assert(outputRules.main.every(e => pairs(e.value).length < 50 && !field(e.value, 'ordinal')));
assert(outputRules.long.every(e => !field(e.value, 'local') && !field(e.value, 'ordinal')));

const flattened = Object.values(outputRules).flat();
assert.equal(flattened.length, rules.length);
assert.equal(new Set(flattened.map(e => e.order)).size, rules.length);
assert.deepEqual(flattened.flatMap(e => [...e.comments.map(c => c.raw), e.raw]).sort(cmp), originals.slice().sort(cmp));
const familyBuckets = new Map();
for (const [bucket, entries] of Object.entries(outputRules)) for (const entry of entries) for (const family of entry.families) {
    if (familyBuckets.has(family)) assert.equal(familyBuckets.get(family), bucket);
    familyBuckets.set(family, bucket);
}
for (const family of firstByFamily.keys()) {
    const before = rules.filter(r => r.families.includes(family)).map(r => r.order);
    const after = flattened.filter(r => r.families.includes(family)).map(r => r.order);
    assert.deepEqual(after, before, `Rule order changed for ${family}`);
}

const headers = {
    main: input[0].entries[0].value,
    long: { ...input[1].entries[0].value, name: '宝藏猎人主要硬编码汉化-asm长列表',
        desc: '至少50对译文且关联组不涉及local/ordinal的长列表。版本V2.6.18' },
    complex: { ...input[1].entries[0].value, name: '宝藏猎人主要硬编码汉化-asm复杂部分',
        desc: '涉及ordinal、带local的长列表及其类与内容关联规则。版本V2.6.18' },
};
const headerRaw = obj => JSON.stringify(obj, null, 4).split('\n').map((line, i) => i ? '    ' + line : line).join('\r\n');
const outputs = {};
for (const [bucket, entries] of Object.entries(outputRules)) {
    const raw = bucket === 'main' ? input[0].entries[0].raw : headerRaw(headers[bucket]);
    const chunks = [raw, ...entries.flatMap(e => [...e.comments.map(c => c.raw), e.raw])];
    outputs[`the_vault-asm_${bucket}.json`] = '[\r\n    ' + chunks.join(',\r\n    ') + '\r\n]\r\n';
    assert.equal(JSON.parse(outputs[`the_vault-asm_${bucket}.json`]).length, chunks.length);
}
const configPath = path.join(dir, 'config.json');
const configText = fs.readFileSync(configPath, 'utf8');
assert.equal(configText.split('"the_vault-asm_ulti"').length, 2);
const newConfigText = configText.replace('"the_vault-asm_ulti"', '"the_vault-asm_long",\r\n        "the_vault-asm_complex"');
assert.deepEqual(JSON.parse(newConfigText).mods, JSON.parse(configText).mods.flatMap(n =>
    n === 'the_vault-asm_ulti' ? ['the_vault-asm_long', 'the_vault-asm_complex'] : [n]));

const stats = Object.fromEntries(Object.entries(outputRules).map(([bucket, entries]) => [bucket, {
    rules: entries.length, pairs: entries.reduce((sum, e) => sum + pairs(e.value).length, 0),
    ordinalRules: entries.filter(e => field(e.value, 'ordinal')).length,
    localRules: entries.filter(e => field(e.value, 'local')).length,
}]));
assert.equal(Object.values(stats).reduce((n, s) => n + s.pairs, 0), rules.reduce((n, e) => n + pairs(e.value).length, 0));
console.log(JSON.stringify({ mode: process.argv.includes('--apply') ? 'apply' : 'dry-run', stats,
    inputHashes: Object.fromEntries(input.map(s => [s.name, hash(s.text)])),
    preservedRuleAndCommentBlocks: originals.length }, null, 2));

if (process.argv.includes('--apply')) {
    for (const source of input) assert.equal(fs.readFileSync(path.join(dir, source.name), 'utf8'), source.text, 'Input changed during migration');
    for (const filename of ['the_vault-asm_long.json', 'the_vault-asm_complex.json']) assert(!fs.existsSync(path.join(dir, filename)), 'Output already exists');
    assert.equal(fs.readFileSync(configPath, 'utf8'), configText);
    for (const [name, text] of Object.entries(outputs)) fs.writeFileSync(path.join(dir, name), text, 'utf8');
    fs.writeFileSync(configPath, newConfigText, 'utf8');
    // Retire the merged input only after all replacements parse and match the planned output.
    for (const [name, text] of Object.entries(outputs)) assert.equal(fs.readFileSync(path.join(dir, name), 'utf8'), text);
    fs.unlinkSync(path.join(dir, 'the_vault-asm_ulti.json'));
    const report = [
        '# VP ASM 分类记录', '',
        '日期：2026-09-30。仅重组原 main/ulti；译文、约束和规则组保持原样。', '',
        '## 分类与关联', '',
        '- 长度阈值为每组至少 50 对译文。',
        '- 同一外部类及内部类合组，多目标规则整组保留并传递关联。',
        '- ordinal 组，以及包含长列表且涉及 local 的关联组，进入 complex。',
        '- 整个关联组都不含 local/ordinal 的长列表及关联条目进入 long；其余进入 main。',
        '- main 沿用原有包名顺序（原 ulti 全部提取）；提取文件按关联组最小完整类名排序。组内保持原 main → ulti 的相对顺序；注释随规则迁移。',
        '- 怪物图鉴反向替换、TextUtil、GroupUtils 和 GreedAssassinSpawnHandler 按内容关联，整体进入 complex；外观名 Champion 无该依赖，不按同词误合并。', '',
        '## 数量', '', '| 文件 | 规则组 | 译文对 | ordinal 组 | local 组 |', '|---|---:|---:|---:|---:|',
        ...Object.entries(stats).map(([k, s]) => `| asm_${k} | ${s.rules} | ${s.pairs} | ${s.ordinalRules} | ${s.localRules} |`), '',
        `完整保留 ${originals.length} 个原规则/注释块，共 ${rules.length} 组、${Object.values(stats).reduce((n, s) => n + s.pairs, 0)} 对译文；无合并键值、去重或改译。`, '',
        '## 提取清单', '', '| 目标文件 | 关联组排序键 | 来源数组索引（0 起始） | 原因 |', '|---|---|---|---|',
        ...decisions.filter(d => d.bucket !== 'main').sort((a, b) => cmp(a.bucket + a.key, b.bucket + b.key))
            .map(d => `| ${d.bucket} | ${d.key} | ${d.group.map(e => `${e.source.replace('the_vault-asm_', '')}:${e.index}`).join(', ')} | ${d.reason} |`), '',
        '## 原输入 SHA-256', '', ...input.map(s => `- ${s.name}: ${hash(s.text)}`), '',
        '迁移脚本：tools/vp/reorganize-asm.mjs（默认只预览，--apply 写入）。脚本用于原 main+ulti 布局，迁移后不应重复执行；历史输入可从迁移前 Git 版本查阅。', '',
        '已验证完整规则/注释多重集、类关联归属、同类相对顺序、JSON 与总数守恒。游戏运行效果尚未验证。', '',
    ].join('\n');
    fs.writeFileSync(path.join(repo, 'docs/maintenance/vp-asm-layout.md'), report, 'utf8');
}
