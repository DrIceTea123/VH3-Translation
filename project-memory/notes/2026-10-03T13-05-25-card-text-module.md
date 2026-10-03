# 卡牌显示接管：VTP 1.0.7

- 日期：2026-10-03，Asia/Shanghai。
- 来源：用户要求整理 Card 及散落卡牌内容，提取 asm_main、asm_complex、dynamic_2 并按需调整语序；用户进一步明确优先名称、条件、效果及物品提示，界面和命令固定文案留 VP。
- 状态：实现、配置迁移、离线测试和双 JAR 导出完成；未做完整游戏联测，未构建安装包，未提交 Git。
- 关联：2026-10-03T07-31-43-name-module-split-6c198be2。普通轮次追加记录，未归并共享快照。

## 实现与边界

- 新增 card_text，运行侧和转换侧各有模块类；正式配置 101 项（44 个普通键、57 个句式键），全部外置于 program/基础+硬编码汉化/config/vh3_translation_patch/card_text.json，不在生产源码/JAR 保存译文表。
- 21 个方法、36 个钩子。Card 的等级/颜色/类型/套组；条件与叠层；贪婪与任务提示；七类卡组词缀及 Shift 范围；CardEntry 当前缩放效果；卡组包插槽/布局图例；收纳册物品数量；卡牌名称和装备/使用卡组包的最低等级要求。
- 两端通用模块，4 个 BOTH 方法为 CardItem.m_7626_、Color.getColoredText、CardNeighborType.getText、CardDeckItem.sendLockedMessage；其余 17 个方法仅客户端。专用服务端现在读取 research_names、chest_names、card_text，客户端额外读取 mob_names、sound_names。
- 使用来源分析，只在 tooltip 参数作为 receiver 的 List.add/set 前处理最终 Component；不翻译业务列表。枚举仅改显示 getter 的构造参数，未改 Card.TYPES、类型/套组 ID、枚举标识、筛选逻辑、NBT/网络字段或数值算法。
- 英文整句带 {0} 等参数，支持重排、嵌套“当前”、条件分隔词及 Shift 范围；参数保留原有效样式、悬浮及点击信息。完整精确键优先，然后按固定文字长度排列句式，最后精确处理子组件；不全局替换名称内部单词。
- ModuleConfig 增加发布快照前的模块校验钩子。句式参数从 0 连续编号，源参数唯一且不相邻，最多 8 个；非空译文需保留全部参数。首次无效阻止启动，重载失败保留该模块整个旧快照；客户端 F3+T、服务端重启。
- config/the_vault/card 中已有名称/任务描述继续沿用，不复制整套数据；CardItem 允许精确名称覆盖。装备属性继续使用原 gear reader / 语言资源，VTP 只处理外层卡牌句式。

## VP 迁移

- main 15 组、complex 4 组、dynamic_2 1 组，共 20 组 119 对；提取类型/套组、枚举显示名、条件片段、效果片段及物品提示，并移除旧套组动态临时方案。原位置补 VTP 注释。
- 历史规则存 patch-mod/translations/vp/card_text.json；冲突检查包含 classwide、local/ordinal、匿名显示列表与枚举初始化。旧片段不能机械合成新句式，prepare-vp 拒绝自动迁移旧卡牌组；没有不可靠的片段导入命令。
- UI 搜索语法/筛选标签、提取器、工作台、卡组界面标题、配方提示、管理命令及解锁广播固定句仍留 VP。BingoCard 不属本系统；卡组研究名称继续由 research_names 处理。
- 编辑过程中检测到其他工作更新了矿石译名、其他 VP 规则和独立配置。保留这些并发改动；新增接管注释时按目标顺序合入当前文件，验证其他保留规则逐值一致，没有用早期快照覆盖它们。
- 实查时 ASM 当前统计：main 609组/2766对/local35；long 20组/1126对/local3；complex 12组/288对/local7/ordinal4；合计 641组/4180对/local45/ordinal4。动态文件不计入。数值含其他工作的现行变更，并非全由卡牌迁移产生。

## 验证与交付

- 1.0.6 → 1.0.7，本轮只递增一次；失败重试沿用 1.0.7。
- 完整 build 成功：runtime 34 + transformer 59 = 93 项测试。全部 45 个目标方法 / 105 个钩子通过固定核心哈希、方法摘要与 ASM 校验；新增卡牌部分为 21 / 36。
- 卡牌测试覆盖指令还原、非目标业务不变、来源限定、重复/变更/错误命中数拒绝、两端过滤、三份 VP 兼容性、语序/动态值/组件样式、未知值、精确名称、配置校验和失败重载。发布词表仅做结构及语义校验，测试独立最小夹具不冻结用户译文。
- 首轮还对发布句式的 25 种输入及 4 组边界执行过检查并通过；持久回归改为独立小夹具，避免以后改译文需同步断言。
- 完整构建发现旧 XpVpRulesTest 固定要求 Ore Funsoide 保持英文，与当前“方索伊德矿石”冲突。保留用户译文，改为逐对核对 VP 实际配置值，仍测试未知矿石回退及真实注入位置。
- program/基础+硬编码汉化/mods 已导出 vh3_translation_patch-1.0.7.jar 与同版本 transformer；构建核对复制内容，只清理旧 VTP JAR。成品没有 JSON 译文副本。
- 本轮 patch-mod 及维护文档的 git diff --check 通过；全工作区检查另报其他并发配置编辑的 CRLF/空白问题，未据此改写无关文件。
- 新增 docs/card-text.md，更新 README、模块架构、VP 布局说明；完整游戏验收仍未执行。需同步五份模块配置、双 JAR、迁移后 VP 并处理旧缓存。
