# VTP 单 JAR 可行性评估

> 后续状态：用户已批准实施，1.0.16 单 JAR 已完成离线验证；见 [交付说明](single-jar.md)。以下保留评估时的依据与结论。

评估日期：2026-10-04。基线：VTP 1.0.15、Minecraft 1.18.2、Forge 40.3.11、ModLauncher 9.1.3、JDK 17。用户本次要求优先评估合并；VP 格式升级顺序相应后移。本报告不代表已实施合并或完成游戏验收。

## 结论与建议

**可以实现玩家只安装一个 JAR。建议采用外层转换器＋内嵌运行侧 JAR＋专用 IModLocator，保留现有两个加载层和两套源码职责。**

直接解压合并现有两个 JAR，无法沿用默认 Forge 加载流程。若要求把所有类真正放进同一个普通 mod 加载层，则需要重做早期转换入口，代价和兼容风险明显更大。本项目没有必要为了少一个安装文件重写现有翻译模块。

目前对推荐方案的把握为：**加载机制有明确入口，离线扫描与运行侧 mod 识别已验证，完整启动尚待原型联测。** 不把“能识别内层 mod”写成“已验证全部游戏功能”。

## 已核实的限制

Forge 40.3.11 的实际源码和扫描探针给出以下链路：

1. ModDirTransformerDiscoverer 只扫描 mods 目录的顶层 JAR，检查根目录的 ITransformationService 或 IModLocator 服务文件。发现后加入 SERVICE 层候选及排除列表。
2. ModsFolderLocator.scanCandidates 明确排除该列表中的文件。因此，把运行侧 @Mod 和 mods.toml 合到现有 transformer JAR，并不能让它同时成为普通 mod。
3. ModLauncher 先发现并初始化转换服务，再进入扫描阶段。Forge 的 ModDiscoverer 在 SERVICE 层通过 ServiceLoader 发现 IModLocator。这为外层转换器提供一个可用的运行侧定位入口。
4. Forge 的 JarInJarDependencyLocator 从已发现的 mod 候选寻找嵌套依赖。转换器外壳被默认普通扫描排除，单独增加 jarjar 元数据不会自动把它变成该步骤的父 mod；反向把转换器嵌在普通 runtime mod 内，也不能让它进入已经先行完成的转换服务发现流程。

