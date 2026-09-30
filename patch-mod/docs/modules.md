# 模块开发与架构

使用、配置和构建命令见 [README](../README.md)；两个现有方法的输入与注入位置见 [文本获取](text-capture.md)。本文只维护代码职责和新增模块步骤。

## 加载层与职责

modid 为 `vh3_translation_patch`，显示名 VH3 Translation Patch；包名前缀 `com.dricetea.vh3patch`。Forge 40.3.11 会把早期转换服务 JAR 排除在普通 mod 扫描外，因此使用两个配套 JAR。

| 层 / 入口 | 职责 |
|---|---|
| transformer 的 `PatchModule`、`PatchModules` | 模块契约/注册表，按目标类组织变换 |
| transformer 的 `modules/*Module` | 目标清单、运行侧类名、专属 VP 判定与历史导入 |
| `StringReturnPatch`、`MethodFingerprint`、`TargetJar` | 返回钩子、规范化摘要、原 JAR 检查 |
| `TranslationTransformationService` | 启动预检、客户端转换器注册与应用 |
| `PatchTool`、`JsonFiles`、`VpCompatibility` | 离线命令分派、JSON 读写、VP 遍历/迁移 |
| runtime 的 `modules/*Module` | 模块 ID、输入处理、配置查表与回退 |
| `TranslationModule`、`ModuleConfig` | 外部配置契约、严格解析和有效快照 |
| `ClientModules` | 客户端注册与 F3+T；首次失败向 Forge 传播，重载失败保留旧值 |

同一功能在两侧各有一个模块类：运行侧在 `com.dricetea.vh3patch.modules`，早期侧在 `com.dricetea.vh3patch.transformer.modules`。两侧不共享 Java 包；早期侧通过类名字符串生成调用，不加载 Minecraft / the_vault 类型。通用设施共享，具体功能不堆入公共命令。

## 新增模块

1. 运行侧新增 `TranslationModule` 子类，定义唯一 ID（小写字母、数字、下划线），构造时 `super(ID)`；提供静态 `translate(String input, String fallback)`。
2. 默认以输入英文原样查表。特殊输入才覆盖 `mappingKey`，并先确认格式；不要全局统一转换成实体键。命中返回译文，未命中使用明确的回退策略。
3. 在 `program/基础+硬编码汉化/config/vh3_translation_patch/<ID>.json` 增加配置；注册到 `ClientModules.MODULES`。文件名由 ID 决定，不创建内置默认配置，不分语言；空模块用 `{}`。
4. 早期侧实现 `PatchModule`，创建 `transformer/src/main/resources/patches/<ID>.properties`，注册到 `PatchModules`。清单记录补丁/核心版本、JAR 哈希、类/方法/描述符、规范化摘要及返回数；声明运行侧入口和 `ownsVpRule`。
5. 实例 `(String)String` 方法可复用 `StringReturnPatch`；当前两个模块保留原方法，在每个 ARETURN 前调用 helper。不同签名、静态方法或控制流修改需要独立策略；当前清单只描述单方法，不强套现有钩子。
6. 审查真实 JAR、非目标方法和业务 ID 不变、映射与未命中、重载及失败行为。注册表拒绝重复 ID/目标；同类多个目标按注册顺序处理。方法变更必须重新审查，不能只更新摘要消除错误。
7. 从正式 VP 配置移除接管规则，原位置留内容及 VTP 模块说明。旧规则保存到 `translations/vp/` 用于导入/冲突测试；不同文件或多组迁移应显式扩展策略，不能误删其他文案。
8. 按项目版本规则构建、验证并导出；完整游戏验收仍需单独执行。

修改方法时不能只复制 instructions：标签、异常表、局部变量、栈帧、合成方法和访问权限均需检查。通用钩子先改副本，摘要/返回数/ASM 校验全部通过才替换；当前保留原栈帧，只提高必要的最大栈深度。不要按易变的绝对指令序号定位，也不要在生产字节码中写死开发映射名称。

## 旧 VP 导入与溯源

`translations/vp/combat_stats.json`、`sound_names.json` 仍被回归测试和导入任务读取，属于有效维护输入，不能当成失效运行配置删除。

`:transformer:importMobNames` / `:transformer:importSoundNames` 调用模块自己的 `importMappings`；通用命令为 `import-module <模块ID> <旧VP文件> <目标JAR> <候选输出目录>`。Gradle 输出到 `transformer/build/imports/`：候选配置在 `config/vh3_translation_patch/`，报告在 `translations/`；不覆盖正式配置，不写回源码。

现有 `translations/mob-name-import.json`、`sound-name-import.json` 是历史来源报告，不参与游戏加载，也不进入 JAR。结算原 237 条导入 235 条，Black Widow Spider 与 Mummy 尚未确认 ID；声音 203 条全部与真实字段唯一对应。正式配置可以独立增删改，测试只校验其结构，不强制等于历史数据。

VP 发布迁移支持无接管规则或恰好一组；重复组报错。声音旧规则在调用方 `collectSoundEntries/local=MformatSoundName`，冲突判定同时覆盖调用方与格式化方法，保留同类其他界面翻译。

## 后续方向与限制

显示边界是唯一默认修改位置：不改业务 ID、存档/网络字段和 XP 计算。未知实体先检查注册表存在，避免默认实体误命中。客户端 I18n 不得进入服务端路径；复杂文本或新参数布局另行设计。

后续候选为经验提示三个 formatter 与预览、地图房间、研究/任务显示入口、动态规则。通用 VP 转换、多版本差异报告、彻底移除 VP 都未完成，具体任务见 [TODO](../../project-memory/TODO.md)。ASM 的选择不构成性能优于 Mixin 的结论；特殊位置是否使用 Mixin 仍待确认。
