# VaultChestType 名称迁移评估

评估日期：2026-10-02，下文保留当时的事实分析与单独 getter 方案。

**实施更新（2026-10-03，1.0.6）：** getter 已迁入 [chest_names](chest-names.md)，连同容器标题、桶名称、稀有度、经验提示/预览和辅助功能箱型统一处理；原 VP 九条显示字段规则已移除。未创建独立 chest_types，vault_xp 也已拆除。下文双模块方案已被此实现取代。

随后用户提出按翻译对象合并复用。[名称模块拆分评估](name-modules-assessment.md) 建议将此 getter 纳入范围更完整的 chest_names；下文保留较早的单独 getter 方案及事实分析，不表示已经决定新增两个并存模块。

## VP 实际修改的位置

现行 `the_vault-asm_main.json` 中该组只指定 `iskallia.vault.core.vault.stat.VaultChestType`，未限定 method/local，包含 9 对大小写精确的显示名映射：Wooden→木制、Gilded→镀金、Living→活化、Ornate→华丽、Treasure→至尊、Altar→祭坛、Hardened→硬质、Enigma→谜题、Flesh→肉食。

`getName()Ljava/lang/String;` 本身只有 `ALOAD 0 → GETFIELD name → ARETURN`，没有英文字符串常量。这组 VP 规则匹配的是 `<clinit>` 中传给枚举构造器的标题式英文显示常量，构造器将其存入 `private final String name`。因此严格说是初始化的显示字段被汉化，getter 只是返回该字段。

例如 WOODEN 构造时有两个独立字符串：枚举标识 `WOODEN` 和显示名称 `Wooden`。规则只列出后者，未修改 `Enum.name()`、ordinal 或 valueOf 使用的标识。

## 调用范围与结论

扫描当前固定核心 JAR 的 7,903 个 class，找到该 getter 的 3 个直接调用点：

| 调用方法 | 次数 | 用途 |
|---|---:|---|
| `LootStatsContainerElement.<init>` | 1 | 箱子统计的 `Stat.Builder.name(...)` |
| 同一构造方法 | 1 | 桶类型名拼接 ` Barrel` 后传给 `Stat.Builder.name(...)` |
| `ClientVaultXpTracker.formatChestName` | 1 | 拼接经验通知的稀有度、类型与箱/桶名称 |

`name` 字段只见构造器写入、getter 读取。上述直接调用都用于显示，未发现 getter 用于核心内存档、网络字段或业务索引；这不是对其他模组或反射调用的全面保证。

**适合迁移。** 推荐独立 `chest_types` 模块，在 getter 的唯一 ARETURN 前用原返回英文精确查表：`Wooden → 木制` 等 9 条。可以复用 `StringValuePatch.applyReturns` 和已有摘要/命中数/ASM 验证，无需改写枚举 `<clinit>` 或业务字段。运行模块没有客户端依赖，若按现有 VP 的两端作用范围迁移，可声明 BOTH 并注册到 CommonModules；专用服务端也需要对应外部配置。

具体迁移应删除现行 VP 的这 9 对规则，在原位置保留 VTP 接管注释，并将历史组保存供冲突/迁移测试使用；清单绑定核心哈希、getter 签名、摘要及 1 个返回点。缺项保持原英文，F3+T 后新建的统计项读取新值，已有 UI 需要重建；服务端修改配置后重启。

## 与经验模块的关系

迁移后 getter 仍然返回中文，因此不会使 `formatChestName` 的中间拼接重新变成全英文。经验模块仍应从未变的枚举 `name()` 构造完整英文配置键，而将原格式化结果作为回退。这适用于 VP 和未来 VTP 两种提供类型译文的方式。

两个配置职责不同：`chest_types` 是单个类型显示名，`vault_xp` 是整条经验提示名称。按现行模块独立规则，不让两者互读配置，也不通过全局语言键覆盖。

建议迁移后的离线验证覆盖：九种枚举的显示值、未配置回退、`name()/valueOf/ordinal` 不变；getter 之外所有方法摘要不变；结算与经验三个调用点、VP 旧规则冲突、两端初始化与重载。实际整合包联测仍需另行执行。
