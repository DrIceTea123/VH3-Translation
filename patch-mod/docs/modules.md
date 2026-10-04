# 模块开发与架构

使用、配置和构建命令见 [README](../README.md)；结算与声音的输入与注入位置见 [文本获取](text-capture.md)。研究模块见 [研究名覆盖](research-names.md)，宝箱模块见 [宝箱名称](chest-names.md)，经验入口分工见 [经验提示](vault-xp.md)，卡牌组件与句式见 [卡牌显示](card-text.md)。本文只维护代码职责和新增模块步骤。

## 加载层与职责

modid 为 `vh3_translation_patch`，显示名 VH3 Translation Patch；包名前缀 `com.dricetea.vh3patch`。Forge 40.3.11 会把早期转换服务 JAR 排除在普通 mod 扫描外，因此从 1.0.16 起使用外层转换器＋内嵌运行模块＋专用定位器，分发单个 JAR，仍保留 SERVICE/GAME 分层。见 [单 JAR 实现](single-jar.md)。

| 层 / 入口 | 职责 |
|---|---|
| transformer 的 `PatchModule`、`PatchModules` | 模块契约/注册表，按目标类组织变换 |
| transformer 的 `modules/*Module` | 目标清单、运行侧类名、专属 VP 判定与历史导入 |
| `VerifiedMethodPatch`、`StringReturnPatch`、`StringValuePatch` | 共享方法校验/原子替换，返回值钩子及显示值钩子 |
| `EmbeddedRuntime`、`EmbeddedRuntimeLocator` | 校验单 JAR 安装布局、提取带哈希缓存并向 Forge 提供运行模块 |
| `TranslationTransformationService` | 启动预检、按端注册 PRE_CLASS 转换器与应用 |
| `PatchTool`、`JsonFiles`、`VpCompatibility` | 离线命令分派、JSON 读写、VP 遍历/迁移 |
| runtime 的 `modules/*Module` | 模块 ID、输入处理、配置查表与回退 |
| `TranslationModule`、`ModuleConfig` | 外部配置契约、严格解析、模块语义校验和有效快照 |
| `CommonModules` | 两端通用模块首次加载，不引用客户端类型 |
| `ClientModules` | 客户端注册与 F3+T；首次失败向 Forge 传播，重载失败保留旧值 |

同一功能在两侧各有一个模块类：运行侧在 `com.dricetea.vh3patch.modules`，早期侧在 `com.dricetea.vh3patch.transformer.modules`。两侧不共享 Java 包；早期侧通过类名字符串生成调用，不加载 Minecraft / the_vault 类型。通用设施共享，具体功能不堆入公共命令。

## 新增模块

1. 运行侧新增 `TranslationModule` 子类，定义唯一 ID（小写字母、数字、下划线），构造时 `super(ID)`；提供静态 `translate(String input, String fallback)`。
2. 默认以输入英文原样查表。特殊输入才覆盖 `mappingKey`，并先确认格式；不要全局统一转换成实体键。命中返回译文，未命中使用明确的回退策略。
3. 在 `program/基础+硬编码汉化/config/vh3_translation_patch/<ID>.json` 增加配置；客户端专属模块注册到 `ClientModules.MODULES`，两端模块注册到 `CommonModules`（客户端重载自动包含通用模块）。文件名由 ID 决定，不创建内置默认配置，不分语言；空模块用 `{}`。
4. 早期侧实现 `PatchModule`，创建 `transformer/src/main/resources/patches/<ID>.properties`，注册到 `PatchModules`。清单记录补丁/核心版本、JAR 哈希、类/方法/描述符、规范化摘要及命中数/适用端；声明运行侧入口和 `ownsVpRule`。
5. 实例 `(String)String` 方法可复用 `StringReturnPatch`；适用的模块保留原方法，在每个 ARETURN 前调用 helper。多个显示入口可用 `StringValuePatch` 在字段/getter/局部变量产生 String 后注入；有格式化回退时复制原文，传入双参数 helper。清单用 `target.count` 和 `target.<序号>.*` 描述多个方法，序号只标识清单项，不是字节码 ordinal。`CLIENT` 仅客户端，`BOTH` 包括专用与集成服务器。模块负责语义定位，`VerifiedMethodPatch` 统一核对摘要、命中数、重复注入及 ASM 验证。
6. 审查真实 JAR、非目标方法和业务 ID 不变、映射与未命中、重载及失败行为。注册表拒绝重复 ID/目标；同类多个目标按注册顺序处理。方法变更必须重新审查，不能只更新摘要消除错误。
7. 从正式 VP 配置移除接管规则，原位置留内容及 VTP 模块说明。旧规则保存到 `translations/vp/` 用于导入/冲突测试；不同文件或多组迁移应显式扩展策略，不能误删其他文案。
8. 按项目版本规则构建、验证并导出；完整游戏验收仍需单独执行。

