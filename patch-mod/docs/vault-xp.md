# 宝库经验提示：vault_xp

目标为固定核心 `the_vault-1.18.2-3.21.6.6884.jar` 的 `iskallia.vault.client.data.ClientVaultXpTracker`。本模块只在客户端启用；服务端不注册这些目标，也不读取配置。本文依据真实 JAR 的 `javap -p -c -v` 与 ASM 分析，不是上游 Java 源码。

## 方法分析与覆盖

| 方法 | 上游行为 | VTP 注入位置 / 数量 |
|---|---|---|
| `formatChestName(VaultRarity, VaultChestType, boolean)` | 拼接稀有度、`type.getName()`、Chest/Barrel；布尔值为 true 表示桶 | 唯一 String 返回点 / 1 |
| `formatOreName(ResourceLocation)` | 取 ID 路径；末尾 `_ore` 单独拆出后追加 ` Ore`，其他路径直接将下划线换空格并首字母大写 | 两个 String 返回点 / 2 |
| `formatMobName(ResourceLocation)` | 取 ID 路径，将下划线换空格，按空白分词处理大小写 | 唯一 String 返回点 / 1 |
| `handleDeltas(float, XpSnapshot)` | 比较统计快照，计算每项增量及 XP，调用 formatter 或使用固定标签 | 仅 `Treasure Door`、`Coin Pile`、`Treasure Sand`、`Bonus` 四个显示常量之后 / 4 |
| `createPreviewNotifications()` | 创建名称为 `Common Wooden Chest`、XP 为 24、数量为 1 的单条预览 | 仅名称常量之后 / 1 |

共 5 个方法、9 个钩子，签名、摘要和命中数见 [目标清单](../transformer/src/main/resources/patches/vault_xp.properties)。三个 formatter 的算法保留；`capitalize` 不注入，避免翻译名称片段后又被上游大小写处理。

`tick`、`updateBreakdown`、`buildSnapshot`、`computeChestXp`、`computeOreXp`、`computeMobXp`、快照构建 lambda、`tickNotifications`、`addNotification`、`reset` 和各 getter 均不修改。`handleDeltas` 中的默认 XP 查找、资源 ID、算术与判断也保持原样。预览的 24 XP、数量 1、通知数量上限和存活时间不变。

## 配置键及 VP 共存

唯一正式配置为 [vault_xp.json](../../program/基础+硬编码汉化/config/vh3_translation_patch/vault_xp.json)，采用完整英文显示名精确匹配，不使用实体 ID 或语言键。未命中保留原方法结果，不依赖 `combat_stats` 模块在运行时查表。

| 输入 | 配置键 | 初始译文 |
|---|---|---|
| COMMON、WOODEN、false | `Common Wooden Chest` | 普通木制宝箱 |
| COMMON、WOODEN、true | `Common Wooden Barrel` | 普通木制储物桶 |
| `the_vault:ore_benitoite` | `Ore Benitoite` | 蓝锥矿石 |
| `the_vault:chromatic_iron_ore` | `Chromatic Iron Ore` | 异色铁矿石 |
| `the_vault:aggressive_cow` | `Aggressive Cow` | 战斗牛 |
| `the_vault:aggressive_cow_boss` | `Aggressive Cow Boss` | 战斗牛首领 |
| 预览固定名称 | `Common Wooden Chest` | 与实战共用一条映射 |

矿石和怪物直接用原返回结果查表，沿用原算法对大小写、空格及命名空间的处理。不同命名空间形成相同显示名时共享一个键。

宝箱需要额外处理：现行 VP 在 `VaultChestType` 枚举中把 `Wooden` 等显示字段改成“木制”等中文。VTP 虽在 PRE_CLASS 先注入，但钩子在游戏运行时才执行；此时 `getName()` 已返回 VP 翻译，直接查表会得到 `Common 木制 Chest`，无法匹配英文配置。

因此宝箱返回钩子将原结果和仍未改变的两个枚举参数、桶标记传给 `translateChest`。运行侧用枚举 `name()` 构造英文键（例如 `COMMON + WOODEN + false → Common Wooden Chest`），命中则返回配置值；缺项时保留原结果，包括已有 VP 产生的混合文本。不会把枚举显示字段改回英文，也不会删除其他界面对该 VP 规则的依赖。该辅助入口只依赖 Java `Enum`，不加载上游游戏类型。

当前 VP 没有直接针对 `ClientVaultXpTracker` 的规则，本轮无规则需要迁移。模块启动检查拒绝新加入的该类 VP 规则，避免二次处理破坏英文键；没有历史导入器，自动迁移遇到此类规则也要求人工审查，不能静默删除。

## 初始配置来源

首份配置共 354 条：72 条宝箱/桶组合、20 条矿石、258 条怪物、4 条固定标签。其中 320 条为中文，34 条保留英文。

- 宝箱覆盖真实枚举的 4 种稀有度 × 9 种类型 × 箱/桶。稀有度及名称沿用现行 VP 的宝箱实体与结算译文。没有直接对应旧译文的 Treasure、Altar、Hardened、Enigma、Flesh 桶共 20 个组合保留完整英文，不表示这些组合必然在当前玩法中出现。
- 矿石依据父工作区 `origin-3.21.7/config/the_vault/vault_stats.json` 的 `blocksMined`，补充已有语言文件中的异色铁、宝库之石和旧乌托迪矿石。取当前 OpenLoader 汉化资源包对应 `block.the_vault.<路径>` 的译文；`Ore Funsoide` 无可靠对应，保留英文。没有将物品英文名误当 formatter 返回值。
- 怪物以正式 `combat_stats.json` 的 235 条为主，按该类的实际大小写规则生成显示键；再补全基线 `mobsKilled` 中的具体实体。缺失处优先使用资源包同 ID 语言键；`Mummy` 沿用历史 VP 中的“木乃伊”。原版实体采用标准中文名，Goat/Witch 也与现行 VP 一致。排除配置的 `minecraft:default` 和 `mob_type/dungeon_boss` 特殊统计键，不把它们当成具体实体。
- `Treasure Door` 依据现有宝藏门术语；`Coin Pile` 与 `Treasure Sand` 沿用资源包“钱币堆”“宝藏沙”。`Bonus` 暂留英文。

其余 12 个英文怪物占位为 `Dead Beard`、`Soul Vulture`、`Rocky Roller`、`Tusklin`、`Mantis Shrimp`、`Bunfungus`、`Alligator Snapping Turtle`、`Undead Miner`、`Swampy`、`Burned`、`Glacial Hunter`、`Foxhound`。这些只是待核对译名，不是运行时白名单；新增显示名只需编辑 JSON。未来出现未配置实体或方块，仍保留上游结果。

配置仅保存在工程 config，不打包进源码资源或 JAR。首次缺失/损坏阻止客户端启动；F3+T 错误保留旧快照。成功重载后，新产生的通知及新建预览使用新译文，已存入通知列表的旧文本不会追溯改写。

## 验证边界

VTP 1.0.5 的 72 项离线测试通过。新增验证包括：执行真实 formatter 字节码的 72 种箱/桶组合、矿石两个分支、牛/首领及未知值回退；模拟 VP 中文显示字段仍命中英文键；移除新增钩子后全类每个方法恢复原摘要；错误摘要、重复补丁、错误命中数与 VP 冲突拒绝；服务端过滤、配置独立性与重载。

测试中的 Minecraft 资源标识和上游枚举由隔离替身提供，不属于真实游戏验收。仍需在完整整合包确认实战通知、HUD 设置预览、F3+T、中文布局、通知开关及全部转换器共存。
