# 卡牌显示：card_text

VTP 1.0.7 将卡牌名称显示、条件、效果和物品提示统一到 [card_text.json](../../translate-packs/config/vh3_translation_patch/card_text.json)。按用户选择，收纳界面、工作台的固定文案及管理命令仍由 VP 处理。

## 配置与语序

正式配置为 UTF-8 平面英文到中文对象；101 项包含类型、套组、颜色、相对位置、固定标签和完整英文句式。译文只维护在工程配置中，不打入 JAR。旧 VP 用空格、“Card”等零散片段拼句的规则不再直接复用。

| 英文句式键 | 译文示例 |
|---|---|
| ` > If there are exactly {0} {1} Cards` | ` > 需要有{0}张符合以下条件的卡牌：{1}` |
| ` > For Each {0} Card` | ` > 每张符合以下条件的卡牌提供1层效果：{0}` |
| `x{0} to {1} Cards {2}` | `{2}的{1}卡牌效果变为{0}倍` |
| `+{0} Crate Tiers Completing a vault with at least {1} Resource Cards` | `携带至少{1}张资源卡完成宝库时，板条箱等级+{0}` |
| `{0} (Currently {1})` | `{0}（当前{1}）` |

`{0}` 等是原句中已有的文本片段，不是业务 ID，也不是 Java 格式化参数。可以移动参数，例如将资源卡数量移到句首；参数中的数值不重新计算。数量上下限、单复数、无筛选条件、无位置条件、Shift 数值范围分别配置。类型/套组继续沿用“数值、进阶、陪衬”等已有译法。

查找顺序为：完整文本精确键 → 完整句式 → 子组件的精确键。句式按固定文字长度降序排列，同长按英文键排序，不依赖 JSON 顺序。捕获到的参数会继续解析子句和以 `, `、`, and `、` or `、`/` 分隔的筛选词；不在任意名称内全局替换英文单词。未知文本保留。空配置 `{}` 关闭本模块覆盖，空译文显式隐藏。

源句式参数必须从 `{0}` 连续编号，每个出现一次，最多到 `{7}`，不能相邻且句式必须有固定文字；非空译文必须保留全部参数，不得增加未知参数。参数匹配非空文本。配置校验失败时不发布新快照：首次失败阻止启动，F3+T 失败保留旧配置。客户端重建的 tooltip 使用新值；专用服务端修改后重启。

组件按原有文本区段切分、重排，捕获参数保留有效颜色、粗体、悬浮及点击样式；译文固定文字沿用原句固定文字的主要样式。输入组件不被修改。名称精确覆盖沿用名称根组件样式；它不会尝试把卡牌名称内部的词当作条件句式翻译。

## 字节码边界

固定基线仍为 the_vault `1.18.2-3.21.6.6884`。清单 [card_text.properties](../transformer/src/main/resources/patches/card_text.properties) 共 **21 个方法、36 个钩子**。

| 显示入口 | 方法数 / 钩子数 | 端与处理 |
|---|---:|---|
| `Card.addText` | 1 / 4 | CLIENT；等级、颜色、类型、套组组件加入 tooltip 时翻译 |
| `CardCondition.addText`、`CardScaler.addText` | 2 / 2 | CLIENT；完整条件及叠层句式，含当前调整后需求 |
| `GreedCardModifier.addText`、`TaskLootCardModifier.addText` | 2 / 2 | CLIENT；贪婪倍率/目标/位置及任务物品提示 |
| 七种 `modifier.deck.*.addText` | 7 / 7 | CLIENT；全局、槽位、板条箱、非陪衬、资源双倍、资源需求、颜色效率，含 Shift 范围 |
| `CardDeck.addText` | 1 / 5 | CLIENT；插槽、词缀标签及空行（空行原样保留） |
| `CardEntry.lambda$addScaledTooltip$0` | 1 / 1 | CLIENT；最终 `List.set`，保留当前缩放效果及原比较逻辑 |
| `CardDeckItem` 两个 tooltip lambda | 2 / 8 | CLIENT；插槽范围、布局网格图例 |
| `CardBinderBlock.m_5871_` | 1 / 3 | CLIENT；物品已存储数量，含 `256+` 上限显示 |
| `CardEntry.Color.getColoredText`、`CardNeighborType.getText` | 2 / 2 | BOTH；仅翻译 TextComponent 构造入参 |
| `CardItem.m_7626_` | 1 / 1 | BOTH；卡牌物品名称返回值精确覆盖 |
| `CardDeckItem.sendLockedMessage` | 1 / 1 | BOTH；装备/使用物品时的等级要求，保留实际最低等级 |

