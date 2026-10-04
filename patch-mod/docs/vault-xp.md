# 宝库经验提示的名称归属

VTP **1.0.6** 已按用户要求拆除 `vault_xp`。`ClientVaultXpTracker` 的业务方法和计算仍保留，各显示入口分别使用共享名称模块或 VP；无需安装空的 `vault_xp.json`。

| 显示入口 | 归属 | 配置与处理 |
|---|---|---|
| `formatMobName(ResourceLocation)` | VTP `mob_names` | 原参数取实体 ID，按冒号后的路径查表；与结算页共用 246 条映射 |
| `formatChestName(VaultRarity,VaultChestType,boolean)` | VTP `chest_names` | 从枚举 `name()` 和桶标记构造英文名称；共用宝箱词表 |
| `createPreviewNotifications()` 的 `Common Wooden Chest` | VTP `chest_names` | 使用同一查表与组合逻辑 |
| `handleDeltas` 调用 `formatOreName()` 的结果 | VP `asm_main` | `method=handleDeltas, local=MformatOreName`，20 对完整英文显示名称 |
| `handleDeltas` 的四个固定标签 | VP `asm_main` | Treasure Door、Coin Pile、Treasure Sand、Bonus，限定方法的静态规则 |

两个 VTP 模块共同修改 Tracker 类，但各自拥有不同方法；注册表合并成一个 PRE_CLASS 转换器。矿石方法、`handleDeltas` 和 `capitalize` 不再由 VTP 注入。VP 随后在 CLASS 阶段处理保留的规则。加载次序不会阻止运行时 `getName()` 返回中文，因此宝箱键由枚举标识重建，不直接拿中英混合结果匹配完整英文。

## 怪物名称合并

正式配置为 [mob_names.json](../../translate-packs/config/vh3_translation_patch/mob_names.json)，由 `combat_stats.json` 扩展。原 235 条与经验提示对应值完全一致，原样保留；另外依据基线 `vault_stats.json` 的真实实体 ID 合并 11 条有来源的译名，共 246 条。保持战斗牛与首领、各等级居民的区别。

12 条原英文占位未强制写入实体表：Dead Beard、Soul Vulture、Rocky Roller、Tusklin、Mantis Shrimp、Bunfungus、Alligator Snapping Turtle、Undead Miner、Swampy、Burned、Glacial Hunter、Foxhound。缺项先尝试已注册实体的当前语言译名，再回退原格式化结果，避免英文占位遮住资源包已有译名。未注册 ID 不取注册表默认实体。新增译文只需添加实际路径键。

此模块接管两个 formatter、三个返回点。BountyHud 僵尸预览仍为 VP；GroupUtils 的语言名称和分组、FighterEntity 的玩家/皮肤名称、宠物与装备外观未纳入本次映射。分组属于 bestiary_groups，不能与具体实体名称合并。

## 矿石和固定标签

20 条矿石名称和四个固定标签由正式 VP 词表维护；例如 Ore Funsoide 已有“方索伊德矿石”，不再沿用早期英文占位结论。两组位于 `asm_main` 的 `iskallia.vault.client.data` 排序位置，均有用途与边界注释。未知名称保持原文回退；结算页矿石短名算法继续使用原来的 VP 与资源包逻辑。

选择调用结果规则而非 `RformatOreName`：早期检查的 VP 1.4.4+1 的 `InsnNodeHandler` 在 ARETURN 后插入调用，不能可靠替换方法返回值；`MethodNodeHandler` 能在调用之后、结果被使用之前插入。测试直接执行该 VP 处理器，验证一次矿石调用结果钩子、20 条映射和四个标签。两个 formatter 分支产生 `Ore Benitoite`、`Chromatic Iron Ore` 等完整键后再翻译，不触碰 ResourceLocation 和 XP 查找键。现行离线回归使用 VP 1.5.3-hotfix；完整整合包仍需游戏联测。

## 升级与验证

当前安装统一使用单 JAR、现行 14 份配置与配套 VP，见 [安装说明](../../wiki/install.md)。`combat_stats.json` 改名为 `mob_names.json`；旧 `vault_xp.json` 不再读取。工程内已知译文已迁移，个人自定义项需手动合并：怪物项按真实实体 ID 路径合入 `mob_names`，宝箱完整键直接合入 `chest_names`，矿石与四个标签改对应 VP 词条。不能由英文显示名猜测实体 ID。

VTP 配置客户端 F3+T 生效，既有结算页需重开，已显示通知不追溯重译；VP 配置修改按其缓存/重启流程生效。专用服务端须新增 `chest_names.json`，参见 [宝箱模块](chest-names.md)。

离线验证覆盖真实 formatter 的 72 种箱/桶组合、模拟中文 getter、两模块同类应用、牛/首领及未知实体回退、矿石两分支保持原算法、真实 VP 处理器及剩余配置兼容。宝箱钩子移除后恢复原方法摘要；核心哈希、错误摘要/命中数、重复应用、两端过滤、配置重载和首次错误均有校验。尚未执行游戏验收。
