# 添加与维护模块

一个模块对应一个明确的接管内容。当前只有结算界面的怪物名称 `combat_stats`；本次没有接管经验提示、声音或其他 VP 条目。

## 当前代码入口

以下路径均相对于 `patch-mod/`；Java 文件以 `com.dricetea.vh3patch` 为包名前缀。

| 文件 / 类 | 职责 |
|---|---|
| `runtime/.../modules/CombatStatsModule.java` | 结算怪物名的运行逻辑，`ID = "combat_stats"` 决定配置名；原始 ID 取路径后查表 |
| `runtime/.../module/TranslationModule.java` | 通用模块基类，默认以英文输入原样查表，持有自己的配置 |
| `runtime/.../config/ModuleConfig.java` | 严格读取 JSON、首次生成、成功后整体替换快照 |
| `runtime/.../client/ClientModules.java` | 客户端模块列表和 F3+T 重载监听 |
| `runtime/src/main/resources/module-defaults/combat_stats.json` | 默认的 235 条映射，打包进运行侧 JAR |
| `transformer/.../transformer/modules/CombatStatsModule.java` | 对应的 ASM 模块，声明目标清单、运行侧类名和 VP 冲突常量 |
| `transformer/.../transformer/PatchModules.java` | 早期模块列表，启动检查与离线工具共用 |
| `transformer/.../transformer/StringReturnPatch.java` | 适用于实例 `(String)String` 方法的通用返回值钩子 |
| `transformer/src/main/resources/patches/combat_stats.properties` | 该模块绑定的上游版本、JAR 哈希、方法摘要与返回点数量 |

两个 JAR 属于不同加载层，所以一个功能各有一个同名模块类，分别放在 `com.dricetea.vh3patch.modules` 和 `com.dricetea.vh3patch.transformer.modules`。不要把两个 JAR 的类放入同一个 Java 包，也不要在早期层直接 import 游戏侧类。

## 新模块步骤

1. 在运行侧 `modules` 包增加一个 `TranslationModule` 子类，声明唯一的 `ID`（小写字母、数字和下划线），构造时传给 `super(ID)`。增加静态 `translate(String input, String fallback)` 入口：先调用该实例的 `configuredTranslation(input)`，未命中则返回原结果或执行该模块明确的后备逻辑。
2. 默认不要重写 `mappingKey`，此时以英文原文为键，大小写和空格都精确匹配。只有输入本身为标识或需要特别处理时，才像结算模块那样重写。不要在公共层把所有文本转成实体键。
3. 增加 `runtime/src/main/resources/module-defaults/<ID>.json`，内容为字符串到字符串的平面对象。空模块可先使用 `{}`，但不能缺少文件。加入 `ClientModules.MODULES`；框架自动生成 `config/vh3_translation_patch/<ID>.json` 并监听重载。
4. 在早期层 `transformer.modules` 包增加对应模块类，实现 `PatchModule`。加载自己的 `patches/<ID>.properties`，以字符串声明运行侧类名，并加入 `PatchModules` 的列表。运行侧与早期层使用同一 ID。
5. 对实例 `(String)String` 方法可以复用 `StringReturnPatch`；不同参数、静态方法、返回类型或替换策略应自行实现模块的 `target/apply`，不能强套这个钩子。清单目前描述单个目标方法，扩展到多方法模块需要相应扩展契约。
6. 用真实上游 JAR 审查并记录签名、哈希、方法摘要、预期返回数量，添加覆盖正常行为和失败情况的测试。公共注册表会拒绝重复 ID 和重复目标，同一个类的不同模块按注册顺序一起处理。
7. 核实 VP 的接管范围，在原位置留下说明内容和 VTP 接管情况的 `_comment`。旧规则另存于 `translations/vp/` 用于导入及冲突回归测试。当前发布迁移命令支持无接管规则或恰好一组规则，遇到重复组会报错；若新模块分散在不同文件或有不同迁移语义，应扩展迁移输入/策略。

配置只在模块入口内使用，不注入原版全局语言系统。框架不做语言筛选，也不会让其他模块继承某个模块的译文。旧 `importMobNames` 命令现在从 `translations/vp/combat_stats.json` 导入，只用于结算模块，会重写源码默认映射和导入报告，不适合作为其他模块的通用导入器。

## 更新与联测

在源码中修改默认 JSON 会影响下一次构建；玩家已经生成的配置不会被新 JAR 自动覆盖，需要明确合并新增条目。开发时不要通过重新打包来覆盖用户文件。

使用 `build.ps1` 执行测试、真实字节码校验和发布映射；配套安装两个同次构建的 JAR。版本/摘要/VP 冲突仍会阻止启动。配置读取错误仅保留有效快照并报错，两种错误的处理策略不同。

客户端验证应覆盖：首次生成文件、编辑 F3+T、损坏配置、修复后再次 F3+T、结算中的牛与首领，以及未配置名称的回退。离线测试不能代替完整整合包中的加载层和界面验证。
