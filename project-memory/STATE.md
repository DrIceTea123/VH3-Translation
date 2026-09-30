# 项目状态快照

日期：2026-09-30。来源：初始方案对话及本地文件检查。后续变化需同时读取 notes/。

## 项目与输入

- 名称：VH3翻译；整合包：Vault Hunters 3rd Edition；主要翻译核心 mod：the_vault。
- 项目根目录：`C:\OriginCenter\MC翻译\Vault Hunters 3rd Edition`。
- 主工程：`[PROGRAM]`；`origin-*` 文件夹存原始输入；`Translated-旧版` 为历史翻译。
- 已完成 VP 配置：`[PROGRAM]\基础+硬编码汉化\config\vaultpatcher_asm`。
- 原始核心 JAR：`origin-3.21.7\the_vault-1.18.2-3.21.6.6884.jar`；没有源码。
- 原始 JAR 字节数：59,859,216；class 条目数：7,903。
- SHA-256：`E4B1E896558D69403D5A36CAF9049611642E459F295C964CE24A6BE06D67EE38`。
- JAR Manifest 版本：`1.18.2-3.21.6.6884`；整合包目录版本 `3.21.7` 与核心 mod 版本不同，不要混淆。
- mods.toml：Minecraft 1.18.2，Forge 最低范围 `[40,)`；实际整合包 Forge 精确版本尚未核实。
- `[PROGRAM]\bzlr.iss` 确认安装包指定 VP `1.4.4+3`。当前配置 class_patch=false、use_cache=true。
- 此项目未检测为 Git 仓库；本次未初始化 Git。

## 已有翻译规模（不是实际指令命中数）

| 配置 | 规则组 | 键值对 |
|---|---:|---:|
| the_vault-asm_main.json | 659 | 4,156 |
| the_vault-asm_ulti.json | 5 | 667 |
| 两份 the_vault-dynamic 配置合计 | 11 | 67 |
| other_mods.json | 35 | 86 |

主要 ASM 配置涉及 682 个不同目标类。超长部分为研究 60、任务 85、声音 203、怪物 237、房间 82。主文件约束中存在 method/local/ordinal；动态文件含 ordinal 和重复匹配故障的补救注释，迁移不能只改字段名。

## 已核对的字节码事实

通过 JDK 17 的 javap -p -c 读取原始 JAR，未启动游戏，未执行目标 mod：

- `iskallia.vault.client.data.ClientVaultXpTracker`：有独立的 `formatChestName(VaultRarity, VaultChestType, boolean)`、`formatOreName(ResourceLocation)`、`formatMobName(ResourceLocation)`。宝箱名将稀有度、类型及 Chest/Barrel 拼接；矿石和怪物名基于路径改写为英文。`createPreviewNotifications()` 另有固定英文预览。
- `iskallia.vault.client.gui.screen.summary.element.CombatStatsContainerElement.formatMobName(String)`：调用方传入实体 ResourceLocation.toString()；原方法去命名空间，将下划线、大小写转换成英文。
- `iskallia.vault.client.gui.screen.accessibility.VaultSoundOptionsScreen.formatSoundName(String)`：参数来自 ModSounds 的反射字段名；collectSoundEntries() 当时也持有 SoundEvent，可考虑在这里使用声音 ID。
- `iskallia.vault.client.map.VaultMapRenderHelper.getTooltipText(ResourceLocation, boolean)`：保留房间路径及上下文；适合在大小写转换前按类别生成显示文本。
- `iskallia.vault.research.type.Research.getName()`：直接返回内部 name 字段；不能未经调用分析全局中文化。
- 原始 en_us.json：aggressive_cow 与 aggressive_cow_boss 均为 Cow；现有汉化分别是战斗牛、战斗牛首领。复用实体语言键时必须保留此类专用覆盖。
- 目标 JAR 中 Minecraft 方法出现 m_…_ 命名，开发/发布映射需处理。

## 当前完成与未完成

- 完成：读取工程；比较三条路线；核对上述目标；用户接受渐进独立补丁方案；建立项目文档和共享记忆入口。
- 未完成：独立补丁 mod 代码、构建工具、开发环境、规则迁移、字节码补丁验证、整合包及专用服务端测试。
- 当前下一步：先确定 Forge 精确版本与独立补丁工程位置，按本轮用户任务推进；不要因为存在 TODO 自动开始开发。
