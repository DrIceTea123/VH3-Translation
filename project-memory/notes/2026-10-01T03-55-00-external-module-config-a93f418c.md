# 两个模块改用独立外部配置并记录文本获取位置

- 日期：2026-10-01 03:55:00 Asia/Shanghai
- 来源：本对话，用户要求新增条目只改配置，将配置放到基础+硬编码汉化，源码与导出 mod 不保留配置；追问两个模块的原始文本获取方法
- 类型：用户要求 / 已接受决策 / 实现事实 / 验证结论
- 状态：VTP 1.0.3 实现、离线验证和导出完成；未游戏联测，未提交 Git
- 关联：T-006、T-005；取代 patch-modules 与 sound-module 记录中的内置默认配置、首次生成和首次失败回退策略

## 用户确认与实现

- 两个模块的当前运行时配置唯一维护位置为 `program/基础+硬编码汉化/config/vh3_translation_patch/combat_stats.json`、`sound_names.json`。由源码资源目录移动，分别 235 / 203 条，迁移前后字节完全一致。
- 现有模块增删改条目只需编辑 JSON，无需修改源码或重编译；增加接管方法或新模块仍需源码。安装时配置必须一起复制到游戏同名位置，工程文件不自动同步到实例。
- 移除了源码内的 module-defaults 配置和运行侧默认资源读取、自动生成逻辑；两个导出 JAR 均不含映射配置。历史 VP 规则及导入报告仍是维护/回归资料，不用于运行时加载，不打包。
- 用户明确选择：首次配置缺失或损坏阻止启动并提示修复。TranslationModule.initialize 将包含文件路径的异常向 Forge 事件传播；成功初始化后的 F3+T 失败仍保留每模块上次有效快照。删除文件也不重新生成，重启前必须恢复。空对象合法。
- 两个导入 Gradle 任务改为只输出候选配置到 transformer/build/imports/config/vh3_translation_patch，附带导入报告，不覆盖人工维护的正式配置，不写入源码资源。
- 测试不再要求正式配置等于历史映射或固定条目数；正式文件只验证严格 JSON 结构。历史导入测试独立核验旧 VP 对应关系。新增任意键及运行钩子、缺失/损坏初始加载、重载保留旧值均有覆盖。

## 原始文本分析

- 详细分析见 `patch-mod/docs/text-capture.md`，含字节码还原的等价代码；偏移指原始方法字节码，不是源码行号。
- combat_stats：统计键 ResourceLocation.toString() → formatMobName；构造方法一处调用偏移 563 → 566。原方法 split(":") 后取 parts[1]，下划线转空格、逐词首字母大写。原 ARETURN 在 15 和 143。
- sound_names：collectSoundEntries 反射 ModSounds 的 SoundEvent 字段，Field.getName() @141 → formatSoundName @144。原方法小写后按下划线分词，首字母大写，sfx 特判为 SFX；唯一 ARETURN @108。字段原名例 RAFFLE_SFX，非声音资源 ID。
- 两模块都保留原算法，在返回前从局部槽 1 取未被改写的原始参数，与原结果传给 translate。只有结算模块取第一个冒号之后的路径；声音模块精确使用完整字段名。不存在截断或替换整段上游算法。
- 配置不会新增原界面未收集的数据，或取消上游的 BOSS_FIGHT_1～4 过滤。声音名缓存于界面构造期，仍按已确认行为 F3+T 后重开。

## 验证与导出

- 通过 build.ps1 -BumpPatch 将 1.0.2 递增一次为 1.0.3，执行 clean build，使用现有缓存 offline 与单次 ForgeGradle 检查参数。未修改全局配置。
- 48 项测试通过，核心 JAR 哈希、方法摘要及转换后字节码检查通过；VP 兼容输出 removedGroups=0，原工程 VP 无需变化。
- 运行侧 JAR 10784 字节，转换侧 JAR 39316 字节，已导出到 program/基础+硬编码汉化/mods。两个导出文件与 build/distribution 哈希一致；检查没有 module-defaults、两个映射 JSON 或测试文件。旧 1.0.2 工程配套文件由构建清理。
- git diff --check 通过；没有暂存、提交或推送。未修改游戏实例，完整启动与 F3+T 事件仍待游戏联测。
