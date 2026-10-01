# 研究名称：覆盖与文本获取

模块为 `research_names`，配置唯一维护于 [research_names.json](../../program/基础+硬编码汉化/config/vh3_translation_patch/research_names.json)。当前导入 54 个唯一键，形式为 `"Waystones": "传送石碑"`。键是 the_vault 使用的原始英文研究标识，例如 `Refined Storage`、`Vault Decks`；保留大小写、空格，不截取、不转成语言键。

## 显示边界

以下根据固定核心 `1.18.2-3.21.6.6884` 的真实字节码分析。表中偏移仅供查阅；生产定位使用方法签名/摘要及相邻指令语义，不使用这些绝对偏移。补丁不截断原方法，只在显示值产生后调用模块查表。

| 类 / 方法 | 取得的原文与注入位置 | 生效位置 |
|---|---|---|
| `ResearchDialog.renderHeading` | `researchName` 字段 @145，紧接 `TextComponent` 构造 @148；同方法另外两次业务读取保留 | 客户端研究详情标题 |
| `ResearchWidget.renderHover` | `researchName` 字段 @24，进入 `Style` / `FormattedCharSequence` 前；依赖查询仍用原名 | 客户端悬浮标题 |
| `ResearchWidget.lambda$renderHover$1` | `Research.getName()` 返回值 @2，拼接依赖条目前 | 客户端前置依赖研究 |
| `ResearchWidget.lambda$renderHover$2` | `Research.getName()` 返回值 @2，拼接互斥条目前 | 客户端互斥研究，补齐旧 VP 未覆盖处 |
| `StageManager.warnResearchRequirement` | 参数槽位 0 的 String @4，构造文本 @5 前 | 客户端制作、使用、方块/实体交互和攻击等研究限制提示 |
| `StageManager.lambda$onItemTooltip$0` | `ResearchTree.restrictedBy(...)` 返回值 @6，仅此返回值流向提示；`null` 原样保留 | 客户端物品研究要求提示 |
| `KnowledgeBrewItem.lambda$appendHoverText$3` | 参数槽位 1 的研究名 @21，构造文本 @22 前 | 客户端知识精酿研究树提示 |
| `DeckCraftingScreen.lambda$addTabButtons$2` | `getDeckResearchName()` 返回值 @21，构造文本 @24 前 | 客户端卡组工作台研究锁定提示 |
| `DeckForgeRecipe.getDisabledText` | 原始 `getDeckResearchName()` @107；包围 `convertToTitleCase` @110，保留原文和原格式化结果 | 客户端卡组配方的解锁提示，补齐旧 VP 未覆盖处 |
| `PlayerResearchesData.lambda$research$3` | `Research.getName()` 返回值 @70，构造研究名称文本前 | 服务端研究解锁广播 |
| `PlayerResearchTransferUtil.onPlayerLogin` | 局部槽位 7、9 的旧/新研究名 @250、@262，在文本 `append` 前 | 服务端研究替换通知；前面的移除/添加操作保留原标识 |

共 8 个类、11 个方法、12 个注入点。前 9 个方法声明 `CLIENT`；后 2 个声明 `BOTH`，供专用服务器以及客户端进程中的集成服务器使用。完整包名、描述符、摘要和命中数在 [方法清单](../transformer/src/main/resources/patches/research_names.properties)。

一般显示值调用 `translate(原文)`，无映射原样返回。卡组配方调用 `translate(原文, 原格式化结果)`，使用大小写转换前的原文查表，未命中时保留上游标题格式。映射只改界面/通知中的研究名称，提示前后缀、颜色和排版继续走上游及保留的 VP 翻译。

## 未改变的逻辑

没有替换 `Research.getName()`、`ResearchTree.restrictedBy()` 或卡组研究配置 getter 本身。研究查询、是否已研究、依赖判定、解锁/移除、研究树序列化、同步包和服务端日志仍使用原始标识。审查也包括方法引用与研究字段读取，不能因为名字相同就翻译所有调用点。

旧 VP 的研究列表与卡组名单条已移除并保留原位注释；历史输入放在 `translations/vp/research_names.json`，只供导入和冲突测试，不进入 mod JAR。Waystones 的两个旧译名按用户选择统一为“传送石碑”。新增真实研究只需添加外部 JSON 条目；新增显示入口或上游方法变更仍需代码适配。

## 两端配置与验证

客户端 F3+T 重载，错误保留旧配置；缓存的界面可重开。专用服务端启动时读取自己的 `research_names.json`，修改后重启；不读取结算/声音文件、不加载客户端语言系统。首次缺失或损坏文件均阻止启动。服务端生成的消息采用服务端配置，客户端不能用本地配置重新翻译已接收的字符串；已发送消息不会追溯更新。

离线验证覆盖全部目标的 ASM 分析、移除新增钩子后逐方法摘要与上游一致、非目标方法不变、服务端只注册通用目标、重复/缺失/不匹配拒绝加载、历史导入/VP 冲突以及外部配置重载。VTP 用 PRE_CLASS 阶段先于 VP 的 CLASS 阶段处理，保留同方法内的普通 VP 文案。真实客户端/专用服务器启动、研究操作、通知与整合包转换器共存仍需游戏联测。
