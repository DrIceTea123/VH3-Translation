# VP ASM 分类与接管

配置在 `program/基础+硬编码汉化/config/vaultpatcher_asm/`。现行布局是 main / long / complex；旧 ulti 已整合。这里只维护分类规则与当前统计，历史迁移过程见项目 notes 和 Git。

## 分类规则

- 长列表：每组至少 **50 对**译文。
- 关联按同一外部类及其内部类处理；多目标规则整组保留，并传递关联。
- 同时检查内容关联，例如“刺客怪物 / Assassin”的正反替换；不能只按类名，也不因普通同词误合并无关内容。
- 含 ordinal 的组及关联规则进入 complex；含长列表且关联组涉及 local 的内容也整体进入 complex。
- 关联组完全不含 local/ordinal 的长列表及相关条目进入 long；其余进入 main。main 可以保留未归入提取组的短 local 规则。
- main 保持包名顺序；提取文件按关联组最小完整类名排序，组内保留原相对顺序，注释随规则移动。

交叉组 VaultAccessibilityScreen、VaultRulesScreen、祭坛任务相关类已按用户确认整体进入 complex。怪物图鉴、TextUtil、GroupUtils、GreedAssassinSpawnHandler 按内容合组；装备外观 Champion 不属于该依赖。

## 当前统计

截至 2026-10-01，两组名称规则已经被 VTP 接管：

| 文件 | 规则组 | 译文对 | ordinal 组 | local 组 |
|---|---:|---:|---:|---:|
| asm_main | 612 | 2809 | 0 | 36 |
| asm_long | 14 | 839 | 0 | 0 |
| asm_complex | 36 | 735 | 7 | 14 |
| 合计 | 662 | 4383 | 7 | 50 |

统计包含 pairs 数组和顶层单条 key/value，注释不算规则。动态配置与 other_mods 不计入此表。

## VTP 接管边界

- 结算 formatMobName 原 237 对规则 → combat_stats（已确认 ID 的 235 条）。
- 声音 collectSoundEntries 中 formatSoundName 返回值原 203 对规则 → sound_names；同类 6 条普通界面文案仍归 VP。
- 两处原位置均留有内容和 VTP 接管注释。旧规则在 `patch-mod/translations/vp/`，供导入/冲突测试，不再装回运行配置。
- 新接管必须同时更新冲突判定、配置和原位注释；版本/方法/旧 VP 冲突会阻止启动。

旧 main+ulti 一次性迁移已完成，当前输入不能再次使用旧脚本，因此删除 `tools/vp/reorganize-asm.mjs`。如需复查首次迁移的索引、哈希和脚本，可在整理前提交 `cd2c506` 中读取该脚本及本文历史版本；不应直接对现行配置执行。