修改方法时不能只复制 instructions：标签、异常表、局部变量、栈帧、合成方法和访问权限均需检查。通用钩子先改副本，摘要/返回数/ASM 校验全部通过才替换；当前保留原栈帧，只提高必要的最大栈深度。不要按易变的绝对指令序号定位，也不要在生产字节码中写死开发映射名称。

VTP 在 ModLauncher 9.1.3 的 PRE_CLASS 阶段校验与注入，先于 VP 的 CLASS 翻译；这样同一研究方法内保留的普通提示语可以继续由 VP 处理。其他更早的变换若改变目标方法，仍按摘要不匹配阻止加载。该阶段顺序已经核实，但不等于完成整个整合包的转换器联测。

## 旧 VP 导入与溯源

`translations/vp/mob_names.json`、`sound_names.json`、`research_names.json`、`chest_names.json` 仍被回归测试和导入任务读取，属于有效维护输入，不能当成失效运行配置删除。

`:transformer:importMobNames` / `:transformer:importSoundNames` / `:transformer:importResearchNames` / `:transformer:importChestNames` 调用模块自己的 `importMappings`；通用命令为 `import-module <模块ID> <旧VP文件> <目标JAR> <候选输出目录>`。Gradle 输出到 `transformer/build/imports/`：候选配置在 `config/vh3_translation_patch/`，报告在 `translations/`；不覆盖正式配置，不写回源码。

现有 `translations/mob-name-import.json`、`sound-name-import.json` 是历史来源报告，不参与游戏加载，也不进入 JAR。结算原 237 条导入 235 条，历史报告当时将 Black Widow Spider 与 Mummy 列为未确认；1.0.6 根据基线真实 ID 已另行加入 mummy 等 11 条译文；声音 203 条全部与真实字段唯一对应。正式配置可以独立增删改，测试只校验其结构，不强制等于历史数据。

宝箱历史片段包含混合组，通用迁移拒绝自动整组删除，必须逐对审核；宝箱导入只生成候选译文。VP 发布迁移支持无接管规则；旧模块各有一组，研究模块接管研究列表和卡组名称两组，重复组或混入无关目标时报错。研究历史 61 对合并为 54 个唯一键，Waystones 按用户选择采用旧文件中后一译名“传送石碑”。声音旧规则在调用方 `collectSoundEntries/local=MformatSoundName`，冲突判定同时覆盖调用方与格式化方法，保留同类其他界面翻译。

card_text 的历史片段另外保存在 `translations/vp/card_text.json`，仅供溯源和冲突回归。它使用完整句式替换旧零散片段，拒绝自动整组迁移，不提供片段直接导入。`ModuleConfig.Validator` 在新快照发布前执行模块语义校验；卡牌句式参数不合法时保留旧快照。

## 后续方向与限制

显示边界是唯一默认修改位置：不改业务 ID、存档/网络字段和 XP 计算。未知实体先检查注册表存在，避免默认实体误命中。客户端 I18n 不得进入服务端路径；复杂文本或新参数布局另行设计。

经验提示已拆分到 mob_names、chest_names 与 VP；`StringValuePatch.applyReturns` 支持任意参数布局的 String 显示返回值，宝箱钩子另传稳定枚举参数以避开已翻译的 getter 输出。`VerifiedMethodPatch` 可显式声明额外栈空间，默认仍为 1。

地图房间、Quest、主题、图鉴和特殊词缀已接管，见 [本轮报告](takeover-report.md)；[箱子类型迁移评估](chest-types-assessment.md) 的 getter 已纳入 chest_names，未新建并存的 chest_types 模块。通用 VP 转换、多版本差异报告、彻底移除 VP 都未完成，具体任务见 [TODO](../../project-memory/TODO.md)。ASM 的选择不构成性能优于 Mixin 的结论；特殊位置是否使用 Mixin 仍待确认。

本轮显示模块共用 `DisplayMethodPatch` 与 `TemplateModule`。`VerifiedMethodPatch` 深复制 invokedynamic 参数数组，保证修改 Lambda 引导参数时失败不影响原方法；图鉴用真实变换方法和最小依赖桩执行引导回归。铭文名称的 VP 所有权检查明确保留加载状态文案。
