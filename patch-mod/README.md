# VH3 Translation Patch（VTP）

独立的显示翻译补丁，目前接管结算怪物名称、声音设置名称和研究名称，其余翻译继续由 VP 处理。当前版本 **1.0.4**，目标 MC 1.18.2 / Forge 40.3.11 / the_vault 1.18.2-3.21.6.6884。61 项离线测试已通过，完整整合包尚未联测。

## 编辑翻译与安装

正式配置位于 [汉化工程的配置目录](../program/基础+硬编码汉化/config/vh3_translation_patch/)，不在源码资源或 JAR 中保留副本。

| 模块文件 | 键 | 示例 |
|---|---|---|
| combat_stats.json | 实体 ID 冒号后的路径 | `"aggressive_cow": "战斗牛"` |
| sound_names.json | ModSounds 原始 Java 字段名，区分大小写 | `"RAFFLE_SFX": "速通音效"` |
| research_names.json | 原始英文研究标识，区分大小写 | `"Waystones": "传送石碑"` |

给这三个模块增删改条目只改 JSON，无需改源码、重编译或替换 JAR。配置为 UTF-8 平面字符串对象，不允许注释、重复键、非字符串值及尾随内容；`{}` 表示不覆盖，空字符串表示显式空译文。

安装时同时携带：

- `mods/vh3_translation_patch-1.0.4.jar` 与 `mods/vh3_translation_patch-transformer-1.0.4.jar`。
- `config/vh3_translation_patch/` 下的三个 JSON（专用服务端只读取 research_names.json）。
- 当前汉化工程的 VP 配置；已接管规则应为原位置的 VTP 注释，不能装回旧规则。分类见 [VP 维护说明](../docs/maintenance/vp-asm-layout.md)。

游戏读取已安装目录的配置，修改工程文件不会自动同步。编辑游戏配置后按 **F3+T**；声音设置需关闭重开，结算页可重开以重建显示项。首次缺失或损坏文件阻止启动；重载失败保留该模块上次有效快照。不会自动创建、补齐或覆盖文件。

配置只影响所属模块，不覆盖全局语言键。未命中时，结算模块尝试已注册实体的当前语言译名，再回退原结果；声音与研究模块直接回退原结果。原始参数、截取方式、上游算法和注入点见 [文本获取分析](docs/text-capture.md)。新增方法或模块需写代码，见 [模块开发](docs/modules.md)。

## 客户端与服务端

两端使用同一对 JAR，按目标方法声明的适用端注册。结算和声音模块只作用于客户端界面；研究模块覆盖客户端的研究标题、依赖/互斥提示、限制提示、知识精酿、卡组工作台及配方提示，并在服务端生成解锁广播、研究替换通知时翻译名称。具体方法与原始文本见 [研究名覆盖](docs/research-names.md)。

专用服务端必须安装双 JAR、外部 research_names.json 及兼容的 VP 配置，首次配置失败同样阻止启动。服务端不读取结算/声音配置，也不加载客户端 I18n；服务端配置修改后重启生效。客户端继续支持 F3+T，缓存的界面可重开；已发出的消息不会追溯重译。

服务端生成的通知由服务端配置决定，客户端本地配置不能改变已经收到的文本。仅客户端安装 VTP 时，只覆盖本地显示；单人游戏的集成服务器使用同一进程的研究模块和配置。研究名称用原文精确查表，不改解锁判定、存档或业务网络标识。

## 构建与导出

在本目录执行：

```powershell
.\build.ps1                         # 构建当前版本
.\build.ps1 -BumpPatch              # 普通 mod 修改：修订号 +1，构建并导出
.\build.ps1 -BumpCore               # 已完成核心版本适配后：小版本和修订号均 +1
```

脚本选择 JDK 17 并在结束后恢复环境，可用 `-JavaHome 'JDK 17 目录'` 指定。版本规则见 [RULES](../project-memory/RULES.md)：修订号不重置，同轮失败重试不带递增参数。仅整理文档或删除空目录不改变 mod 版本。

完整 `build` 包含测试、真实核心哈希/方法摘要/字节码校验和导出：

- `build/distribution/`：两个 JAR 与 `compat/` VP 兼容副本/迁移报告。
- `../program/基础+硬编码汉化/mods/`：自动复制同版本双 JAR，逐字节验证后仅清理本补丁旧 JAR。
- `transformer/build/verification/`：变换 class 和校验报告；子工程 `build/reports/tests/test/`：测试报告。

构建不覆盖正式外部配置，不启动游戏、不修改游戏实例、不提交 Git。`check` 或 `distribution` 本身不导出到工程，`build` / `exportToProgram` 才导出。兼容输入已迁移时报告 `removedGroups=0` 正常。

默认只读父工作区 `origin-3.21.7/the_vault-1.18.2-3.21.6.6884.jar`；其他位置用 `-PvaultJar=<路径>`，不会绕过摘要检查。首次构建需下载依赖；已具备缓存但 ForgeGradle 联网检查失败时，可单次执行：

```powershell
.\build.ps1 -GradleArgs 'build','--offline','-Dnet.minecraftforge.gradle.check.certs=false','--console=plain'
```

不把该联网检查参数持久化。源码及 JSON 显式使用 UTF-8，但不要对 Windows JDK 17 的 Gradle 守护进程强设 `-Dfile.encoding=UTF-8`，否则中文路径参数文件可能解析错误。

只验证可传 `-GradleArgs 'check','--console=plain'`；核对目标用 `:transformer:inspectTarget` / `:transformer:verifyTarget`。旧 VP 导入只生成候选文件，见模块开发说明。

## 联测与回退

在独立测试副本中安装双 JAR、三个外部配置及当前 VP 配置，保留并合并原有自定义译文。核实 VP 缓存；首次联测可在测试副本设置 `debug_mode.use_cache=false`。检查 preflight / applied 日志、结算牛与首领差异、声音搜索/排序/音量、F3+T、首次错误拒绝启动及重载错误保留旧值。

回退时移除两个 JAR、恢复旧 VP 规则并处理缓存。核心/方法/配套/VP 冲突会阻止启动；其他转换器造成的冲突可能在目标类加载时才出现。支持生产客户端 `forgeclient` 与专用服务端 `forgeserver`，尚未接入开发/数据生成启动目标。剩余验收统一见 [TODO](../project-memory/TODO.md)。