ForgeGradle 的 [Jar-in-Jar 文档](https://docs.minecraftforge.net/en/fg-6.x/dependencies/jarinjar/) 说明了嵌套依赖及 META-INF/jarjar/metadata.json 的打包方式，但没有承诺嵌套 ITransformationService 会被早期发现。这里的时序结论以本机固定版本加载器源码及探针为依据，而不是套用其他 Minecraft 版本的打包教程。

## 方案比较

| 方案 | 单个安装文件 | 对现有实现的影响 | 判断 |
|---|---|---|---|
| 直接把两个 JAR 平铺合并 | 是 | 运行侧失去默认 mod 发现入口；还存在加载层可见性问题 | 不采用 |
| 只用标准 Jar-in-Jar，任一侧包住另一侧 | 是 | 单靠嵌套不能同时满足早期服务和普通 mod 发现 | 不足以完成合并 |
| 外层 transformer＋内嵌 runtime＋专用定位器 | 是 | 改打包、运行侧定位与配套预检；翻译模块和 PRE_CLASS 变换保留 | 推荐 |
| 普通 mod＋改用 Mixin/Forge coremod 等入口 | 是 | 必须重新设计转换时序与原始指纹校验，验证与 VP 的先后顺序 | 技术上可研究，当前不推荐 |
| 保持双 JAR，由安装程序一次安装 | 安装包可单文件，mods 仍两文件 | 几乎无需改动；手动升级仍需保持配套 | 最保守备选 |

同一物理 JAR 通过过滤视图重复注册进不同加载层也值得研究，但需要自行处理包集合、模块名称和资源可见性；相较内嵌独立运行侧 JAR，收益有限、实现更脆弱，不作为本轮推荐。

## 推荐结构

```text
mods/vh3_translation_patch-<version>.jar
  META-INF/services/...ITransformationService
  META-INF/services/...IModLocator
  com/dricetea/vh3patch/transformer/...     ← SERVICE 层
  patches/...
  META-INF/vh3_translation_patch/runtime.jar
      META-INF/mods.toml
      com/dricetea/vh3patch/...            ← GAME 层
```

外部 14 个翻译配置继续放在 config/vh3_translation_patch，不打进 JAR。现有 runtime 模块、配置加载和 F3+T 逻辑保留；项目源码仍分 transformer 与 runtime。

优先采用“内层 JAR 提取至游戏目录内专用缓存，再由定位器返回”的方式。提取路径应包含版本和内容哈希，校验后原子发布；不在 mods 自动生成第二个安装文件。一次启动内转换服务与定位器共享同一份已验证结果，不依赖服务枚举顺序，不在运行中删除正在使用的缓存。首次写入失败要明确报错；缓存损坏可从内嵌原件恢复。无落盘嵌套文件系统可作为后续优化，不作为第一版必要条件。

这是“单文件分发、内部仍分层”，并不等于运行时只存在一个类加载器或完全没有缓存文件。

## 必须改动的地方

| 位置 | 必要改动 |
|---|---|
| transformer 外层资源 | 注册 IModLocator；外层不放 Minecraft/Forge 运行侧类 |
| 构建与导出 | runtime 先 reobf，再嵌入外层；仅导出一个成品，检查内外版本和内层哈希 |
| TranslationTransformationService.preflight | 现在仅扫描 mods 中独立 runtime JAR；改为校验内嵌 runtime，并识别误留的旧 runtime/transformer 安装 |
| 运行侧配套检查 | 保留 ready/version 协议，调整“两文件配套”的报错文案；单文件同样需要确认转换服务成功启动 |
| 定位器与缓存 | 提取/校验内层、通过 Forge ModFile 入口返回候选、处理重复加载及损坏恢复 |
| 测试与安装说明 | 旧版双 JAR→新版单 JAR 的迁移、回退、安装包清理范围，以及客户端/服务端加载验证 |

原核心精确哈希、方法摘要、105 个目标方法、VP 冲突检查、PRE_CLASS 优先于 VP CLASS 的策略继续保留。将 runtime 隐藏进内层而不修改现有 preflight，会先得到“没有配套 runtime”的错误，不能只改 Gradle 打包任务。

## 离线验证结果与边界

使用当前 1.0.15 两个真实成品生成隔离布局，调用缓存中的 Forge 40.3.11 实际扫描器，结果如下：

| 布局 | 早期服务候选 | 普通 mod 候选 |
|---|---|---|
| 当前双 JAR | transformer.jar | runtime.jar |
| 直接平铺合并 | flat.jar | 无 |
| runtime 外层、内嵌 transformer | 无 | runtime-outer.jar |
| transformer 外层、内嵌 runtime | transformer-outer.jar | 无 |

另从最后一种外层中提取未经改写的 runtime JAR，由最小专用定位器调用 Forge AbstractJarFileModLocator.scanMods：成功返回 MOD，mods.toml 解析出的 modId 为 vh3_translation_patch，运行侧 helper 资源存在。

嵌套布局探针只验证早期扫描是否递归检查内层；没有生成标准 jarjar 元数据，也没有完整执行 JarInJarDependencyLocator。关于标准 Jar-in-Jar 的限制来自前述源码时序分析。最小定位器由探针直接实例化，未验证它在真实 SERVICE 模块层的服务注册。探针为 mods.toml 解析补充了版本元数据夹具；没有执行 ModLauncher 启动流程、加载 Minecraft 类、实例化 @Mod 或启动服务器。这些限制均不能由现有 199 项翻译回归测试覆盖。

复现工具：tools/assessment/assess-jar-layout.py 和 JarLayoutProbe.java。以 `--runtime` 和 `--transformer` 指定评估时归档的旧双 JAR，执行 Python 脚本后，临时布局与原始输出位于 build/merge-assessment。固定证据保存在 [评估记录](evidence/jar-merge-assessment.json)。工具只读取当前成品与本机依赖缓存，写入 build；不替换 program mods。

若进入实施，发布门槛为：

1. 专用定位器在真实 SERVICE 层被发现；内层 runtime 在 GAME 层被识别、实例化，外层不提前加载 Minecraft 类。
2. 单 JAR 客户端和专用服务器都能启动，14 个模块按端初始化，105 个目标方法的转换顺序保持正确。
3. F3+T、首次缺失配置、损坏重载保留旧值、旧 VP 冲突保护均保持行为。
4. 缓存缺失/损坏/不可写、重复外壳、误留旧双 JAR、升级与回退能得到确定结果和明确提示。
5. 现有 199 项回归继续通过，并补足新打包、定位器、缓存和安装迁移测试。

## 本阶段状态

评估已完成，建议下一步仅在用户批准方案后实施单文件分层封装。此次没有改生产加载代码、构建导出任务、正式 JAR 或版本号；VTP 仍为 1.0.15。前一轮 VP 检测修复尚未提交，本次评估也未提交，避免在用户确认之前混合提交。原先确认的双 JAR 架构限制应理解为“默认扫描不能直接平铺合并”，而不是“无法做单文件分发”。

主要本机源码依据：fmlloader-1.18.2-40.3.11-sources.jar 的 ModDirTransformerDiscoverer、ModsFolderLocator、ModDiscoverer、JarInJarDependencyLocator、AbstractModLocator；modlauncher-9.1.3-sources.jar 的 TransformationServicesHandler；forgespi-4.0.15-4.x-sources.jar 的 IModLocator。对应归档散列记入评估记录。
