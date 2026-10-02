# 宝箱与储物桶名称：chest_names

VTP 1.0.6 将宝箱名称、宝箱稀有度、储物桶和强化宝箱统一到 [chest_names.json](../../program/基础+硬编码汉化/config/vh3_translation_patch/chest_names.json)。运行侧为两端通用模块，不引用客户端 I18n。映射只在工程配置中维护，不打入 JAR。

## 配置规则

平面英文到中文配置共 **53 条**：9 个类型词、4 个宝箱稀有度词、16 个箱/桶/强化宝箱基础名、4 个带冒号的统计前缀、20 个完整桶组合覆盖。

1. 完整名称精确命中优先，例如添加 `"Common Wooden Chest": "自定义普通木箱"`。
2. 未命中完整名称时，若开头为已知稀有度，且稀有度和剩余基础名均有配置，则连接两项译文。例如 `Common` + `Wooden Chest` 得到“普通木制宝箱”。Java 不保存中文译文。
3. 缺少任一组成项时保留上游结果。完整英文占位也是显式覆盖，优先于组合。

`Wooden` 与 `Wooden Chest` 用途不同：前者是结算箱型短名，后者是完整箱名。保留独立键，修改类型译法时应一并修改对应基础名；不采用泛化英文替换，避免破坏特殊语序。`Ornate Strongbox` 沿用“强化华丽宝箱”。统计前缀 `Common: ` 等独立配置包含中文冒号，后方数字、XP 文本不改。

20 个完整覆盖为 Treasure、Altar、Hardened、Enigma、Flesh 五种桶 × 四种稀有度。迁移初稿保留旧经验模块的英文占位；本轮最终核对时发现工作区中这些值已改为中文，保留现值，例如“普通至尊储物桶”。可逐项修改，或增加基础桶名并移除其四条完整覆盖；不能忘记完整条目优先级。实现也支持把值改回完整英文占位。

## 字节码覆盖

目标清单为 [chest_names.properties](../transformer/src/main/resources/patches/chest_names.properties)，共 **10 个方法、53 个钩子**。

| 目标 | 端 | 钩子数 | 边界 |
|---|---|---:|---|
| `VaultChestType.getName` | BOTH | 1 | 返回值查表；不改字段初始化或 Enum.name/valueOf/ordinal |
| `VaultChestTileEntity.m_5446_` | BOTH | 32 | 只替换 TextComponent 构造的 String 参数，保留方块分支和父类回退 |
| Tracker `formatChestName` | CLIENT | 1 | 原枚举参数生成英文键，原显示结果作缺项回退 |
| Tracker `createPreviewNotifications` | CLIENT | 1 | 普通木制宝箱预览复用同一配置 |
| `LootStatsContainerElement.<init>` | CLIENT | 13 | 12 个稀有度拼接结果；1 个桶名称使用枚举生成英文键 |
| `VaultChestIconElement.lambda$new$13/$11/$9/$7` | CLIENT | 4 | 四种宝箱稀有度 |
| `VaultAccessibilityScreen.lambda$buildHunterTab$19` | CLIENT | 1 | 七种箱型转完整箱名，其他枚举值原样返回 |

宝箱实体的 Treasure Chest 分支原本不带稀有度，继续保持；XP 提示原本带稀有度，继续组合。客户端 F3+T 后新建显示读取新配置；服务端标题由服务端词表决定，专用服务端修改后重启。已生成组件不追溯修改。

## VP 提取与保留边界

从 main 提取 VaultChestType 9 对、VaultChestTileEntity 两组 36 对、LootStats 的 5 对、VaultChestIcon 的 4 对；从 long 提取辅助功能 Mname 混合组的 7 对。共 **6 组、61 对**；原位置留接管注释，混合组只删除相关 pairs。去重与去除历史前导空格后形成 33 个有效词条，再加 20 个完整桶组合覆盖。

旧片段保存为 [translations/vp/chest_names.json](../translations/vp/chest_names.json)，仅供导入和冲突测试。`:transformer:importChestNames` 导出候选词表到 build，不覆盖正式配置；通用 `prepare-vp` 遇到旧宝箱组会拒绝整组自动删除，防止损失混合组内其他文案。

仍保留 VP 的内容包括总计、陷阱、经验值、搜刮数量及说明句；装备亲和、装备/宠物/遗物外观；TreasureContainer 的“珍宝”、UniqueCrate 等其他容器。它们不属于本模块的宝库箱/桶类型名称。动态 summary 规则中的 Common/Omega/Challenge/Other 还服务于房间等语境，未整组迁入；正常配置下新模块输出中文，不再命中英文片段。删除 VTP 覆盖后，剩余 VP 动态规则仍可能影响英文回退，这是共存配置的行为。

冲突检查覆盖旧字段规则、宝箱 formatter 和预览、混合组相关键，以及 Tracker 调用方的 M/RformatChestName；允许矿石和固定标签规则。核心字节码已离线验证，专用服务端与完整整合包尚未进行游戏联测。
