# VP ASM 分类记录

日期：2026-09-30。仅重组原 main/ulti；译文、约束和规则组保持原样。

后续变更（2026-09-30，VTP 1.0.1）：结算怪物名的 `formatMobName` 规则已在原位置替换为 VTP 接管注释，旧 237 对映射另存于 `patch-mod/translations/vp/combat_stats.json`。因此当前 complex 为 37 组、938 对（local 组为 15），三份合计 663 组、4586 对。下文数量与清单保留为最初分类时的快照，不代表接管后的实时数量。

## 分类与关联

- 长度阈值为每组至少 50 对译文。
- 同一外部类及内部类合组，多目标规则整组保留并传递关联。
- ordinal 组，以及包含长列表且涉及 local 的关联组，进入 complex。
- 整个关联组都不含 local/ordinal 的长列表及关联条目进入 long；其余进入 main。
- main 沿用原有包名顺序（原 ulti 全部提取）；提取文件按关联组最小完整类名排序。组内保持原 main → ulti 的相对顺序；注释随规则迁移。
- 怪物图鉴反向替换、TextUtil、GroupUtils 和 GreedAssassinSpawnHandler 按内容关联，整体进入 complex；外观名 Champion 无该依赖，不按同词误合并。

## 数量

| 文件 | 规则组 | 译文对 | ordinal 组 | local 组 |
|---|---:|---:|---:|---:|
| asm_main | 612 | 2809 | 0 | 36 |
| asm_long | 14 | 839 | 0 | 0 |
| asm_complex | 38 | 1175 | 7 | 16 |

完整保留 683 个原规则/注释块，共 664 组、4823 对译文；无合并键值、去重或改译。

## 提取清单

| 目标文件 | 关联组排序键 | 来源数组索引（0 起始） | 原因 |
|---|---|---|---|
| complex | iskallia.vault.client.gui.screen.accessibility.VaultAccessibilityScreen | main.json:94, main.json:96 | 至少 50 对且关联组涉及 local |
| complex | iskallia.vault.client.gui.screen.accessibility.VaultSoundOptionsScreen | main.json:97, ulti.json:6 | 至少 50 对且关联组涉及 local |
| complex | iskallia.vault.client.gui.screen.bestiary.element.EntityGroupElement | main.json:99, main.json:409, main.json:651, main.json:656 | ordinal 及关联规则 |
| complex | iskallia.vault.client.gui.screen.block.DeckCraftingScreen | main.json:108, main.json:109 | ordinal 及关联规则 |
| complex | iskallia.vault.client.gui.screen.block.VaultForgeScreen | main.json:134, main.json:135, main.json:136 | ordinal 及关联规则 |
| complex | iskallia.vault.client.gui.screen.bounty.element.task.AbstractTaskElement | main.json:139, main.json:140 | ordinal 及关联规则 |
| complex | iskallia.vault.client.gui.screen.player.legacy.tab.split.dialog.ResearchDialog | main.json:170, main.json:179, main.json:579, ulti.json:2 | ordinal 及关联规则 |
| complex | iskallia.vault.client.gui.screen.quest.QuestButtonElement | main.json:192, ulti.json:4 | 至少 50 对且关联组涉及 local |
| complex | iskallia.vault.client.gui.screen.rules.VaultRulesScreen | main.json:193, main.json:195 | 至少 50 对且关联组涉及 local |
| complex | iskallia.vault.client.gui.screen.summary.element.CombatStatsContainerElement | main.json:196, ulti.json:8 | 至少 50 对且关联组涉及 local |
| complex | iskallia.vault.client.gui.screen.summary.element.CrystalStatsContainerElement | main.json:198, main.json:199 | ordinal 及关联规则 |
| complex | iskallia.vault.client.map.VaultMapRenderHelper | main.json:247, ulti.json:10 | 至少 50 对且关联组涉及 local |
| complex | iskallia.vault.core.card.Card | main.json:331, main.json:332, main.json:333, main.json:338 | ordinal 及关联规则 |
| complex | iskallia.vault.core.vault.influence.Influences | main.json:361, main.json:637, main.json:638 | 至少 50 对且关联组涉及 local |
| complex | iskallia.vault.entity.entity.pet.PetModelType | main.json:399, main.json:562 | 至少 50 对且关联组涉及 local |
| long | iskallia.vault.command.vault.VaultCommand | main.json:294 | 至少 50 对，关联组无 local/ordinal |
| long | iskallia.vault.init.ModDynamicModels | main.json:464, main.json:465, main.json:466, main.json:467, main.json:468, main.json:469, main.json:470, main.json:471, main.json:472, main.json:473, main.json:474 | 至少 50 对，关联组无 local/ordinal |
| long | iskallia.vault.init.ModGearAttributes | main.json:476 | 至少 50 对，关联组无 local/ordinal |
| long | iskallia.vault.skill.ability.component.AbilityLabelFactory | main.json:633 | 至少 50 对，关联组无 local/ordinal |

## 原输入 SHA-256

- the_vault-asm_main.json: a6570b836197fc884e45de9588e32142f9619ff3ba29127858f5b71e4700f6dd
- the_vault-asm_ulti.json: f60934ffeb81fd22849ec63b68bfd347a0e110c19b6bf097a5ac9c01aecf949e

迁移脚本：tools/vp/reorganize-asm.mjs（默认只预览，--apply 写入）。脚本用于原 main+ulti 布局，迁移后不应重复执行；历史输入可从迁移前 Git 版本查阅。

已验证完整规则/注释多重集、类关联归属、同类相对顺序、JSON 与总数守恒。游戏运行效果尚未验证。