列表注入使用 ASM 来源分析，仅选择 receiver 来自 tooltip 参数的 `List.add(Object)` / `List.set(int,Object)`。同方法中的类型、套组及其他逻辑列表不受影响。保留 `Card.TYPES`、`getGroups`、筛选器、枚举名、NBT、网络字段及效果计算。`Color` 和相对位置只翻译显示 getter，枚举初始化不改。

通用模块不引用客户端 I18n。专用服务端读取 `card_text.json`，仅注册 4 个 BOTH 方法；其余 17 个方法只在客户端注入。

## VP 迁移与复用边界

从 `asm_main` 提取 15 组、`asm_complex` 提取 4 组、`dynamic_2` 提取 1 组，合计 **20 组、119 对**。包括匿名类中的类型/套组显示映射、旧枚举显示词、Card 的 local/ordinal 映射，以及 dynamic_2 的套组临时方案。原位置保留接管说明；历史片段在 [translations/vp/card_text.json](../translations/vp/card_text.json)，仅用于溯源及冲突测试。

新模块不依赖旧 VP 对同一类多个 local 的处理结果。VTP 的 PRE_CLASS 注入先于 VP 的 CLASS 处理，但后续 VP 仍可能改写原句；因此同时迁移相关 ASM/动态规则并加入冲突预检。旧词片段不能可靠合成完整句式，`prepare-vp` 遇到旧卡牌规则会拒绝自动移除，本模块不提供未经审核的片段导入命令。

复用已有译文和保留内容：

- `config/the_vault/card/` 内已有卡牌名、任务描述、卡组包名、补充包名和核心名继续由上游读取；不复制第二份名称表。`CardItem` 返回值允许在新模块增加实际显示名的精确覆盖。任务描述中的 `${task}`、`${count}`、`${current}`、`${target}` 仍由原算法处理，与新句式 `{0}` 无关。
- 装备属性说明仍由已有 gear reader / 语言资源处理；`CardEntry` 只接管外层“当前”说明。配置中不复制装备属性或词缀的整套语言表。
- 收纳界面搜索语法和固定筛选标签、精华提取器、卡组界面标题、卡组工作台、配方进度提示、管理命令及解锁广播固定句继续留 VP。卡组研究名仍由 `research_names` 维护。
- `BingoCard` 与本卡牌系统不同，不纳入。宝箱/怪物等任务文本已有配置来源，不因同词再次复制到 card_text。

## 验证与游戏验收

离线检查覆盖真实核心 JAR 摘要、所有目标方法、命中数、ASM 栈校验、非目标指令不变、重复/变更拒绝、两端注册、三份 VP 兼容性、参数重排与组件样式、未知值回退及错误重载。单元测试使用独立最小夹具；发布配置只验证结构和句式合法性，增删改译文不要求修改测试断言。

完整整合包客户端和专用服务端尚未游戏验收。联测按[当前安装说明](../../wiki/install.md)同步单 JAR、14 份模块配置及配套 VP，并处理旧 VP 缓存；检查多条件卡、贪婪卡、资源任务卡、七类卡组词缀、Shift/当前值、布局、物品数量、名称覆盖和 F3+T。
